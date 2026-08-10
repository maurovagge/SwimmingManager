package ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.swimmingmanager.data.AppDatabase
import com.example.swimmingmanager.data.Club
import com.example.swimmingmanager.data.Competition
import com.example.swimmingmanager.data.RaceResult
import com.example.swimmingmanager.engine.EventDictionary
import com.example.swimmingmanager.engine.RaceEngine
import com.example.swimmingmanager.ui.state.LiveMeetPhase
import com.example.swimmingmanager.ui.state.LiveMeetState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun LiveMeetScreen(
    competition: Competition,
    playerClubId: Int,
    db: AppDatabase,
    currentYear: Int,
    onMeetFinished: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    val meetState = remember {
        LiveMeetState(competition, playerClubId, db, currentYear, coroutineScope)
    }

    var userNation by remember { mutableStateOf("") }
    var userRegion by remember { mutableStateOf("") }

    LaunchedEffect(competition.name) {
        withContext(Dispatchers.IO) {
            val playerClub = db.clubDao().getClubById(playerClubId)
            val nation = playerClub?.country ?: "ITA"
            val region = playerClub?.region ?: ""

            withContext(Dispatchers.Main) {
                userNation = nation
                userRegion = region
                // THE FIX: Passing both nation and region
                meetState.initialize(nation, region)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE3F2FD))
    ) {
        Surface(
            color = Color(0xFF1565C0),
            contentColor = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = competition.name.uppercase(),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Weekend di Gara - Settimana ${competition.week}",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            when (meetState.phase) {
                LiveMeetPhase.INITIALIZING -> {
                    CircularProgressIndicator(color = Color(0xFF1565C0))
                }

                LiveMeetPhase.READY_TO_RACE -> {
                    ReadyToRaceView(meetState)
                }

                LiveMeetPhase.SHOWING_RESULTS -> {
                    ResultsScoreboardView(
                        meetState = meetState,
                        competition = competition,
                        playerClubId = playerClubId,
                        playerClubNation = userNation
                    )
                }

                LiveMeetPhase.MEET_FINISHED -> {
                    MeetFinishedView(onMeetFinished)
                }
            }
        }
    }
}

