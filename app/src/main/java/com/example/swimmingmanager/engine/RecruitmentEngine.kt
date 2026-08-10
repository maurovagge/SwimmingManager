package com.example.swimmingmanager.engine

import com.example.swimmingmanager.data.AppDatabase
import com.example.swimmingmanager.data.Message
import com.example.swimmingmanager.data.Swimmer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

object RecruitmentEngine {

    suspend fun processAnnualRecruitment(db: AppDatabase, playerClubId: Int, currentYear: Int) {
        withContext(Dispatchers.IO) {

            val allClubsLight = db.clubDao().getAllClubsLight()
            val newSwimmers = mutableListOf<Swimmer>()
            val messagesToInsert = mutableListOf<Message>()

            if (WorldGenerator.firstNamesMap.isEmpty() || WorldGenerator.lastNamesMap.isEmpty()) return@withContext

            allClubsLight.forEach { club ->
                val newArrivalsCount = Random.nextInt(1, 3)

                repeat(newArrivalsCount) {
                    val newSwimmer = SwimmerGenerator.generateNewGen(
                        clubId = club.id,
                        nation = club.country,
                        clubPrestige = club.prestige,
                        firstNamesMap = WorldGenerator.firstNamesMap,
                        lastNamesMap = WorldGenerator.lastNamesMap
                    )
                    newSwimmers.add(newSwimmer)

                    // Se il giovane si unisce al club del giocatore, manda un messaggio
                    if (club.id == playerClubId) {
                        messagesToInsert.add(
                            Message(
                                title = "Nuovo Talento in Squadra!",
                                body = "${newSwimmer.firstName} ${newSwimmer.lastName} si è unito alle nostre giovanili. Promette bene!",
                                type = "RECRUIT",
                                year = currentYear,
                                week = 1
                            )
                        )
                    }
                }
            }

            if (newSwimmers.isNotEmpty()) {
                db.swimmerDao().insertSwimmers(newSwimmers)
            }
            
            if (messagesToInsert.isNotEmpty()) {
                messagesToInsert.forEach { db.messageDao().insertMessage(it) }
            }
        }
    }
}
