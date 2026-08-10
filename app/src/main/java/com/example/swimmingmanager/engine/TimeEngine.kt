package com.example.swimmingmanager.engine

import com.example.swimmingmanager.data.Swimmer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.math.min
import kotlin.random.Random

object TimeEngine {

    // Main function: calculates a full week of training for a list of swimmers
    suspend fun advanceWeek(swimmers: List<Swimmer>): List<Swimmer> = coroutineScope {

        val chunks = swimmers.chunked(500)

        val deferredChunks = chunks.map { chunk ->
            async(Dispatchers.Default) {
                chunk.forEach { swimmer ->
                    applyTrainingEffects(swimmer)
                }
                chunk
            }
        }

        deferredChunks.awaitAll().flatten()
    }

    private fun applyTrainingEffects(swimmer: Swimmer) {
        // Base growth multipliers. Tweak these to balance the game speed
        val baseBoost = 0.12 
        val baseStyleGrowth = 0.15 

        var fatigueChange = 0.0

        // --- THE DEVELOPMENT POOL ---
        var remainingPool = swimmer.potentialPoints

        /**
         * Safely increases a skill, deducts the cost from the pool.
         * DIMINISHING RETURNS: If a skill is >= 90, it costs 3x more potential points.
         */
        fun trainSkill(currentValue: Double, boost: Double): Double {
            if (remainingPool <= 0.0 || currentValue >= 100.0) return currentValue

            // Se la skill è già eccellente (>= 90), il costo in punti potenziale triplica
            val costMultiplier = if (currentValue >= 90.0) 3.0 else 1.0
            
            // Calcoliamo quanto incremento possiamo permetterci con il pool rimanente
            val maxPossibleSkillGain = remainingPool / costMultiplier
            val skillGain = min(boost, maxPossibleSkillGain)
            
            // Il costo effettivo sottratto dal potenziale
            val potentialCost = skillGain * costMultiplier
            remainingPool = (remainingPool - potentialCost).coerceAtLeast(0.0)

            return min(currentValue + skillGain, 100.0)
        }

        // 1. APPLY PHYSIOLOGICAL TRAINING (Metabolisms)
        when (swimmer.currentTraining) {
            "ACTIVE_RECOVERY" -> {
                fatigueChange = -0.8
                swimmer.technique = trainSkill(swimmer.technique, baseBoost)
            }
            "AEROBIC_BASE" -> {
                fatigueChange = +0.6
                swimmer.endurance = trainSkill(swimmer.endurance, baseBoost * 3)
                swimmer.technique = trainSkill(swimmer.technique, baseBoost)
            }
            "VO2_MAX" -> {
                fatigueChange = +1.0 
                swimmer.endurance = trainSkill(swimmer.endurance, baseBoost * 2)
                swimmer.speed = trainSkill(swimmer.speed, baseBoost * 2)
            }
            "LACTATE_TOLERANCE" -> {
                fatigueChange = +1.0
                swimmer.speed = trainSkill(swimmer.speed, baseBoost * 2)
                swimmer.endurance = trainSkill(swimmer.endurance, baseBoost)
                swimmer.sprintPower = trainSkill(swimmer.sprintPower, baseBoost)
            }
            "LACTATE_PRODUCTION" -> {
                fatigueChange = +0.7
                swimmer.speed = trainSkill(swimmer.speed, baseBoost * 3)
                swimmer.sprintPower = trainSkill(swimmer.sprintPower, baseBoost * 2)
                swimmer.underwater = trainSkill(swimmer.underwater, baseBoost)
            }
            "PURE_SPEED" -> {
                fatigueChange = +0.1 
                swimmer.sprintPower = trainSkill(swimmer.sprintPower, baseBoost * 3)
                swimmer.technique = trainSkill(swimmer.technique, baseBoost * 2)
                swimmer.underwater = trainSkill(swimmer.underwater, baseBoost * 2)
                swimmer.speed = trainSkill(swimmer.speed, baseBoost)
            }
            "TECHNIQUE_DRILLS" -> {
                fatigueChange = -0.4 
                swimmer.technique = trainSkill(swimmer.technique, baseBoost * 3)
                swimmer.underwater = trainSkill(swimmer.underwater, baseBoost * 2)
            }
            "BALANCED" -> {
                fatigueChange = +0.5
                swimmer.speed = trainSkill(swimmer.speed, baseBoost)
                swimmer.endurance = trainSkill(swimmer.endurance, baseBoost)
                swimmer.technique = trainSkill(swimmer.technique, baseBoost)
                swimmer.sprintPower = trainSkill(swimmer.sprintPower, baseBoost)
            }
            "TAPERING" -> {
                if (swimmer.tiredness < 1.0) {
                    swimmer.endurance -= 0.5
                    swimmer.speed -= 0.5
                    swimmer.sprintPower -= 0.3
                }
                fatigueChange = -2.5 
                swimmer.moral += 1.0 
            }
        }

        // 2. APPLY STROKE FOCUS
        when (swimmer.focusStyle) {
            "FREESTYLE" -> swimmer.freestyle = trainSkill(swimmer.freestyle, baseStyleGrowth)
            "BACKSTROKE" -> swimmer.backstroke = trainSkill(swimmer.backstroke, baseStyleGrowth)
            "BREASTSTROKE" -> swimmer.breaststroke = trainSkill(swimmer.breaststroke, baseStyleGrowth)
            "BUTTERFLY" -> swimmer.butterfly = trainSkill(swimmer.butterfly, baseStyleGrowth)
            "MEDLEY" -> {
                val medleyBoost = baseStyleGrowth / 4.0
                swimmer.freestyle = trainSkill(swimmer.freestyle, medleyBoost)
                swimmer.backstroke = trainSkill(swimmer.backstroke, medleyBoost)
                swimmer.breaststroke = trainSkill(swimmer.breaststroke, medleyBoost)
                swimmer.butterfly = trainSkill(swimmer.butterfly, medleyBoost)
            }
            "DEFAULT" -> {
                when (swimmer.mainStyle) {
                    "SL" -> swimmer.freestyle = trainSkill(swimmer.freestyle, baseStyleGrowth)
                    "DO" -> swimmer.backstroke = trainSkill(swimmer.backstroke, baseStyleGrowth)
                    "RA" -> swimmer.breaststroke = trainSkill(swimmer.breaststroke, baseStyleGrowth)
                    "FA" -> swimmer.butterfly = trainSkill(swimmer.butterfly, baseStyleGrowth)
                }
            }
        }

        if (swimmer.isDeclining) {
            val decayFactor = 0.05 + ((swimmer.age - 26) * 0.01)
            swimmer.sprintPower -= decayFactor
            swimmer.speed -= decayFactor
            swimmer.endurance -= (decayFactor * 0.5)
        }

        // --- 3. FINALIZE THE WEEK ---
        swimmer.potentialPoints = remainingPool
        swimmer.tiredness = (swimmer.tiredness + fatigueChange).coerceIn(0.0, 5.0)
        swimmer.moral = swimmer.moral.coerceIn(0.0, 5.0)
    }

    fun processEndSeason(swimmers: List<Swimmer>): List<Swimmer> {
        swimmers.forEach { swimmer ->
            if (swimmer.isRetired) return@forEach 
            swimmer.age += 1
            if (swimmer.isDeclining) {
                val retirementChance = (swimmer.age - 25) * 0.05
                if (Random.nextDouble() < retirementChance) {
                    swimmer.isRetired = true
                    swimmer.currentTraining = "RETIRED"
                    return@forEach 
                }
            }
            if (!swimmer.isDeclining && swimmer.age >= 26) {
                val declineChance = (swimmer.age - 24) * 0.05
                if (Random.nextDouble() < declineChance) {
                    swimmer.isDeclining = true
                }
            }
        }
        return swimmers
    }
}