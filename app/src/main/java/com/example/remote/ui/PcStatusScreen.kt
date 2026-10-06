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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Locale

data class MemoryStatus(
    val total: Long? = null,
    val used: Long? = null,
    val percent: Float? = null
)

data class DiskStatus(
    val path: String? = null,
    val total: Long? = null,
    val used: Long? = null,
    val free: Long? = null,
    val percent: Float? = null
)

data class PowerStatus(
    val has_battery: Boolean = false,
    val percent: Int? = null,
    val charging: Boolean? = null,
    val plugged: Boolean? = null
)

data class NetworkStatus(
    val connected: Boolean = true,
    val interfaceName: String? = null,
    val ssid: String? = null
)

data class SystemStatusResponse(
    val success: Boolean = false,
    val pc_name: String? = null,
    val ip_address: String? = null,
    val cpu_percent: Float? = null,
    val memory: MemoryStatus? = null,
    val disk: DiskStatus? = null,
    val uptime_seconds: Long? = null,
    val power: PowerStatus? = null,
    val network: NetworkStatus? = null,
    val error: String? = null
)

fun parseSystemStatus(jsonStr: String): SystemStatusResponse {
    return try {
        val obj = JSONObject(jsonStr)
        val success = obj.optBoolean("success", false)
        val pcName = if (obj.has("pc_name") && !obj.isNull("pc_name")) obj.optString("pc_name") else null
        val ipAddress = if (obj.has("ip_address") && !obj.isNull("ip_address")) obj.optString("ip_address") else null
        val cpuPercent = if (obj.has("cpu_percent") && !obj.isNull("cpu_percent")) obj.optDouble("cpu_percent").toFloat() else null

        val memObj = obj.optJSONObject("memory")
        val memory = if (memObj != null) {
            MemoryStatus(
                total = if (memObj.has("total") && !memObj.isNull("total")) memObj.optLong("total") else null,
                used = if (memObj.has("used") && !memObj.isNull("used")) memObj.optLong("used") else null,
                percent = if (memObj.has("percent") && !memObj.isNull("percent")) memObj.optDouble("percent").toFloat() else null
            )
        } else null

        val diskObj = obj.optJSONObject("disk")
        val disk = if (diskObj != null) {
            DiskStatus(
                path = if (diskObj.has("path") && !diskObj.isNull("path")) diskObj.optString("path") else null,
                total = if (diskObj.has("total") && !diskObj.isNull("total")) diskObj.optLong("total") else null,
                used = if (diskObj.has("used") && !diskObj.isNull("used")) diskObj.optLong("used") else null,
                free = if (diskObj.has("free") && !diskObj.isNull("free")) diskObj.optLong("free") else null,
                percent = if (diskObj.has("percent") && !diskObj.isNull("percent")) diskObj.optDouble("percent").toFloat() else null
            )
        } else null

        val uptimeSec = if (obj.has("uptime_seconds") && !obj.isNull("uptime_seconds")) obj.optLong("uptime_seconds") else null

        val powObj = obj.optJSONObject("power")
        val power = if (powObj != null) {
            PowerStatus(
                has_battery = if (powObj.has("has_battery") && !powObj.isNull("has_battery")) powObj.optBoolean("has_battery") else false,
                percent = if (powObj.has("percent") && !powObj.isNull("percent")) powObj.optInt("percent") else null,
                charging = if (powObj.has("charging") && !powObj.isNull("charging")) powObj.optBoolean("charging") else null,
                plugged = if (powObj.has("plugged") && !powObj.isNull("plugged")) powObj.optBoolean("plugged") else null
            )
        } else null

        val netObj = obj.optJSONObject("network")
        val network = if (netObj != null) {
            NetworkStatus(
                connected = if (netObj.has("connected") && !netObj.isNull("connected")) netObj.optBoolean("connected") else true,
                interfaceName = if (netObj.has("interface") && !netObj.isNull("interface")) netObj.optString("interface") else null,
                ssid = if (netObj.has("ssid") && !netObj.isNull("ssid")) netObj.optString("ssid") else null
            )
        } else null

        SystemStatusResponse(
            success = success,
            pc_name = pcName,
            ip_address = ipAddress,
            cpu_percent = cpuPercent,
            memory = memory,
            disk = disk,
            uptime_seconds = uptimeSec,
            power = power,
            network = network,
            error = if (obj.has("error") && !obj.isNull("error")) obj.optString("error") else null
        )
    } catch (e: Exception) {
        SystemStatusResponse(success = false, error = e.message ?: "Invalid status JSON")
    }
}

