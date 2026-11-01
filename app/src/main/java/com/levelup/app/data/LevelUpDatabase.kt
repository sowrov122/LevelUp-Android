package com.levelup.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        RoutineTaskEntity::class,
        TaskInstanceEntity::class,
        ScoreEventEntity::class,
        FocusSessionEntity::class,
        AchievementEntity::class,
        OwnedRewardEntity::class,
        BlockedAppEntity::class,
        InterruptionEventEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class LevelUpDatabase : RoomDatabase() {
    abstract fun routineTaskDao(): RoutineTaskDao
    abstract fun taskInstanceDao(): TaskInstanceDao
    abstract fun scoreEventDao(): ScoreEventDao
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun achievementDao(): AchievementDao
    abstract fun ownedRewardDao(): OwnedRewardDao
    abstract fun blockedAppDao(): BlockedAppDao
    abstract fun interruptionDao(): InterruptionDao

    companion object {
        fun build(context: Context): LevelUpDatabase =
            Room.databaseBuilder(context.applicationContext, LevelUpDatabase::class.java, "levelup.db")
                .addMigrations(MIGRATION_1_2)
                .build()

        /** Phase 6: adds the selected-app list and the interruption history. Existing data is untouched. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `blocked_apps` (`packageName` TEXT NOT NULL, `displayName` TEXT NOT NULL, " +
                        "`isEnabled` INTEGER NOT NULL, `addedAt` INTEGER NOT NULL, PRIMARY KEY(`packageName`))",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `interruption_events` (`id` TEXT NOT NULL, `packageName` TEXT NOT NULL, " +
                        "`displayName` TEXT NOT NULL, `detectedAt` INTEGER NOT NULL, `focusSessionId` TEXT, " +
                        "`actionTaken` TEXT NOT NULL, `pointsPenalty` INTEGER NOT NULL, `idempotencyKey` TEXT NOT NULL, " +
                        "PRIMARY KEY(`id`))",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_interruption_events_idempotencyKey` " +
                        "ON `interruption_events` (`idempotencyKey`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_interruption_events_detectedAt` ON `interruption_events` (`detectedAt`)",
                )
            }
        }
    }
}
