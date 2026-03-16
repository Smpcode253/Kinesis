# Aura Assistant

**A proactive and context-aware voice assistant for Android, designed for seamless, hands-free operation.**

Aura is an intelligent assistant that understands your context, especially while driving, to help you manage communications, navigation, and tasks safely and efficiently. It uses a Large Language Model (LLM) to interpret natural language commands and acts as a true co-pilot for your daily life.

## Core Features

-   **Contextual Awareness:** Understands if you're driving, at home, or at work to adapt its behaviour.
-   **Natural Language Understanding:** Speak naturally. Aura uses an LLM to figure out what you mean.
-   **Hands-Free Control:** Make calls, send messages, and start navigation without ever touching your screen.
-   **Proactive Suggestions:** Suggests actions based on incoming notifications while driving.
-   **Task & Agenda Management:** Create reminders, add calendar events, and ask "what's my day look like?".
-   **Privacy-Focused:** All your data is stored locally on your device via Room.

## Tech Stack

-   **Language:** [Kotlin](https://kotlinlang.org/)
-   **Architecture:** Clean Architecture with a Service-based core loop
-   **Asynchronous Programming:** [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-guide.html)
-   **Database:** [Room](https://developer.android.com/training/data-storage/room) for local data persistence
-   **Networking:** [Retrofit](https://square.github.io/retrofit/) for LLM client (OpenAI-compatible)
-   **Serialization:** [Gson](https://github.com/google/gson)

## Project Structure

```
app/src/main/java/com/aura/assistant/
├── AuraApplication.kt          # App-level DI / singletons
├── MainActivity.kt             # Entry point UI
├── core/
│   ├── AuraCoreService.kt      # Foreground service — main assistant loop
│   ├── BootReceiver.kt         # Auto-starts service after reboot
│   ├── ContextManager.kt       # Detects driving/walking context via accelerometer
│   ├── IntentRouter.kt         # Routes parsed intents to the correct executor
│   └── ReminderAlarmReceiver.kt
├── data/db/
│   ├── AuraDatabase.kt         # Room database
│   ├── dao/                    # Data access objects
│   └── entities/               # Reminder, TrustedContact, ConversationEntry
├── domain/
│   ├── model/                  # AuraIntent (sealed class), UserContext, ExecutorResult
│   └── repository/             # ReminderRepository, ContactRepository
├── executor/
│   ├── CommsExecutor.kt        # Phone calls & SMS (with trust-level checks)
│   ├── NavExecutor.kt          # Google Maps navigation
│   └── TaskExecutor.kt         # Reminders (AlarmManager) & calendar events
├── llm/
│   ├── LlmClient.kt            # Parses speech → AuraIntent via LLM API
│   └── LlmApiService.kt        # Retrofit interface (OpenAI-compatible)
├── notification/
│   └── AuraNotificationListenerService.kt  # Proactive notification announcements
├── settings/
│   ├── SettingsActivity.kt     # Trusted contacts management UI
│   ├── SettingsViewModel.kt
│   └── TrustedContactsAdapter.kt
├── speech/
│   └── SpeechManager.kt        # Voice-to-Text + Text-to-Speech
└── ui/
    └── MainViewModel.kt
```

## Getting Started

### Prerequisites

-   Android Studio (latest stable version)
-   An Android device or emulator running API level 26+

### Installation

1.  **Clone the repo**
    ```sh
    git clone https://github.com/Smpcode253/Kinesis.git
    ```
2.  **Open in Android Studio**
    -   Open Android Studio and select `Open an existing project`.
    -   Navigate to the cloned directory and open it.
3.  **Set up API Keys**
    -   Create a `local.properties` file in the root of the project.
    -   Add your LLM API key:
      ```properties
      LLM_API_KEY="YOUR_API_KEY_HERE"
      ```
4.  **Build and Run**
    -   Let Android Studio sync Gradle dependencies.
    -   Click `Run` to build and install the app.

### Permissions

Aura requests the following permissions at runtime:
- `RECORD_AUDIO` — for voice input
- `CALL_PHONE` — to make calls
- `SEND_SMS` — to send messages
- `READ_CONTACTS` — to resolve contact names

Grant **Notification Access** via *Settings → Apps → Special app access → Notification access* to enable proactive driving notifications.

## Running Tests

```sh
./gradlew test
```

Unit tests cover:
- `AuraIntentTest` — domain model correctness
- `UserContextTest` — context detection logic
- `TrustLevelTest` — trust level validation
- `LlmJsonParsingTest` — LLM response → AuraIntent parsing
- `NavExecutorTest` — navigation intent handling

## License

Distributed under the MIT License.

