# NoteSync

`NoteSync` is a to-do list application for Android that allows users to manage their tasks. It is designed with an offline-first architecture, ensuring that users can continue to use the app even without an internet connection. Changes are automatically synchronized with a Supabase backend when the connection is restored.

## Features

*   **User Authentication:** Users can register and log in to their accounts.
*   **Task Management:**
    *   Create, read, update, and delete tasks.
    *   Mark tasks as complete.
*   **Offline-First:** The app is fully functional offline.
    *   Tasks are stored locally on the device.
    *   Changes made offline are automatically synced with the server when the connection is re-established.
*   **Data Synchronization:** Two-way data synchronization between the local database and the Supabase backend.

## Architecture

The application follows a modern Android architecture with an offline-first approach.

*   **UI Layer (Compose):** The user interface is built entirely with Jetpack Compose. The UI observes data from the `UserRepository` and sends user events to it.
*   **Data Layer (`UserRepository`):** A singleton object that acts as the single source of truth for the application's data. It is responsible for:
    *   Managing an in-memory cache of user and task data.
    *   Handling all business logic related to user authentication and task management.
    *   Communicating with the Supabase backend for data synchronization.
*   **Persistence Layer (`SyncManager`):** This layer is responsible for persisting data locally. It uses Jetpack DataStore to store tasks for each user, allowing for offline access.
*   **Backend:** Supabase is used for user authentication and as the cloud database for storing task data.

## Technologies Used

*   **Language:** [Kotlin](https://kotlinlang.org/)
*   **UI:** [Jetpack Compose](https://developer.android.com/jetpack/compose) (with Material 3)
*   **Navigation:** [Navigation Compose](https://developer.android.com/jetpack/compose/navigation)
*   **Local Storage:** [Jetpack DataStore](https://developer.android.com/topic/libraries/architecture/datastore) (with Gson for serialization)
*   **Backend as a Service (BaaS):** [Supabase](https://supabase.io/)
    *   Authentication (`auth-kt`)
    *   Database (`postgrest-kt`)
*   **Asynchronous Programming:** Kotlin Coroutines
*   **Networking:** [Ktor Client](https://ktor.io/docs/client-overview.html)
*   **Serialization:** [Kotlinx Serialization](https://github.com/Kotlin/kotlinx.serialization) and [Gson](https://github.com/google/gson)

## Setup and Configuration

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/your-username/NoteSync.git
    ```
2.  **Open in Android Studio:** Open the cloned project in Android Studio.
3.  **Configure Supabase:**
    You need to add your Supabase URL and public-anon-key to the project. Open the file `app/src/main/java/com/filizzola/projeto_mobile/MainActivity.kt` and update the `SupabaseConfig` object with your credentials:

    ```kotlin
    object SupabaseConfig {
        val client by lazy {
            createSupabaseClient(
                supabaseUrl = "YOUR_SUPABASE_URL",
                supabaseKey = "YOUR_SUPABASE_ANON_KEY"
            ) {
                install(Auth)
                install(Postgrest)
            }
        }
    }
    ```
4.  **Sync Gradle:** Sync the project with the Gradle files.
5.  **Run the app:** Run the application on an Android emulator or a physical device.