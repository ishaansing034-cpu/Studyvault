package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.model.ChapterProgress
import com.example.model.FlashcardItem
import com.example.model.GeneratedMcq
import com.example.model.QuizAttempt
import com.example.model.RevisionQuestion
import com.example.model.StudyNote
import com.example.model.SubjectDefinition
import com.example.model.UiState
import com.example.ui.components.EmptyNotesIllustration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RevisionCenterScreen(
    initialTab: Int = 0,
    subjects: List<SubjectDefinition>,
    notes: List<StudyNote>,
    flashcards: List<FlashcardItem>,
    revisionQuestions: List<RevisionQuestion>,
    quizAttempts: List<QuizAttempt>,
    chapterProgress: List<ChapterProgress>,
    quizState: UiState<List<GeneratedMcq>>?,
    onBack: () -> Unit,
    onAddFlashcard: (subject: String, chapter: String, q: String, a: String) -> Unit,
    onToggleFlashcardMastered: (FlashcardItem) -> Unit,
    onDeleteFlashcard: (FlashcardItem) -> Unit,
    onAddRevisionQuestion: (subject: String, chapter: String, marks: Int, q: String, a: String) -> Unit,
    onDeleteRevisionQuestion: (RevisionQuestion) -> Unit,
    onGenerateQuiz: (subject: String, chapter: String, note: StudyNote?) -> Unit,
    onQuizCompleted: (subject: String, chapter: String, score: Int, total: Int) -> Unit,
    onOpenNote: (String) -> Unit,
    onTogglePin: (StudyNote) -> Unit,
    onToggleFavourite: (StudyNote) -> Unit,
    onToggleBookmark: (StudyNote) -> Unit,
    onToggleChapterCompleted: (subjectId: String, sectionName: String, chapterName: String, completed: Boolean) -> Unit,
    onGenerateAiFlashcardsFromAssistant: () -> Unit
) {
    val tabs = listOf("Flashcards", "Quizzes", "Important Qs", "Bookmarked", "Tracker")
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, tabs.lastIndex)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Revision Center") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("revision_back_btn")) {
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
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 12.dp
            ) {
                tabs.forEachIndexed { idx, title ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        text = { Text(title, fontWeight = if (selectedTab == idx) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.testTag("revision_tab_$idx")
                    )
                }
            }

            when (selectedTab) {
                0 -> FlashcardsTab(
                    flashcards = flashcards,
                    onAddFlashcard = onAddFlashcard,
                    onToggleMastered = onToggleFlashcardMastered,
                    onDeleteFlashcard = onDeleteFlashcard,
                    onGenerateWithAi = onGenerateAiFlashcardsFromAssistant
                )
                1 -> PracticeQuizTab(
                    subjects = subjects,
                    notes = notes,
                    quizState = quizState,
                    quizAttempts = quizAttempts,
                    onGenerateQuiz = onGenerateQuiz,
                    onQuizCompleted = onQuizCompleted
                )
                2 -> ImportantQuestionsTab(
                    questions = revisionQuestions,
                    onAddQuestion = onAddRevisionQuestion,
                    onDeleteQuestion = onDeleteRevisionQuestion
                )
                3 -> BookmarkedTopicsTab(
                    subjects = subjects,
                    notes = notes,
                    onOpenNote = onOpenNote,
                    onTogglePin = onTogglePin,
                    onToggleFavourite = onToggleFavourite,
                    onToggleBookmark = onToggleBookmark
                )
                4 -> ChapterTrackerTab(
                    subjects = subjects,
                    chapterProgress = chapterProgress,
                    onToggleChapterCompleted = onToggleChapterCompleted
                )
            }
        }
    }
}

