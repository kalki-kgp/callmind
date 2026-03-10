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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    val dateFormat = SimpleDateFormat("EEEE, MMM dd yyyy 'at' hh:mm a", Locale.getDefault())
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.contactName ?: uiState.phoneNumber) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.exportCall { intent ->
                            context.startActivity(Intent.createChooser(intent, "Share call summary"))
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Export")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Call metadata
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(uiState.callType, style = MaterialTheme.typography.labelMedium)
                        uiState.durationSeconds?.let { secs ->
                            Text(
                                "${secs / 60}m ${secs % 60}s",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                    if (uiState.timestamp > 0) {
                        Text(
                            text = dateFormat.format(Date(uiState.timestamp)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            // Summary
            if (uiState.summary != null) {
                SectionCard("Summary") {
                    Text(uiState.summary!!, style = MaterialTheme.typography.bodyMedium)
                }
            }

            // Sentiment + Topics
            if (uiState.sentiment != null || uiState.topics.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (uiState.sentiment != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Sentiment: ", style = MaterialTheme.typography.titleSmall)
                                AssistChip(
                                    onClick = {},
                                    label = {
                                        Text(
                                            uiState.sentiment!!,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                )
                            }
                        }
                        if (uiState.topics.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Topics", style = MaterialTheme.typography.titleSmall)
                            FlowRow(
                                modifier = Modifier.padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                uiState.topics.forEach { topic ->
                                    AssistChip(
                                        onClick = {},
                                        label = { Text(topic) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Action Items
            if (uiState.actionItems.isNotEmpty()) {
                SectionCard("Action Items") {
                    uiState.actionItems.forEachIndexed { index, item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = item.isCompleted,
                                onCheckedChange = { viewModel.toggleActionItem(item.id, it) }
                            )
                            Text(
                                item.description,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }
            }

            // Key Points
            if (uiState.keyPoints.isNotEmpty()) {
                SectionCard("Key Points") {
                    uiState.keyPoints.forEach { point ->
                        Text(
                            "• $point",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }

            // Transcript
            if (uiState.transcript != null) {
                SectionCard("Transcript") {
                    Text(
                        uiState.transcript!!,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}
