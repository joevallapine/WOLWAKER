package com.jvallapine.wolwaker

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.RemoteViews

/**
 * Widget de pantalla de inicio: un botón que manda el magic packet
 * directamente, sin tener que abrir la app. Usa la MAC / IP de
 * broadcast guardadas desde MainActivity (SharedPreferences "wol_prefs").
 */
class WolWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            updateWidget(context, appWidgetManager, id, defaultLabel(context))
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACTION_WAKE) return

        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        val appWidgetManager = AppWidgetManager.getInstance(context)

        val prefs = WolSender.prefs(context)
        val mac = prefs.getString(WolSender.KEY_MAC, "") ?: ""
        val broadcast = prefs.getString(WolSender.KEY_BROADCAST, null) ?: "255.255.255.255"

        if (mac.isBlank()) {
            updateWidget(context, appWidgetManager, appWidgetId, "Abre la app y configura la MAC")
            resetLater(context, appWidgetManager, appWidgetId)
            return
        }

        updateWidget(context, appWidgetManager, appWidgetId, "Enviando…")

        val pendingResult = goAsync()
        Thread {
            val msg: String = try {
                WolSender.sendMagicPacketBlocking(context, mac, broadcast)
                "Enviado ✓"
            } catch (e: Exception) {
                "Error: ${e.message}"
            }
            Handler(Looper.getMainLooper()).post {
                updateWidget(context, appWidgetManager, appWidgetId, msg)
                resetLater(context, appWidgetManager, appWidgetId)
                pendingResult.finish()
            }
        }.start()
    }

    private fun resetLater(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        Handler(Looper.getMainLooper()).postDelayed({
            updateWidget(context, appWidgetManager, appWidgetId, defaultLabel(context))
        }, 4000)
    }

    private fun defaultLabel(context: Context) = context.getString(R.string.widget_button_text)

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        label: String
    ) {
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
        val views = RemoteViews(context.packageName, R.layout.widget_wol)
        views.setTextViewText(R.id.widgetButton, label)

        val clickIntent = Intent(context, WolWidgetProvider::class.java).apply {
            action = ACTION_WAKE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getBroadcast(context, appWidgetId, clickIntent, flags)
        views.setOnClickPendingIntent(R.id.widgetButton, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    companion object {
        const val ACTION_WAKE = "com.jvallapine.wolwaker.ACTION_WAKE_WIDGET"

        /** Llamar cuando se guardan datos nuevos en la Activity, para refrescar los widgets. */
        fun requestUpdateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, WolWidgetProvider::class.java))
            if (ids.isNotEmpty()) {
                val intent = Intent(context, WolWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
                context.sendBroadcast(intent)
            }
        }
    }
}
