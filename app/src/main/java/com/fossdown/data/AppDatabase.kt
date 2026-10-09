package com.fossdown.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

@Database(entities = [ProgressEvent::class], version = 4, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE events ADD COLUMN progressStyle TEXT NOT NULL DEFAULT 'BAR'"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE events ADD COLUMN weekDotMode TEXT NOT NULL DEFAULT 'PARTIAL'"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE events ADD COLUMN remainingUnits TEXT NOT NULL DEFAULT ''"
                )
                db.execSQL(
                    "ALTER TABLE events ADD COLUMN weeksOneDecimal INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        fun get(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pretty_progress.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                get(context).eventDao().apply {
                                    seedDefaults().forEach { upsert(it) }
                                }
                            }
                        }
                    }).build().also { instance = it }
            }
        }

        private fun seedDefaults(): List<ProgressEvent> {
            val now = System.currentTimeMillis()
            val yearStart = Calendar.getInstance().apply {
                set(Calendar.MONTH, Calendar.JANUARY)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val yearEnd = Calendar.getInstance().apply {
                set(Calendar.MONTH, Calendar.DECEMBER)
                set(Calendar.DAY_OF_MONTH, 31)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
            }.timeInMillis
            val christmas = Calendar.getInstance().apply {
                set(Calendar.MONTH, Calendar.DECEMBER)
                set(Calendar.DAY_OF_MONTH, 25)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
            }.timeInMillis
            val vacation = now + TimeUnit.DAYS.toMillis(45)

            return listOf(
                ProgressEvent(
                    title = "Days left in the year",
                    mode = EventMode.COUNTDOWN,
                    startEpochMillis = yearStart,
                    endEpochMillis = yearEnd,
                    theme = ThemeStyle.MINIMAL,
                    accentColor = 0xFF7CFFB2,
                    iconKey = "calendar",
                    progressStyle = ProgressStyle.BAR
                ),
                ProgressEvent(
                    title = "Christmas",
                    mode = EventMode.COUNTDOWN,
                    startEpochMillis = yearStart,
                    endEpochMillis = christmas,
                    theme = ThemeStyle.AQUA,
                    accentColor = 0xFFFF6B6B,
                    backgroundColor = 0xFF1B2430,
                    iconKey = "gift",
                    progressStyle = ProgressStyle.CIRCLE
                ),
                ProgressEvent(
                    title = "Vacation",
                    mode = EventMode.COUNTDOWN,
                    startEpochMillis = now,
                    endEpochMillis = vacation,
                    theme = ThemeStyle.SWISS,
                    accentColor = 0xFF4FC3F7,
                    iconKey = "plane",
                    displayUnit = DisplayUnit.WEEKS,
                    progressStyle = ProgressStyle.SEGMENTS
                ),
                ProgressEvent(
                    title = "Days since I started",
                    mode = EventMode.COUNT_UP,
                    startEpochMillis = now - TimeUnit.DAYS.toMillis(30),
                    endEpochMillis = null,
                    theme = ThemeStyle.GRID,
                    accentColor = 0xFFFFD166,
                    iconKey = "star",
                    progressStyle = ProgressStyle.DOTS
                )
            )
        }
    }
}
