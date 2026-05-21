package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "users")
data class User(
    @PrimaryKey val phone: String, // Serving as unique identifier
    val name: String,
    val role: String, // "customer" or "driver"
    val pin: String, // Simple login pin
    val vehicleNumber: String = "", // For drivers only
    val rating: Float = 4.8f, // Global driver rating
    val ratingsCount: Int = 10,
    val balance: Double = 350.0 // Virtual cash wallet for payments
) : Serializable

@Entity(tableName = "rides")
data class Ride(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val customerPhone: String,
    val customerName: String,
    val driverPhone: String = "",
    val driverName: String = "",
    val pickupName: String,
    val dropName: String,
    val status: String, // "PENDING", "ACCEPTED", "ARRIVED", "TRIP_STARTED", "COMPLETED", "CANCELLED"
    val fare: Double,
    val paymentMethod: String, // "UPI", "Wallet", "Cash", "Card"
    val paymentStatus: String, // "PENDING", "PAID"
    val timestamp: Long = System.currentTimeMillis(),
    val driverRating: Float = 0f, // Rating given of 1 to 5 stars
    val feedback: String = ""
) : Serializable

@Entity(tableName = "call_logs")
data class CallLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fromPhone: String,
    val fromName: String,
    val toPhone: String,
    val toName: String,
    val durationSeconds: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
) : Serializable
