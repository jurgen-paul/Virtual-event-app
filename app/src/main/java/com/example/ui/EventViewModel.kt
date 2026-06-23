package com.example.ui

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.network.GeminiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class EventTab {
    REGISTRATION,
    LIVE_STREAM,
    ANALYTICS,
    INBOX
}

data class LivePoll(
    val question: String,
    val options: List<String>,
    val votes: MutableList<Int>,
    val userSelectedOption: Int? = null
)

class EventViewModel(application: Application) : AndroidViewModel(application) {
    private val database = EventDatabase.getDatabase(application)
    private val repository = EventRepository(database.eventDao())

    // UI state flows
    val userRegistration: StateFlow<UserRegistration?> = repository.userRegistration.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val allSessions: StateFlow<List<EventSession>> = repository.allSessions.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allNotifications: StateFlow<List<AttendeeNotification>> = repository.allNotifications.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allMetrics: StateFlow<List<EngagementMetric>> = repository.allMetrics.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Current State Management
    var currentTab = mutableStateOf(EventTab.REGISTRATION)
    var activeSession = mutableStateOf<EventSession?>(null)
    
    // Live Stream Simulator Variables
    var isPlaying = mutableStateOf(false)
    var activeParticipants = mutableStateOf(128)
    var isUserFocused = mutableStateOf(true) // Simulates focus tracking (telemetry test)
    var aiCritique = mutableStateOf("No critique yet. Start watching the session to compile real-time telemetry analytics.")

    // Simulated Presentation Slides
    private val slides = listOf(
        "Slide 1: Welcome & Introductions",
        "Slide 2: Architectural Overview",
        "Slide 3: Technical Implementation Details",
        "Slide 4: Optimizations & Best Practices",
        "Slide 5: Live Q&A and Interactive Demos"
    )
    var currentSlideIndex = mutableStateOf(0)
    var currentSlideText = derivedStateOf { slides[currentSlideIndex.value] }

    // Live Session Chat
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    // Interactive Session Polls
    private val initialPolls = mapOf(
        "session_1" to LivePoll(
            question = "Which Jetpack Compose feature are you most excited to master?",
            options = listOf("Custom Canvas Drawing", "Complex Ripple & Shader Effects", "Edge-to-Edge Architecture", "Subcomposition & Lazy Layouts"),
            votes = mutableListOf(24, 18, 51, 32)
        ),
        "session_2" to LivePoll(
            question = "How do you plan to deploy Gemini integrations in your next project?",
            options = listOf("Direct REST Networking", "Firebase AI & App Check", "In-App Local Core Models", "Cloud Function Proxies"),
            votes = mutableListOf(45, 29, 12, 18)
        ),
        "session_3" to LivePoll(
            question = "What is your primary bottleneck when measuring user engagement?",
            options = listOf("Complex Telemetry Infrastructure", "Privacy & Consent Constraints", "Rendering High-Fidelity Charts", "Data Stream Volume Analytics"),
            votes = mutableListOf(37, 42, 19, 28)
        )
    )
    var sessionPolls = mutableMapOf<String, LivePoll>().apply {
        putAll(initialPolls)
    }
    
    var activeSessionPoll = mutableStateOf<LivePoll?>(null)

    // Current interaction metrics (updates local state, periodically flushes/merges to DB)
    private var watchedSecondsCounter = 0
    private var focusedSecondsCounter = 0
    private var chatCountCounter = 0
    private var questionCountCounter = 0
    private var isGeneratingChatReply = false

    init {
        viewModelScope.launch {
            // Check and populate demo sessions
            setupDemoData()
            
            // Listen to current active session's database query flow
            snapshotFlow { activeSession.value }.collectLatest { session ->
                val sessionId = session?.sessionId
                if (sessionId != null) {
                    // Update active poll
                    activeSessionPoll.value = sessionPolls[sessionId]
                    
                    // Fetch real-time chat
                    _chatMessages.value = emptyList() // Clear local first
                    repository.getChatMessages(sessionId).collect { list ->
                        _chatMessages.value = list
                    }
                } else {
                    _chatMessages.value = emptyList()
                    activeSessionPoll.value = null
                }
            }
        }

        // Start background simulator clock (runs telemetry, slides, chat generator, participant oscilllation)
        viewModelScope.launch {
            startLiveStreamSimulator()
        }
    }

