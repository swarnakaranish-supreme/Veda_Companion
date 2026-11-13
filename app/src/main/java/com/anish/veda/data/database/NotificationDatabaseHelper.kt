package com.anish.veda.data.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.anish.veda.data.model.NotificationHistoryItem

class NotificationDatabaseHelper(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_NOTIFICATIONS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TIMESTAMP INTEGER NOT NULL,
                $COL_PACKAGE_NAME TEXT NOT NULL,
                $COL_APP_NAME TEXT NOT NULL,
                $COL_TITLE TEXT NOT NULL,
                $COL_MESSAGE TEXT NOT NULL,
                $COL_CATEGORY INTEGER NOT NULL,
                $COL_CATEGORY_NAME TEXT NOT NULL,
                $COL_WAS_FORWARDED INTEGER NOT NULL,
                $COL_REJECTION_REASON TEXT,
                $COL_IS_READ INTEGER NOT NULL DEFAULT 0,
                $COL_IS_VIP INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_notif_ts ON $TABLE_NOTIFICATIONS ($COL_TIMESTAMP)")
        db.execSQL("CREATE INDEX idx_notif_pkg ON $TABLE_NOTIFICATIONS ($COL_PACKAGE_NAME)")
        db.execSQL("CREATE INDEX idx_notif_cat ON $TABLE_NOTIFICATIONS ($COL_CATEGORY)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NOTIFICATIONS")
        onCreate(db)
    }

    @Synchronized
    fun insertNotification(item: NotificationHistoryItem): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_TIMESTAMP, item.timestamp)
            put(COL_PACKAGE_NAME, item.packageName)
            put(COL_APP_NAME, item.appName)
            put(COL_TITLE, item.title)
            put(COL_MESSAGE, item.message)
            put(COL_CATEGORY, item.category)
            put(COL_CATEGORY_NAME, item.categoryName)
            put(COL_WAS_FORWARDED, if (item.wasForwarded) 1 else 0)
            put(COL_REJECTION_REASON, item.rejectionReason)
            put(COL_IS_READ, if (item.isRead) 1 else 0)
            put(COL_IS_VIP, if (item.isVip) 1 else 0)
        }
        val id = db.insert(TABLE_NOTIFICATIONS, null, values)

        // Prune old notifications if table exceeds MAX_RECORDS to save flash memory
        trimOldRecords(db)
        return id
    }

    private fun trimOldRecords(db: SQLiteDatabase) {
        try {
            db.execSQL(
                """
                DELETE FROM $TABLE_NOTIFICATIONS 
                WHERE $COL_ID NOT IN (
                    SELECT $COL_ID FROM $TABLE_NOTIFICATIONS 
                    ORDER BY $COL_TIMESTAMP DESC LIMIT $MAX_RECORDS
                )
                """.trimIndent()
            )
        } catch (_: Exception) {}
    }

    @Synchronized
    fun getHistory(
        searchQuery: String = "",
        categoryFilter: Int? = null,
        limit: Int = 150
    ): List<NotificationHistoryItem> {
        val list = mutableListOf<NotificationHistoryItem>()
        val db = readableDatabase

        val selectionClauses = mutableListOf<String>()
        val selectionArgs = mutableListOf<String>()

        if (searchQuery.isNotBlank()) {
            selectionClauses.add("($COL_TITLE LIKE ? OR $COL_MESSAGE LIKE ? OR $COL_APP_NAME LIKE ?)")
            val wildcard = "%${searchQuery.trim()}%"
            selectionArgs.add(wildcard)
            selectionArgs.add(wildcard)
            selectionArgs.add(wildcard)
        }

        if (categoryFilter != null) {
            selectionClauses.add("$COL_CATEGORY = ?")
            selectionArgs.add(categoryFilter.toString())
        }

        val selection = if (selectionClauses.isNotEmpty()) {
            selectionClauses.joinToString(" AND ")
        } else {
            null
        }

        val cursor = db.query(
            TABLE_NOTIFICATIONS,
            null,
            selection,
            if (selectionArgs.isNotEmpty()) selectionArgs.toTypedArray() else null,
            null,
            null,
            "$COL_TIMESTAMP DESC",
            limit.toString()
        )

        cursor.use {
            val idIdx = it.getColumnIndexOrThrow(COL_ID)
            val tsIdx = it.getColumnIndexOrThrow(COL_TIMESTAMP)
            val pkgIdx = it.getColumnIndexOrThrow(COL_PACKAGE_NAME)
            val appIdx = it.getColumnIndexOrThrow(COL_APP_NAME)
            val titleIdx = it.getColumnIndexOrThrow(COL_TITLE)
            val msgIdx = it.getColumnIndexOrThrow(COL_MESSAGE)
            val catIdx = it.getColumnIndexOrThrow(COL_CATEGORY)
            val catNameIdx = it.getColumnIndexOrThrow(COL_CATEGORY_NAME)
            val fwdIdx = it.getColumnIndexOrThrow(COL_WAS_FORWARDED)
            val reasonIdx = it.getColumnIndexOrThrow(COL_REJECTION_REASON)
            val readIdx = it.getColumnIndexOrThrow(COL_IS_READ)
            val vipIdx = it.getColumnIndexOrThrow(COL_IS_VIP)

            while (it.moveToNext()) {
                list.add(
                    NotificationHistoryItem(
                        id = it.getLong(idIdx),
                        timestamp = it.getLong(tsIdx),
                        packageName = it.getString(pkgIdx),
                        appName = it.getString(appIdx),
                        title = it.getString(titleIdx),
                        message = it.getString(msgIdx),
                        category = it.getInt(catIdx),
                        categoryName = it.getString(catNameIdx),
                        wasForwarded = it.getInt(fwdIdx) == 1,
                        rejectionReason = it.getString(reasonIdx),
                        isRead = it.getInt(readIdx) == 1,
                        isVip = it.getInt(vipIdx) == 1
                    )
                )
            }
        }
        return list
    }

    @Synchronized
    fun getUnreadCount(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_NOTIFICATIONS WHERE $COL_IS_READ = 0",
            null
        )
        return cursor.use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
    }

    @Synchronized
    fun markAllAsRead() {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_IS_READ, 1)
        }
        db.update(TABLE_NOTIFICATIONS, values, "$COL_IS_READ = 0", null)
    }

    @Synchronized
    fun markAsRead(id: Long) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_IS_READ, 1)
        }
        db.update(TABLE_NOTIFICATIONS, values, "$COL_ID = ?", arrayOf(id.toString()))
    }

    @Synchronized
    fun deleteNotification(id: Long): Int {
        val db = writableDatabase
        return db.delete(TABLE_NOTIFICATIONS, "$COL_ID = ?", arrayOf(id.toString()))
    }

    @Synchronized
    fun clearHistory(): Int {
        val db = writableDatabase
        return db.delete(TABLE_NOTIFICATIONS, null, null)
    }

    companion object {
        private const val DATABASE_NAME = "veda_notifications.db"
        private const val DATABASE_VERSION = 1
        private const val MAX_RECORDS = 500

        const val TABLE_NOTIFICATIONS = "notification_history"
        const val COL_ID = "id"
        const val COL_TIMESTAMP = "timestamp"
        const val COL_PACKAGE_NAME = "package_name"
        const val COL_APP_NAME = "app_name"
        const val COL_TITLE = "title"
        const val COL_MESSAGE = "message"
        const val COL_CATEGORY = "category"
        const val COL_CATEGORY_NAME = "category_name"
        const val COL_WAS_FORWARDED = "was_forwarded"
        const val COL_REJECTION_REASON = "rejection_reason"
        const val COL_IS_READ = "is_read"
        const val COL_IS_VIP = "is_vip"
    }
}
