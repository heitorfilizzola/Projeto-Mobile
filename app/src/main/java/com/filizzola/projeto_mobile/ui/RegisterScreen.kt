package com.filizzola.projeto_mobile.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RegisterScreen(modifier: Modifier = Modifier, onRegisterClick: () -> Unit) {
    var usernameInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var passInput by remember { mutableStateOf("")}
    var passConfirmInput by remember { mutableStateOf("") }
    var borderGray = Color(0xFFCACACA)

    val appIcons = Icons.Outlined
    val bgColor = MaterialTheme.colorScheme.background

    bgImage(
        modifier = Modifier
            .fillMaxSize()
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = bgColor,
            modifier = Modifier
                .fillMaxHeight(0.75f)
                .fillMaxWidth(0.85f)
                .shadow(8.dp)
                .border(width = 6.dp, color = borderGray, shape = RoundedCornerShape(65.dp)),
            shape = RoundedCornerShape(65.dp),
        ) {
            Column(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 40.dp),
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
                    onValueChanged = { usernameInput = it },
                    modifier = Modifier.padding(bottom = 16.dp).fillMaxWidth()
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
                    onValueChanged = { emailInput = it },
                    modifier = Modifier.padding(bottom = 16.dp).fillMaxWidth()
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
                        imeAction = ImeAction.Send
                    ),
                    value = passInput,
                    onValueChanged = { passInput = it },
                    modifier = Modifier.padding(bottom = 16.dp).fillMaxWidth()
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
                    onValueChanged = { passConfirmInput = it },
                    modifier = Modifier.padding(bottom = 16.dp).fillMaxWidth()
                )

                Button(
                    onClick = onRegisterClick,
                    modifier = modifier
                        .padding(16.dp)
                ) {
                    Text(text = "Register", fontSize = 24.sp)
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
    TextField(
        value = value,
        singleLine = true,
        leadingIcon = leadingIcon,
        modifier = modifier,
        onValueChange = onValueChanged,
        label = {Text(label)},
        keyboardOptions = keyboardOptions
    )
}
