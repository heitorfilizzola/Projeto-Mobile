package com.filizzola.projeto_mobile.ui

import android.util.Log
import android.widget.Toast
import androidx.activity.result.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.filizzola.projeto_mobile.R
import com.filizzola.projeto_mobile.data.User
import com.filizzola.projeto_mobile.data.UserRepository
import com.filizzola.projeto_mobile.ui.theme.ProjetoMobileTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

/**
 * Composable para exibir a imagem de fundo da tela.
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


@Composable
fun GreetingLogin(
    modifier: Modifier = Modifier,
    onLoginSuccess: (User) -> Unit,
    onNavigateToRegister: () -> Unit,
    onClickTaski: () -> Unit
) {
    var emailInput by remember { mutableStateOf("") }
    var passInput by remember { mutableStateOf("") }
    val borderGray = Color(0xFFCACACA)

    val context = LocalContext.current
    val appIcons = Icons.Outlined
    val bgColor = MaterialTheme.colorScheme.background

    val scope = rememberCoroutineScope()

    bgImage()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .fillMaxWidth()
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = bgColor,
            modifier = Modifier
//                .fillMaxHeight(0.60f)
                .fillMaxWidth(0.85f)
                .shadow(8.dp)
                .border(width = 6.dp, color = borderGray, shape = RoundedCornerShape(65.dp)),
            shape = RoundedCornerShape(65.dp),
        ) {
            Column(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 40.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
//                verticalArrangement = Arrangement.Center
            ) {

                Text(
                    text = "NoteSync",
                    modifier = modifier.padding(bottom = 32.dp, top = 32.dp),
                    fontSize = 30.sp
                )

                LoginField(
                    label = "E-Mail",
                    leadingIcon = { Icon(imageVector = appIcons.Email, contentDescription = "Email icon") },
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    value = emailInput,
                    onValueChanged = { emailInput = it },
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .fillMaxWidth()
                )

                LoginField(
                    label = "Password",
                    leadingIcon = { Icon(imageVector = appIcons.Lock, contentDescription = "Padlock icon") },
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Send
                    ),
                    value = passInput,
                    onValueChanged = { passInput = it },
                    isPassword = true,
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .fillMaxWidth()
                )

                Button(
                    onClick = {
                        if (emailInput.isBlank() || passInput.isBlank()) {
                            Toast.makeText(context, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
                        } else {
                            scope.launch {
                            val loggedInUser = UserRepository.login(emailInput, passInput)
                            if (loggedInUser != null) {
                                Toast.makeText(context, "Login bem-sucedido!", Toast.LENGTH_SHORT).show()
                                Log.d("loginDone", "Login com sucesso do usuario $loggedInUser")
                                onLoginSuccess(loggedInUser)
                            } else {
                                Toast.makeText(context, "Email ou senha incorretos.", Toast.LENGTH_SHORT).show()
                            }
                        }}
                    },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text(text = "Login", fontSize = 24.sp)
                }

                    Text("Não tem uma conta? ",
                        modifier = Modifier.padding(top = 24.dp)) // <-- Texto 1
                    Text(                     // <-- Texto 2
                        modifier = Modifier
                            .clickable { onNavigateToRegister() }
                            .padding(bottom = 32.dp),
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

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ProjetoMobileTheme {
        GreetingLogin(
            onLoginSuccess = {},
            onNavigateToRegister = {},
            onClickTaski = {}
        )
    }
}