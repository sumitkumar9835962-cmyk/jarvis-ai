package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.*
import com.example.viewmodel.JarvisViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JarvisMainScreen(
    viewModel: JarvisViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var textInput by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        viewModel.initSpeech(context)
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startListening(context) { err ->
                Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Microphone permission required for voice commands, Boss.", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (uiState.isListening) ArcGlow else CyberCyan)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "JARVIS SYSTEM",
                                color = TextCyanLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = uiState.statusText,
                            color = CyberCyan.copy(alpha = 0.7f),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleTts() }) {
                        Icon(
                            imageVector = if (uiState.ttsEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "Toggle TTS",
                            tint = if (uiState.ttsEnabled) CyberCyan else Color.Gray
                        )
                    }
                    IconButton(onClick = { viewModel.toggleAutoExecute() }) {
                        Icon(
                            imageVector = if (uiState.autoExecuteActions) Icons.Default.Bolt else Icons.Default.FlashOff,
                            contentDescription = "Toggle Auto Execute",
                            tint = if (uiState.autoExecuteActions) AmberGold else Color.Gray
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkObsidian,
                    titleContentColor = TextCyanLight
                )
            )
        },
        containerColor = DarkObsidian
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Animated Arc Reactor Visualizer Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                ArcReactorVisualizer(
                    isListening = uiState.isListening,
                    isProcessing = uiState.isProcessing,
                    statusText = uiState.statusText,
                    onCoreClick = {
                        if (uiState.isListening) {
                            viewModel.stopListening()
                        } else {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                viewModel.startListening(context) { err ->
                                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    }
                )
            }

            // Quick Preset Action Chips
            QuickActionChips(
                onCommandClick = { cmd ->
                    viewModel.processUserCommand(cmd, context)
                }
            )

            // Conversation and Action JSON Output Log
            CommandHistoryView(
                messages = uiState.messages,
                onExecuteAction = { msg ->
                    viewModel.executeAction(msg, context)
                },
                modifier = Modifier.weight(1f)
            )

            // Input Dock
            Surface(
                color = TechSurface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(listOf(CardBorderCyan, NeonBlue)),
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                    )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    // Speech Mic Button
                    IconButton(
                        onClick = {
                            if (uiState.isListening) {
                                viewModel.stopListening()
                            } else {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                    viewModel.startListening(context) { err ->
                                        Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (uiState.isListening) ArcGlow else TechSurfaceVariant)
                            .border(1.dp, CyberCyan, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Input",
                            tint = if (uiState.isListening) DarkObsidian else CyberCyan
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Text Command Input Field
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = {
                            Text(
                                text = "Command Jarvis, Boss...",
                                color = TextCyanLight.copy(alpha = 0.4f),
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (textInput.isNotBlank()) {
                                    viewModel.processUserCommand(textInput, context)
                                    textInput = ""
                                    keyboardController?.hide()
                                }
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = CardBorderCyan,
                            focusedTextColor = TextCyanLight,
                            unfocusedTextColor = TextCyanLight,
                            cursorColor = CyberCyan
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Send Button
                    IconButton(
                        onClick = {
                            if (textInput.isNotBlank()) {
                                viewModel.processUserCommand(textInput, context)
                                textInput = ""
                                keyboardController?.hide()
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (textInput.isNotBlank()) CyberCyan else TechSurfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send Command",
                            tint = if (textInput.isNotBlank()) DarkObsidian else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
