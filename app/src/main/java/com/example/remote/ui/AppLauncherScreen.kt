package com.example.remote.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.remote.ConnectionState
import kotlinx.coroutines.launch
import org.json.JSONObject

data class AppInfoItem(
    val id: String,
    val name: String,
    val description: String,
    val available: Boolean = true
)

data class LaunchAppResponse(
    val success: Boolean,
    val app: String,
    val message: String
)

fun parseLaunchAppResponse(jsonStr: String): LaunchAppResponse {
    return try {
        val obj = JSONObject(jsonStr)
        LaunchAppResponse(
            success = obj.optBoolean("success", false),
            app = obj.optString("app", ""),
            message = obj.optString("message", "Unable to launch application")
        )
    } catch (e: Exception) {
        LaunchAppResponse(success = false, app = "", message = e.message ?: "Unable to launch application")
    }
}

fun parseAppListResponse(jsonStr: String): List<AppInfoItem>? {
    return try {
        val obj = JSONObject(jsonStr)
        if (!obj.optBoolean("success", false)) return null
        val arr = obj.optJSONArray("apps") ?: return null
        val list = mutableListOf<AppInfoItem>()
        for (i in 0 until arr.length()) {
            val item = arr.getJSONObject(i)
            list.add(
                AppInfoItem(
                    id = item.optString("id", ""),
                    name = item.optString("name", ""),
                    description = item.optString("description", ""),
                    available = item.optBoolean("available", true)
                )
            )
        }
        list
    } catch (e: Exception) {
        null
    }
}

val DEFAULT_APP_LIST = listOf(
    AppInfoItem("chrome", "Google Chrome", "Web Browser"),
    AppInfoItem("edge", "Microsoft Edge", "Web Browser"),
    AppInfoItem("vscode", "Visual Studio Code", "Code Editor"),
    AppInfoItem("explorer", "File Explorer", "Windows File Manager"),
    AppInfoItem("notepad", "Notepad", "Text Editor"),
    AppInfoItem("calculator", "Calculator", "Windows Calculator"),
    AppInfoItem("spotify", "Spotify", "Music Streaming"),
    AppInfoItem("settings", "Windows Settings", "System Settings"),
    AppInfoItem("task_manager", "Task Manager", "Process & Performance Monitor")
)

fun getAppIcon(appId: String): ImageVector {
    return when (appId.lowercase()) {
        "chrome" -> Icons.Default.Language
        "edge" -> Icons.Default.Public
        "vscode" -> Icons.Default.Code
        "explorer" -> Icons.Default.FolderOpen
        "notepad" -> Icons.Default.Description
        "calculator" -> Icons.Default.Calculate
        "spotify" -> Icons.Default.MusicNote
        "settings" -> Icons.Default.Settings
        "task_manager" -> Icons.Default.Assessment
        else -> Icons.Default.Apps
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLauncherScreen(
    connectionState: ConnectionState,
    onBack: () -> Unit,
    onFetchAppList: suspend () -> String?,
    onLaunchApp: suspend (String) -> String?,
    onReconnect: (() -> Unit)? = null
) {
    var appList by remember { mutableStateOf(DEFAULT_APP_LIST) }
    var launchingAppId by remember { mutableStateOf<String?>(null) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var feedbackIsError by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    fun refreshList() {
        if (connectionState != ConnectionState.CONNECTED) return
        isRefreshing = true
        scope.launch {
            val json = onFetchAppList()
            if (json != null) {
                val updated = parseAppListResponse(json)
                if (updated != null && updated.isNotEmpty()) {
                    appList = updated
                }
            }
            isRefreshing = false
        }
    }

    LaunchedEffect(connectionState) {
        if (connectionState == ConnectionState.CONNECTED) {
            refreshList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Launcher", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { refreshList() },
                        enabled = connectionState == ConnectionState.CONNECTED && !isRefreshing
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            if (connectionState != ConnectionState.CONNECTED) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "PC is disconnected",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Connect to your PC to launch applications.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                        )
                        if (onReconnect != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = onReconnect,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Retry Connection")
                            }
                        }
                    }
                }
            }

            if (feedbackMessage != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (feedbackIsError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = feedbackMessage ?: "",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (feedbackIsError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Text(
                text = "Launch applications on your remote PC",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            appList.forEach { app ->
                AppCard(
                    app = app,
                    isLaunching = launchingAppId == app.id,
                    isConnected = connectionState == ConnectionState.CONNECTED,
                    onOpen = {
                        if (connectionState != ConnectionState.CONNECTED) {
                            feedbackMessage = "PC is disconnected"
                            feedbackIsError = true
                            return@AppCard
                        }

                        launchingAppId = app.id
                        feedbackMessage = "Launching ${app.name}..."
                        feedbackIsError = false

                        scope.launch {
                            val jsonResp = onLaunchApp(app.id)
                            launchingAppId = null

                            if (jsonResp != null) {
                                val parsed = parseLaunchAppResponse(jsonResp)
                                if (parsed.success) {
                                    feedbackMessage = parsed.message.ifEmpty { "${app.name} launched" }
                                    feedbackIsError = false
                                } else {
                                    feedbackMessage = parsed.message.ifEmpty { "Unable to launch application" }
                                    feedbackIsError = true
                                }
                            } else {
                                feedbackMessage = "Unable to launch application"
                                feedbackIsError = true
                            }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun AppCard(
    app: AppInfoItem,
    isLaunching: Boolean,
    isConnected: Boolean,
    onOpen: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = getAppIcon(app.id),
                            contentDescription = app.name,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = app.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (app.available) app.description else "Not installed on PC",
                        fontSize = 12.sp,
                        color = if (app.available) Color.Gray else MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onOpen,
                enabled = isConnected && app.available && !isLaunching,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                if (isLaunching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(if (app.available) "OPEN" else "N/A", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}