    private suspend fun setupDemoData() = withContext(Dispatchers.IO) {
        val count = database.eventDao().getSessionById("session_1")
        if (count == null) {
            val sessions = listOf(
                EventSession(
                    sessionId = "session_1",
                    title = "Jetpack Compose Deepdive: High-Performance Animations",
                    speaker = "Sarah Chen",
                    speakerTitle = "Principal UI Architect, Google",
                    time = "10:00 AM - 11:15 AM PST",
                    description = "Master custom Jetpack Compose Canvas work, lookahead layout scopes, and edge-to-edge window inset optimizations for high-fidelity Android interfaces.",
                    isRegistered = false,
                    videoUrl = "",
                    initialParticipants = 452
                ),
                EventSession(
                    sessionId = "session_2",
                    title = "AI-Driven Architectures: Leveraging Gemini 3.5 Flash",
                    speaker = "Dr. Michael Stone",
                    speakerTitle = "Lead AI Engineer, Google DeepMind",
                    time = "11:30 AM - 12:45 PM PST",
                    description = "Learn production patterns for direct REST and Firebase AI model integration. Build secure prompt engineering schemes directly inside modern MVVM structures.",
                    isRegistered = false,
                    videoUrl = "",
                    initialParticipants = 521
                ),
                EventSession(
                    sessionId = "session_3",
                    title = "Polishing Real-Time Telemetry & Data Visualization",
                    speaker = "Marcus Vance",
                    speakerTitle = "Systems UX Architect, EventSpace",
                    time = "2:00 PM - 3:15 PM PST",
                    description = "A detailed look at event streaming telemetry, measuring client focus loss, and translating sparse metrics into premium Canvas charts.",
                    isRegistered = false,
                    videoUrl = "",
                    initialParticipants = 293
                )
            )
            repository.insertSessions(sessions)

            // Inject initial welcome and chat messages
            for (session in sessions) {
                repository.insertChatMessage(
                    ChatMessage(
                        sessionId = session.sessionId,
                        userName = "System Moderator",
                        userRole = "Moderator",
                        text = "Welcome to ${session.title}! Feel free to interact in the live chat or submit your poll answers."
                    )
                )
                repository.insertChatMessage(
                    ChatMessage(
                        sessionId = session.sessionId,
                        userName = "Dr. Linda Gray",
                        userRole = "Attendee",
                        text = "I'm extremely excited for this session! Been working with Compose for a year."
                    )
                )

                // Populate initial blank metrics
                repository.updateEngagementMetric(
                    EngagementMetric(sessionId = session.sessionId)
                )
            }
        }
    }

    fun selectSession(session: EventSession) {
        activeSession.value = session
        activeParticipants.value = session.initialParticipants
        // Reset counters for the session
        watchedSecondsCounter = 0
        focusedSecondsCounter = 0
        chatCountCounter = 0
        questionCountCounter = 0
        currentSlideIndex.value = 0
        currentTab.value = EventTab.LIVE_STREAM
        isPlaying.value = true // Auto-play when selected
    }

    fun togglePlayback() {
        isPlaying.value = !isPlaying.value
    }

    fun submitPollResponse(optionIndex: Int) {
        val session = activeSession.value ?: return
        val currentPoll = sessionPolls[session.sessionId] ?: return
        
        if (currentPoll.userSelectedOption == null) {
            val updatedVotes = currentPoll.votes.toMutableList()
            updatedVotes[optionIndex] = updatedVotes[optionIndex] + 1
            
            // Create a brand new LivePoll instance to guarantee UI state updates
            val updatedPoll = currentPoll.copy(
                votes = updatedVotes,
                userSelectedOption = optionIndex
            )
            
            sessionPolls[session.sessionId] = updatedPoll
            activeSessionPoll.value = updatedPoll

            viewModelScope.launch {
                // Update metric in repository
                val existing = repository.getEngagementMetric(session.sessionId).firstOrNull() ?: EngagementMetric(session.sessionId)
                val updatedMetric = existing.copy(
                    pollAnswersCount = existing.pollAnswersCount + 1,
                    lastUpdated = System.currentTimeMillis()
                )
                repository.updateEngagementMetric(updatedMetric)
                
                // Add System log in chat
                repository.insertChatMessage(
                    ChatMessage(
                        sessionId = session.sessionId,
                        userName = "Event Bot",
                        userRole = "Organizer",
                        text = "🗳️ You responded directly to the active live poll: \"${currentPoll.options[optionIndex]}\"!"
                    )
                )
            }
        }
    }

