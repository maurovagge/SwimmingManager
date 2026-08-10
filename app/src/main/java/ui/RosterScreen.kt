package ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.swimmingmanager.data.Swimmer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RosterScreen(
    swimmers: List<Swimmer>,
    onBackClick: () -> Unit,
    onSwimmerClick: (Swimmer) -> Unit,
    onTeamTrainingUpdate: (String, String) -> Unit
) {
    // 1. SPLIT AND SORT THE ROSTER
    // Active swimmers train and race
    val activeSwimmers = swimmers.filter { !it.isRetired }.sortedByDescending { it.overall }
    // Retired swimmers go to the Hall of Fame
    val retiredSwimmers = swimmers.filter { it.isRetired }.sortedByDescending { it.overall }

    // 2. TAB STATE MANAGEMENT
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Rosa Attiva (${activeSwimmers.size})", "Hall of Fame (${retiredSwimmers.size})")

    // State for expanding/collapsing the whole training card
    var isTrainingCardExpanded by remember { mutableStateOf(false) }

    // State variables for the team training dropdowns
    var expandedTraining by remember { mutableStateOf(false) }
    var selectedTraining by remember { mutableStateOf("BALANCED") }
    val trainingOptions = listOf(
        "ACTIVE_RECOVERY", "AEROBIC_BASE", "VO2_MAX",
        "LACTATE_TOLERANCE", "LACTATE_PRODUCTION",
        "PURE_SPEED", "TECHNIQUE_DRILLS", "BALANCED", "TAPERING"
    )

    var expandedFocus by remember { mutableStateOf(false) }
    var selectedFocus by remember { mutableStateOf("DEFAULT") }
    val focusOptions = listOf("DEFAULT", "FREESTYLE", "BACKSTROKE", "BREASTSTROKE", "BUTTERFLY", "MEDLEY")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("La Mia Squadra") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Indietro")
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

            // --- TABS NAVIGATION ---
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.White,
                contentColor = Color(0xFF1976D2)
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            // --- DYNAMIC CONTENT BASED ON SELECTED TAB ---
            when (selectedTabIndex) {
                0 -> {
                    // ==========================================
                    // TAB 0: ACTIVE ROSTER & TRAINING CONTROLS
                    // ==========================================

                    // --- EXPANDABLE TEAM TRAINING CONTROLS CARD ---
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            // Make the header clickable to toggle expansion
                            .clickable { isTrainingCardExpanded = !isTrainingCardExpanded },
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {

                            // --- ALWAYS VISIBLE HEADER ---
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Imposta Allenamento di Squadra",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color(0xFF1565C0)
                                )
                                Icon(
                                    imageVector = if (isTrainingCardExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (isTrainingCardExpanded) "Comprimi" else "Espandi",
                                    tint = Color.Gray
                                )
                            }

                            // --- HIDDEN CONTROLS (Shows only when expanded) ---
                            AnimatedVisibility(visible = isTrainingCardExpanded) {
                                Column {
                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Training Program Dropdown
                                    ExposedDropdownMenuBox(
                                        expanded = expandedTraining,
                                        onExpandedChange = { expandedTraining = !expandedTraining }
                                    ) {
                                        OutlinedTextField(
                                            value = selectedTraining,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Programma (Regime)") },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTraining) },
                                            modifier = Modifier
                                                .menuAnchor()
                                                .fillMaxWidth()
                                        )
                                        ExposedDropdownMenu(
                                            expanded = expandedTraining,
                                            onDismissRequest = { expandedTraining = false }
                                        ) {
                                            trainingOptions.forEach { option ->
                                                DropdownMenuItem(
                                                    text = { Text(option) },
                                                    onClick = {
                                                        selectedTraining = option
                                                        expandedTraining = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Stroke Focus Dropdown
                                    ExposedDropdownMenuBox(
                                        expanded = expandedFocus,
                                        onExpandedChange = { expandedFocus = !expandedFocus }
                                    ) {
                                        OutlinedTextField(
                                            value = selectedFocus,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Focus Stile") },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFocus) },
                                            modifier = Modifier
                                                .menuAnchor()
                                                .fillMaxWidth()
                                        )
                                        ExposedDropdownMenu(
                                            expanded = expandedFocus,
                                            onDismissRequest = { expandedFocus = false }
                                        ) {
                                            focusOptions.forEach { option ->
                                                DropdownMenuItem(
                                                    text = { Text(option) },
                                                    onClick = {
                                                        selectedFocus = option
                                                        expandedFocus = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Apply Button
                                    Button(
                                        onClick = {
                                            onTeamTrainingUpdate(selectedTraining, selectedFocus)
                                            // Automatically close the card after applying
                                            isTrainingCardExpanded = false
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047))
                                    ) {
                                        Text("Applica a tutta la squadra", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // --- ACTIVE SWIMMERS ROSTER LIST ---
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(activeSwimmers) { swimmer ->
                            SwimmerRosterCard(swimmer = swimmer, onClick = { onSwimmerClick(swimmer) })
                        }

                        if (activeSwimmers.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Text("Nessun atleta in rosa.", color = Color.Gray, modifier = Modifier.padding(16.dp))
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // ==========================================
                    // TAB 1: HALL OF FAME (RETIRED SWIMMERS)
                    // ==========================================
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Optional header for the legends
                        item {
                            Text(
                                text = "Le Leggende del Club 🏛️",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color(0xFFE65100), // Deep Orange / Gold
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        // Reusing the beautiful SwimmerRosterCard for retired athletes!
                        items(retiredSwimmers) { swimmer ->
                            SwimmerRosterCard(swimmer = swimmer, onClick = { onSwimmerClick(swimmer) })
                        }

                        if (retiredSwimmers.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Text("Nessun atleta ritirato al momento.", color = Color.Gray, modifier = Modifier.padding(16.dp))
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
fun SwimmerRosterCard(swimmer: Swimmer, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        // Subtle background change if retired to give it a "vintage" look
        colors = CardDefaults.cardColors(containerColor = if (swimmer.isRetired) Color(0xFFFFF8E1) else Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // --- OVERALL BADGE ---
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(getOverallColor(swimmer.overall)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = swimmer.overall.toString(),
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // --- MAIN INFORMATION ---
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${swimmer.lastName?.uppercase()} ${swimmer.firstName}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF212121)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${swimmer.age} anni • ${swimmer.nationality}",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    // Aging or retirement conditions
                    if (swimmer.isRetired) {
                        Text("(Ritirato)", fontSize = 11.sp, color = Color.Red, fontWeight = FontWeight.Bold)
                    } else if (swimmer.isDeclining) {
                        Text("(In calo)", fontSize = 11.sp, color = Color(0xFFE64A19))
                    }
                }
            }

            // --- STROKE AND POTENTIAL BADGE ---
            Column(horizontalAlignment = Alignment.End) {
                // Main stroke label
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = getStyleColor(swimmer.mainStyle),
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Text(
                        text = swimmer.mainStyle,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                // Potential label (Hide potential if retired, as it's no longer relevant)
                if (!swimmer.isRetired) {
                    Text(
                        text = "Pot: ${swimmer.potentialCategory}",
                        fontSize = 11.sp,
                        color = Color.DarkGray
                    )
                }
            }
        }
    }
}

// --- HELPER FUNCTIONS FOR COLORS ---

fun getOverallColor(overall: Int): Color {
    return when {
        overall >= 80 -> Color(0xFFFFB300) // Gold
        overall >= 70 -> Color(0xFF43A047) // Green
        overall >= 60 -> Color(0xFF1E88E5) // Blue
        overall >= 50 -> Color(0xFFF4511E) // Orange
        else -> Color(0xFF757575)          // Gray
    }
}

fun getStyleColor(style: String): Color {
    return when (style) {
        "SL" -> Color(0xFF039BE5) // Light Blue Freestyle
        "DO" -> Color(0xFF8E24AA) // Purple Backstroke
        "RA" -> Color(0xFF43A047) // Green Breaststroke
        "FA" -> Color(0xFFE53935) // Red Butterfly
        "MX" -> Color(0xFFFDD835) // Yellow Medley
        else -> Color.Gray
    }
}