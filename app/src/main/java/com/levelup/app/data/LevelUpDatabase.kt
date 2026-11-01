package com.levelup.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        RoutineTaskEntity::class,
        TaskInstanceEntity::class,
        ScoreEventEntity::class,
        FocusSessionEntity::class,
        AchievementEntity::class,
        OwnedRewardEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class LevelUpDatabase : RoomDatabase() {
    abstract fun routineTaskDao(): RoutineTaskDao
    abstract fun taskInstanceDao(): TaskInstanceDao
    abstract fun scoreEventDao(): ScoreEventDao
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun achievementDao(): AchievementDao
    abstract fun ownedRewardDao(): OwnedRewardDao

    companion object {
        fun build(context: Context): LevelUpDatabase =
            Room.databaseBuilder(context.applicationContext, LevelUpDatabase::class.java, "levelup.db").build()
    }
}
