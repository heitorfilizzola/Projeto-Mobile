package com.filizzola.projeto_mobile.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.filizzola.projeto_mobile.data.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class RegisterViewModel(application: Application) : AndroidViewModel(application) {

    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _confirmPassword = MutableStateFlow("")
    val confirmPassword: StateFlow<String> = _confirmPassword.asStateFlow()

    private val _registrationState = MutableStateFlow<RegistrationState>(RegistrationState.Idle)
    val registrationState: StateFlow<RegistrationState> = _registrationState.asStateFlow()

    fun onUsernameChange(username: String) {
        _username.value = username
    }

    fun onEmailChange(email: String) {
        _email.value = email
    }

    fun onPasswordChange(password: String) {
        _password.value = password
    }

    fun onConfirmPasswordChange(confirmPassword: String) {
        _confirmPassword.value = confirmPassword
    }

    fun register() {
        val areFieldsBlank = _username.value.isBlank() || _email.value.isBlank() || _password.value.isBlank()
        val doPasswordsMatch = _password.value == _confirmPassword.value

        if (areFieldsBlank) {
            _registrationState.value = RegistrationState.Error("Por favor, preencha todos os campos.")
            return
        }

        if (!doPasswordsMatch) {
            _registrationState.value = RegistrationState.Error("As senhas não conferem!")
            return
        }

        viewModelScope.launch {
            _registrationState.value = RegistrationState.Loading
            try {
                UserRepository.createUser(
                    newUsername = _username.value,
                    newEmail = _email.value,
                    newPassword = _password.value
                )
                val userFile = File(getApplication<Application>().filesDir, "users.json")
                UserRepository.saveUsersToFile(userFile)
                _registrationState.value = RegistrationState.Success
            } catch (e: Exception) {
                _registrationState.value = RegistrationState.Error(e.message ?: "Ocorreu um erro durante o registro.")
            }
        }
    }
}

sealed class RegistrationState {
    object Idle : RegistrationState()
    object Loading : RegistrationState()
    object Success : RegistrationState()
    data class Error(val message: String) : RegistrationState()
}
