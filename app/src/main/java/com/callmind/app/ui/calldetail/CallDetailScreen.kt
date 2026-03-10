package com.callmind.app.ui.calldetail

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callmind.app.ui.components.CallTypeIcon
import com.callmind.app.ui.components.ContactAvatar
import com.callmind.app.ui.theme.DarkBackground
import com.callmind.app.ui.theme.DarkSurface
import com.callmind.app.ui.theme.DarkSurfaceVariant
import com.callmind.app.ui.theme.GreenPrimary
import com.callmind.app.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CallDetailScreen(
    onBack: () -> Unit,
    viewModel: CallDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {},
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
                    containerColor = DarkBackground
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Avatar
            ContactAvatar(
                name = uiState.contactName ?: uiState.phoneNumber,
                size = 96.dp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Name
            Text(
                text = uiState.contactName ?: uiState.phoneNumber,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Action buttons row
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = 32.dp)
            ) {
                FilledTonalButton(
                    onClick = {
                        viewModel.exportCall { intent ->
                            context.startActivity(Intent.createChooser(intent, "Share call summary"))
                        }
                    },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = DarkSurfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share")
                }
                FilledTonalButton(
                    onClick = { viewModel.processCall() },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = DarkSurfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        if (uiState.summary != null) Icons.Default.Refresh else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (uiState.summary != null) "Reprocess" else "Process")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Call info card
            DetailCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CallTypeIcon(callType = uiState.callType)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = uiState.callType,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    uiState.durationSeconds?.let { secs ->
                        Text(
                            text = "${secs / 60}m ${secs % 60}s",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
                if (uiState.timestamp > 0) {
                    val dateFormat = SimpleDateFormat("dd/MM/yyyy  HH:mm", Locale.getDefault())
                    Text(
                        text = dateFormat.format(Date(uiState.timestamp)),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                if (uiState.phoneNumber.isNotEmpty() && uiState.contactName != null) {
                    Text(
                        text = uiState.phoneNumber,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Processing error
            if (uiState.processingError != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Processing Failed",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            uiState.processingError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Summary
            if (uiState.summary != null) {
                Spacer(modifier = Modifier.height(12.dp))
                DetailSectionCard(
                    title = "Summary",
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text(
                        uiState.summary!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            // Sentiment + Topics
            if (uiState.sentiment != null || uiState.topics.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                DetailCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    if (uiState.sentiment != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Sentiment",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            AssistChip(
                                onClick = {},
                                label = {
                                    Text(
                                        uiState.sentiment!!,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = DarkSurfaceVariant,
                                    labelColor = GreenPrimary
                                ),
                                border = null
                            )
                        }
                    }
                    if (uiState.topics.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Topics",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextSecondary
                        )
                        FlowRow(
                            modifier = Modifier.padding(top = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            uiState.topics.forEach { topic ->
                                AssistChip(
                                    onClick = {},
                                    label = { Text(topic) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = DarkSurfaceVariant,
                                        labelColor = MaterialTheme.colorScheme.onBackground
                                    ),
                                    border = null
                                )
                            }
                        }
                    }
                }
            }

            // Action Items
            if (uiState.actionItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                DetailSectionCard(
                    title = "Action Items",
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    uiState.actionItems.forEach { item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = item.isCompleted,
                                onCheckedChange = { viewModel.toggleActionItem(item.id, it) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = GreenPrimary,
                                    uncheckedColor = TextSecondary,
                                    checkmarkColor = DarkBackground
                                )
                            )
                            Text(
                                item.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }
            }

            // Key Points
            if (uiState.keyPoints.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                DetailSectionCard(
                    title = "Key Points",
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    uiState.keyPoints.forEach { point ->
                        Text(
                            "\u2022  $point",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(vertical = 3.dp)
                        )
                    }
                }
            }

            // Transcript
            if (uiState.transcript != null) {
                Spacer(modifier = Modifier.height(12.dp))
                DetailSectionCard(
                    title = "Transcript",
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text(
                        uiState.transcript!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DetailCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
private fun DetailSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}
