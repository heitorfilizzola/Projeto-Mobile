package com.filizzola.projeto_mobile.ui

import android.widget.Toast
import androidx.activity.result.launch
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.filizzola.projeto_mobile.data.UserRepository
import com.filizzola.projeto_mobile.ui.theme.ProjetoMobileTheme
import kotlinx.coroutines.launch
import java.io.File


import androidx.lifecycle.viewmodel.compose.viewModel
import com.filizzola.projeto_mobile.viewmodel.RegisterViewModel
import com.filizzola.projeto_mobile.viewmodel.RegistrationState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect

@Composable
fun RegisterScreen(
    modifier: Modifier = Modifier,
    onRegisterClick: () -> Unit,
    onLoginBtnClick: () -> Unit,
    registerViewModel: RegisterViewModel = viewModel()
) {
    val usernameInput by registerViewModel.username.collectAsState()
    val emailInput by registerViewModel.email.collectAsState()
    val passInput by registerViewModel.password.collectAsState()
    val passConfirmInput by registerViewModel.confirmPassword.collectAsState()
    val registrationState by registerViewModel.registrationState.collectAsState()

    val borderGray = Color(0xFFCACACA)
    val context = LocalContext.current
    val appIcons = Icons.Outlined
    val bgColor = MaterialTheme.colorScheme.background

    LaunchedEffect(registrationState) {
        when (val state = registrationState) {
            is RegistrationState.Success -> {
                Toast.makeText(context, "Usuário registrado com sucesso!", Toast.LENGTH_SHORT).show()
                Toast.makeText(context, "Verifique sua caixa de entrada para ativar sua conta.", Toast.LENGTH_LONG).show()
                onRegisterClick()
            }
            is RegistrationState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
            }
            else -> {}
        }
    }

    bgImage(
        modifier = Modifier
            .fillMaxSize()
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .fillMaxWidth()
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = bgColor,
            modifier = Modifier
//                .fillMaxHeight(0.76f)
                .fillMaxWidth(0.85f)
                .shadow(8.dp)
                .border(width = 6.dp, color = borderGray, shape = RoundedCornerShape(65.dp)),
            shape = RoundedCornerShape(65.dp),
        ) {
            Box(
                modifier = Modifier
                    .verticalScroll(rememberScrollState()),
            ) {
                IconButton(
                    onClick = onLoginBtnClick,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 24.dp, top = 24.dp)
                ) {
                    Icon(
                        imageVector = appIcons.ArrowBackIosNew,
                        contentDescription = null
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 40.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Register",
                        modifier = modifier
                            .padding(bottom = 32.dp, top = 32.dp),
                        fontSize = 30.sp
                    )

                    RegisterField(
                        label = "Username",
                        leadingIcon = {
                            Icon(
                                imageVector = appIcons.AccountCircle,
                                contentDescription = "Email icon"
                            )
                        },
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        value = usernameInput,
                        onValueChanged = { registerViewModel.onUsernameChange(it) },
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .fillMaxWidth()
                    )

                    LoginField(
                        label = "E-Mail",
                        leadingIcon = {
                            Icon(
                                imageVector = appIcons.Email,
                                contentDescription = "Email icon"
                            )
                        },
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        value = emailInput,
                        onValueChanged = { registerViewModel.onEmailChange(it) },
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .fillMaxWidth()
                    )

                    RegisterField(
                        label = "Password",
                        leadingIcon = {
                            Icon(
                                imageVector = appIcons.Lock,
                                contentDescription = "Padlock icon"
                            )
                        },
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        value = passInput,
                        onValueChanged = { registerViewModel.onPasswordChange(it) },
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .fillMaxWidth()
                    )

                    RegisterField(
                        label = "Confirm Password",
                        leadingIcon = {
                            Icon(
                                imageVector = appIcons.Lock,
                                contentDescription = "Padlock icon"
                            )
                        },
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Send
                        ),
                        value = passConfirmInput,
                        onValueChanged = { registerViewModel.onConfirmPasswordChange(it) },
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .fillMaxWidth()
                    )

                    Button(
                        onClick = { registerViewModel.register() },
                        modifier = modifier.padding(16.dp),
                        enabled = registrationState != RegistrationState.Loading
                    ) {
                        Text(text = "Register", fontSize = 24.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun RegisterField(
    label: String,
    leadingIcon: @Composable (() -> Unit)?,
    keyboardOptions: KeyboardOptions,
    value: String,
    modifier: Modifier = Modifier,
    onValueChanged: (String) -> Unit
){
    OutlinedTextField(
        value = value,
        singleLine = true,
        leadingIcon = leadingIcon,
        modifier = modifier,
        onValueChange = onValueChanged,
        label = {Text(label)},
        keyboardOptions = keyboardOptions
    )
}


@Preview(showBackground = true)
@Composable
fun registerPreview() {
    ProjetoMobileTheme {
        RegisterScreen(
            modifier = Modifier,
            onRegisterClick = {},
            onLoginBtnClick = {}
        )
    }
}