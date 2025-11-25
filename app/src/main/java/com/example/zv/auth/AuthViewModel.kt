package com.example.zv.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Обертка для пользователя, которая может быть либо FirebaseUser, либо Guest
sealed class AppUser {
    data class Firebase(val user: FirebaseUser) : AppUser()
    object Guest : AppUser()
    
    val isAnonymous: Boolean
        get() = this is Guest
    
    val uid: String
        get() = when (this) {
            is Firebase -> user.uid
            is Guest -> "guest_user"
        }
}

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()
    
    private val _appUser = MutableStateFlow<AppUser?>(null)
    val appUser: StateFlow<AppUser?> = _appUser.asStateFlow()
    
    // Флаг для отслеживания гостевого режима
    private var isGuestMode = false
    
    init {
        // Проверяем текущего пользователя при инициализации
        val firebaseUser = auth.currentUser
        _currentUser.value = firebaseUser
        if (firebaseUser != null) {
            _appUser.value = AppUser.Firebase(firebaseUser)
            _authState.value = AuthState.Authenticated
        } else {
            _authState.value = AuthState.Unauthenticated
        }
        
        auth.addAuthStateListener { firebaseAuth ->
            if (!isGuestMode) {
                val user = firebaseAuth.currentUser
                _currentUser.value = user
                _appUser.value = user?.let { AppUser.Firebase(it) }
                _authState.value = if (user != null) {
                    AuthState.Authenticated
                } else {
                    AuthState.Unauthenticated
                }
            }
        }
    }
    
    fun signIn(email: String, password: String, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                isGuestMode = false
                _authState.value = AuthState.Loading
                val result = auth.signInWithEmailAndPassword(email, password).await()
                _currentUser.value = result.user
                _appUser.value = result.user?.let { AppUser.Firebase(it) }
            } catch (e: Exception) {
                _authState.value = AuthState.Unauthenticated
                onError(e.message ?: "Ошибка входа")
            }
        }
    }
    
    fun signUp(email: String, password: String, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                isGuestMode = false
                _authState.value = AuthState.Loading
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                _currentUser.value = result.user
                _appUser.value = result.user?.let { AppUser.Firebase(it) }
            } catch (e: Exception) {
                _authState.value = AuthState.Unauthenticated
                onError(e.message ?: "Ошибка регистрации")
            }
        }
    }
    
    fun signInAsGuest(onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                // Используем локальный гостевой режим вместо Firebase анонимной аутентификации
                isGuestMode = true
                _authState.value = AuthState.Loading
                
                // Создаем гостевого пользователя
                _appUser.value = AppUser.Guest
                _currentUser.value = null
                _authState.value = AuthState.Authenticated
            } catch (e: Exception) {
                isGuestMode = false
                _authState.value = AuthState.Unauthenticated
                onError(e.message ?: "Ошибка входа как гость")
            }
        }
    }
    
    fun signOut() {
        isGuestMode = false
        _appUser.value = null
        if (_currentUser.value != null) {
            auth.signOut()
        }
        _currentUser.value = null
        _authState.value = AuthState.Unauthenticated
    }
}

sealed class AuthState {
    object Loading : AuthState()
    object Authenticated : AuthState()
    object Unauthenticated : AuthState()
}
