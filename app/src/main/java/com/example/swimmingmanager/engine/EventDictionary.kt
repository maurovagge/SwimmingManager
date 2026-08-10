package com.example.swimmingmanager.engine

enum class Stroke {
    FREESTYLE, BACKSTROKE, BREASTSTROKE, BUTTERFLY, MEDLEY
}

enum class EventType {
    INDIVIDUAL, RELAY
}

enum class Gender {
    M, F
}

data class SwimmingEvent(
    val id: String, 
    val distance: Int,
    val stroke: Stroke,
    val gender: Gender,
    val type: EventType = EventType.INDIVIDUAL,
    val isShortCourseOnly: Boolean = false
) {
    fun getDisplayName(): String {
        val strokeName = when (stroke) {
            Stroke.FREESTYLE -> "Stile Libero"
            Stroke.BACKSTROKE -> "Dorso"
            Stroke.BREASTSTROKE -> "Rana"
            Stroke.BUTTERFLY -> "Delfino"
            Stroke.MEDLEY -> "Misti"
        }
        val genderName = if (gender == Gender.M) "M" else "F"

        return if (type == EventType.RELAY) {
            "4x${distance} $strokeName ($genderName)"
        } else {
            "${distance} $strokeName ($genderName)"
        }
    }
}

object EventDictionary {
    private val baseEvents = listOf(
        SwimmingEvent("50_FREE", 50, Stroke.FREESTYLE, Gender.M),
        SwimmingEvent("100_FREE", 100, Stroke.FREESTYLE, Gender.M),
        SwimmingEvent("200_FREE", 200, Stroke.FREESTYLE, Gender.M),
        SwimmingEvent("400_FREE", 400, Stroke.FREESTYLE, Gender.M),
        SwimmingEvent("800_FREE", 800, Stroke.FREESTYLE, Gender.M),
        SwimmingEvent("1500_FREE", 1500, Stroke.FREESTYLE, Gender.M),
        SwimmingEvent("50_BACK", 50, Stroke.BACKSTROKE, Gender.M),
        SwimmingEvent("100_BACK", 100, Stroke.BACKSTROKE, Gender.M),
        SwimmingEvent("200_BACK", 200, Stroke.BACKSTROKE, Gender.M),
        SwimmingEvent("50_BREAST", 50, Stroke.BREASTSTROKE, Gender.M),
        SwimmingEvent("100_BREAST", 100, Stroke.BREASTSTROKE, Gender.M),
        SwimmingEvent("200_BREAST", 200, Stroke.BREASTSTROKE, Gender.M),
        SwimmingEvent("50_FLY", 50, Stroke.BUTTERFLY, Gender.M),
        SwimmingEvent("100_FLY", 100, Stroke.BUTTERFLY, Gender.M),
        SwimmingEvent("200_FLY", 200, Stroke.BUTTERFLY, Gender.M),
        SwimmingEvent("100_MEDLEY", 100, Stroke.MEDLEY, Gender.M, isShortCourseOnly = true),
        SwimmingEvent("200_MEDLEY", 200, Stroke.MEDLEY, Gender.M),
        SwimmingEvent("400_MEDLEY", 400, Stroke.MEDLEY, Gender.M),
        SwimmingEvent("4x100_FREE_RELAY", 100, Stroke.FREESTYLE, Gender.M, EventType.RELAY),
        SwimmingEvent("4x200_FREE_RELAY", 200, Stroke.FREESTYLE, Gender.M, EventType.RELAY),
        SwimmingEvent("4x100_MEDLEY_RELAY", 100, Stroke.MEDLEY, Gender.M, EventType.RELAY)
    )

    private val scOnlyEvents = listOf(
        SwimmingEvent("4x50_FREE_RELAY_M", 50, Stroke.FREESTYLE, Gender.M, EventType.RELAY, isShortCourseOnly = true),
        SwimmingEvent("4x50_FREE_RELAY_F", 50, Stroke.FREESTYLE, Gender.F, EventType.RELAY, isShortCourseOnly = true),
        SwimmingEvent("4x50_MEDLEY_RELAY_M", 50, Stroke.MEDLEY, Gender.M, EventType.RELAY, isShortCourseOnly = true),
        SwimmingEvent("4x50_MEDLEY_RELAY_F", 50, Stroke.MEDLEY, Gender.F, EventType.RELAY, isShortCourseOnly = true)
    )

    val standardEvents: List<SwimmingEvent> = baseEvents.flatMap { baseEvent ->
        listOf(
            baseEvent.copy(id = "${baseEvent.id}_M", gender = Gender.M),
            baseEvent.copy(id = "${baseEvent.id}_F", gender = Gender.F)
        )
    }

    private val allPossibleEvents = standardEvents + scOnlyEvents

    fun getEventsForCourse(isShortCourse: Boolean): List<SwimmingEvent> {
        return if (isShortCourse) {
            allPossibleEvents
        } else {
            allPossibleEvents.filter { !it.isShortCourseOnly }
        }
    }

    fun getEventById(eventId: String): SwimmingEvent? {
        return allPossibleEvents.find { it.id == eventId }
    }
}