@Composable
fun ReadyToRaceView(meetState: LiveMeetState) {
    val eventName = EventDictionary.getEventById(meetState.currentEventId)?.getDisplayName() ?: "Gara"
    val roundName = translateRound(meetState.currentRound)
    val currentCR = meetState.meetRecordsMap[meetState.currentEventId]

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth(0.9f)
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = roundName.uppercase(), color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = eventName, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)

            Spacer(modifier = Modifier.height(16.dp))

            if (currentCR != null) {
                RecordBadge("CR", currentCR, Color(0xFFE65100))
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { meetState.simulateCurrentRound() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(32.dp)
            ) {
                Text("SIMULA GARA", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ResultsScoreboardView(
    meetState: LiveMeetState,
    competition: Competition,
    playerClubId: Int,
    playerClubNation: String
) {
    val eventId = meetState.currentEventId
    val eventName = EventDictionary.getEventById(eventId)?.getDisplayName() ?: ""
    val roundName = translateRound(meetState.currentRound)

    val currentCR = meetState.meetRecordsMap[eventId] ?: Int.MAX_VALUE
    val currentWR = meetState.worldRecordsMap[eventId] ?: Int.MAX_VALUE
    val currentNR = meetState.nationalRecordsMap[eventId] ?: Int.MAX_VALUE
    val currentRR = meetState.regionalRecordsMap[eventId] ?: Int.MAX_VALUE

    val isRelay = eventId.startsWith("4x")

    Column(modifier = Modifier.fillMaxSize()) {
        Text(text = "$eventName - $roundName", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))

        Row(modifier = Modifier.padding(vertical = 8.dp)) {
            RecordBadge("WR", currentWR, Color(0xFFD50000))
            Spacer(modifier = Modifier.width(4.dp))
            RecordBadge("NR", currentNR, Color(0xFF2962FF))
            Spacer(modifier = Modifier.width(4.dp))
            RecordBadge("RR", currentRR, Color(0xFF00838F))
            Spacer(modifier = Modifier.width(4.dp))
            RecordBadge("CR", currentCR, Color(0xFFE65100))
        }

        Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            LazyColumn(contentPadding = PaddingValues(8.dp), modifier = Modifier.fillMaxSize()) {
                val itemCount = if (isRelay) meetState.currentRelayResults.size else meetState.currentResults.size

                items(itemCount) { index ->
                    val info = if (isRelay) {
                        val res = meetState.currentRelayResults[index]
                        ResultDisplayInfo(res.totalTimeMs, res.teamId == playerClubId, res.teamName, "Staffetta", if (res.teamId == playerClubId) playerClubNation else "")
                    } else {
                        val res = meetState.currentResults[index]
                        val swimmer = meetState.getSwimmer(res.swimmerId)
                        val subtitle = if (competition.isNationalTeamEvent) swimmer?.nationality ?: "" else "${meetState.getClubName(swimmer?.clubId ?: -1)} • ${swimmer?.age} anni"
                        ResultDisplayInfo(res.timeMs, swimmer?.clubId == playerClubId, "${swimmer?.lastName?.uppercase()} ${swimmer?.firstName}", subtitle, swimmer?.nationality ?: "")
                    }

                    val brokeWR = info.timeMs <= currentWR
                    val brokeNR = info.timeMs <= currentNR && info.nationality == playerClubNation
                    val brokeRR = info.timeMs <= currentRR && info.isPlayer
                    val brokeCR = info.timeMs <= currentCR

                    Row(modifier = Modifier.fillMaxWidth().background(if (info.isPlayer) Color(0xFFFFF9C4) else Color.Transparent).padding(12.dp)) {
                        Text(text = "${index + 1}°", fontWeight = FontWeight.Bold, modifier = Modifier.width(32.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = info.name, fontWeight = if (info.isPlayer) FontWeight.ExtraBold else FontWeight.Medium)
                            Text(text = info.subtitle, fontSize = 12.sp, color = Color.Gray)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = RaceEngine.formatTime(info.timeMs), fontWeight = FontWeight.ExtraBold)
                            Row {
                                if (brokeWR && !isRelay) RecordTag("WR", Color(0xFFD50000))
                                if (brokeNR && !isRelay) RecordTag("NR", Color(0xFF2962FF))
                                if (brokeRR && !isRelay) RecordTag("RR", Color(0xFF00838F))
                                if (brokeCR && !isRelay) RecordTag("CR", Color(0xFFE65100))
                            }
                        }
                    }
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { meetState.advanceToNextRoundOrEvent() }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text("CONTINUA", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun RecordBadge(label: String, time: Int, color: Color) {
    if (time == Int.MAX_VALUE) return
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = Modifier.padding(bottom = 4.dp)
    ) {
        Text(text = "$label: ${RaceEngine.formatTime(time)}", fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun RecordTag(label: String, color: Color) {
    Text(text = "$label! ", fontSize = 10.sp, fontWeight = FontWeight.Black, color = color, modifier = Modifier.padding(start = 2.dp))
}

@Composable
fun MeetFinishedView(onMeetFinished: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize()) {
        Text(text = "🏁", fontSize = 64.sp)
        Text(text = "Manifestazione Conclusa!", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onMeetFinished) { Text("Torna alla Dashboard") }
    }
}

data class ResultDisplayInfo(val timeMs: Int, val isPlayer: Boolean, val name: String, val subtitle: String, val nationality: String)

fun translateRound(round: String): String {
    return when (round) {
        "HEATS" -> "Batterie"
        "SEMI_FINAL" -> "Semifinale"
        "FINAL" -> "Finale"
        "TIMED_FINAL" -> "Serie"
        else -> round
    }
}
