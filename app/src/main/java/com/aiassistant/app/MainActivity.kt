package com.aiassistant.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnOverlay = findViewById<Button>(R.id.btnOverlay)
        val btnAccessibility = findViewById<Button>(R.id.btnAccessibility)
        val btnCapture = findViewById<Button>(R.id.btnCapture)
        val statusText = findViewById<TextView>(R.id.statusText)

        // زر تشغيل الزر العائم
        btnOverlay.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
                Toast.makeText(this, "امنح صلاحية العرض فوق التطبيقات", Toast.LENGTH_LONG).show()
            } else {
                val serviceIntent = Intent(this, FloatingService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }
                Toast.makeText(this, "تم تشغيل الزر العائم", Toast.LENGTH_SHORT).show()
            }
        }

        // زر تفعيل Accessibility
        btnAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        // زر منح إذن التقاط الشاشة
        btnCapture.setOnClickListener {
            ScreenCapture.requestPermission(this)
        }

        statusText.text = "1. Allow Overlay\n2. Enable Accessibility\n3. Grant Screen Capture\n4. Start Floating Button"
    }

    // استقبال نتيجة إذن MediaProjection
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 9001 && resultCode == Activity.RESULT_OK && data != null) {
            ScreenCapture.start(this, resultCode, data)
            Toast.makeText(this, "تم منح إذن التقاط الشاشة", Toast.LENGTH_SHORT).show()
        } else if (requestCode == 9001) {
            Toast.makeText(this, "تم رفض إذن التقاط الشاشة", Toast.LENGTH_SHORT).show()
        }
    }
}
