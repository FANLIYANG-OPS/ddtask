package com.ddtask.scheduler

import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.ddtask.scheduler.databinding.ActivityMainBinding
import com.ddtask.scheduler.fragment.TasksFragment
import com.ddtask.scheduler.ui.MainPagerAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewPager()
        setupBottomNav()
        setupFab()
        requestExactAlarmPermissionIfNeeded()
    }

    private fun setupViewPager() {
        binding.viewPager.adapter = MainPagerAdapter(this)
        binding.viewPager.isUserInputEnabled = false
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.bottomNav.menu.findItem(
                    if (position == 0) R.id.nav_tasks else R.id.nav_settings
                )?.isChecked = true
                binding.fabAdd.visibility = if (position == 0) View.VISIBLE else View.GONE
            }
        })
    }

    private fun setupBottomNav() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_tasks -> binding.viewPager.setCurrentItem(0, false)
                R.id.nav_settings -> binding.viewPager.setCurrentItem(1, false)
            }
            true
        }
    }

    private fun setupFab() {
        binding.fabAdd.setOnClickListener {
            (supportFragmentManager.findFragmentByTag("f0") as? TasksFragment)
                ?.showAddTaskDialog()
        }
    }

    private fun requestExactAlarmPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(AlarmManager::class.java)
            if (!alarmManager.canScheduleExactAlarms()) {
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.exact_alarm_title)
                    .setMessage(R.string.exact_alarm_message)
                    .setPositiveButton(R.string.go_settings) { _, _ ->
                        startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = Uri.parse("package:$packageName")
                        })
                    }
                    .setNegativeButton(R.string.later, null)
                    .show()
            }
        }
    }
}
