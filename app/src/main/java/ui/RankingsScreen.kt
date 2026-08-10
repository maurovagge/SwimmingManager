package ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.swimmingmanager.data.AppDatabase
import com.example.swimmingmanager.data.RaceResult
import com.example.swimmingmanager.data.Swimmer
import com.example.swimmingmanager.engine.RaceEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankingsScreen(
    db: AppDatabase,
    currentYear: Int,
    playerNation: String,
    playerRegion: String,
    playerClubId: Int,
    onBackClick: () -> Unit,
    onSwimmerClick: (Swimmer) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    // --- FILTERS STATE ---
    var selectedScope by remember { mutableStateOf("Mondo") }
    var selectedGender by remember { mutableStateOf("M") }
    var selectedStroke by remember { mutableStateOf("FREE") }
    var selectedDistance by remember { mutableStateOf("100") }
    var isShortCourse by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("Assoluti") }

    // --- DATA STATE ---
    var isLoading by remember { mutableStateOf(false) }
    var rankings by remember { mutableStateOf<List<RaceResult>>(emptyList()) }
    var swimmersMap by remember { mutableStateOf<Map<Int, Swimmer>>(emptyMap()) }
    var clubsInfoMap by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }

    fun fetchRankings() {
        coroutineScope.launch(Dispatchers.IO) {
            isLoading = true

            val (minAge, maxAge) = when (selectedCategory) {
                "Ragazzi" -> Pair(0, 15)
                "Juniores" -> Pair(16, 17)
                "Cadetti" -> Pair(18, 19)
                else -> Pair(0, 99)
            }

            val targetNation = if (selectedScope == "Mondo") null else playerNation
            val targetRegion = if (selectedScope == "Regione") playerRegion else null
            val eventId = "${selectedDistance}_${selectedStroke}_${selectedGender}"

            val results = db.raceResultDao().getTop100Ranking(
                eventId = eventId,
                year = currentYear,
                isShortCourse = isShortCourse,
                minAge = minAge,
                maxAge = maxAge,
                nationCode = targetNation,
                regionName = targetRegion
            )

            val activeSwimmerIds = results.map { it.swimmerId }.distinct()
            if (activeSwimmerIds.isNotEmpty()) {
                val swimmers = db.swimmerDao().getSwimmersByIds(activeSwimmerIds)
                val allClubs = db.clubDao().getAllClubs().associateBy { it.id }

                val sMap = swimmers.associateBy { it.id }
                val cMap = swimmers.associateBy({ it.id }, {
                    val clubName = allClubs[it.clubId]?.name ?: "UNK"
                    if (it.clubId == playerClubId) "⭐ $clubName" else clubName
                })
                
                withContext(Dispatchers.Main) {
                    swimmersMap = sMap
                    clubsInfoMap = cMap
                }
            } else {
                withContext(Dispatchers.Main) {
                    swimmersMap = emptyMap()
                    clubsInfoMap = emptyMap()
                }
            }

            withContext(Dispatchers.Main) {
                rankings = results
                isLoading = false
            }
        }
    }

    LaunchedEffect(selectedScope, selectedGender, selectedStroke, selectedDistance, isShortCourse, selectedCategory) {
        fetchRankings()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ranking Nazionali e Mondiali") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) { 
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro") 
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
            Card(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterDropdown(
                            modifier = Modifier.weight(1f),
                            label = "Livello",
                            options = listOf("Mondo", "Nazione", "Regione"),
                            selectedOption = selectedScope,
                            onOptionSelected = { selectedScope = it }
                        )
                        FilterDropdown(
                            modifier = Modifier.weight(1f),
                            label = "Categoria",
                            options = listOf("Assoluti", "Cadetti", "Juniores", "Ragazzi"),
                            selectedOption = selectedCategory,
                            onOptionSelected = { selectedCategory = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterDropdown(
                            modifier = Modifier.weight(1f),
                            label = "Stile",
                            options = listOf("FREE" to "Stile L.", "BACK" to "Dorso", "BREAST" to "Rana", "FLY" to "Farfalla", "MEDLEY" to "Misti"),
                            selectedOption = selectedStroke,
                            onOptionSelected = { selectedStroke = it },
                            isKeyValue = true
                        )
                        FilterDropdown(
                            modifier = Modifier.weight(1f),
                            label = "Distanza",
                            options = listOf("50", "100", "200", "400", "800", "1500"),
                            selectedOption = selectedDistance,
                            onOptionSelected = { selectedDistance = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row {
                            FilterChip(selected = selectedGender == "M", onClick = { selectedGender = "M" }, label = { Text("Maschi") })
                            Spacer(modifier = Modifier.width(8.dp))
                            FilterChip(selected = selectedGender == "F", onClick = { selectedGender = "F" }, label = { Text("Femmine") })
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Vasca: ", fontSize = 14.sp)
                            Text(
                                text = if (isShortCourse) "Corta (25m)" else "Lunga (50m)",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1565C0),
                                modifier = Modifier.clickable { isShortCourse = !isShortCourse }
                            )
                        }
                    }
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else if (rankings.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nessun tempo registrato.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(rankings) { index, result ->
                        val rank = index + 1
                        val swimmer = swimmersMap[result.swimmerId]
                        val clubInfo = clubsInfoMap[result.swimmerId] ?: ""

                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                .clickable { swimmer?.let { onSwimmerClick(it) } },
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = when (rank) { 1 -> "🥇"; 2 -> "🥈"; 3 -> "🥉"; else -> "$rank°" },
                                    modifier = Modifier.width(36.dp), fontWeight = FontWeight.Bold, fontSize = 16.sp
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${swimmer?.lastName?.uppercase()} ${swimmer?.firstName}",
                                        fontWeight = if (clubInfo.contains("⭐")) FontWeight.ExtraBold else FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "${swimmer?.nationality} • Anno ${currentYear - (swimmer?.age ?: 0)} | $clubInfo",
                                        fontSize = 12.sp,
                                        color = if (clubInfo.contains("⭐")) Color(0xFFF57C00) else Color.Gray
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(text = RaceEngine.formatTime(result.timeMs), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color(0xFF1565C0))
                                    Text(text = "Sett. ${result.week}", fontSize = 11.sp, color = Color.LightGray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDropdown(
    modifier: Modifier = Modifier,
    label: String,
    options: List<Any>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    isKeyValue: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    val displayText = if (isKeyValue) {
        @Suppress("UNCHECKED_CAST")
        (options as List<Pair<String, String>>).find { it.first == selectedOption }?.second ?: selectedOption
    } else {
        selectedOption
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                val (key, labelText) = if (isKeyValue) {
                    @Suppress("UNCHECKED_CAST")
                    val pair = option as Pair<String, String>
                    pair.first to pair.second
                } else {
                    option.toString() to option.toString()
                }
                DropdownMenuItem(
                    text = { Text(labelText) },
                    onClick = { onOptionSelected(key); expanded = false }
                )
            }
        }
    }
}
