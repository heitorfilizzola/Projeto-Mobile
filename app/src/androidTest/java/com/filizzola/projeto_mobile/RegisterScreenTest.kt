package com.filizzola.projeto_mobile

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.filizzola.projeto_mobile.ui.RegisterScreenContent
import com.filizzola.projeto_mobile.ui.theme.ProjetoMobileTheme
import org.junit.Rule
import org.junit.Test

class RegisterScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()
    private fun launchRegisterScreen(
        username: String = "",
        email: String = "",
        password: String = "",
        confirmPassword: String = "",
        isLoading: Boolean = false,
        onRegisterClick: () -> Unit = {},
        onBackClick: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            ProjetoMobileTheme {
                RegisterScreenContent(
                    username = username,
                    email = email,
                    password = password,
                    confirmPassword = confirmPassword,
                    onUsernameChange = {},
                    onEmailChange = {},
                    onPasswordChange = {},
                    onConfirmPasswordChange = {},
                    onRegisterClick = onRegisterClick,
                    onBackClick = onBackClick,
                    isLoading = isLoading
                )
            }
        }
    }

    @Test
    fun verificaElementosVisuais() {
        launchRegisterScreen()

        composeTestRule.onNodeWithTag("register_title").assertIsDisplayed()

        composeTestRule.onNodeWithTag("username_field").assertIsDisplayed()
        composeTestRule.onNodeWithTag("email_field").assertIsDisplayed()
        composeTestRule.onNodeWithTag("password_field").assertIsDisplayed()
        composeTestRule.onNodeWithTag("confirm_password_field").assertIsDisplayed()

        composeTestRule.onNodeWithTag("register_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("back_button").assertIsDisplayed()
    }

    @Test
    fun verificaPreenchimentoDeFormulario() {
        launchRegisterScreen(
            username = "Heitor",
            email = "heitor@email.com"
        )

        composeTestRule.onNodeWithTag("username_field").assertExists()

        composeTestRule.onNodeWithTag("password_field").performClick()
    }

    @Test
    fun verificaCliqueBotaoRegistrar() {
        var clicouRegistrar = false
        launchRegisterScreen(
            onRegisterClick = { clicouRegistrar = true }
        )

        composeTestRule.onNodeWithTag("register_button").performClick()
        assert(clicouRegistrar)
    }

    @Test
    fun verificaCliqueBotaoVoltar() {
        var clicouVoltar = false
        launchRegisterScreen(
            onBackClick = { clicouVoltar = true }
        )

        composeTestRule.onNodeWithTag("back_button").performClick()
        assert(clicouVoltar)
    }

    @Test
    fun verificaBotaoDesabilitadoDuranteLoading() {
        launchRegisterScreen(isLoading = true)

        composeTestRule.onNodeWithTag("register_button")
            .assertIsDisplayed()
            .assertIsNotEnabled()
    }
}