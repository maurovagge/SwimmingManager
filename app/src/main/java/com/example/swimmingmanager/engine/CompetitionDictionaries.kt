package com.example.swimmingmanager.engine

object CompetitionDictionaries {

    // Helper function to route the calendar generator to the correct localized dictionary
    // We will add all 21 nations here as we build them
    fun getDictionaryForNation(nationCode: String): Map<String, String> {
        return when (nationCode) {
            "ITA" -> italy
            "USA" -> usa
            "AUS" -> australia
            "GBR" -> greatBritain
            "FRA" -> france
            "CHN" -> china
            "JPN" -> japan
            "CAN" -> canada
            "GER" -> germany
            "HUN" -> hungary
            "NED" -> netherlands
            "BRA" -> brazil
            "ESP" -> spain
            "SWE" -> sweden
            "RSA" -> southAfrica
            "KOR" -> southKorea
            "ROU" -> romania
            "LTU" -> lithuania
            "BEL" -> belgium
            "SUI" -> switzerland
            "RUS" -> russia
            // Default fallback if a nation is not heavily localized
            else -> usa
        }
    }

    // --- 1. ITALY (ITA) ---
    private val italy = mapOf(
        // Short Course (25m)
        "NAT_TROPHY_25_1" to "Trofeo Nico Sapio",
        "NAT_TROPHY_25_2" to "Trofeo Mussi-Lombardi-Femiano",
        "NAT_CHAMP_WINTER_25" to "Campionato Italiano Assoluto Invernale",
        "NAT_TEAM_CHAMP_25" to "Coppa Caduti di Brema", // Team Event
        "NAT_YOUTH_CRITERIA_25" to "Criteria Nazionali Giovanili",
        // Long Course (50m)
        "NAT_TROPHY_50_1" to "Trofeo Città di Milano",
        "NAT_TROPHY_50_2" to "Trofeo Internazionale Settecolli",
        "NAT_CHAMP_SPRING_50" to "Campionato Italiano Assoluto Primaverile",
        "NAT_YOUTH_CHAMP_50" to "Campionato Italiano di Categoria Estivo"
    )

    // --- 2. UNITED STATES (USA) ---
    private val usa = mapOf(
        // Short Course (25m - replacing SCY for game logic)
        "NAT_TROPHY_25_1" to "Speedo Winter Junior Nationals",
        "NAT_TROPHY_25_2" to "Toyota US Open Championships",
        "NAT_CHAMP_WINTER_25" to "Speedo Winter Championships",
        "NAT_TEAM_CHAMP_25" to "NCSA National Club Team Champs", // Team Event
        "NAT_YOUTH_CRITERIA_25" to "NCSA Spring Championships",
        // Long Course (50m)
        "NAT_TROPHY_50_1" to "TYR Pro Swim Series",
        "NAT_TROPHY_50_2" to "Atlanta Classic",
        "NAT_CHAMP_SPRING_50" to "Phillips 66 National Championships",
        "NAT_YOUTH_CHAMP_50" to "Speedo Junior National Championships"
    )

    // --- 3. AUSTRALIA (AUS) ---
    private val australia = mapOf(
        // Short Course (25m)
        "NAT_TROPHY_25_1" to "Sydney Sprints SC",
        "NAT_TROPHY_25_2" to "Melbourne SC Grand Prix",
        "NAT_CHAMP_WINTER_25" to "Australian Short Course Championships",
        "NAT_TEAM_CHAMP_25" to "Australian National Club Championships", // Team Event
        "NAT_YOUTH_CRITERIA_25" to "Australian Age SC Championships",
        // Long Course (50m)
        "NAT_TROPHY_50_1" to "NSW State Open Championships",
        "NAT_TROPHY_50_2" to "Brisbane Aquatic Super Series",
        "NAT_CHAMP_SPRING_50" to "Australian Swimming Trials",
        "NAT_YOUTH_CHAMP_50" to "Australian Age LC Championships"
    )

