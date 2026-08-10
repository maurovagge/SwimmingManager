package com.example.swimmingmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.swimmingmanager.data.AppDatabase
import com.example.swimmingmanager.data.Club
import com.example.swimmingmanager.data.Message
import com.example.swimmingmanager.engine.EventDictionary
import com.example.swimmingmanager.ui.Destination
import com.example.swimmingmanager.ui.viewmodel.MainViewModel
import ui.*

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    
                    val db = AppDatabase.getDatabase(this)

                    when {
                        viewModel.isLoading -> {
                            LoadingScreen()
                        }
                        viewModel.mySavedClubId == -1 -> {
                            ClubSelectionScreen(clubs = viewModel.clubList) { selectedClub ->
                                viewModel.selectClub(selectedClub)
                            }
                        }
                        else -> {
                            when (val screen = viewModel.currentScreen) {
                                is Destination.Dashboard -> {
                                    MainDashboardContainer()
                                }

                                is Destination.Roster -> {
                                    RosterScreen(
                                        swimmers = viewModel.myRosterList,
                                        onBackClick = { viewModel.navigateTo(Destination.Dashboard) },
                                        onSwimmerClick = { viewModel.navigateTo(Destination.SwimmerProfile(it)) },
                                        onTeamTrainingUpdate = { training, focus ->
                                            viewModel.updateTeamTraining(training, focus)
                                        }
                                    )
                                }

                                is Destination.SwimmerProfile -> {
                                    SwimmerProfileScreen(
                                        swimmer = screen.swimmer,
                                        onBackClick = { viewModel.navigateTo(Destination.Dashboard) },
                                        db = db,
                                        currentYear = viewModel.currentYear,
                                        playerClubId = viewModel.mySavedClubId,
                                        onSwimmerUpdate = { viewModel.updateSwimmerProfile(it) },
                                        onExpelSwimmer = { viewModel.expelSwimmer(it) }
                                    )
                                }

                                is Destination.Calendar -> {
                                    CalendarScreen(
                                        competitions = viewModel.competitionList.filter { it.year == viewModel.currentYear },
                                        onBackClick = { viewModel.navigateTo(Destination.Dashboard) },
                                        onCompetitionClick = { comp ->
                                            val isPast = comp.year < viewModel.currentYear ||
                                                    (comp.year == viewModel.currentYear && comp.week < viewModel.currentWeek)
                                            
                                            if (isPast) {
                                                viewModel.navigateTo(Destination.CompetitionResults(comp))
                                            } else {
                                                val isMajor = comp.tier in listOf("WORLD", "CONTINENTAL", "INTERNATIONAL")
                                                if (isMajor) {
                                                    viewModel.navigateTo(Destination.NationalCallUps(comp))
                                                } else {
                                                    viewModel.navigateTo(Destination.CompetitionDetail(comp))
                                                }
                                            }
                                        }
                                    )
                                }

                                is Destination.CompetitionDetail -> {
                                    CompetitionDetailScreen(
                                        competition = screen.competition,
                                        roster = viewModel.myRosterList,
                                        availableEvents = EventDictionary.getEventsForCourse(screen.competition.isShortCourse),
                                        db = db,
                                        currentYear = viewModel.currentYear,
                                        clubNation = viewModel.myClub?.country ?: "",
                                        onBackClick = { viewModel.navigateTo(Destination.Calendar) }
                                    )
                                }

                                is Destination.CompetitionResults -> {
                                    CompetitionResultsScreen(
                                        competition = screen.competition,
                                        db = db,
                                        playerClubId = viewModel.mySavedClubId,
                                        onBackClick = { viewModel.navigateTo(Destination.Calendar) },
                                        onSwimmerClick = { viewModel.navigateTo(Destination.SwimmerProfile(it)) }
                                    )
                                }

                                is Destination.NationalCallUps -> {
                                    val factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                                        @Suppress("UNCHECKED_CAST")
                                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                            return CallUpsViewModel(db) as T
                                        }
                                    }
                                    val callUpsViewModel: CallUpsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = factory)

                                    NationalCallUpsScreen(
                                        competition = screen.competition,
                                        playerNation = viewModel.myClub?.country ?: "",
                                        availableEvents = EventDictionary.getEventsForCourse(screen.competition.isShortCourse),
                                        playerClubId = viewModel.mySavedClubId,
                                        currentYear = viewModel.currentYear,
                                        onBackClick = { viewModel.navigateTo(Destination.Calendar) },
                                        viewModel = callUpsViewModel,
                                        db = db
                                    )
                                }

                                is Destination.TimeStandards -> {
                                    LaunchedEffect(Unit) { viewModel.loadTimeStandards() }
                                    TimeStandardsScreen(
                                        standardsList = viewModel.myTimeStandards,
                                        currentYear = viewModel.currentYear,
                                        onBackClick = { viewModel.navigateTo(Destination.Dashboard) }
                                    )
                                }

                                is Destination.Rankings -> {
                                    RankingsScreen(
                                        db = db,
                                        currentYear = viewModel.currentYear,
                                        playerNation = viewModel.myClub?.country ?: "",
                                        playerRegion = viewModel.myClub?.region ?: "",
                                        playerClubId = viewModel.mySavedClubId,
                                        onBackClick = { viewModel.navigateTo(Destination.Dashboard) },
                                        onSwimmerClick = { viewModel.navigateTo(Destination.SwimmerProfile(it)) }
                                    )
                                }

                                is Destination.Records -> {
                                    RecordsScreen(
                                        db = db,
                                        playerNation = viewModel.myClub?.country ?: "",
                                        playerRegion = viewModel.myClub?.region ?: "",
                                        playerClubId = viewModel.mySavedClubId,
                                        onBackClick = { viewModel.navigateTo(Destination.Dashboard) },
                                        onSwimmerClick = { viewModel.navigateTo(Destination.SwimmerProfile(it)) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun LoadingScreen() {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Caricamento mondo...")
            }
        }
    }

    @Composable
    private fun MainDashboardContainer() {
        var showResetDialog by remember { mutableStateOf(false) }
        var selectedMessage by remember { mutableStateOf<Message?>(null) }
        val messages by viewModel.messages.collectAsState()

        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = { Text("Nuova Partita") },
                text = { Text("Sei sicuro di voler ricominciare da capo? Tutti i tuoi progressi e i dati del mondo verranno cancellati definitivamente.") },
                confirmButton = {
                    TextButton(onClick = {
                        showResetDialog = false
                        viewModel.startNewGame()
                    }) {
                        Text("RICOMINCIA", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) {
                        Text("ANNULLA")
                    }
                }
            )
        }

        if (selectedMessage != null) {
            AlertDialog(
                onDismissRequest = { selectedMessage = null },
                title = { Text(selectedMessage!!.title, fontWeight = FontWeight.Bold) },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text(selectedMessage!!.body)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedMessage = null }) {
                        Text("CHIUDI")
                    }
                }
            )
        }

        if (viewModel.activeMeet != null) {
            LiveMeetScreen(
                competition = viewModel.activeMeet!!,
                playerClubId = viewModel.mySavedClubId,
                db = AppDatabase.getDatabase(this),
                currentYear = viewModel.currentYear,
                onMeetFinished = { viewModel.onMeetFinished() }
            )
        } else {
            DashboardScreen(
                club = viewModel.myClub,
                messages = messages,
                currentWeek = viewModel.currentWeek,
                currentYear = viewModel.currentYear,
                isSimulating = viewModel.isSimulating,
                onViewRosterClick = { viewModel.navigateTo(Destination.Roster) },
                onViewCalendarClick = { viewModel.navigateTo(Destination.Calendar) },
                onViewRecordsClick = { viewModel.navigateTo(Destination.Records) },
                onViewTimeStandardsClick = { viewModel.navigateTo(Destination.TimeStandards) },
                onViewRankingsClick = { viewModel.navigateTo(Destination.Rankings) },
                onAdvanceWeekClick = { viewModel.advanceWeek() },
                onNewGameClick = { showResetDialog = true },
                onMessageClick = {
                    selectedMessage = it
                    viewModel.markMessageAsRead(it.id)
                }
            )
        }
    }

    @Composable
    fun DashboardScreen(
        club: Club?,
        messages: List<Message>,
        currentWeek: Int,
        currentYear: Int,
        isSimulating: Boolean,
        onViewRosterClick: () -> Unit,
        onAdvanceWeekClick: () -> Unit,
        onViewCalendarClick: () -> Unit,
        onViewTimeStandardsClick: () -> Unit,
        onViewRankingsClick: () -> Unit,
        onViewRecordsClick: () -> Unit,
        onNewGameClick: () -> Unit,
        onMessageClick: (Message) -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF0F4F8))
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (club != null) {
                Spacer(modifier = Modifier.height(8.dp))
                
                InboxSection(messages = messages, onMessageClick = onMessageClick)

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Anno $currentYear • Settimana $currentWeek",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD32F2F)
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFEEEEEE))
                        Text(text = "Stai allenando:", fontSize = 14.sp, color = Color.Gray)
                        Text(text = club.name, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF212121))
                        Text(text = "${club.city}, ${club.region}  " + "⭐".repeat(club.prestige), fontSize = 14.sp, color = Color.DarkGray)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onAdvanceWeekClick,
                    enabled = !isSimulating,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047), disabledContainerColor = Color.Gray),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth().height(64.dp)
                ) {
                    if (isSimulating) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Simulazione in corso...", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Text("AVANZA SETTIMANA ⏭️", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                DashboardMenuButton("La Mia Squadra", Icons.Default.Person, Color(0xFF1976D2), onViewRosterClick)
                DashboardMenuButton("Calendario Gare", Icons.Default.DateRange, Color(0xFF1976D2), onViewCalendarClick)
                DashboardMenuButton("Tempi Limite", Icons.Default.Info, Color(0xFFF57C00), onViewTimeStandardsClick)
                DashboardMenuButton("Ranking", Icons.Default.List, Color(0xFF00838F), onViewRankingsClick)
                DashboardMenuButton("Record", Icons.Default.Star, Color(0xFF00838F), onViewRecordsClick)
                
                Spacer(modifier = Modifier.height(32.dp))

                OutlinedButton(
                    onClick = onNewGameClick,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("NUOVA PARTITA", fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    @Composable
    fun InboxSection(messages: List<Message>, onMessageClick: (Message) -> Unit) {
        val unreadCount = messages.count { !it.isRead }
        
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Centro Notifiche",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF455A64)
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (unreadCount > 0) {
                        Text(
                            text = "Smarca tutto",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF1976D2),
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier
                                .clickable { viewModel.markAllMessagesAsRead() }
                                .padding(end = 12.dp)
                        )
                        Badge(containerColor = Color.Red) {
                            Text("$unreadCount nuove", color = Color.White)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (messages.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "Nessun messaggio recente.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            } else {
                LazyRow(
                    contentPadding = PaddingValues(end = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(messages) { message ->
                        MessageCard(message = message, onClick = { onMessageClick(message) })
                    }
                }
            }
        }
    }

    @Composable
    fun MessageCard(message: Message, onClick: () -> Unit) {
        val backgroundColor = when (message.type) {
            "CALLUP" -> Color(0xFFE3F2FD)
            "RETIREMENT" -> Color(0xFFFBE9E7)
            "RECRUIT" -> Color(0xFFF1F8E9)
            "SEASON_END" -> Color(0xFFFFF3E0)
            else -> Color.White
        }
        val accentColor = when (message.type) {
            "CALLUP" -> Color(0xFF1976D2)
            "RETIREMENT" -> Color(0xFFD32F2F)
            "RECRUIT" -> Color(0xFF388E3C)
            "SEASON_END" -> Color(0xFFF57C00)
            else -> Color.Gray
        }

        Card(
            modifier = Modifier
                .width(280.dp)
                .height(100.dp)
                .clickable { onClick() },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            border = if (!message.isRead) BorderStroke(2.dp, accentColor) else null,
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when(message.type) {
                            "CALLUP" -> Icons.Default.Star
                            "RETIREMENT" -> Icons.Default.Refresh
                            "RECRUIT" -> Icons.Default.Person
                            else -> Icons.Default.Notifications
                        },
                        contentDescription = null,
                        tint = accentColor
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = message.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = message.body,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.DarkGray
                    )
                }
            }
        }
    }

    @Composable
    fun DashboardMenuButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, onClick: () -> Unit) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = color)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Text(text = text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
