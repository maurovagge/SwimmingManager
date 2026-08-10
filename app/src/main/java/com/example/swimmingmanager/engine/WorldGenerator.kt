package com.example.swimmingmanager.engine

import android.content.Context
import com.example.swimmingmanager.data.Club
import com.example.swimmingmanager.data.Swimmer
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.random.Random

object WorldGenerator {

    val firstNamesMap = mutableMapOf<String, List<String>>()
    val lastNamesMap = mutableMapOf<String, List<String>>()
    private var namesLoaded = false

    fun loadNames(context: Context) {
        if (namesLoaded) return
        try {
            val reader = BufferedReader(InputStreamReader(context.assets.open("first&last_names.csv")))
            reader.readLine()
            reader.forEachLine { line ->
                val cols = line.split(",")
                if (cols.size >= 4) {
                    val nation = cols[0].trim()
                    val gender = cols[1].trim()
                    val type = cols[2].trim()
                    val rawNames = cols[3].trim().split(";")
                    val names = rawNames.map { it.replace(Regex("[A-Z]{3}$"), "").trim() }
                    
                    val key = "${nation}_${gender}"
                    if (type == "FirstName") {
                        val existing = firstNamesMap[key] ?: emptyList()
                        firstNamesMap[key] = existing + names
                    } else if (type == "LastName") {
                        val existing = lastNamesMap[key] ?: emptyList()
                        lastNamesMap[key] = existing + names
                    }
                }
            }
            namesLoaded = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun generateSwimmer(id: Int, nation: String, gender: String, clubId: Int, clubPrestige: Int): Swimmer {
        val angloNations = listOf("AUS", "GBR", "CAN", "RSA", "IRL")
        val lookupCode = if (firstNamesMap.containsKey("${nation}_${gender}")) nation 
                         else if (angloNations.contains(nation)) "USA" 
                         else nation

        val firstNamesList = firstNamesMap["${lookupCode}_${gender}"] ?: listOf("Unknown")
        val lastNamesList = lastNamesMap["${lookupCode}_${gender}"] ?: lastNamesMap["${lookupCode}_A"] ?: listOf("Swimmer")

        val age = Random.nextInt(14, 26)
        val ageFactor = (age - 14) / 12.0
        val prestigeBaseBonus = clubPrestige * 3.0
        var baseSkillLevel = Random.nextDouble(30.0, 55.0) + (ageFactor * 30.0) + prestigeBaseBonus
        baseSkillLevel = baseSkillLevel.coerceIn(30.0, 85.0)

        val potentialPool = Random.nextDouble(9.0, 25.0) + (maxOf(0, 26 - age) * 5.0) + (clubPrestige * 3.0)

        val styles = mutableListOf(
            baseSkillLevel * Random.nextDouble(0.2, 0.7),
            baseSkillLevel * Random.nextDouble(0.2, 0.7),
            baseSkillLevel * Random.nextDouble(0.2, 0.7),
            baseSkillLevel * Random.nextDouble(0.2, 0.7)
        )
        styles[Random.nextInt(4)] = baseSkillLevel * Random.nextDouble(0.85, 1.0)

        return Swimmer(
            id = id,
            firstName = firstNamesList.random(),
            lastName = lastNamesList.random(),
            nationality = nation,
            gender = gender,
            age = age,
            clubId = clubId,
            potentialPoints = potentialPool,
            sprintPower = baseSkillLevel * Random.nextDouble(0.7, 1.0),
            endurance = baseSkillLevel * Random.nextDouble(0.7, 1.0),
            technique = baseSkillLevel * Random.nextDouble(0.8, 1.0),
            speed = baseSkillLevel * Random.nextDouble(0.75, 1.0),
            resilience = Random.nextDouble(40.0, 99.0),
            adaptability = Random.nextDouble(40.0, 99.0),
            experience = (maxOf(0, age - 14) * (Random.nextDouble(1.0, 3.0) + clubPrestige * 1.5)).coerceIn(0.0, 100.0),
            freestyle = styles[0], backstroke = styles[1], breaststroke = styles[2], butterfly = styles[3],
            underwater = baseSkillLevel * Random.nextDouble(0.4, 0.9),
            tiredness = 0.0, moral = 5.0
        )
    }

    fun createWorldData(context: Context): Pair<List<Club>, List<Swimmer>> {
        loadNames(context)
        val clubsList = mutableListOf<Club>()
        val swimmersList = mutableListOf<Swimmer>()
        var currentClubId = 1
        var currentSwimmerId = 1

        try {
            val reader = BufferedReader(InputStreamReader(context.assets.open("clubs.csv")))
            reader.readLine()
            reader.forEachLine { line ->
                val cols = line.split(",")
                if (cols.size >= 5) {
                    val nation = cols[0].trim()
                    val region = cols[1].trim()
                    val city = cols[2].trim()
                    val clubName = cols[3].trim()
                    val prestige = cols[4].trim().toIntOrNull() ?: 3
                    
                    clubsList.add(Club(currentClubId, clubName, nation, city, region, prestige, prestige))
                    
                    // Aggiunta Elite Veterans per alzare il livello iniziale dei top club
                    val eliteCount = when (prestige) {
                        5 -> 2
                        4 -> 1
                        else -> 0
                    }
                    
                    repeat(eliteCount) {
                        val elite = SwimmerGenerator.generateEliteVeteran(
                            currentClubId, nation, firstNamesMap, lastNamesMap
                        )
                        swimmersList.add(elite.copy(id = currentSwimmerId++))
                    }

                    val athletesPerGender = 9 + (prestige * 2)
                    repeat(athletesPerGender) {
                        swimmersList.add(generateSwimmer(currentSwimmerId++, nation, "M", currentClubId, prestige))
                        swimmersList.add(generateSwimmer(currentSwimmerId++, nation, "F", currentClubId, prestige))
                    }
                    currentClubId++
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
        return Pair(clubsList, swimmersList)
    }
}
