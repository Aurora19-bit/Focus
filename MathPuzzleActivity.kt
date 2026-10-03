package com.vidhya.focuslock

import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import kotlin.random.Random

class MathPuzzleActivity : AppCompatActivity() {
    private var expectedAnswer: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_puzzle)
        val sessionManager = SessionManager(this)

        val n1 = Random.nextInt(15, 35)
        val n2 = Random.nextInt(12, 25)
        val n3 = Random.nextInt(15, 60)
        expectedAnswer = (n1 * n2) - n3

        findViewById<TextView>(R.id.tvEquation)?.text = "$n1 × $n2 - $n3 = ?"
        val etAnswer = findViewById<EditText>(R.id.etAnswer)

        findViewById<MaterialButton>(R.id.btnSubmit)?.setOnClickListener {
            val input = etAnswer?.text?.toString()?.trim()
            if (input == expectedAnswer.toString()) {
                sessionManager.grantEmergencyPass(2)
                Toast.makeText(this, "Correct! 2-minute pass granted.", Toast.LENGTH_LONG).show()
                finish()
            } else {
                sessionManager.recordDisableAttempt()
                Toast.makeText(this, "Incorrect! Stay focused and try again.", Toast.LENGTH_SHORT).show()
                etAnswer?.setText("")
            }
        }
    }
}