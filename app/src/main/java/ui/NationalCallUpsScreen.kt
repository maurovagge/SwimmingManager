package ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.swimmingmanager.data.AppDatabase
import com.example.swimmingmanager.data.Competition
import com.example.swimmingmanager.data.RaceRegistrationDao
import com.example.swimmingmanager.engine.SwimmingEvent
import com.example.swimmingmanager.engine.TimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// --- VIEW MODEL ---
// This handles the data loading in the background so the UI doesn't freeze
class CallUpsViewModel(private val db: AppDatabase) : ViewModel() {

    // State holding the roster mapped by Event ID
    var rosterByEvent by mutableStateOf<Map<String, List<RaceRegistrationDao.CallUpDisplayInfo>>>(emptyMap())
        private set

    // Called when the screen opens to load all the qualified swimmers
    fun loadCallUps(compId: Int, nationCode: String, currentYear: Int, events: List<SwimmingEvent>, isShortCourse: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {

            // ONE single query for the entire competition!
            val allCallUps = db.raceRegistrationDao().getAllDetailedNationalCallUps(
                compId = compId,
                nationCode = nationCode,
                currentYear = currentYear,
                isShortCourse = isShortCourse
            )

            // Group them by eventCode instantly in RAM
            val newRoster = mutableMapOf<String, List<RaceRegistrationDao.CallUpDisplayInfo>>()

            val groupedByEvent = allCallUps.groupBy { it.eventCode }

            for (event in events) {
                val eventCallUps = groupedByEvent[event.id] ?: emptyList()

                // Map the new data class back to your original UI data class
                newRoster[event.id] = eventCallUps.map {
                    RaceRegistrationDao.CallUpDisplayInfo(
                        firstName = it.firstName,
                        lastName = it.lastName,
                        clubId = it.clubId,
                        qualificationTimeMs = it.qualificationTimeMs
                    )
                }
            }

            rosterByEvent = newRoster
        }
    }
}

// --- MAIN SCREEN COMPOSABLE ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NationalCallUpsScreen(
    competition: Competition,
    playerNation: String,
    availableEvents: List<SwimmingEvent>,
    onBackClick: () -> Unit,
    playerClubId: Int,
    currentYear: Int,
    viewModel: CallUpsViewModel,
    db: AppDatabase
) {
    // Intercepts the physical back button of the Android device
    BackHandler(onBack = onBackClick)

    // State to manage which tab is currently selected (0 = Call-ups, 1 = Records)
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Convocazioni", "Record Manifestazione")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nazionale: $playerNation") },
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
            // --- Tabs Row ---
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

            // --- Tab Content ---
            when (selectedTabIndex) {
                0 -> CallUpsTab(
                    competition = competition,
                    playerNation = playerNation,
                    availableEvents = availableEvents,
                    playerClubId = playerClubId,
                    currentYear = currentYear,
                    viewModel = viewModel
                )
                1 -> MeetInfoRecordsTab(
                    competition = competition,
                    db = db
                )
            }
        }
    }
}

// --- NEW COMPOSABLE: Isolates the Call-Ups loading and display ---
@Composable
fun CallUpsTab(
    competition: Competition,
    playerNation: String,
    availableEvents: List<SwimmingEvent>,
    playerClubId: Int,
    currentYear: Int,
    viewModel: CallUpsViewModel
) {
    // Trigger the data load exactly once when the screen appears
    LaunchedEffect(competition.id) {
        viewModel.loadCallUps(competition.id, playerNation, currentYear, availableEvents, isShortCourse = competition.isShortCourse)
    }

    // Scrollable list of events
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(availableEvents) { event ->
            val roster = viewModel.rosterByEvent[event.id] ?: emptyList()

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Event Header
                    Text(
                        text = event.getDisplayName(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF1565C0)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (roster.isEmpty()) {
                        Text("Nessun qualificato.", fontStyle = FontStyle.Italic, color = Color.Gray)
                    } else {
                        // Display the called-up athletes
                        roster.forEach { info ->
                            NationalCallUpRow(info = info, playerClubId = playerClubId)
                        }
                    }
                }
            }
        }
    }
}

// --- SINGLE ROW COMPOSABLE ---
@Composable
fun NationalCallUpRow(
    info: RaceRegistrationDao.CallUpDisplayInfo,
    playerClubId: Int
) {
    val isMySwimmer = info.clubId == playerClubId

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Name Column
        Text(
            text = "${info.firstName} ${info.lastName}",
            fontWeight = if (isMySwimmer) FontWeight.ExtraBold else FontWeight.Medium,
            color = if (isMySwimmer) Color(0xFF2E7D32) else Color.Unspecified,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f)
        )

        // Qualification Time Column
        Surface(
            color = if (isMySwimmer) Color(0xFFE8F5E9) else Color(0xFFF5F5F5),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(
                text = TimeFormatter.formatLongToTime(info.qualificationTimeMs),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}
