package ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.swimmingmanager.data.AppDatabase
import com.example.swimmingmanager.data.Competition
import com.example.swimmingmanager.data.RaceRecord
import com.example.swimmingmanager.data.RaceResult
import com.example.swimmingmanager.data.RelayResult
import com.example.swimmingmanager.data.Swimmer
import com.example.swimmingmanager.engine.EventDictionary
import com.example.swimmingmanager.engine.RaceEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.collections.take

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompetitionResultsScreen(
    competition: Competition,
    db: AppDatabase,
    playerClubId: Int,
    onBackClick: () -> Unit,
    onSwimmerClick: (Swimmer) -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Risultati", "Record Manifestazione")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Risultati: ${competition.name}") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1565C0),
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
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.White,
                contentColor = Color(0xFF1565C0)
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            when (selectedTabIndex) {
                0 -> ResultsTab(
                    competition = competition,
                    db = db,
                    playerClubId = playerClubId,
                    onSwimmerClick = onSwimmerClick
                )
                1 -> HistoricalRecordsTab(
                    competition = competition,
                    db = db
                )
            }
        }
    }
}

@Composable
fun ResultsTab(
    competition: Competition,
    db: AppDatabase,
    playerClubId: Int,
    onSwimmerClick: (Swimmer) -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }

    var availableYears by remember { mutableStateOf(emptyList<Int>()) }
    var selectedYear by remember { mutableIntStateOf(competition.year) }

    var eventResultsMap by remember { mutableStateOf<Map<String, List<RaceResult>>>(emptyMap()) }
    var relayResultsMap by remember { mutableStateOf<Map<String, List<RelayResult>>>(emptyMap()) }

    var swimmerNamesMap by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
    var swimmerClubsMap by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
    var fullSwimmersMap by remember { mutableStateOf<Map<Int, Swimmer>>(emptyMap()) }

    var worldRecordsMap by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var nationalRecordsMap by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var meetRecordsMap by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var playerNation by remember { mutableStateOf("") }

    LaunchedEffect(competition.name) {
        withContext(Dispatchers.IO) {
            val years = db.raceResultDao().getAvailableYearsForCompetition(competition.name)
            val swimmers = db.swimmerDao().getAllSwimmers()
            val clubs = db.clubDao().getAllClubs()
            val clubMap = clubs.associateBy { it.id }
            
            val playerClub = clubMap[playerClubId]
            val nation = playerClub?.country ?: ""

            // Caricamento Record Mondiali
            val wrs: Map<String, Int> = db.raceRecordDao().getAllRecordsByType("WORLD", "ALL")
                .filter { it.isShortCourse == competition.isShortCourse }
                .associate { it.eventId to it.timeMs }
            
            // Caricamento Record Nazionali
            val nrs: Map<String, Int> = if (nation.isNotEmpty()) {
                db.raceRecordDao().getAllRecordsByType("NATIONAL", nation)
                    .filter { it.isShortCourse == competition.isShortCourse }
                    .associate { it.eventId to it.timeMs }
            } else emptyMap()

            // THE FIX: Caricamento Record Manifestazione dalla tabella dedicata
            val mrs: Map<String, Int> = db.raceRecordDao().getMeetRecords(competition.name)
                .associate { it.eventId to it.timeMs }

            withContext(Dispatchers.Main) {
                availableYears = years
                selectedYear = if (years.contains(competition.year)) competition.year else years.firstOrNull() ?: competition.year
                
                fullSwimmersMap = swimmers.associateBy { it.id }
                swimmerNamesMap = swimmers.associateBy({ it.id }, { "${it.lastName?.uppercase()} ${it.firstName}" })
                swimmerClubsMap = swimmers.associateBy({ it.id }, { swimmer ->
                    val clubName = clubMap[swimmer.clubId]?.name ?: "UNK"
                    if (swimmer.clubId == playerClubId) "⭐ $clubName" else clubName
                })
                
                worldRecordsMap = wrs
                nationalRecordsMap = nrs
                meetRecordsMap = mrs
                playerNation = nation
            }
        }
    }

    LaunchedEffect(selectedYear) {
        isLoading = true
        withContext(Dispatchers.IO) {
            val allResults = db.raceResultDao().getHistoricalResults(competition.name, selectedYear)
            val finalResults = allResults.filter { it.roundType == "FINAL" || it.roundType == "TIMED_FINAL" }
            val newEventMap = finalResults.groupBy { it.eventId }.mapValues { it.value.sortedBy { r -> r.timeMs } }

            val allRelays = db.raceResultDao().getHistoricalRelays(competition.name, selectedYear)
            val newRelayMap = allRelays.groupBy { it.eventId }.mapValues { it.value.sortedBy { r -> r.totalTimeMs } }

            withContext(Dispatchers.Main) {
                eventResultsMap = newEventMap
                relayResultsMap = newRelayMap
                isLoading = false
            }
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            if (availableYears.size > 1) {
                val currentIndex = availableYears.indexOf(selectedYear)
                val hasOlder = currentIndex < availableYears.size - 1
                val hasNewer = currentIndex > 0

                Row(
                    modifier = Modifier.fillMaxWidth().background(Color.White).padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { if (hasOlder) selectedYear = availableYears[currentIndex + 1] }, enabled = hasOlder) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = if (hasOlder) Color(0xFF1565C0) else Color.LightGray)
                    }
                    Text(text = "Edizione $selectedYear", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0), modifier = Modifier.padding(horizontal = 24.dp))
                    IconButton(onClick = { if (hasNewer) selectedYear = availableYears[currentIndex - 1] }, enabled = hasNewer) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = if (hasNewer) Color(0xFF1565C0) else Color.LightGray)
                    }
                }
            }

            LazyColumn(contentPadding = PaddingValues(16.dp), modifier = Modifier.weight(1f)) {
                val individualEvents = eventResultsMap.keys.sorted()
                items(individualEvents) { eventId ->
                    EventResultsCard(
                        eventName = EventDictionary.getEventById(eventId)?.getDisplayName() ?: eventId,
                        eventId = eventId,
                        results = eventResultsMap[eventId] ?: emptyList(),
                        swimmerNamesMap = swimmerNamesMap,
                        swimmerClubsMap = swimmerClubsMap,
                        fullSwimmersMap = fullSwimmersMap,
                        worldRecordsMap = worldRecordsMap,
                        nationalRecordsMap = nationalRecordsMap,
                        meetRecordsMap = meetRecordsMap,
                        playerNation = playerNation,
                        isNationalTeamEvent = competition.isNationalTeamEvent,
                        onSwimmerClick = onSwimmerClick
                    )
                }

                val relayEvents = relayResultsMap.keys.sorted()
                if (relayEvents.isNotEmpty()) {
                    item { Text("Staffette", fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(vertical = 12.dp)) }
                    items(relayEvents) { eventId ->
                        RelayResultsCard(
                            eventName = eventId.replace("_", " "), 
                            results = relayResultsMap[eventId] ?: emptyList(), 
                            swimmerNamesMap = swimmerNamesMap,
                            isNationalTeamEvent = competition.isNationalTeamEvent
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EventResultsCard(
    eventName: String,
    eventId: String,
    results: List<RaceResult>,
    swimmerNamesMap: Map<Int, String>,
    swimmerClubsMap: Map<Int, String>,
    fullSwimmersMap: Map<Int, Swimmer>,
    worldRecordsMap: Map<String, Int>,
    nationalRecordsMap: Map<String, Int>,
    meetRecordsMap: Map<String, Int>,
    playerNation: String,
    isNationalTeamEvent: Boolean,
    onSwimmerClick: (Swimmer) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    val currentWR = worldRecordsMap[eventId] ?: Int.MAX_VALUE
    val currentNR = nationalRecordsMap[eventId] ?: Int.MAX_VALUE
    val currentCR = meetRecordsMap[eventId] ?: Int.MAX_VALUE

    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable { expanded = !expanded },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = eventName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
                Icon(imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color.Gray)
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    results.take(8).forEachIndexed { index, result ->
                        val rank = index + 1
                        val swimmer = fullSwimmersMap[result.swimmerId]
                        val swimmerName = swimmerNamesMap[result.swimmerId] ?: "Unknown"
                        
                        val clubInfo = if (isNationalTeamEvent) {
                            swimmer?.nationality ?: ""
                        } else {
                            swimmerClubsMap[result.swimmerId] ?: ""
                        }
                        
                        val brokeWR = result.timeMs <= currentWR
                        val brokeNR = result.timeMs <= currentNR && swimmer?.nationality == playerNation
                        val brokeCR = result.timeMs <= currentCR

                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { swimmer?.let { onSwimmerClick(it) } }.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val rankText = when (rank) { 1 -> "🥇"; 2 -> "🥈"; 3 -> "🥉"; else -> "$rank°" }
                            Text(text = rankText, modifier = Modifier.width(32.dp), fontWeight = if (rank <= 3) FontWeight.Bold else FontWeight.Normal)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = swimmerName, fontWeight = if (clubInfo.contains("⭐")) FontWeight.ExtraBold else FontWeight.Medium, fontSize = 14.sp)
                                Text(text = clubInfo, fontSize = 11.sp, color = if (clubInfo.contains("⭐")) Color(0xFFF57C00) else Color.Gray)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = RaceEngine.formatTime(result.timeMs), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Row {
                                    if (brokeWR) ResultRecordTag("WR", Color(0xFFD50000))
                                    if (brokeNR) ResultRecordTag("NR", Color(0xFF2962FF))
                                    if (brokeCR) ResultRecordTag("CR", Color(0xFFE65100))
                                }
                            }
                        }
                        if (index < minOf(results.size, 8) - 1) HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                    }
                }
            }
        }
    }
}

