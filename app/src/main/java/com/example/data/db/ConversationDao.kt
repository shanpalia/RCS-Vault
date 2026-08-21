package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.ConversationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY lastTimestamp DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE conversationId = :conversationId LIMIT 1")
    fun getConversationById(conversationId: String): Flow<ConversationEntity?>

    @Query("SELECT * FROM conversations WHERE conversationId = :conversationId LIMIT 1")
    suspend fun getConversationByIdSync(conversationId: String): ConversationEntity?

    @Query("SELECT * FROM conversations ORDER BY lastTimestamp DESC")
    suspend fun getAllConversationsSync(): List<ConversationEntity>

    @Query("""
        SELECT * FROM conversations 
        WHERE contactName LIKE '%' || :query || '%' 
           OR (contactPhone IS NOT NULL AND contactPhone LIKE '%' || :query || '%')
           OR lastMessage LIKE '%' || :query || '%'
        ORDER BY lastTimestamp DESC
    """)
    fun searchConversations(query: String): Flow<List<ConversationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(conversation: ConversationEntity)

    @Query("DELETE FROM conversations WHERE conversationId = :conversationId")
    suspend fun deleteById(conversationId: String)

    @Query("DELETE FROM conversations")
    suspend fun clearAll()
}
