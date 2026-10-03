package com.vidhya.focuslock

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class BlockActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_block)
        val pkg = intent.getStringExtra("BLOCKED_PACKAGE") ?: "This app"

        findViewById<TextView>(R.id.tvBlockedMessage)?.text =
            "\"" + pkg + "\" is locked until your focus session ends."

        findViewById<Button>(R.id.btnGoHome)?.setOnClickListener {
            val home = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(home)
            finish()
        }

        findViewById<Button>(R.id.btnEmergency)?.setOnClickListener {
            startActivity(Intent(this, MathPuzzleActivity::class.java))
            finish()
        }
    }

    override fun onBackPressed() {
        val home = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(home)
        finish()
    }
}