@Composable
fun ResultRecordTag(label: String, color: Color) {
    Text(
        text = "$label! ",
        fontSize = 9.sp,
        fontWeight = FontWeight.Black,
        color = color,
        modifier = Modifier.padding(start = 2.dp)
    )
}

@Composable
fun RelayResultsCard(
    eventName: String,
    results: List<RelayResult>,
    swimmerNamesMap: Map<Int, String>,
    isNationalTeamEvent: Boolean
) {
    var expanded by remember { mutableStateOf(false) }
    val isMedley = eventName.contains("MEDLEY", ignoreCase = true)

    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable { expanded = !expanded },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = eventName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
                Icon(imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null)
            }

            Spacer(modifier = Modifier.height(8.dp))

            results.forEachIndexed { index, result ->
                val rank = index + 1
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val medal = when(rank) { 1 -> "🥇"; 2 -> "🥈"; 3 -> "🥉"; else -> "$rank°" }
                        Text(text = medal, modifier = Modifier.width(32.dp))
                        Text(text = result.teamName, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text(text = RaceEngine.formatTime(result.totalTimeMs), fontWeight = FontWeight.ExtraBold)
                    }

                    AnimatedVisibility(visible = expanded) {
                        Column(modifier = Modifier.padding(start = 32.dp, top = 4.dp, bottom = 8.dp)) {
                            val frazionisti = listOf(
                                Triple(result.swimmer1Id, result.swimmer1SplitMs, if(isMedley) "Dorso" else "1ª Fraz."),
                                Triple(result.swimmer2Id, result.swimmer2SplitMs, if(isMedley) "Rana" else "2ª Fraz."),
                                Triple(result.swimmer3Id, result.swimmer3SplitMs, if(isMedley) "Delfino" else "3ª Fraz."),
                                Triple(result.swimmer4Id, result.swimmer4SplitMs, if(isMedley) "Stile" else "4ª Fraz.")
                            )
                            frazionisti.forEach { (id, split, label) ->
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                                    Text("$label: ", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.width(60.dp))
                                    Text(swimmerNamesMap[id] ?: "Sconosciuto", fontSize = 12.sp, modifier = Modifier.weight(1f))
                                    Text(RaceEngine.formatTime(split), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                            HorizontalDivider(modifier = Modifier.padding(top = 8.dp), color = Color.LightGray.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoricalRecordsTab(competition: Competition, db: AppDatabase) {
    var isLoading by remember { mutableStateOf(true) }
    var recordsList by remember { mutableStateOf<List<RaceRecord>>(emptyList()) }
    var swimmersMap by remember { mutableStateOf<Map<Int, Swimmer>>(emptyMap()) }

    LaunchedEffect(competition.name) {
        withContext(Dispatchers.IO) {
            // THE FIX: Leggiamo dalla tabella dei record permanenti
            val meetRecords = db.raceRecordDao().getMeetRecords(competition.name)
            val swimmerIds = meetRecords.map { it.swimmerId }.distinct()
            if (swimmerIds.isNotEmpty()) {
                val swimmers = db.swimmerDao().getAllSwimmers().filter { it.id in swimmerIds }
                swimmersMap = swimmers.associateBy { it.id }
            }
            withContext(Dispatchers.Main) {
                recordsList = meetRecords.sortedBy { it.eventId }
                isLoading = false
            }
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    } else if (recordsList.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Nessun record registrato.", color = Color.Gray) }
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), modifier = Modifier.fillMaxSize()) {
            items(recordsList) { record ->
                val swimmer = swimmersMap[record.swimmerId]
                val eventName = EventDictionary.getEventById(record.eventId)?.getDisplayName() ?: record.eventId
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(text = eventName, modifier = Modifier.width(80.dp), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                            Text(text = "${swimmer?.lastName?.uppercase()} ${swimmer?.firstName}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Anno ${record.year}", fontSize = 12.sp, color = Color.Gray)
                        }
                        Text(text = RaceEngine.formatTime(record.timeMs), fontWeight = FontWeight.ExtraBold, color = Color(0xFFD32F2F), fontSize = 16.sp)
                    }
                }
            }
        }
    }
}
