package com.jvallapine.wolwaker

import android.content.Context
import android.net.wifi.WifiManager
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/**
 * Lógica compartida para construir y enviar el "magic packet" de
 * Wake-on-LAN. La usan tanto la Activity principal como el widget de
 * pantalla de inicio, para no duplicar código.
 */
object WolSender {

    class InvalidMacException(message: String) : Exception(message)

    /** Bloqueante: úsalo siempre desde un hilo de fondo, nunca desde el hilo principal. */
    @Throws(Exception::class)
    fun sendMagicPacketBlocking(context: Context, macText: String, broadcastText: String) {
        val macBytes = parseMac(macText)
        var wifiLock: WifiManager.MulticastLock? = null
        try {
            val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            wifiLock = wifi?.createMulticastLock("wolwaker")
            wifiLock?.setReferenceCounted(true)
            wifiLock?.acquire()

            val packet = buildMagicPacket(macBytes)
            val address = InetAddress.getByName(broadcastText.ifBlank { "255.255.255.255" })

            DatagramSocket().use { socket ->
                socket.broadcast = true
                // Se manda a los dos puertos estándar de WoL (9 y 7) por compatibilidad.
                for (port in intArrayOf(9, 7)) {
                    socket.send(DatagramPacket(packet, packet.size, address, port))
                }
            }
        } finally {
            try {
                if (wifiLock?.isHeld == true) wifiLock.release()
            } catch (_: Exception) {
                // ignorar
            }
        }
    }

    /** Convierte "AA:BB:CC:DD:EE:FF" (o con '-') en un ByteArray de 6 bytes. */
    fun parseMac(mac: String): ByteArray {
        val cleaned = mac.trim().replace("-", ":")
        val parts = cleaned.split(":")
        if (parts.size != 6) throw InvalidMacException("Formato de MAC inválido")
        return ByteArray(6) { i ->
            try {
                (Integer.parseInt(parts[i], 16) and 0xFF).toByte()
            } catch (e: NumberFormatException) {
                throw InvalidMacException("Formato de MAC inválido")
            }
        }
    }

    private fun buildMagicPacket(mac: ByteArray): ByteArray {
        val packet = ByteArray(6 + 16 * 6)
        for (i in 0 until 6) packet[i] = 0xFF.toByte()
        for (i in 0 until 16) {
            System.arraycopy(mac, 0, packet, 6 + i * 6, 6)
        }
        return packet
    }

    /** Calcula la IP de broadcast de la red WiFi actual a partir de la máscara DHCP. */
    fun computeBroadcastAddress(context: Context): String? {
        return try {
            val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val dhcp = wifi?.dhcpInfo ?: return null
            val ip = dhcp.ipAddress
            val mask = dhcp.netmask
            if (ip == 0 || mask == 0) return null
            val broadcast = (ip and mask) or mask.inv()
            intToIp(broadcast)
        } catch (e: Exception) {
            null
        }
    }

    private fun intToIp(value: Int): String {
        return listOf(
            value and 0xFF,
            (value shr 8) and 0xFF,
            (value shr 16) and 0xFF,
            (value shr 24) and 0xFF
        ).joinToString(".")
    }

    fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences("wol_prefs", Context.MODE_PRIVATE)

    const val KEY_NAME = "name"
    const val KEY_MAC = "mac"
    const val KEY_BROADCAST = "broadcast"
}
