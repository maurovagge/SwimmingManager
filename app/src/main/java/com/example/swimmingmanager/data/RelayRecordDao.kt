package com.example.swimmingmanager.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RelayRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: RelayRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<RelayRecord>)

    @Query("SELECT * FROM relay_records WHERE eventId = :eventId AND isShortCourse = :isShort AND recordType = :type AND tag = :tag")
    suspend fun getRelayRecord(eventId: String, isShort: Boolean, type: String, tag: String): RelayRecord?

    @Query("SELECT * FROM relay_records WHERE recordType = :type AND tag = :tag")
    suspend fun getRecordsByType(type: String, tag: String): List<RelayRecord>

    @Query("SELECT * FROM relay_records WHERE isShortCourse = :isShort AND recordType = 'WORLD'")
    suspend fun getWorldRecordsList(isShort: Boolean): List<RelayRecord>

    @Query("SELECT * FROM relay_records WHERE isShortCourse = :isShort AND recordType = 'NATIONAL' AND tag = :nation")
    suspend fun getNationalRecordsList(isShort: Boolean, nation: String): List<RelayRecord>

    @Query("SELECT * FROM relay_records WHERE isShortCourse = :isShort AND recordType = 'REGIONAL' AND tag = :region")
    suspend fun getRegionalRecordsList(isShort: Boolean, region: String): List<RelayRecord>

    @Query("SELECT * FROM relay_records")
    suspend fun getAllRelayRecords(): List<RelayRecord>
}
