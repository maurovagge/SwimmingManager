package com.example.swimmingmanager.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "swimmers",
    indices = [
        Index(value = ["clubId"]),
        Index(value = ["isRetired"]),
        Index(value = ["nationality"])
    ]
)
data class Swimmer (
    @PrimaryKey(autoGenerate = true)  val id: Int=0,
    val firstName: String?,
    val lastName: String?,
    val nationality: String?,
    var age: Int,
    val gender: String, // "M" or "F"
    val clubId: Int, // club id

    // The pool of improvement points the swimmer can spend during their career
    var potentialPoints: Double,

    // Technical skills (0.0 to 100.0)
    var butterfly: Double,
    var breaststroke: Double,
    var backstroke: Double,
    var freestyle: Double,
    var underwater: Double,

    // Physical skills (0.0 to 100.0)
    var sprintPower: Double, // 50 and 100
    var endurance: Double, // 400,800,1500
    var technique: Double, // influences every race and other paramaters
    var speed: Double, // 50, 100, 200 and 400

    // Mental skills (0.0 to 100.0) - NOT IMPLEMENTED YET
    var resilience: Double, //ability to maintain high motivation after bad races
    var adaptability: Double, //ability to perform well in heavy trainings periods
    var experience: Double, //increases with age and level of competition, influences performances in clutch moments

    // temporary parameters (0.0 to 5.0)
    var tiredness: Double, // depends on volume of training
    var moral: Double, // influenced by results

    // Stores the current physiological training plan
    var currentTraining: String = "BALANCED",
    // Stores the current stroke focus
    var focusStyle: String = "DEFAULT",

    // --- CAREER STATUS ---
    // True if the swimmer has started their physical decline
    var isDeclining: Boolean = false,
    // True if the swimmer has retired from professional swimming
    var isRetired: Boolean = false
){
    val overall: Int
        get(){
            val bestStyle=maxOf(butterfly,breaststroke,freestyle,backstroke)
            // Mental skills are excluded from OVR calculation for now
            return ((speed + sprintPower + endurance + technique + bestStyle + underwater) / 6.0).toInt()
        }
    val mainStyle: String
        get() {
            val bestStyle = maxOf(butterfly, breaststroke, freestyle, backstroke)
            if (((butterfly + breaststroke + backstroke + freestyle) / 4.0) >= bestStyle - 10) {
                return "MX"
            }
            return when (bestStyle) {
                freestyle -> "SL"
                backstroke -> "DO"
                breaststroke -> "RA"
                else -> "FA"
            }
        }

    // Translates the remaining points into a readable string for the UI
    val potentialCategory: String
        get() = when {
            potentialPoints > 180.0 -> "Elite"      // Generational talent
            potentialPoints > 100.0 -> "Very High"  // Future champion
            potentialPoints > 60.0 -> "High"        // Solid pro
            potentialPoints > 30.0 -> "Medium"      // Average improvement left
            potentialPoints > 10.0 -> "Low"         // Almost peaked
            else -> "Peaked"                        // Reached maximum potential
        }
}
