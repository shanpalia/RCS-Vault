package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.BackupEventEntity
import com.example.data.model.ConversationEntity
import com.example.data.model.MediaFileEntity
import com.example.data.model.MessageEntity
import com.example.data.model.ReportEntity

@Database(
    entities = [
        MessageEntity::class,
        ConversationEntity::class,
        MediaFileEntity::class,
        BackupEventEntity::class,
        ReportEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun conversationDao(): ConversationDao
    abstract fun mediaDao(): MediaDao
    abstract fun backupEventDao(): BackupEventDao
    abstract fun reportDao(): ReportDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "rcs_backup_recovery.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
