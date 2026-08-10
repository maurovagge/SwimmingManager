package com.example.swimmingmanager.engine

import com.example.swimmingmanager.data.Swimmer
import kotlin.random.Random

object SwimmerGenerator {

    fun generateNewGen(
        clubId: Int,
        nation: String,
        clubPrestige: Int,
        firstNamesMap: Map<String, List<String>>,
        lastNamesMap: Map<String, List<String>>
    ): Swimmer {
        val rand = Random.nextDouble()
        
        // Definiamo la "scaletta" di potenziale e boost iniziale
        val (potentialRange, skillBoost) = when {
            rand < 0.02 -> {
                // FENOMENO ASSOLUTO (2% di probabilità)
                260.0..335.0 to 3.0
            }
            rand < 0.10 -> {
                // GRANDE TALENTO (8% di probabilità)
                160.0..250.0 to 2.0
            }
            rand < 0.30 -> {
                // BUONA PROMESSA (20% di probabilità)
                120.0..160.0 to 0.5
            }
            else -> {
                // STANDARD (70% di probabilità)
                80.0..120.0 to 0.0
            }
        }

        return generateCustomSwimmer(
            clubId, nation, clubPrestige, firstNamesMap, lastNamesMap, 
            ageRange = 14..15, 
            potentialRange = potentialRange, 
            skillBoost = skillBoost
        )
    }

    /**
     * Il "Prodigio": 14-15 anni, potenziale ELITE ASSOLUTO
     * Usato per eventi speciali o generazioni manuali mirate
     */
    fun generateYoungProdigy(
        clubId: Int, nation: String, clubPrestige: Int,
        firstNamesMap: Map<String, List<String>>, lastNamesMap: Map<String, List<String>>
    ): Swimmer {
        return generateCustomSwimmer(clubId, nation, clubPrestige, firstNamesMap, lastNamesMap, 
            ageRange = 14..14, 
            potentialRange = 250.0..335.0,
            skillBoost = 3.0)
    }

    /**
     * La "Promessa": 16-17 anni, già forte a livello nazionale
     */
    fun generateRisingStar(
        clubId: Int, nation: String, clubPrestige: Int,
        firstNamesMap: Map<String, List<String>>, lastNamesMap: Map<String, List<String>>
    ): Swimmer {
        return generateCustomSwimmer(clubId, nation, clubPrestige, firstNamesMap, lastNamesMap, 
            ageRange = 16..16, 
            potentialRange = 120.0..180.0, 
            skillBoost = 12.0)
    }

    /**
     * Il "Veterano": 22-25 anni, pilastro della squadra
     */
    fun generateSolidVeteran(
        clubId: Int, nation: String, clubPrestige: Int,
        firstNamesMap: Map<String, List<String>>, lastNamesMap: Map<String, List<String>>
    ): Swimmer {
        return generateCustomSwimmer(clubId, nation, clubPrestige, firstNamesMap, lastNamesMap, 
            ageRange = 22..25, 
            potentialRange = 10.0..27.0,
            skillBoost = 30.0)
    }

