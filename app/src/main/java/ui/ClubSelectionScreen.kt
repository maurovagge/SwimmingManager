package ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.swimmingmanager.data.Club

// 1. Screen that shows the complete list of clubs
@Composable
fun ClubSelectionScreen(
    clubs: List<Club>,
    onClubSelected: (Club) -> Unit
) {
    // 1. SEARCH STATE: this variable remembers what's in the search bar
    var searchQuery by remember { mutableStateOf("") }

    var clubToConfirm by remember { mutableStateOf<Club?>(null) }

    // 2. This filters list everytime searchQuery changes
    val filteredClubs = clubs.filter { club ->
        club.name.contains(searchQuery, ignoreCase = true) ||
                club.city.contains(searchQuery, ignoreCase = true) ||
                club.region.contains(searchQuery, ignoreCase = true)||
                club.country.contains(searchQuery, ignoreCase = true)
    }

    if (clubToConfirm != null) {
        AlertDialog(
            onDismissRequest = { clubToConfirm = null }, // A click out of the pop-up leads to its closure
            title = { Text(text = "Firma il Contratto") },
            text = {
                Text(text = "Sei sicuro di voler diventare il manager di ${clubToConfirm?.name}?\nQuesta scelta è irrevocabile per questa partita.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClubSelected(clubToConfirm!!) // Pass the selected club to MainActivity
                        clubToConfirm = null // Close pop-up
                    }
                ) {
                    Text("Conferma")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { clubToConfirm = null }
                ) {
                    Text("Annulla")
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

        Text(
            text = "Scegli il tuo Club",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // 3. Textfield
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { newText -> searchQuery = newText }, // State update
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            label = { Text("Cerca per nome, città, regione o nazione...") },
            singleLine = true,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search, // Standard searching icon
                    contentDescription = "Cerca"
                )
            }
        )

        // 4. List is based on filtered clubs, LazyColumn is the Jetpack Compose equivalent of RecycleView
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp) // Space between lines
        ) {
            items(filteredClubs) { club ->
                ClubRow(club = club, onClick = { clubToConfirm=club })
            }

            if (filteredClubs.isEmpty()) {
                item {
                    Text(
                        text = "Nessun club trovato per '$searchQuery'",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

// 2. Card for every club
@Composable
fun ClubRow(club: Club, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }, // This makes the line clickable
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left column shows club name and geographical position
            Column {
                Text(
                    text = club.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${club.city}, ${club.region} (${club.country})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Right column shows level of prestige
            Text(
                text = "⭐".repeat(club.prestige),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}