package com.example.swimmingmanager.engine

import com.example.swimmingmanager.data.Competition

object CalendarGenerator {

    // Generates the full 52-week calendar for a specific year and nation
    fun generateYearlyCalendar(nationCode: String, currentYear: Int): List<Competition> {
        val dict = CompetitionDictionaries.getDictionaryForNation(nationCode)
        val calendar = mutableListOf<Competition>()

        // Helper to define if the nation belongs to the European championships or Intercontinental
        val europeanNations = listOf("ITA", "GBR", "FRA", "GER", "HUN", "NED", "ESP", "SWE", "ROU", "LTU", "BEL", "SUI", "RUS")
        val isEuropean = europeanNations.contains(nationCode)
        val continentalPrefix = if (isEuropean) "European" else "Intercontinental"

        // ==========================================================
        // --- AUTUMN / WINTER (SHORT COURSE 25m) ---
        // ==========================================================

        // Week 4: Regional Trial 1 (No history)
        calendar.add(Competition(
            name = "Regional Trial 1 (SC)", week = 4, isShortCourse = true, tier = "REGIONAL",
            minAge = 14, maxAge = 99, isNationalTeamEvent = false, hasHistory = false, isTeamEvent = false, year = currentYear
        ))

        // Week 8: National Trophy 1
        calendar.add(Competition(
            name = dict["NAT_TROPHY_25_1"] ?: "National Trophy 1", week = 8, isShortCourse = true, tier = "NATIONAL",
            minAge = 14, maxAge = 99, isNationalTeamEvent = false, hasHistory = true, isTeamEvent = false, year = currentYear
        ))

        // Week 10: Regional Trophy SC (No history)
        calendar.add(Competition(
            name = "Regional Trophy (SC)", week = 10, isShortCourse = true, tier = "REGIONAL",
            minAge = 14, maxAge = 99, isNationalTeamEvent = false, hasHistory = false, isTeamEvent = false, year = currentYear
        ))

        // Week 12: Regional Trial 2 (No history)
        calendar.add(Competition(
            name = "Regional Trial 2 (SC)", week = 12, isShortCourse = true, tier = "REGIONAL",
            minAge = 14, maxAge = 99, isNationalTeamEvent = false, hasHistory = false, isTeamEvent = false, year = currentYear
        ))

        // Week 16: Winter Absolute National Championship
        calendar.add(Competition(
            name = dict["NAT_CHAMP_WINTER_25"] ?: "Winter National Championship", week = 16, isShortCourse = true, tier = "NATIONAL",
            minAge = 14, maxAge = 99, isNationalTeamEvent = false, hasHistory = true, isTeamEvent = false, year = currentYear
        ))

        // Week 18: National Team Championship (e.g., Coppa Brema)
        calendar.add(Competition(
            name = dict["NAT_TEAM_CHAMP_25"] ?: "National Club Championship", week = 18, isShortCourse = true, tier = "NATIONAL",
            minAge = 14, maxAge = 99, isNationalTeamEvent = false, hasHistory = true, isTeamEvent = true, year = currentYear
        ))

        // Week 22: Regional Finals
        calendar.add(Competition(
            name = "Regional Short Course Finals", week = 22, isShortCourse = true, tier = "REGIONAL",
            minAge = 14, maxAge = 99, isNationalTeamEvent = false, hasHistory = true, isTeamEvent = false, year = currentYear
        ))

        // Weeks 24, 25, 26: Youth National Criteria (Divided by age groups)
        calendar.add(Competition(name = "${dict["NAT_YOUTH_CRITERIA_25"]} (Ragazzi)", week = 24, isShortCourse = true, tier = "NATIONAL", minAge = 14, maxAge = 15, isNationalTeamEvent = false, hasHistory = true, isTeamEvent = false, year = currentYear))
        calendar.add(Competition(name = "${dict["NAT_YOUTH_CRITERIA_25"]} (Juniores)", week = 25, isShortCourse = true, tier = "NATIONAL", minAge = 16, maxAge = 17, isNationalTeamEvent = false, hasHistory = true, isTeamEvent = false, year = currentYear))
        calendar.add(Competition(name = "${dict["NAT_YOUTH_CRITERIA_25"]} (Cadetti)", week = 26, isShortCourse = true, tier = "NATIONAL", minAge = 18, maxAge = 19, isNationalTeamEvent = false, hasHistory = true, isTeamEvent = false, year = currentYear))


        // ==========================================================
        // --- SPRING / SUMMER (LONG COURSE 50m) ---
        // ==========================================================

        // Week 28: Regional Trial 1 (No history)
        calendar.add(Competition(
            name = "Regional Trial 1 (LC)", week = 28, isShortCourse = false, tier = "REGIONAL",
            minAge = 14, maxAge = 99, isNationalTeamEvent = false, hasHistory = false, isTeamEvent = false, year = currentYear
        ))

        // Week 32: National Trophy 1 (e.g., Settecolli)
        calendar.add(Competition(
            name = dict["NAT_TROPHY_50_1"] ?: "National Trophy 1 (LC)", week = 32, isShortCourse = false, tier = "NATIONAL",
            minAge = 14, maxAge = 99, isNationalTeamEvent = false, hasHistory = true, isTeamEvent = false, year = currentYear
        ))

        // Week 36: Spring Absolute National Championship (Moved to create better pacing)
        calendar.add(Competition(
            name = dict["NAT_CHAMP_SPRING_50"] ?: "Spring National Championship", week = 36, isShortCourse = false, tier = "NATIONAL",
            minAge = 14, maxAge = 99, isNationalTeamEvent = false, hasHistory = true, isTeamEvent = false, year = currentYear
        ))

        // Week 38: Regional Trophy LC (No history)
        calendar.add(Competition(
            name = "Regional Trophy (LC)", week = 38, isShortCourse = false, tier = "REGIONAL",
            minAge = 14, maxAge = 99, isNationalTeamEvent = false, hasHistory = false, isTeamEvent = false, year = currentYear
        ))

        // Week 40: Regional Trial 2 (LC) (No history)
        calendar.add(Competition(
            name = "Regional Trial 2 (LC)", week = 40, isShortCourse = false, tier = "REGIONAL",
            minAge = 14, maxAge = 99, isNationalTeamEvent = false, hasHistory = false, isTeamEvent = false, year = currentYear
        ))

        // Week 42: National Trophy 2
        calendar.add(Competition(
            name = dict["NAT_TROPHY_50_2"] ?: "National Trophy 2 (LC)", week = 42, isShortCourse = false, tier = "NATIONAL",
            minAge = 14, maxAge = 99, isNationalTeamEvent = false, hasHistory = true, isTeamEvent = false, year = currentYear
        ))

        // Week 44: Regional Finals LC
        calendar.add(Competition(
            name = "Regional Long Course Finals", week = 44, isShortCourse = false, tier = "REGIONAL",
            minAge = 14, maxAge = 99, isNationalTeamEvent = false, hasHistory = true, isTeamEvent = false, year = currentYear
        ))

        // Weeks 45, 46, 47: Youth Summer Championships
        calendar.add(Competition(name = "${dict["NAT_YOUTH_CHAMP_50"]} (Ragazzi)", week = 45, isShortCourse = false, tier = "NATIONAL", minAge = 14, maxAge = 15, isNationalTeamEvent = false, hasHistory = true, isTeamEvent = false, year = currentYear))
        calendar.add(Competition(name = "${dict["NAT_YOUTH_CHAMP_50"]} (Juniores)", week = 46, isShortCourse = false, tier = "NATIONAL", minAge = 16, maxAge = 17, isNationalTeamEvent = false, hasHistory = true, isTeamEvent = false, year = currentYear))
        calendar.add(Competition(name = "${dict["NAT_YOUTH_CHAMP_50"]} (Cadetti)", week = 47, isShortCourse = false, tier = "NATIONAL", minAge = 18, maxAge = 19, isNationalTeamEvent = false, hasHistory = true, isTeamEvent = false, year = currentYear))


        // ==========================================================
        // --- INTERNATIONAL EVENTS (The 4-Year Matrix) ---
        // ==========================================================

        // cycleYear will be 1, 2, 3, or 0 (0 is the Olympic Year)
        val cycleYear = currentYear % 4

        // 1. WINTER INTERNATIONALS (Week 20 - Short Course 25m)
        if (cycleYear == 1 || cycleYear == 3) {
            // Years 1 & 3: Continental SC Championships
            calendar.add(Competition(name = "$continentalPrefix SC Championships", week = 20, isShortCourse = true, tier = "INTERNATIONAL", minAge = 14, maxAge = 99, isNationalTeamEvent = true, hasHistory = true, isTeamEvent = false, year = currentYear))
        } else {
            // Years 2 & 4 (Olympic Year): World SC Championships
            calendar.add(Competition(name = "World SC Championships", week = 20, isShortCourse = true, tier = "INTERNATIONAL", minAge = 14, maxAge = 99, isNationalTeamEvent = true, hasHistory = true, isTeamEvent = false, year = currentYear))
        }

        // 2. SUMMER INTERNATIONALS (Week 48 - Long Course 50m)
        if (cycleYear == 0) {
            // Year 4: Olympic Games
            calendar.add(Competition(name = "Olympic Games", week = 48, isShortCourse = false, tier = "INTERNATIONAL", minAge = 14, maxAge = 99, isNationalTeamEvent = true, hasHistory = true, isTeamEvent = false, year = currentYear))
        } else if (cycleYear == 1 || cycleYear == 3) {
            // Years 1 & 3: World LC Championships
            calendar.add(Competition(name = "World Aquatics Championships", week = 48, isShortCourse = false, tier = "INTERNATIONAL", minAge = 14, maxAge = 99, isNationalTeamEvent = true, hasHistory = true, isTeamEvent = false, year = currentYear))
        } else if (cycleYear == 2) {
            // Year 2: Continental LC Championships
            calendar.add(Competition(name = "$continentalPrefix LC Championships", week = 48, isShortCourse = false, tier = "INTERNATIONAL", minAge = 14, maxAge = 99, isNationalTeamEvent = true, hasHistory = true, isTeamEvent = false, year = currentYear))
        }

        // 3. JUNIOR INTERNATIONALS (Week 50 - Alternating SC/LC)
        // Juniors and Cadetti (16-19)
        if (currentYear % 2 == 0) {
            // Even years (2, 4): Junior Worlds LC
            calendar.add(Competition(name = "World Junior Championships (LC)", week = 50, isShortCourse = false, tier = "INTERNATIONAL", minAge = 16, maxAge = 19, isNationalTeamEvent = true, hasHistory = true, isTeamEvent = false, year = currentYear))
        } else {
            // Odd years (1, 3): Junior Worlds SC
            calendar.add(Competition(name = "World Junior Championships (SC)", week = 50, isShortCourse = true, tier = "INTERNATIONAL", minAge = 16, maxAge = 19, isNationalTeamEvent = true, hasHistory = true, isTeamEvent = false, year = currentYear))
        }

        // Return the calendar sorted strictly by week so the timeline is chronological
        return calendar.sortedBy { it.week }
    }
}