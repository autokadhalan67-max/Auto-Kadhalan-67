package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE phone = :phone LIMIT 1")
    suspend fun getUserByPhone(phone: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Update
    suspend fun updateUser(user: User)

    @Query("SELECT * FROM users WHERE role = 'driver' ORDER BY rating DESC")
    fun getDriversFlow(): Flow<List<User>>

    @Query("UPDATE users SET balance = :newBalance WHERE phone = :phone")
    suspend fun updateUserBalance(phone: String, newBalance: Double)

    @Query("UPDATE users SET rating = :newRating, ratingsCount = :count WHERE phone = :phone")
    suspend fun updateDriverRating(phone: String, newRating: Float, count: Int)
}

@Dao
interface RideDao {
    @Query("SELECT * FROM rides WHERE customerPhone = :customerPhone ORDER BY timestamp DESC")
    fun getRidesForCustomer(customerPhone: String): Flow<List<Ride>>

    @Query("SELECT * FROM rides WHERE driverPhone = :driverPhone ORDER BY timestamp DESC")
    fun getRidesForDriver(driverPhone: String): Flow<List<Ride>>

    @Query("SELECT * FROM rides WHERE status = 'PENDING' ORDER BY timestamp DESC")
    fun getPendingRides(): Flow<List<Ride>>

    @Query("SELECT * FROM rides WHERE id = :rideId LIMIT 1")
    suspend fun getRideById(rideId: Int): Ride?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRide(ride: Ride): Long

    @Update
    suspend fun updateRide(ride: Ride)

    @Query("UPDATE rides SET status = :status WHERE id = :id")
    suspend fun updateRideStatus(id: Int, status: String)

    @Query("UPDATE rides SET driverRating = :rating, feedback = :feedback WHERE id = :id")
    suspend fun rateRide(id: Int, rating: Float, feedback: String)
}

@Dao
interface CallLogDao {
    @Query("SELECT * FROM call_logs WHERE fromPhone = :phone OR toPhone = :phone ORDER BY timestamp DESC")
    fun getCallLogs(phone: String): Flow<List<CallLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallLog(callLog: CallLog)
}

@Database(entities = [User::class, Ride::class, CallLog::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun rideDao(): RideDao
    abstract fun callLogDao(): CallLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "auto_kadhalan_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
