package com.example.swimmingmanager.engine

import com.example.swimmingmanager.data.Swimmer

object RelayTeamSelector {

    /**
     * Selects the best 4 swimmers for a relay event by simulating their actual performance.
     * Logic: It doesn't just look at skills, it calculates the projected time (Projected Performance).
     */
    fun selectTeamForRelay(teamRoster: List<Swimmer>, eventId: String): List<Swimmer>? {
        // 1. Filter out duplicates (Safety Shield) and ensure we have at least 4 athletes
        val distinctRoster = teamRoster.distinctBy { it.id }
        if (distinctRoster.size < 4) return null

        val isMedley = eventId.contains("MEDLEY")

        return if (isMedley) {
            selectMedleyTeam(distinctRoster)
        } else {
            selectFreestyleTeam(distinctRoster)
        }
    }

    /**
     * Freestyle Strategy: Takes the 4 fastest swimmers based on their projected freestyle time.
     */
    private fun selectFreestyleTeam(roster: List<Swimmer>): List<Swimmer> {
        return roster.sortedBy { swimmer ->
            // We simulate a 100m Free to see who is actually faster RIGHT NOW.
            // A lower time is better, so we use sortedBy (ascending).
            calculateProjectedTime(swimmer, Stroke.FREESTYLE)
        }.take(4)
    }

    /**
     * Medley Strategy: Assigns the best specialist for each leg,
     * ensuring no swimmer is picked twice.
     */
    private fun selectMedleyTeam(roster: List<Swimmer>): List<Swimmer>? {
        val availableSwimmers = roster.toMutableList()
        val selectedTeam = mutableListOf<Swimmer>()

        // 1. Best Backstroker
        val bestBack = availableSwimmers.minByOrNull { calculateProjectedTime(it, Stroke.BACKSTROKE) } ?: return null
        selectedTeam.add(bestBack)
        availableSwimmers.remove(bestBack)

        // 2. Best Breastroker among remaining
        val bestBreast = availableSwimmers.minByOrNull { calculateProjectedTime(it, Stroke.BREASTSTROKE) } ?: return null
        selectedTeam.add(bestBreast)
        availableSwimmers.remove(bestBreast)

        // 3. Best Flyer among remaining
        val bestFly = availableSwimmers.minByOrNull { calculateProjectedTime(it, Stroke.BUTTERFLY) } ?: return null
        selectedTeam.add(bestFly)
        availableSwimmers.remove(bestFly)

        // 4. Best Freestyler to close the relay
        val bestFree = availableSwimmers.minByOrNull { calculateProjectedTime(it, Stroke.FREESTYLE) } ?: return null
        selectedTeam.add(bestFree)

        return if (selectedTeam.size == 4) selectedTeam else null
    }

    /**
     * HELPER: Calculates a projected time for selection purposes.
     * This mimics the RaceEngine logic but is used only for AI decision making.
     */
    private fun calculateProjectedTime(swimmer: Swimmer, stroke: Stroke): Int {
        // We use a dummy 100m event to compare athletes on a standard distance.
        val dummyEvent = SwimmingEvent(id = "SELECT", distance = 100, stroke = stroke, gender = if(swimmer.gender=="M")Gender.M else Gender.F)

        // IMPORTANT: We simulate with current tiredness.
        // If a champion is exhausted, the AI will pick a fresher second-tier swimmer.
        return RaceEngine.simulateRace(swimmer, dummyEvent, isShortCourse = false)
    }
}