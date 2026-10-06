package com.example.remote.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap

data class DiscoveredPc(
    val name: String,
    val ipAddress: String,
    val port: Int = 5000,
    val serviceType: String = "_remote._tcp",
    val isOnline: Boolean = true
)

sealed class DiscoveryState {
    object Idle : DiscoveryState()
    object Searching : DiscoveryState()
    data class Success(val pcs: List<DiscoveredPc>) : DiscoveryState()
    data class Empty(val message: String = "No Nexa Remote PCs found on this Wi-Fi. Make sure your PC and phone are connected to the same Wi-Fi network.") : DiscoveryState()
    data class Error(val message: String = "PC discovery unavailable.") : DiscoveryState()
}

class NsdDiscoveryManager(private val context: Context) {

    private val TAG = "NsdDiscoveryManager"
    private val SERVICE_TYPE = "_remote._tcp."

    private val nsdManager: NsdManager? =
        context.applicationContext.getSystemService(Context.NSD_SERVICE) as? NsdManager

    private val wifiManager: WifiManager? =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    private var multicastLock: WifiManager.MulticastLock? = null

    private val _discoveryState = MutableStateFlow<DiscoveryState>(DiscoveryState.Idle)
    val discoveryState: StateFlow<DiscoveryState> = _discoveryState.asStateFlow()

    private val discoveredPcsMap = ConcurrentHashMap<String, DiscoveredPc>()

    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var resolveChannel: Channel<NsdServiceInfo>? = null
    private var resolveJob: Job? = null
    private var timeoutJob: Job? = null
    private var fallbackJob: Job? = null
    private var isDiscovering = false

    private val scope = CoroutineScope(Dispatchers.IO)

