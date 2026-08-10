package com.example.swimmingmanager.ui

import com.example.swimmingmanager.data.Competition
import com.example.swimmingmanager.data.Swimmer

sealed class Destination {
    object Dashboard : Destination()
    object Roster : Destination()
    object Calendar : Destination()
    object Rankings : Destination()
    object Records : Destination()
    object TimeStandards : Destination()
    data class SwimmerProfile(val swimmer: Swimmer) : Destination()
    data class CompetitionDetail(val competition: Competition) : Destination()
    data class CompetitionResults(val competition: Competition) : Destination()
    data class NationalCallUps(val competition: Competition) : Destination()
}
