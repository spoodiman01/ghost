package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class Profile(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: ProfileType,
    val isActive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
