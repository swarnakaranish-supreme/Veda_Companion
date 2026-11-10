package com.anish.veda.services

import com.anish.veda.bluetooth.BleProtocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ServiceLifecycleRegressionTest {

    @Test
    fun testAndroidManifestDeclaresRequiredForegroundServicePermissions() {
        val manifestFile = File("src/main/AndroidManifest.xml")
        assertTrue("AndroidManifest.xml should exist", manifestFile.exists())
        val content = manifestFile.readText()

        // 1. Must declare FOREGROUND_SERVICE
        assertTrue(
            "Manifest must declare android.permission.FOREGROUND_SERVICE",
            content.contains("android.permission.FOREGROUND_SERVICE")
        )

        // 2. Must declare FOREGROUND_SERVICE_CONNECTED_DEVICE for Android 14+ / targetSdk 35
        assertTrue(
            "Manifest must declare android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE",
            content.contains("android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE")
        )

        // 3. Must declare BLUETOOTH_CONNECT and BLUETOOTH_SCAN
        assertTrue(
            "Manifest must declare android.permission.BLUETOOTH_CONNECT",
            content.contains("android.permission.BLUETOOTH_CONNECT")
        )
        assertTrue(
            "Manifest must declare android.permission.BLUETOOTH_SCAN",
            content.contains("android.permission.BLUETOOTH_SCAN")
        )

        // 4. Service declaration must specify foregroundServiceType="connectedDevice"
        assertTrue(
            "Service declaration must specify foregroundServiceType=\"connectedDevice\"",
            content.contains("android:foregroundServiceType=\"connectedDevice\"")
        )
    }

    @Test
    fun testMainActivityDoesNotStartServiceOnCreate() {
        val mainActivityFile = File("src/main/java/com/anish/veda/MainActivity.kt")
        assertTrue("MainActivity.kt should exist", mainActivityFile.exists())
        val content = mainActivityFile.readText()

        // Verify that MainActivity.onCreate() does NOT start VedaBleForegroundService
        assertFalse(
            "MainActivity must NOT call VedaBleForegroundService.start() in onCreate()",
            content.contains("VedaBleForegroundService.start")
        )
        assertFalse(
            "MainActivity must NOT call startForegroundService() in onCreate()",
            content.contains("startForegroundService")
        )
    }

    @Test
    fun testBootReceiverDoesNotStartServiceInBackground() {
        val bootReceiverFile = File("src/main/java/com/anish/veda/services/VedaBootReceiver.kt")
        assertTrue("VedaBootReceiver.kt should exist", bootReceiverFile.exists())
        val content = bootReceiverFile.readText()

        // Verify that VedaBootReceiver does NOT call VedaBleForegroundService.connect()
        assertFalse(
            "VedaBootReceiver must NOT start foreground service on BOOT or PACKAGE_REPLACED",
            content.contains("VedaBleForegroundService.connect")
        )
    }

    @Test
    fun testApplicationClassDoesNotStartService() {
        val appFile = File("src/main/java/com/anish/veda/VedaApplication.kt")
        assertTrue("VedaApplication.kt should exist", appFile.exists())
        val content = appFile.readText()

        assertFalse(
            "VedaApplication must NOT call VedaBleForegroundService.start()",
            content.contains("VedaBleForegroundService.start")
        )
        assertFalse(
            "VedaApplication must NOT call startForegroundService()",
            content.contains("startForegroundService")
        )
    }

    @Test
    fun testMainViewModelDoesNotDuplicateBleConnect() {
        val vmFile = File("src/main/java/com/anish/veda/viewmodel/MainViewModel.kt")
        assertTrue("MainViewModel.kt should exist", vmFile.exists())
        val content = vmFile.readText()

        // MainViewModel must NOT call bleManager.connect() directly (service owns connection)
        assertFalse(
            "MainViewModel must delegate connection to VedaBleForegroundService without calling bleManager.connect() directly",
            content.contains("bleManager.connect")
        )
    }

    @Test
    fun testVedaBleForegroundServiceUsesStartNotSticky() {
        val svcFile = File("src/main/java/com/anish/veda/services/VedaBleForegroundService.kt")
        assertTrue("VedaBleForegroundService.kt should exist", svcFile.exists())
        val content = svcFile.readText()

        assertTrue(
            "VedaBleForegroundService must return START_NOT_STICKY to prevent illegal background restarts",
            content.contains("return START_NOT_STICKY")
        )
        assertFalse(
            "VedaBleForegroundService must NOT return START_STICKY",
            content.contains("return START_STICKY")
        )
    }

    @Test
    fun testProtocolFormatPayloadsPreserved() {
        val testPacket = BleProtocol.buildNormalNotificationPacket("TestApp", "Header", "Message")
        assertEquals("N|0|TestApp|Header|Message", testPacket)
        assertTrue(BleProtocol.isValidNotificationPacket(testPacket))
    }
}
