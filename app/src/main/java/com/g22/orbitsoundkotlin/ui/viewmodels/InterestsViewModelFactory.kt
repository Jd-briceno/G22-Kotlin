package com.g22.orbitsoundkotlin.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.g22.orbitsoundkotlin.data.UserPreferencesRepository
import com.g22.orbitsoundkotlin.data.repositories.InterestsRepository

class InterestsViewModelFactory(
    private val interestsRepository: InterestsRepository,
    private val preferencesRepository: UserPreferencesRepository?
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InterestsViewModel::class.java)) {
            return InterestsViewModel(
                interestsRepository = interestsRepository,
                preferencesRepository = preferencesRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

