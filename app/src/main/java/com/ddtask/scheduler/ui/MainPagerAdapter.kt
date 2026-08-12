package com.ddtask.scheduler.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.ddtask.scheduler.fragment.NotificationsFragment
import com.ddtask.scheduler.fragment.SettingsFragment
import com.ddtask.scheduler.fragment.TasksFragment

class MainPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> TasksFragment()
            1 -> NotificationsFragment()
            else -> SettingsFragment()
        }
    }
}
