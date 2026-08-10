package ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.swimmingmanager.data.Competition
import androidx.activity.compose.BackHandler

@Composable
fun CalendarScreen(
    competitions: List<Competition>,
    onBackClick: () -> Unit,
    onCompetitionClick: (Competition) -> Unit
) {
    // Intercepts the physical back button of the Android device
    BackHandler(onBack = onBackClick)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA)) // Very light gray/off-white background
    ) {
        // --- Header with Back Button ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Text(
                text = "◁ Indietro",
                color = Color(0xFF1976D2),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { onBackClick() }
                    .padding(end = 16.dp, top = 8.dp, bottom = 8.dp)
            )
            Text(
                text = "Calendario",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1E1E1E)
            )
        }

        // --- Scrolling List of Competitions ---
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(competitions) { comp ->
                CompetitionCard(competition = comp, onClick = { onCompetitionClick(comp) })
            }
        }
    }
}

@Composable
fun CompetitionCard(competition: Competition, onClick: () -> Unit) {
    // Adjusted colors for better contrast and readability
    val tierColor = when (competition.tier) {
        "REGIONAL" -> Color(0xFF2E7D32) // Darker Forest Green
        "NATIONAL" -> Color(0xFF1976D2) // Deep Material Blue
        "INTERNATIONAL" -> Color(0xFFF57F17) // Deep Amber/Gold (Highly visible)
        else -> Color.Gray
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        // THE NEW BORDER! Colored with the tier color, slightly transparent
        border = BorderStroke(1.5.dp, tierColor.copy(alpha = 0.6f)),
        colors = CardDefaults.cardColors(containerColor = Color.White), // Force white background inside
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side: Week Number Circle
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(tierColor.copy(alpha = 0.15f)) // Very light background of the tier color
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "WK", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = tierColor)
                    Text(text = "${competition.week}", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = tierColor)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Middle: Competition Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = competition.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF212121),
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Details Row (Course length and Categories)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val courseText = if (competition.isShortCourse) "25m Short Course" else "50m Long Course"
                    Text(
                        text = courseText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF757575)
                    )

                    if (competition.isTeamEvent) {
                        Text(
                            text = " • Team Event",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD32F2F) // Deep Red for team events
                        )
                    }
                }
            }
        }
    }
}