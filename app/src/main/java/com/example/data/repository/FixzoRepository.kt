package com.example.data.repository

import com.example.data.dao.FixzoDao
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class FixzoRepository(private val dao: FixzoDao) {

    val allWorkers: Flow<List<WorkerEntity>> = dao.getAllWorkers()
    val allBookings: Flow<List<BookingEntity>> = dao.getAllBookings()
    val activeBooking: Flow<BookingEntity?> = dao.getActiveBooking()
    val walletTransactions: Flow<List<WalletTransactionEntity>> = dao.getWalletTransactions()

    // Query workers by category and location
    fun getWorkers(category: String, location: String): Flow<List<WorkerEntity>> {
        return dao.getWorkersByCategoryAndLocation(category, location)
    }

    suspend fun getWorkerById(id: Int): WorkerEntity? = dao.getWorkerById(id)

    suspend fun registerWorker(worker: WorkerEntity): Long = dao.insertWorker(worker)

    suspend fun updateWorker(worker: WorkerEntity) = dao.updateWorker(worker)

    suspend fun createBooking(booking: BookingEntity): Long = dao.insertBooking(booking)

    suspend fun updateBooking(booking: BookingEntity) = dao.updateBooking(booking)

    suspend fun getBookingById(id: Int): BookingEntity? = dao.getBookingById(id)

    fun getChatMessages(bookingId: Int): Flow<List<ChatMessageEntity>> = dao.getMessagesForBooking(bookingId)

    suspend fun sendChatMessage(message: ChatMessageEntity): Long = dao.insertChatMessage(message)

    suspend fun addWalletTransaction(transaction: WalletTransactionEntity): Long = dao.insertWalletTransaction(transaction)

    // Seed data helper
    suspend fun seedDatabaseIfEmpty() {
        val count = dao.getWorkersCount()
        if (count == 0) {
            val seedWorkers = listOf(
                WorkerEntity(
                    name = "M. Asif Khan",
                    phone = "+923001234567",
                    category = "Plumber",
                    location = "Islamabad",
                    rating = 4.8,
                    ratingCount = 24,
                    isVerified = true,
                    cnicNumber = "37405-1234567-1",
                    currentLat = 33.6844,
                    currentLng = 73.0479
                ),
                WorkerEntity(
                    name = "Zubair Masood",
                    phone = "+923129876541",
                    category = "Plumber",
                    location = "Karachi",
                    rating = 4.9,
                    ratingCount = 15,
                    isVerified = true,
                    cnicNumber = "42201-9876543-1",
                    currentLat = 24.8607,
                    currentLng = 67.0011
                ),
                WorkerEntity(
                    name = "Tanveer Ahmed",
                    phone = "+923214567890",
                    category = "Electrician",
                    location = "Lahore",
                    rating = 4.7,
                    ratingCount = 38,
                    isVerified = true,
                    cnicNumber = "35202-4567890-1",
                    currentLat = 31.5204,
                    currentLng = 74.3587
                ),
                WorkerEntity(
                    name = "Kamran Butt",
                    phone = "+923337654321",
                    category = "AC Technician",
                    location = "Islamabad",
                    rating = 4.6,
                    ratingCount = 19,
                    isVerified = true,
                    cnicNumber = "37405-5555555-5",
                    currentLat = 33.6934,
                    currentLng = 73.0679
                ),
                WorkerEntity(
                    name = "Bilal Shah",
                    phone = "+923459876543",
                    category = "Carpenter",
                    location = "Rawalpindi",
                    rating = 4.5,
                    ratingCount = 8,
                    isVerified = false, // CNIC verification pending visual showcase
                    cnicNumber = null,
                    currentLat = 33.5984,
                    currentLng = 73.0441
                ),
                WorkerEntity(
                    name = "Nadeem Akhtar",
                    phone = "+923211122334",
                    category = "Painter",
                    location = "Faisalabad",
                    rating = 4.9,
                    ratingCount = 42,
                    isVerified = true,
                    cnicNumber = "33100-3333333-3",
                    currentLat = 31.4504,
                    currentLng = 73.1350
                ),
                WorkerEntity(
                    name = "Sajid Mahmood",
                    phone = "+923157766554",
                    category = "Welder",
                    location = "Peshawar",
                    rating = 4.4,
                    ratingCount = 11,
                    isVerified = true,
                    cnicNumber = "17301-4444444-4",
                    currentLat = 34.0151,
                    currentLng = 71.5249
                ),
                WorkerEntity(
                    name = "Imran Qureshi",
                    phone = "+923329988776",
                    category = "AC Technician",
                    location = "Lahore",
                    rating = 4.8,
                    ratingCount = 31,
                    isVerified = true,
                    cnicNumber = "35201-9999999-9",
                    currentLat = 31.5604,
                    currentLng = 74.3287
                ),
                WorkerEntity(
                    name = "Kashif Ali",
                    phone = "+923051412355",
                    category = "Electrician",
                    location = "Islamabad",
                    rating = 4.2,
                    ratingCount = 5,
                    isVerified = false,
                    cnicNumber = null,
                    currentLat = 33.7294,
                    currentLng = 73.0931
                )
            )

            for (worker in seedWorkers) {
                dao.insertWorker(worker)
            }

            // Seed initial wallet transactions showing dashboard usage history
            dao.insertWalletTransaction(
                WalletTransactionEntity(
                    amount = 1200.0,
                    type = "Deposit",
                    description = "Job completion - Plumbing Service",
                    referenceChannel = "Job Completion"
                )
            )
            dao.insertWalletTransaction(
                WalletTransactionEntity(
                    amount = 800.0,
                    type = "Deposit",
                    description = "Job completion - Switchboard Installation",
                    referenceChannel = "Job Completion"
                )
            )
            dao.insertWalletTransaction(
                WalletTransactionEntity(
                    amount = 1000.0,
                    type = "Withdrawal",
                    description = "Wallet payout transferred to Cash Account",
                    referenceChannel = "EasyPaisa"
                )
            )
        }
    }
}