    // --- 4. GREAT BRITAIN (GBR) ---
    private val greatBritain = mapOf(
        // Short Course (25m)
        "NAT_TROPHY_25_1" to "Swim England National Winter Champs",
        "NAT_TROPHY_25_2" to "Scottish National SC Champs",
        "NAT_CHAMP_WINTER_25" to "British Swimming Winter Championships",
        "NAT_TEAM_CHAMP_25" to "National Arena Swimming League Final", // Team Event
        "NAT_YOUTH_CRITERIA_25" to "Swim England SC Junior Champs",
        // Long Course (50m)
        "NAT_TROPHY_50_1" to "Edinburgh International Swim Meet",
        "NAT_TROPHY_50_2" to "AP Race London International",
        "NAT_CHAMP_SPRING_50" to "British Swimming Championships",
        "NAT_YOUTH_CHAMP_50" to "British Summer Championships"
    )

    // --- 5. FRANCE (FRA) ---
    private val france = mapOf(
        // Short Course (25m)
        "NAT_TROPHY_25_1" to "Meeting National d'Automne",
        "NAT_TROPHY_25_2" to "Meeting de l'Ouest 25m",
        "NAT_CHAMP_WINTER_25" to "Championnats de France 25m",
        "NAT_TEAM_CHAMP_25" to "Championnats de France Interclubs", // Team Event
        "NAT_YOUTH_CRITERIA_25" to "Championnats de France Jeunes 25m",
        // Long Course (50m)
        "NAT_TROPHY_50_1" to "FFN Golden Tour Camille Muffat",
        "NAT_TROPHY_50_2" to "Meeting Open Méditerranée",
        "NAT_CHAMP_SPRING_50" to "Championnats de France Elite",
        "NAT_YOUTH_CHAMP_50" to "Championnats de France Juniors"
    )

    // --- 6. CHINA (CHN) ---
    private val china = mapOf(
        // Short Course (25m)
        "NAT_TROPHY_25_1" to "Chinese Winter Swimming Championships",
        "NAT_TROPHY_25_2" to "National Swimming Club Cup",
        "NAT_CHAMP_WINTER_25" to "Chinese National Short Course Championships",
        "NAT_TEAM_CHAMP_25" to "Chinese National Interclub Championships", // Team Event
        "NAT_YOUTH_CRITERIA_25" to "National Youth Short Course Championships",
        // Long Course (50m)
        "NAT_TROPHY_50_1" to "Chinese Spring Swimming Championships",
        "NAT_TROPHY_50_2" to "National Swimming Title Autumn Meet",
        "NAT_CHAMP_SPRING_50" to "Chinese National Swimming Championships",
        "NAT_YOUTH_CHAMP_50" to "National Youth Swimming Championships"
    )

    // --- 7. JAPAN (JPN) ---
    private val japan = mapOf(
        // Short Course (25m)
        "NAT_TROPHY_25_1" to "Tokyo SC Invitational",
        "NAT_TROPHY_25_2" to "Japan Open SC Meet",
        "NAT_CHAMP_WINTER_25" to "Japan Short Course Championships",
        "NAT_TEAM_CHAMP_25" to "Japan Inter-College/Club SC Championships", // Team Event
        "NAT_YOUTH_CRITERIA_25" to "Japan Junior SC Olympic Cup",
        // Long Course (50m)
        "NAT_TROPHY_50_1" to "Kosuke Kitajima Cup",
        "NAT_TROPHY_50_2" to "Japan Open",
        "NAT_CHAMP_SPRING_50" to "Japan Swim (National Championships)",
        "NAT_YOUTH_CHAMP_50" to "Japan Junior Olympic Cup (Summer)"
    )

    // --- 8. CANADA (CAN) ---
    private val canada = mapOf(
        // Short Course (25m)
        "NAT_TROPHY_25_1" to "Odlum Brown Grand Prix",
        "NAT_TROPHY_25_2" to "Eastern/Western Canadian SC Championships",
        "NAT_CHAMP_WINTER_25" to "Swimming Canada Winter Championships",
        "NAT_TEAM_CHAMP_25" to "Canadian Club SC Challenge", // Team Event
        "NAT_YOUTH_CRITERIA_25" to "Canadian Junior SC Championships",
        // Long Course (50m)
        "NAT_TROPHY_50_1" to "Mel Zajac Jr. International",
        "NAT_TROPHY_50_2" to "Canadian Swimming Open",
        "NAT_CHAMP_SPRING_50" to "Bell Canadian Swimming Trials",
        "NAT_YOUTH_CHAMP_50" to "Canadian Junior and Senior Championships"
    )

