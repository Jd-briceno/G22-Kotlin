package com.g22.orbitsoundkotlin.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.g22.orbitsoundkotlin.data.local.AppDatabase
import com.g22.orbitsoundkotlin.data.repositories.SessionActivityRepository

/**
 * Factory para crear CaptainsLogViewModel con dependencias inyectadas.
 * 
 * Inyecta:
 * - SessionActivityRepository (con acceso a Room Database)
 * - userId (del usuario autenticado)
 * - userEmail (email del usuario autenticado, opcional)
 */
class CaptainsLogViewModelFactory(
    private val context: Context,
    private val userId: String,
    private val userEmail: String? = null
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CaptainsLogViewModel::class.java)) {
            val database = AppDatabase.getInstance(context)
            val repository = SessionActivityRepository(database)
            return CaptainsLogViewModel(repository, userId, userEmail) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

