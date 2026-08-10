package ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.swimmingmanager.data.Competition
import com.example.swimmingmanager.data.Swimmer
import com.example.swimmingmanager.engine.SwimmingEvent
import androidx.activity.compose.BackHandler
import com.example.swimmingmanager.data.AppDatabase
import com.example.swimmingmanager.data.RaceRecord
import com.example.swimmingmanager.data.RaceRegistration
import com.example.swimmingmanager.data.RaceResult
import com.example.swimmingmanager.engine.EventDictionary
import com.example.swimmingmanager.engine.QualificationEngine
import com.example.swimmingmanager.engine.RaceEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CompetitionDetailScreen(
    competition: Competition,
    roster: List<Swimmer>,
    availableEvents: List<SwimmingEvent>,
    db: AppDatabase,
    currentYear: Int,
    clubNation: String,
    onBackClick: () -> Unit
) {
    BackHandler(onBack = onBackClick)

    val sortedRoster = roster.sortedBy { it.lastName?.lowercase() }
    val hasRelays = (competition.tier != "REGIONAL") || (competition.week % 22 == 0)

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = if (hasRelays) {
        listOf("Gare Singole", "Staffette", "Record & Info")
    } else {
        listOf("Iscrizioni", "Record & Info")
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FA))
    ) {
        Surface(color = Color(0xFF1976D2), contentColor = Color.White, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "◁ Indietro al Calendario", fontSize = 14.sp, modifier = Modifier.clickable { onBackClick() }.padding(bottom = 8.dp))
                Text(text = competition.name, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                Text(text = "Settimana ${competition.week} • Tier: ${competition.tier}", fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
            }
        }

        TabRow(selectedTabIndex = selectedTabIndex, containerColor = Color.White, contentColor = Color(0xFF1976D2)) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title, fontWeight = FontWeight.Bold) }
                )
            }
        }

        if (hasRelays) {
            when (selectedTabIndex) {
                0 -> RegistrationsTab(sortedRoster, availableEvents, competition, db, currentYear, clubNation)
                1 -> RelaysRegistrationTab(sortedRoster, availableEvents, competition, db)
                2 -> MeetInfoRecordsTab(competition, db)
            }
        } else {
            when (selectedTabIndex) {
                0 -> RegistrationsTab(sortedRoster, availableEvents, competition, db, currentYear, clubNation)
                1 -> MeetInfoRecordsTab(competition, db)
            }
        }
    }
}

@Composable
fun RegistrationsTab(
    roster: List<Swimmer>,
    availableEvents: List<SwimmingEvent>,
    competition: Competition,
    db: AppDatabase,
    currentYear: Int,
    clubNation: String
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        if (competition.tier == "INTERNATIONAL") {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Le convocazioni internazionali sono automatiche. Qui vedrai solo i tuoi atleti qualificati.",
                        modifier = Modifier.padding(16.dp),
                        color = Color(0xFFE65100),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        items(roster) { swimmer ->
            SwimmerRegistrationCard(
                swimmer = swimmer,
                availableEvents = availableEvents,
                competition = competition,
                currentYear = currentYear,
                db = db,
                clubNation = clubNation
            )
        }
    }
}

