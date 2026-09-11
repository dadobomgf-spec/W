package com.aiassistant.app

import android.content.Context
import android.content.Intent
import android.net.Uri

object CommandHandler {

    private val APPS = mapOf(
        "chrome" to "com.android.chrome",
        "play store" to "com.android.vending",
        "youtube" to "com.google.android.youtube",
        "whatsapp" to "com.whatsapp",
        "telegram" to "org.telegram.messenger",
        "camera" to "com.sec.android.app.camera",
        "settings" to "com.android.settings",
        "gallery" to "com.sec.android.gallery3d",
        "zarchiver" to "ru.zdevs.zarchiver"
    )

    fun handle(text: String, context: Context): String? {
        val lower = text.lowercase()

        if (lower.startsWith("open ")) {
            val name = lower.removePrefix("open ").trim()
            for ((key, pkg) in APPS) {
                if (key in name) {
                    val intent = context.packageManager.getLaunchIntentForPackage(pkg)
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                        return "Opening $key..."
                    }
                }
            }
            val marketIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("market://search?q=$name")
            )
            marketIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(marketIntent)
            return "Searching Play Store for: $name"
        }

        if (lower.startsWith("search ")) {
            val query = text.removePrefix("search ").trim()
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/search?q=${query.replace(" ", "+")}")
            )
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return "Searching Google for: $query"
        }

        return null
    }
}

