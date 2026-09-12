package com.aiassistant.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FloatingService : Service() {

    private lateinit var windowManager: WindowManager
    private var floatingBtn: Button? = null
    private var chatView: LinearLayout? = null
    private var response: TextView? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startAsForeground()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createFloatingButton()
    }

    private fun startAsForeground() {
        val channelId = "ai_assistant_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "AI Assistant",
                NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(channel)
        }
        val notif = NotificationCompat.Builder(this, channelId)
            .setContentTitle("AI Assistant")
            .setContentText("Floating button active")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        startForeground(1, notif)
    }

    private fun createFloatingButton() {
        val btn = Button(this).apply {
            text = "+"
            textSize = 24f
            setBackgroundColor(0xFFFFA500.toInt())
            setTextColor(0xFF000000.toInt())
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 30
            y = 300
        }
        btn.setOnClickListener { showChatWindow() }
        windowManager.addView(btn, params)
        floatingBtn = btn
    }

    private fun showChatWindow() {
        if (chatView != null) return

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xE6000000.toInt())
            setPadding(30, 30, 30, 30)
        }

        val title = TextView(this).apply {
            text = "AI Assistant (Gemini Vision)"
            setTextColor(0xFFFFA500.toInt())
            textSize = 18f
        }

        val input = EditText(this).apply {
            hint = "اكتب أمرك..."
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0xFF888888.toInt())
        }

        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val sendBtn = Button(this).apply {
            text = "Send"
            setBackgroundColor(0xFFFFA500.toInt())
            setTextColor(0xFF000000.toInt())
        }

        val lookBtn = Button(this).apply {
            text = "Look"
            setBackgroundColor(0xFF2196F3.toInt())
            setTextColor(0xFFFFFFFF.toInt())
        }

        val closeBtn = Button(this).apply {
            text = "X"
            setBackgroundColor(0xFFFF0000.toInt())
            setTextColor(0xFFFFFFFF.toInt())
        }

        val resp = TextView(this).apply {
            text = ""
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 14f
            setPadding(0, 20, 0, 20)
        }

        val scroll = ScrollView(this).apply {
            addView(resp)
        }
        response = resp

        sendBtn.setOnClickListener {
            val userText = input.text.toString().trim()
            if (userText.isEmpty()) return@setOnClickListener
            input.setText("")
            resp.text = "Thinking..."

            CoroutineScope(Dispatchers.IO).launch {
                val action = CommandHandler.handle(userText, this@FloatingService)
                val result = action ?: AIClient.ask(userText)
                withContext(Dispatchers.Main) { resp.text = result }
            }
        }

        lookBtn.setOnClickListener {
            val userText = input.text.toString().trim()
            if (userText.isEmpty()) {
                resp.text = "اكتب سؤالك أولاً"
                return@setOnClickListener
            }
            input.setText("")
            resp.text = "جاري التقاط الشاشة..."

            val bmp = ScreenCapture.capture()
            if (bmp == null) {
                resp.text = "فشل التقاط الشاشة. امنح الإذن."
                return@setOnClickListener
            }

            resp.text = "جاري التحليل..."

            val prompt = buildPrompt(userText)

            CoroutineScope(Dispatchers.IO).launch {
                val result = AIClient.askWithImage(bmp, prompt)
                withContext(Dispatchers.Main) {
                    resp.text = result
                    executeIfAction(result)
                }
            }
        }

        closeBtn.setOnClickListener {
            try { chatView?.let { windowManager.removeView(it) } } catch (_: Exception) {}
            chatView = null
        }

        btnRow.addView(sendBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        btnRow.addView(lookBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        btnRow.addView(closeBtn, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        layout.addView(title)
        layout.addView(input)
        layout.addView(btnRow)
        layout.addView(scroll)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP
            y = 100
        }

        windowManager.addView(layout, params)
        chatView = layout
    }

    private fun buildPrompt(userCommand: String): String {
        return "أنت مساعد ذكي على هاتف أندرويد. أمامك لقطة شاشة.\n\n" +
               "أمر المستخدم: \"" + userCommand + "\"\n\n" +
               "أجب بأحد الأشكال التالية فقط:\n" +
               "ACTION:CLICK:x:y\n" +
               "ACTION:SCROLL:fromX:fromY:toX:toY\n" +
               "ACTION:BACK\n" +
               "ACTION:HOME\n" +
               "أو أجب بالعربية وصفاً موجزاً لما تراه."
    }

    private fun executeIfAction(text: String) {
        try {
            if (text.startsWith("ACTION:CLICK:")) {
                val parts = text.trim().split(":")
                val x = parts[2].toFloatOrNull() ?: return
                val y = parts[3].toFloatOrNull() ?: return
                AccessibilityHelper.instance?.clickAt(x, y)
            } else if (text.startsWith("ACTION:SCROLL:")) {
                val parts = text.trim().split(":")
                val fx = parts[2].toFloatOrNull() ?: return
                val fy = parts[3].toFloatOrNull() ?: return
                val tx = parts[4].toFloatOrNull() ?: return
                val ty = parts[5].toFloatOrNull() ?: return
                AccessibilityHelper.instance?.swipe(fx, fy, tx, ty)
            } else if (text.trim() == "ACTION:BACK") {
                AccessibilityHelper.instance?.pressBack()
            } else if (text.trim() == "ACTION:HOME") {
                AccessibilityHelper.instance?.pressHome()
            }
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        try { floatingBtn?.let { windowManager.removeView(it) } } catch (_: Exception) {}
        try { chatView?.let { windowManager.removeView(it) } } catch (_: Exception) {}
        floatingBtn = null
        chatView = null
        response = null
    }
}
