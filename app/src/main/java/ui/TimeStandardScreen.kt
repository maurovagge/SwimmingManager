package ui

import androidx.compose.animation.AnimatedVisibility
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
import com.example.swimmingmanager.data.TimeStandard
import com.example.swimmingmanager.engine.RaceEngine
import androidx.activity.compose.BackHandler

@Composable
fun TimeStandardsScreen(
    standardsList: List<TimeStandard>,
    currentYear: Int,
    onBackClick: () -> Unit
) {
    BackHandler(onBack = onBackClick)

    // State for Pool Length Toggle
    var isShortCourseSelected by remember { mutableStateOf(false) } // Default to 50m

    // Filter the standards based on the toggle BEFORE grouping them
    val filteredStandards = standardsList.filter { it.isShortCourse == isShortCourseSelected }
    val standardsByTier = filteredStandards.groupBy { it.tier }

    val tiers = listOf("NATIONAL_RAGAZZI", "NATIONAL_JUNIORES", "NATIONAL_CADETTI", "NATIONAL_ABSOLUTE")
    val displayNames = listOf("Ragazzi", "Juniores", "Cadetti", "Assoluti")

    var selectedTabIndex by remember { mutableIntStateOf(3) }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FA))
    ) {
        // --- Header ---
        Surface(
            color = Color(0xFF1976D2),
            contentColor = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "◁ Torna alla Home",
                    fontSize = 14.sp,
                    modifier = Modifier.clickable { onBackClick() }.padding(bottom = 8.dp)
                )
                Text(
                    text = "Tempi Limite $currentYear",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(modifier = Modifier.height(12.dp))

                // --- POOL LENGTH TOGGLE (25m vs 50m) ---
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Vasca Lunga (50m)", fontSize = 14.sp, fontWeight = if (!isShortCourseSelected) FontWeight.Bold else FontWeight.Normal)
                    Switch(
                        checked = isShortCourseSelected,
                        onCheckedChange = { isShortCourseSelected = it },
                        modifier = Modifier.padding(horizontal = 8.dp),
                        colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFFF57C00))
                    )
                    Text("Vasca Corta (25m)", fontSize = 14.sp, fontWeight = if (isShortCourseSelected) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }

        // --- Tabs ---
        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Color.White,
            contentColor = Color(0xFF1976D2),
            edgePadding = 8.dp
        ) {
            displayNames.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title, fontWeight = FontWeight.Bold) }
                )
            }
        }

        // --- Active Tab Content ---
        val activeTier = tiers[selectedTabIndex]
        val activeStandards = standardsByTier[activeTier] ?: emptyList()

        // Filter and Sort Males and Females
        val maleStandards = activeStandards
            .filter { it.eventId.endsWith("_M") }
            .sortedBy { getEventSortWeight(it.eventId) }

        val femaleStandards = activeStandards
            .filter { it.eventId.endsWith("_F") }
            .sortedBy { getEventSortWeight(it.eventId) }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (activeStandards.isEmpty()) {
                item {
                    Text(
                        text = "Nessun tempo limite generato per questa categoria.",
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 32.dp)
                    )
                }
            } else {
                // Male Section Dropdown
                item {
                    GenderSectionCard(title = "MASCHILE", standards = maleStandards)
                }

                // Female Section Dropdown
                item {
                    GenderSectionCard(title = "FEMMINILE", standards = femaleStandards)
                }
            }
        }
    }
}

// Custom sorting logic to recreate standard Olympic order
fun getEventSortWeight(eventId: String): Int {
    val parts = eventId.split("_")
    val distance = parts.getOrNull(0)?.toIntOrNull() ?: 0
    val stroke = parts.getOrNull(1) ?: ""

    // Standard order: Fly, Back, Breast, Free, Medley
    val strokeWeight = when(stroke) {
        "FLY" -> 1
        "BACK" -> 2
        "BREAST" -> 3
        "FREE" -> 4
        "MEDLEY" -> 5
        else -> 6
    }

    // Example: 100_FLY becomes 10100. 50_FREE becomes 40050.
    return (strokeWeight * 10000) + distance
}

@Composable
fun GenderSectionCard(title: String, standards: List<TimeStandard>) {
    var isExpanded by remember { mutableStateOf(true) } // Opened by default

    Card(
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Header Row (Clickable)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2)
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.ArrowDropDown,
                    contentDescription = "Expand/Collapse",
                    tint = Color.Gray
                )
            }

            // Expanded List
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    standards.forEach { standard ->
                        TimeStandardRow(standard = standard)
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                    }
                }
            }
        }
    }
}

@Composable
fun TimeStandardRow(standard: TimeStandard) {
    val parts = standard.eventId.split("_")
    val distance = parts.getOrNull(0) ?: ""
    val stroke = parts.getOrNull(1) ?: ""
    val formattedEventName = "${distance}m $stroke"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = formattedEventName,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF212121)
        )
        Text(
            text = RaceEngine.formatTime(standard.timeMs),
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFFD32F2F)
        )
    }
}