package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Note
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.GeneratedFlashcard
import com.example.model.GeneratedImportantQuestion
import com.example.model.GeneratedMcq
import com.example.model.StudyNote
import com.example.model.UiState
import com.example.ui.components.RichTextViewer
import com.example.viewmodel.AiChatMessage
import com.example.viewmodel.AssistantAction

@Composable
fun AiAssistantScreen(
    notes: List<StudyNote>,
    selectedNote: StudyNote?,
    useOnlySavedNotes: Boolean,
    messages: List<AiChatMessage>,
    isStreaming: Boolean,
    errorMessage: String?,
    generatedFlashcardsState: UiState<List<GeneratedFlashcard>>?,
    generatedQuestionsState: UiState<List<GeneratedImportantQuestion>>?,
    quizState: UiState<List<GeneratedMcq>>?,
    savedBanner: String?,
    onSelectNote: (StudyNote?) -> Unit,
    onToggleOnlySavedNotes: (Boolean) -> Unit,
    onSendPrompt: (String) -> Unit,
    onRunQuickAction: (AssistantAction) -> Unit,
    onSaveFlashcards: (List<GeneratedFlashcard>) -> Unit,
    onSaveQuestions: (List<GeneratedImportantQuestion>) -> Unit,
    onOpenRevisionQuiz: () -> Unit,
    onClearChat: () -> Unit,
    onClearBanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    var promptInput by remember { mutableStateOf("") }
    var noteMenuOpen by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Header + Context Controls
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_ai_orb),
                            contentDescription = "StudyVault AI Mentor",
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Column {
                            Text(
                                text = "AI Study Assistant",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Class 10 Board Exam Mentor",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (messages.isNotEmpty()) {
                        IconButton(onClick = onClearChat, modifier = Modifier.testTag("clear_ai_chat")) {
                            Icon(Icons.Filled.DeleteSweep, contentDescription = "Clear chat")
                        }
                    }
                }

                // Note context picker + Strict Saved Notes switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { noteMenuOpen = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ai_note_context_btn"),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Filled.Note, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = selectedNote?.title ?: "All Saved Notes (${notes.size})",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        DropdownMenu(
                            expanded = noteMenuOpen,
                            onDismissRequest = { noteMenuOpen = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Saved Notes (${notes.size})") },
                                onClick = {
                                    onSelectNote(null)
                                    noteMenuOpen = false
                                }
                            )
                            notes.take(25).forEach { n ->
                                DropdownMenuItem(
                                    text = { Text("${n.subjectName}: ${n.title}", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    onClick = {
                                        onSelectNote(n)
                                        noteMenuOpen = false
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Notes Only", style = MaterialTheme.typography.labelSmall)
                        Switch(
                            checked = useOnlySavedNotes,
                            onCheckedChange = onToggleOnlySavedNotes,
                            modifier = Modifier.testTag("ai_notes_only_switch")
                        )
                    }
                }

                // Quick Study Actions
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(AssistantAction.entries) { action ->
                        AssistChip(
                            onClick = { onRunQuickAction(action) },
                            label = { Text(action.label) },
                            enabled = !isStreaming,
                            modifier = Modifier.testTag("ai_action_${action.name}")
                        )
                    }
                }
            }
        }

        if (!savedBanner.isNullOrBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(savedBanner, style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(onClick = onClearBanner, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)) {
                        Text("OK", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // Chat Feed + Save-to-Revision-Center cards
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Ask anything about your Class 10 syllabus or saved notes",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tap Summarize, Explain Simply, Important Qs, Create MCQs, Flashcards, or Easy Points above — or ask a specific question below.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            items(messages, key = { it.id }) { msg ->
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = if (msg.isUser) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Surface(
                        shape = RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (msg.isUser) 18.dp else 4.dp,
                            bottomEnd = if (msg.isUser) 4.dp else 18.dp
                        ),
                        color = if (msg.isUser) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surface,
                        tonalElevation = if (msg.isUser) 0.dp else 2.dp,
                        modifier = Modifier.fillMaxWidth(if (msg.isUser) 0.85f else 0.96f)
                    ) {
                        Box(
                            modifier = Modifier
                                .then(
                                    if (!msg.isUser) Modifier.border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant,
                                        RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)
                                    ) else Modifier
                                )
                                .padding(14.dp)
                        ) {
                            if (msg.isUser) {
                                Text(
                                    text = msg.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else if (msg.text.isBlank() && msg.isStreaming) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Text("Thinking...", style = MaterialTheme.typography.bodySmall)
                                }
                            } else {
                                RichTextViewer(rawText = msg.text)
                            }
                        }
                    }
                }
            }

            // Structured Save Cards if Flashcards / Questions / Quiz were generated
            if (generatedFlashcardsState is UiState.Success && generatedFlashcardsState.data.isNotEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${generatedFlashcardsState.data.size} Flashcards Ready",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Button(
                                onClick = { onSaveFlashcards(generatedFlashcardsState.data) },
                                modifier = Modifier.testTag("save_ai_flashcards_btn")
                            ) {
                                Icon(Icons.Filled.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Save to Revision")
                            }
                        }
                    }
                }
            }

            if (generatedQuestionsState is UiState.Success && generatedQuestionsState.data.isNotEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${generatedQuestionsState.data.size} Exam Questions Ready",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Button(
                                onClick = { onSaveQuestions(generatedQuestionsState.data) },
                                modifier = Modifier.testTag("save_ai_questions_btn")
                            ) {
                                Icon(Icons.Filled.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Save to Revision")
                            }
                        }
                    }
                }
            }

            if (quizState is UiState.Success && quizState.data.isNotEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${quizState.data.size}-Question MCQ Quiz Ready",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Button(
                                onClick = onOpenRevisionQuiz,
                                modifier = Modifier.testTag("open_ai_quiz_btn")
                            ) {
                                Text("Take Quiz")
                            }
                        }
                    }
                }
            }

            if (!errorMessage.isNullOrBlank()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }

        // Input Composer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = promptInput,
                onValueChange = { promptInput = it },
                placeholder = { Text("Ask StudyVault AI...") },
                maxLines = 3,
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_prompt_input"),
                shape = RoundedCornerShape(18.dp)
            )
            IconButton(
                onClick = {
                    val p = promptInput
                    promptInput = ""
                    onSendPrompt(p)
                },
                enabled = promptInput.isNotBlank() && !isStreaming,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (promptInput.isNotBlank() && !isStreaming) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                    .testTag("ai_send_btn")
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send prompt",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}
