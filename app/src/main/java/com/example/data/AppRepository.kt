package com.example.data

import kotlinx.coroutines.flow.Flow

class AppRepository(private val db: AppDatabase) {
    private val userDao = db.userDao()
    private val rideDao = db.rideDao()
    private val callLogDao = db.callLogDao()

    // Users
    suspend fun getUserByPhone(phone: String): User? = userDao.getUserByPhone(phone)
    suspend fun insertUser(user: User) = userDao.insertUser(user)
    suspend fun updateUser(user: User) = userDao.updateUser(user)
    fun getDriversFlow(): Flow<List<User>> = userDao.getDriversFlow()
    suspend fun updateUserBalance(phone: String, newBalance: Double) = userDao.updateUserBalance(phone, newBalance)
    suspend fun updateDriverRating(phone: String, rating: Float, ratingCount: Int) = userDao.updateDriverRating(phone, rating, ratingCount)

    // Rides
    fun getRidesForCustomer(customerPhone: String): Flow<List<Ride>> = rideDao.getRidesForCustomer(customerPhone)
    fun getRidesForDriver(driverPhone: String): Flow<List<Ride>> = rideDao.getRidesForDriver(driverPhone)
    fun getPendingRides(): Flow<List<Ride>> = rideDao.getPendingRides()
    suspend fun getRideById(rideId: Int): Ride? = rideDao.getRideById(rideId)
    suspend fun createRide(ride: Ride): Long = rideDao.insertRide(ride)
    suspend fun updateRide(ride: Ride) = rideDao.updateRide(ride)
    suspend fun updateRideStatus(id: Int, status: String) = rideDao.updateRideStatus(id, status)
    suspend fun rateRide(id: Int, rating: Float, feedback: String) {
        rideDao.rateRide(id, rating, feedback)
        
        // Let's recalculate and update driver overall rating
        val ride = rideDao.getRideById(id)
        if (ride != null && ride.driverPhone.isNotEmpty()) {
            val driver = userDao.getUserByPhone(ride.driverPhone)
            if (driver != null) {
                val newCount = driver.ratingsCount + 1
                val totalRatingSum = (driver.rating * driver.ratingsCount) + rating
                val newAvgRating = totalRatingSum / newCount
                userDao.updateDriverRating(ride.driverPhone, newAvgRating, newCount)
            }
        }
    }

    // Call Logs
    fun getCallLogs(phone: String): Flow<List<CallLog>> = callLogDao.getCallLogs(phone)
    suspend fun insertCallLog(callLog: CallLog) = callLogDao.insertCallLog(callLog)
}
