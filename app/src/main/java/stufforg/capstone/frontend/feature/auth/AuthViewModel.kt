package stufforg.capstone.frontend.feature.auth

import androidx.compose.runtime.Immutable
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import stufforg.capstone.frontend.core.data.StuffOrgRepository
import stufforg.capstone.frontend.stuffOrgRepository

@Immutable
data class AuthUiState(
    val isCreateAccount: Boolean = false,
    val identifier: String = "",
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val repeatPassword: String = "",
    val rememberMe: Boolean = true,
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    val passwordStrengthScore: Int
        get() {
            if (password.isEmpty()) return 0
            var score = 0
            if (password.length >= 8) score++
            if (password.length >= 12) score++
            if (password.any { it.isDigit() } && password.any { it.isLetter() }) score++
            if (password.any { !it.isLetterOrDigit() } || password.any { it.isUpperCase() }) score++
            return score.coerceIn(1, 4)
        }

    val passwordsMatch: Boolean
        get() = password.isNotEmpty() && password == repeatPassword
}

class AuthViewModel(
    private val repository: StuffOrgRepository,
    initialCreateAccount: Boolean = false,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState(isCreateAccount = initialCreateAccount))
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun setCreateAccountMode(createAccount: Boolean) {
        _uiState.update { it.copy(isCreateAccount = createAccount, errorMessage = null) }
    }

    fun onIdentifierChange(value: String) {
        _uiState.update { it.copy(identifier = value, errorMessage = null) }
    }

    fun onUsernameChange(value: String) {
        _uiState.update { it.copy(username = value, errorMessage = null) }
    }

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, errorMessage = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun onRepeatPasswordChange(value: String) {
        _uiState.update { it.copy(repeatPassword = value, errorMessage = null) }
    }

    fun onRememberMeChange(value: Boolean) {
        _uiState.update { it.copy(rememberMe = value) }
    }

    fun onTogglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun submitEmailAuth(onAuthenticated: () -> Unit) {
        val state = _uiState.value
        if (state.isLoading) return

        if (state.isCreateAccount) {
            if (state.username.isBlank() || state.email.isBlank() || state.password.isBlank()) {
                _uiState.update { it.copy(errorMessage = "Please fill in username, email, and password.") }
                return
            }
            if (!state.email.contains("@")) {
                _uiState.update { it.copy(errorMessage = "Please enter a valid email address.") }
                return
            }
            if (state.password != state.repeatPassword) {
                _uiState.update { it.copy(errorMessage = "Passwords do not match.") }
                return
            }
        } else {
            if (state.identifier.isBlank() || state.password.isBlank()) {
                _uiState.update { it.copy(errorMessage = "Please enter your email or username and password.") }
                return
            }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = if (state.isCreateAccount) {
                repository.register(
                    username = state.username,
                    email = state.email,
                    password = state.password,
                    repeatPassword = state.repeatPassword,
                )
            } else {
                repository.login(
                    identifier = state.identifier,
                    password = state.password,
                    savePassword = state.rememberMe,
                )
            }

            result
                .onSuccess {
                    _uiState.update { s -> s.copy(isLoading = false) }
                    onAuthenticated()
                }
                .onFailure { err ->
                    _uiState.update { s ->
                        s.copy(
                            isLoading = false,
                            errorMessage = err.message ?: "Authentication failed",
                        )
                    }
                }
        }
    }

    fun submitGoogleIdToken(
        tokenResult: Result<String>,
        onAuthenticated: () -> Unit,
    ) {
        tokenResult
            .onSuccess { idToken ->
                viewModelScope.launch {
                    _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                    repository.loginWithGoogle(idToken)
                        .onSuccess {
                            _uiState.update { s -> s.copy(isLoading = false) }
                            onAuthenticated()
                        }
                        .onFailure { err ->
                            _uiState.update { s ->
                                s.copy(
                                    isLoading = false,
                                    errorMessage = err.message ?: "Google Sign-In failed",
                                )
                            }
                        }
                }
            }
            .onFailure { err ->
                if (err !is GetCredentialCancellationException) {
                    _uiState.update { it.copy(errorMessage = err.message) }
                }
            }
    }

    companion object {
        fun factory(initialCreateAccount: Boolean): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                AuthViewModel(
                    repository = stuffOrgRepository(),
                    initialCreateAccount = initialCreateAccount,
                )
            }
        }
    }
}

