package ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.swimmingmanager.data.AppDatabase
import com.example.swimmingmanager.data.RaceRecord
import com.example.swimmingmanager.data.RelayRecord
import com.example.swimmingmanager.data.Swimmer
import com.example.swimmingmanager.engine.EventDictionary
import com.example.swimmingmanager.engine.RaceEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordsScreen(
    db: AppDatabase,
    playerNation: String,
    playerRegion: String,
    playerClubId: Int,
    onBackClick: () -> Unit,
    onSwimmerClick: (Swimmer) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    var selectedScope by remember { mutableStateOf("Mondo") }
    var isShortCourse by remember { mutableStateOf(false) }
    var isRelayMode by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(true) }
    var individualRecords by remember { mutableStateOf<List<RaceRecord>>(emptyList()) }
    var relayRecords by remember { mutableStateOf<List<RelayRecord>>(emptyList()) }
    var fullSwimmersMap by remember { mutableStateOf<Map<Int, Swimmer>>(emptyMap()) }
    var clubsMap by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }

    fun fetchRecords() {
        coroutineScope.launch(Dispatchers.IO) {
            isLoading = true

            if (!isRelayMode) {
                val results = when (selectedScope) {
                    "Mondo" -> db.raceRecordDao().getWorldRecordsList(isShortCourse)
                    "Nazione" -> db.raceRecordDao().getNationalRecordsList(isShortCourse, playerNation)
                    "Regione" -> db.raceRecordDao().getRegionalRecordsList(isShortCourse, playerRegion)
                    else -> emptyList()
                }

                val swimmerIds = results.map { it.swimmerId }.distinct()
                val swimmers = db.swimmerDao().getAllSwimmers().filter { it.id in swimmerIds }
                val allClubs = db.clubDao().getAllClubs().associateBy { it.id }

                fullSwimmersMap = swimmers.associateBy { it.id }
                clubsMap = swimmers.associateBy({ it.id }, {
                    val clubName = allClubs[it.clubId]?.name ?: "UNK"
                    if (it.clubId == playerClubId) "⭐ $clubName" else clubName
                })

                withContext(Dispatchers.Main) {
                    individualRecords = results.sortedBy { it.eventId }
                    relayRecords = emptyList()
                    isLoading = false
                }
            } else {
                val results = when (selectedScope) {
                    "Mondo" -> db.relayRecordDao().getWorldRecordsList(isShortCourse)
                    "Nazione" -> db.relayRecordDao().getNationalRecordsList(isShortCourse, playerNation)
                    "Regione" -> db.relayRecordDao().getRegionalRecordsList(isShortCourse, playerRegion)
                    else -> emptyList()
                }

                val swimmerIds = results.flatMap { listOf(it.swimmer1Id, it.swimmer2Id, it.swimmer3Id, it.swimmer4Id) }.distinct()
                val swimmers = db.swimmerDao().getAllSwimmers().filter { it.id in swimmerIds }
                fullSwimmersMap = swimmers.associateBy { it.id }

                withContext(Dispatchers.Main) {
                    relayRecords = results.sortedBy { it.eventId }
                    individualRecords = emptyList()
                    isLoading = false
                }
            }
        }
    }

    LaunchedEffect(selectedScope, isShortCourse, isRelayMode) {
        fetchRecords()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Albo dei Record", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, "Indietro") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFD32F2F),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF5F5F5))
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        FilterChip(
                            selected = !isRelayMode,
                            onClick = { isRelayMode = false },
                            label = { Text("Individuali") }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        FilterChip(
                            selected = isRelayMode,
                            onClick = { isRelayMode = true },
                            label = { Text("Staffette") }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(selected = selectedScope == "Mondo", onClick = { selectedScope = "Mondo" }, label = { Text("🌍 Mondo") })
                        FilterChip(selected = selectedScope == "Nazione", onClick = { selectedScope = "Nazione" }, label = { Text("🇮🇹 Nazione") })
                        FilterChip(selected = selectedScope == "Regione", onClick = { selectedScope = "Regione" }, label = { Text("📍 Regione") })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Vasca: ", fontSize = 13.sp, color = Color.Gray)
                        Text(
                            text = if (isShortCourse) "CORTA (25m)" else "LUNGA (50m)",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = Color(0xFFD32F2F),
                            modifier = Modifier.clickable { isShortCourse = !isShortCourse }
                        )
                    }
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFFD32F2F))
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (!isRelayMode) {
                        items(individualRecords) { record ->
                            val swimmer = fullSwimmersMap[record.swimmerId]
                            val clubInfo = clubsMap[record.swimmerId] ?: ""
                            val eventName = EventDictionary.getEventById(record.eventId)?.getDisplayName() ?: record.eventId

                            IndividualRecordItem(
                                eventName = eventName,
                                swimmerName = "${swimmer?.lastName?.uppercase()} ${swimmer?.firstName}",
                                subText = "${swimmer?.nationality} • $clubInfo",
                                time = RaceEngine.formatTime(record.timeMs),
                                year = record.year,
                                isPlayerHighlight = clubInfo.contains("⭐"),
                                onClick = { swimmer?.let { onSwimmerClick(it) } }
                            )
                        }
                    } else {
                        items(relayRecords) { record ->
                            RelayRecordAccordion(
                                record = record,
                                swimmersMap = fullSwimmersMap,
                                isPlayerHighlight = record.teamId == playerClubId,
                                onSwimmerClick = onSwimmerClick
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IndividualRecordItem(
    eventName: String,
    swimmerName: String,
    subText: String,
    time: String,
    year: Int,
    isPlayerHighlight: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = eventName,
                modifier = Modifier.width(90.dp),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp,
                color = Color.DarkGray
            )

            Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                Text(
                    text = swimmerName,
                    fontWeight = if (isPlayerHighlight) FontWeight.ExtraBold else FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subText,
                    fontSize = 11.sp,
                    color = if (isPlayerHighlight) Color(0xFFF57C00) else Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(text = time, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color(0xFFD32F2F))
                Text(text = "Anno $year", fontSize = 10.sp, color = Color.LightGray)
            }
        }
    }
}