    // --- 9. GERMANY (GER) ---
    private val germany = mapOf(
        // Short Course (25m)
        "NAT_TROPHY_25_1" to "ISF Aachen",
        "NAT_TROPHY_25_2" to "Schwimmfest Rostock",
        "NAT_CHAMP_WINTER_25" to "Deutsche Kurzbahnmeisterschaften (DKM)",
        "NAT_TEAM_CHAMP_25" to "Deutscher Mannschaftswettbewerb Schwimmen (DMS)", // Team Event
        "NAT_YOUTH_CRITERIA_25" to "Deutsche Kurzbahnmeisterschaften der Jugend",
        // Long Course (50m)
        "NAT_TROPHY_50_1" to "Berlin Swim Open",
        "NAT_TROPHY_50_2" to "Magdeburg Swim Meet",
        "NAT_CHAMP_SPRING_50" to "Deutsche Meisterschaften Schwimmen",
        "NAT_YOUTH_CHAMP_50" to "Deutsche Jahrgangsmeisterschaften (DJM)"
    )

    // --- 10. HUNGARY (HUN) ---
    private val hungary = mapOf(
        // Short Course (25m)
        "NAT_TROPHY_25_1" to "Győr SC Open",
        "NAT_TROPHY_25_2" to "Budapest SC Meet",
        "NAT_CHAMP_WINTER_25" to "Hungarian Short Course Championships",
        "NAT_TEAM_CHAMP_25" to "Hungarian National Club Championships", // Team Event
        "NAT_YOUTH_CRITERIA_25" to "Hungarian Junior SC Championships",
        // Long Course (50m)
        "NAT_TROPHY_50_1" to "Budapest Open",
        "NAT_TROPHY_50_2" to "Debrecen Swim Cup",
        "NAT_CHAMP_SPRING_50" to "Hungarian National Championships",
        "NAT_YOUTH_CHAMP_50" to "Hungarian Age Group Championships"
    )

    // --- 11. NETHERLANDS (NED) ---
    private val netherlands = mapOf(
        // Short Course (25m)
        "NAT_TROPHY_25_1" to "Amsterdam Swim Cup (SC)",
        "NAT_TROPHY_25_2" to "Rotterdam Qualification Meet (SC)",
        "NAT_CHAMP_WINTER_25" to "ONK Kortebaan (Dutch SC Championships)",
        "NAT_TEAM_CHAMP_25" to "KNZB Nationale Zwemcompetitie", // Team Event
        "NAT_YOUTH_CRITERIA_25" to "ONK Jeugd Kortebaan",
        // Long Course (50m)
        "NAT_TROPHY_50_1" to "Eindhoven Qualification Meet",
        "NAT_TROPHY_50_2" to "Hague Grand Prix",
        "NAT_CHAMP_SPRING_50" to "Dutch National Championships (LC)",
        "NAT_YOUTH_CHAMP_50" to "Dutch Junior Championships"
    )

    // --- 12. BRAZIL (BRA) ---
    private val brazil = mapOf(
        // Short Course (25m)
        "NAT_TROPHY_25_1" to "Torneio Open de Natação",
        "NAT_TROPHY_25_2" to "Copa CBDA",
        "NAT_CHAMP_WINTER_25" to "Troféu José Finkel",
        "NAT_TEAM_CHAMP_25" to "Campeonato Brasileiro Interclubes", // Team Event
        "NAT_YOUTH_CRITERIA_25" to "Troféu Carlos Campos Sobrinho",
        // Long Course (50m)
        "NAT_TROPHY_50_1" to "Copa Mercosul",
        "NAT_TROPHY_50_2" to "Troféu Maria Lenk (Trials)",
        "NAT_CHAMP_SPRING_50" to "Troféu Brasil de Natação",
        "NAT_YOUTH_CHAMP_50" to "Troféu Julio de Lamare"
    )

