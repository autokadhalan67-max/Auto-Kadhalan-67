package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random

data class ActiveCall(
    val peerPhone: String,
    val peerName: String,
    val peerRole: String,
    val isIncoming: Boolean,
    val status: String, // "RINGING", "CONNECTED", "DISCONNECTED"
    val durationSeconds: Int = 0
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AppRepository
    
    init {
        val database = AppDatabase.getDatabase(application)
        repository = AppRepository(database)
        seedInitialDrivers()
    }

    // Auth States
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _registerStatus = MutableStateFlow<String?>(null)
    val registerStatus: StateFlow<String?> = _registerStatus.asStateFlow()

    // Map & Booking Flows
    private val _selectedPickup = MutableStateFlow<MapLocation?>(null)
    val selectedPickup: StateFlow<MapLocation?> = _selectedPickup.asStateFlow()

    private val _selectedDrop = MutableStateFlow<MapLocation?>(null)
    val selectedDrop: StateFlow<MapLocation?> = _selectedDrop.asStateFlow()

    private val _currentRide = MutableStateFlow<Ride?>(null)
    val currentRide: StateFlow<Ride?> = _currentRide.asStateFlow()

    // Simulated real tracking navigation state
    private val _navProgress = MutableStateFlow(0.0f) // 0.0 to 1.0
    val navProgress: StateFlow<Float> = _navProgress.asStateFlow()

    private val _autoX = MutableStateFlow(0.5f)
    val autoX: StateFlow<Float> = _autoX.asStateFlow()

    private val _autoY = MutableStateFlow(0.45f)
    val autoY: StateFlow<Float> = _autoY.asStateFlow()

    private val _navInstructions = MutableStateFlow("Select pickup and drop location inside Aruppukottai Taluk")
    val navInstructions: StateFlow<String> = _navInstructions.asStateFlow()

    private val _simulatedSpeed = MutableStateFlow(0) // km/h
    val simulatedSpeed: StateFlow<Int> = _simulatedSpeed.asStateFlow()

    private val _simulatedEta = MutableStateFlow(0) // minutes
    val simulatedEta: StateFlow<Int> = _simulatedEta.asStateFlow()

    // Combined/Reactive Ride Flows
    val pendingRides: StateFlow<List<Ride>> = repository.getPendingRides()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customerRides: StateFlow<List<Ride>> = _currentUser
        .filterNotNull()
        .filter { it.role == "customer" }
        .flatMapLatest { repository.getRidesForCustomer(it.phone) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val driverRides: StateFlow<List<Ride>> = _currentUser
        .filterNotNull()
        .filter { it.role == "driver" }
        .flatMapLatest { repository.getRidesForDriver(it.phone) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDrivers: StateFlow<List<User>> = repository.getDriversFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Call System
    private val _activeCall = MutableStateFlow<ActiveCall?>(null)
    val activeCall: StateFlow<ActiveCall?> = _activeCall.asStateFlow()

    private val _callLogsList = MutableStateFlow<List<CallLog>>(emptyList())
    val callLogsList: StateFlow<List<CallLog>> = _callLogsList.asStateFlow()

    private var navigationJob: Job? = null
    private var callTimerJob: Job? = null

    init {
        // Observe call logs reactively once a user is logged in
        viewModelScope.launch {
            _currentUser.collect { user ->
                if (user != null) {
                    repository.getCallLogs(user.phone).collect { logs ->
                        _callLogsList.value = logs
                    }
                } else {
                    _callLogsList.value = emptyList()
                }
            }
        }
    }

    private fun seedInitialDrivers() {
        viewModelScope.launch {
            // Seed a default customer and three core drivers of Auto Kadhalan 67
            val firstDriver = repository.getUserByPhone("9876543210")
            if (firstDriver == null) {
                repository.insertUser(User("9876543210", "Karthik (Auto Kadhalan 67)", "driver", "6700", "TN-67-A-6700", 4.9f, 42, 1250.0))
                repository.insertUser(User("9876543211", "Senthil Kumar", "driver", "1234", "TN-67-B-4321", 4.7f, 25, 870.0))
                repository.insertUser(User("9876543212", "Muthu G", "driver", "1111", "TN-67-AP-9090", 4.5f, 16, 500.0))
                repository.insertUser(User("8888888888", "Aruppukottai Rider", "customer", "1234", "", 4.8f, 0, 400.0))
            }
        }
    }

    fun selectPickup(location: MapLocation) {
        _selectedPickup.value = location
        // Auto-center map indicator to location
        _autoX.value = location.x
        _autoY.value = location.y
        _navInstructions.value = "Selected pickup: ${location.name}. Now choose where you want to drop."
    }

    fun selectDrop(location: MapLocation) {
        _selectedDrop.value = location
        val pickup = _selectedPickup.value
        if (pickup != null) {
            val dist = AruppukottaiMapData.calculateDistanceKm(pickup, location)
            val fare = AruppukottaiMapData.calculateFare(pickup, location)
            _navInstructions.value = "Route ready. ${String.format("%.2f", dist)} Km. Fare: Rs. ${String.format("%.1f", fare)}. Book your ride now!"
        }
    }

    // Login
    fun login(phone: String, pin: String, role: String) {
        viewModelScope.launch {
            _loginError.value = null
            if (phone.isBlank() || pin.isBlank()) {
                _loginError.value = "Phone and PIN are required."
                return@launch
            }
            val user = repository.getUserByPhone(phone)
            if (user == null) {
                _loginError.value = "User not registered in database."
            } else if (user.pin != pin) {
                _loginError.value = "Invalid PIN."
            } else if (user.role != role) {
                _loginError.value = "Account is registered as ${user.role}, not $role."
            } else {
                _currentUser.value = user
                _loginError.value = null
            }
        }
    }

    // Register
    fun register(phone: String, name: String, pin: String, role: String, vehicleNumber: String) {
        viewModelScope.launch {
            _registerStatus.value = null
            if (phone.isBlank() || name.isBlank() || pin.isBlank()) {
                _registerStatus.value = "All fields except vehicle are required."
                return@launch
            }
            if (role == "driver" && vehicleNumber.isBlank()) {
                _registerStatus.value = "Drivers must provide vehicle registration number."
                return@launch
            }
            val existing = repository.getUserByPhone(phone)
            if (existing != null) {
                _registerStatus.value = "Phone number already registered."
                return@launch
            }

            val newUser = User(
                phone = phone,
                name = name,
                role = role,
                pin = pin,
                vehicleNumber = if (role == "driver") vehicleNumber else "",
                rating = 4.8f,
                ratingsCount = 1,
                balance = if (role == "customer") 500.0 else 0.0 // Starting balances
            )
            repository.insertUser(newUser)
            _currentUser.value = newUser
            _registerStatus.value = "SUCCESS"
        }
    }

    fun logout() {
        _currentUser.value = null
        _currentRide.value = null
        _selectedPickup.value = null
        _selectedDrop.value = null
        cancelNavigationSimulation()
    }

    // Customer Side Booking Request
    fun bookRide(paymentMethod: String) {
        val customer = _currentUser.value ?: return
        val pickup = _selectedPickup.value ?: return
        val drop = _selectedDrop.value ?: return
        val fare = AruppukottaiMapData.calculateFare(pickup, drop)

        viewModelScope.launch {
            if (paymentMethod == "Wallet" && customer.balance < fare) {
                _navInstructions.value = "Insufficient wallet balance (Max: Rs. ${customer.balance}). Please choose direct Cash or UPI."
                return@launch
            }

            val newRide = Ride(
                customerPhone = customer.phone,
                customerName = customer.name,
                pickupName = pickup.name,
                dropName = drop.name,
                status = "PENDING",
                fare = fare,
                paymentMethod = paymentMethod,
                paymentStatus = "PENDING"
            )
            
            val generatedId = repository.createRide(newRide)
            val savedRide = newRide.copy(id = generatedId.toInt())
            _currentRide.value = savedRide
            _navInstructions.value = "Locating Auto Kadhalan drivers in Aruppukottai Taluk..."

            // Start automated AI Driver pickup match if no human driver reacts in 4 seconds
            triggerAiDriverMatching(savedRide)
        }
    }

    private fun triggerAiDriverMatching(ride: Ride) {
        viewModelScope.launch {
            delay(4000)
            // Verify if the ride has already been accepted by a human driver
            val queriedRide = repository.getRideById(ride.id)
            if (queriedRide != null && queriedRide.status == "PENDING") {
                // Pick a legendary mock driver to drive
                val randomDrivers = listOf(
                    Triple("9876543210", "Karthik (Auto Kadhalan 67)", "TN-67-A-6700"),
                    Triple("9876543211", "Senthil Kumar", "TN-67-B-4321"),
                    Triple("9876543212", "Muthu G", "TN-67-AP-9090")
                )
                val selected = randomDrivers.random()
                
                val updatedRide = queriedRide.copy(
                    driverPhone = selected.first,
                    driverName = selected.second,
                    status = "ACCEPTED"
                )
                repository.updateRide(updatedRide)
                _currentRide.value = updatedRide
                
                // Keep the active session in sync
                _navInstructions.value = "Accepted by ${selected.second}! Auto is arriving at ${ride.pickupName}..."
                
                // Transition automatically to ARRIVED after 4 seconds
                delay(4000)
                updateSimulatedRideStatus(updatedRide.id, "ARRIVED")
            }
        }
    }

    // Driver Side Handlers
    fun acceptRideRequest(ride: Ride) {
        val driver = _currentUser.value ?: return
        if (driver.role != "driver") return
        
        viewModelScope.launch {
            val updated = ride.copy(
                driverPhone = driver.phone,
                driverName = driver.name,
                status = "ACCEPTED"
            )
            repository.updateRide(updated)
            _currentRide.value = updated
            _navInstructions.value = "You accepted the ride. Head towards ${ride.pickupName}!"
        }
    }

    fun driverMarkArrived() {
        val ride = _currentRide.value ?: return
        viewModelScope.launch {
            updateSimulatedRideStatus(ride.id, "ARRIVED")
        }
    }

    fun driverStartTrip() {
        val ride = _currentRide.value ?: return
        viewModelScope.launch {
            updateSimulatedRideStatus(ride.id, "TRIP_STARTED")
        }
    }

    fun driverCompleteTrip() {
        val ride = _currentRide.value ?: return
        viewModelScope.launch {
            updateSimulatedRideStatus(ride.id, "COMPLETED")
        }
    }

    fun cancelRide() {
        val ride = _currentRide.value ?: return
        viewModelScope.launch {
            repository.updateRideStatus(ride.id, "CANCELLED")
            _currentRide.value = null
            _navInstructions.value = "Ride cancelled. Select locations to search again."
            cancelNavigationSimulation()
        }
    }

    private suspend fun updateSimulatedRideStatus(rideId: Int, newStatus: String) {
        val current = repository.getRideById(rideId) ?: return
        val updated = current.copy(status = newStatus)
        repository.updateRide(updated)
        _currentRide.value = updated

        when (newStatus) {
            "ARRIVED" -> {
                _navInstructions.value = "Your Auto has arrived! Please board your vehicle."
                _autoX.value = getCoordinatesByName(updated.pickupName).first
                _autoY.value = getCoordinatesByName(updated.pickupName).second
                _simulatedSpeed.value = 0
            }
            "TRIP_STARTED" -> {
                _navInstructions.value = "Trip started! Navigating along Aruppukottai Roads."
                // Launch GPS mapping system interpolation logic
                startNavigationSimulation(updated)
            }
            "COMPLETED" -> {
                // If payment method is wallet, transfer balance immediately
                if (updated.paymentMethod == "Wallet" && updated.paymentStatus == "PENDING") {
                    transferWalletSimulated(updated)
                }
                _navInstructions.value = "Trip Completed. Thank you for riding with Auto Kadhalan 67!"
                cancelNavigationSimulation()
                _simulatedSpeed.value = 0
            }
        }
    }

    private suspend fun transferWalletSimulated(ride: Ride) {
        val customer = repository.getUserByPhone(ride.customerPhone)
        val driver = repository.getUserByPhone(ride.driverPhone)
        
        if (customer != null && driver != null) {
            val newCustomerBalance = customer.balance - ride.fare
            val newDriverBalance = driver.balance + ride.fare
            
            repository.updateUserBalance(customer.phone, newCustomerBalance)
            repository.updateUserBalance(driver.phone, newDriverBalance)
            
            val updatedRide = ride.copy(paymentStatus = "PAID")
            repository.updateRide(updatedRide)
            _currentRide.value = updatedRide
            
            // Sync local cached profile values
            if (_currentUser.value?.phone == customer.phone) {
                _currentUser.value = customer.copy(balance = newCustomerBalance)
            } else if (_currentUser.value?.phone == driver.phone) {
                _currentUser.value = driver.copy(balance = newDriverBalance)
            }
        }
    }

    // Process secure simulated direct card / UPI payment gates
    fun processGatewayPayment(rideId: Int, gatewayName: String, extraDetails: String) {
        viewModelScope.launch {
            val ride = repository.getRideById(rideId)
            if (ride != null) {
                val updatedRide = ride.copy(paymentStatus = "PAID")
                repository.updateRide(updatedRide)
                _currentRide.value = updatedRide
                
                // Add actual credit to Driver's wallet in all methods
                if (ride.driverPhone.isNotEmpty()) {
                    val driver = repository.getUserByPhone(ride.driverPhone)
                    if (driver != null) {
                        repository.updateUserBalance(driver.phone, driver.balance + ride.fare)
                    }
                }
                
                _navInstructions.value = "Payment of Rs. ${String.format("%.1f", ride.fare)} received securely via $gatewayName."
            }
        }
    }

    // Stars & Comments Rating
    fun submitDriverRating(ratingValue: Float, comment: String) {
        val ride = _currentRide.value ?: return
        viewModelScope.launch {
            repository.rateRide(ride.id, ratingValue, comment)
            val updated = ride.copy(driverRating = ratingValue, feedback = comment)
            _currentRide.value = updated
            
            // Clean active panel so customer can register a new ride request
            delay(1500)
            _currentRide.value = null
            _selectedPickup.value = null
            _selectedDrop.value = null
            _navInstructions.value = "Your feedback has been logged. Book another ride!"
        }
    }

    // Simulation Engine coordinates running
    private fun startNavigationSimulation(ride: Ride) {
        cancelNavigationSimulation()
        navigationJob = viewModelScope.launch {
            val pickupPos = getCoordinatesByName(ride.pickupName)
            val dropPos = getCoordinatesByName(ride.dropName)
            
            val durationSeconds = 12
            for (step in 1..durationSeconds) {
                delay(1000)
                val progress = step.toFloat() / durationSeconds.toFloat()
                _navProgress.value = progress

                // Interpolated Position with some light jittering to simulate actual traffic coordinates
                val jitterX = (Random.nextFloat() - 0.5f) * 0.02f
                val jitterY = (Random.nextFloat() - 0.5f) * 0.02f
                
                _autoX.value = pickupPos.first + (dropPos.first - pickupPos.first) * progress + jitterX
                _autoY.value = pickupPos.second + (dropPos.second - pickupPos.second) * progress + jitterY
                
                // Dynamic instructions
                _navInstructions.value = when {
                    progress < 0.25f -> "Leaving ${ride.pickupName}, boarding Aruppukottai Main Road."
                    progress < 0.50f -> "Approaching Aruppukottai bypass. Navigating at 45 km/h."
                    progress < 0.75f -> "Cruising through beautiful Aruppukottai countryside. Enjoy your ride!"
                    else -> "Arriving shortly at ${ride.dropName}. Preparing receipt cards."
                }
                
                _simulatedSpeed.value = 35 + Random.nextInt(15) // 35 to 50 km/h
                _simulatedEta.value = kotlin.math.max(1, ((durationSeconds - step) * 0.4).toInt())
            }
            
            // When simulator arrives: complete ride
            updateSimulatedRideStatus(ride.id, "COMPLETED")
        }
    }

    private fun cancelNavigationSimulation() {
        navigationJob?.cancel()
        navigationJob = null
        _navProgress.value = 0f
        _simulatedSpeed.value = 0
        _simulatedEta.value = 0
    }

    private fun getCoordinatesByName(name: String): Pair<Float, Float> {
        val match = AruppukottaiMapData.locations.firstOrNull { it.name == name }
        return if (match != null) Pair(match.x, match.y) else Pair(0.5f, 0.5f)
    }

    // Call Log Simulation
    fun initiatePhoneCall(receiverPhone: String, receiverName: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val call = ActiveCall(
                peerPhone = receiverPhone,
                peerName = receiverName,
                peerRole = if (user.role == "customer") "Driver" else "Customer",
                isIncoming = false,
                status = "RINGING"
            )
            _activeCall.value = call
            
            // Simple call sequence logic: Accept call automatically after 2.5 seconds
            delay(2500)
            connectPhoneCall()
        }
    }

    fun receiveIncomingCallSimulated(callerPhone: String, callerName: String) {
        val user = _currentUser.value ?: return
        val call = ActiveCall(
            peerPhone = callerPhone,
            peerName = callerName,
            peerRole = if (user.role == "customer") "Driver" else "Customer",
            isIncoming = true,
            status = "RINGING"
        )
        _activeCall.value = call
    }

    fun connectPhoneCall() {
        val current = _activeCall.value ?: return
        if (current.status == "CONNECTED") return
        
        _activeCall.value = current.copy(status = "CONNECTED")
        
        // Start duration ticking
        callTimerJob?.cancel()
        callTimerJob = viewModelScope.launch {
            var seconds = 0
            while (true) {
                delay(1000)
                seconds++
                val callState = _activeCall.value
                if (callState != null && callState.status == "CONNECTED") {
                    _activeCall.value = callState.copy(durationSeconds = seconds)
                } else {
                    break
                }
            }
        }
    }

    fun hangUpPhoneCall() {
        val call = _activeCall.value ?: return
        val user = _currentUser.value ?: return
        
        callTimerJob?.cancel()
        callTimerJob = null
        
        viewModelScope.launch {
            _activeCall.value = call.copy(status = "DISCONNECTED")
            
            // Insert call logs record to Room!
            val log = CallLog(
                fromPhone = if (call.isIncoming) call.peerPhone else user.phone,
                fromName = if (call.isIncoming) call.peerName else user.name,
                toPhone = if (call.isIncoming) user.phone else call.peerPhone,
                toName = if (call.isIncoming) user.name else call.peerName,
                durationSeconds = call.durationSeconds
            )
            repository.insertCallLog(log)
            
            delay(1500)
            _activeCall.value = null
        }
    }

    fun triggerQuickDeposit(amount: Double) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val updatedBalance = user.balance + amount
            repository.updateUserBalance(user.phone, updatedBalance)
            _currentUser.value = user.copy(balance = updatedBalance)
        }
    }

    // Add rating metrics computations
    fun getRatingMetricsForUser(): String {
        val user = _currentUser.value ?: return "Rating: 4.8★"
        return if (user.role == "driver") {
            "★ ${String.format("%.1f", user.rating)} (${user.ratingsCount} reviews)"
        } else {
            "Rating: 4.9★ (Gold Customer)"
        }
    }
}
