package com.g22.orbitsoundkotlin.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.g22.orbitsoundkotlin.data.local.AppDatabase
import com.g22.orbitsoundkotlin.data.repositories.ActivityStatsRepository
import com.google.firebase.auth.FirebaseAuth

/**
 * Factory para crear ActivityStatsViewModel con dependencias inyectadas.
 */
class ActivityStatsViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ActivityStatsViewModel::class.java)) {
            val database = AppDatabase.getInstance(context)
            val repository = ActivityStatsRepository(database)
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "unknown_user"
            val userEmail = FirebaseAuth.getInstance().currentUser?.email
            return ActivityStatsViewModel(repository, userId, userEmail) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

