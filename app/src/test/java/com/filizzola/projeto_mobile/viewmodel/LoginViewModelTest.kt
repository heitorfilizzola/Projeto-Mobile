package com.filizzola.projeto_mobile.viewmodel

import android.app.Application
import com.filizzola.projeto_mobile.data.User
import com.filizzola.projeto_mobile.data.UserRepository
import com.filizzola.projeto_mobile.utils.LoginManager
import com.filizzola.projeto_mobile.utils.MainDispatcherRule
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: LoginViewModel
    private val application = mockk<Application>(relaxed = true)

    @Before
    fun setup() {
        // Mock do Singleton UserRepository
        mockkObject(UserRepository)

        // Mock da construção do LoginManager (pois é criado dentro do ViewModel)
        mockkConstructor(LoginManager::class)
        coEvery { anyConstructed<LoginManager>().saveLoggedUser(any()) } just Runs

        viewModel = LoginViewModel(application)
    }

    @Test
    fun `onEmailChange deve atualizar o estado do email`() {
        viewModel.onEmailChange("teste@email.com")
        assertEquals("teste@email.com", viewModel.email.value)
    }

    @Test
    fun `login deve falhar se campos estiverem vazios`() {
        viewModel.onEmailChange("")
        viewModel.onPasswordChange("")

        viewModel.login()

        assertTrue(viewModel.loginState.value is LoginState.Error)
        assertEquals("Preencha todos os campos", (viewModel.loginState.value as LoginState.Error).message)
    }

    @Test
    fun `login com sucesso deve atualizar estado para Success`() = runTest {
        // Arrange
        val email = "user@test.com"
        val password = "password"
        val mockUser = User(id = "123", username = "Test User", email = email, uTaskList = arrayListOf())

        viewModel.onEmailChange(email)
        viewModel.onPasswordChange(password)

        coEvery { UserRepository.login(email, password) } returns mockUser

        // Act
        viewModel.login()

        // Assert
        assertTrue(viewModel.loginState.value is LoginState.Success)
        assertEquals(mockUser, (viewModel.loginState.value as LoginState.Success).user)

        // Verifica se o LoginManager salvou o utilizador
        coVerify { anyConstructed<LoginManager>().saveLoggedUser("123") }
    }

    @Test
    fun `login com credenciais erradas deve retornar Error`() = runTest {
        // Arrange
        val email = "wrong@test.com"
        val password = "wrong"

        viewModel.onEmailChange(email)
        viewModel.onPasswordChange(password)

        // Simulamos que o UserRepository retorna null (login falhou)
        coEvery { UserRepository.login(email, password) } returns null

        // Act
        viewModel.login()

        // Assert
        assertTrue(viewModel.loginState.value is LoginState.Error)
        assertEquals("Email ou senha incorretos.", (viewModel.loginState.value as LoginState.Error).message)
    }
}