fun formatBytes(bytes: Long?): String {
    if (bytes == null || bytes < 0) return "Unavailable"
    val gb = 1024.0 * 1024.0 * 1024.0
    val mb = 1024.0 * 1024.0
    return if (bytes >= gb) {
        String.format(Locale.US, "%.1f GB", bytes / gb)
    } else if (bytes >= mb) {
        String.format(Locale.US, "%.1f MB", bytes / mb)
    } else {
        "$bytes B"
    }
}

fun formatUptime(seconds: Long?): String {
    if (seconds == null || seconds < 0) return "Unavailable"
    val days = seconds / 86400
    val hours = (seconds % 86400) / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60

    val parts = mutableListOf<String>()
    if (days > 0) parts.add("$days day${if (days > 1) "s" else ""}")
    if (hours > 0) parts.add("$hours hour${if (hours > 1) "s" else ""}")
    if (minutes > 0) parts.add("$minutes min${if (minutes > 1) "s" else ""}")
    if (parts.isEmpty()) {
        parts.add("$secs sec${if (secs > 1) "s" else ""}")
    }

    return parts.joinToString(" ")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PcStatusScreen(
    connectionState: ConnectionState,
    connectedPcName: String,
    currentIpAddress: String,
    onBack: () -> Unit,
    onFetchStatus: suspend () -> String?,
    onReconnect: (() -> Unit)? = null
) {
    var statusData by remember { mutableStateOf<SystemStatusResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    suspend fun doFetch() {
        val json = onFetchStatus()
        if (json != null) {
            val parsed = parseSystemStatus(json)
            if (parsed.success) {
                statusData = parsed
                errorMessage = null
            } else {
                errorMessage = parsed.error ?: "Failed to retrieve status"
            }
        } else {
            errorMessage = "PC Status unavailable"
        }
        isLoading = false
        isRefreshing = false
    }

    // Auto-refresh loop every ~3 seconds when screen is active
    LaunchedEffect(connectionState) {
        if (connectionState == ConnectionState.CONNECTED) {
            while (isActive) {
                doFetch()
                delay(3000)
            }
        } else {
            isLoading = false
            isRefreshing = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PC Status Dashboard", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            isRefreshing = true
                            scope.launch { doFetch() }
                        },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // --- 1. PC NAME & CONNECTION CARD ---
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🖥️ ", fontSize = 22.sp)
                            Column {
                                val pcNameDisplay = statusData?.pc_name ?: connectedPcName.ifEmpty { "PC Host" }
                                Text(
                                    text = pcNameDisplay,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "System Information",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = when (connectionState) {
                                ConnectionState.CONNECTED -> Color(0xFFE8F5E9)
                                ConnectionState.CONNECTING, ConnectionState.RECONNECTING, ConnectionState.AUTHENTICATING, ConnectionState.DISCOVERING -> Color(0xFFFFF8E1)
                                else -> Color(0xFFFFEBEE)
                            }
                        ) {
                            val badgeText = when (connectionState) {
                                ConnectionState.CONNECTED -> "● Connected"
                                ConnectionState.CONNECTING, ConnectionState.RECONNECTING, ConnectionState.AUTHENTICATING, ConnectionState.DISCOVERING -> "● Connecting..."
                                else -> "● Disconnected"
                            }
                            val badgeColor = when (connectionState) {
                                ConnectionState.CONNECTED -> Color(0xFF2E7D32)
                                ConnectionState.CONNECTING, ConnectionState.RECONNECTING, ConnectionState.AUTHENTICATING, ConnectionState.DISCOVERING -> Color(0xFFF57F17)
                                else -> Color(0xFFC62828)
                            }
                            Text(
                                text = badgeText,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontSize = 12.sp,
                                color = badgeColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

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
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Connect or pair to your PC to view live status metrics.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                        )
                        if (onReconnect != null) {
                            Spacer(modifier = Modifier.height(12.dp))
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
            } else if (isLoading) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Loading PC status...", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            // --- 2. NETWORK CARD ---
            val activeIp = statusData?.ip_address ?: currentIpAddress
            val activeIface = statusData?.network?.interfaceName ?: "LAN Interface"
            val activeSsid = statusData?.network?.ssid

            StatusCard(
                title = "Network",
                icon = Icons.Default.Wifi,
                content = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("IPv4 Address:", fontSize = 14.sp, color = Color.Gray)
                            Text(activeIp.ifBlank { "Unavailable" }, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Interface:", fontSize = 14.sp, color = Color.Gray)
                            Text(activeIface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Wi-Fi / Network:", fontSize = 14.sp, color = Color.Gray)
                            Text(
                                text = if (activeSsid != null) "Connected ($activeSsid)" else "Connected",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            )

            // --- 3. CPU CARD ---
            val cpuPercent = statusData?.cpu_percent
            StatusCard(
                title = "CPU Usage",
                icon = Icons.Default.Speed,
                content = {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Usage", fontSize = 14.sp, color = Color.Gray)
                            Text(
                                text = if (cpuPercent != null) "${cpuPercent.toInt()}%" else "Unavailable",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        val progress = if (cpuPercent != null) (cpuPercent / 100f).coerceIn(0f, 1f) else 0f
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = when {
                                progress > 0.85f -> MaterialTheme.colorScheme.error
                                progress > 0.60f -> Color(0xFFF57F17)
                                else -> MaterialTheme.colorScheme.primary
                            },
                            trackColor = MaterialTheme.colorScheme.surface
                        )
                    }
                }
            )

            // --- 4. RAM CARD ---
            val memory = statusData?.memory
            StatusCard(
                title = "RAM Usage",
                icon = Icons.Default.Memory,
                content = {
                    Column {
                        val usedStr = formatBytes(memory?.used)
                        val totalStr = formatBytes(memory?.total)
                        val memPercent = memory?.percent

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("$usedStr / $totalStr", fontSize = 14.sp, color = Color.Gray)
                            Text(
                                text = if (memPercent != null) "${memPercent.toInt()}%" else "Unavailable",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        val progress = if (memPercent != null) (memPercent / 100f).coerceIn(0f, 1f) else 0f
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = when {
                                progress > 0.85f -> MaterialTheme.colorScheme.error
                                progress > 0.60f -> Color(0xFFF57F17)
                                else -> MaterialTheme.colorScheme.primary
                            },
                            trackColor = MaterialTheme.colorScheme.surface
                        )
                    }
                }
            )

            // --- 5. DISK CARD ---
            val disk = statusData?.disk
            StatusCard(
                title = "Disk (${disk?.path ?: "C:"})",
                icon = Icons.Default.Storage,
                content = {
                    Column {
                        val usedStr = formatBytes(disk?.used)
                        val totalStr = formatBytes(disk?.total)
                        val freeStr = formatBytes(disk?.free)
                        val diskPercent = disk?.percent

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${disk?.path ?: "C:"} $usedStr / $totalStr", fontSize = 14.sp, color = Color.Gray)
                            Text(
                                text = if (diskPercent != null) "${diskPercent.toInt()}%" else "Unavailable",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Free: $freeStr", fontSize = 12.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        val progress = if (diskPercent != null) (diskPercent / 100f).coerceIn(0f, 1f) else 0f
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = when {
                                progress > 0.90f -> MaterialTheme.colorScheme.error
                                progress > 0.75f -> Color(0xFFF57F17)
                                else -> MaterialTheme.colorScheme.primary
                            },
                            trackColor = MaterialTheme.colorScheme.surface
                        )
                    }
                }
            )

            // --- 6. UPTIME CARD ---
            val uptimeSeconds = statusData?.uptime_seconds
            StatusCard(
                title = "Uptime",
                icon = Icons.Default.Schedule,
                content = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("System running time:", fontSize = 14.sp, color = Color.Gray)
                        Text(
                            text = formatUptime(uptimeSeconds),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )

            // --- 7. POWER / BATTERY CARD ---
            val power = statusData?.power
            StatusCard(
                title = "Power & Battery",
                icon = Icons.Default.Power,
                content = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Power State:", fontSize = 14.sp, color = Color.Gray)

                        val powerText = if (power != null && power.has_battery && power.percent != null) {
                            val state = if (power.charging == true) "Charging ⚡" else "Discharging"
                            "${power.percent}% ($state)"
                        } else {
                            "AC Power / No Battery"
                        }

                        Text(
                            text = powerText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )

            if (errorMessage != null && connectionState == ConnectionState.CONNECTED && !isLoading) {
                Text(
                    text = errorMessage ?: "",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun StatusCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
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
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}
