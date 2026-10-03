package com.vidhya.focuslock

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SessionLog(
    val id: String,
    val timestamp: Long,
    val plannedMinutes: Int,
    val breakMinutes: Int,
    val completedMinutes: Int,
    val status: String,
    val blockedAppsCount: Int
)

class SessionManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("focus_lock_prefs", Context.MODE_PRIVATE)

    var isLocked: Boolean
        get() = prefs.getBoolean("is_locked", false)
        set(v) = prefs.edit().putBoolean("is_locked", v).apply()

    var sessionEndTime: Long
        get() = prefs.getLong("session_end_time", 0L)
        set(v) = prefs.edit().putLong("session_end_time", v).apply()

    var sessionStartTime: Long
        get() = prefs.getLong("session_start_time", 0L)
        set(v) = prefs.edit().putLong("session_start_time", v).apply()

    var sessionDurationMinutes: Int
        get() = prefs.getInt("session_duration", 25)
        set(v) = prefs.edit().putInt("session_duration", v).apply()

    var breakDurationMinutes: Int
        get() = prefs.getInt("break_duration", 5)
        set(v) = prefs.edit().putInt("break_duration", v).apply()

    var isInBreak: Boolean
        get() = prefs.getBoolean("is_in_break", false)
        set(v) = prefs.edit().putBoolean("is_in_break", v).apply()

    var breakEndTime: Long
        get() = prefs.getLong("break_end_time", 0L)
        set(v) = prefs.edit().putLong("break_end_time", v).apply()

    var blockedPackages: Set<String>
        get() = prefs.getStringSet("blocked_packages", emptySet()) ?: emptySet()
        set(v) = prefs.edit().putStringSet("blocked_packages", v).apply()

    var disableAttemptsCount: Int
        get() = prefs.getInt("disable_attempts", 0)
        set(v) = prefs.edit().putInt("disable_attempts", v).apply()

    fun recordDisableAttempt() {
        disableAttemptsCount = disableAttemptsCount + 1
    }

    fun startSession(focusMins: Int, breakMins: Int = 0) {
        val now = System.currentTimeMillis()
        sessionStartTime = now
        sessionDurationMinutes = focusMins
        breakDurationMinutes = breakMins
        isInBreak = false
        breakEndTime = 0L
        isLocked = true
        sessionEndTime = now + (focusMins * 60 * 1000L)
        prefs.edit().putBoolean("emergency_pass_active", false).apply()
    }

    fun startBreak(breakMins: Int) {
        if (breakMins <= 0) {
            endSession(completed = true)
            return
        }
        val now = System.currentTimeMillis()
        isInBreak = true
        breakEndTime = now + (breakMins * 60 * 1000L)
    }

    fun endSession(completed: Boolean, statusOverride: String? = null) {
        val planned = sessionDurationMinutes
        val elapsedMs = System.currentTimeMillis() - sessionStartTime
        val completedMins = if (completed) planned else maxOf(1, (elapsedMs / 60000).toInt())
        val status = statusOverride ?: (if (completed) "COMPLETED" else "CANCELLED")

        addSessionLog(
            SessionLog(
                id = System.currentTimeMillis().toString(),
                timestamp = System.currentTimeMillis(),
                plannedMinutes = planned,
                breakMinutes = breakDurationMinutes,
                completedMinutes = minOf(planned, completedMins),
                status = status,
                blockedAppsCount = blockedPackages.size
            )
        )

        val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val currentDaily = prefs.getInt("daily_$todayKey", 0)
        prefs.edit().putInt("daily_$todayKey", currentDaily + minOf(planned, completedMins)).apply()

        isLocked = false
        isInBreak = false
        sessionEndTime = 0L
        breakEndTime = 0L
        prefs.edit().putBoolean("emergency_pass_active", false).apply()
    }

    fun isSessionActive(): Boolean {
        if (!isLocked) return false
        val now = System.currentTimeMillis()
        if (isInBreak) {
            if (now >= breakEndTime) {
                endSession(completed = true)
                return false
            }
            return true
        }
        if (now >= sessionEndTime) {
            if (breakDurationMinutes > 0) {
                startBreak(breakDurationMinutes)
                return true
            } else {
                endSession(completed = true)
                return false
            }
        }
        return true
    }

    fun getRemainingTimeMs(): Long {
        val now = System.currentTimeMillis()
        return if (isInBreak) {
            maxOf(0L, breakEndTime - now)
        } else {
            maxOf(0L, sessionEndTime - now)
        }
    }

    fun isEmergencyPassActive(): Boolean {
        val active = prefs.getBoolean("emergency_pass_active", false)
        val expires = prefs.getLong("emergency_expires", 0L)
        return active && (System.currentTimeMillis() < expires)
    }

    fun grantEmergencyPass(durationMinutes: Int = 2) {
        prefs.edit()
            .putBoolean("emergency_pass_active", true)
            .putLong("emergency_expires", System.currentTimeMillis() + (durationMinutes * 60 * 1000L))
            .apply()
        recordDisableAttempt()
    }

    fun addSessionLog(log: SessionLog) {
        val logs = getSessionLogs().toMutableList()
        logs.add(0, log)
        if (logs.size > 50) logs.removeAt(logs.size - 1)
        val arr = JSONArray()
        for (item in logs) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("ts", item.timestamp)
            obj.put("planned", item.plannedMinutes)
            obj.put("break", item.breakMinutes)
            obj.put("completed", item.completedMinutes)
            obj.put("status", item.status)
            obj.put("apps", item.blockedAppsCount)
            arr.put(obj)
        }
        prefs.edit().putString("session_logs_json", arr.toString()).apply()
    }

    fun getSessionLogs(): List<SessionLog> {
        val json = prefs.getString("session_logs_json", null) ?: return emptyList()
        val list = mutableListOf<SessionLog>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    SessionLog(
                        id = obj.getString("id"),
                        timestamp = obj.getLong("ts"),
                        plannedMinutes = obj.getInt("planned"),
                        breakMinutes = obj.optInt("break", 0),
                        completedMinutes = obj.getInt("completed"),
                        status = obj.getString("status"),
                        blockedAppsCount = obj.getInt("apps")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun clearLogs() {
        prefs.edit().remove("session_logs_json").apply()
    }

    fun getTotalFocusMinutes(): Int {
        return getSessionLogs().sumOf { it.completedMinutes }
    }

    fun getCompletedSessionsCount(): Int {
        return getSessionLogs().count { it.status == "COMPLETED" }
    }

    fun getDisciplineScore(): Int {
        val totalSessions = getSessionLogs().size
        if (totalSessions == 0) return 100
        val completed = getCompletedSessionsCount()
        val attemptsPenalty = minOf(30, disableAttemptsCount * 2)
        val baseScore = (completed * 100) / totalSessions
        return maxOf(10, minOf(100, baseScore - attemptsPenalty))
    }

    fun getWeeklyStats(): List<Pair<String, Int>> {
        val result = mutableListOf<Pair<String, Int>>()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        for (i in 6 downTo 0) {
            val c = java.util.Calendar.getInstance()
            c.add(java.util.Calendar.DAY_OF_YEAR, -i)
            val dateKey = sdf.format(c.time)
            val dayName = SimpleDateFormat("EEE", Locale.getDefault()).format(c.time)
            val mins = prefs.getInt("daily_$dateKey", 0)
            result.add(dayName to mins)
        }
        return result
    }
}