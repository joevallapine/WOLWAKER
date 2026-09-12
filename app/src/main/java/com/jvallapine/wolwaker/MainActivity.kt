package com.jvallapine.wolwaker

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * WolWaker - manda un "magic packet" de Wake-on-LAN por la red WiFi local
 * para encender una PC que esté en hibernación / apagada / suspendida,
 * siempre que la PC tenga Wake-on-LAN habilitado en la BIOS y en el
 * adaptador de red.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var editName: EditText
    private lateinit var editMac: EditText
    private lateinit var editBroadcast: EditText
    private lateinit var textStatus: TextView

    private val prefs by lazy { WolSender.prefs(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        editName = findViewById(R.id.editName)
        editMac = findViewById(R.id.editMac)
        editBroadcast = findViewById(R.id.editBroadcast)
        textStatus = findViewById(R.id.textStatus)
        val btnSave: Button = findViewById(R.id.btnSave)
        val btnWake: Button = findViewById(R.id.btnWake)

        // Cargar datos guardados, o autocompletar broadcast según el WiFi actual.
        editName.setText(prefs.getString(WolSender.KEY_NAME, ""))
        editMac.setText(prefs.getString(WolSender.KEY_MAC, ""))
        editBroadcast.setText(
            prefs.getString(WolSender.KEY_BROADCAST, null)
                ?: WolSender.computeBroadcastAddress(this)
                ?: "255.255.255.255"
        )

        btnSave.setOnClickListener {
            saveFields()
            status("Datos guardados ✓")
        }

        btnWake.setOnClickListener {
            saveFields()
            sendMagicPacket()
        }
    }

    private fun saveFields() {
        prefs.edit()
            .putString(WolSender.KEY_NAME, editName.text.toString().trim())
            .putString(WolSender.KEY_MAC, editMac.text.toString().trim())
            .putString(WolSender.KEY_BROADCAST, editBroadcast.text.toString().trim())
            .apply()

        // Si hay widgets en la pantalla de inicio, que también vean los datos nuevos.
        WolWidgetProvider.requestUpdateAll(this)
    }

    private fun sendMagicPacket() {
        val macText = editMac.text.toString().trim()
        val broadcastText = editBroadcast.text.toString().trim().ifEmpty { "255.255.255.255" }

        status("Enviando…")

        Thread {
            try {
                WolSender.sendMagicPacketBlocking(applicationContext, macText, broadcastText)
                runOnUiThread { status("Paquete enviado ✓ (espera ~15-30 s a que encienda)") }
            } catch (e: WolSender.InvalidMacException) {
                runOnUiThread { status("MAC inválida. Usa el formato AA:BB:CC:DD:EE:FF") }
            } catch (e: Exception) {
                runOnUiThread { status("Error al enviar: ${e.message}") }
            }
        }.start()
    }

    private fun status(msg: String) {
        textStatus.text = msg
    }
}
