package com.example.music.data.repository

import android.content.Context
import android.util.Log
import com.example.music.data.model.AccountType
import com.example.music.data.model.AdminCredentials
import com.example.music.data.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Repositorio de autenticación
 * Gestiona login, registro y sincronización de datos
 *
 * 3 Modos de usuario:
 * 1. GUEST - Sin login, datos locales (se pierden al desinstalar)
 * 2. LOCAL - Con cuenta, datos sincronizados con Firebase
 * 3. ADMIN - Desarrollador, acceso a features especiales
 */
class AuthRepository(private val context: Context) {

    private val TAG = "AuthRepository"

    // Estado actual del usuario
    private val _currentUser = MutableStateFlow<UserPreferences?>(null)
    val currentUser: Flow<UserPreferences?> = _currentUser.asStateFlow()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: Flow<AuthState> = _authState.asStateFlow()

    /**
     * Inicializar - cargar usuario actual de DataStore
     */
    suspend fun initialize() {
        try {
            // TODO: Cargar de DataStore
            // Por ahora, crear usuario guest
            val guestUser = createGuestUser()
            _currentUser.value = guestUser
            _authState.value = AuthState.Success(guestUser)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing auth", e)
            _authState.value = AuthState.Error("Failed to initialize")
        }
    }

    /**
     * Login con email y password
     * Verifica primero si es admin, luego intenta Firebase
     */
    suspend fun login(email: String, password: String): AuthResult {
        return try {
            _authState.value = AuthState.Loading

            // ✅ Verificar si es cuenta admin
            if (email == AdminCredentials.ADMIN_EMAIL && password == AdminCredentials.ADMIN_PASSWORD) {
                val adminUser = UserPreferences(
                    userId = "admin_${UUID.randomUUID()}",
                    userName = AdminCredentials.ADMIN_USERNAME,
                    userEmail = AdminCredentials.ADMIN_EMAIL,
                    isLoggedIn = true,
                    isAdmin = true,
                    developerMode = true,
                    showDebugInfo = true,
                    allowExperimentalFeatures = true
                )

                _currentUser.value = adminUser
                _authState.value = AuthState.Success(adminUser)

                Log.d(TAG, "✅ Admin login successful")
                return AuthResult.Success(adminUser)
            }

            // TODO: Intentar login con Firebase
            // Por ahora, simular login local
            val user = UserPreferences(
                userId = UUID.randomUUID().toString(),
                userName = email.substringBefore("@"),
                userEmail = email,
                isLoggedIn = true,
                isAdmin = false
            )

            _currentUser.value = user
            _authState.value = AuthState.Success(user)

            Log.d(TAG, "✅ User login successful: ${user.userName}")
            AuthResult.Success(user)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Login error", e)
            _authState.value = AuthState.Error(e.message ?: "Login failed")
            AuthResult.Error(e.message ?: "Login failed")
        }
    }

    /**
     * Registro de nueva cuenta
     */
    suspend fun register(email: String, password: String, userName: String): AuthResult {
        return try {
            _authState.value = AuthState.Loading

            // TODO: Crear cuenta en Firebase
            // Por ahora, crear cuenta local
            val user = UserPreferences(
                userId = UUID.randomUUID().toString(),
                userName = userName,
                userEmail = email,
                isLoggedIn = true,
                isAdmin = false
            )

            _currentUser.value = user
            _authState.value = AuthState.Success(user)

            Log.d(TAG, "✅ User registered: $userName")
            AuthResult.Success(user)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Registration error", e)
            _authState.value = AuthState.Error(e.message ?: "Registration failed")
            AuthResult.Error(e.message ?: "Registration failed")
        }
    }

    /**
     * Logout - volver a modo guest
     */
    suspend fun logout() {
        try {
            Log.d(TAG, "🔓 Logging out user: ${_currentUser.value?.userName}")

            // TODO: Limpiar Firebase session

            // Crear nuevo usuario guest
            val guestUser = createGuestUser()
            _currentUser.value = guestUser
            _authState.value = AuthState.Success(guestUser)

            Log.d(TAG, "✅ Logged out, now in guest mode")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Logout error", e)
        }
    }

    /**
     * Sincronizar datos con Firebase
     * Solo si el usuario está logueado
     */
    suspend fun syncData(): SyncResult {
        val user = _currentUser.value

        if (user == null || !user.isLoggedIn) {
            return SyncResult.NotLoggedIn
        }

        return try {
            Log.d(TAG, "🔄 Syncing data for user: ${user.userName}")

            // TODO: Sincronizar con Firebase
            // - Playlists
            // - Favorites
            // - Recently played
            // - Statistics

            SyncResult.Success

        } catch (e: Exception) {
            Log.e(TAG, "❌ Sync error", e)
            SyncResult.Error(e.message ?: "Sync failed")
        }
    }

    /**
     * Actualizar perfil de usuario
     */
    suspend fun updateProfile(userName: String? = null, profileImageUrl: String? = null): Boolean {
        val currentUser = _currentUser.value ?: return false

        val updatedUser = currentUser.copy(
            userName = userName ?: currentUser.userName,
            profileImageUrl = profileImageUrl ?: currentUser.profileImageUrl
        )

        _currentUser.value = updatedUser
        _authState.value = AuthState.Success(updatedUser)

        // TODO: Guardar en DataStore
        // TODO: Sincronizar con Firebase si está logueado

        return true
    }

    /**
     * Verificar si el usuario es admin
     */
    fun isAdmin(): Boolean {
        return _currentUser.value?.isAdmin == true
    }

    /**
     * Verificar si el usuario está logueado
     */
    fun isLoggedIn(): Boolean {
        return _currentUser.value?.isLoggedIn == true
    }

    /**
     * Crear usuario guest por defecto
     */
    private fun createGuestUser(): UserPreferences {
        return UserPreferences(
            userId = null,
            userName = "Guest",
            userEmail = null,
            isLoggedIn = false,
            isAdmin = false
        )
    }
}

/**
 * Estados de autenticación
 */
sealed class AuthState {
    object Loading : AuthState()
    data class Success(val user: UserPreferences) : AuthState()
    data class Error(val message: String) : AuthState()
}

/**
 * Resultado de operaciones de auth
 */
sealed class AuthResult {
    data class Success(val user: UserPreferences) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

/**
 * Resultado de sincronización
 */
sealed class SyncResult {
    object Success : SyncResult()
    object NotLoggedIn : SyncResult()
    data class Error(val message: String) : SyncResult()
}