    fun registerAttendee(name: String, email: String, jobTitle: String, interests: String) {
        viewModelScope.launch {
            val reg = UserRegistration(
                email = email,
                name = name,
                jobTitle = jobTitle,
                interests = interests
            )
            repository.registerUser(reg)

            // Trigger beautiful AI personalized welcome notification!
            val emailDetails = GeminiService.generateCustomEmail(name, jobTitle, interests)
            val welcomeEmail = AttendeeNotification(
                recipientEmail = email,
                subject = emailDetails.first,
                body = emailDetails.second
            )
            repository.insertNotification(welcomeEmail)

            // Auto-register them for all current sessions
            val sessions = repository.allSessions.first()
            for (session in sessions) {
                repository.setSessionRegistration(session.sessionId, isRegistered = true)
            }

            // Move to live stream list
            currentTab.value = EventTab.LIVE_STREAM
            if (sessions.isNotEmpty()) {
                selectSession(sessions.first())
            }
        }
    }

    fun updateProfile(name: String, jobTitle: String, interests: String) {
        val existing = userRegistration.value ?: return
        viewModelScope.launch {
            val reg = existing.copy(
                name = name,
                jobTitle = jobTitle,
                interests = interests
            )
            repository.registerUser(reg)
            
            // Add a notification about profile update
            repository.insertNotification(
                AttendeeNotification(
                    recipientEmail = existing.email,
                    subject = "EventSpace Profile Successfully Synced",
                    body = "Hello ${name},\n\nYour profile has been updated! Our recommender engine is actively analyzing your secondary interests: '$interests' to customize your active stream recommendations of Sarah Chen and Dr. Michael Stone."
                )
            )
        }
    }

    fun unregister() {
        viewModelScope.launch {
            repository.unregisterUser()
            repository.clearNotifications()
            // Reset registrations
            val sessions = repository.allSessions.first()
            for (s in sessions) {
                repository.setSessionRegistration(s.sessionId, isRegistered = false)
            }
            isPlaying.value = false
            activeSession.value = null
            currentTab.value = EventTab.REGISTRATION
        }
    }

    fun clearNotifications() {
        viewModelScope.launch {
            repository.clearNotifications()
        }
    }

    fun sendChatMessage(text: String, isQuestion: Boolean = false) {
        val session = activeSession.value ?: return
        val user = userRegistration.value ?: return

        viewModelScope.launch {
            val role = if (isQuestion) "Speaker" else "Attendee" 
            val message = ChatMessage(
                sessionId = session.sessionId,
                userName = user.name,
                userRole = role,
                text = text
            )
            repository.insertChatMessage(message)
            
            // Increment local stats count
            chatCountCounter++
            if (isQuestion) questionCountCounter++

            // Save telemetry
            val existing = repository.getEngagementMetric(session.sessionId).firstOrNull() ?: EngagementMetric(session.sessionId)
            repository.updateEngagementMetric(
                existing.copy(
                    chatMessagesSent = existing.chatMessagesSent + 1,
                    questionsAsked = existing.questionsAsked + questionCountCounter,
                    lastUpdated = System.currentTimeMillis()
                )
            )

            // Trigger AI reaction in chat if not already thinking
            if (!isGeneratingChatReply) {
                isGeneratingChatReply = true
                triggerSimulatedChatReply(session, text, user)
            }
        }
    }

