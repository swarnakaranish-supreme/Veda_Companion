package com.anish.veda.utils

import com.anish.veda.bluetooth.BleConstants
import java.util.UUID

object Constants {
    // BLE Protocol UUIDs directly from ESP32-C3 firmware (ble_phone.cpp)
    const val SERVICE_UUID_STRING = BleConstants.SERVICE_UUID_STRING
    const val WRITE_CHARACTERISTIC_UUID_STRING = BleConstants.WRITE_CHARACTERISTIC_UUID_STRING

    val BLE_SERVICE_UUID: UUID = BleConstants.SERVICE_UUID
    val BLE_WRITE_CHARACTERISTIC_UUID: UUID = BleConstants.WRITE_CHARACTERISTIC_UUID

    // Expected watch advertised local name
    const val DEFAULT_WATCH_NAME = BleConstants.DEFAULT_DEVICE_NAME

    // Foreground service notification
    const val FOREGROUND_CHANNEL_ID = "veda_ble_service_channel"
    const val FOREGROUND_NOTIFICATION_ID = 1001

    // Firmware payload limits
    const val MAX_APP_LENGTH = 48
    const val MAX_TITLE_LENGTH = 72
    const val MAX_MESSAGE_LENGTH = 180

    // Notification Categories in watch firmware:
    // 0: Generic App, 1: Incoming Call, 2: Missed Call, 4: Messaging, 5: Calendar, 6: Email
    const val CATEGORY_GENERIC = 0
    const val CATEGORY_INCOMING_CALL = 1
    const val CATEGORY_MISSED_CALL = 2
    const val CATEGORY_MESSAGING = 4
    const val CATEGORY_CALENDAR = 5
    const val CATEGORY_EMAIL = 6

    // Intent actions for service communication
    const val ACTION_CONNECT = "com.anish.veda.ACTION_CONNECT"
    const val ACTION_DISCONNECT = "com.anish.veda.ACTION_DISCONNECT"
    const val ACTION_SYNC_TIME = "com.anish.veda.ACTION_SYNC_TIME"
    const val ACTION_START_SCAN = "com.anish.veda.ACTION_START_SCAN"
    const val ACTION_STOP_SCAN = "com.anish.veda.ACTION_STOP_SCAN"
    const val EXTRA_DEVICE_ADDRESS = "extra_device_address"
    const val EXTRA_DEVICE_NAME = "extra_device_name"
}
