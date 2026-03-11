package com.inkstride.app.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrator {
    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `settings_new` (
                    `id` INTEGER NOT NULL,
                    `characterName` TEXT NOT NULL,
                    `distanceUnit` TEXT NOT NULL,
                    PRIMARY KEY(`id`),
                    CHECK(`id` = 1),
                    CHECK(length(trim(`characterName`)) BETWEEN 1 AND 40),
                    CHECK(`distanceUnit` IN ('mile', 'kilometer'))
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO `settings_new` (`id`, `characterName`, `distanceUnit`)
                SELECT
                    1,
                    substr(trim(COALESCE(`characterName`, 'Inker')), 1, 40),
                    CASE
                        WHEN lower(COALESCE(`distanceUnit`, '')) = 'kilometer' THEN 'kilometer'
                        ELSE 'mile'
                    END
                FROM `settings`
                ORDER BY `id`
                LIMIT 1
                """.trimIndent()
            )
            db.execSQL("DROP TABLE `settings`")
            db.execSQL("ALTER TABLE `settings_new` RENAME TO `settings`")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `progress_state_new` (
                    `id` INTEGER NOT NULL,
                    `dayNumber` INTEGER NOT NULL,
                    `totalSteps` INTEGER NOT NULL,
                    `totalDistance` REAL NOT NULL,
                    `lastSyncEpochMilliseconds` INTEGER NOT NULL,
                    PRIMARY KEY(`id`),
                    CHECK(`id` = 1),
                    CHECK(`dayNumber` >= 1),
                    CHECK(`totalSteps` >= 0),
                    CHECK(`totalDistance` >= 0),
                    CHECK(`lastSyncEpochMilliseconds` >= 0)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO `progress_state_new` (
                    `id`, `dayNumber`, `totalSteps`, `totalDistance`, `lastSyncEpochMilliseconds`
                )
                SELECT
                    1,
                    CASE WHEN `dayNumber` < 1 THEN 1 ELSE `dayNumber` END,
                    CASE WHEN `totalSteps` < 0 THEN 0 ELSE `totalSteps` END,
                    CASE WHEN `totalDistance` < 0 THEN 0 ELSE `totalDistance` END,
                    CASE WHEN `lastSyncEpochMilliseconds` < 0 THEN 0 ELSE `lastSyncEpochMilliseconds` END
                FROM `progress_state`
                ORDER BY `id`
                LIMIT 1
                """.trimIndent()
            )
            db.execSQL("DROP TABLE `progress_state`")
            db.execSQL("ALTER TABLE `progress_state_new` RENAME TO `progress_state`")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `daily_stats_new` (
                    `dateKey` TEXT NOT NULL,
                    `stepsToday` INTEGER NOT NULL,
                    `distanceToday` REAL NOT NULL,
                    PRIMARY KEY(`dateKey`),
                    CHECK(`dateKey` GLOB '[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]'),
                    CHECK(`stepsToday` >= 0),
                    CHECK(`distanceToday` >= 0)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT OR REPLACE INTO `daily_stats_new` (`dateKey`, `stepsToday`, `distanceToday`)
                SELECT
                    `dateKey`,
                    CASE WHEN `stepsToday` < 0 THEN 0 ELSE `stepsToday` END,
                    CASE WHEN `distanceToday` < 0 THEN 0 ELSE `distanceToday` END
                FROM `daily_stats`
                WHERE `dateKey` GLOB '[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]'
                """.trimIndent()
            )
            db.execSQL("DROP TABLE `daily_stats`")
            db.execSQL("ALTER TABLE `daily_stats_new` RENAME TO `daily_stats`")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `milestone_new` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `distanceMarker` REAL NOT NULL,
                    `isMajor` INTEGER NOT NULL,
                    `area_name` TEXT NOT NULL,
                    CHECK(`distanceMarker` >= 0),
                    CHECK(`area_name` = '' OR length(trim(`area_name`)) > 0)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO `milestone_new` (`id`, `distanceMarker`, `isMajor`, `area_name`)
                SELECT
                    `id`,
                    CASE
                        WHEN `distanceMarker` < 0 THEN CAST((1000000000 + `id`) AS REAL)
                        ELSE `distanceMarker`
                    END,
                    `isMajor`,
                    CASE
                        WHEN `area_name` = '' THEN ''
                        ELSE trim(`area_name`)
                    END
                FROM `milestone`
                """.trimIndent()
            )
            db.execSQL("DROP TABLE `milestone`")
            db.execSQL("ALTER TABLE `milestone_new` RENAME TO `milestone`")
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_milestone_distanceMarker` ON `milestone` (`distanceMarker`)"
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `story_segment_new` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `milestoneId` INTEGER NOT NULL,
                    `text` TEXT NOT NULL,
                    FOREIGN KEY(`milestoneId`) REFERENCES `milestone`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                    CHECK(length(trim(`text`)) > 0)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO `story_segment_new` (`id`, `milestoneId`, `text`)
                SELECT
                    `id`,
                    `milestoneId`,
                    CASE
                        WHEN length(trim(`text`)) > 0 THEN trim(`text`)
                        ELSE '[missing story segment]'
                    END
                FROM `story_segment`
                """.trimIndent()
            )
            db.execSQL("DROP TABLE `story_segment`")
            db.execSQL("ALTER TABLE `story_segment_new` RENAME TO `story_segment`")
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_story_segment_milestoneId` ON `story_segment` (`milestoneId`)"
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `unlock_state_new` (
                    `storySegmentId` INTEGER NOT NULL,
                    `unlocked` INTEGER NOT NULL,
                    `read` INTEGER NOT NULL,
                    PRIMARY KEY(`storySegmentId`),
                    FOREIGN KEY(`storySegmentId`) REFERENCES `story_segment`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                    CHECK(NOT(`read` = 1 AND `unlocked` = 0))
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO `unlock_state_new` (`storySegmentId`, `unlocked`, `read`)
                SELECT
                    `storySegmentId`,
                    `unlocked`,
                    CASE
                        WHEN `read` = 1 AND `unlocked` = 0 THEN 0
                        ELSE `read`
                    END
                FROM `unlock_state`
                """.trimIndent()
            )
            db.execSQL("DROP TABLE `unlock_state`")
            db.execSQL("ALTER TABLE `unlock_state_new` RENAME TO `unlock_state`")
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_unlock_state_storySegmentId` ON `unlock_state` (`storySegmentId`)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_unlock_state_unlocked_read_storySegmentId` ON `unlock_state` (`unlocked`, `read`, `storySegmentId`)"
            )

            db.execSQL(
                """
                CREATE TRIGGER IF NOT EXISTS `settings_singleton_id_guard`
                BEFORE UPDATE ON `settings`
                WHEN NEW.`id` != 1
                BEGIN
                    SELECT RAISE(ABORT, 'settings.id must remain 1');
                END
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TRIGGER IF NOT EXISTS `progress_state_singleton_id_guard`
                BEFORE UPDATE ON `progress_state`
                WHEN NEW.`id` != 1
                BEGIN
                    SELECT RAISE(ABORT, 'progress_state.id must remain 1');
                END
                """.trimIndent()
            )
        }
    }


    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE `milestone` ADD COLUMN `is_persistent` INTEGER NOT NULL DEFAULT 1"
            )
        }
    }
}