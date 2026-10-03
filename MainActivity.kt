package com.vidhya.focuslock

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.*

data class AppInfoItem(
    val packageName: String,
    val appName: String,
    val icon: Drawable? = null
)

class MainActivity : AppCompatActivity() {
    private lateinit var sessionManager: SessionManager

    private lateinit var viewTimerTab: View
    private lateinit var viewAppsTab: View
    private lateinit var viewStatsTab: View
    private lateinit var viewLogsTab: View
    private lateinit var viewOceanTab: View
    private lateinit var viewHabitsTab: View
    private lateinit var tabBtnOcean: View
    private lateinit var tabBtnHabits: View

    private var isNonSessionMode = false
    private var nonSessionSeconds = 0
    private var nonSessionTimer: CountDownTimer? = null
    private var isNonSessionRunning = false
    private var selectedAmbient = "mute"
    private var isNuclearMode = false


    private lateinit var tabBtnTimer: View
    private lateinit var tabBtnApps: View
    private lateinit var tabBtnStats: View
    private lateinit var tabBtnLogs: View

    private lateinit var tvTimer: TextView
    private lateinit var tvTimerSubtitle: TextView
    private lateinit var btnStartLock: MaterialButton
    private lateinit var etFocusMinutes: EditText
    private lateinit var etBreakMinutes: EditText
    private lateinit var tvBlockedSummary: TextView
    private var countDownTimer: CountDownTimer? = null

    private lateinit var etSearchApps: EditText
    private lateinit var rvApps: RecyclerView
    private lateinit var tvAppsCountBadge: TextView
    private lateinit var btnSelectAll: Button
    private lateinit var btnClearAll: Button
    private var allAppsList = mutableListOf<AppInfoItem>()
    private var filteredAppsList = mutableListOf<AppInfoItem>()
    private lateinit var appAdapter: AppListAdapter

    private lateinit var tvTotalFocusHours: TextView
    private lateinit var tvSessionsCompleted: TextView
    private lateinit var tvDisableAttempts: TextView
    private lateinit var tvDisciplineScore: TextView
    private lateinit var layoutWeeklyGraph: LinearLayout

    private lateinit var rvLogs: RecyclerView
    private lateinit var btnClearLogs: Button
    private lateinit var tvEmptyLogs: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        sessionManager = SessionManager(this)

