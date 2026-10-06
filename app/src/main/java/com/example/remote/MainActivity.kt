package com.example.remote

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.remote.discovery.DiscoveredPc
import com.example.remote.discovery.DiscoveryState
import com.example.remote.discovery.NsdDiscoveryManager
import com.example.remote.security.KeystoreEncryptedStorage
import com.example.remote.ui.AppLauncherScreen
import com.example.remote.ui.PcStatusScreen
import com.example.remote.ui.theme.RemoteTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import java.util.regex.Pattern

enum class ConnectionState {
    DISCONNECTED,
    DISCOVERING,
    CONNECTING,
    AUTHENTICATING,
    CONNECTED,
    RECONNECTING,
    OFFLINE,
    ERROR
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RemoteTheme {
                RemoteHomeScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteHomeScreen() {
    val TAG = "RemoteHomeScreen"
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("remote_prefs", Context.MODE_PRIVATE) }
    val secureStorage = remember { KeystoreEncryptedStorage(context) }

    val deviceId = remember { secureStorage.getDeviceId() }
    val deviceName = remember { Build.MODEL }

    val savedIp = prefs.getString("last_pc_ip", "") ?: ""
    val savedPcName = prefs.getString("last_pc_name", "") ?: ""
    val savedAutoReconnect = prefs.getBoolean("auto_reconnect_enabled", true)
    val savedHapticFeedback = prefs.getBoolean("haptic_feedback_enabled", true)
    val savedQuality = prefs.getString("screen_quality", "High (85%)") ?: "High (85%)"

    var ipAddress by remember { mutableStateOf(savedIp) }
    var connectedPcName by remember { mutableStateOf(savedPcName) }

    var autoReconnectEnabled by remember { mutableStateOf(savedAutoReconnect) }
    var hapticFeedbackEnabled by remember { mutableStateOf(savedHapticFeedback) }
    var screenQualityPreference by remember { mutableStateOf(savedQuality) }

    var manualIpInput by remember { mutableStateOf(savedIp) }
    var pairingCodeInput by remember { mutableStateOf("") }

    var showManualIpDialog by remember { mutableStateOf(false) }
    var showPairCodeDialog by remember { mutableStateOf(false) }
    var pendingTargetIp by remember { mutableStateOf("") }
    var pendingTargetName by remember { mutableStateOf<String?>(null) }
    var pairDialogError by remember { mutableStateOf("") }

    // Single Connection State Model & Safeguards
    var connectionState by remember { mutableStateOf(ConnectionState.DISCONNECTED) }
    var userInitiatedDisconnect by remember { mutableStateOf(false) }
    var isPairingInProgress by remember { mutableStateOf(false) }
    var connectionStatus by remember { mutableStateOf("No computer connected") }

    var reconnectJob by remember { mutableStateOf<Job?>(null) }
    var heartbeatJob by remember { mutableStateOf<Job?>(null) }
    var retryCount by remember { mutableIntStateOf(0) }

    var showMouseScreen by remember { mutableStateOf(false) }
    var showKeyboardScreen by remember { mutableStateOf(false) }
    var showScreenScreen by remember { mutableStateOf(false) }
    var showClipboardScreen by remember { mutableStateOf(false) }
    var showFileTransferScreen by remember { mutableStateOf(false) }
    var showVoiceControlScreen by remember { mutableStateOf(false) }
    var showSystemControlScreen by remember { mutableStateOf(false) }
    var showQuickControlScreen by remember { mutableStateOf(false) }
    var showSettingsScreen by remember { mutableStateOf(false) }
    var showPcStatusScreen by remember { mutableStateOf(false) }
    var showAppLauncherScreen by remember { mutableStateOf(false) }

    var pcResX by remember { mutableIntStateOf(1920) }
    var pcResY by remember { mutableIntStateOf(1080) }

    val snackbarHostState = remember { SnackbarHostState() }
    val socketHolder = remember { mutableStateOf<Socket?>(null) }
    val commandMutex = remember { Mutex() }
    val scope = rememberCoroutineScope()

    // NSD Discovery Manager
    val discoveryManager = remember { NsdDiscoveryManager(context) }
    val discoveryState by discoveryManager.discoveryState.collectAsState()

    DisposableEffect(Unit) {
        discoveryManager.startDiscovery()
        onDispose {
            discoveryManager.stopDiscovery()
        }
    }

    // Connect with authenticated token
    fun connectToPc(targetIp: String, pcName: String? = null) {
        val cleanIp = targetIp.trim()
        if (cleanIp.isEmpty()) return

        val lookupName = pcName ?: connectedPcName.ifEmpty { savedPcName }
        var token = secureStorage.getAuthToken(lookupName)
        if (token.isEmpty()) {
            token = secureStorage.getAuthToken(cleanIp)
        }

        userInitiatedDisconnect = false
        retryCount = 0

        connectionState = ConnectionState.CONNECTING
        connectionStatus = "Connecting..."

        scope.launch(Dispatchers.IO) {
            try {
                socketHolder.value?.let {
                    try { it.close() } catch (_: Exception) {}
                }
                socketHolder.value = null

                val socket = Socket()
                socket.connect(InetSocketAddress(cleanIp, 5000), 5000)
                val inputStream = socket.getInputStream()
                val outputStream = socket.getOutputStream()

                val buffer = ByteArray(256)
                inputStream.read(buffer)

                val authJson = "{\"type\":\"AUTH\",\"device_id\":\"$deviceId\",\"token\":\"$token\"}\n"
                outputStream.write(authJson.toByteArray(Charsets.UTF_8))
                outputStream.flush()

                val authRespBuffer = ByteArray(256)
                val authRespRead = inputStream.read(authRespBuffer)
                val authResp = if (authRespRead > 0) String(authRespBuffer, 0, authRespRead) else ""

                if (authResp.contains("AUTH_OK")) {
                    if (authResp.contains("resX")) {
                        try {
                            val json = authResp.trim()
                            val xIdx = json.indexOf("\"resX\":")
                            val yIdx = json.indexOf("\"resY\":")
                            if (xIdx != -1 && yIdx != -1) {
                                val xVal = json.substring(xIdx + 7).takeWhile { it.isDigit() }.toInt()
                                val yVal = json.substring(yIdx + 7).takeWhile { it.isDigit() }.toInt()
                                pcResX = xVal
                                pcResY = yVal
                            }
                        } catch (_: Exception) {}
                    }

                    socketHolder.value = socket
                    val nameToSave = pcName ?: if (cleanIp == savedIp && savedPcName.isNotEmpty()) savedPcName else "PC ($cleanIp)"

                    secureStorage.saveAuthToken(nameToSave, token, cleanIp)

                    prefs.edit()
                        .putString("last_pc_ip", cleanIp)
                        .putString("last_pc_name", nameToSave)
                        .apply()

                    withContext(Dispatchers.Main) {
                        ipAddress = cleanIp
                        connectedPcName = nameToSave
                        connectionState = ConnectionState.CONNECTED
                        connectionStatus = "Connected"
                    }
                } else if (authResp.contains("AUTH_FAILED")) {
                    socket.close()
                    secureStorage.removeAuthToken(lookupName)
                    secureStorage.removeAuthToken(cleanIp)
                    withContext(Dispatchers.Main) {
                        connectionState = ConnectionState.ERROR
                        connectionStatus = "Device authorization revoked. Pair again."
                    }
                } else {
                    socket.close()
                    withContext(Dispatchers.Main) {
                        connectionState = ConnectionState.ERROR
                        connectionStatus = "Connection failed"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    connectionState = ConnectionState.ERROR
                    connectionStatus = "Failed: ${e.message}"
                }
            }
        }
    }

    // Step 1: Initiate Pairing Request
    fun pairToPc(targetIp: String, pcName: String? = null) {
        val cleanIp = targetIp.trim()
        if (cleanIp.isEmpty() || isPairingInProgress) return

        val lookupName = pcName ?: connectedPcName.ifEmpty { savedPcName }
        val existingToken = secureStorage.getAuthToken(lookupName).ifEmpty { secureStorage.getAuthToken(cleanIp) }

        if (existingToken.isNotEmpty()) {
            Log.d(TAG, "Device is already paired locally for $lookupName. Connecting directly...")
            connectToPc(cleanIp, pcName)
            return
        }

        isPairingInProgress = true
        userInitiatedDisconnect = false
        connectionState = ConnectionState.AUTHENTICATING
        connectionStatus = "Requesting pairing..."

        scope.launch(Dispatchers.IO) {
            try {
                val socket = Socket()
                socket.connect(InetSocketAddress(cleanIp, 5000), 5000)
                val inputStream = socket.getInputStream()
                val outputStream = socket.getOutputStream()

                val buffer = ByteArray(256)
                inputStream.read(buffer)

                val pairJson = "{\"type\":\"PAIR_REQUEST\",\"device_id\":\"$deviceId\",\"device_name\":\"$deviceName\"}\n"
                outputStream.write(pairJson.toByteArray(Charsets.UTF_8))
                outputStream.flush()

                val respBuffer = ByteArray(256)
                val respRead = inputStream.read(respBuffer)
                val resp = if (respRead > 0) String(respBuffer, 0, respRead) else ""
                socket.close()

                withContext(Dispatchers.Main) {
                    isPairingInProgress = false
                    if (resp.contains("PAIR_CODE_REQUIRED")) {
                        pendingTargetIp = cleanIp
                        pendingTargetName = pcName
                        pairingCodeInput = ""
                        pairDialogError = ""
                        showPairCodeDialog = true
                        connectionStatus = "Enter pairing code from PC"
                    } else if (resp.contains("ALREADY_PAIRED")) {
                        connectionStatus = "Already paired on PC. Authenticating..."
                        val tok = secureStorage.getAuthToken(lookupName).ifEmpty { secureStorage.getAuthToken(cleanIp) }
                        if (tok.isNotEmpty()) {
                            connectToPc(cleanIp, pcName)
                        } else {
                            connectionStatus = "Already paired on PC. Use existing token."
                        }
                    } else {
                        connectionState = ConnectionState.ERROR
                        connectionStatus = "Pairing failed"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isPairingInProgress = false
                    connectionState = ConnectionState.ERROR
                    connectionStatus = "Pairing failed: ${e.message}"
                }
            }
        }
    }

    // Auto-reconnect implementation
    fun triggerAutoReconnect(reason: String) {
        if (userInitiatedDisconnect || !autoReconnectEnabled) {
            Log.d(TAG, "Auto-reconnect skipped: userInitiatedDisconnect=$userInitiatedDisconnect, autoReconnectEnabled=$autoReconnectEnabled")
            return
        }

        val targetName = connectedPcName.ifEmpty { savedPcName }
        val targetIpAddr = ipAddress.ifBlank { savedIp }
        val token = secureStorage.getAuthToken(targetName).ifEmpty { secureStorage.getAuthToken(targetIpAddr) }

        if (token.isEmpty()) {
            connectionState = ConnectionState.DISCONNECTED
            connectionStatus = "Disconnected"
            return
        }

        reconnectJob?.cancel()
        reconnectJob = scope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                connectionState = ConnectionState.RECONNECTING
                connectionStatus = "Reconnecting to ${targetName.ifEmpty { "PC" }}..."
            }

            val delayMs = when (retryCount) {
                0 -> 1000L
                1 -> 2000L
                2 -> 4000L
                3 -> 8000L
                else -> 15000L
            }
            Log.d(TAG, "Reconnection attempt #${retryCount + 1} in ${delayMs}ms ($reason)")
            delay(delayMs)

            if (!isActive || userInitiatedDisconnect || !autoReconnectEnabled) return@launch

            var targetIp = ipAddress.ifBlank { savedIp }
            var success = false

            if (targetIp.isNotBlank()) {
                val socket = Socket()
                try {
                    socket.connect(InetSocketAddress(targetIp.trim(), 5000), 4000)
                    val inputStream = socket.getInputStream()
                    val outputStream = socket.getOutputStream()

                    val buffer = ByteArray(256)
                    val bytesRead = inputStream.read(buffer)
                    if (bytesRead > 0) {
                        val authJson = "{\"type\":\"AUTH\",\"device_id\":\"$deviceId\",\"token\":\"$token\"}\n"
                        outputStream.write(authJson.toByteArray(Charsets.UTF_8))
                        outputStream.flush()

                        val authRespBuffer = ByteArray(256)
                        val authRespRead = inputStream.read(authRespBuffer)
                        val authResp = if (authRespRead > 0) String(authRespBuffer, 0, authRespRead) else ""

                        if (authResp.contains("AUTH_OK")) {
                            if (authResp.contains("resX")) {
                                try {
                                    val json = authResp.trim()
                                    val xIdx = json.indexOf("\"resX\":")
                                    val yIdx = json.indexOf("\"resY\":")
                                    if (xIdx != -1 && yIdx != -1) {
                                        val xVal = json.substring(xIdx + 7).takeWhile { it.isDigit() }.toInt()
                                        val yVal = json.substring(yIdx + 7).takeWhile { it.isDigit() }.toInt()
                                        pcResX = xVal
                                        pcResY = yVal
                                    }
                                } catch (_: Exception) {}
                            }
                            socketHolder.value = socket
                            success = true
                        } else if (authResp.contains("AUTH_FAILED")) {
                            socket.close()
                            secureStorage.removeAuthToken(targetName)
                            secureStorage.removeAuthToken(targetIp)
                            withContext(Dispatchers.Main) {
                                connectionStatus = "Device authorization revoked. Pair again."
                                connectionState = ConnectionState.ERROR
                            }
                            userInitiatedDisconnect = true
                            return@launch
                        } else {
                            socket.close()
                        }
                    } else {
                        socket.close()
                    }
                } catch (_: Exception) {
                    try { socket.close() } catch (_: Exception) {}
                }
            }

            if (!success && !userInitiatedDisconnect && autoReconnectEnabled && isActive) {
                Log.d(TAG, "Direct connect to $targetIp failed. Re-discovering PC '$targetName' via mDNS...")
                withContext(Dispatchers.Main) {
                    connectionState = ConnectionState.DISCOVERING
                    connectionStatus = "Locating $targetName on network..."
                }

                discoveryManager.startDiscovery()
                delay(3000)

                val discoveredList = (discoveryManager.discoveryState.value as? DiscoveryState.Success)?.pcs
                val matchedPc = discoveredList?.find {
                    it.name.equals(targetName, ignoreCase = true) || cleanServiceName(it.name).equals(cleanServiceName(targetName), ignoreCase = true) || it.name.contains("REDDY", ignoreCase = true)
                } ?: discoveredList?.firstOrNull()

                if (matchedPc != null) {
                    targetIp = matchedPc.ipAddress
                    val reToken = secureStorage.getAuthToken(matchedPc.name).ifEmpty { token }
                    if (reToken.isNotEmpty()) {
                        val socket = Socket()
                        try {
                            socket.connect(InetSocketAddress(targetIp, 5000), 4000)
                            val inputStream = socket.getInputStream()
                            val outputStream = socket.getOutputStream()

                            val buffer = ByteArray(256)
                            inputStream.read(buffer)

                            val authJson = "{\"type\":\"AUTH\",\"device_id\":\"$deviceId\",\"token\":\"$reToken\"}\n"
                            outputStream.write(authJson.toByteArray(Charsets.UTF_8))
                            outputStream.flush()

                            val authRespBuffer = ByteArray(256)
                            val authRespRead = inputStream.read(authRespBuffer)
                            val authResp = if (authRespRead > 0) String(authRespBuffer, 0, authRespRead) else ""

                            if (authResp.contains("AUTH_OK")) {
                                socketHolder.value = socket
                                success = true
                                secureStorage.saveAuthToken(matchedPc.name, reToken, targetIp)
                                withContext(Dispatchers.Main) {
                                    ipAddress = targetIp
                                    connectedPcName = matchedPc.name
                                    prefs.edit().putString("last_pc_ip", targetIp).putString("last_pc_name", matchedPc.name).apply()
                                }
                            } else {
                                socket.close()
                            }
                        } catch (_: Exception) {
                            try { socket.close() } catch (_: Exception) {}
                        }
                    }
                }
            }

            if (success) {
                retryCount = 0
                withContext(Dispatchers.Main) {
                    connectionState = ConnectionState.CONNECTED
                    connectionStatus = "Connected"
                }
            } else {
                retryCount++
                if (retryCount >= 5) {
                    withContext(Dispatchers.Main) {
                        connectionState = ConnectionState.OFFLINE
                        connectionStatus = "PC unreachable (Offline)"
                    }
                }
                triggerAutoReconnect("Retry attempt $retryCount failed")
            }
        }
    }

    fun onConnectionLost(reason: String) {
        socketHolder.value?.let { try { it.close() } catch (_: Exception) {} }
        socketHolder.value = null
        heartbeatJob?.cancel()
        heartbeatJob = null

        if (!userInitiatedDisconnect && autoReconnectEnabled) {
            triggerAutoReconnect(reason)
        } else {
            connectionState = ConnectionState.DISCONNECTED
            connectionStatus = "Disconnected"
        }
    }

    // Heartbeat PING / PONG Job
    fun startHeartbeatJob() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch(Dispatchers.IO) {
            while (isActive && connectionState == ConnectionState.CONNECTED) {
                delay(12000)
                val socket = socketHolder.value
                if (socket == null || !socket.isConnected || socket.isClosed) {
                    Log.w(TAG, "Heartbeat detected closed socket")
                    onConnectionLost("Socket closed")
                    break
                }

                try {
                    val pingJson = "{\"type\":\"PING\"}\n"
                    socket.getOutputStream().write(pingJson.toByteArray(Charsets.UTF_8))
                    socket.getOutputStream().flush()
                } catch (e: Exception) {
                    Log.w(TAG, "Heartbeat ping failed: ${e.message}")
                    onConnectionLost("Heartbeat write failed")
                    break
                }
            }
        }
    }

