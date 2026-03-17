import androidx.room.*;
import kotlinx.coroutines.*;
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// ================== ENUMS FOR TYPE SAFETY ==================
// Using enums prevents typos and makes the code more robust and readable.

enum class EventType { USER_COMMAND, NOTIFICATION, CALL, CALENDAR, LOCATION }
enum class IntentType { COMMS_CALL, COMMS_ANSWER_CALL, COMMS_REPLY, NAV_START, TASK_CREATE_REMINDER, TASK_VIEW_AGENDA, TASK_CREATE_EVENT, NONE }
enum class PolicyMode { AUTO_EXECUTE, CONFIRM_VOICE, CONFIRM_TAP, SUGGEST_ONLY }
enum class Domain { COMMS, NAV, TASKS, FINANCE, OTHER }
enum class ActionStatus { SUCCESS, FAILED }
enum class ActionSource { USER_COMMAND, PROACTIVE }

// ================== DATA MODELS (Room) ==================
// Using @Serializable for future-proofing with TypeConverters.

@Serializable
@Entity
data class Event(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: EventType,
    val payloadJson: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity
data class IntentRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventId: Long,
    val intentType: IntentType,
    val slotsJson: String,
    val policyMode: PolicyMode,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity
data class ActionRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val intentId: Long,
    val domain: Domain,
    val actionDescription: String,
    val reason: String,
    val source: ActionSource,
    val status: ActionStatus,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity
data class Preferences(
    @PrimaryKey val id: Long = 1,
    val trustComms: PolicyMode = PolicyMode.SUGGEST_ONLY,
    val trustNav: PolicyMode = PolicyMode.SUGGEST_ONLY,
    val trustTasks: PolicyMode = PolicyMode.SUGGEST_ONLY,
    val trustFinance: PolicyMode = PolicyMode.SUGGEST_ONLY,
    val trustedContactsJson: String = "[]",
    val quietHoursJson: String = "{}",
    val drivingSettingsJson: String = "{}"
)

// ================== DB & DAO ==================
// Added TypeConverters for enums. Added query for Preferences.

class Converters {
    @TypeConverter fun fromEventType(value: EventType): String = value.name
    @TypeConverter fun toEventType(value: String): EventType = EventType.valueOf(value)
    
    // ... Add converters for all other enums (IntentType, PolicyMode, etc.) ...
}

@Dao
interface EventDao {
    @Insert fun insert(event: Event): Long
}

@Dao
interface IntentDao {
    @Insert fun insert(intent: IntentRecord): Long
}

@Dao
interface ActionDao {
    @Insert fun insert(action: ActionRecord): Long
    @Query("SELECT * FROM ActionRecord ORDER BY createdAt DESC LIMIT :limit")
    fun getHistory(limit: Int = 100): List<ActionRecord>
}

@Dao
interface PreferencesDao {
    @Query("SELECT * FROM Preferences WHERE id = 1")
    fun get(): Preferences?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun save(prefs: Preferences)
}


@Database(
    entities = [Event::class, IntentRecord::class, ActionRecord::class, Preferences::class],
    version = 1
)
@TypeConverters(Converters::class)
abstract class AppDb : RoomDatabase() {
    abstract fun eventDao(): EventDao
    abstract fun intentDao(): IntentDao
    abstract fun actionDao(): ActionDao
    abstract fun preferencesDao(): PreferencesDao
}

// ================== LLM CLIENT (with type-safe slot classes) ==================

// Using delegation for type-safe access to map values. This is much safer than casting.
class MapDelegate<T>(private val map: Map<String, Any?>, private val key: String) {
    @Suppress("UNCHECKED_CAST")
    operator fun getValue(thisRef: Any?, property: Any?): T? = map[key] as? T
}

// Define dedicated, type-safe data classes for the slots of each intent.
data class CommsCallSlots(private val slots: Map<String, Any?>) {
    val contactName: String? by MapDelegate(slots, "contact_name")
    val mode: String? by MapDelegate(slots, "mode")
}

data class NavStartSlots(private val slots: Map<String, Any?>) {
    val destinationType: String? by MapDelegate(slots, "destination_type")
    val destinationText: String? by MapDelegate(slots, "destination_text")
    val contactName: String? by MapDelegate(slots, "contact_name")
    val sourceMessageHint: String? by MapDelegate(slots, "source_message_hint")
}
// ... Create similar data classes for all other intents ...

data class InterpretRequest(
    val user_command: String?,
    val event_context: Map<String, Any?>,
    val user_state: Map<String, Any?>,
    val conversation_context: Map<String, Any?>, // Added convo context to request
    val event_payload: Map<String, Any?>
)

data class PolicyDecision(val mode: PolicyMode, val reason: String)

data class InterpretResponse(
    val intent: IntentType,
    val slots: Map<String, Any?>,
    val policy_decision: PolicyDecision
)

interface LlmApi {
    suspend fun interpret(req: InterpretRequest): InterpretResponse
    suspend fun summarize(body: Map<String, Any?>): Map<String, Any?>
}

// ================== EXECUTORS (No change, interfaces are good) ==================
interface CommsExecutor {
    fun sendSms(phone: String, text: String): Boolean
    fun placeCall(phone: String, speaker: Boolean): Boolean
    fun answerCall(callId: String?, speaker: Boolean): Boolean
}

interface NavExecutor {
    fun startNavigation(destinationText: String): Boolean
}

interface TaskExecutor {
    fun createReminder(text: String, trigger: Map<String, Any?>): Boolean
    fun createCalendarEvent(title: String, time: String, participants: List<String>, location: String?): Boolean
    fun getAgenda(day: String): List<Map<String, Any?>>
}

interface FinanceExecutor {
    fun checkBalance(accountType: String): Boolean
    fun transfer(from: String, to: String, amount: Double): Boolean
    fun payBill(payee: String, amountMode: String): Boolean
}

// ================== CONVERSATION CONTEXT ==================

data class ConversationContext(
    var lastContactName: String? = null,
    var lastChannel: String? = null,
    var lastMessageText: String? = null,
    var lastMessageApp: String? = null,
    var lastCallFrom: String? = null,
    var lastAddressText: String? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "lastContactName" to lastContactName,
        "lastChannel" to lastChannel,
        // etc.
    )
}

