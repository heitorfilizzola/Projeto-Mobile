package com.filizzola.projeto_mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
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
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.filizzola.projeto_mobile.ui.theme.ProjetoMobileTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ProjetoMobileTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Surface(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        GreetingLogin(
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun bgImage(modifier: Modifier = Modifier){
    val image = painterResource(R.drawable.login_pg_bg)
    Image(
        painter = image,
        contentDescription = null,
        contentScale = ContentScale.Crop,
    )
}

@Composable
fun GreetingLogin( modifier: Modifier = Modifier) {
    var emailInput by remember { mutableStateOf("") }
    var passInput by remember { mutableStateOf("")}

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
                .fillMaxHeight(0.65f)
                .fillMaxWidth(0.85f)
                .shadow(8.dp),
            shape = RoundedCornerShape(24.dp),
        ) {
            Column(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "NoteSync",
                    modifier = modifier
                        .padding(bottom = 32.dp, top = 32.dp),
                    fontSize = 30.sp
                )

                LoginField(
                    label = "E-Mail",
                    leadingIcon = R.drawable.mail,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    value = emailInput,
                    onValueChanged = { emailInput = it },
                    modifier = Modifier.padding(bottom = 16.dp).fillMaxWidth()
                )

                LoginField(
                    label = "Password",
                    leadingIcon = R.drawable.lock,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Send
                    ),
                    value = passInput,
                    onValueChanged = { passInput = it },
                    modifier = Modifier.padding(bottom = 16.dp).fillMaxWidth()
                )

                Button(
                    onClick = {},
                    modifier = modifier
                        .padding(bottom = 16.dp)
                ) {
                    Text(text = "Login", fontSize = 24.sp)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ProjetoMobileTheme {
        GreetingLogin()
    }
}

@Composable
fun LoginField(
    label: String,
    @DrawableRes leadingIcon: Int,
    keyboardOptions: KeyboardOptions,
    value: String,
    modifier: Modifier = Modifier,
    onValueChanged: (String) -> Unit
    ){
    TextField(
        value = value,
        singleLine = true,
        leadingIcon = {Icon(painter = painterResource(id = leadingIcon), null)},
        modifier = modifier,
        onValueChange = onValueChanged,
        label = {Text(label)},
        keyboardOptions = keyboardOptions
    )
}