    LaunchedEffect(connectionState) {
        if (connectionState == ConnectionState.CONNECTED) {
            startHeartbeatJob()
        }
    }

    // App Foreground / Background Lifecycle Management
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                Log.d(TAG, "App backgrounded - pausing heartbeat")
                heartbeatJob?.cancel()
                heartbeatJob = null
            } else if (event == Lifecycle.Event.ON_START) {
                Log.d(TAG, "App foregrounded - checking connection status")
                val activeName = connectedPcName.ifEmpty { savedPcName }
                val activeIp = ipAddress.ifBlank { savedIp }
                if (!userInitiatedDisconnect && autoReconnectEnabled && secureStorage.getAuthToken(activeName).ifEmpty { secureStorage.getAuthToken(activeIp) }.isNotEmpty()) {
                    val socket = socketHolder.value
                    if (socket == null || !socket.isConnected || socket.isClosed) {
                        triggerAutoReconnect("App returned to foreground")
                    } else if (connectionState == ConnectionState.CONNECTED) {
                        startHeartbeatJob()
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Submit user entered pairing code
    fun submitPairCode(targetIp: String, code: String, pcName: String? = null) {
        val cleanIp = targetIp.trim()
        val cleanCode = code.trim()
        if (cleanIp.isEmpty() || cleanCode.isEmpty()) return

        scope.launch(Dispatchers.IO) {
            try {
                val socket = Socket()
                socket.connect(InetSocketAddress(cleanIp, 5000), 5000)
                val inputStream = socket.getInputStream()
                val outputStream = socket.getOutputStream()

                val buffer = ByteArray(256)
                inputStream.read(buffer)

                val submitJson = "{\"type\":\"PAIR_SUBMIT\",\"device_id\":\"$deviceId\",\"pairing_code\":\"$cleanCode\"}\n"
                outputStream.write(submitJson.toByteArray(Charsets.UTF_8))
                outputStream.flush()

                val respBuffer = ByteArray(512)
                val respRead = inputStream.read(respBuffer)
                val resp = if (respRead > 0) String(respBuffer, 0, respRead) else ""
                socket.close()

                withContext(Dispatchers.Main) {
                    if (resp.contains("PAIR_SUCCESS")) {
                        showPairCodeDialog = false
                        try {
                            val tokenIdx = resp.indexOf("\"auth_token\":")
                            if (tokenIdx != -1) {
                                val tokenSub = resp.substring(tokenIdx + 13).trim()
                                val token = tokenSub.substringAfter("\"").takeWhile { it != '"' }
                                if (token.isNotEmpty()) {
                                    val nameToSave = pcName ?: connectedPcName.ifEmpty { savedPcName }.ifEmpty { "PC" }
                                    secureStorage.saveAuthToken(nameToSave, token, cleanIp)
                                    snackbarHostState.showSnackbar("✅ Device paired successfully!")
                                    connectToPc(cleanIp, nameToSave)
                                }
                            }
                        } catch (_: Exception) {
                            pairDialogError = "Error reading auth token"
                        }
                    } else {
                        val reason = if (resp.contains("reason")) {
                            resp.substringAfter("\"reason\":\"").substringBefore("\"")
                        } else {
                            "Incorrect pairing code"
                        }
                        pairDialogError = reason
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    pairDialogError = "Connection error: ${e.message}"
                }
            }
        }
    }

    // Explicit User Disconnect
    fun disconnectUser() {
        userInitiatedDisconnect = true
        reconnectJob?.cancel()
        reconnectJob = null
        heartbeatJob?.cancel()
        heartbeatJob = null

        scope.launch(Dispatchers.IO) {
            try {
                socketHolder.value?.close()
            } catch (_: Exception) {}
            socketHolder.value = null
            withContext(Dispatchers.Main) {
                connectionState = ConnectionState.DISCONNECTED
                connectionStatus = "Disconnected"
            }
        }
    }

    // Revoke & Unpair device
    fun unpairPc(targetIp: String, pcName: String? = null) {
        val cleanIp = targetIp.trim()
        val lookupName = pcName ?: connectedPcName.ifEmpty { savedPcName }
        val token = secureStorage.getAuthToken(lookupName).ifEmpty { secureStorage.getAuthToken(cleanIp) }

        userInitiatedDisconnect = true
        reconnectJob?.cancel()
        reconnectJob = null
        heartbeatJob?.cancel()
        heartbeatJob = null

        scope.launch(Dispatchers.IO) {
            try {
                if (token.isNotEmpty() && cleanIp.isNotEmpty()) {
                    val socket = Socket()
                    socket.connect(InetSocketAddress(cleanIp, 5000), 3000)
                    val inputStream = socket.getInputStream()
                    val outputStream = socket.getOutputStream()

                    val buffer = ByteArray(256)
                    inputStream.read(buffer)

                    val unpairJson = "{\"type\":\"UNPAIR\",\"device_id\":\"$deviceId\",\"token\":\"$token\"}\n"
                    outputStream.write(unpairJson.toByteArray(Charsets.UTF_8))
                    outputStream.flush()
                    socket.close()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Server revoke request failed: ${e.message}")
            } finally {
                secureStorage.removeAuthToken(lookupName)
                secureStorage.removeAuthToken(cleanIp)
                withContext(Dispatchers.Main) {
                    try { socketHolder.value?.close() } catch (_: Exception) {}
                    socketHolder.value = null
                    connectionState = ConnectionState.DISCONNECTED
                    connectionStatus = "Unpaired"
                    snackbarHostState.showSnackbar("Device unpaired")
                }
            }
        }
    }

    // Ensure working authenticated socket for commands
    suspend fun getCommandSocket(): Socket? {
        val currentSocket = socketHolder.value
        if (currentSocket != null && currentSocket.isConnected && !currentSocket.isClosed) {
            return currentSocket
        }

        if (ipAddress.isBlank()) return null
        val token = secureStorage.getAuthToken(connectedPcName).ifEmpty { secureStorage.getAuthToken(ipAddress) }
        if (token.isEmpty()) return null

        return try {
            Log.d(TAG, "Attempting reconnection to port 5000...")
            val newSocket = Socket()
            newSocket.connect(InetSocketAddress(ipAddress.trim(), 5000), 3000)

            val inputStream = newSocket.getInputStream()
            val outputStream = newSocket.getOutputStream()

            val buffer = ByteArray(256)
            inputStream.read(buffer)

            val authJson = "{\"type\":\"AUTH\",\"device_id\":\"$deviceId\",\"token\":\"$token\"}\n"
            outputStream.write(authJson.toByteArray(Charsets.UTF_8))
            outputStream.flush()

            val authRespBuffer = ByteArray(256)
            val authRespRead = inputStream.read(authRespBuffer)
            val authResp = if (authRespRead > 0) String(authRespBuffer, 0, authRespRead) else ""

            if (authResp.contains("AUTH_OK")) {
                socketHolder.value = newSocket
                Log.d(TAG, "Reconnected and authenticated successfully")
                newSocket
            } else {
                newSocket.close()
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Reconnection failed: ${e.message}")
            null
        }
    }

    // Shared function to send commands over TCP socket
    val sendCommandWithResult: suspend (String) -> String? = { command ->
        withContext(Dispatchers.IO) {
            commandMutex.withLock {
                try {
                    val socket = getCommandSocket()
                    if (socket != null) {
                        val output = socket.getOutputStream()
                        val fullCommand = command + "\n"

                        if (command.contains("CLIPBOARD_SET")) {
                            Log.d("REMOTE_DEBUG", "Sending CLIPBOARD_SET")
                        } else {
                            Log.d(TAG, "Sending command: ${command.take(50)}${if(command.length > 50) "..." else ""}")
                        }

                        output.write(fullCommand.toByteArray(Charsets.UTF_8))
                        output.flush()

                        if (command.contains("CLIPBOARD_SET")) {
                            Log.d("REMOTE_DEBUG", "CLIPBOARD_SET sent successfully")
                        }
                        null
                    } else {
                        val err = "No connection or Auth failed"
                        if (command.contains("CLIPBOARD_SET")) Log.e("REMOTE_DEBUG", "CLIPBOARD_SET failed: $err")
                        onConnectionLost("Command socket unavailable")
                        err
                    }
                } catch (e: Exception) {
                    val err = e.message ?: e.javaClass.simpleName
                    if (command.contains("CLIPBOARD_SET")) Log.e("REMOTE_DEBUG", "CLIPBOARD_SET failed", e)
                    socketHolder.value = null
                    onConnectionLost("Command send failed: $err")
                    err
                }
            }
        }
    }

    val sendQueryCommand: suspend (String) -> String? = { command ->
        withContext(Dispatchers.IO) {
            commandMutex.withLock {
                try {
                    val socket = getCommandSocket()
                    if (socket != null) {
                        try {
                            socket.soTimeout = 4000
                            val output = socket.getOutputStream()
                            output.write((command + "\n").toByteArray(Charsets.UTF_8))
                            output.flush()

                            val inputStream = socket.getInputStream()
                            var responseLine: String? = null
                            while (true) {
                                val baos = ByteArrayOutputStream()
                                while (true) {
                                    val b = inputStream.read()
                                    if (b == -1 || b == '\n'.code) break
                                    baos.write(b)
                                }
                                val line = baos.toString("UTF-8").trim()
                                if (line.isEmpty()) break
                                if (line.contains("SYSTEM_STATUS") || line.contains("LAUNCH_APP") || line.contains("APP_LIST") || line.contains("success") || line.contains("pc_name")) {
                                    responseLine = line
                                    break
                                }
                            }
                            responseLine
                        } finally {
                            try { socket.soTimeout = 0 } catch (_: Exception) {}
                        }
                    } else {
                        onConnectionLost("Command socket unavailable")
                        null
                    }
                } catch (e: Exception) {
                    val err = e.message ?: e.javaClass.simpleName
                    Log.e(TAG, "Query command failed: $err", e)
                    socketHolder.value = null
                    onConnectionLost("Query command failed: $err")
                    null
                }
            }
        }
    }

    val sendCommand: (String) -> Unit = { command ->
        scope.launch {
            sendCommandWithResult(command)
        }
    }

    if (showPcStatusScreen) {
        PcStatusScreen(
            connectionState = connectionState,
            connectedPcName = connectedPcName.ifEmpty { savedPcName },
            currentIpAddress = ipAddress.ifBlank { savedIp },
            onBack = { showPcStatusScreen = false },
            onFetchStatus = {
                sendQueryCommand("{\"type\":\"SYSTEM_STATUS\"}")
            },
            onReconnect = {
                triggerAutoReconnect("Status retry clicked")
            }
        )
        return
    }

    if (showAppLauncherScreen) {
        AppLauncherScreen(
            connectionState = connectionState,
            onBack = { showAppLauncherScreen = false },
            onFetchAppList = {
                sendQueryCommand("{\"type\":\"APP_LIST\"}")
            },
            onLaunchApp = { appId ->
                sendQueryCommand("{\"type\":\"LAUNCH_APP\",\"app\":\"$appId\"}")
            },
            onReconnect = {
                triggerAutoReconnect("App Launcher retry clicked")
            }
        )
        return
    }

    if (showMouseScreen) {
        MouseControlScreen(
            onBack = { showMouseScreen = false },
            onSendCommand = sendCommand
        )
        return
    }

    if (showKeyboardScreen) {
        KeyboardControlScreen(
            onBack = { showKeyboardScreen = false },
            onSendCommand = sendCommand
        )
        return
    }

    if (showScreenScreen) {
        ScreenControlScreen(
            ipAddress = ipAddress,
            deviceId = deviceId,
            token = secureStorage.getAuthToken(connectedPcName).ifEmpty { secureStorage.getAuthToken(ipAddress) },
            pcResX = pcResX,
            pcResY = pcResY,
            onBack = { showScreenScreen = false },
            onSendCommand = sendCommand
        )
        return
    }

    if (showClipboardScreen) {
        ClipboardControlScreen(
            onBack = { showClipboardScreen = false },
            onSendCommand = sendCommandWithResult
        )
        return
    }

    if (showFileTransferScreen) {
        FileTransferScreen(
            ipAddress = ipAddress,
            deviceId = deviceId,
            token = secureStorage.getAuthToken(connectedPcName).ifEmpty { secureStorage.getAuthToken(ipAddress) },
            onBack = { showFileTransferScreen = false }
        )
        return
    }

    if (showVoiceControlScreen) {
        VoiceControlScreen(
            onBack = { showVoiceControlScreen = false },
            onSendCommand = sendCommand
        )
        return
    }

    if (showSystemControlScreen) {
        SystemControlScreen(
            onBack = { showSystemControlScreen = false },
            onSendCommand = sendCommand
        )
        return
    }

    if (showQuickControlScreen) {
        QuickControlScreen(
            onBack = { showQuickControlScreen = false },
            onSendCommand = sendCommand
        )
        return
    }

    if (showSettingsScreen) {
        SettingsScreen(
            connectionState = connectionState,
            connectionStatus = connectionStatus,
            connectedPcName = connectedPcName.ifEmpty { savedPcName },
            ipAddress = ipAddress.ifBlank { savedIp },
            pcResX = pcResX,
            pcResY = pcResY,
            deviceId = deviceId,
            deviceName = deviceName,
            autoReconnectEnabled = autoReconnectEnabled,
            onAutoReconnectChanged = { enabled ->
                autoReconnectEnabled = enabled
                prefs.edit().putBoolean("auto_reconnect_enabled", enabled).apply()
            },
            hapticFeedbackEnabled = hapticFeedbackEnabled,
            onHapticFeedbackChanged = { enabled ->
                hapticFeedbackEnabled = enabled
                prefs.edit().putBoolean("haptic_feedback_enabled", enabled).apply()
            },
            screenQuality = screenQualityPreference,
            onScreenQualityChanged = { quality ->
                screenQualityPreference = quality
                prefs.edit().putString("screen_quality", quality).apply()
            },
            onPairNewDevice = {
                showSettingsScreen = false
                showManualIpDialog = true
            },
            onUnpairDevice = {
                unpairPc(ipAddress, connectedPcName)
            },
            onBack = { showSettingsScreen = false }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nexa Remote", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showSettingsScreen = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Devices, "Nexa Remote", Modifier.size(44.dp), MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text("Nexa Remote", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Your PC, remotely connected.", fontSize = 15.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(24.dp))

            // --- CONNECTION & PAIRING CARD ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    val isConnected = connectionState == ConnectionState.CONNECTED

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Connection", fontSize = 20.sp, fontWeight = FontWeight.Bold)

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = when (connectionState) {
                                ConnectionState.CONNECTED -> Color(0xFFE8F5E9)
                                ConnectionState.RECONNECTING, ConnectionState.CONNECTING, ConnectionState.AUTHENTICATING, ConnectionState.DISCOVERING -> Color(0xFFFFF8E1)
                                ConnectionState.OFFLINE, ConnectionState.ERROR -> Color(0xFFFFEBEE)
                                else -> MaterialTheme.colorScheme.surface
                            }
                        ) {
                            val badgeText = when (connectionState) {
                                ConnectionState.CONNECTED -> "🟢 Connected"
                                ConnectionState.RECONNECTING -> "🟡 Reconnecting..."
                                ConnectionState.CONNECTING, ConnectionState.AUTHENTICATING, ConnectionState.DISCOVERING -> "🟡 Connecting..."
                                ConnectionState.OFFLINE -> "🔴 Offline"
                                ConnectionState.ERROR -> "🔴 Connection Error"
                                ConnectionState.DISCONNECTED -> "⚪ Disconnected"
                            }
                            val badgeColor = when (connectionState) {
                                ConnectionState.CONNECTED -> Color(0xFF2E7D32)
                                ConnectionState.RECONNECTING, ConnectionState.CONNECTING, ConnectionState.AUTHENTICATING, ConnectionState.DISCOVERING -> Color(0xFFF57F17)
                                ConnectionState.OFFLINE, ConnectionState.ERROR -> Color(0xFFC62828)
                                ConnectionState.DISCONNECTED -> Color.Gray
                            }
                            Text(
                                text = badgeText,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontSize = 12.sp,
                                color = badgeColor,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (isConnected) {
                        Text(
                            text = "Connected to $connectedPcName\nIP: $ipAddress",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { disconnectUser() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Disconnect")
                            }
                            OutlinedButton(
                                onClick = { unpairPc(ipAddress, connectedPcName) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Unpair")
                            }
                        }
                    } else {
                        if (connectionStatus != "No computer connected" && connectionStatus != "Disconnected") {
                            Text(
                                text = connectionStatus,
                                color = if (connectionState == ConnectionState.CONNECTED) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // --- DISCOVERED PCs SECTION ---
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Available PCs", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            IconButton(onClick = { discoveryManager.startDiscovery() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh PCs", modifier = Modifier.size(20.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        when (val state = discoveryState) {
                            is DiscoveryState.Searching -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Searching for PCs on Wi-Fi...", fontSize = 14.sp, color = Color.Gray)
                                }
                            }

                            is DiscoveryState.Success -> {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    state.pcs.forEach { pc ->
                                        val isPaired = secureStorage.isPaired(pc.name) || secureStorage.isPaired(pc.ipAddress)
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Computer,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(32.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Column {
                                                        Text(pc.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                                        Text(
                                                            if (isPaired) "🔐 Paired • 🟢 Available" else "🟢 Available • ${pc.ipAddress}",
                                                            fontSize = 12.sp,
                                                            color = Color.Gray
                                                        )
                                                    }
                                                }

                                                if (isPaired) {
                                                    Button(
                                                        onClick = { connectToPc(pc.ipAddress, pc.name) },
                                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                                    ) {
                                                        Text("Connect", fontSize = 13.sp)
                                                    }
                                                } else {
                                                    OutlinedButton(
                                                        onClick = { pairToPc(pc.ipAddress, pc.name) },
                                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                                    ) {
                                                        Text("Pair", fontSize = 13.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            is DiscoveryState.Empty -> {
                                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                    Text(state.message, fontSize = 14.sp, color = Color.Gray)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedButton(
                                        onClick = { discoveryManager.startDiscovery() },
                                        modifier = Modifier.align(Alignment.Start)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Retry")
                                    }
                                }
                            }

                            is DiscoveryState.Error -> {
                                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                    Text(state.message, fontSize = 14.sp, color = MaterialTheme.colorScheme.error)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedButton(
                                        onClick = { discoveryManager.startDiscovery() },
                                        modifier = Modifier.align(Alignment.Start)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Retry")
                                    }
                                }
                            }

                            DiscoveryState.Idle -> {}
                        }

                        // --- REMEMBERED LAST PC ---
                        if (savedIp.isNotEmpty() || savedPcName.isNotEmpty()) {
                            val isSavedInDiscovered = (discoveryState as? DiscoveryState.Success)?.pcs?.any {
                                it.name.equals(savedPcName, ignoreCase = true) || it.ipAddress == savedIp
                            } == true
                            if (!isSavedInDiscovered) {
                                val isSavedPaired = secureStorage.isPaired(savedPcName) || secureStorage.isPaired(savedIp)
                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                                Text("Last Connected", fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(6.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                Icons.Default.History,
                                                contentDescription = null,
                                                tint = Color.Gray,
                                                modifier = Modifier.size(28.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(savedPcName.ifEmpty { "Saved PC" }, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                                Text(if (isSavedPaired) "🔐 Paired • $savedIp" else savedIp, fontSize = 12.sp, color = Color.Gray)
                                            }
                                        }
                                        if (isSavedPaired) {
                                            Button(
                                                onClick = { connectToPc(savedIp, savedPcName) },
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text("Connect", fontSize = 13.sp)
                                            }
                                        } else {
                                            OutlinedButton(
                                                onClick = { pairToPc(savedIp, savedPcName) },
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text("Pair", fontSize = 13.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        TextButton(
                            onClick = {
                                manualIpInput = ipAddress
                                showManualIpDialog = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Enter IP manually")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text("Control Center", modifier = Modifier.fillMaxWidth(), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RemoteControlCard(Modifier.weight(1f), Icons.Default.Mouse, "Mouse", "Touchpad & clicks") {
                    if (connectionState == ConnectionState.CONNECTED) showMouseScreen = true
                    else connectionStatus = "Connect to computer first"
                }
                RemoteControlCard(Modifier.weight(1f), Icons.Default.Keyboard, "Keyboard", "Type & shortcuts") {
                    if (connectionState == ConnectionState.CONNECTED) showKeyboardScreen = true
                    else connectionStatus = "Connect to computer first"
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RemoteControlCard(Modifier.weight(1f), Icons.Default.Monitor, "Screen", "Stream & touch") {
                    if (connectionState == ConnectionState.CONNECTED) showScreenScreen = true
                    else connectionStatus = "Connect to computer first"
                }
                RemoteControlCard(Modifier.weight(1f), Icons.Default.ContentPaste, "Clipboard", "Sync text") {
                    if (connectionState == ConnectionState.CONNECTED) showClipboardScreen = true
                    else connectionStatus = "Connect to computer first"
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RemoteControlCard(Modifier.weight(1f), Icons.Default.Folder, "Files", "Transfer files") {
                    if (connectionState == ConnectionState.CONNECTED) showFileTransferScreen = true
                    else connectionStatus = "Connect to computer first"
                }
                RemoteControlCard(Modifier.weight(1f), Icons.Default.PowerSettingsNew, "System", "Power & session") {
                    if (connectionState == ConnectionState.CONNECTED) showSystemControlScreen = true
                    else connectionStatus = "Connect to computer first"
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RemoteControlCard(Modifier.weight(1f), Icons.Default.Tune, "Quick Control", "Wi-Fi & Audio") {
                    if (connectionState == ConnectionState.CONNECTED) showQuickControlScreen = true
                    else connectionStatus = "Connect to computer first"
                }
                RemoteControlCard(Modifier.weight(1f), Icons.Default.Mic, "Voice", "Voice commands") {
                    if (connectionState == ConnectionState.CONNECTED) showVoiceControlScreen = true
                    else connectionStatus = "Connect to computer first"
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RemoteControlCard(Modifier.weight(1f), Icons.Default.Analytics, "PC Status", "CPU & Memory") {
                    showPcStatusScreen = true
                }
                RemoteControlCard(Modifier.weight(1f), Icons.Default.Apps, "App Launcher", "Launch apps") {
                    showAppLauncherScreen = true
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            RemoteControlCard(Modifier.fillMaxWidth(), Icons.Default.Settings, "Settings", "Preferences & About Nexa Remote") {
                showSettingsScreen = true
            }
        }
    }

    if (showManualIpDialog) {
        AlertDialog(
            onDismissRequest = { showManualIpDialog = false },
            title = { Text("Connect Manually") },
            text = {
                Column {
                    Text("Enter your computer's IP address on the local network.", color = Color.Gray, fontSize = 14.sp)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = manualIpInput,
                        onValueChange = { manualIpInput = it },
                        label = { Text("PC IP Address") },
                        placeholder = { Text("e.g. 192.168.1.50") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showManualIpDialog = false
                    val target = manualIpInput.trim()
                    if (secureStorage.isPaired(connectedPcName) || secureStorage.isPaired(target)) {
                        connectToPc(target, connectedPcName.ifEmpty { "PC ($target)" })
                    } else {
                        pairToPc(target, connectedPcName.ifEmpty { "PC ($target)" })
                    }
                }) { Text("Continue") }
            },
            dismissButton = {
                TextButton(onClick = { showManualIpDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showPairCodeDialog) {
        AlertDialog(
            onDismissRequest = { showPairCodeDialog = false },
            title = { Text("Pairing Code") },
            text = {
                Column {
                    Text("Enter the temporary 6-digit code shown on the PC server console:", color = Color.Gray, fontSize = 14.sp)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = pairingCodeInput,
                        onValueChange = { pairingCodeInput = it },
                        label = { Text("Pairing Code") },
                        placeholder = { Text("e.g. 482913") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (pairDialogError.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(pairDialogError, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    submitPairCode(pendingTargetIp, pairingCodeInput, pendingTargetName)
                }) { Text("Submit Pair") }
            },
            dismissButton = {
                TextButton(onClick = { showPairCodeDialog = false }) { Text("Cancel") }
            }
        )
    }
}

private fun cleanServiceName(name: String): String {
    return name
        .removePrefix("NexaRemote-")
        .removePrefix("Remote-")
        .removeSuffix("._remote._tcp.local.")
        .removeSuffix("._remote._tcp.local")
        .removeSuffix(".local")
        .trim()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickControlScreen(
    onBack: () -> Unit,
    onSendCommand: (String) -> Unit
) {
    var brightnessValue by remember { mutableFloatStateOf(50f) }
    var feedbackMessage by remember { mutableStateOf("") }

    fun sendWithFeedback(cmd: String, msg: String) {
        feedbackMessage = msg
        onSendCommand(cmd)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quick Control Panel", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (feedbackMessage.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = feedbackMessage,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // --- NETWORK SECTION ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Network Controls", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.Wifi, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Wi-Fi
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Wi-Fi", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = { sendWithFeedback("{\"type\":\"WIFI_ON\"}", "Enabling Wi-Fi adapter on PC...") },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("On", fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = { sendWithFeedback("{\"type\":\"WIFI_OFF\"}", "Disabling Wi-Fi adapter on PC...") },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Off", fontSize = 12.sp)
                            }
                            IconButton(onClick = { sendWithFeedback("{\"type\":\"WIFI_OPEN_SETTINGS\"}", "Opening Wi-Fi Settings on PC...") }) {
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Settings", modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // Bluetooth
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Bluetooth", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                        OutlinedButton(
                            onClick = { sendWithFeedback("{\"type\":\"BLUETOOTH_OPEN_SETTINGS\"}", "Opening Bluetooth Settings on PC...") },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Settings", fontSize = 12.sp)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // Mobile Hotspot
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.WifiTethering, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Mobile Hotspot", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                        OutlinedButton(
                            onClick = { sendWithFeedback("{\"type\":\"HOTSPOT_OPEN_SETTINGS\"}", "Opening Mobile Hotspot Settings on PC...") },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Settings", fontSize = 12.sp)
                        }
                    }
                }
            }

            // --- DISPLAY & BRIGHTNESS SECTION ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Display Brightness", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("${brightnessValue.toInt()}%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(onClick = {
                            brightnessValue = (brightnessValue - 10f).coerceAtLeast(0f)
                            sendWithFeedback("{\"type\":\"BRIGHTNESS_DOWN\"}", "Decreasing display brightness on PC...")
                        }) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease")
                        }

                        Slider(
                            value = brightnessValue,
                            onValueChange = { brightnessValue = it },
                            onValueChangeFinished = {
                                sendWithFeedback("{\"type\":\"BRIGHTNESS_SET\",\"value\":${brightnessValue.toInt()}}", "Setting brightness to ${brightnessValue.toInt()}% on PC...")
                            },
                            valueRange = 0f..100f,
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(onClick = {
                            brightnessValue = (brightnessValue + 10f).coerceAtMost(100f)
                            sendWithFeedback("{\"type\":\"BRIGHTNESS_UP\"}", "Increasing display brightness on PC...")
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "Increase")
                        }
                    }
                }
            }

            // --- WINDOWS FEATURES SECTION ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Windows Features", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.NightsStay, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Night Light", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                        OutlinedButton(
                            onClick = { sendWithFeedback("{\"type\":\"NIGHT_LIGHT_OPEN_SETTINGS\"}", "Opening Night Light Settings on PC...") },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Settings", fontSize = 12.sp)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DoNotDisturb, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Focus Assist (DND)", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                        OutlinedButton(
                            onClick = { sendWithFeedback("{\"type\":\"DND_OPEN_SETTINGS\"}", "Opening Focus Assist Settings on PC...") },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Settings", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    connectionState: ConnectionState,
    connectionStatus: String,
    connectedPcName: String,
    ipAddress: String,
    pcResX: Int,
    pcResY: Int,
    deviceId: String,
    deviceName: String,
    autoReconnectEnabled: Boolean,
    onAutoReconnectChanged: (Boolean) -> Unit,
    hapticFeedbackEnabled: Boolean,
    onHapticFeedbackChanged: (Boolean) -> Unit,
    screenQuality: String,
    onScreenQualityChanged: (String) -> Unit,
    onPairNewDevice: () -> Unit,
    onUnpairDevice: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val secureStorage = remember { KeystoreEncryptedStorage(context) }
    val isPaired = remember(connectedPcName, ipAddress) {
        secureStorage.isPaired(connectedPcName) || secureStorage.isPaired(ipAddress)
    }

    val versionName = remember {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0"
        } catch (_: Exception) {
            "1.0"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- SECTION 1: CONNECTION SETTINGS ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Connection Settings", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.Wifi, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Status: $connectionStatus ($connectionState)", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    if (connectedPcName.isNotEmpty()) {
                        Text("Connected PC: $connectedPcName", fontSize = 13.sp, color = Color.Gray)
                    }
                    if (ipAddress.isNotEmpty()) {
                        Text("IP Address: $ipAddress", fontSize = 13.sp, color = Color.Gray)
                    }

                    Text("Active Ports: Command (5000) | Stream (5001) | Files (5002)", fontSize = 12.sp, color = Color.Gray)

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto-Reconnect", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text("Automatically recover connection on Wi-Fi/IP change", fontSize = 12.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = autoReconnectEnabled,
                            onCheckedChange = onAutoReconnectChanged
                        )
                    }
                }
            }

            // --- SECTION 2: DEVICE & PAIRING ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Device & Pairing", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Phone Model: $deviceName", fontSize = 13.sp, color = Color.Gray)
                    Text("Device ID: ${deviceId.take(8)}...${deviceId.takeLast(4)}", fontSize = 13.sp, color = Color.Gray)

                    val pairedPcText = if (connectedPcName.isNotEmpty()) connectedPcName else "None"
                    Text("Paired PC: $pairedPcText (${if (isPaired) "🔐 Authenticated" else "Unpaired"})", fontSize = 13.sp, color = Color.Gray)

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onPairNewDevice,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pair New PC", fontSize = 12.sp)
                        }

                        if (isPaired) {
                            OutlinedButton(
                                onClick = onUnpairDevice,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Unpair", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // --- SECTION 3: SCREEN SETTINGS ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Screen Settings", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.Monitor, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Display Resolution: ${pcResX}x${pcResY}", fontSize = 13.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Streaming Quality Preference:", fontSize = 13.sp, fontWeight = FontWeight.Medium)

                    Spacer(modifier = Modifier.height(6.dp))

                    val qualityOptions = listOf("High (85%)", "Medium (60%)", "Low (40%)")
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        qualityOptions.forEach { option ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (screenQuality == option),
                                    onClick = { onScreenQualityChanged(option) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(option, fontSize = 13.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Pinch-to-zoom, panning, and touch coordinate mapping remain enabled.", fontSize = 11.sp, color = Color.Gray)
                }
            }

            // --- SECTION 4: APP PREFERENCES ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("App Preferences", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Haptic Feedback", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text("Provide subtle vibration for remote touch events", fontSize = 12.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = hapticFeedbackEnabled,
                            onCheckedChange = onHapticFeedbackChanged
                        )
                    }
                }
            }

            // --- SECTION 5: ABOUT ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Computer, contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Nexa Remote", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Secure local-network remote control for your Windows PC.", fontSize = 13.sp, color = Color.Gray)

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Text(
                            text = "Version $versionName",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemControlScreen(
    onBack: () -> Unit,
    onSendCommand: (String) -> Unit
) {
    var pendingPowerAction by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("System Controls", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- MEDIA CONTROLS ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Media & Volume", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onSendCommand("{\"type\":\"VOLUME_UP\"}") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Vol +", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = { onSendCommand("{\"type\":\"VOLUME_DOWN\"}") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.VolumeDown, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Vol -", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = { onSendCommand("{\"type\":\"VOLUME_MUTE\"}") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.VolumeOff, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Mute", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onSendCommand("{\"type\":\"MEDIA_PREVIOUS\"}") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Prev", fontSize = 12.sp)
                        }
                        Button(
                            onClick = { onSendCommand("{\"type\":\"MEDIA_PLAY_PAUSE\"}") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Play/Pause", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = { onSendCommand("{\"type\":\"MEDIA_NEXT\"}") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Next", fontSize = 12.sp)
                        }
                    }
                }
            }

            // --- DESKTOP CONTROLS ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Desktop Shortcuts", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { onSendCommand("{\"type\":\"SHOW_DESKTOP\"}") },
                            modifier = Modifier.weight(1f).height(50.dp)
                        ) {
                            Icon(Icons.Default.DesktopWindows, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Show Desktop")
                        }
                        Button(
                            onClick = { onSendCommand("{\"type\":\"LOCK_PC\"}") },
                            modifier = Modifier.weight(1f).height(50.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Lock PC")
                        }
                    }
                }
            }

            // --- POWER CONTROLS (CONFIRMATION REQUIRED) ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Power Management", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Actions require confirmation.", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { pendingPowerAction = "SLEEP" },
                            modifier = Modifier.weight(1f).height(50.dp)
                        ) {
                            Icon(Icons.Default.NightsStay, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sleep", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = { pendingPowerAction = "RESTART" },
                            modifier = Modifier.weight(1f).height(50.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Restart", fontSize = 12.sp)
                        }
                        Button(
                            onClick = { pendingPowerAction = "SHUTDOWN" },
                            modifier = Modifier.weight(1f).height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Shutdown", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    if (pendingPowerAction != null) {
        val action = pendingPowerAction!!
        val title = when (action) {
            "SLEEP" -> "Sleep PC?"
            "RESTART" -> "Restart PC?"
            "SHUTDOWN" -> "Shutdown PC?"
            else -> ""
        }
        val message = when (action) {
            "SLEEP" -> "Are you sure you want to put your PC to sleep?"
            "RESTART" -> "Are you sure you want to restart your PC?"
            "SHUTDOWN" -> "Are you sure you want to shut down your PC?"
            else -> ""
        }
        val cmdType = when (action) {
            "SLEEP" -> "SLEEP_PC"
            "RESTART" -> "RESTART_PC"
            "SHUTDOWN" -> "SHUTDOWN_PC"
            else -> ""
        }

        AlertDialog(
            onDismissRequest = { pendingPowerAction = null },
            title = { Text(title, fontWeight = FontWeight.Bold) },
            text = { Text(message, fontSize = 14.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        pendingPowerAction = null
                        onSendCommand("{\"type\":\"$cmdType\"}")
                    },
                    colors = if (action == "SHUTDOWN" || action == "RESTART") {
                        ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    } else {
                        ButtonDefaults.buttonColors()
                    }
                ) {
                    Text(action.lowercase(Locale.ROOT).replaceFirstChar { it.uppercase() })
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingPowerAction = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteControlCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(108.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MouseControlScreen(onBack: () -> Unit, onSendCommand: (String) -> Unit) {
    val viewConfiguration = LocalViewConfiguration.current
    var isDragging by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mouse Control", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            var accumulatedX = 0f
                            var accumulatedY = 0f
                            var isMoved = false
                            val downTime = System.currentTimeMillis()
                            val startPosition = down.position

                            isDragging = false

                            do {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break

                                if (change.pressed) {
                                    val dragAmount = change.position - change.previousPosition
                                    val totalDistance = (change.position - startPosition).getDistance()

                                    if (totalDistance > viewConfiguration.touchSlop) {
                                        isMoved = true
                                        isDragging = true
                                    }

                                    if (isMoved) {
                                        change.consume()
                                        accumulatedX += dragAmount.x
                                        accumulatedY += dragAmount.y

                                        val dx = accumulatedX.toInt()
                                        val dy = accumulatedY.toInt()

                                        if (dx != 0 || dy != 0) {
                                            onSendCommand("{\"type\":\"MOVE\",\"dx\":$dx,\"dy\":$dy}")
                                            accumulatedX -= dx
                                            accumulatedY -= dy
                                        }
                                    }
                                } else {
                                    break
                                }
                            } while (event.changes.any { it.pressed })

                            val duration = System.currentTimeMillis() - downTime
                            val finalDistance = (down.position - startPosition).getDistance()

                            if (!isMoved && finalDistance < viewConfiguration.touchSlop && duration < 300) {
                                onSendCommand("{\"type\":\"CLICK\",\"button\":\"left\"}")
                            }

                            isDragging = false
                            accumulatedX = 0f
                            accumulatedY = 0f
                        }
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.TouchApp,
                            contentDescription = null,
                            modifier = Modifier.size(52.dp),
                            tint = if (isDragging) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isDragging) "Moving Cursor..." else "Touchpad",
                            fontSize = 16.sp,
                            color = if (isDragging) MaterialTheme.colorScheme.primary else Color.Gray,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap = Left Click • Drag = Move • Use buttons for Right Click & Scroll",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { onSendCommand("{\"type\":\"SCROLL\",\"amount\":10}") },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Scroll Up")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Scroll Up")
                }
                OutlinedButton(
                    onClick = { onSendCommand("{\"type\":\"SCROLL\",\"amount\":-10}") },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Scroll Down")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Scroll Down")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { onSendCommand("{\"type\":\"CLICK\",\"button\":\"left\"}") },
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    Text("Left Click")
                }
                Button(
                    onClick = { onSendCommand("{\"type\":\"CLICK\",\"button\":\"right\"}") },
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    Text("Right Click")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyboardControlScreen(onBack: () -> Unit, onSendCommand: (String) -> Unit) {
    var textInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Keyboard Control", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                label = { Text("Type text to send") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (textInput.isNotEmpty()) {
                        val escapedText = textInput.replace("\"", "\\\"").replace("\n", "\\n")
                        onSendCommand("{\"type\":\"TYPE\",\"text\":\"$escapedText\"}")
                        textInput = ""
                    }
                })
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (textInput.isNotEmpty()) {
                        val escapedText = textInput.replace("\"", "\\\"").replace("\n", "\\n")
                        onSendCommand("{\"type\":\"TYPE\",\"text\":\"$escapedText\"}")
                        textInput = ""
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Send Text")
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Special Keys", modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KeyButton("Esc", Modifier.weight(1f)) { onSendCommand("{\"type\":\"KEY\",\"key\":\"esc\"}") }
                KeyButton("Tab", Modifier.weight(1f)) { onSendCommand("{\"type\":\"KEY\",\"key\":\"tab\"}") }
                KeyButton("Win", Modifier.weight(1f)) { onSendCommand("{\"type\":\"KEY\",\"key\":\"win\"}") }
                KeyButton("Backspace", Modifier.weight(1.5f)) { onSendCommand("{\"type\":\"KEY\",\"key\":\"backspace\"}") }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KeyButton("Space", Modifier.weight(2f)) { onSendCommand("{\"type\":\"KEY\",\"key\":\"space\"}") }
                KeyButton("Enter", Modifier.weight(1.5f)) { onSendCommand("{\"type\":\"KEY\",\"key\":\"enter\"}") }
                KeyButton("Del", Modifier.weight(1f)) { onSendCommand("{\"type\":\"KEY\",\"key\":\"delete\"}") }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Arrow Keys", modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                KeyButton(icon = Icons.Default.KeyboardArrowUp, modifier = Modifier.width(80.dp)) {
                    onSendCommand("{\"type\":\"KEY\",\"key\":\"up\"}")
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KeyButton(icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft, modifier = Modifier.width(80.dp)) {
                        onSendCommand("{\"type\":\"KEY\",\"key\":\"left\"}")
                    }
                    KeyButton(icon = Icons.Default.KeyboardArrowDown, modifier = Modifier.width(80.dp)) {
                        onSendCommand("{\"type\":\"KEY\",\"key\":\"down\"}")
                    }
                    KeyButton(icon = Icons.AutoMirrored.Filled.KeyboardArrowRight, modifier = Modifier.width(80.dp)) {
                        onSendCommand("{\"type\":\"KEY\",\"key\":\"right\"}")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Shortcuts", modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KeyButton("Ctrl+C", Modifier.weight(1f)) { onSendCommand("{\"type\":\"HOTKEY\",\"keys\":[\"ctrl\",\"c\"]}") }
                KeyButton("Ctrl+V", Modifier.weight(1f)) { onSendCommand("{\"type\":\"HOTKEY\",\"keys\":[\"ctrl\",\"v\"]}") }
                KeyButton("Ctrl+Z", Modifier.weight(1f)) { onSendCommand("{\"type\":\"HOTKEY\",\"keys\":[\"ctrl\",\"z\"]}") }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KeyButton("Alt+Tab", Modifier.weight(1f)) { onSendCommand("{\"type\":\"HOTKEY\",\"keys\":[\"alt\",\"tab\"]}") }
                KeyButton("Win+D", Modifier.weight(1f)) { onSendCommand("{\"type\":\"HOTKEY\",\"keys\":[\"win\",\"d\"]}") }
                KeyButton("Win+L", Modifier.weight(1f)) { onSendCommand("{\"type\":\"HOTKEY\",\"keys\":[\"win\",\"l\"]}") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenControlScreen(
    ipAddress: String,
    deviceId: String,
    token: String,
    pcResX: Int,
    pcResY: Int,
    onBack: () -> Unit,
    onSendCommand: (String) -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context as? Activity }

    var screenBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var statusMessage by remember { mutableStateOf("Connecting to screen stream...") }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var isFullscreen by remember { mutableStateOf(false) }

    fun enterFullscreen() {
        isFullscreen = true
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        activity?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowInsetsControllerCompat(window, window.decorView).apply {
                hide(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

    fun exitFullscreen() {
        isFullscreen = false
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, true)
            WindowInsetsControllerCompat(window, window.decorView).apply {
                show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exitFullscreen()
        }
    }

    BackHandler {
        if (isFullscreen) {
            exitFullscreen()
        } else {
            onBack()
        }
    }

    LaunchedEffect(ipAddress, deviceId, token) {
        withContext(Dispatchers.IO) {
            while (isActive) {
                var socket: Socket? = null
                try {
                    socket = Socket()
                    socket.connect(InetSocketAddress(ipAddress.trim(), 5001), 4000)
                    val inputStream = socket.getInputStream()
                    val outputStream = socket.getOutputStream()

                    outputStream.write("AUTH|$deviceId|$token\n".toByteArray(Charsets.UTF_8))
                    outputStream.flush()

                    val authBuf = ByteArray(64)
                    val aRead = inputStream.read(authBuf)
                    val authResp = if (aRead > 0) String(authBuf, 0, aRead) else ""

                    if (!authResp.contains("AUTH_OK")) {
                        withContext(Dispatchers.Main) { statusMessage = "Screen stream unavailable" }
                        socket.close()
                        delay(3000)
                        continue
                    }

                    withContext(Dispatchers.Main) { statusMessage = "Streaming" }

                    while (isActive && socket.isConnected && !socket.isClosed) {
                        val lengthBuffer = ByteArray(4)
                        var read = 0
                        while (read < 4) {
                            val count = inputStream.read(lengthBuffer, read, 4 - read)
                            if (count <= 0) break
                            read += count
                        }
                        if (read < 4) break

                        val frameLen = ByteBuffer.wrap(lengthBuffer).order(ByteOrder.BIG_ENDIAN).int
                        if (frameLen <= 0 || frameLen > 10000000) continue

                        val frameBuffer = ByteArray(frameLen)
                        var frameRead = 0
                        while (frameRead < frameLen) {
                            val count = inputStream.read(frameBuffer, frameRead, frameLen - frameRead)
                            if (count <= 0) break
                            frameRead += count
                        }
                        if (frameRead < frameLen) break

                        val bitmap = BitmapFactory.decodeByteArray(frameBuffer, 0, frameLen)
                        if (bitmap != null) {
                            withContext(Dispatchers.Main) {
                                screenBitmap = bitmap
                            }
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        statusMessage = "Screen stream unavailable"
                    }
                    delay(2000)
                } finally {
                    try { socket?.close() } catch (_: Exception) {}
                }
            }
        }
    }

    Scaffold(
        topBar = {
            if (!isFullscreen) {
                TopAppBar(
                    title = { Text("Screen Streaming", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { enterFullscreen() }) {
                            Icon(Icons.Default.Fullscreen, contentDescription = "Fullscreen Landscape")
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isFullscreen) PaddingValues(0.dp) else paddingValues)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            val currentBitmap = screenBitmap
            if (currentBitmap != null) {
                Image(
                    bitmap = currentBitmap.asImageBitmap(),
                    contentDescription = "Screen Stream",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { containerSize = it.size }
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 5f)
                                if (scale == 1f) {
                                    offset = Offset.Zero
                                } else {
                                    offset += pan
                                }
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { tapOffset ->
                                    val mapped = mapTouchToPc(tapOffset.x, tapOffset.y, pcResX, pcResY, containerSize, currentBitmap)
                                    if (mapped != null) {
                                        onSendCommand("{\"type\":\"MOVE_ABS\",\"x\":${mapped.x.toInt()},\"y\":${mapped.y.toInt()}}")
                                        onSendCommand("{\"type\":\"CLICK\",\"button\":\"left\"}")
                                    }
                                },
                                onLongPress = { tapOffset ->
                                    val mapped = mapTouchToPc(tapOffset.x, tapOffset.y, pcResX, pcResY, containerSize, currentBitmap)
                                    if (mapped != null) {
                                        onSendCommand("{\"type\":\"MOVE_ABS\",\"x\":${mapped.x.toInt()},\"y\":${mapped.y.toInt()}}")
                                        onSendCommand("{\"type\":\"CLICK\",\"button\":\"right\"}")
                                    }
                                }
                            )
                        }
                )

                if (isFullscreen) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.5f),
                            contentColor = Color.White
                        ) {
                            IconButton(onClick = { exitFullscreen() }) {
                                Icon(Icons.Default.FullscreenExit, contentDescription = "Exit Fullscreen")
                            }
                        }
                    }
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(statusMessage, color = Color.White, fontSize = 14.sp)
                }
            }
        }
    }
}

private fun mapTouchToPc(
    touchX: Float,
    touchY: Float,
    pcResX: Int,
    pcResY: Int,
    containerSize: IntSize,
    bitmap: Bitmap
): Offset? {
    if (containerSize.width <= 0 || containerSize.height <= 0) return null

    val containerWidth = containerSize.width.toFloat()
    val containerHeight = containerSize.height.toFloat()
    val imageWidth = bitmap.width.toFloat()
    val imageHeight = bitmap.height.toFloat()

    val containerAspect = containerWidth / containerHeight
    val imageAspect = imageWidth / imageHeight

    val renderedWidth: Float
    val renderedHeight: Float
    val offsetX: Float
    val offsetY: Float

    if (containerAspect > imageAspect) {
        renderedHeight = containerHeight
        renderedWidth = imageAspect * containerHeight
        offsetX = (containerWidth - renderedWidth) / 2f
        offsetY = 0f
    } else {
        renderedWidth = containerWidth
        renderedHeight = containerWidth / imageAspect
        offsetX = 0f
        offsetY = (containerHeight - renderedHeight) / 2f
    }

    val relativeX = touchX - offsetX
    val relativeY = touchY - offsetY

    if (relativeX < 0 || relativeX > renderedWidth || relativeY < 0 || relativeY > renderedHeight) {
        return null
    }

    val pcX = (relativeX / renderedWidth) * pcResX
    val pcY = (relativeY / renderedHeight) * pcResY

    return Offset(pcX, pcY)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipboardControlScreen(
    onBack: () -> Unit,
    onSendCommand: suspend (String) -> String?
) {
    var clipboardText by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Clipboard Control", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Send text directly to your PC's clipboard.", color = Color.Gray, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = clipboardText,
                onValueChange = { clipboardText = it },
                label = { Text("Clipboard Content") },
                modifier = Modifier.fillMaxWidth().height(150.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (clipboardText.isNotEmpty()) {
                        statusMessage = "Sending..."
                        scope.launch {
                            val jsonText = clipboardText
                                .replace("\\", "\\\\")
                                .replace("\"", "\\\"")
                                .replace("\n", "\\n")
                                .replace("\r", "\\r")
                                .replace("\t", "\\t")
                            val err = onSendCommand("{\"type\":\"CLIPBOARD_SET\",\"text\":\"$jsonText\"}")
                            if (err == null) {
                                statusMessage = "Clipboard updated on PC!"
                            } else {
                                statusMessage = "Failed: $err"
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Copy to PC Clipboard")
            }

            if (statusMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(statusMessage, color = if (statusMessage.startsWith("Clipboard")) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileTransferScreen(
    ipAddress: String,
    deviceId: String,
    token: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("") }
    var fileSize by remember { mutableLongStateOf(0L) }
    var isTransferring by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var statusMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedFileUri = uri
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    fileName = if (nameIndex != -1) cursor.getString(nameIndex) else "file"
                    fileSize = if (sizeIndex != -1) cursor.getLong(sizeIndex) else 0L
                }
            }
            statusMessage = "File selected: $fileName (${fileSize / 1024} KB)"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("File Transfer", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Upload File to PC", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(if (fileName.isNotEmpty()) fileName else "No file selected", fontSize = 14.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { filePickerLauncher.launch("*/*") }) {
                            Text("Select File")
                        }
                        Button(
                            onClick = {
                                val uri = selectedFileUri ?: return@Button
                                isTransferring = true
                                progress = 0f
                                statusMessage = "Uploading $fileName..."
                                scope.launch(Dispatchers.IO) {
                                    var socket: Socket? = null
                                    try {
                                        socket = Socket()
                                        socket.connect(InetSocketAddress(ipAddress.trim(), 5002), 5000)
                                        val inputStream = socket.getInputStream()
                                        val output = socket.getOutputStream()

                                        output.write("AUTH|$deviceId|$token|UPLOAD|$fileName|$fileSize\n".toByteArray(Charsets.UTF_8))
                                        output.flush()

                                        val authRespBuf = ByteArray(64)
                                        val aRead = inputStream.read(authRespBuf)
                                        val authResp = if (aRead > 0) String(authRespBuf, 0, aRead) else ""

                                        if (!authResp.contains("AUTH_OK")) {
                                            withContext(Dispatchers.Main) {
                                                statusMessage = "Upload Failed: Auth Error"
                                                isTransferring = false
                                            }
                                            socket.close()
                                            return@launch
                                        }

                                        context.contentResolver.openInputStream(uri)?.use { inputStreamFile ->
                                            val buffer = ByteArray(65536)
                                            var sent = 0L
                                            while (sent < fileSize) {
                                                val count = inputStreamFile.read(buffer)
                                                if (count <= 0) break
                                                output.write(buffer, 0, count)
                                                sent += count
                                                val p = sent.toFloat() / fileSize.toFloat()
                                                withContext(Dispatchers.Main) { progress = p }
                                            }
                                            output.flush()
                                        }
                                        socket.close()
                                        withContext(Dispatchers.Main) {
                                            statusMessage = "Upload Complete!"
                                            isTransferring = false
                                        }
                                    } catch (e: Exception) {
                                        try { socket?.close() } catch (_: Exception) {}
                                        withContext(Dispatchers.Main) {
                                            statusMessage = "Upload Failed: ${e.message}"
                                            isTransferring = false
                                        }
                                    }
                                }
                            },
                            enabled = selectedFileUri != null && !isTransferring
                        ) {
                            Text("Upload")
                        }
                    }
                }
            }

            if (isTransferring) {
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            }

            if (statusMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(statusMessage, fontSize = 14.sp, color = Color.Gray)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceControlScreen(onBack: () -> Unit, onSendCommand: (String) -> Unit) {
    val context = LocalContext.current
    var recognizedText by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Tap the microphone to speak") }
    var isListening by remember { mutableStateOf(false) }

    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }

    DisposableEffect(Unit) {
        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { status = "Listening..." }
            override fun onBeginningOfSpeech() { status = "Listening..." }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                status = "Processing command..."
                isListening = false
            }
            override fun onError(error: Int) {
                status = "Error listening ($error). Tap mic to retry."
                isListening = false
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val text = matches[0]
                    recognizedText = text
                    parseAndExecuteCommand(text, onSendCommand) { status = it }
                } else {
                    status = "No speech recognized"
                }
                isListening = false
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
        speechRecognizer.setRecognitionListener(listener)
        onDispose { speechRecognizer.destroy() }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            }
            speechRecognizer.startListening(intent)
            isListening = true
        } else {
            status = "Audio permission required"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Voice Control", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            FloatingActionButton(
                onClick = {
                    if (!isListening) {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    } else {
                        speechRecognizer.stopListening()
                        isListening = false
                    }
                },
                modifier = Modifier.size(80.dp),
                containerColor = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Mic, contentDescription = "Mic", modifier = Modifier.size(40.dp))
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(status, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.Gray)

            if (recognizedText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("\"$recognizedText\"", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(30.dp))
            Text("Voice Commands Help", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    HelpGroup("Mouse Commands", "click, right click, double click, scroll up, scroll down")
                    HelpGroup("Keyboard / Navigation", "enter, space, backspace, escape, tab, delete, up, down, left, right")
                    HelpGroup("Shortcuts", "copy, paste, undo, redo, save, find, select all, cut, show desktop, lock pc")
                    HelpGroup("Media", "volume up, volume down, mute, play, pause, next song, previous song")
                    HelpGroup("Typing", "type [text to type] (e.g. \"type hello world\")")
                }
            }
        }
    }
}

@Composable
fun HelpGroup(title: String, examples: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(title, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(examples, color = Color.Gray, fontSize = 12.sp)
    }
}

fun parseAndExecuteCommand(
    text: String,
    onSendCommand: (String) -> Unit,
    updateStatus: (String) -> Unit
) {
    var cmd = text.lowercase(Locale.ROOT)
    cmd = Pattern.compile("[^a-z0-9\\s/-]").matcher(cmd.trim()).replaceAll("")
    cmd = Pattern.compile("\\s+").matcher(cmd).replaceAll(" ")

    Log.d("VoiceControl", "Normalized: $cmd")

    if (cmd.startsWith("hit ") || cmd.startsWith("press ")) {
        cmd = cmd.substringAfter(" ")
    }

    when (cmd) {
        "click", "left click", "left mouse click" -> {
            updateStatus("Executing: Left Click")
            onSendCommand("{\"type\":\"CLICK\",\"button\":\"left\"}")
        }
        "right click", "right mouse click" -> {
            updateStatus("Executing: Right Click")
            onSendCommand("{\"type\":\"CLICK\",\"button\":\"right\"}")
        }
        "double click", "double-click" -> {
            updateStatus("Executing: Double Click")
            onSendCommand("{\"type\":\"CLICK\",\"button\":\"left\"}")
            onSendCommand("{\"type\":\"CLICK\",\"button\":\"left\"}")
        }
        "scroll up", "scroll upward" -> {
            updateStatus("Executing: Scroll Up")
            onSendCommand("{\"type\":\"SCROLL\",\"amount\":10}")
        }
        "scroll down", "scroll downward" -> {
            updateStatus("Executing: Scroll Down")
            onSendCommand("{\"type\":\"SCROLL\",\"amount\":-10}")
        }
        "enter" -> {
            updateStatus("Executing: Enter")
            onSendCommand("{\"type\":\"KEY\",\"key\":\"enter\"}")
        }
        "escape", "esc" -> {
            updateStatus("Executing: Escape")
            onSendCommand("{\"type\":\"KEY\",\"key\":\"esc\"}")
        }
        "tab" -> {
            updateStatus("Executing: Tab")
            onSendCommand("{\"type\":\"KEY\",\"key\":\"tab\"}")
        }
        "backspace" -> {
            updateStatus("Executing: Backspace")
            onSendCommand("{\"type\":\"KEY\",\"key\":\"backspace\"}")
        }
        "delete" -> {
            updateStatus("Executing: Delete")
            onSendCommand("{\"type\":\"KEY\",\"key\":\"delete\"}")
        }
        "space" -> {
            updateStatus("Executing: Space")
            onSendCommand("{\"type\":\"KEY\",\"key\":\"space\"}")
        }
        "home" -> {
            updateStatus("Executing: Home")
            onSendCommand("{\"type\":\"KEY\",\"key\":\"home\"}")
        }
        "end" -> {
            updateStatus("Executing: End")
            onSendCommand("{\"type\":\"KEY\",\"key\":\"end\"}")
        }
        "page up" -> {
            updateStatus("Executing: Page Up")
            onSendCommand("{\"type\":\"KEY\",\"key\":\"pageup\"}")
        }
        "page down" -> {
            updateStatus("Executing: Page Down")
            onSendCommand("{\"type\":\"KEY\",\"key\":\"pagedown\"}")
        }
        "arrow up", "up arrow", "up" -> {
            updateStatus("Executing: Up")
            onSendCommand("{\"type\":\"KEY\",\"key\":\"up\"}")
        }
        "arrow down", "down arrow", "down" -> {
            updateStatus("Executing: Down")
            onSendCommand("{\"type\":\"KEY\",\"key\":\"down\"}")
        }
        "arrow left", "left arrow", "left" -> {
            updateStatus("Executing: Left")
            onSendCommand("{\"type\":\"KEY\",\"key\":\"left\"}")
        }
        "arrow right", "right arrow", "right" -> {
            updateStatus("Executing: Right")
            onSendCommand("{\"type\":\"KEY\",\"key\":\"right\"}")
        }
        "alt tab", "alt-tab" -> {
            updateStatus("Executing: Alt+Tab")
            onSendCommand("{\"type\":\"HOTKEY\",\"keys\":[\"alt\",\"tab\"]}")
        }
        "copy", "ctrl c", "control c" -> {
            updateStatus("Executing: Copy")
            onSendCommand("{\"type\":\"HOTKEY\",\"keys\":[\"ctrl\",\"c\"]}")
        }
        "paste", "ctrl v", "control v" -> {
            updateStatus("Executing: Paste")
            onSendCommand("{\"type\":\"HOTKEY\",\"keys\":[\"ctrl\",\"v\"]}")
        }
        "undo", "ctrl z", "control z" -> {
            updateStatus("Executing: Undo")
            onSendCommand("{\"type\":\"HOTKEY\",\"keys\":[\"ctrl\",\"z\"]}")
        }
        "redo", "ctrl y", "control y" -> {
            updateStatus("Executing: Redo")
            onSendCommand("{\"type\":\"HOTKEY\",\"keys\":[\"ctrl\",\"y\"]}")
        }
        "save", "ctrl s", "control s" -> {
            updateStatus("Executing: Save")
            onSendCommand("{\"type\":\"HOTKEY\",\"keys\":[\"ctrl\",\"s\"]}")
        }
        "find", "ctrl f", "control f" -> {
            updateStatus("Executing: Find")
            onSendCommand("{\"type\":\"HOTKEY\",\"keys\":[\"ctrl\",\"f\"]}")
        }
        "select all", "ctrl a", "control a" -> {
            updateStatus("Executing: Select All")
            onSendCommand("{\"type\":\"HOTKEY\",\"keys\":[\"ctrl\",\"a\"]}")
        }
        "cut", "ctrl x", "control x" -> {
            updateStatus("Executing: Cut")
            onSendCommand("{\"type\":\"HOTKEY\",\"keys\":[\"ctrl\",\"x\"]}")
        }
        "show desktop", "windows d", "windows desktop", "go to desktop" -> {
            updateStatus("Executing: Show Desktop")
            onSendCommand("{\"type\":\"SHOW_DESKTOP\"}")
        }
        "lock computer", "lock pc", "windows l" -> {
            updateStatus("Executing: Lock PC")
            onSendCommand("{\"type\":\"LOCK_PC\"}")
        }
        "volume up", "increase volume" -> {
            updateStatus("Executing: Volume Up")
            onSendCommand("{\"type\":\"VOLUME_UP\"}")
        }
        "volume down", "decrease volume" -> {
            updateStatus("Executing: Volume Down")
            onSendCommand("{\"type\":\"VOLUME_DOWN\"}")
        }
        "mute", "mute volume" -> {
            updateStatus("Executing: Mute")
            onSendCommand("{\"type\":\"VOLUME_MUTE\"}")
        }
        "play", "pause", "play pause", "play/pause" -> {
            updateStatus("Executing: Play/Pause")
            onSendCommand("{\"type\":\"MEDIA_PLAY_PAUSE\"}")
        }
        "next track", "next song" -> {
            updateStatus("Executing: Next Track")
            onSendCommand("{\"type\":\"MEDIA_NEXT\"}")
        }
        "previous track", "previous song" -> {
            updateStatus("Executing: Previous Track")
            onSendCommand("{\"type\":\"MEDIA_PREVIOUS\"}")
        }
        "maximize", "maximize window" -> {
            updateStatus("Executing: Maximize")
            onSendCommand("{\"type\":\"HOTKEY\",\"keys\":[\"win\",\"up\"]}")
        }
        "minimize", "minimize window" -> {
            updateStatus("Executing: Minimize")
            onSendCommand("{\"type\":\"HOTKEY\",\"keys\":[\"win\",\"down\"]}")
        }
        "close window" -> {
            updateStatus("Executing: Close Window")
            onSendCommand("{\"type\":\"HOTKEY\",\"keys\":[\"alt\",\"f4\"]}")
        }
        else -> {
            if (Pattern.compile("f[1-9]|f1[0-2]").matcher(cmd).matches()) {
                updateStatus("Executing: $cmd")
                onSendCommand("{\"type\":\"KEY\",\"key\":\"$cmd\"}")
            } else if (cmd.startsWith("type ") || cmd.startsWith("write ")) {
                val textToType = if (cmd.startsWith("type ")) cmd.removePrefix("type ") else cmd.removePrefix("write ")
                if (textToType.trim().isNotEmpty()) {
                    updateStatus("Executing: Type")
                    val escaped = textToType.trim().replace("\"", "\\\"")
                    onSendCommand("{\"type\":\"TYPE\",\"text\":\"$escaped\"}")
                } else {
                    updateStatus("Type what?")
                }
            } else if (cmd == "shift" || cmd == "control" || cmd == "ctrl" || cmd == "alt") {
                updateStatus("Executing: $cmd")
                onSendCommand("{\"type\":\"KEY\",\"key\":\"$cmd\"}")
            } else {
                updateStatus("Command not supported")
            }
        }
    }
}

@Composable
fun KeyButton(
    text: String? = null,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = text ?: "", modifier = Modifier.size(20.dp))
        } else if (text != null) {
            Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}