    @Synchronized
    fun startDiscovery() {
        if (isDiscovering) {
            Log.d(TAG, "Discovery already running, restarting...")
            stopDiscovery()
        }

        if (nsdManager == null) {
            _discoveryState.value = DiscoveryState.Error("NSD Service not available on this device.")
            return
        }

        discoveredPcsMap.clear()
        _discoveryState.value = DiscoveryState.Searching
        isDiscovering = true

        // Acquire Wi-Fi Multicast Lock
        try {
            if (multicastLock == null) {
                multicastLock = wifiManager?.createMulticastLock("RemoteNsdMulticastLock")
                multicastLock?.setReferenceCounted(false)
            }
            multicastLock?.acquire()
            Log.d(TAG, "Multicast lock acquired")
        } catch (e: Exception) {
            Log.w(TAG, "Could not acquire multicast lock: ${e.message}")
        }

        // Initialize channel and sequential resolver job
        resolveChannel = Channel(Channel.UNLIMITED)
        startResolverJob()

        // Primary mDNS run: If no PCs found after 3s, trigger local-subnet unicast probe fallback
        timeoutJob = scope.launch {
            delay(3000)
            if (isDiscovering && discoveredPcsMap.isEmpty()) {
                Log.d(TAG, "mDNS yielded 0 results after 3s. Launching local-subnet probe fallback...")
                startFallbackSubnetProbe()
            }
        }

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Log.d(TAG, "Service discovery started for: $regType")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                Log.d(TAG, "Service found: ${service.serviceName}, type: ${service.serviceType}")
                if (service.serviceType.contains("_remote")) {
                    resolveChannel?.trySend(service)
                }
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                Log.d(TAG, "Service lost: ${service.serviceName}")
                val cleanName = cleanServiceName(service.serviceName)
                discoveredPcsMap.remove(cleanName)
                updateStateWithDiscoveredPcs()
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.d(TAG, "Discovery stopped: $serviceType")
                isDiscovering = false
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Discovery start failed: errorCode = $errorCode")
                isDiscovering = false
                _discoveryState.value = DiscoveryState.Error("PC discovery unavailable (Error $errorCode).")
                stopDiscovery()
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Discovery stop failed: errorCode = $errorCode")
                isDiscovering = false
            }
        }

        try {
            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start service discovery: ${e.message}", e)
            isDiscovering = false
            _discoveryState.value = DiscoveryState.Error("Failed to start discovery: ${e.message}")
        }
    }

    private fun startResolverJob() {
        resolveJob = scope.launch {
            val channel = resolveChannel ?: return@launch
            for (serviceInfo in channel) {
                if (!isDiscovering) break
                resolveServiceSequentially(serviceInfo)
            }
        }
    }

    private suspend fun resolveServiceSequentially(serviceInfo: NsdServiceInfo) {
        withContext(Dispatchers.IO) {
            val resChannel = Channel<DiscoveredPc?>(1)

            val resolveListener = object : NsdManager.ResolveListener {
                override fun onResolveFailed(service: NsdServiceInfo, errorCode: Int) {
                    Log.e(TAG, "Resolve failed for ${service.serviceName}: $errorCode")
                    resChannel.trySend(null)
                }

                override fun onServiceResolved(service: NsdServiceInfo) {
                    Log.d(TAG, "Service resolved: ${service.serviceName}, host: ${service.host}, port: ${service.port}")
                    val host = service.host
                    val hostAddress = host?.hostAddress

                    if (hostAddress != null && !hostAddress.contains(":")) { // Ensure IPv4
                        val displayName = cleanServiceName(service.serviceName)
                        val port = if (service.port > 0) service.port else 5000
                        val pc = DiscoveredPc(
                            name = displayName,
                            ipAddress = hostAddress,
                            port = port
                        )
                        resChannel.trySend(pc)
                    } else {
                        resChannel.trySend(null)
                    }
                }
            }

            try {
                nsdManager?.resolveService(serviceInfo, resolveListener)
                val resolvedPc = withTimeoutOrNull(3000) {
                    resChannel.receive()
                }

                if (resolvedPc != null) {
                    discoveredPcsMap[resolvedPc.name] = resolvedPc
                    updateStateWithDiscoveredPcs()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error resolving service ${serviceInfo.serviceName}: ${e.message}")
            }
        }
    }

    private fun startFallbackSubnetProbe() {
        fallbackJob = scope.launch(Dispatchers.IO) {
            val candidateIps = getLocalSubnetIps()
            if (candidateIps == null || candidateIps.isEmpty()) {
                Log.d(TAG, "No valid private IPv4 subnet found for fallback probe.")
                if (discoveredPcsMap.isEmpty() && isDiscovering) {
                    _discoveryState.value = DiscoveryState.Empty("No Nexa Remote PCs found on this Wi-Fi. Make sure your PC and phone are connected to the same Wi-Fi network.")
                }
                return@launch
            }

            Log.d(TAG, "Starting fallback subnet probe for ${candidateIps.size} local IPs...")
            val semaphore = Semaphore(20) // Bounded concurrency (max 20 parallel probes)
            val jobs = candidateIps.map { ip ->
                launch {
                    if (!isDiscovering) return@launch
                    semaphore.acquire()
                    try {
                        val discovered = probeIpForNexaServer(ip)
                        if (discovered != null && isDiscovering) {
                            Log.d(TAG, "Fallback probe verified Nexa Remote PC at ${discovered.ipAddress} (${discovered.name})")
                            discoveredPcsMap[discovered.name] = discovered
                            updateStateWithDiscoveredPcs()
                        }
                    } finally {
                        semaphore.release()
                    }
                }
            }
            jobs.joinAll()
            Log.d(TAG, "Fallback subnet probe completed. Total discovered: ${discoveredPcsMap.size}")
            if (discoveredPcsMap.isEmpty() && isDiscovering) {
                _discoveryState.value = DiscoveryState.Empty("No Nexa Remote PCs found on this Wi-Fi. Make sure your PC and phone are connected to the same Wi-Fi network.")
            }
        }
    }

    private suspend fun probeIpForNexaServer(ip: String): DiscoveredPc? {
        return withContext(Dispatchers.IO) {
            var socket: Socket? = null
            try {
                socket = Socket()
                socket.soTimeout = 300
                socket.connect(InetSocketAddress(ip, 5000), 250)

                val inputStream = socket.getInputStream()
                val outputStream = socket.getOutputStream()

                // Read initial server banner/greeting
                val buffer = ByteArray(256)
                val readInitial = inputStream.read(buffer)
                val initialBanner = if (readInitial > 0) String(buffer, 0, readInitial) else ""

                // Perform lightweight probe handshake using PAIR_REQUEST
                val probeMsg = "{\"type\":\"PAIR_REQUEST\",\"device_id\":\"probe\",\"device_name\":\"NexaScanner\"}\n"
                outputStream.write(probeMsg.toByteArray(Charsets.UTF_8))
                outputStream.flush()

                val respBuffer = ByteArray(512)
                val respRead = inputStream.read(respBuffer)
                val resp = if (respRead > 0) String(respBuffer, 0, respRead) else ""

                val combined = "$initialBanner $resp"
                val isNexaServer = combined.contains("PAIR_CODE_REQUIRED") ||
                        combined.contains("ALREADY_PAIRED") ||
                        combined.contains("AUTH_") ||
                        combined.contains("pc_name") ||
                        combined.contains("type")

                if (isNexaServer) {
                    var pcName = "PC ($ip)"
                    try {
                        if (combined.contains("\"pc_name\":")) {
                            val idx = combined.indexOf("\"pc_name\":")
                            val sub = combined.substring(idx + 10).trim().removePrefix("\"")
                            val nameEnd = sub.indexOf("\"")
                            if (nameEnd > 0) {
                                pcName = cleanServiceName(sub.substring(0, nameEnd))
                            }
                        }
                    } catch (_: Exception) {}

                    DiscoveredPc(
                        name = pcName,
                        ipAddress = ip,
                        port = 5000
                    )
                } else {
                    null
                }
            } catch (_: Exception) {
                null
            } finally {
                try { socket?.close() } catch (_: Exception) {}
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun getLocalSubnetIps(): List<String>? {
        try {
            // Method 1: DhcpInfo via WifiManager
            val dhcp = wifiManager?.dhcpInfo
            if (dhcp != null && dhcp.ipAddress != 0) {
                val ip = dhcp.ipAddress
                val netmask = if (dhcp.netmask != 0) dhcp.netmask else 0x00FFFFFF

                val ipBytes = byteArrayOf(
                    (ip and 0xFF).toByte(),
                    ((ip shr 8) and 0xFF).toByte(),
                    ((ip shr 16) and 0xFF).toByte(),
                    ((ip shr 24) and 0xFF).toByte()
                )
                val inetAddr = InetAddress.getByAddress(ipBytes)
                if (isPrivateIpv4(inetAddr)) {
                    val maskBytes = byteArrayOf(
                        (netmask and 0xFF).toByte(),
                        ((netmask shr 8) and 0xFF).toByte(),
                        ((netmask shr 16) and 0xFF).toByte(),
                        ((netmask shr 24) and 0xFF).toByte()
                    )

                    val b0 = (ipBytes[0].toInt() and 0xFF) and (maskBytes[0].toInt() and 0xFF)
                    val b1 = (ipBytes[1].toInt() and 0xFF) and (maskBytes[1].toInt() and 0xFF)
                    val b2 = (ipBytes[2].toInt() and 0xFF) and (maskBytes[2].toInt() and 0xFF)
                    val myB3 = (ipBytes[3].toInt() and 0xFF)

                    val list = mutableListOf<String>()
                    for (i in 1..254) {
                        if (i != myB3) {
                            list.add("$b0.$b1.$b2.$i")
                        }
                    }
                    if (list.isNotEmpty()) return list
                }
            }

            // Method 2: NetworkInterface enumeration fallback
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (!iface.isUp || iface.isLoopback) continue
                val name = iface.name.lowercase()
                if (!name.contains("wlan") && !name.contains("ap") && !name.contains("eth") && !name.contains("en")) continue

                val addrs = iface.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (addr is Inet4Address && isPrivateIpv4(addr)) {
                        val ipBytes = addr.address
                        val b0 = ipBytes[0].toInt() and 0xFF
                        val b1 = ipBytes[1].toInt() and 0xFF
                        val b2 = ipBytes[2].toInt() and 0xFF
                        val b3 = ipBytes[3].toInt() and 0xFF

                        val list = mutableListOf<String>()
                        for (i in 1..254) {
                            if (i != b3) {
                                list.add("$b0.$b1.$b2.$i")
                            }
                        }
                        if (list.isNotEmpty()) return list
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating local subnet: ${e.message}")
        }
        return null
    }

    private fun isPrivateIpv4(address: InetAddress): Boolean {
        if (address.isLoopbackAddress || address.isAnyLocalAddress || address.isLinkLocalAddress) return false
        val bytes = address.address
        if (bytes.size != 4) return false

        val b0 = bytes[0].toInt() and 0xFF
        val b1 = bytes[1].toInt() and 0xFF

        // 10.0.0.0/8
        if (b0 == 10) return true
        // 172.16.0.0/12
        if (b0 == 172 && (b1 in 16..31)) return true
        // 192.168.0.0/16
        if (b0 == 192 && b1 == 168) return true

        return false
    }

    private fun updateStateWithDiscoveredPcs() {
        if (!isDiscovering && _discoveryState.value !is DiscoveryState.Searching) return

        val pcs = discoveredPcsMap.values.toList()
        if (pcs.isNotEmpty()) {
            _discoveryState.value = DiscoveryState.Success(pcs)
        } else if (_discoveryState.value !is DiscoveryState.Searching) {
            _discoveryState.value = DiscoveryState.Empty("No Nexa Remote PCs found on this Wi-Fi. Make sure your PC and phone are connected to the same Wi-Fi network.")
        }
    }

    private fun cleanServiceName(serviceName: String): String {
        return serviceName
            .removePrefix("NexaRemote-")
            .removePrefix("Remote-")
            .removeSuffix("._remote._tcp.local.")
            .removeSuffix("._remote._tcp.local")
            .removeSuffix(".local")
            .ifEmpty { serviceName }
    }

    @Synchronized
    fun stopDiscovery() {
        isDiscovering = false
        timeoutJob?.cancel()
        timeoutJob = null
        fallbackJob?.cancel()
        fallbackJob = null
        resolveJob?.cancel()
        resolveJob = null
        resolveChannel?.close()
        resolveChannel = null

        discoveryListener?.let { listener ->
            try {
                nsdManager?.stopServiceDiscovery(listener)
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping service discovery: ${e.message}")
            }
        }
        discoveryListener = null

        try {
            if (multicastLock?.isHeld == true) {
                multicastLock?.release()
                Log.d(TAG, "Multicast lock released")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing multicast lock: ${e.message}")
        }
    }
}
