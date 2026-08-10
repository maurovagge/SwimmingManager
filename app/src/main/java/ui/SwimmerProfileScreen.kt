package ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.swimmingmanager.data.AppDatabase
import com.example.swimmingmanager.data.Medal
import com.example.swimmingmanager.data.Swimmer
import com.example.swimmingmanager.engine.EventDictionary
import com.example.swimmingmanager.engine.RaceEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

data class EventRecords(
    val eventName: String,
    val pbShort: Int?,
    val pbLong: Int?,
    val sbShort: Int?,
    val sbLong: Int?,
    val isWRShort: Boolean = false,
    val isNRShort: Boolean = false,
    val isWRLong: Boolean = false,
    val isNRLong: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwimmerProfileScreen(
    swimmer: Swimmer,
    db: AppDatabase,
    currentYear: Int,
    playerClubId: Int,
    onBackClick: () -> Unit,
    onSwimmerUpdate: (Swimmer) -> Unit,
    onExpelSwimmer: (Swimmer) -> Unit
) {
    var recordsList by remember { mutableStateOf<List<EventRecords>>(emptyList()) }
    var medalsList by remember { mutableStateOf<List<Medal>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showExpelDialog by remember { mutableStateOf(false) }

    LaunchedEffect(swimmer.id) {
        withContext(Dispatchers.IO) {
            val allPBs = db.personalBestDao().getAllPBsForSwimmer(swimmer.id)
            val allResults = db.raceResultDao().getResultsForSwimmer(swimmer.id)
            val allMedals = db.medalDao().getMedalsForSwimmer(swimmer.id)

            val wrShort = db.raceRecordDao().getWorldRecordsList(true)
            val wrLong = db.raceRecordDao().getWorldRecordsList(false)
            val nationality = swimmer.nationality ?: ""
            val nrShort = if (nationality.isNotEmpty()) db.raceRecordDao().getNationalRecordsList(true, nationality) else emptyList()
            val nrLong = if (nationality.isNotEmpty()) db.raceRecordDao().getNationalRecordsList(false, nationality) else emptyList()

            val processed = (allResults.map { it.eventId } + allPBs.map { it.eventId }).distinct().map { id ->
                val histPbS = allPBs.find { it.eventId == id && it.isShortCourse }?.timeMs
                val histPbL = allPBs.find { it.eventId == id && !it.isShortCourse }?.timeMs
                val currSbS = allResults.filter { it.year == currentYear && it.eventId == id && it.isShortCourse }.minByOrNull { it.timeMs }?.timeMs
                val currSbL = allResults.filter { it.year == currentYear && it.eventId == id && !it.isShortCourse }.minByOrNull { it.timeMs }?.timeMs

                // The effective PB is the best time ever, including the current season
                val effectivePbS = listOfNotNull(histPbS, currSbS).minOrNull()
                val effectivePbL = listOfNotNull(histPbL, currSbL).minOrNull()

                val wrS = wrShort.find { it.eventId == id }
                val wrL = wrLong.find { it.eventId == id }
                val nrS = nrShort.find { it.eventId == id }
                val nrL = nrLong.find { it.eventId == id }

                EventRecords(
                    eventName = EventDictionary.getEventById(id)?.getDisplayName() ?: id,
                    pbShort = effectivePbS,
                    pbLong = effectivePbL,
                    sbShort = currSbS,
                    sbLong = currSbL,
                    // Check if current effective PB is faster or equal to the record (handles both new record and current holder)
                    isWRShort = effectivePbS != null && wrS != null && effectivePbS <= wrS.timeMs,
                    isNRShort = effectivePbS != null && nrS != null && effectivePbS <= nrS.timeMs,
                    isWRLong = effectivePbL != null && wrL != null && effectivePbL <= wrL.timeMs,
                    isNRLong = effectivePbL != null && nrL != null && effectivePbL <= nrL.timeMs
                )
            }
            withContext(Dispatchers.Main) {
                recordsList = processed.sortedBy { it.eventName }
                medalsList = allMedals
                isLoading = false
            }
        }
    }

    if (showExpelDialog) {
        AlertDialog(
            onDismissRequest = { showExpelDialog = false },
            title = { Text("Expel Swimmer") },
            text = { Text("Are you sure you want to release ${swimmer.firstName} ${swimmer.lastName}?") },
            confirmButton = {
                TextButton(onClick = {
                    onExpelSwimmer(swimmer)
                    showExpelDialog = false
                }) {
                    Text("RELEASE", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExpelDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Swimmer Profile") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    if (swimmer.clubId == playerClubId && !swimmer.isRetired) {
                        IconButton(onClick = { showExpelDialog = true }) {
                            Icon(Icons.Default.Delete, null, tint = Color.Red)
                        }
                    }
                }
            )
        }
    ) { p ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(p)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HeaderSection(swimmer)
            PalmaresSection(medalsList)
            if (!isLoading && recordsList.isNotEmpty()) RecordsSection(recordsList)
            if (swimmer.clubId == playerClubId && !swimmer.isRetired) TrainingPlanSection(swimmer, onSwimmerUpdate)
            StylesSection(swimmer)
            PhysicalSection(swimmer)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingPlanSection(swimmer: Swimmer, onUpdate: (Swimmer) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Training", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            val trainingTypes = listOf(
                "ACTIVE_RECOVERY", "AEROBIC_BASE", "VO2_MAX", "LACTATE_TOLERANCE",
                "LACTATE_PRODUCTION", "PURE_SPEED", "TECHNIQUE_DRILLS", "BALANCED", "TAPERING"
            )
            val focusStyles = listOf("FREESTYLE", "BACKSTROKE", "BREASTSTROKE", "BUTTERFLY", "MEDLEY", "DEFAULT")
            var tExp by remember { mutableStateOf(false) }
            var fExp by remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(expanded = tExp, onExpandedChange = { tExp = !tExp }) {
                OutlinedTextField(
                    value = swimmer.currentTraining,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Regime") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(tExp) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = tExp, onDismissRequest = { tExp = false }) {
                    trainingTypes.forEach {
                        DropdownMenuItem(
                            text = { Text(it) },
                            onClick = {
                                onUpdate(swimmer.copy(currentTraining = it))
                                tExp = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            ExposedDropdownMenuBox(expanded = fExp, onExpandedChange = { fExp = !fExp }) {
                OutlinedTextField(
                    value = swimmer.focusStyle,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Stroke Focus") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(fExp) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = fExp, onDismissRequest = { fExp = false }) {
                    focusStyles.forEach {
                        DropdownMenuItem(
                            text = { Text(it) },
                            onClick = {
                                onUpdate(swimmer.copy(focusStyle = it))
                                fExp = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HeaderSection(s: Swimmer) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "${s.firstName} ${s.lastName}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text("${s.age} years old | ${s.gender} | ${s.nationality}", style = MaterialTheme.typography.bodyMedium)
            Row(
                modifier = Modifier.padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("OVR", style = MaterialTheme.typography.labelMedium)
                    Text(
                        "${s.overall}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("POT", style = MaterialTheme.typography.labelMedium)
                    Text(
                        s.potentialCategory,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@Composable
fun StylesSection(s: Swimmer) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Technical Skills", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            AttributeRow("Freestyle", s.freestyle)
            AttributeRow("Backstroke", s.backstroke)
            AttributeRow("Breaststroke", s.breaststroke)
            AttributeRow("Butterfly", s.butterfly)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            AttributeRow("Underwater", s.underwater)
        }
    }
}

@Composable
fun PhysicalSection(s: Swimmer) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Physical Skills", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            AttributeRow("Sprint Power", s.sprintPower)
            AttributeRow("Endurance", s.endurance)
            AttributeRow("Technique", s.technique)
            AttributeRow("Speed", s.speed)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            TirednessRow("Tiredness", s.tiredness)
        }
    }
}

@Composable
fun AttributeRow(l: String, v: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = l,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(130.dp)
        )
        LinearProgressIndicator(
            progress = { (v / 100f).toFloat() },
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(CircleShape),
            color = if (v > 80) Color(0xFF4CAF50) else if (v > 60) Color(0xFFFFC107) else MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = String.format(Locale.US, "%.1f", v),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(40.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun TirednessRow(l: String, v: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = l,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(130.dp)
        )
        LinearProgressIndicator(
            progress = { (v / 5f).toFloat() },
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(CircleShape),
            color = if (v > 4.0) Color.Red else if (v > 2.5) Color(0xFFFFC107) else Color(0xFF4CAF50),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = String.format(Locale.US, "%.1f/5.0", v),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(65.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun RecordsSection(list: List<EventRecords>) {
    var exp by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { exp = !exp },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Personal & Season Bests",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium
                )
                Icon(if (exp) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null)
            }
            AnimatedVisibility(visible = exp) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    list.forEach { r ->
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Text(r.eventName, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("25m PB: ${r.pbShort?.let { RaceEngine.formatTime(it) } ?: "--"}", fontSize = 12.sp)
                                        if (r.pbShort != null) {
                                            if (r.isWRShort) RecordBadge("WR", Color(0xFFD32F2F))
                                            else if (r.isNRShort) RecordBadge("NR", Color(0xFF1976D2))
                                        }
                                    }
                                    Text("25m SB: ${r.sbShort?.let { RaceEngine.formatTime(it) } ?: "--"}", fontSize = 12.sp)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("50m PB: ${r.pbLong?.let { RaceEngine.formatTime(it) } ?: "--"}", fontSize = 12.sp)
                                        if (r.pbLong != null) {
                                            if (r.isWRLong) RecordBadge("WR", Color(0xFFD32F2F))
                                            else if (r.isNRLong) RecordBadge("NR", Color(0xFF1976D2))
                                        }
                                    }
                                    Text("50m SB: ${r.sbLong?.let { RaceEngine.formatTime(it) } ?: "--"}", fontSize = 12.sp)
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
fun RecordBadge(text: String, color: Color) {
    Surface(
        modifier = Modifier.padding(start = 4.dp),
        color = color,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
        )
    }
}

@Composable
fun PalmaresSection(medals: List<Medal>) {
    if (medals.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Palmarès",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    PalmaresSummary(medals, modifier = Modifier.padding(top = 4.dp))
                }
                Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null)
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    val courseGroups = medals.groupBy { it.course }
                    
                    listOf("LONG", "SHORT").forEach { course ->
                        val mInCourse = courseGroups[course] ?: emptyList()
                        if (mInCourse.isNotEmpty()) {
                            val title = if (course == "LONG") "Long Course (50m)" else "Short Course (25m)"
                            PalmaresCourseAccordion(title, mInCourse)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PalmaresCourseAccordion(title: String, medals: List<Medal>) {
    var exp by remember { mutableStateOf(false) }
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                .clickable { exp = !exp }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                PalmaresSummary(medals, modifier = Modifier.padding(top = 2.dp))
            }
            Icon(if (exp) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        }
        AnimatedVisibility(visible = exp) {
            Column(modifier = Modifier.padding(start = 8.dp, top = 4.dp)) {
                val absoluteMedals = medals.filter { it.category == "ASSOLUTI" }
                val youthMedals = medals.filter { it.category != "ASSOLUTI" }
                
                if (youthMedals.isNotEmpty()) {
                    PalmaresCareerAccordion("Youth Career", youthMedals, isSenior = false)
                }
                if (absoluteMedals.isNotEmpty()) {
                    PalmaresCareerAccordion("Absolute Career", absoluteMedals, isSenior = true)
                }
            }
        }
    }
}

@Composable
fun PalmaresCareerAccordion(title: String, medals: List<Medal>, isSenior: Boolean) {
    var exp by remember { mutableStateOf(false) }
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { exp = !exp }
                .padding(vertical = 8.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val careerColor = if (isSenior) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge, color = careerColor)
                PalmaresSummary(medals, modifier = Modifier.padding(top = 2.dp))
            }
            Icon(if (exp) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null, modifier = Modifier.size(18.dp), tint = careerColor)
        }
        AnimatedVisibility(visible = exp) {
            Column(modifier = Modifier.padding(start = 8.dp)) {
                val tierGroups = medals.groupBy { it.tier }
                val tiersToShow = if (isSenior) {
                    listOf("OLYMPIC", "WORLD", "CONTINENTAL", "NATIONAL", "NATIONAL_TROPHY")
                } else {
                    listOf("WORLD", "NATIONAL")
                }
                
                tiersToShow.forEach { tier ->
                    val tierMedals = tierGroups[tier] ?: emptyList()
                    if (tierMedals.isNotEmpty()) {
                        PalmaresTierAccordion(tier, tierMedals)
                    }
                }
            }
        }
    }
}

@Composable
fun PalmaresTierAccordion(tier: String, medals: List<Medal>) {
    var exp by remember { mutableStateOf(false) }
    
    val label: String
    val icon: androidx.compose.ui.graphics.vector.ImageVector
    
    when(tier) {
        "OLYMPIC" -> {
            label = "Olympic Games"
            icon = Icons.Default.Star
        }
        "WORLD" -> {
            label = "World Championships"
            icon = Icons.Default.Star
        }
        "CONTINENTAL" -> {
            label = "Continental Competitions"
            icon = Icons.Default.LocationOn
        }
        "NATIONAL" -> {
            label = "National Championships"
            icon = Icons.Default.Place
        }
        "NATIONAL_TROPHY" -> {
            label = "National Trophies"
            icon = Icons.Default.Info
        }
        else -> {
            label = tier
            icon = Icons.Default.Star
        }
    }
    
    val tierColor = when(tier) {
        "OLYMPIC" -> Color(0xFFE91E63)
        "WORLD" -> Color(0xFF1976D2)
        "CONTINENTAL" -> Color(0xFF388E3C)
        "NATIONAL" -> Color(0xFFD32F2F)
        "NATIONAL_TROPHY" -> Color(0xFFF57C00)
        else -> MaterialTheme.colorScheme.secondary
    }

    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Surface(
            onClick = { exp = !exp },
            shape = MaterialTheme.shapes.medium,
            color = tierColor.copy(alpha = 0.1f),
            border = androidx.compose.foundation.BorderStroke(1.dp, tierColor.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(icon, null, modifier = Modifier.size(20.dp), tint = tierColor)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = label.uppercase(), 
                            style = MaterialTheme.typography.labelMedium, 
                            fontWeight = FontWeight.ExtraBold,
                            color = tierColor,
                            letterSpacing = 1.sp
                        )
                    }
                    PalmaresSummary(medals, modifier = Modifier.padding(top = 4.dp, start = 32.dp))
                }
                Icon(
                    if (exp) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, 
                    null, 
                    modifier = Modifier.size(20.dp),
                    tint = tierColor
                )
            }
        }
        AnimatedVisibility(visible = exp) {
            Column(modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 4.dp)) {
                medals.sortedByDescending { it.year }.forEach { m ->
                    MedalRow(m)
                }
            }
        }
    }
}

@Composable
fun MedalRow(m: Medal) {
    val medalColor = when (m.color) {
        "GOLD" -> Color(0xFFFFD700)
        "SILVER" -> Color(0xFFC0C0C0)
        "BRONZE" -> Color(0xFFCD7F32)
        else -> Color.Gray
    }
    val eventName = EventDictionary.getEventById(m.eventId)?.getDisplayName() ?: m.eventId

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(medalColor)
                .padding(2.dp)
                .background(Color.White.copy(alpha = 0.3f), CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = eventName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${m.year} • ${m.category}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PalmaresSummary(medals: List<Medal>, modifier: Modifier = Modifier) {
    val goldCount = medals.count { it.color == "GOLD" }
    val silverCount = medals.count { it.color == "SILVER" }
    val bronzeCount = medals.count { it.color == "BRONZE" }
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (goldCount > 0) MedalSummaryChip(Color(0xFFFFD700), goldCount)
        if (silverCount > 0) MedalSummaryChip(Color(0xFFC0C0C0), silverCount)
        if (bronzeCount > 0) MedalSummaryChip(Color(0xFFCD7F32), bronzeCount)
    }
}

@Composable
fun MedalSummaryChip(color: Color, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = "$count", fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
