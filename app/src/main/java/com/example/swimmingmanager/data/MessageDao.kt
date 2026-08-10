package com.example.swimmingmanager.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message)

    @Query("SELECT * FROM messages ORDER BY year DESC, week DESC, id DESC")
    fun getAllMessagesFlow(): Flow<List<Message>>

    @Query("UPDATE messages SET isRead = 1 WHERE id = :messageId")
    suspend fun markAsRead(messageId: Int)

    @Query("UPDATE messages SET isRead = 1 WHERE isRead = 0")
    suspend fun markAllAsRead()

    @Query("DELETE FROM messages WHERE year < :currentYear - 1")
    suspend fun purgeOldMessages(currentYear: Int)
    
    @Query("DELETE FROM messages")
    suspend fun deleteAllMessages()
}
