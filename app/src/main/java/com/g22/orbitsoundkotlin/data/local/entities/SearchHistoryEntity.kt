package com.g22.orbitsoundkotlin.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Search history entity for Library screen searches.
 * Stores user search queries for autocomplete and recent searches feature.
 * 
 * Indexed by userId for efficient retrieval of user-specific search history.
 * No expiration - persists until manually cleared by user.
 */
@Entity(
    tableName = "search_history",
    indices = [Index(value = ["userId"])]
)
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val query: String,
    val timestamp: Long = System.currentTimeMillis()
)

