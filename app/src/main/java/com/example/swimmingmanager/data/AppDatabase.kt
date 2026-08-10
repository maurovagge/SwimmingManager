package com.example.swimmingmanager.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase


@Database(entities=[Swimmer::class, Club::class, Competition::class, RaceRegistration::class, RaceResult::class, TimeStandard::class, Medal::class, RelayResult::class, PersonalBest::class, RaceRecord::class, Message::class, RelayRecord::class, SeasonBest::class], version=20, exportSchema=false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun swimmerDao(): SwimmerDao
    abstract fun clubDao(): ClubDao
    abstract fun competitionDao(): CompetitionDao
    abstract fun raceRegistrationDao(): RaceRegistrationDao
    abstract fun raceResultDao(): RaceResultDao
    abstract fun timeStandardDao(): TimeStandardDao
    abstract fun medalDao(): MedalDao
    abstract fun personalBestDao(): PersonalBestDao
    abstract fun seasonBestDao(): SeasonBestDao
    abstract fun raceRecordDao(): RaceRecordDao
    abstract fun relayRecordDao(): RelayRecordDao
    abstract fun messageDao(): MessageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "swimming_manager_db"
                )
                    .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}