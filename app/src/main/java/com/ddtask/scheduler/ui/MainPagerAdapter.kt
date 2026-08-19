package com.ddtask.scheduler.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.ddtask.scheduler.MainActivity
import com.ddtask.scheduler.fragment.NotificationsFragment
import com.ddtask.scheduler.fragment.SettingsHostFragment
import com.ddtask.scheduler.fragment.TasksFragment

/** 主界面 ViewPager2 适配器：任务 / 通知 / 设置。 */
class MainPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = MainActivity.TAB_COUNT

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            MainActivity.TAB_TASKS -> TasksFragment()
            MainActivity.TAB_NOTIFICATIONS -> NotificationsFragment()
            else -> SettingsHostFragment()
        }
    }
}