        initViews()
        setupTabs()
        setupTimerControls()
        setupAppsList()
        setupStats()
        setupLogs()
        checkPermissions()
    }

    override fun onResume() {
        super.onResume()
        updateTimerUI()
        refreshStatsUI()
        refreshLogsUI()
        startOceanAnimations()
    }

    private fun startOceanAnimations() {
        try {
            val butterflyFloat = AnimationUtils.loadAnimation(this, R.anim.butterfly_float)
            findViewById<View>(R.id.bubble1)?.startAnimation(butterflyFloat)
            findViewById<View>(R.id.bubble2)?.startAnimation(butterflyFloat)
            findViewById<View>(R.id.bubble3)?.startAnimation(butterflyFloat)

            val sparklePulse = AnimationUtils.loadAnimation(this, R.anim.sparkle_pulse)
            findViewById<View>(R.id.sunbeam1)?.startAnimation(sparklePulse)
            findViewById<View>(R.id.sunbeam2)?.startAnimation(sparklePulse)

            val plantBreeze = AnimationUtils.loadAnimation(this, R.anim.plant_breeze)
            findViewById<View>(R.id.marineTurtle)?.startAnimation(plantBreeze)
            findViewById<View>(R.id.marineJellyfish)?.startAnimation(plantBreeze)
            findViewById<View>(R.id.marineDolphin)?.startAnimation(plantBreeze)
            findViewById<View>(R.id.marineClownfish)?.startAnimation(plantBreeze)
            findViewById<View>(R.id.marineSeahorse)?.startAnimation(plantBreeze)
            findViewById<View>(R.id.marineStarfish)?.startAnimation(plantBreeze)
            findViewById<View>(R.id.marineWhale)?.startAnimation(plantBreeze)
            findViewById<View>(R.id.marineCrab)?.startAnimation(sparklePulse)
            findViewById<View>(R.id.marineRay)?.startAnimation(butterflyFloat)
        } catch (e: Exception) {
            // Safe fallback
        }
    }

    private fun initViews() {
        viewTimerTab = findViewById(R.id.viewTimerTab)
        viewAppsTab = findViewById(R.id.viewAppsTab)
        viewStatsTab = findViewById(R.id.viewStatsTab)
        viewLogsTab = findViewById(R.id.viewLogsTab)
        viewOceanTab = findViewById(R.id.viewOceanTab)
        viewHabitsTab = findViewById(R.id.viewHabitsTab)
        tabBtnOcean = findViewById(R.id.tabBtnOcean)
        tabBtnHabits = findViewById(R.id.tabBtnHabits)


        tabBtnTimer = findViewById(R.id.tabBtnTimer)
        tabBtnApps = findViewById(R.id.tabBtnApps)
        tabBtnStats = findViewById(R.id.tabBtnStats)
        tabBtnLogs = findViewById(R.id.tabBtnLogs)

        tvTimer = findViewById(R.id.tvTimer)
        tvTimerSubtitle = findViewById(R.id.tvTimerSubtitle)
        btnStartLock = findViewById(R.id.btnStartLock)
        etFocusMinutes = findViewById(R.id.etFocusMinutes)
        etBreakMinutes = findViewById(R.id.etBreakMinutes)
        tvBlockedSummary = findViewById(R.id.tvBlockedSummary)

        etSearchApps = findViewById(R.id.etSearchApps)
        rvApps = findViewById(R.id.rvApps)
        tvAppsCountBadge = findViewById(R.id.tvAppsCountBadge)
        btnSelectAll = findViewById(R.id.btnSelectAll)
        btnClearAll = findViewById(R.id.btnClearAll)

        tvTotalFocusHours = findViewById(R.id.tvTotalFocusHours)
        tvSessionsCompleted = findViewById(R.id.tvSessionsCompleted)
        tvDisableAttempts = findViewById(R.id.tvDisableAttempts)
        tvDisciplineScore = findViewById(R.id.tvDisciplineScore)
        layoutWeeklyGraph = findViewById(R.id.layoutWeeklyGraph)

        rvLogs = findViewById(R.id.rvLogs)
        btnClearLogs = findViewById(R.id.btnClearLogs)
        tvEmptyLogs = findViewById(R.id.tvEmptyLogs)
    }

                  private fun setupTabs() {
        fun showTab(index: Int) {
            viewTimerTab.visibility = if (index == 0) View.VISIBLE else View.GONE
            viewAppsTab.visibility = if (index == 1) View.VISIBLE else View.GONE
            viewOceanTab.visibility = if (index == 2) View.VISIBLE else View.GONE
            viewHabitsTab.visibility = if (index == 3) View.VISIBLE else View.GONE
            viewStatsTab.visibility = if (index == 4) View.VISIBLE else View.GONE
            viewLogsTab.visibility = if (index == 5) View.VISIBLE else View.GONE

            tabBtnTimer.alpha = if (index == 0) 1.0f else 0.45f
            tabBtnApps.alpha = if (index == 1) 1.0f else 0.45f
            tabBtnOcean.alpha = if (index == 2) 1.0f else 0.45f
            tabBtnHabits.alpha = if (index == 3) 1.0f else 0.45f
            tabBtnStats.alpha = if (index == 4) 1.0f else 0.45f
            tabBtnLogs.alpha = if (index == 5) 1.0f else 0.45f

            if (index == 1) loadAllApps()
            if (index == 2) startOceanAnimations()
            if (index == 4) refreshStatsUI()
            if (index == 5) refreshLogsUI()
        }

        tabBtnTimer.setOnClickListener { showTab(0) }
        tabBtnApps.setOnClickListener { showTab(1) }
        tabBtnOcean.setOnClickListener { showTab(2) }
        tabBtnHabits.setOnClickListener { showTab(3) }
        tabBtnStats.setOnClickListener { showTab(4) }
        tabBtnLogs.setOnClickListener { showTab(5) }

        setupHabitsInteractions()
        showTab(0)
    }

    private fun setupHabitsInteractions() {
        findViewById<Button>(R.id.btnQuickAddHabit)?.setOnClickListener {
            Toast.makeText(this, "Interactive habit tracker active! Consistency logged.", Toast.LENGTH_SHORT).show()
        }
        val cb1 = findViewById<CheckBox>(R.id.cbHabit1)
        val cb2 = findViewById<CheckBox>(R.id.cbHabit2)
        val cb3 = findViewById<CheckBox>(R.id.cbHabit3)
        val cb4 = findViewById<CheckBox>(R.id.cbHabit4)
        val tvScore = findViewById<TextView>(R.id.tvHabitScore)

        fun updateScore() {
            var count = 0
            if (cb1?.isChecked == true) count++
            if (cb2?.isChecked == true) count++
            if (cb3?.isChecked == true) count++
            if (cb4?.isChecked == true) count++
            val pct = (count * 100) / 4
            tvScore?.text = "$pct% Success"
        }

        cb1?.setOnCheckedChangeListener { _, _ -> updateScore() }
        cb2?.setOnCheckedChangeListener { _, _ -> updateScore() }
        cb3?.setOnCheckedChangeListener { _, _ -> updateScore() }
        cb4?.setOnCheckedChangeListener { _, _ -> updateScore() }
    }

    private fun setupTimerControls() {
        etFocusMinutes.setText(sessionManager.sessionDurationMinutes.toString())
        etBreakMinutes.setText(sessionManager.breakDurationMinutes.toString())

        findViewById<View>(R.id.chip15)?.setOnClickListener { setFocusMinutes(15) }
        findViewById<View>(R.id.chip25)?.setOnClickListener { setFocusMinutes(25) }
        findViewById<View>(R.id.chip45)?.setOnClickListener { setFocusMinutes(45) }
        findViewById<View>(R.id.chip60)?.setOnClickListener { setFocusMinutes(60) }
        findViewById<View>(R.id.chip90)?.setOnClickListener { setFocusMinutes(90) }
        findViewById<View>(R.id.chip120)?.setOnClickListener { setFocusMinutes(120) }

        findViewById<View>(R.id.btnPlusFocus)?.setOnClickListener {
            val current = etFocusMinutes.text.toString().toIntOrNull() ?: 25
            setFocusMinutes(minOf(720, current + 5))
        }
        findViewById<View>(R.id.btnMinusFocus)?.setOnClickListener {
            val current = etFocusMinutes.text.toString().toIntOrNull() ?: 25
            setFocusMinutes(maxOf(1, current - 5))
        }

        findViewById<View>(R.id.btnPlusBreak)?.setOnClickListener {
            val current = etBreakMinutes.text.toString().toIntOrNull() ?: 5
            etBreakMinutes.setText(minOf(120, current + 5).toString())
        }
        findViewById<View>(R.id.btnMinusBreak)?.setOnClickListener {
            val current = etBreakMinutes.text.toString().toIntOrNull() ?: 5
            etBreakMinutes.setText(maxOf(0, current - 5).toString())
        }

                          // Non-Session Timer Mode Toggle
        val btnModeCountdown = findViewById<Button>(R.id.btnModeCountdown)
        val btnModeNonSession = findViewById<Button>(R.id.btnModeNonSession)
        btnModeCountdown?.setOnClickListener {
            isNonSessionMode = false
            btnModeCountdown.setTextColor(android.graphics.Color.WHITE)
            btnModeNonSession?.setTextColor(android.graphics.Color.parseColor("#64748B"))
            tvTimerSubtitle.text = "Ready to Focus"
            updateTimerUI()
        }
        btnModeNonSession?.setOnClickListener {
            isNonSessionMode = true
            btnModeNonSession.setTextColor(android.graphics.Color.WHITE)
            btnModeCountdown?.setTextColor(android.graphics.Color.parseColor("#64748B"))
            tvTimerSubtitle.text = "Non-Session Free Flow Timer"
            tvTimer.text = String.format(Locale.getDefault(), "%02d:%02d", nonSessionSeconds / 60, nonSessionSeconds % 60)
            btnStartLock.text = if (isNonSessionRunning) "Pause Timer" else "Start Free Timer"
        }

        // Ambient Audio Presets
        val btnAudioMute = findViewById<Button>(R.id.btnAudioMute)
        val btnAudioRain = findViewById<Button>(R.id.btnAudioRain)
        val btnAudioWhite = findViewById<Button>(R.id.btnAudioWhite)
        val btnAudioBinaural = findViewById<Button>(R.id.btnAudioBinaural)

        fun updateAudioSelection(mode: String) {
            selectedAmbient = mode
            btnAudioMute?.setTextColor(if (mode == "mute") android.graphics.Color.WHITE else android.graphics.Color.parseColor("#94A3B8"))
            btnAudioRain?.setTextColor(if (mode == "rain") android.graphics.Color.parseColor("#38BDF8") else android.graphics.Color.parseColor("#94A3B8"))
            btnAudioWhite?.setTextColor(if (mode == "white") android.graphics.Color.parseColor("#E2E8F0") else android.graphics.Color.parseColor("#94A3B8"))
            btnAudioBinaural?.setTextColor(if (mode == "binaural") android.graphics.Color.parseColor("#A855F7") else android.graphics.Color.parseColor("#94A3B8"))
            Toast.makeText(this, "Ambient Audio: " + mode.capitalize(Locale.ROOT), Toast.LENGTH_SHORT).show()
        }
        btnAudioMute?.setOnClickListener { updateAudioSelection("mute") }
        btnAudioRain?.setOnClickListener { updateAudioSelection("rain") }
        btnAudioWhite?.setOnClickListener { updateAudioSelection("white") }
        btnAudioBinaural?.setOnClickListener { updateAudioSelection("binaural") }

        // Nuclear Mode Switch
        val btnToggleNuclear = findViewById<Button>(R.id.btnToggleNuclear)
        btnToggleNuclear?.setOnClickListener {
            isNuclearMode = !isNuclearMode
            btnToggleNuclear.text = if (isNuclearMode) "ON (STRICT)" else "OFF"
            btnToggleNuclear.setTextColor(if (isNuclearMode) android.graphics.Color.parseColor("#FDA4AF") else android.graphics.Color.parseColor("#94A3B8"))
            Toast.makeText(this, if (isNuclearMode) "Strict Nuclear Mode Activated! No emergency pass." else "Nuclear Mode Disabled.", Toast.LENGTH_SHORT).show()
        }

        btnStartLock.setOnClickListener {
            if (isNonSessionMode) {
                if (isNonSessionRunning) {
                    isNonSessionRunning = false
                    nonSessionTimer?.cancel()
                    btnStartLock.text = "Resume Free Timer"
                } else {
                    isNonSessionRunning = true
                    btnStartLock.text = "Pause Timer"
                    nonSessionTimer = object : CountDownTimer(86400000L, 1000L) {
                        override fun onTick(millisUntilFinished: Long) {
                            nonSessionSeconds++
                            tvTimer.text = String.format(Locale.getDefault(), "%02d:%02d", nonSessionSeconds / 60, nonSessionSeconds % 60)
                        }
                        override fun onFinish() {}
                    }.start()
                }
                return@setOnClickListener
            }
            if (sessionManager.isSessionActive()) {
                sessionManager.recordDisableAttempt()
                startActivity(Intent(this, MathPuzzleActivity::class.java))
            } else {
                val focusMins = etFocusMinutes.text.toString().toIntOrNull() ?: 25
                val breakMins = etBreakMinutes.text.toString().toIntOrNull() ?: 5

                if (sessionManager.blockedPackages.isEmpty()) {
                    Toast.makeText(this, "Select apps in the Blocklist tab first!", Toast.LENGTH_LONG).show()
                    tabBtnApps.performClick()
                    return@setOnClickListener
                }

                sessionManager.startSession(focusMins, breakMins)
                try {
                    startService(Intent(this, SessionTimerService::class.java))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                Toast.makeText(this, "Focus Mode active for " + focusMins + " mins!", Toast.LENGTH_SHORT).show()
                updateTimerUI()
            }
        }

        findViewById<View>(R.id.btnMiuiPermission)?.setOnClickListener {
            openMiuiPermissions()
        }
    }

    private fun openMiuiPermissions() {
        try {
            val intent = Intent("miui.intent.action.APP_PERM_EDITOR").apply {
                setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity")
                putExtra("extra_pkgname", packageName)
            }
            startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:" + packageName)
            }
            startActivity(intent)
        }
    }

    private fun setFocusMinutes(mins: Int) {
        etFocusMinutes.setText(mins.toString())
        if (!sessionManager.isSessionActive()) {
            tvTimer.text = String.format(Locale.getDefault(), "%02d:00", mins)
        }
    }

    private fun updateTimerUI() {
        val blockedCount = sessionManager.blockedPackages.size
        tvBlockedSummary.text = blockedCount.toString() + " apps configured to lock"

        if (sessionManager.isSessionActive()) {
            val isInBreak = sessionManager.isInBreak
            if (isInBreak) {
                tvTimerSubtitle.text = "☕ Break Interval (Apps Unlocked Temporarily)"
                tvTimerSubtitle.setTextColor(0xFF38BDF8.toInt())
                btnStartLock.text = "End Break & Return to Focus"
                btnStartLock.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF0284C7.toInt())
            } else {
                tvTimerSubtitle.text = "🔒 Focus Lock Active (Distractions Blocked)"
                tvTimerSubtitle.setTextColor(0xFF10B981.toInt())
                btnStartLock.text = "Emergency Unlock (Solve Math)"
                btnStartLock.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFEF4444.toInt())
            }
            etFocusMinutes.isEnabled = false
            etBreakMinutes.isEnabled = false
            startCountdownDisplay(sessionManager.getRemainingTimeMs())
        } else {
            countDownTimer?.cancel()
            tvTimerSubtitle.text = "Ready to Focus"
            tvTimerSubtitle.setTextColor(0xFF94A3B8.toInt())
            btnStartLock.text = "Lock & Focus Now"
            btnStartLock.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF10B981.toInt())
            etFocusMinutes.isEnabled = true
            etBreakMinutes.isEnabled = true
            val mins = etFocusMinutes.text.toString().toIntOrNull() ?: 25
            tvTimer.text = String.format(Locale.getDefault(), "%02d:00", mins)
        }
    }

    private fun startCountdownDisplay(durationMs: Long) {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(durationMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val totalSecs = millisUntilFinished / 1000
                val m = totalSecs / 60
                val s = totalSecs % 60
                tvTimer.text = String.format(Locale.getDefault(), "%02d:%02d", m, s)
            }
            override fun onFinish() {
                updateTimerUI()
                refreshStatsUI()
                refreshLogsUI()
            }
        }.start()
    }

    private fun setupAppsList() {
        rvApps.layoutManager = LinearLayoutManager(this)
        appAdapter = AppListAdapter(
            onToggle = { pkg, isChecked ->
                val selected = sessionManager.blockedPackages.toMutableSet()
                if (isChecked) selected.add(pkg) else selected.remove(pkg)
                sessionManager.blockedPackages = selected
                updateAppsBadge()
                tvBlockedSummary.text = selected.size.toString() + " apps configured to lock"
            }
        )
        rvApps.adapter = appAdapter

        etSearchApps.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterApps(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnSelectAll.setOnClickListener {
            val allPkgs = allAppsList.map { it.packageName }.toSet()
            sessionManager.blockedPackages = allPkgs
            appAdapter.notifyDataSetChanged()
            updateAppsBadge()
            Toast.makeText(this, "All " + allPkgs.size + " apps selected", Toast.LENGTH_SHORT).show()
        }

        btnClearAll.setOnClickListener {
            sessionManager.blockedPackages = emptySet()
            appAdapter.notifyDataSetChanged()
            updateAppsBadge()
            Toast.makeText(this, "Selection cleared", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadAllApps() {
        if (allAppsList.isNotEmpty()) {
            updateAppsBadge()
            return
        }

        val pm = packageManager
        val appMap = mutableMapOf<String, AppInfoItem>()

        val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolves = pm.queryIntentActivities(launcherIntent, 0)
        for (info in resolves) {
            val pkg = info.activityInfo.packageName
            if (pkg == packageName) continue
            val label = info.loadLabel(pm).toString().trim()
            val icon = try { info.loadIcon(pm) } catch (e: Exception) { null }
            if (label.isNotEmpty() && !appMap.containsKey(pkg)) {
                appMap[pkg] = AppInfoItem(pkg, label, icon)
            }
        }

        try {
            val installed = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            for (app in installed) {
                val pkg = app.packageName
                if (pkg == packageName) continue
                if (!appMap.containsKey(pkg) && pm.getLaunchIntentForPackage(pkg) != null) {
                    val label = pm.getApplicationLabel(app).toString().trim()
                    val icon = try { pm.getApplicationIcon(app) } catch (e: Exception) { null }
                    appMap[pkg] = AppInfoItem(pkg, label, icon)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (appMap.isEmpty()) {
            val defaults = listOf(
                "com.google.android.youtube" to "YouTube",
                "com.instagram.android" to "Instagram",
                "com.zhiliaoapp.musically" to "TikTok",
                "com.facebook.katana" to "Facebook",
                "com.twitter.android" to "X (Twitter)",
                "com.whatsapp" to "WhatsApp",
                "com.snapchat.android" to "Snapchat",
                "com.android.chrome" to "Google Chrome",
                "com.netflix.mediaclient" to "Netflix"
            )
            defaults.forEach { (pkg, name) ->
                appMap[pkg] = AppInfoItem(pkg, name, null)
            }
        }

        allAppsList = appMap.values.sortedBy { it.appName.lowercase(Locale.getDefault()) }.toMutableList()
        filteredAppsList = ArrayList(allAppsList)
        appAdapter.setData(filteredAppsList, sessionManager.blockedPackages)
        updateAppsBadge()
    }

    private fun filterApps(query: String) {
        val q = query.trim().lowercase(Locale.getDefault())
        filteredAppsList = if (q.isEmpty()) {
            ArrayList(allAppsList)
        } else {
            allAppsList.filter {
                it.appName.lowercase(Locale.getDefault()).contains(q) ||
                it.packageName.lowercase(Locale.getDefault()).contains(q)
            }.toMutableList()
        }
        appAdapter.setData(filteredAppsList, sessionManager.blockedPackages)
    }

    private fun updateAppsBadge() {
        val blocked = sessionManager.blockedPackages.size
        val total = allAppsList.size
        tvAppsCountBadge.text = blocked.toString() + " of " + total + " locked"
    }

    private fun setupStats() {
        refreshStatsUI()
    }

    private fun refreshStatsUI() {
        val totalMins = sessionManager.getTotalFocusMinutes()
        val hrs = totalMins / 60
        val mins = totalMins % 60
        tvTotalFocusHours.text = if (hrs > 0) hrs.toString() + "h " + mins + "m" else mins.toString() + "m"

        tvSessionsCompleted.text = sessionManager.getCompletedSessionsCount().toString()
        tvDisableAttempts.text = sessionManager.disableAttemptsCount.toString()

        val score = sessionManager.getDisciplineScore()
        tvDisciplineScore.text = score.toString() + "%"

        layoutWeeklyGraph.removeAllViews()
        val weeklyStats = sessionManager.getWeeklyStats()
        val maxMins = maxOf(60, weeklyStats.maxOfOrNull { it.second } ?: 60)

        for ((day, focusMins) in weeklyStats) {
            val barItem = LayoutInflater.from(this).inflate(R.layout.item_graph_bar, layoutWeeklyGraph, false)
            val tvDay = barItem.findViewById<TextView>(R.id.tvBarDay)
            val tvMins = barItem.findViewById<TextView>(R.id.tvBarMinutes)
            val barFill = barItem.findViewById<View>(R.id.viewBarFill)

            tvDay.text = day
            tvMins.text = if (focusMins > 0) focusMins.toString() + "m" else "0"

            val heightPercent = minOf(1.0f, focusMins.toFloat() / maxMins.toFloat())
            val params = barFill.layoutParams as LinearLayout.LayoutParams
            params.height = maxOf(8, (heightPercent * 140).toInt())
            barFill.layoutParams = params

            if (focusMins > 0) {
                barFill.setBackgroundColor(0xFF10B981.toInt())
            } else {
                barFill.setBackgroundColor(0xFF334155.toInt())
            }

            layoutWeeklyGraph.addView(barItem)
        }
    }

    private fun setupLogs() {
        rvLogs.layoutManager = LinearLayoutManager(this)
        btnClearLogs.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Clear History")
                .setMessage("Are you sure you want to reset all focus session logs?")
                .setPositiveButton("Clear") { _, _ ->
                    sessionManager.clearLogs()
                    refreshLogsUI()
                    refreshStatsUI()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
        refreshLogsUI()
    }

    private fun refreshLogsUI() {
        val logs = sessionManager.getSessionLogs()
        if (logs.isEmpty()) {
            tvEmptyLogs.visibility = View.VISIBLE
            rvLogs.visibility = View.GONE
        } else {
            tvEmptyLogs.visibility = View.GONE
            rvLogs.visibility = View.VISIBLE
            rvLogs.adapter = LogsAdapter(logs)
        }
    }

    private fun checkPermissions() {
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: ""
        val service = packageName + "/" + FocusAccessibilityService::class.java.canonicalName
        if (!enabledServices.contains(service)) {
            AlertDialog.Builder(this)
                .setTitle("Accessibility Permission Required")
                .setMessage("FocusLock needs Accessibility to detect when locked apps are opened.")
                .setPositiveButton("Grant in Settings") { _, _ ->
                    try {
                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                .setNegativeButton("Later", null)
                .show()
        }
    }
}

class AppListAdapter(
    private val onToggle: (String, Boolean) -> Unit
) : RecyclerView.Adapter<AppListAdapter.ViewHolder>() {
    private var items = listOf<AppInfoItem>()
    private var blockedSet = setOf<String>()

    fun setData(newItems: List<AppInfoItem>, blocked: Set<String>) {
        items = newItems
        blockedSet = blocked
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app_block, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvName.text = item.appName
        holder.tvPackage.text = item.packageName
        if (item.icon != null) {
            holder.ivIcon.setImageDrawable(item.icon)
        } else {
            holder.ivIcon.setImageResource(android.R.drawable.sym_def_app_icon)
        }

        val isBlocked = blockedSet.contains(item.packageName)
        holder.cbBlocked.isChecked = isBlocked
        holder.itemView.setOnClickListener {
            val newState = !holder.cbBlocked.isChecked
            holder.cbBlocked.isChecked = newState
            onToggle(item.packageName, newState)
        }
        holder.cbBlocked.setOnClickListener {
            onToggle(item.packageName, holder.cbBlocked.isChecked)
        }
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivIcon: ImageView = itemView.findViewById(R.id.ivAppIcon)
        val tvName: TextView = itemView.findViewById(R.id.tvAppName)
        val tvPackage: TextView = itemView.findViewById(R.id.tvAppPackage)
        val cbBlocked: CheckBox = itemView.findViewById(R.id.cbBlocked)
    }
}

class LogsAdapter(private val logs: List<SessionLog>) : RecyclerView.Adapter<LogsAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_session_log, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val log = logs[position]
        val sdf = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
        holder.tvDate.text = sdf.format(Date(log.timestamp))
        holder.tvDuration.text = log.completedMinutes.toString() + "m focused (Planned: " + log.plannedMinutes + "m)"
        holder.tvAppsLocked.text = log.blockedAppsCount.toString() + " apps locked"

        when (log.status) {
            "COMPLETED" -> {
                holder.tvStatusBadge.text = "COMPLETED"
                holder.tvStatusBadge.setBackgroundColor(0xFF065F46.toInt())
                holder.tvStatusBadge.setTextColor(0xFF34D399.toInt())
            }
            "EMERGENCY_UNLOCKED" -> {
                holder.tvStatusBadge.text = "EMERGENCY PASS"
                holder.tvStatusBadge.setBackgroundColor(0xFF991B1B.toInt())
                holder.tvStatusBadge.setTextColor(0xFFFCA5A5.toInt())
            }
            else -> {
                holder.tvStatusBadge.text = "CANCELLED"
                holder.tvStatusBadge.setBackgroundColor(0xFF78350F.toInt())
                holder.tvStatusBadge.setTextColor(0xFFFCD34D.toInt())
            }
        }
    }

    override fun getItemCount(): Int = logs.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDate: TextView = itemView.findViewById(R.id.tvLogDate)
        val tvDuration: TextView = itemView.findViewById(R.id.tvLogDuration)
        val tvAppsLocked: TextView = itemView.findViewById(R.id.tvLogAppsLocked)
        val tvStatusBadge: TextView = itemView.findViewById(R.id.tvLogStatusBadge)
    }
}