@Composable
fun RelayRecordAccordion(
    record: RelayRecord,
    swimmersMap: Map<Int, Swimmer>,
    isPlayerHighlight: Boolean,
    onSwimmerClick: (Swimmer) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val rotationState by animateFloatAsState(targetValue = if (expanded) 180f else 0f)

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // HEADER (Collapsed State)
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = EventDictionary.getEventById(record.eventId)?.getDisplayName() ?: record.eventId,
                    modifier = Modifier.width(90.dp),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    color = Color.DarkGray
                )

                Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                    Text(
                        text = if (isPlayerHighlight) "⭐ ${record.teamName}" else record.teamName,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = if (isPlayerHighlight) Color(0xFFF57C00) else Color.Black
                    )
                    Text(
                        text = if (expanded) "Clicca per chiudere" else "Clicca per i dettagli",
                        fontSize = 10.sp,
                        color = Color.LightGray
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(text = RaceEngine.formatTime(record.timeMs), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color(0xFFD32F2F))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Anno ${record.year}", fontSize = 10.sp, color = Color.LightGray)
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp).rotate(rotationState),
                            tint = Color.Gray
                        )
                    }
                }
            }

            // EXPANDED DETAILS (The Quartetto)
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFAFAFA))
                        .padding(bottom = 12.dp, start = 12.dp, end = 12.dp)
                ) {
                    HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp), thickness = 0.5.dp)
                    
                    Text("COMPOSIZIONE QUARTETTO E FRAZIONI:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(bottom = 4.dp))

                    val legs = listOf(
                        Triple(record.swimmer1Id, record.swimmer1SplitMs, "1ª"),
                        Triple(record.swimmer2Id, record.swimmer2SplitMs, "2ª"),
                        Triple(record.swimmer3Id, record.swimmer3SplitMs, "3ª"),
                        Triple(record.swimmer4Id, record.swimmer4SplitMs, "4ª")
                    )

                    legs.forEach { (sid, split, label) ->
                        val swimmer = swimmersMap[sid]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { swimmer?.let { onSwimmerClick(it) } },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = label, modifier = Modifier.width(30.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                            Text(
                                text = "${swimmer?.lastName?.uppercase() ?: "???"} ${swimmer?.firstName ?: ""}",
                                modifier = Modifier.weight(1f),
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = RaceEngine.formatTime(split),
                                modifier = Modifier.width(70.dp),
                                textAlign = TextAlign.End,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF1976D2) // Blue for splits
                            )
                        }
                    }
                }
            }
        }
    }
}