    /**
     * Il "Fuoriclasse": 26-30 anni, 85-93 overall, 0 potenziale.
     * Generato solo alla creazione del mondo per i top club per alzare il livello iniziale.
     * Altamente specializzato in uno stile.
     */
    fun generateEliteVeteran(
        clubId: Int, nation: String,
        firstNamesMap: Map<String, List<String>>, lastNamesMap: Map<String, List<String>>
    ): Swimmer {
        val gender = if (Random.nextBoolean()) "M" else "F"
        val firstNamesList = firstNamesMap["${nation}_${gender}"] ?: listOf("Unknown")
        val lastNamesList = lastNamesMap["${nation}_${gender}"] ?: lastNamesMap["${nation}_A"] ?: listOf("Swimmer")

        val age = Random.nextInt(26, 31)
        // Per ottenere un overall tra 85 e 93, impostiamo un livello base fisico alto
        val baseSkillLevel = Random.nextDouble(90.0, 96.0)
        
        // Generazione stili specializzati
        val styleValues = mutableListOf<Double>()
        styleValues.add(baseSkillLevel * Random.nextDouble(0.96, 1.0)) // Main Style
        styleValues.add(Random.nextDouble(60.0, 70.0))                 // Secondary Style
        styleValues.add(Random.nextDouble(30.0, 50.0))                 // Weak Style 1
        styleValues.add(Random.nextDouble(30.0, 50.0))                 // Weak Style 2
        styleValues.shuffle()

        return Swimmer(
            firstName = firstNamesList.random(),
            lastName = lastNamesList.random(),
            nationality = nation,
            gender = gender,
            age = age,
            clubId = clubId,
            potentialPoints = 0.0,
            sprintPower = baseSkillLevel * Random.nextDouble(0.9, 1.0),
            endurance = baseSkillLevel * Random.nextDouble(0.9, 1.0),
            technique = baseSkillLevel * Random.nextDouble(0.9, 1.0),
            speed = baseSkillLevel * Random.nextDouble(0.9, 1.0),
            resilience = Random.nextDouble(85.0, 99.0),
            adaptability = Random.nextDouble(75.0, 99.0),
            experience = Random.nextDouble(85.0, 100.0),
            butterfly = styleValues[3], freestyle = styleValues[0], breaststroke = styleValues[2], backstroke = styleValues[1],
            underwater = baseSkillLevel * Random.nextDouble(0.9, 1.0),
            tiredness = 0.0, moral = 5.0
        )
    }

    private fun generateCustomSwimmer(
        clubId: Int, nation: String, clubPrestige: Int,
        firstNamesMap: Map<String, List<String>>, lastNamesMap: Map<String, List<String>>,
        ageRange: IntRange, potentialRange: ClosedRange<Double>, skillBoost: Double
    ): Swimmer {
        val gender = if (Random.nextBoolean()) "M" else "F"
        val firstNamesList = firstNamesMap["${nation}_${gender}"] ?: listOf("Unknown")
        val lastNamesList = lastNamesMap["${nation}_${gender}"] ?: lastNamesMap["${nation}_A"] ?: listOf("Swimmer")

        val age = ageRange.random()
        val ageFactor = (age - 14) / 12.0
        // Calcolo abilità base con boost maggiorato
        val baseSkillLevel = (Random.nextDouble(45.0, 60.0) + (ageFactor * 25.0) + (clubPrestige * 2.0) + skillBoost).coerceIn(30.0, 95.0)
        
        val styleValues = List(4) { baseSkillLevel * Random.nextDouble(0.4, 0.7) }.toMutableList()
        styleValues[Random.nextInt(4)] = baseSkillLevel * Random.nextDouble(0.92, 1.0)

        return Swimmer(
            firstName = firstNamesList.random(),
            lastName = lastNamesList.random(),
            nationality = nation,
            gender = gender,
            age = age,
            clubId = clubId,
            potentialPoints = Random.nextDouble(potentialRange.start, potentialRange.endInclusive),
            sprintPower = baseSkillLevel * Random.nextDouble(0.8, 1.0),
            endurance = baseSkillLevel * Random.nextDouble(0.8, 1.0),
            technique = baseSkillLevel * Random.nextDouble(0.85, 1.0),
            speed = baseSkillLevel * Random.nextDouble(0.8, 1.0),
            resilience = Random.nextDouble(60.0, 99.0),
            adaptability = Random.nextDouble(60.0, 99.0),
            experience = (ageFactor * 50.0 + Random.nextDouble(10.0, 25.0)).coerceIn(0.0, 100.0),
            butterfly = styleValues[3], freestyle = styleValues[0], breaststroke = styleValues[2], backstroke = styleValues[1],
            underwater = baseSkillLevel * Random.nextDouble(0.6, 0.95),
            tiredness = 0.0, moral = 5.0
        )
    }
}
