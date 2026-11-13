package com.anish.veda.data.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.anish.veda.data.model.AiActionType
import com.anish.veda.data.model.AiChatMessage
import com.anish.veda.data.model.MessageSender

class AiChatDatabaseHelper(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_CHAT (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TEXT TEXT NOT NULL,
                $COL_SENDER TEXT NOT NULL,
                $COL_TIMESTAMP INTEGER NOT NULL,
                $COL_ACTION_TYPE TEXT NOT NULL,
                $COL_ACTION_PAYLOAD TEXT,
                $COL_ACTION_EXECUTED INTEGER NOT NULL DEFAULT 0,
                $COL_SMART_REPLIES TEXT
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_chat_ts ON $TABLE_CHAT ($COL_TIMESTAMP)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CHAT")
        onCreate(db)
    }

    @Synchronized
    fun insertMessage(msg: AiChatMessage): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_TEXT, msg.text)
            put(COL_SENDER, msg.sender.name)
            put(COL_TIMESTAMP, msg.timestamp)
            put(COL_ACTION_TYPE, msg.actionType.name)
            put(COL_ACTION_PAYLOAD, msg.actionPayload)
            put(COL_ACTION_EXECUTED, if (msg.actionExecuted) 1 else 0)
            put(COL_SMART_REPLIES, if (msg.smartReplies.isNotEmpty()) msg.smartReplies.joinToString(";;") else null)
        }
        val id = db.insert(TABLE_CHAT, null, values)
        trimOldMessages(db)
        return id
    }

    private fun trimOldMessages(db: SQLiteDatabase) {
        try {
            db.execSQL(
                """
                DELETE FROM $TABLE_CHAT 
                WHERE $COL_ID NOT IN (
                    SELECT $COL_ID FROM $TABLE_CHAT 
                    ORDER BY $COL_TIMESTAMP DESC LIMIT $MAX_RECORDS
                )
                """.trimIndent()
            )
        } catch (_: Exception) {}
    }

    @Synchronized
    fun getRecentMessages(limit: Int = 80): List<AiChatMessage> {
        val list = mutableListOf<AiChatMessage>()
        val db = readableDatabase

        val cursor = db.query(
            TABLE_CHAT,
            null,
            null,
            null,
            null,
            null,
            "$COL_TIMESTAMP ASC",
            limit.toString()
        )

        cursor.use {
            val idIdx = it.getColumnIndexOrThrow(COL_ID)
            val textIdx = it.getColumnIndexOrThrow(COL_TEXT)
            val senderIdx = it.getColumnIndexOrThrow(COL_SENDER)
            val tsIdx = it.getColumnIndexOrThrow(COL_TIMESTAMP)
            val actionIdx = it.getColumnIndexOrThrow(COL_ACTION_TYPE)
            val payloadIdx = it.getColumnIndexOrThrow(COL_ACTION_PAYLOAD)
            val execIdx = it.getColumnIndexOrThrow(COL_ACTION_EXECUTED)
            val repliesIdx = it.getColumnIndexOrThrow(COL_SMART_REPLIES)

            while (it.moveToNext()) {
                val rawReplies = it.getString(repliesIdx)
                val repliesList = if (!rawReplies.isNullOrBlank()) {
                    rawReplies.split(";;").filter { s -> s.isNotBlank() }
                } else {
                    emptyList()
                }

                list.add(
                    AiChatMessage(
                        id = it.getLong(idIdx),
                        text = it.getString(textIdx),
                        sender = try { MessageSender.valueOf(it.getString(senderIdx)) } catch (_: Exception) { MessageSender.VEDA },
                        timestamp = it.getLong(tsIdx),
                        actionType = try { AiActionType.valueOf(it.getString(actionIdx)) } catch (_: Exception) { AiActionType.NONE },
                        actionPayload = it.getString(payloadIdx),
                        actionExecuted = it.getInt(execIdx) == 1,
                        smartReplies = repliesList
                    )
                )
            }
        }
        return list
    }

    @Synchronized
    fun markActionExecuted(id: Long) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_ACTION_EXECUTED, 1)
        }
        db.update(TABLE_CHAT, values, "$COL_ID = ?", arrayOf(id.toString()))
    }

    @Synchronized
    fun clearHistory(): Int {
        val db = writableDatabase
        return db.delete(TABLE_CHAT, null, null)
    }

    companion object {
        private const val DATABASE_NAME = "veda_ai_chat.db"
        private const val DATABASE_VERSION = 1
        private const val MAX_RECORDS = 200

        const val TABLE_CHAT = "chat_messages"
        const val COL_ID = "id"
        const val COL_TEXT = "text"
        const val COL_SENDER = "sender"
        const val COL_TIMESTAMP = "timestamp"
        const val COL_ACTION_TYPE = "action_type"
        const val COL_ACTION_PAYLOAD = "action_payload"
        const val COL_ACTION_EXECUTED = "action_executed"
        const val COL_SMART_REPLIES = "smart_replies"
    }
}
