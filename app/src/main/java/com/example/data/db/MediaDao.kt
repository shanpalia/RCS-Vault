package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.MediaFileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Query("SELECT * FROM media_files ORDER BY timestamp DESC")
    fun getAllMedia(): Flow<List<MediaFileEntity>>

    @Query("SELECT * FROM media_files WHERE category = :category ORDER BY timestamp DESC")
    fun getMediaByCategory(category: String): Flow<List<MediaFileEntity>>

    @Query("SELECT * FROM media_files WHERE conversationId = :conversationId ORDER BY timestamp DESC")
    fun getMediaForConversation(conversationId: String): Flow<List<MediaFileEntity>>

    @Query("SELECT * FROM media_files ORDER BY timestamp DESC")
    suspend fun getAllMediaSync(): List<MediaFileEntity>

    @Query("SELECT * FROM media_files WHERE conversationId = :conversationId ORDER BY timestamp DESC")
    suspend fun getMediaForConversationSync(conversationId: String): List<MediaFileEntity>

    @Query("SELECT * FROM media_files WHERE sha256Hash = :hash LIMIT 1")
    suspend fun findByHash(hash: String): MediaFileEntity?

    @Query("SELECT COUNT(*) FROM media_files WHERE category = :category")
    fun getCountByCategory(category: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM media_files WHERE category = :category")
    suspend fun getCountByCategorySync(category: String): Int

    @Query("SELECT COALESCE(SUM(sizeBytes), 0) FROM media_files")
    fun getTotalStorageUsed(): Flow<Long>

    @Query("SELECT COALESCE(SUM(sizeBytes), 0) FROM media_files")
    suspend fun getTotalStorageUsedSync(): Long

    @Query("""
        SELECT * FROM media_files 
        WHERE fileName LIKE '%' || :query || '%' 
           OR (senderName IS NOT NULL AND senderName LIKE '%' || :query || '%')
        ORDER BY timestamp DESC
    """)
    fun searchMedia(query: String): Flow<List<MediaFileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(media: MediaFileEntity): Long

    @Query("DELETE FROM media_files WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM media_files WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteOlderThan(cutoffTimestamp: Long): Int

    @Query("DELETE FROM media_files")
    suspend fun clearAll()
}
