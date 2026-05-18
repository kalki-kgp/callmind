package com.callmind.app.ui.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callmind.app.ui.theme.DarkBackground
import com.callmind.app.ui.theme.DarkSurface
import com.callmind.app.ui.theme.DarkSurfaceVariant
import com.callmind.app.ui.theme.GreenPrimary
import com.callmind.app.ui.theme.CallMissed
import com.callmind.app.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = DarkSurfaceVariant,
        unfocusedContainerColor = DarkSurfaceVariant,
        focusedBorderColor = GreenPrimary,
        unfocusedBorderColor = DarkSurfaceVariant,
        cursorColor = GreenPrimary,
        focusedTextColor = MaterialTheme.colorScheme.onBackground,
        unfocusedTextColor = MaterialTheme.colorScheme.onBackground
    )

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Recording directory
            SettingsCard {
                Text(
                    "Recording Directory",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "Path where call recordings are saved",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = uiState.recordingDirectory,
                    onValueChange = viewModel::onRecordingDirectoryChanged,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = textFieldColors
                )
            }

            // Gemini API Key (always needed for transcription + embeddings)
            SettingsCard {
                Text(
                    "Gemini API Key",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "Required for transcription and embeddings",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = uiState.geminiApiKey,
                    onValueChange = viewModel::onApiKeyChanged,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    shape = RoundedCornerShape(12.dp),
                    colors = textFieldColors
                )
            }

            // LLM Provider selection
            SettingsCard {
                Text(
                    "Analysis LLM Provider",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "Which LLM to use for call analysis",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = uiState.llmProvider == "gemini",
                        onClick = { viewModel.onLlmProviderChanged("gemini") },
                        label = { Text("Gemini") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GreenPrimary,
                            selectedLabelColor = DarkBackground,
                            containerColor = DarkSurfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onBackground
                        )
                    )
                    FilterChip(
                        selected = uiState.llmProvider == "openai_compatible",
                        onClick = { viewModel.onLlmProviderChanged("openai_compatible") },
                        label = { Text("OpenAI Compatible") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GreenPrimary,
                            selectedLabelColor = DarkBackground,
                            containerColor = DarkSurfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onBackground
                        )
                    )
                }

                // OpenAI-compatible provider fields
                if (uiState.llmProvider == "openai_compatible") {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Base URL",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    OutlinedTextField(
                        value = uiState.openAiBaseUrl,
                        onValueChange = viewModel::onOpenAiBaseUrlChanged,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors,
                        placeholder = {
                            Text("https://api.tokenfactory...", color = TextSecondary)
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "API Key",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    OutlinedTextField(
                        value = uiState.openAiApiKey,
                        onValueChange = viewModel::onOpenAiApiKeyChanged,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Model",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    OutlinedTextField(
                        value = uiState.openAiModel,
                        onValueChange = viewModel::onOpenAiModelChanged,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors,
                        placeholder = {
                            Text("deepseek-ai/DeepSeek-V3-0324-fast", color = TextSecondary)
                        }
                    )
                }
            }

            // Speech-to-Text
            SettingsCard {
                Text(
                    "Speech-to-Text",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))

                SettingToggle(
                    title = "Use Local STT (Vosk)",
                    description = if (uiState.isVoskModelDownloaded)
                        "Transcribe on-device — no cloud needed"
                    else
                        "Download model first (36 MB)",
                    checked = uiState.useLocalStt,
                    onCheckedChange = viewModel::onUseLocalSttChanged,
                    enabled = uiState.isVoskModelDownloaded
                )

                if (!uiState.isVoskModelDownloaded) {
                    Spacer(modifier = Modifier.height(8.dp))
                    if (uiState.isDownloadingModel) {
                        LinearProgressIndicator(
                            progress = { uiState.modelDownloadProgress },
                            modifier = Modifier.fillMaxWidth(),
                            color = GreenPrimary,
                            trackColor = DarkSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Downloading model... ${(uiState.modelDownloadProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    } else {
                        Button(
                            onClick = { viewModel.downloadVoskModel() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DarkSurfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onBackground
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Download Vosk Model (36 MB)")
                        }
                    }
                }
            }

            // Processing
            SettingsCard {
                Text(
                    "Processing",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))

                SettingToggle(
                    title = "Auto-process recordings",
                    description = "Automatically transcribe and analyze new recordings",
                    checked = uiState.autoProcess,
                    onCheckedChange = viewModel::onAutoProcessChanged
                )
            }

            // Connection Tests
            SettingsCard {
                Text(
                    "Connection Tests",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "Test your STT and LLM connections (saves settings first)",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.testStt() },
                        enabled = !uiState.isSttTesting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkSurfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onBackground
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (uiState.isSttTesting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = GreenPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Test STT")
                        }
                    }
                    Button(
                        onClick = { viewModel.testLlm() },
                        enabled = !uiState.isLlmTesting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkSurfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onBackground
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (uiState.isLlmTesting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = GreenPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Test LLM")
                        }
                    }
                }
                uiState.sttTestResult?.let { result ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        result,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (result.startsWith("OK")) GreenPrimary else CallMissed
                    )
                }
                uiState.llmTestResult?.let { result ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        result,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (result.startsWith("OK")) GreenPrimary else CallMissed
                    )
                }
            }

            Button(
                onClick = {
                    viewModel.saveSettings()
                    Toast.makeText(context, "Settings saved", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GreenPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Settings")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
private fun SettingToggle(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) MaterialTheme.colorScheme.onBackground else TextSecondary
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = GreenPrimary,
                checkedTrackColor = GreenPrimary.copy(alpha = 0.4f),
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = DarkSurfaceVariant
            )
        )
    }
}
