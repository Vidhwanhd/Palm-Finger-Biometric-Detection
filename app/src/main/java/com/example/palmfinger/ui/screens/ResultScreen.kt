package com.example.palmfinger.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.palmfinger.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    navController: NavController,
    viewModel: MainViewModel = viewModel()
) {

    val brightness by viewModel.brightness.collectAsState()
    val lightType by viewModel.lightType.collectAsState()
    val blurScore by viewModel.blurScore.collectAsState()
    val handSide by viewModel.handSide.collectAsState()
    val fingerCount by viewModel.fingerCount.collectAsState()
    val deviceId by viewModel.deviceId.collectAsState()

    val isSuccess = fingerCount == 5

    Scaffold(
        containerColor = Color(0xFF0D1117),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Biometric Scan Report",
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black
                )
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Status Badge
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isSuccess)
                        Color(0xFF0F5132)
                    else
                        Color(0xFF842029)
                )
            ) {
                Text(
                    text = if (isSuccess) "✔ SCAN SUCCESSFUL" else "✖ SCAN INCOMPLETE",
                    color = Color.White,
                    modifier = Modifier.padding(12.dp)
                )
            }

            // Report Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF161B22)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    ReportItem("Device ID", deviceId)
                    ReportItem("Brightness Score", "$brightness ($lightType)")
                    ReportItem("Blur Score", blurScore.toString())
                    ReportItem("Detected Hand", handSide)
                    ReportItem("Fingers Scanned", "$fingerCount / 5")
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            Button(
                onClick = {
                    viewModel.resetFingerCount()
                    navController.navigate("palm") {
                        popUpTo("palm") { inclusive = true }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF238636)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start New Scan")
            }
        }
    }
}

@Composable
fun ReportItem(title: String, value: String) {

    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = Color.Gray
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White
        )
        Divider(color = Color.DarkGray)
    }
}
