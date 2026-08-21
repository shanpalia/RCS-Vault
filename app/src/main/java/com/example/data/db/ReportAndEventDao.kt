package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.BackupEventEntity
import com.example.data.model.ReportEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BackupEventDao {
    @Query("SELECT * FROM backup_events ORDER BY timestamp DESC LIMIT 50")
    fun getRecentEvents(): Flow<List<BackupEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: BackupEventEntity): Long

    @Query("DELETE FROM backup_events")
    suspend fun clearAll()
}

@Dao
interface ReportDao {
    @Query("SELECT * FROM reports ORDER BY generatedTimestamp DESC")
    fun getAllReports(): Flow<List<ReportEntity>>

    @Query("SELECT * FROM reports WHERE id = :reportId LIMIT 1")
    suspend fun getReportById(reportId: String): ReportEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: ReportEntity)

    @Query("DELETE FROM reports WHERE id = :id")
    suspend fun deleteReportById(id: String)

    @Query("DELETE FROM reports")
    suspend fun clearAll()
}
