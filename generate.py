import os, shutil

for folder in ['app/src', 'app/build', 'build']:
    if os.path.exists(folder):
        shutil.rmtree(folder, ignore_errors=True)

files = {}

files['settings.gradle'] = r'''pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "FocusLock"
include ':app'
'''

files['build.gradle'] = r'''plugins {
    id 'com.android.application' version '8.5.0' apply false
    id 'org.jetbrains.kotlin.android' version '1.9.24' apply false
}
'''

files['gradle.properties'] = r'''org.gradle.jvmargs=-Xmx2g
android.nonTransitiveRClass=true
'''

files['app/build.gradle'] = r'''plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
}

android {
    namespace 'com.aurora.focuslock'
    compileSdk 34

    defaultConfig {
        applicationId 'com.aurora.focuslock'
        minSdk 24
        targetSdk 34
        versionCode 1
        versionName '3.6.0'
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = '17'
    }
}
'''

files['app/src/main/AndroidManifest.xml'] = r'''<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application
        android:label="@string/app_name"
        android:theme="@style/AppTheme">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
'''

files['app/src/main/res/values/strings.xml'] = r'''<resources>
    <string name="app_name">FocusLock</string>
</resources>
'''

files['app/src/main/res/values/themes.xml'] = r'''<resources>
    <style name="AppTheme" parent="android:Theme.Material.NoActionBar">
        <item name="android:statusBarColor">#0A2342</item>
        <item name="android:navigationBarColor">#051121</item>
        <item name="android:windowBackground">@android:color/black</item>
    </style>
</resources>
'''

files['app/src/main/java/com/aurora/focuslock/MainActivity.kt'] = r'''package com.aurora.focuslock

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.CountDownTimer
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    private var minutes = 25
    private var running = false
    private var timer: CountDownTimer? = null
    private lateinit var timeText: TextView
    private lateinit var statusText: TextView
    private lateinit var startBtn: Button
    private lateinit var presetRow: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.gravity = Gravity.CENTER
        root.setPadding(48, 48, 48, 48)
        root.background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(Color.parseColor("#0A2342"), Color.parseColor("#051121"))
        )

        val title = TextView(this)
        title.text = "Deep Ocean Sanctuary"
        title.setTextColor(Color.parseColor("#7FD1C7"))
        title.textSize = 20f
        title.gravity = Gravity.CENTER
        root.addView(title)

        timeText = TextView(this)
        timeText.text = format(minutes * 60000L)
        timeText.setTextColor(Color.WHITE)
        timeText.textSize = 72f
        timeText.typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        timeText.gravity = Gravity.CENTER
        timeText.setPadding(0, 64, 0, 16)
        root.addView(timeText)

        statusText = TextView(this)
        statusText.text = "Pick a duration and dive in"
        statusText.setTextColor(Color.parseColor("#9FB8CC"))
        statusText.textSize = 16f
        statusText.gravity = Gravity.CENTER
        statusText.setPadding(0, 0, 0, 48)
        root.addView(statusText)

        presetRow = LinearLayout(this)
        presetRow.orientation = LinearLayout.HORIZONTAL
        presetRow.gravity = Gravity.CENTER
        for (m in intArrayOf(15, 25, 45)) {
            val b = makeButton(m.toString() + " min", "#12395E")
            b.setOnClickListener {
                if (!running) {
                    minutes = m
                    timeText.text = format(m * 60000L)
                }
            }
            presetRow.addView(b)
        }
        root.addView(presetRow)

        startBtn = makeButton("Start Focus", "#1F8A84")
        startBtn.setOnClickListener {
            if (running) stopFocus(false) else startFocus()
        }
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        lp.topMargin = 64
        startBtn.layoutParams = lp
        root.addView(startBtn)

        setContentView(root)
    }

    private fun makeButton(label: String, color: String): Button {
        val b = Button(this)
        b.text = label
        b.setTextColor(Color.WHITE)
        b.isAllCaps = false
        val bg = GradientDrawable()
        bg.setColor(Color.parseColor(color))
        bg.cornerRadius = 60f
        b.background = bg
        val p = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        p.setMargins(12, 0, 12, 0)
        b.layoutParams = p
        return b
    }

    private fun startFocus() {
        running = true
        startBtn.text = "Surface (give up)"
        statusText.text = "Stay deep. Screen is pinned."
        try {
            startLockTask()
        } catch (e: Exception) {
        }
        timer = object : CountDownTimer(minutes * 60000L, 1000L) {
            override fun onTick(ms: Long) {
                timeText.text = format(ms)
            }

            override fun onFinish() {
                stopFocus(true)
            }
        }.start()
    }

    private fun stopFocus(done: Boolean) {
        timer?.cancel()
        running = false
        try {
            stopLockTask()
        } catch (e: Exception) {
        }
        startBtn.text = "Start Focus"
        timeText.text = format(minutes * 60000L)
        statusText.text = if (done) "Session complete. Well done." else "Session ended early"
    }

    private fun format(ms: Long): String {
        val s = ms / 1000
        return String.format("%02d:%02d", s / 60, s % 60)
    }
}
'''

for path, content in files.items():
    folder = os.path.dirname(path)
    if folder:
        os.makedirs(folder, exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
    print("Created: " + path)

print("Generated " + str(len(files)) + " files")
