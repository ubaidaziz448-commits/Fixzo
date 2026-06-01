package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workers")
data class WorkerEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phone: String,
    val category: String, // Plumber, Electrician, Carpenter, AC Technician, Painter, Welder
    val location: String, // Islamabad, Lahore, Karachi, Rawalpindi, Peshawar, Faisalabad
    val rating: Double = 5.0,
    val ratingCount: Int = 1,
    val isVerified: Boolean = false,
    val cnicNumber: String? = null,
    val currentLat: Double = 33.6844, // Default Islamabad
    val currentLng: Double = 73.0479,
    val isOffline: Boolean = false
)

@Entity(tableName = "bookings")
data class BookingEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val workerId: Int,
    val workerName: String,
    val workerCategory: String,
    val customerName: String,
    val customerPhone: String,
    val status: String, // Pending, Accepted, In_Progress, Completed, Cancelled
    val paymentMethod: String, // JazzCash, EasyPaisa, Cash
    val amount: Double,
    val ratingSubmitted: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bookingId: Int,
    val sender: String, // "Customer" or "Worker"
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "wallet_transactions")
data class WalletTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val type: String, // "Deposit" (earnings) or "Withdrawal"
    val description: String,
    val referenceChannel: String, // "JazzCash" or "EasyPaisa" or "Job Completion"
    val timestamp: Long = System.currentTimeMillis()
)