    private suspend fun triggerSimulatedChatReply(session: EventSession, userText: String, userReg: UserRegistration) {
        // Create an automated response from a random attendee
        val attendeeNames = listOf("Leo Patel", "Chloe Vance", "Hiroshi Sato", "Emma Lindqvist", "Marcus Aurelius")
        val chosenAttendee = attendeeNames.random()
        val randomRole = (0..2).random() // Attendee, Speaker, Moderator
        
        viewModelScope.launch {
            delay(1500) // Small typing lag for high realism!
            val replyText = GeminiService.generateChatResponse(
                sessionTitle = session.title,
                lastUserMessage = userText,
                attendeeName = chosenAttendee,
                attendeeRoleId = randomRole
            )
            val replyMessage = ChatMessage(
                sessionId = session.sessionId,
                userName = chosenAttendee,
                userRole = when (randomRole) {
                    1 -> "Speaker"
                    2 -> "Moderator"
                    else -> "Attendee"
                },
                text = replyText,
                isAI = true
            )
            repository.insertChatMessage(replyMessage)
            isGeneratingChatReply = false
        }
    }

    // Runs a persistent loop every 1 second when active
    private suspend fun startLiveStreamSimulator() {
        while (true) {
            delay(1000)
            
            val session = activeSession.value
            if (session != null && isPlaying.value) {
                // Playback is active! Run simulations
                
                // 1. Increment watch seconds
                watchedSecondsCounter++
                if (isUserFocused.value) {
                    focusedSecondsCounter++
                }

                // Push telemetry increments back to DB every 5 seconds to reduce write volume
                if (watchedSecondsCounter % 5 == 0) {
                    val existing = repository.getEngagementMetric(session.sessionId).firstOrNull() ?: EngagementMetric(session.sessionId)
                    val updated = existing.copy(
                        watchedSeconds = existing.watchedSeconds + 5,
                        activeFocusSeconds = existing.activeFocusSeconds + (if (isUserFocused.value) 5 else 0),
                        lastUpdated = System.currentTimeMillis()
                    )
                    repository.updateEngagementMetric(updated)
                }

                // 2. Oscillate active audience size
                val change = (-3..4).random()
                activeParticipants.value = (activeParticipants.value + change).coerceIn(40, 1000)

                // 3. Move presentation slides (increment every 25 watched seconds)
                if (watchedSecondsCounter > 0 && watchedSecondsCounter % 25 == 0) {
                    currentSlideIndex.value = (currentSlideIndex.value + 1) % slides.size
                    
                    // Automatically insert notification "Event Alert" or system message
                    val systemCard = ChatMessage(
                        sessionId = session.sessionId,
                        userName = "System Broadcast",
                        userRole = "Moderator",
                        text = "📢 Speaker ${session.speaker} has advanced to: \"${currentSlideText.value}\"."
                    )
                    repository.insertChatMessage(systemCard)
                }

                // 4. Trigger casual ambient public comments (every 18 seconds)
                if (watchedSecondsCounter > 0 && watchedSecondsCounter % 18 == 0) {
                    val ambientNames = listOf("Alex Rivera", "Amina Kouyaté", "Sven Hansen", "Tariq Ali", "Sofia Rossi")
                    val phrases = listOf(
                        "Very clean implementation!",
                        "Is there a link for the dependencies configurations?",
                        "Wow, look at those dynamic Canvas lines",
                        "Great response style from Sarah Chen!",
                        "Can this be configured on older SDKs?"
                    )
                    repository.insertChatMessage(
                        ChatMessage(
                            sessionId = session.sessionId,
                            userName = ambientNames.random(),
                            userRole = "Attendee",
                            text = phrases.random(),
                            isAI = true
                        )
                    )
                }

                // 5. Update AI performance critique every 15 seconds of play
                if (watchedSecondsCounter > 0 && watchedSecondsCounter % 15 == 0) {
                    val critique = GeminiService.generateMetricCritique(
                        sessionTitle = session.title,
                        watchedSecs = watchedSecondsCounter,
                        engagedSecs = focusedSecondsCounter,
                        chatCount = chatCountCounter
                    )
                    aiCritique.value = critique
                }
            }
        }
    }
}
