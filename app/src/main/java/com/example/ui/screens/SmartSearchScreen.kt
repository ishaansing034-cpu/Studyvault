package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AiSmartSearchResult
import com.example.model.StudyNote
import com.example.model.SubjectDefinition
import com.example.model.UiState
import com.example.ui.components.EmptyNotesIllustration
import com.example.ui.components.StudyNoteCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartSearchScreen(
    subjects: List<SubjectDefinition>,
    searchState: UiState<Pair<AiSmartSearchResult?, List<StudyNote>>>?,
    onSearch: (String) -> Unit,
    onBack: () -> Unit,
    onOpenNote: (String) -> Unit,
    onTogglePin: (StudyNote) -> Unit,
    onToggleFavourite: (StudyNote) -> Unit,
    onToggleBookmark: (StudyNote) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val subjectColorMap = subjects.associate { it.id to it.accentColor }

    val sampleQueries = listOf(
        "Find all 5-mark questions in History",
        "Show formulas from Trigonometry",
        "Find my notes on Electricity",
        "Show all important definitions in Biology"
    )

    LaunchedEffect(Unit) {
        if (searchState == null) {
            onSearch("")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Smart Search") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("search_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search naturally across your vault...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("smart_search_input")
                )
                Button(
                    onClick = { onSearch(query) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("smart_search_submit")
                ) {
                    Text("Go")
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(sampleQueries) { example ->
                    AssistChip(
                        onClick = {
                            query = example
                            onSearch(example)
                        },
                        label = { Text(example) },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }

            when (searchState) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator()
                            Text("Searching with Gemini AI...", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                is UiState.Error -> {
                    Text(
                        text = searchState.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                is UiState.Success -> {
                    val (intent, results) = searchState.data
                    if (intent != null) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = intent.interpretedIntent,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                val filters = listOfNotNull(
                                    intent.targetSubject?.let { "Subject: $it" },
                                    intent.targetChapter?.let { "Chapter: $it" },
                                    intent.targetNoteType?.let { "Type: ${it.label}" }
                                ).joinToString(" • ")
                                if (filters.isNotBlank()) {
                                    Text(
                                        text = filters,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }

                    if (results.isEmpty()) {
                        EmptyNotesIllustration(
                            title = "No matching notes found",
                            subtitle = "Try another natural-language query or add a new note for this topic."
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(results, key = { it.id }) { note ->
                                val accent = subjectColorMap[note.subjectId] ?: MaterialTheme.colorScheme.primary
                                StudyNoteCard(
                                    note = note,
                                    accentColor = accent,
                                    onClick = { onOpenNote(note.id) },
                                    onTogglePin = { onTogglePin(note) },
                                    onToggleFavourite = { onToggleFavourite(note) },
                                    onToggleBookmark = { onToggleBookmark(note) }
                                )
                            }
                        }
                    }
                }
                null -> Unit
            }
        }
    }
}
