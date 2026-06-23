package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "user_registration")
data class UserRegistration(
    @PrimaryKey val email: String,
    val name: String,
    val jobTitle: String,
    val interests: String, // comma-separated strings
    val registeredAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "event_session")
data class EventSession(
    @PrimaryKey val sessionId: String,
    val title: String,
    val speaker: String,
    val speakerTitle: String,
    val time: String,
    val description: String,
    val isRegistered: Boolean = false,
    val videoUrl: String,
    val initialParticipants: Int = 120
)

@Entity(tableName = "chat_message")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val userName: String,
    val userRole: String, // "Attendee", "Speaker", "Moderator", "Organizer"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isAI: Boolean = false
)

@Entity(tableName = "attendee_notification")
data class AttendeeNotification(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipientEmail: String,
    val subject: String,
    val body: String,
    val sentTimestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

@Entity(tableName = "engagement_metric")
data class EngagementMetric(
    @PrimaryKey val sessionId: String,
    val watchedSeconds: Int = 0,
    val activeFocusSeconds: Int = 0, 
    val chatMessagesSent: Int = 0,
    val questionsAsked: Int = 0,
    val pollAnswersCount: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Dao
interface EventDao {
    // Registration
    @Query("SELECT * FROM user_registration LIMIT 1")
    fun getUserRegistration(): Flow<UserRegistration?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRegistration(registration: UserRegistration)

    @Query("DELETE FROM user_registration")
    suspend fun clearRegistration()

    // Sessions
    @Query("SELECT * FROM event_session")
    fun getAllSessions(): Flow<List<EventSession>>

    @Query("SELECT * FROM event_session WHERE sessionId = :sessionId")
    suspend fun getSessionById(sessionId: String): EventSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<EventSession>)

    @Query("UPDATE event_session SET isRegistered = :isRegistered WHERE sessionId = :sessionId")
    suspend fun updateSessionRegistration(sessionId: String, isRegistered: Boolean)

    // Chat
    @Query("SELECT * FROM chat_message WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getChatMessages(sessionId: String): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessage)

    @Query("DELETE FROM chat_message WHERE sessionId = :sessionId")
    suspend fun clearChatForSession(sessionId: String)

    // Notifications (Email Logs)
    @Query("SELECT * FROM attendee_notification ORDER BY sentTimestamp DESC")
    fun getAllNotifications(): Flow<List<AttendeeNotification>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: AttendeeNotification)

    @Query("DELETE FROM attendee_notification")
    suspend fun clearNotifications()

    // Engagement Metrics
    @Query("SELECT * FROM engagement_metric WHERE sessionId = :sessionId")
    fun getEngagementMetric(sessionId: String): Flow<EngagementMetric?>

    @Query("SELECT * FROM engagement_metric")
    fun getAllEngagementMetrics(): Flow<List<EngagementMetric>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEngagementMetric(metric: EngagementMetric)
}

@Database(
    entities = [
        UserRegistration::class,
        EventSession::class,
        ChatMessage::class,
        AttendeeNotification::class,
        EngagementMetric::class
    ],
    version = 1,
    exportSchema = false
)
abstract class EventDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao

    companion object {
        @Volatile
        private var INSTANCE: EventDatabase? = null

        fun getDatabase(context: Context): EventDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EventDatabase::class.java,
                    "event_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class EventRepository(private val eventDao: EventDao) {
    val userRegistration: Flow<UserRegistration?> = eventDao.getUserRegistration()
    val allSessions: Flow<List<EventSession>> = eventDao.getAllSessions()
    val allNotifications: Flow<List<AttendeeNotification>> = eventDao.getAllNotifications()
    val allMetrics: Flow<List<EngagementMetric>> = eventDao.getAllEngagementMetrics()

    fun getChatMessages(sessionId: String): Flow<List<ChatMessage>> = eventDao.getChatMessages(sessionId)
    fun getEngagementMetric(sessionId: String): Flow<EngagementMetric?> = eventDao.getEngagementMetric(sessionId)

    suspend fun registerUser(registration: UserRegistration) {
        eventDao.insertRegistration(registration)
    }

    suspend fun unregisterUser() {
        eventDao.clearRegistration()
    }

    suspend fun setSessionRegistration(sessionId: String, isRegistered: Boolean) {
        eventDao.updateSessionRegistration(sessionId, isRegistered)
    }

    suspend fun insertChatMessage(message: ChatMessage) {
        eventDao.insertChatMessage(message)
    }

    suspend fun insertNotification(notification: AttendeeNotification) {
        eventDao.insertNotification(notification)
    }

    suspend fun insertSessions(sessions: List<EventSession>) {
        eventDao.insertSessions(sessions)
    }

    suspend fun updateEngagementMetric(metric: EngagementMetric) {
        eventDao.insertEngagementMetric(metric)
    }

    suspend fun clearNotifications() {
        eventDao.clearNotifications()
    }
}
