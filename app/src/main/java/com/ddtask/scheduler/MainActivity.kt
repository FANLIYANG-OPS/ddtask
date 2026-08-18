package com.ddtask.scheduler

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.ddtask.scheduler.databinding.ActivityMainBinding
import com.ddtask.scheduler.fragment.TasksFragment
import com.ddtask.scheduler.ui.MainPagerAdapter
import com.ddtask.scheduler.util.ClockInSessionManager
import com.ddtask.scheduler.util.SettingsStorage

/** 应用主界面：任务 / 通知 / 设置 三 Tab，首次进入时弹出权限向导。 */
class MainActivity : AppCompatActivity() {

    companion object {
        const val TAB_TASKS = 0
        const val TAB_NOTIFICATIONS = 1
        const val TAB_SETTINGS = 2
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var settingsStorage: SettingsStorage

    private val permissionSetupLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* user finished setup wizard */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        settingsStorage = SettingsStorage(this)
        setupViewPager()
        setupBottomNav()
        setupFab()
        launchPermissionSetupIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        ClockInSessionManager(this).onAppForeground()
    }

    /** 未完成权限向导时自动拉起 [PermissionSetupActivity]。 */
    private fun launchPermissionSetupIfNeeded() {
        if (!settingsStorage.permissionSetupCompleted) {
            permissionSetupLauncher.launch(
                Intent(this, PermissionSetupActivity::class.java)
            )
        }
    }

    private fun setupViewPager() {
        binding.viewPager.adapter = MainPagerAdapter(this)
        // 禁用左右滑动，仅通过底部导航切换 Tab
        binding.viewPager.isUserInputEnabled = false
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val menuItemId = when (position) {
                    0 -> R.id.nav_tasks
                    1 -> R.id.nav_notifications
                    else -> R.id.nav_settings
                }
                binding.bottomNav.menu.findItem(menuItemId)?.isChecked = true
                binding.fabAdd.visibility = if (position == 0) View.VISIBLE else View.GONE
            }
        })
    }

    private fun setupBottomNav() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_tasks -> binding.viewPager.setCurrentItem(0, false)
                R.id.nav_notifications -> binding.viewPager.setCurrentItem(1, false)
                R.id.nav_settings -> binding.viewPager.setCurrentItem(2, false)
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

    fun openTab(index: Int) {
        binding.viewPager.setCurrentItem(index, false)
    }

    fun openPermissionSetup() {
        permissionSetupLauncher.launch(
            Intent(this, PermissionSetupActivity::class.java)
        )
    }
}
