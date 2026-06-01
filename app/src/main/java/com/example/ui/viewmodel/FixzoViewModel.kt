package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.*
import com.example.data.repository.FixzoRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class Language {
    EN, UR
}

sealed class Screen {
    object Welcome : Screen()
    object VerificationSim : Screen()
    object CustomerDashboard : Screen()
    object WorkerDashboard : Screen()
    data class ChatScreen(val bookingId: Int) : Screen()
    object CostCalculatorScreen : Screen()
    object EmergencyPortalScreen : Screen()
}

class FixzoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FixzoRepository
    
    // UI Navigation backstack state (Zero-dependency custom stack)
    private val _navigationStack = MutableStateFlow<List<Screen>>(listOf(Screen.Welcome))
    val currentScreen: StateFlow<Screen> = _navigationStack
        .map { it.lastOrNull() ?: Screen.Welcome }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Screen.Welcome)

    // Language setting state (English vs Urdu)
    private val _language = MutableStateFlow(Language.EN)
    val language: StateFlow<Language> = _language.asStateFlow()

    // Active User Context state
    val isWorkerMode = MutableStateFlow(false)
    val registeredName = MutableStateFlow("")
    val registeredPhone = MutableStateFlow("")
    val registeredCity = MutableStateFlow("Islamabad")
    val registeredCategory = MutableStateFlow("Plumber")
    
    // Registration helper states
    val regNameInput = MutableStateFlow("")
    val regPhoneInput = MutableStateFlow("")
    val regCityInput = MutableStateFlow("Islamabad")
    val regCategoryInput = MutableStateFlow("Plumber")
    val smsVerificationCode = MutableStateFlow("")
    val smsSentTicks = MutableStateFlow(0)
    val generatedOtp = MutableStateFlow("1234")
    val showSmsNotification = MutableStateFlow(false)
    private var countdownJob: Job? = null

    // Search state
    val searchQuery = MutableStateFlow("")
    val selectedCategoryFilter = MutableStateFlow("All")
    val selectedCityFilter = MutableStateFlow("Islamabad")

    // Database observers
    val allWorkers: StateFlow<List<WorkerEntity>>
    val activeBooking: StateFlow<BookingEntity?>
    val walletTransactions: StateFlow<List<WalletTransactionEntity>>
    val allBookings: StateFlow<List<BookingEntity>>

    // Calculated fields based on filters
    private val _filteredWorkers = MutableStateFlow<List<WorkerEntity>>(emptyList())
    val filteredWorkers: StateFlow<List<WorkerEntity>> = _filteredWorkers.asStateFlow()

    // Emergency broadcasting states
    val isEmergencyActive = MutableStateFlow(false)
    val emergencyStage = MutableStateFlow("") // "idle", "broadcasting", "responded"
    val emergencyLog = MutableStateFlow<List<String>>(emptyList())

    // Location components (Mock live map tracking parameters)
    val workerLat = MutableStateFlow(33.6844)
    val workerLng = MutableStateFlow(73.0479)
    val customerLat = MutableStateFlow(33.6934)
    val customerLng = MutableStateFlow(73.0679)
    val isWorkerMoving = MutableStateFlow(false)
    private var movementJob: Job? = null
    private var currentSimulatedBookingId: Int? = null

    // Wallet balances (recalculated from transactions)
    val walletBalance = MutableStateFlow(1000.0) // Initial cash seed

    // CNIC input state
    val cnicInput = MutableStateFlow("")
    val isCNICVerified = MutableStateFlow(false)

    init {
        val database = AppDatabase.getDatabase(application)
        repository = FixzoRepository(database.fixzoDao())
        
        allWorkers = repository.allWorkers.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
        activeBooking = repository.activeBooking.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), null
        )
        walletTransactions = repository.walletTransactions.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
        allBookings = repository.allBookings.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )

        // Prepopulate database with dummy workers if empty
        viewModelScope.launch {
            repository.seedDatabaseIfEmpty()
        }

        // Recalculate wallet balance on transactions updates
        viewModelScope.launch {
            walletTransactions.collect { transactions ->
                var bal = 1000.0 // Base starting wallet amount
                for (t in transactions) {
                    if (t.type == "Deposit") {
                        bal += t.amount
                    } else {
                        bal -= t.amount
                    }
                }
                walletBalance.value = bal
            }
        }

        // Combined filtering logic for workers results
        viewModelScope.launch {
            combine(allWorkers, searchQuery, selectedCategoryFilter, selectedCityFilter) { list, search, cat, city ->
                list.filter { worker ->
                    val matchCity = worker.location.equals(city, ignoreCase = true)
                    val matchCategory = if (cat == "All") {
                        true
                    } else {
                        worker.category.equals(cat, ignoreCase = true)
                    }
                    val matchSearch = if (search.isEmpty()) {
                        true
                    } else {
                        worker.name.contains(search, ignoreCase = true) || 
                        worker.category.contains(search, ignoreCase = true)
                    }
                    matchCity && matchCategory && matchSearch
                }
            }.collect {
                _filteredWorkers.value = it
            }
        }

        // Trigger coordinate animation when active booking transitions to Accepted
        viewModelScope.launch {
            activeBooking.collect { booking ->
                if (booking != null && (booking.status == "Accepted" || booking.status == "In_Progress")) {
                    startLocationSimulation(booking.id)
                } else {
                    stopLocationSimulation()
                }
            }
        }
    }

    // Toggle app language between Urdu / English
    fun toggleLanguage() {
        _language.value = if (_language.value == Language.EN) Language.UR else Language.EN
    }

    // Navigation triggers
    fun navigateTo(screen: Screen) {
        val current = _navigationStack.value.toMutableList()
        current.add(screen)
        _navigationStack.value = current
    }

    fun navigateBack() {
        val current = _navigationStack.value.toMutableList()
        if (current.size > 1) {
            current.removeAt(current.size - 1)
            _navigationStack.value = current
        }
    }

    // Pakistan Phone validation helper
    fun isValidPakistanPhone(phone: String): Boolean {
        val cleaned = phone.replace(Regex("[\\s\\-\\(\\)]"), "")
        val pattern = Regex("^(?:\\+92|92)?(?:0)?(?:3[0-9]{9})$")
        return cleaned.matches(pattern)
    }

    // Registration and Verification Flow
    fun startRegistration(isWorker: Boolean) {
        isWorkerMode.value = isWorker
        val otp = (1000..9999).random().toString()
        generatedOtp.value = otp
        smsVerificationCode.value = ""
        smsSentTicks.value = 60
        showSmsNotification.value = true
        
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            // Dismiss simulation notification banner automatically after 10 seconds
            launch {
                delay(10000)
                showSmsNotification.value = false
            }
            while (smsSentTicks.value > 0) {
                delay(1000)
                smsSentTicks.value -= 1
            }
        }
        navigateTo(Screen.VerificationSim)
    }

    fun resendSmsCode() {
        val otp = (1000..9999).random().toString()
        generatedOtp.value = otp
        smsSentTicks.value = 60
        showSmsNotification.value = true
        
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            launch {
                delay(10000)
                showSmsNotification.value = false
            }
            while (smsSentTicks.value > 0) {
                delay(1000)
                smsSentTicks.value -= 1
            }
        }
    }

    fun verifySMSCode(code: String): Boolean {
        // Accepts the newly generated random OTP, or "1234" as back-up diagnostics override
        if (code == generatedOtp.value || code == "1234") {
            countdownJob?.cancel()
            showSmsNotification.value = false
            // Save registered data
            registeredName.value = regNameInput.value
            registeredPhone.value = regPhoneInput.value
            registeredCity.value = regCityInput.value
            registeredCategory.value = regCategoryInput.value

            viewModelScope.launch {
                if (isWorkerMode.value) {
                    val newWorker = WorkerEntity(
                        name = registeredName.value,
                        phone = registeredPhone.value,
                        category = registeredCategory.value,
                        location = registeredCity.value,
                        rating = 5.0,
                        ratingCount = 1,
                        isVerified = isCNICVerified.value,
                        cnicNumber = if (isCNICVerified.value) "37405-0000000-1" else null
                    )
                    repository.registerWorker(newWorker)
                    navigateTo(Screen.WorkerDashboard)
                } else {
                    navigateTo(Screen.CustomerDashboard)
                }
            }
            return true
        }
        return false
    }

    // Direct Interaction - Place custom booking order immediately
    fun bookWorker(worker: WorkerEntity, estimateAmount: Double = 500.0, paymentOpt: String = "Cash") {
        viewModelScope.launch {
            val booking = BookingEntity(
                workerId = worker.id,
                workerName = worker.name,
                workerCategory = worker.category,
                customerName = if (registeredName.value.isEmpty()) "Customer (You)" else registeredName.value,
                customerPhone = if (registeredPhone.value.isEmpty()) "+923000000000" else registeredPhone.value,
                status = "Pending",
                paymentMethod = paymentOpt,
                amount = estimateAmount
            )
            val bookingId = repository.createBooking(booking)
            
            // Post first greeting chat message automatically
            repository.sendChatMessage(
                ChatMessageEntity(
                    bookingId = bookingId.toInt(),
                    sender = "Customer",
                    message = if (_language.value == Language.EN) {
                        "Hello ${worker.name}, I need a ${worker.category} at my location. Booking has been placed."
                    } else {
                        "ہیلو ${worker.name}، مجھے اپنے پتے پر ${worker.category} کی ضرورت ہے۔ بکنگ کر دی گئی ہے۔"
                    }
                )
            )

            // Dynamic accept sequence
            delay(2000)
            val created = repository.getBookingById(bookingId.toInt())
            if (created != null) {
                repository.updateBooking(created.copy(status = "Accepted"))
                // Worker chat reply
                repository.sendChatMessage(
                    ChatMessageEntity(
                        bookingId = bookingId.toInt(),
                        sender = "Worker",
                        message = if (_language.value == Language.EN) {
                            "Assalam-o-Alaikum! Request accepted. I am preparing my tools and heading to your location. Keep an eye on the simulation map."
                        } else {
                            "السلام علیکم! درخواست قبول کر لی گئی ہے۔ میں اپنے اوزار تیار کر کے آپ کے پتے کی طرف روانہ ہو رہا ہوں۔ نقشہ دیکھیں۔"
                        }
                    )
                )
            }
        }
    }

    // Modify active booking status
    fun updateActiveBookingStatus(status: String) {
        viewModelScope.launch {
            val active = activeBooking.value
            if (active != null) {
                val updated = active.copy(status = status)
                repository.updateBooking(updated)

                if (status == "In_Progress") {
                    repository.sendChatMessage(
                        ChatMessageEntity(
                            bookingId = active.id,
                            sender = "Worker",
                            message = if (_language.value == Language.EN) {
                                "I have arrived! Starting the work now."
                            } else {
                                "میں پہنچ گیا ہوں! اب کام شروع کر رہا ہوں۔"
                            }
                        )
                    )
                } else if (status == "Completed") {
                    // Credit to Worker wallet if digital option
                    if (active.paymentMethod != "Cash") {
                        repository.addWalletTransaction(
                            WalletTransactionEntity(
                                amount = active.amount,
                                type = "Deposit",
                                description = "Earning from Job #${active.id} (Paid via ${active.paymentMethod})",
                                referenceChannel = "Job Completion"
                            )
                        )
                    }
                    repository.sendChatMessage(
                        ChatMessageEntity(
                            bookingId = active.id,
                            sender = "Worker",
                            message = if (_language.value == Language.EN) {
                                "Work finished! Total amount is PKR ${active.amount}. Please rate my service."
                            } else {
                                "کام مکمل ہو گیا ہے! کل رقم ${active.amount} روپے ہے۔ برائے مہربانی میرے کام کی ریٹنگ کریں۔"
                            }
                        )
                    )
                }
            }
        }
    }

    fun submitWorkerRating(rating: Float) {
        viewModelScope.launch {
            val active = activeBooking.value
            if (active != null) {
                val worker = repository.getWorkerById(active.workerId)
                if (worker != null) {
                    val newCount = worker.ratingCount + 1
                    val newRating = ((worker.rating * worker.ratingCount) + rating) / newCount
                    repository.updateWorker(worker.copy(rating = newRating, ratingCount = newCount))
                }
                // Update booking to record submission
                repository.updateBooking(active.copy(ratingSubmitted = true, status = "Completed"))
            }
        }
    }

    // In-app conversational chat submit (with simulated immediate responses)
    fun sendChatMessage(bookingId: Int, content: String) {
        if (content.trim().isEmpty()) return
        viewModelScope.launch {
            val chatMsg = ChatMessageEntity(
                bookingId = bookingId,
                sender = "Customer",
                message = content
            )
            repository.sendChatMessage(chatMsg)

            // Interactive Bot/Worker simulation response after delay
            delay(1500)
            val lower = content.lowercase()
            val responseText = if (_language.value == Language.EN) {
                when {
                    lower.contains("hello") || lower.contains("hi") || lower.contains("aoa") || lower.contains("salam") -> {
                        "Walaikum Assalam! Yes, I am on my way to help. What is the specific job detail?"
                    }
                    lower.contains("price") || lower.contains("rate") || lower.contains("pk") || lower.contains("charge") -> {
                        "Yes, the estimated charge is fixed as shown in the cost calculator recipe. No extra charges!"
                    }
                    lower.contains("time") || lower.contains("kab") || lower.contains("delay") || lower.contains("wait") -> {
                        "Reaching soon! I am riding my motorcycle. Kindly check my movement progress indicator on the dashboard map."
                    }
                    else -> {
                        "Understood. I will do this perfectly for you. Reaching shortly."
                    }
                }
            } else {
                when {
                    lower.contains("hello") || lower.contains("hi") || lower.contains("aoa") || lower.contains("salam") -> {
                        "وعلیکم السلام! جی میں آپ کی مدد کے لیے آ رہا ہوں۔ کیا کام کرنا ہے تفصیل بتائیں؟"
                    }
                    lower.contains("price") || lower.contains("rate") || lower.contains("pk") || lower.contains("charge") -> {
                        "قیمت بالکل مناسب اور فکسڈ ہے۔ جو کیلکولیٹر میں بنی ہے وہی چارجز ہوں گے۔ کوئی فالتو پیسے نہیں۔"
                    }
                    lower.contains("time") || lower.contains("kab") || lower.contains("delay") || lower.contains("wait") -> {
                        "میں موٹر سائیکل پر آرہا ہوں، بہت جلد پہنچ جاؤں گا۔ آپ لائیو نقشہ دیکھ سکتے ہیں۔"
                    }
                    else -> {
                        "ٹھیک ہے، میں یہ کام بہترین طریقے سے کر دوں گا۔ تھوڑی دیر میں پہنچتا ہوں۔"
                    }
                }
            }

            repository.sendChatMessage(
                ChatMessageEntity(
                    bookingId = bookingId,
                    sender = "Worker",
                    message = responseText
                )
            )
        }
    }

    // Live Tracking coordinate movement simulation
    private fun startLocationSimulation(bookingId: Int) {
        if (currentSimulatedBookingId == bookingId && movementJob?.isActive == true) {
            // Already simulating movement for this booking, don't restart or reset!
            return
        }
        currentSimulatedBookingId = bookingId
        movementJob?.cancel()
        isWorkerMoving.value = true
        // Set worker start coordinates slightly offset from customer
        workerLat.value = 33.6744
        workerLng.value = 73.0379
        // Customer standard coordinate
        customerLat.value = 33.6894
        customerLng.value = 73.0579

        movementJob = viewModelScope.launch {
            val steps = 30
            val latStep = (customerLat.value - workerLat.value) / steps
            val lngStep = (customerLng.value - workerLng.value) / steps

            for (i in 1..steps) {
                delay(1200) // update coordinate position every 1.2s
                workerLat.value += latStep
                workerLng.value += lngStep
            }
            isWorkerMoving.value = false
            
            // Auto Transition to In_Progress upon virtual arrival
            val current = activeBooking.value
            if (current != null && current.id == bookingId && current.status == "Accepted") {
                repository.updateBooking(current.copy(status = "In_Progress"))
                repository.sendChatMessage(
                    ChatMessageEntity(
                        bookingId = current.id,
                        sender = "Worker",
                        message = if (_language.value == Language.EN) {
                            "I have reached your doorstep. Please let me in to start!"
                        } else {
                            "میں آپ کے گھر پہنچ گیا ہوں۔ کام شروع کرنے کے لیے دروازہ کھولیں۔"
                        }
                    )
                )
            }
        }
    }

    private fun stopLocationSimulation() {
        movementJob?.cancel()
        isWorkerMoving.value = false
    }

    // ID/CNIC Verification Submission
    fun verifyCNIC(number: String) {
        if (number.length >= 13) {
            isCNICVerified.value = true
            viewModelScope.launch {
                // If they are a registered active worker, update database record
                if (registeredName.value.isNotEmpty() && isWorkerMode.value) {
                    val all = allWorkers.value
                    val matching = all.find { it.name == registeredName.value }
                    if (matching != null) {
                        repository.updateWorker(matching.copy(isVerified = true, cnicNumber = number))
                    }
                }
            }
        }
    }

    // Emergency Panic Dispatch
    fun triggerEmergencyPanic() {
        isEmergencyActive.value = true
        emergencyStage.value = "broadcasting"
        emergencyLog.value = if (_language.value == Language.EN) {
            listOf("Searching for closeby registered workers...", "Broadcasting distress GPS coordinates...")
        } else {
            listOf("قریبی رجسٹرڈ کاریگروں کی تلاش جاری ہے...", "ہنگامی لوکیشن کوآرڈینیٹس بھیجے جا رہے ہیں...")
        }

        viewModelScope.launch {
            delay(1500)
            emergencyLog.value = emergencyLog.value + if (_language.value == Language.EN) {
                listOf("Found 3 workers in 5km radius.", "Sending urgent SMS alert callbacks...")
            } else {
                listOf("5 کلومیٹر کے دائرے میں 3 کاریگر مل گئے ہیں۔", "ہنگامی ایس ایم ایس الرٹ بھیجا جا رہا ہے...")
            }

            delay(1500)
            emergencyStage.value = "responded"
            emergencyLog.value = emergencyLog.value + if (_language.value == Language.EN) {
                listOf(
                    "ALERT ACCEPTED!",
                    "Specialist M. Asif Khan (Plumber) has accepted emergency route dispatch and is calling you immediately."
                )
            } else {
                listOf(
                    "الرٹ قبول کر لیا گیا ہے!",
                    "کاریگر ایم آصف خان (پلمبر) نے ہنگامی مدد قبول کر لی ہے اور وہ فوری آپ سے رابطہ کر رہے ہیں۔"
                )
            }
        }
    }

    fun resetEmergency() {
        isEmergencyActive.value = false
        emergencyStage.value = "idle"
        emergencyLog.value = emptyList()
    }

    // Wallet Payout / Withdrawal
    fun requestWithdrawal(amount: Double, channel: String): Boolean {
        if (amount > 0 && amount <= walletBalance.value) {
            viewModelScope.launch {
                repository.addWalletTransaction(
                    WalletTransactionEntity(
                        amount = amount,
                        type = "Withdrawal",
                        description = "Funds payout withdrawal to registered mobile number",
                        referenceChannel = channel
                    )
                )
            }
            return true
        }
        return false
    }

    // Get translated textual strings
    fun getString(en: String, ur: String): String {
        return if (_language.value == Language.EN) en else ur
    }

    // Observe live messages
    fun getChatMessages(bookingId: Int): Flow<List<ChatMessageEntity>> {
        return repository.getChatMessages(bookingId)
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
        movementJob?.cancel()
    }
}
