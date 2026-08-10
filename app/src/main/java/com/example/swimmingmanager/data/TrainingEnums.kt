package com.example.swimmingmanager.data

// Defines the physiological focus of the weekly training
enum class TrainingType {
    ACTIVE_RECOVERY,   // Decreases tiredness, slight technique boost
    AEROBIC_BASE,      // High endurance boost, medium tiredness increase
    VO2_MAX,           // High endurance & speed, high tiredness
    LACTATE_TOLERANCE, // High speed, slight endurance/sprint, high tiredness
    LACTATE_PRODUCTION,// Max speed, medium sprint, medium tiredness
    PURE_SPEED,        // Max sprint, medium technique, neutral tiredness
    TECHNIQUE_DRILLS,  // Max technique & underwater, decreases tiredness slightly
    BALANCED,          // Balanced improvements, medium tiredness
    TAPERING           // Clears tiredness, boosts moral (detraining if already rested)
}

// Defines the stroke focus for the week
enum class FocusStyle {
    FREESTYLE,
    BACKSTROKE,
    BREASTSTROKE,
    BUTTERFLY,
    MEDLEY,            // Distributes the style boost across all 4 styles evenly
    DEFAULT            // Automatically targets the swimmer's 'mainStyle' (useful for team plans)
}