// ================== ASSISTANT SERVICE CORE LOOP (Major Refactor) ==================

class AssistantService : Service() {

    // Use dependency injection in a real app (e.g., Hilt, Koin)
    private lateinit var db: AppDb
    private lateinit var llm: LlmApi
    // ... executors ...
    
    private val convo = ConversationContext()

    // FIX: Use a proper CoroutineScope for background tasks. Never use runBlocking on main thread.
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // init db, llm client, executors, etc.
    }

    fun onUserSpeech(text: String) {
        handleEvent(EventType.USER_COMMAND, mapOf("text" to text), userCommand = text)
    }

    fun onNotificationCaptured(payload: Map<String, Any?>) {
        handleEvent(EventType.NOTIFICATION, payload, userCommand = null)
    }

    private fun handleEvent(
        type: EventType,
        payload: Map<String, Any?>,
        userCommand: String?
    ) {
        serviceScope.launch { // Launch a coroutine for the entire flow
            val eventId = db.eventDao().insert(Event(type = type, payloadJson = toJson(payload)))
            val context = buildEventContext(type)
            val state = loadUserState()
            
            val req = InterpretRequest(
                user_command = userCommand,
                event_context = context,
                user_state = state,
                conversation_context = convo.toMap(),
                event_payload = payload
            )

            val resp = llm.interpret(req)
            val intentId = db.intentDao().insert(
                IntentRecord(
                    eventId = eventId,
                    intentType = resp.intent,
                    slotsJson = toJson(resp.slots),
                    policyMode = resp.policy_decision.mode
                )
            )
            handlePolicyAndExecute(intentId, resp)
        }
    }

    private suspend fun handlePolicyAndExecute(intentId: Long, resp: InterpretResponse) {
        val domain = domainForIntent(resp.intent)
        val actionDesc = describeAction(resp.intent, resp.slots)

        when (resp.policy_decision.mode) {
            PolicyMode.AUTO_EXECUTE -> {
                val result = executeIntent(resp.intent, resp.slots)
                db.actionDao().insert(
                    ActionRecord(
                        intentId = intentId,
                        domain = domain,
                        actionDescription = actionDesc,
                        reason = resp.policy_decision.reason,
                        source = ActionSource.USER_COMMAND,
                        status = if (result.isSuccess) ActionStatus.SUCCESS else ActionStatus.FAILED
                    )
                )
            }
            PolicyMode.CONFIRM_VOICE -> {
                speak("I'm about to $actionDesc. Okay?")
                // ... listen for yes/no and then call executeIntent ...
            }
            PolicyMode.CONFIRM_TAP -> {
                showConfirmNotification(intentId, domain, actionDesc, resp.policy_decision.reason)
            }
            PolicyMode.SUGGEST_ONLY -> {
                // This is a successful suggestion, not an execution.
                 db.actionDao().insert(
                    ActionRecord(
                        intentId = intentId,
                        domain = domain,
                        actionDescription = "suggest:$actionDesc",
                        reason = resp.policy_decision.reason,
                        source = ActionSource.PROACTIVE, // Or USER_COMMAND if it was a direct but un-executable query
                        status = ActionStatus.SUCCESS 
                    )
                )
            }
        }
    }

    // FIX: Return a Result for explicit success/failure handling. Use safe slot accessors.
    private fun executeIntent(intent: IntentType, slots: Map<String, Any?>): Result<Unit> = runCatching {
        when (intent) {
            IntentType.COMMS_CALL -> {
                val callSlots = CommsCallSlots(slots)
                val phone = resolvePhone(callSlots.contactName) ?: throw IllegalArgumentException("Contact not found")
                comms.placeCall(phone, callSlots.mode == "SPEAKER")
            }
            IntentType.NAV_START -> {
                val navSlots = NavStartSlots(slots)
                // Update conversation context
                if(navSlots.destinationText != null) convo.lastAddressText = navSlots.destinationText
                val dest = resolveDestination(navSlots) ?: throw IllegalArgumentException("Destination not found")
                nav.startNavigation(dest)
            }
            // ... handle all other intents with their type-safe slot classes ...
            else -> throw NotImplementedError("Intent $intent not implemented.")
        }
    }

    // ===== Helpers (Improved) =====
    
    private suspend fun loadUserState(): Map<String, Any?> {
        // Correctly load preferences from DB on a background thread.
        val prefs = db.preferencesDao().get() ?: Preferences()
        val recentActions = db.actionDao().getHistory(limit = 10)
        return mapOf(
            "trust_levels" to mapOf(
                "comms" to prefs.trustComms,
                "nav" to prefs.trustNav,
                "tasks" to prefs.trustTasks,
                "finance" to prefs.trustFinance
            ),
            "trusted_contacts" to fromJsonList<String>(prefs.trustedContactsJson),
            "recent_actions" to recentActions // Include recent actions for the LLM
        )
    }

    private fun domainForIntent(intent: IntentType): Domain = when (intent) {
        IntentType.COMMS_CALL, IntentType.COMMS_REPLY, IntentType.COMMS_ANSWER_CALL -> Domain.COMMS
        IntentType.NAV_START -> Domain.NAV
        IntentType.TASK_CREATE_REMINDER, IntentType.TASK_CREATE_EVENT, IntentType.TASK_VIEW_AGENDA -> Domain.TASKS
        else -> Domain.OTHER
    }
    
    private fun describeAction(intent: IntentType, slots: Map<String, Any?>): String {
        return when(intent) {
            IntentType.COMMS_CALL -> "call ${CommsCallSlots(slots).contactName ?: "contact"}"
            IntentType.NAV_START -> "navigate to ${NavStartSlots(slots).destinationText ?: "destination"}"
            // ... more descriptive action strings ...
            else -> intent.name
        }
    }
    
    private fun resolvePhone(contactName: String?): String? = contactName // In real app: lookup in contacts provider
    
    private fun resolveDestination(slots: NavStartSlots): String? {
        // Logic: 1. Direct text, 2. Resolve from contact, 3. Use last remembered address
        return slots.destinationText 
            ?: slots.contactName?.let { resolveAddressForContact(it) }
            ?: convo.lastAddressText
    }
    
    private fun resolveAddressForContact(name: String): String? = null // Stub for contact address lookup

    // ... other stubs like isDriving(), toJson(), etc. ...
    private fun toJson(obj: Any?): String = Json.encodeToString(serializer(), obj) // Example using kotlinx.serialization
    
    override fun onDestroy() {
        super.onDestroy()
        // Cancel all coroutines when the service is destroyed to prevent leaks.
        serviceScope.cancel()
    }
}