    // --- 13. SPAIN (ESP) ---
    private val spain = mapOf(
        // Short Course (25m)
        "NAT_TROPHY_25_1" to "Grand Prix Ciudad de Barcelona (SC)",
        "NAT_TROPHY_25_2" to "Trofeo Internacional Castalia",
        "NAT_CHAMP_WINTER_25" to "Campeonato de España de Invierno (25m)",
        "NAT_TEAM_CHAMP_25" to "Copa de España de Clubes", // Team Event
        "NAT_YOUTH_CRITERIA_25" to "Campeonato de España Infantil (25m)",
        // Long Course (50m)
        "NAT_TROPHY_50_1" to "Mare Nostrum - Barcelona",
        "NAT_TROPHY_50_2" to "Open de Madrid",
        "NAT_CHAMP_SPRING_50" to "Campeonato de España Open de Primavera",
        "NAT_YOUTH_CHAMP_50" to "Campeonato de España Junior y Absoluto"
    )

    // --- 14. SWEDEN (SWE) ---
    private val sweden = mapOf(
        // Short Course (25m)
        "NAT_TROPHY_25_1" to "Swedish Swim Games",
        "NAT_TROPHY_25_2" to "Stockholm SC Invitational",
        "NAT_CHAMP_WINTER_25" to "Svenska Mästerskapen (SM) Kortbana",
        "NAT_TEAM_CHAMP_25" to "Svenska Simkampen", // Team Event
        "NAT_YOUTH_CRITERIA_25" to "JSM Kortbana",
        // Long Course (50m)
        "NAT_TROPHY_50_1" to "Swim Open Stockholm",
        "NAT_TROPHY_50_2" to "Malmö Meet",
        "NAT_CHAMP_SPRING_50" to "Svenska Mästerskapen (SM) Långbana",
        "NAT_YOUTH_CHAMP_50" to "JSM Långbana"
    )

    // --- 15. SOUTH AFRICA (RSA) ---
    private val southAfrica = mapOf(
        // Short Course (25m)
        "NAT_TROPHY_25_1" to "Durban SC Grand Prix",
        "NAT_TROPHY_25_2" to "Pretoria SC Invite",
        "NAT_CHAMP_WINTER_25" to "SA National Short Course Championships",
        "NAT_TEAM_CHAMP_25" to "SA Inter-Club SC Challenge", // Team Event
        "NAT_YOUTH_CRITERIA_25" to "SA National Junior Age Groups SC",
        // Long Course (50m)
        "NAT_TROPHY_50_1" to "Grand Prix Series - Stellenbosch",
        "NAT_TROPHY_50_2" to "Grand Prix Series - Nelspruit",
        "NAT_CHAMP_SPRING_50" to "SA National Aquatic Championships",
        "NAT_YOUTH_CHAMP_50" to "SA Regional Level 3 Championships"
    )

    // --- 16. SOUTH KOREA (KOR) ---
    private val southKorea = mapOf(
        // Short Course (25m)
        "NAT_TROPHY_25_1" to "Seoul SC Meet",
        "NAT_TROPHY_25_2" to "Jeju SC Invitational",
        "NAT_CHAMP_WINTER_25" to "Korean Short Course Championships",
        "NAT_TEAM_CHAMP_25" to "Korean National Club Championships",
        "NAT_YOUTH_CRITERIA_25" to "Korean Junior SC Championships",
        // Long Course (50m)
        "NAT_TROPHY_50_1" to "Dong-A Swimming Tournament",
        "NAT_TROPHY_50_2" to "MBC Swimming Tournament",
        "NAT_CHAMP_SPRING_50" to "Korean National Sports Festival",
        "NAT_YOUTH_CHAMP_50" to "Korean Junior LC Championships"
    )

    // --- 17. ROMANIA (ROU) ---
    private val romania = mapOf(
        // Short Course (25m)
        "NAT_TROPHY_25_1" to "Cupa României (SC)",
        "NAT_TROPHY_25_2" to "Bucharest SC Invitational",
        "NAT_CHAMP_WINTER_25" to "Campionatul Național în Bazin Scurt",
        "NAT_TEAM_CHAMP_25" to "Campionatul Național de Echipe",
        "NAT_YOUTH_CRITERIA_25" to "Campionatul Național de Cadeți și Juniori (25m)",
        // Long Course (50m)
        "NAT_TROPHY_50_1" to "Romanian International Swimming Championships",
        "NAT_TROPHY_50_2" to "Cupa României (LC)",
        "NAT_CHAMP_SPRING_50" to "Campionatul Național de Înot",
        "NAT_YOUTH_CHAMP_50" to "Campionatul Național de Juniori (50m)"
    )

