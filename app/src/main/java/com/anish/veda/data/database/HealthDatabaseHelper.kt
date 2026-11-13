package com.anish.veda.data.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.anish.veda.data.model.DailyHealthSummary
import com.anish.veda.data.model.HealthMeasurement
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HealthDatabaseHelper(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_MEASUREMENTS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TIMESTAMP INTEGER NOT NULL,
                $COL_HEART_RATE INTEGER NOT NULL,
                $COL_SPO2 INTEGER NOT NULL,
                $COL_QUALITY INTEGER NOT NULL,
                $COL_STATUS TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_health_ts ON $TABLE_MEASUREMENTS ($COL_TIMESTAMP)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_MEASUREMENTS")
        onCreate(db)
    }

    @Synchronized
    fun insertMeasurement(measurement: HealthMeasurement): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_TIMESTAMP, measurement.timestamp)
            put(COL_HEART_RATE, measurement.heartRate)
            put(COL_SPO2, measurement.spo2)
            put(COL_QUALITY, measurement.quality)
            put(COL_STATUS, measurement.status)
        }
        return db.insert(TABLE_MEASUREMENTS, null, values)
    }

    @Synchronized
    fun getRecentMeasurements(limit: Int = 100): List<HealthMeasurement> {
        val list = mutableListOf<HealthMeasurement>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_MEASUREMENTS,
            null,
            null,
            null,
            null,
            null,
            "$COL_TIMESTAMP DESC",
            limit.toString()
        )

        cursor.use {
            val idIdx = it.getColumnIndexOrThrow(COL_ID)
            val tsIdx = it.getColumnIndexOrThrow(COL_TIMESTAMP)
            val hrIdx = it.getColumnIndexOrThrow(COL_HEART_RATE)
            val spo2Idx = it.getColumnIndexOrThrow(COL_SPO2)
            val qualIdx = it.getColumnIndexOrThrow(COL_QUALITY)
            val statusIdx = it.getColumnIndexOrThrow(COL_STATUS)

            while (it.moveToNext()) {
                list.add(
                    HealthMeasurement(
                        id = it.getLong(idIdx),
                        timestamp = it.getLong(tsIdx),
                        heartRate = it.getInt(hrIdx),
                        spo2 = it.getInt(spo2Idx),
                        quality = it.getInt(qualIdx),
                        status = it.getString(statusIdx)
                    )
                )
            }
        }
        return list
    }

    @Synchronized
    fun getMeasurementsInTimeRange(startTime: Long, endTime: Long): List<HealthMeasurement> {
        val list = mutableListOf<HealthMeasurement>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_MEASUREMENTS,
            null,
            "$COL_TIMESTAMP >= ? AND $COL_TIMESTAMP <= ?",
            arrayOf(startTime.toString(), endTime.toString()),
            null,
            null,
            "$COL_TIMESTAMP ASC"
        )

        cursor.use {
            val idIdx = it.getColumnIndexOrThrow(COL_ID)
            val tsIdx = it.getColumnIndexOrThrow(COL_TIMESTAMP)
            val hrIdx = it.getColumnIndexOrThrow(COL_HEART_RATE)
            val spo2Idx = it.getColumnIndexOrThrow(COL_SPO2)
            val qualIdx = it.getColumnIndexOrThrow(COL_QUALITY)
            val statusIdx = it.getColumnIndexOrThrow(COL_STATUS)

            while (it.moveToNext()) {
                list.add(
                    HealthMeasurement(
                        id = it.getLong(idIdx),
                        timestamp = it.getLong(tsIdx),
                        heartRate = it.getInt(hrIdx),
                        spo2 = it.getInt(spo2Idx),
                        quality = it.getInt(qualIdx),
                        status = it.getString(statusIdx)
                    )
                )
            }
        }
        return list
    }

    @Synchronized
    fun getDailySummaries(days: Int = 7): List<DailyHealthSummary> {
        val summaries = mutableListOf<DailyHealthSummary>()
        val now = System.currentTimeMillis()
        val dayMs = 86_400_000L

        for (i in (days - 1) downTo 0) {
            val dayStart = now - (i + 1) * dayMs
            val dayEnd = now - i * dayMs
            val items = getMeasurementsInTimeRange(dayStart, dayEnd)

            val sdf = SimpleDateFormat("EEE", Locale.getDefault())
            val dateLabel = sdf.format(Date(dayEnd))

            if (items.isNotEmpty()) {
                val avgHr = items.map { it.heartRate }.average().toInt()
                val avgSpo2 = items.map { it.spo2 }.average().toInt()
                summaries.add(DailyHealthSummary(dateLabel, avgHr, avgSpo2, items.size))
            } else {
                summaries.add(DailyHealthSummary(dateLabel, 0, 0, 0))
            }
        }
        return summaries
    }

    @Synchronized
    fun clearHistory() {
        val db = writableDatabase
        db.delete(TABLE_MEASUREMENTS, null, null)
    }

    companion object {
        const val DATABASE_NAME = "veda_health.db"
        const val DATABASE_VERSION = 1

        const val TABLE_MEASUREMENTS = "health_measurements"
        const val COL_ID = "id"
        const val COL_TIMESTAMP = "timestamp"
        const val COL_HEART_RATE = "heart_rate"
        const val COL_SPO2 = "spo2"
        const val COL_QUALITY = "quality"
        const val COL_STATUS = "status"
    }
}
