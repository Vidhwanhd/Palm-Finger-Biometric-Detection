package com.example.palmfinger.ui.screens

import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.example.palmfinger.camera.BlurDetector
import com.example.palmfinger.camera.CameraManager
import com.example.palmfinger.camera.LuminosityAnalyzer
import com.example.palmfinger.detection.*
import com.example.palmfinger.utils.StorageUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*


@Composable
fun FingerScreen(
    navController: NavController,
    storedHand: String
) {

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var fingerIndex by remember { mutableStateOf(0) }
    val totalFingers = 5

    var lightType by remember { mutableStateOf("Normal") }
    var isProcessing by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    val fingerNames = listOf(
        "Thumb", "Index", "Middle", "Ring", "Little"
    )

    val analyzer = remember {
        LuminosityAnalyzer(context) { result ->
            lightType = result.lightType
        }
    }

    val previewView = remember { PreviewView(context) }

    val cameraManager = remember {
        CameraManager(context, lifecycleOwner, previewView, analyzer)
    }

    val detector = remember { HandDetector(context) }
    val extractor = remember { MinutiaeExtractor() }

    LaunchedEffect(Unit) {
        cameraManager.startCamera()
    }

    DisposableEffect(Unit) {
        onDispose {
            detector.close()
            cameraManager.shutdown()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Black
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
            )

            // Scanner Frame
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    border = BorderStroke(4.dp, Color.Yellow),
                    color = Color.Transparent
                ) {}
            }

            // Status Card
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 40.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF161B22)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        text = "Scanning: ${fingerNames.getOrElse(fingerIndex) { "Completed" }}",
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Progress: ${minOf(fingerIndex + 1, totalFingers)} / $totalFingers",
                        color = Color.Gray
                    )

                    LinearProgressIndicator(
                        progress = (minOf(fingerIndex + 1, totalFingers)) / totalFingers.toFloat(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                }
            }

            // Scan Button
            Button(
                onClick = {

                    if (isProcessing || showSuccessDialog || fingerIndex >= totalFingers)
                        return@Button

                    if (lightType != "Normal") {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                if (lightType == "Low")
                                    "Low Light Detected. Adjust lighting."
                                else
                                    "Too Bright. Reduce exposure."
                            )
                        }
                        return@Button
                    }

                    isProcessing = true
                    cameraManager.triggerAutoFocus()

                    cameraManager.captureBitmap { bitmap ->

                        scope.launch {

                            val result = withContext(Dispatchers.Default) {

                                if (fingerIndex >= totalFingers)
                                    return@withContext "STOP"

                                if (BlurDetector.isBlurred(bitmap))
                                    return@withContext "BLUR"

                                val detection =
                                    detector.detect(bitmap)
                                        ?: return@withContext "NO_FINGER"

                                if (detection.landmarks().isEmpty())
                                    return@withContext "NO_FINGER"

                                val detectedHand =
                                    detector.getHandSide(detection)

                                val newEmbedding =
                                    extractor.extractEmbedding(detection)

                                val validation =
                                    FingerValidator.validateHand(
                                        storedHand = PalmStorage.storedHandSide,
                                        detectedHand = detectedHand,
                                        storedEmbedding = PalmStorage.storedEmbedding,
                                        newEmbedding = newEmbedding
                                    )

                                if (!validation.isValid)
                                    return@withContext validation.message

                                val timeStamp = SimpleDateFormat(
                                    "yyyyMMdd_HHmmss",
                                    Locale.getDefault()
                                ).format(Date())

                                val fileName =
                                    "${storedHand}_${fingerNames[fingerIndex]}_Finger_$timeStamp.jpg"

                                StorageUtil.saveImageToPublicFolder(
                                    context,
                                    bitmap,
                                    fileName
                                )

                                "SUCCESS"
                            }

                            isProcessing = false

                            when (result) {

                                "BLUR" ->
                                    snackbarHostState.showSnackbar("Image blurred. Recapture.")

                                "NO_FINGER" ->
                                    snackbarHostState.showSnackbar("Finger not detected.")

                                "Incorrect Hand" ->
                                    snackbarHostState.showSnackbar("Incorrect Hand Used.")

                                "Finger does not match" ->
                                    snackbarHostState.showSnackbar("Finger does not match Palm.")

                                "Palm data missing" ->
                                    snackbarHostState.showSnackbar("Palm not captured.")

                                "SUCCESS" -> {

                                    if (fingerIndex < totalFingers - 1) {
                                        fingerIndex++
                                    } else {
                                        fingerIndex = totalFingers - 1
                                        showSuccessDialog = true
                                        cameraManager.shutdown()
                                    }
                                }
                            }
                        }
                    }
                },
                enabled = !isProcessing && fingerIndex < totalFingers,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 40.dp)
                    .fillMaxWidth(0.8f)
            ) {

                if (isProcessing) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text("Scan ${fingerNames.getOrElse(fingerIndex) { "" }}")
                }
            }

            // Success Dialog
            if (showSuccessDialog) {
                AlertDialog(
                    onDismissRequest = {},
                    confirmButton = {
                        Button(onClick = {
                            showSuccessDialog = false
                            navController.navigate("result") {
                                popUpTo("finger") { inclusive = true }
                            }
                        }) {
                            Text("View Report")
                        }
                    },
                    title = { Text("All Fingers Captured") },
                    text = {
                        Text("All five fingers scanned successfully.")
                    }
                )
            }
        }
    }
}
