package com.workguard.patrol.data

import android.content.Context
import com.workguard.patrol.model.PatrolPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONArray
import org.json.JSONObject

data class PatrolSessionSnapshot(
    val taskId: String? = null,
    val patrolSessionId: Long? = null,
    val points: List<PatrolPoint> = emptyList(),
    val completedSessions: Int = 0,
    val shiftKey: String? = null,
    val remainingPoints: Int? = null,
    val sessionComplete: Boolean = false
)

@Singleton
class PatrolSessionStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "workguard_patrol"
        private const val KEY_SNAPSHOT = "patrol_snapshot"
    }

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    private val snapshotRef = AtomicReference<PatrolSessionSnapshot?>(null)

    fun get(): PatrolSessionSnapshot? {
        val cached = snapshotRef.get()
        if (cached != null) return cached
        val raw = prefs.getString(KEY_SNAPSHOT, null) ?: return null
        val parsed = decodeSnapshot(raw) ?: run {
            clear()
            return null
        }
        snapshotRef.set(parsed)
        return parsed
    }

    fun save(snapshot: PatrolSessionSnapshot) {
        snapshotRef.set(snapshot)
        prefs.edit()
            .putString(KEY_SNAPSHOT, encodeSnapshot(snapshot))
            .apply()
    }

    fun clear() {
        snapshotRef.set(null)
        prefs.edit()
            .remove(KEY_SNAPSHOT)
            .apply()
    }

    private fun encodeSnapshot(snapshot: PatrolSessionSnapshot): String {
        val root = JSONObject()
        if (!snapshot.taskId.isNullOrBlank()) {
            root.put("taskId", snapshot.taskId)
        }
        if (snapshot.patrolSessionId != null) {
            root.put("patrolSessionId", snapshot.patrolSessionId)
        }
        root.put("completedSessions", snapshot.completedSessions)
        if (!snapshot.shiftKey.isNullOrBlank()) {
            root.put("shiftKey", snapshot.shiftKey)
        }
        if (snapshot.remainingPoints != null) {
            root.put("remainingPoints", snapshot.remainingPoints)
        }
        root.put("sessionComplete", snapshot.sessionComplete)
        val pointsArray = JSONArray()
        snapshot.points.forEach { point ->
            val item = JSONObject()
            item.put("id", point.id)
            item.put("name", point.name)
            if (!point.description.isNullOrBlank()) {
                item.put("description", point.description)
            }
            if (point.latitude != null) {
                item.put("latitude", point.latitude)
            }
            if (point.longitude != null) {
                item.put("longitude", point.longitude)
            }
            if (point.radiusMeters != null) {
                item.put("radiusMeters", point.radiusMeters)
            }
            item.put("isScanned", point.isScanned)
            pointsArray.put(item)
        }
        root.put("points", pointsArray)
        return root.toString()
    }

    private fun decodeSnapshot(raw: String): PatrolSessionSnapshot? {
        return try {
            val root = JSONObject(raw)
            val taskId = if (root.has("taskId") && !root.isNull("taskId")) {
                root.getString("taskId")
            } else {
                null
            }
            val patrolSessionId = if (root.has("patrolSessionId") && !root.isNull("patrolSessionId")) {
                root.getLong("patrolSessionId")
            } else {
                null
            }
            val completedSessions = root.optInt("completedSessions", 0)
            val shiftKey = if (root.has("shiftKey") && !root.isNull("shiftKey")) {
                root.getString("shiftKey")
            } else {
                null
            }
            val remainingPoints = if (root.has("remainingPoints") && !root.isNull("remainingPoints")) {
                root.getInt("remainingPoints")
            } else {
                null
            }
            val sessionComplete = root.optBoolean("sessionComplete", false)
            val points = mutableListOf<PatrolPoint>()
            val pointsArray = root.optJSONArray("points") ?: JSONArray()
            for (i in 0 until pointsArray.length()) {
                val item = pointsArray.optJSONObject(i) ?: continue
                val id = item.optInt("id", i + 1)
                val name = item.optString("name", "Titik ${i + 1}")
                val description = if (item.has("description") && !item.isNull("description")) {
                    item.getString("description")
                } else {
                    null
                }
                val latitude = if (item.has("latitude") && !item.isNull("latitude")) {
                    item.getDouble("latitude")
                } else {
                    null
                }
                val longitude = if (item.has("longitude") && !item.isNull("longitude")) {
                    item.getDouble("longitude")
                } else {
                    null
                }
                val radiusMeters = if (item.has("radiusMeters") && !item.isNull("radiusMeters")) {
                    item.getDouble("radiusMeters")
                } else {
                    null
                }
                val isScanned = item.optBoolean("isScanned", false)
                points.add(
                    PatrolPoint(
                        id = id,
                        name = name,
                        description = description,
                        latitude = latitude,
                        longitude = longitude,
                        radiusMeters = radiusMeters,
                        isScanned = isScanned
                    )
                )
            }
            PatrolSessionSnapshot(
                taskId = taskId,
                patrolSessionId = patrolSessionId,
                points = points,
                completedSessions = completedSessions,
                shiftKey = shiftKey,
                remainingPoints = remainingPoints,
                sessionComplete = sessionComplete
            )
        } catch (_: Exception) {
            null
        }
    }
}
