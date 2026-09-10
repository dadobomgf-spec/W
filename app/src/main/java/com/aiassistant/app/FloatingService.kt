package com.aiassistant.app

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FloatingService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingBtn: Button
    private var chatView: LinearLayout? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createFloatingButton()
    }

    private fun createFloatingButton() {
        floatingBtn = Button(this).apply {
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

        floatingBtn.setOnClickListener { showChatWindow() }
        windowManager.addView(floatingBtn, params)
    }

    private fun showChatWindow() {
        if (chatView != null) return

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xE6000000.toInt())
            setPadding(30, 30, 30, 30)
        }

        val title = TextView(this).apply {
            text = "AI Assistant"
            setTextColor(0xFFFFA500.toInt())
            textSize = 20f
            setPadding(0, 0, 0, 20)
        }

        val input = EditText(this).apply {
            hint = "Type your command..."
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0xFF888888.toInt())
        }

        val sendBtn = Button(this).apply {
            text = "Send"
            setBackgroundColor(0xFFFFA500.toInt())
            setTextColor(0xFF000000.toInt())
        }

        val response = TextView(this).apply {
            text = ""
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 14f
            setPadding(0, 20, 0, 0)
        }

        val closeBtn = Button(this).apply {
            text = "Close"
            setBackgroundColor(0xFFFF0000.toInt())
            setTextColor(0xFFFFFFFF.toInt())
        }

        sendBtn.setOnClickListener {
            val userText = input.text.toString().trim()
            if (userText.isEmpty()) return@setOnClickListener
            input.setText("")
            response.text = "Thinking..."

            CoroutineScope(Dispatchers.IO).launch {
                val action = CommandHandler.handle(userText, this@FloatingService)
                val result = action ?: AIClient.ask(userText)
                withContext(Dispatchers.Main) {
                    response.text = result
                }
            }
        }

        closeBtn.setOnClickListener {
            chatView?.let { windowManager.removeView(it) }
            chatView = null
        }

        layout.addView(title)
        layout.addView(input)
        layout.addView(sendBtn)
        layout.addView(response)
        layout.addView(closeBtn)

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

    override fun onDestroy() {
        super.onDestroy()
        if (::floatingBtn.isInitialized) {
            windowManager.removeView(floatingBtn)
        }
        chatView?.let { windowManager.removeView(it) }
    }
}
