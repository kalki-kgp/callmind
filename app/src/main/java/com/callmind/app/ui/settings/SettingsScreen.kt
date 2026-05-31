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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
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
    val clipboard = LocalClipboardManager.current

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
            // Active pipeline summary — which method each stage currently uses
            SettingsCard {
                Text(
                    "Active Pipeline",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "Methods used to process a recording, end to end",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))
                PipelineRow(
                    "Transcription",
                    if (uiState.useLocalStt) "Local · Vosk" else "Cloud · Gemini"
                )
                PipelineRow(
                    "Analysis (LLM)",
                    if (uiState.llmProvider == "openai_compatible") "OpenAI-compatible" else "Cloud · Gemini"
                )
                PipelineRow(
                    "Embeddings",
                    if (uiState.embeddingProvider == "local") "Local · on-device" else "Cloud · Gemini"
                )
            }

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
                Text(
                    "Which engine transcribes recordings",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !uiState.useLocalStt,
                        onClick = { viewModel.onUseLocalSttChanged(false) },
                        label = { Text("Cloud (Gemini)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GreenPrimary,
                            selectedLabelColor = DarkBackground,
                            containerColor = DarkSurfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onBackground
                        )
                    )
                    FilterChip(
                        selected = uiState.useLocalStt,
                        onClick = { viewModel.onUseLocalSttChanged(true) },
                        enabled = uiState.isVoskModelDownloaded,
                        label = { Text("Local (Vosk)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GreenPrimary,
                            selectedLabelColor = DarkBackground,
                            containerColor = DarkSurfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onBackground
                        )
                    )
                }

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

            // Search Embeddings
            SettingsCard {
                Text(
                    "Search Embeddings",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "How transcripts are vectorized for semantic search",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = uiState.embeddingProvider == "cloud",
                        onClick = { viewModel.onEmbeddingProviderChanged("cloud") },
                        label = { Text("Cloud (Gemini)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GreenPrimary,
                            selectedLabelColor = DarkBackground,
                            containerColor = DarkSurfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onBackground
                        )
                    )
                    FilterChip(
                        selected = uiState.embeddingProvider == "local",
                        onClick = { viewModel.onEmbeddingProviderChanged("local") },
                        enabled = uiState.isEmbeddingModelDownloaded,
                        label = { Text("On-device") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GreenPrimary,
                            selectedLabelColor = DarkBackground,
                            containerColor = DarkSurfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onBackground
                        )
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Switching provider only affects calls indexed afterward — re-process " +
                        "older calls to make them searchable under the new model.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                if (!uiState.isEmbeddingModelDownloaded) {
                    Spacer(modifier = Modifier.height(8.dp))
                    if (uiState.isDownloadingEmbeddingModel) {
                        LinearProgressIndicator(
                            progress = { uiState.embeddingModelDownloadProgress },
                            modifier = Modifier.fillMaxWidth(),
                            color = GreenPrimary,
                            trackColor = DarkSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Downloading model... ${(uiState.embeddingModelDownloadProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    } else {
                        Button(
                            onClick = { viewModel.downloadEmbeddingModel() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DarkSurfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onBackground
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Download On-device Model")
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
                    "Test each method on its own (saves settings first). Results are " +
                        "selectable — long-press to copy, or use Copy All.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                TestGroupLabel("Speech-to-Text")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TestButton("Cloud (Gemini)", "stt_cloud", uiState.runningTestId, Modifier.weight(1f)) { viewModel.testSttCloud() }
                    TestButton("Local (Vosk)", "stt_local", uiState.runningTestId, Modifier.weight(1f)) { viewModel.testSttLocal() }
                }

                TestGroupLabel("Analysis LLM")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TestButton("Cloud (Gemini)", "llm_cloud", uiState.runningTestId, Modifier.weight(1f)) { viewModel.testLlmCloud() }
                    TestButton("OpenAI-compat", "llm_openai", uiState.runningTestId, Modifier.weight(1f)) { viewModel.testLlmOpenAi() }
                }

                TestGroupLabel("Embeddings")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TestButton("Cloud (Gemini)", "embed_cloud", uiState.runningTestId, Modifier.weight(1f)) { viewModel.testEmbedCloud() }
                    TestButton("Local (device)", "embed_local", uiState.runningTestId, Modifier.weight(1f)) { viewModel.testEmbedLocal() }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.runAllTests() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkSurfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onBackground
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) { Text("Run All") }

                    if (uiState.testResults.isNotEmpty()) {
                        Button(
                            onClick = {
                                val text = uiState.testResults.joinToString("\n\n") { r ->
                                    "[${if (r.success) "OK" else "FAIL"}] ${r.label}\n${r.detail}"
                                }
                                clipboard.setText(AnnotatedString(text))
                                Toast.makeText(context, "Copied all results", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DarkSurfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onBackground
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) { Text("Copy All") }

                        Button(
                            onClick = { viewModel.clearTestResults() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DarkSurfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onBackground
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) { Text("Clear") }
                    }
                }

                uiState.testResults.forEach { result ->
                    Spacer(modifier = Modifier.height(8.dp))
                    TestResultRow(result)
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
private fun PipelineRow(stage: String, method: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            stage,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Text(
            method,
            style = MaterialTheme.typography.bodyMedium,
            color = GreenPrimary
        )
    }
}

@Composable
private fun TestGroupLabel(text: String) {
    Spacer(modifier = Modifier.height(10.dp))
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onBackground
    )
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
private fun TestButton(
    label: String,
    id: String,
    runningTestId: String?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = runningTestId != id,
        colors = ButtonDefaults.buttonColors(
            containerColor = DarkSurfaceVariant,
            contentColor = MaterialTheme.colorScheme.onBackground
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        if (runningTestId == id) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = GreenPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun TestResultRow(result: ConnectionTestResult) {
    val accent = if (result.success) GreenPrimary else CallMissed
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (result.success) "OK" else "FAIL",
                style = MaterialTheme.typography.labelMedium,
                color = accent
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                result.label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
            shape = RoundedCornerShape(8.dp)
        ) {
            SelectionContainer {
                Text(
                    result.detail,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = TextSecondary
                )
            }
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
