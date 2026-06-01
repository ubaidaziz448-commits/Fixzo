package com.example.data.dao

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FixzoDao {

    // Workers queries
    @Query("SELECT * FROM workers ORDER BY rating DESC")
    fun getAllWorkers(): Flow<List<WorkerEntity>>

    @Query("SELECT * FROM workers WHERE id = :id LIMIT 1")
    suspend fun getWorkerById(id: Int): WorkerEntity?

    @Query("SELECT * FROM workers WHERE category = :category AND location = :location")
    fun getWorkersByCategoryAndLocation(category: String, location: String): Flow<List<WorkerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorker(worker: WorkerEntity): Long

    @Update
    suspend fun updateWorker(worker: WorkerEntity)

    @Query("SELECT COUNT(*) FROM workers")
    suspend fun getWorkersCount(): Int

    // Bookings queries
    @Query("SELECT * FROM bookings ORDER BY timestamp DESC")
    fun getAllBookings(): Flow<List<BookingEntity>>

    @Query("SELECT * FROM bookings WHERE id = :id LIMIT 1")
    suspend fun getBookingById(id: Int): BookingEntity?

    @Query("SELECT * FROM bookings WHERE status = 'In_Progress' OR status = 'Accepted' LIMIT 1")
    fun getActiveBooking(): Flow<BookingEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooking(booking: BookingEntity): Long

    @Update
    suspend fun updateBooking(booking: BookingEntity)

    // Chat queries
    @Query("SELECT * FROM chat_messages WHERE bookingId = :bookingId ORDER BY timestamp ASC")
    fun getMessagesForBooking(bookingId: Int): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessageEntity): Long

    // Wallet transaction queries
    @Query("SELECT * FROM wallet_transactions ORDER BY timestamp DESC")
    fun getWalletTransactions(): Flow<List<WalletTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWalletTransaction(transaction: WalletTransactionEntity): Long
}
