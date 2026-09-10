/*
 * Copyright (C) 2025 LooKeR & Contributors
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.looker.kenko.data.model.settings

import com.looker.kenko.data.model.SessionGrouping

import kotlin.time.Instant

data class Settings(
    val isOnboardingDone: Boolean,
    /**
     * True — the plan runs on days of the week; false — its days follow one another
     * whenever the lifter shows up.
     */
    val isWeekMode: Boolean,
    /**
     * Gym the lifter trains at now. Null means no gym is chosen and everything is available.
     */
    val currentGymId: Int?,
    val theme: Theme,
    val colorPalette: ColorPalettes,
    val lastSetTime: Instant?,
    val backupUri: String?,
    val backupInterval: BackupInterval,
    val lastBackupTime: Instant?,
    /**
     * How the session log is cut into groups: by month, by year, by day of the plan, or not at all.
     */
    val sessionGrouping: SessionGrouping,
)
