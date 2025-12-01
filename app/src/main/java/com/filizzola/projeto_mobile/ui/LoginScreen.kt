package com.filizzola.projeto_mobile.ui

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.filizzola.projeto_mobile.R
import com.filizzola.projeto_mobile.ui.theme.ProjetoMobileTheme
import com.filizzola.projeto_mobile.viewmodel.LoginState
import com.filizzola.projeto_mobile.viewmodel.LoginViewModel

/**
 * Composable for displaying the background image of the screen.
 */
@Composable
fun bgImage(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.login_pg_bg),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.fillMaxSize()
    )
}

/**
 * The main login screen composable. It handles user input,
 * authentication state, and navigation.
 */
@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    onLoginSuccess: (String) -> Unit,
    onRegisterClick: () -> Unit,
    loginViewModel: LoginViewModel = viewModel()
) {
    val emailInput by loginViewModel.email.collectAsState()
    val passInput by loginViewModel.password.collectAsState()
    val loginState by loginViewModel.loginState.collectAsState()

    val context = LocalContext.current

    // Effect to handle side-effects of login state changes (e.g., showing toasts, navigating).
    LaunchedEffect(loginState) {
        when (val state = loginState) {
            is LoginState.Success -> {
                Toast.makeText(context, "Login bem-sucedido!", Toast.LENGTH_SHORT).show()
                Log.d("LoginScreen", "Login successful for user: ${state.user.id}")
                onLoginSuccess(state.user.id) // Pass only the user ID on success.
            }
            is LoginState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
            }
            else -> {
                // Handle other states like Loading or Idle if necessary.
            }
        }
    }

    // UI Layout
    val borderGray = Color(0xFFCACACA)
    val appIcons = Icons.Outlined
    val bgColor = MaterialTheme.colorScheme.background

    bgImage()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = bgColor,
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .shadow(8.dp)
                .border(width = 6.dp, color = borderGray, shape = RoundedCornerShape(65.dp)),
            shape = RoundedCornerShape(65.dp),
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 40.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "NoteSync",
                    modifier = modifier.padding(vertical = 32.dp),
                    fontSize = 30.sp
                )

                LoginField(
                    label = "E-Mail",
                    leadingIcon = { Icon(appIcons.Email, "Email icon") },
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    value = emailInput,
                    onValueChanged = { loginViewModel.onEmailChange(it) },
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .fillMaxWidth()
                        .testTag("email_field")
                )

                LoginField(
                    label = "Password",
                    leadingIcon = { Icon(appIcons.Lock, "Padlock icon") },
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Send
                    ),
                    value = passInput,
                    onValueChanged = { loginViewModel.onPasswordChange(it) },
                    isPassword = true,
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .fillMaxWidth()
                        .testTag("password_field")
                )

                Button(
                    onClick = { loginViewModel.login() },
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .testTag("login_button"),
                    enabled = loginState !is LoginState.Loading
                ) {
                    Text(text = "Login", fontSize = 24.sp)
                }

                Text("Não tem uma conta? ", modifier = Modifier.padding(top = 24.dp))

                Text(
                    modifier = Modifier
                        .clickable { onRegisterClick() }
                        .padding(bottom = 32.dp)
                        .testTag("register_text"),
                    text = buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        ) {
                            append("Registre-se")
                        }
                    }
                )
            }
        }
    }
}

/**
 * A standardized text field for the login screen.
 */
@Composable
fun LoginField(
    label: String,
    leadingIcon: @Composable (() -> Unit)?,
    keyboardOptions: KeyboardOptions,
    value: String,
    onValueChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false
) {
    OutlinedTextField(
        value = value,
        singleLine = true,
        leadingIcon = leadingIcon,
        modifier = modifier,
        onValueChange = onValueChanged,
        label = { Text(label) },
        keyboardOptions = keyboardOptions,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None
    )
}

/**
 * Preview for the LoginScreen.
 */
@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    ProjetoMobileTheme {
        LoginScreen(
            onLoginSuccess = {},
            onRegisterClick = {}
        )
    }
}