@Composable
fun SwimmerRegistrationCard(
    swimmer: Swimmer,
    availableEvents: List<SwimmingEvent>,
    competition: Competition,
    db: AppDatabase,
    currentYear: Int,
    clubNation: String
) {
    var isExpanded by remember { mutableStateOf(false) }
    var qualifiedEvents by remember { mutableStateOf<List<SwimmingEvent>>(emptyList()) }
    var isLoadingEligibility by remember { mutableStateOf(false) }
    var registeredEventCodes by remember { mutableStateOf<Set<String>>(emptySet()) }
    val coroutineScope = rememberCoroutineScope()

    val maxAllowedRaces = if (competition.tier == "REGIONAL") 4 else Int.MAX_VALUE
    val limitDisplay = if (maxAllowedRaces == Int.MAX_VALUE) "∞" else maxAllowedRaces.toString()

    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            val allSwimmerRegs = withContext(Dispatchers.IO) {
                db.raceRegistrationDao().getRegistrationsForSwimmer(swimmer.id)
            }
            registeredEventCodes = allSwimmerRegs
                .filter { it.competitionId == competition.id }
                .map { it.eventCode }
                .toSet()

            if (qualifiedEvents.isEmpty()) {
                isLoadingEligibility = true
                val validEvents = mutableListOf<SwimmingEvent>()
                val swimmerGenderStr = if (swimmer.gender == "M") "_M" else "_F"

                for (event in availableEvents) {
                    if (event.id.startsWith("4x")) continue
                    if (event.id.endsWith(swimmerGenderStr)) {
                        val isEligible = QualificationEngine.checkEligibility(
                            db = db, swimmer = swimmer, event = event,
                            competition = competition, nationCode = clubNation, currentYear = currentYear
                        )
                        if (isEligible) validEvents.add(event)
                    }
                }
                qualifiedEvents = validEvents
                isLoadingEligibility = false
            }
        }
    }

    Card(
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { isExpanded = !isExpanded }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${swimmer.firstName} ${swimmer.lastName}",
                            fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF212121)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        val styleColor = when(swimmer.mainStyle) {
                            "SL" -> Color(0xFF1976D2)
                            "DO" -> Color(0xFF8E24AA)
                            "RA" -> Color(0xFFF57C00)
                            "FA" -> Color(0xFF9C27B0)
                            "MX" -> Color(0xFFFBC02D)
                            else -> Color.Gray
                        }
                        SmallInfoBadge(text = swimmer.mainStyle, color = styleColor)
                        
                        Spacer(modifier = Modifier.width(4.dp))

                        val isSprinter = (swimmer.sprintPower + swimmer.speed) >= (swimmer.speed + swimmer.endurance)
                        val typeLabel = if (isSprinter) "VEL" else "FON"
                        val typeColor = if (isSprinter) Color(0xFFD32F2F) else Color(0xFF0097A7)
                        SmallInfoBadge(text = typeLabel, color = typeColor)
                    }
                    Text(
                        text = "Età: ${swimmer.age} • Iscrizioni: ${registeredEventCodes.size}/$limitDisplay",
                        fontSize = 14.sp, color = Color.Gray
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.ArrowDropDown,
                    contentDescription = "Expand", tint = Color.Gray
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.fillMaxWidth().background(Color(0xFFF1F8E9)).padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    if (isLoadingEligibility) {
                        Text("Verifica tempi limite in corso...", color = Color.Gray, fontSize = 14.sp)
                    } else if (qualifiedEvents.isEmpty()) {
                        Text("Non qualificato per nessuna gara in questa competizione.", color = Color(0xFFD32F2F), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    } else {
                        qualifiedEvents.forEach { event ->
                            val isRegistered = registeredEventCodes.contains(event.id)

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = event.getDisplayName(), fontWeight = FontWeight.Medium)

                                Button(
                                    onClick = {
                                        coroutineScope.launch(Dispatchers.IO) {
                                            if (isRegistered) {
                                                db.raceRegistrationDao().unregisterSwimmer(
                                                    swimmerId = swimmer.id, compId = competition.id, event = event.id
                                                )
                                                registeredEventCodes = registeredEventCodes - event.id
                                            } else {
                                                if (registeredEventCodes.size < maxAllowedRaces) {
                                                    db.raceRegistrationDao().registerSwimmer(
                                                        RaceRegistration(
                                                            swimmerId = swimmer.id, competitionId = competition.id,
                                                            eventCode = event.id, week = competition.week
                                                        )
                                                    )
                                                    registeredEventCodes = registeredEventCodes + event.id
                                                }
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isRegistered) Color(0xFFD32F2F)
                                        else if (registeredEventCodes.size >= maxAllowedRaces) Color.LightGray
                                        else Color(0xFF4CAF50)
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text(text = if (isRegistered) "Ritira" else "Iscrivi", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SmallInfoBadge(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun MeetInfoRecordsTab(competition: Competition, db: AppDatabase) {
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
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (recordsList.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Nessun record registrato per questa manifestazione.", color = Color.Gray)
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(recordsList) { record ->
                val swimmer = swimmersMap[record.swimmerId]
                val eventName = EventDictionary.getEventById(record.eventId)?.getDisplayName() ?: record.eventId

                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = eventName,
                            modifier = Modifier.width(80.dp),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                            Text(
                                text = "${swimmer?.lastName?.uppercase()} ${swimmer?.firstName}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text("Anno ${record.year}", fontSize = 12.sp, color = Color.Gray)
                        }
                        Text(
                            text = RaceEngine.formatTime(record.timeMs),
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFD32F2F),
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RelaysRegistrationTab(
    roster: List<Swimmer>,
    availableEvents: List<SwimmingEvent>,
    competition: Competition,
    db: AppDatabase
) {
    val relayEvents = availableEvents.filter { it.id.startsWith("4x") }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(relayEvents) { event ->
            RelayTeamBuilderCard(
                event = event,
                roster = roster,
                competition = competition,
                db = db
            )
        }
    }
}

@Composable
fun RelayTeamBuilderCard(
    event: SwimmingEvent,
    roster: List<Swimmer>,
    competition: Competition,
    db: AppDatabase
) {
    val coroutineScope = rememberCoroutineScope()
    val targetGender = if (event.id.endsWith("_M")) "M" else "F"
    val eligibleSwimmers = roster.filter { swimmer ->
        swimmer.gender == targetGender &&
                swimmer.age >= competition.minAge &&
                swimmer.age <= competition.maxAge
    }
    val isMedley = event.id.contains("MEDLEY")

    var selectedSwimmers by remember { mutableStateOf(listOf<Swimmer?>(null, null, null, null)) }
    var isSaved by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(event.id) {
        val existingRegs = withContext(Dispatchers.IO) {
            db.raceRegistrationDao().getRegistrationsForCompetition(competition.id)
                .filter { it.eventCode == event.id }
        }

        if (existingRegs.isNotEmpty()) {
            val savedTeam = mutableListOf<Swimmer?>()
            existingRegs.take(4).forEach { reg ->
                savedTeam.add(eligibleSwimmers.find { it.id == reg.swimmerId })
            }
            while (savedTeam.size < 4) savedTeam.add(null)
            selectedSwimmers = savedTeam
            isSaved = savedTeam.all { it != null }
        }
        isLoading = false
    }

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = event.getDisplayName(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
                if (isSaved) {
                    Text("✅ Iscritti", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (eligibleSwimmers.size < 4) {
                Text("Non hai abbastanza atleti per formare la squadra.", color = Color.Red, fontSize = 14.sp)
            } else if (!isLoading) {
                for (i in 0..3) {
                    val slotLabel = if (isMedley) {
                        when (i) { 0 -> "Dorso"; 1 -> "Rana"; 2 -> "Delfino"; else -> "Stile Libero" }
                    } else {
                        "Frazione ${i + 1}"
                    }

                    RelaySlotDropdown(
                        label = slotLabel,
                        selectedSwimmer = selectedSwimmers[i],
                        availableSwimmers = eligibleSwimmers,
                        alreadySelected = selectedSwimmers.filterNotNull(),
                        onSwimmerSelected = { newSwimmer ->
                            val newList = selectedSwimmers.toMutableList()
                            newList[i] = newSwimmer
                            selectedSwimmers = newList
                            isSaved = false
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                val isTeamComplete = selectedSwimmers.all { it != null }
                Button(
                    onClick = {
                        if (isTeamComplete) {
                            coroutineScope.launch(Dispatchers.IO) {
                                val existing = db.raceRegistrationDao().getRegistrationsForCompetition(competition.id)
                                    .filter { it.eventCode == event.id }
                                existing.forEach { reg ->
                                    db.raceRegistrationDao().unregisterSwimmer(reg.swimmerId, competition.id, event.id)
                                }
                                selectedSwimmers.forEach { swimmer ->
                                    if (swimmer != null) {
                                        db.raceRegistrationDao().registerSwimmer(
                                            RaceRegistration(
                                                swimmerId = swimmer.id,
                                                competitionId = competition.id,
                                                eventCode = event.id,
                                                week = competition.week
                                            )
                                        )
                                    }
                                }
                                isSaved = true
                            }
                        }
                    },
                    enabled = isTeamComplete && !isSaved,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                ) {
                    Text(if (isSaved) "Formazione Salvata" else "Conferma Formazione")
                }
            }
        }
    }
}

@Composable
fun RelaySlotDropdown(
    label: String,
    selectedSwimmer: Swimmer?,
    availableSwimmers: List<Swimmer>,
    alreadySelected: List<Swimmer>,
    onSwimmerSelected: (Swimmer?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(text = label, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF5F5F5), RoundedCornerShape(4.dp))
                .clickable { expanded = true }
                .padding(12.dp)
        ) {
            Text(
                text = selectedSwimmer?.let { "${it.lastName?.uppercase()} ${it.firstName}" } ?: "Seleziona atleta...",
                color = if (selectedSwimmer != null) Color.Black else Color.Gray,
                fontSize = 14.sp
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            DropdownMenuItem(
                text = { Text("Nessuno") },
                onClick = { onSwimmerSelected(null); expanded = false }
            )

            availableSwimmers.forEach { swimmer ->
                val isAlreadyUsed = alreadySelected.contains(swimmer) && swimmer != selectedSwimmer
                DropdownMenuItem(
                    text = { Text("${swimmer.lastName?.uppercase()} ${swimmer.firstName}") },
                    onClick = {
                        if (!isAlreadyUsed) {
                            onSwimmerSelected(swimmer)
                            expanded = false
                        }
                    },
                    enabled = !isAlreadyUsed
                )
            }
        }
    }
}