    // --- 18. LITHUANIA (LTU) ---
    private val lithuania = mapOf(
        // Short Course (25m)
        "NAT_TROPHY_25_1" to "Anykščiai Sprint Festival",
        "NAT_TROPHY_25_2" to "Kaunas Grand Prix (SC)",
        "NAT_CHAMP_WINTER_25" to "Lietuvos Plaukimo Čempionatas (25m)",
        "NAT_TEAM_CHAMP_25" to "Lietuvos Komandinis Čempionatas",
        "NAT_YOUTH_CRITERIA_25" to "Lietuvos Jaunių ir Jaunučių Čempionatas (25m)",
        // Long Course (50m)
        "NAT_TROPHY_50_1" to "D. ir Z. Grigaliūnų Taurė",
        "NAT_TROPHY_50_2" to "Klaipėda Open",
        "NAT_CHAMP_SPRING_50" to "Lietuvos Plaukimo Čempionatas (50m)",
        "NAT_YOUTH_CHAMP_50" to "Lietuvos Jaunimo Čempionatas (50m)"
    )

    // --- 19. BELGIUM (BEL) ---
    private val belgium = mapOf(
        // Short Course (25m)
        "NAT_TROPHY_25_1" to "Flanders Swimming Cup (SC)",
        "NAT_TROPHY_25_2" to "Grand Prix de Wallonie",
        "NAT_CHAMP_WINTER_25" to "Belgian Short Course Championships",
        "NAT_TEAM_CHAMP_25" to "Belgian Interclub Championships",
        "NAT_YOUTH_CRITERIA_25" to "Belgian Junior SC Championships",
        // Long Course (50m)
        "NAT_TROPHY_50_1" to "Flanders Speedo Cup",
        "NAT_TROPHY_50_2" to "Antwerp Diamond Speedo Race",
        "NAT_CHAMP_SPRING_50" to "Belgian Open Championships",
        "NAT_YOUTH_CHAMP_50" to "Belgian Age Group Championships"
    )

    // --- 20. SWITZERLAND (SUI) ---
    private val switzerland = mapOf(
        // Short Course (25m)
        "NAT_TROPHY_25_1" to "SC Uster Wallisellen Meet",
        "NAT_TROPHY_25_2" to "Lausanne Swim Cup",
        "NAT_CHAMP_WINTER_25" to "Schweizer Kurzbahnmeisterschaften",
        "NAT_TEAM_CHAMP_25" to "Schweizer Vereinsmeisterschaften (SVM)",
        "NAT_YOUTH_CRITERIA_25" to "Schweizer Nachwuchsmeisterschaften (25m)",
        // Long Course (50m)
        "NAT_TROPHY_50_1" to "Challenge International de Genève (CIG)",
        "NAT_TROPHY_50_2" to "Basel Meet",
        "NAT_CHAMP_SPRING_50" to "Schweizer Langbahnmeisterschaften",
        "NAT_YOUTH_CHAMP_50" to "Schweizer Nachwuchsmeisterschaften (50m)"
    )

    // --- 21. RUSSIA (RUS) ---
    private val russia = mapOf(
        // Short Course (25m)
        "NAT_TROPHY_25_1" to "Vladimir Salnikov Cup",
        "NAT_TROPHY_25_2" to "Russian SC Cup Stage",
        "NAT_CHAMP_WINTER_25" to "Russian Short Course Swimming Championships",
        "NAT_TEAM_CHAMP_25" to "Russian Interclub SC Championship",
        "NAT_YOUTH_CRITERIA_25" to "Merry Dolphin National Festival",
        // Long Course (50m)
        "NAT_TROPHY_50_1" to "Russian Swimming Cup",
        "NAT_TROPHY_50_2" to "Moscow Open",
        "NAT_CHAMP_SPRING_50" to "Russian Swimming Championships",
        "NAT_YOUTH_CHAMP_50" to "Russian Junior Swimming Championships"
    )

}