@Composable
private fun FlashcardsTab(
    flashcards: List<FlashcardItem>,
    onAddFlashcard: (subject: String, chapter: String, q: String, a: String) -> Unit,
    onToggleMastered: (FlashcardItem) -> Unit,
    onDeleteFlashcard: (FlashcardItem) -> Unit,
    onGenerateWithAi: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var activeIndex by remember { mutableIntStateOf(0) }
    var flipped by remember(activeIndex) { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.weight(1f).testTag("add_flashcard_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("New Card")
                }
                OutlinedButton(
                    onClick = onGenerateWithAi,
                    modifier = Modifier.weight(1f).testTag("ai_flashcard_shortcut"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("AI Cards")
                }
            }
        }

        if (flashcards.isEmpty()) {
            item {
                EmptyNotesIllustration(
                    title = "No flashcards yet",
                    subtitle = "Create a flashcard or use AI Assistant to turn any note into study flashcards."
                )
            }
        } else {
            val safeIdx = activeIndex.coerceIn(0, flashcards.lastIndex)
            val card = flashcards[safeIdx]

            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable { flipped = !flipped }
                        .testTag("active_flashcard_deck"),
                    shape = RoundedCornerShape(24.dp),
                    color = if (flipped) MaterialTheme.colorScheme.secondaryContainer
                    else MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${card.subjectName} • ${card.chapterName}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(Icons.Filled.Flip, contentDescription = "Tap to flip", modifier = Modifier.size(18.dp))
                        }

                        Text(
                            text = if (flipped) card.answer else card.question,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = if (flipped) "ANSWER (Tap to flip)" else "QUESTION (Tap to reveal)",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { if (safeIdx > 0) activeIndex = safeIdx - 1 },
                        enabled = safeIdx > 0
                    ) { Text("Prev") }

                    Text("${safeIdx + 1} / ${flashcards.size}", style = MaterialTheme.typography.labelLarge)

                    OutlinedButton(
                        onClick = { if (safeIdx < flashcards.lastIndex) activeIndex = safeIdx + 1 },
                        enabled = safeIdx < flashcards.lastIndex
                    ) { Text("Next") }
                }
            }

            items(flashcards, key = { it.id }) { item ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { onToggleMastered(item) }) {
                            Icon(
                                if (item.mastered) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                contentDescription = "Toggle mastered",
                                tint = if (item.mastered) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.question, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(item.answer, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { onDeleteFlashcard(item) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete flashcard", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var subject by remember { mutableStateOf("Science") }
        var chapter by remember { mutableStateOf("Electricity") }
        var question by remember { mutableStateOf("") }
        var answer by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Create Flashcard") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("Subject") }, singleLine = true)
                    OutlinedTextField(value = chapter, onValueChange = { chapter = it }, label = { Text("Chapter") }, singleLine = true)
                    OutlinedTextField(value = question, onValueChange = { question = it }, label = { Text("Front (Question / Term)") })
                    OutlinedTextField(value = answer, onValueChange = { answer = it }, label = { Text("Back (Answer / Formula)") })
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddFlashcard(subject, chapter, question, answer)
                        showAddDialog = false
                    },
                    enabled = question.isNotBlank() && answer.isNotBlank()
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ImportantQuestionsTab(
    questions: List<RevisionQuestion>,
    onAddQuestion: (subject: String, chapter: String, marks: Int, q: String, a: String) -> Unit,
    onDeleteQuestion: (RevisionQuestion) -> Unit
) {
    var marksFilter by remember { mutableStateOf<Int?>(null) }
    var expandedId by remember { mutableStateOf<String?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    val filtered = questions.filter { marksFilter == null || it.marks.toInt() == marksFilter }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(null to "All", 2 to "2M", 3 to "3M", 5 to "5M").forEach { (m, label) ->
                        FilterChip(
                            selected = marksFilter == m,
                            onClick = { marksFilter = m },
                            label = { Text(label) }
                        )
                    }
                }
                OutlinedButton(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("add_important_q_btn")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("Add")
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                EmptyNotesIllustration(
                    title = "No important questions saved yet",
                    subtitle = "Save 2-mark, 3-mark, or 5-mark board exam questions here or generate them via AI Study Assistant."
                )
            }
        } else {
            items(filtered, key = { it.id }) { q ->
                val isOpen = expandedId == q.id
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { expandedId = if (isOpen) null else q.id },
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "${q.marks} Marks • ${q.subjectName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            IconButton(onClick = { onDeleteQuestion(q) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete question", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            }
                        }
                        Text(q.question, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(q.chapterName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        AnimatedVisibility(visible = isOpen) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow
                            ) {
                                Text(
                                    text = q.answer,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.fillMaxWidth().padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var subject by remember { mutableStateOf("Social Science") }
        var chapter by remember { mutableStateOf("Nationalism in India") }
        var marks by remember { mutableIntStateOf(5) }
        var question by remember { mutableStateOf("") }
        var answer by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Important Question") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("Subject") }, singleLine = true)
                    OutlinedTextField(value = chapter, onValueChange = { chapter = it }, label = { Text("Chapter") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(2, 3, 5).forEach { m ->
                            FilterChip(selected = marks == m, onClick = { marks = m }, label = { Text("$m Marks") })
                        }
                    }
                    OutlinedTextField(value = question, onValueChange = { question = it }, label = { Text("Question") })
                    OutlinedTextField(value = answer, onValueChange = { answer = it }, label = { Text("Answer / Key Points") })
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddQuestion(subject, chapter, marks, question, answer)
                        showAddDialog = false
                    },
                    enabled = question.isNotBlank() && answer.isNotBlank()
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }
}
