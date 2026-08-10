package com.example.swimmingmanager.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RaceRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<RaceRecord>)

    @Query("SELECT * FROM race_records")
    suspend fun getAllRecords(): List<RaceRecord>

    @Query("SELECT * FROM race_records WHERE recordType = :type AND tag = :tag")
    suspend fun getAllRecordsByType(type: String, tag: String): List<RaceRecord>

    @Query("SELECT * FROM race_records WHERE recordType = 'MEET' AND tag = :meetName")
    suspend fun getMeetRecords(meetName: String): List<RaceRecord>

    @Query("SELECT * FROM race_records WHERE eventId = :eventId AND isShortCourse = :isShort AND recordType = 'WORLD'")
    suspend fun getWorldRecord(eventId: String, isShort: Boolean): RaceRecord?

    @Query("SELECT * FROM race_records WHERE eventId = :eventId AND isShortCourse = :isShort AND recordType = 'NATIONAL' AND tag = :nation")
    suspend fun getNationalRecord(eventId: String, isShort: Boolean, nation: String): RaceRecord?

    @Query("SELECT * FROM race_records WHERE isShortCourse = :isShort AND recordType = 'WORLD'")
    suspend fun getWorldRecordsList(isShort: Boolean): List<RaceRecord>

    @Query("SELECT * FROM race_records WHERE isShortCourse = :isShort AND recordType = 'NATIONAL' AND tag = :nation")
    suspend fun getNationalRecordsList(isShort: Boolean, nation: String): List<RaceRecord>

    @Query("SELECT * FROM race_records WHERE isShortCourse = :isShort AND recordType = 'REGIONAL' AND tag = :region")
    suspend fun getRegionalRecordsList(isShort: Boolean, region: String): List<RaceRecord>
}
