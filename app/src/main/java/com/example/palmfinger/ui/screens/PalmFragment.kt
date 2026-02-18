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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.palmfinger.camera.BlurDetector
import com.example.palmfinger.camera.CameraManager
import com.example.palmfinger.camera.LuminosityAnalyzer
import com.example.palmfinger.detection.HandDetector
import com.example.palmfinger.detection.MinutiaeExtractor
import com.example.palmfinger.detection.PalmStorage
import com.example.palmfinger.utils.StorageUtil
import com.example.palmfinger.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PalmScreen(
    navController: NavController,
    viewModel: MainViewModel
) {

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var lightType by remember { mutableStateOf("Normal") }
    var detectedHandSide by remember { mutableStateOf("Unknown") }

    var isProcessing by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

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
                    .padding(80.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    border = BorderStroke(4.dp, Color.Green),
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
                        text = "Lighting: $lightType",
                        color = Color.White
                    )

                    if (detectedHandSide != "Unknown") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "$detectedHandSide Hand Detected",
                            color = if (detectedHandSide == "Left")
                                Color.Cyan
                            else
                                Color.Magenta,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }

            // Scan Button
            Button(
                onClick = {

                    if (isProcessing) return@Button

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

                                if (BlurDetector.isBlurred(bitmap))
                                    return@withContext "BLUR"

                                val detection =
                                    detector.detect(bitmap)
                                        ?: return@withContext "NO_PALM"

                                if (detection.landmarks().isEmpty())
                                    return@withContext "NO_PALM"

                                if (detector.isPalmDorsal(detection))
                                    return@withContext "DORSAL"

                                val handSide =
                                    detector.getHandSide(detection)

                                val embedding =
                                    extractor.extractEmbedding(detection)

                                if (embedding.isEmpty())
                                    return@withContext "NO_PALM"

                                // Store palm data (OK in background)
                                PalmStorage.storedHandSide = handSide
                                PalmStorage.storedEmbedding = embedding

                                // Save image
                                val timeStamp = SimpleDateFormat(
                                    "yyyyMMdd_HHmmss",
                                    Locale.getDefault()
                                ).format(Date())

                                val fileName =
                                    "${handSide}_Hand_$timeStamp.jpg"

                                StorageUtil.saveImageToPublicFolder(
                                    context,
                                    bitmap,
                                    fileName
                                )

                                handSide
                            }

                            isProcessing = false

                            when (result) {

                                "BLUR" ->
                                    snackbarHostState.showSnackbar("Palm image blurred.")

                                "NO_PALM" ->
                                    snackbarHostState.showSnackbar("No palm detected.")

                                "DORSAL" ->
                                    snackbarHostState.showSnackbar(
                                        "Palm dorsal side detected, minutiae points won’t be extracted."
                                    )

                                else -> {
                                    // ✅ UPDATE VIEWMODEL ON MAIN THREAD
                                    viewModel.updateHandSide(result)

                                    detectedHandSide = result
                                    showSuccessDialog = true
                                }
                            }
                        }
                    }

                },
                enabled = !isProcessing,
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
                    Text("Scan Palm")
                }
            }

            // Success Dialog
            if (showSuccessDialog) {
                AlertDialog(
                    onDismissRequest = {},
                    confirmButton = {
                        Button(onClick = {
                            showSuccessDialog = false
                            navController.navigate("finger/$detectedHandSide")
                        }) {
                            Text("Continue")
                        }
                    },
                    title = {
                        Text("Palm Captured Successfully")
                    },
                    text = {
                        Text("Your $detectedHandSide hand has been recorded successfully. Now capture your $detectedHandSide hand fingers.")
                    }
                )
            }
        }
    }
}
