package com.example.ui.screens

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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.CbseCurriculum
import com.example.model.ChapterProgress
import com.example.model.GeneratedMcq
import com.example.model.QuizAttempt
import com.example.model.StudyNote
import com.example.model.SubjectDefinition
import com.example.model.UiState
import com.example.ui.components.EmptyNotesIllustration
import com.example.ui.components.StudyNoteCard

@Composable
fun PracticeQuizTab(
    subjects: List<SubjectDefinition>,
    notes: List<StudyNote>,
    quizState: UiState<List<GeneratedMcq>>?,
    quizAttempts: List<QuizAttempt>,
    onGenerateQuiz: (subjectName: String, chapterName: String, note: StudyNote?) -> Unit,
    onQuizCompleted: (subjectName: String, chapterName: String, score: Int, total: Int) -> Unit
) {
    var selectedSubject by remember(subjects) { mutableStateOf(subjects.first()) }
    var selectedChapter by remember(selectedSubject.id) {
        mutableStateOf(selectedSubject.allChapters().firstOrNull()?.second ?: "General")
    }
    var selectedNote by remember { mutableStateOf<StudyNote?>(null) }
    var subMenuOpen by remember { mutableStateOf(false) }
    var chapMenuOpen by remember { mutableStateOf(false) }

    var currentIndex by remember(quizState) { mutableIntStateOf(0) }
    var selectedOption by remember(quizState, currentIndex) { mutableStateOf<Int?>(null) }
    var score by remember(quizState) { mutableIntStateOf(0) }
    var finished by remember(quizState) { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("AI Practice Quiz Generator", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { subMenuOpen = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(selectedSubject.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            DropdownMenu(expanded = subMenuOpen, onDismissRequest = { subMenuOpen = false }) {
                                subjects.forEach { sub ->
                                    DropdownMenuItem(
                                        text = { Text(sub.name) },
                                        onClick = {
                                            selectedSubject = sub
                                            selectedChapter = sub.allChapters().firstOrNull()?.second ?: "General"
                                            selectedNote = null
                                            subMenuOpen = false
                                        }
                                    )
                                }
                            }
                        }

                        Box(modifier = Modifier.weight(1.3f)) {
                            OutlinedButton(
                                onClick = { chapMenuOpen = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(selectedChapter, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            DropdownMenu(expanded = chapMenuOpen, onDismissRequest = { chapMenuOpen = false }) {
                                selectedSubject.allChapters().forEach { (_, chap) ->
                                    DropdownMenuItem(
                                        text = { Text(chap) },
                                        onClick = {
                                            selectedChapter = chap
                                            selectedNote = notes.firstOrNull {
                                                it.subjectId == selectedSubject.id && it.chapterName.equals(chap, ignoreCase = true)
                                            }
                                            chapMenuOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = { onGenerateQuiz(selectedSubject.name, selectedChapter, selectedNote) },
                        enabled = quizState !is UiState.Loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("generate_quiz_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Start 5-Question Quiz")
                    }
                }
            }
        }

        when (quizState) {
            is UiState.Loading -> {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator()
                            Text("Crafting CBSE Class 10 MCQs...", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            is UiState.Error -> {
                item {
                    Text(quizState.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
            }
            is UiState.Success -> {
                val questions = quizState.data
                if (questions.isNotEmpty()) {
                    item {
                        if (finished) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text("Quiz Complete!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "Score: $score / ${questions.size}",
                                        style = MaterialTheme.typography.displaySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Button(onClick = { onGenerateQuiz(selectedSubject.name, selectedChapter, selectedNote) }) {
                                        Text("Try New Questions")
                                    }
                                }
                            }
                        } else {
                            val q = questions[currentIndex.coerceIn(0, questions.lastIndex)]
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surface,
                                tonalElevation = 2.dp
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "Question ${currentIndex + 1} of ${questions.size}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(q.question, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                                    q.options.forEachIndexed { idx, opt ->
                                        val isPicked = selectedOption == idx
                                        val isCorrect = idx == q.correctIndex
                                        val bgColor = when {
                                            selectedOption == null -> MaterialTheme.colorScheme.surfaceContainerHigh
                                            isCorrect -> MaterialTheme.colorScheme.secondaryContainer
                                            isPicked -> MaterialTheme.colorScheme.errorContainer
                                            else -> MaterialTheme.colorScheme.surfaceContainerHigh
                                        }
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable(enabled = selectedOption == null) {
                                                    selectedOption = idx
                                                    if (idx == q.correctIndex) score++
                                                },
                                            shape = RoundedCornerShape(12.dp),
                                            color = bgColor
                                        ) {
                                            Text(
                                                text = opt,
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier.padding(14.dp)
                                            )
                                        }
                                    }

                                    if (selectedOption != null) {
                                        Text(
                                            text = q.explanation,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Button(
                                            onClick = {
                                                if (currentIndex < questions.lastIndex) {
                                                    currentIndex++
                                                    selectedOption = null
                                                } else {
                                                    finished = true
                                                    onQuizCompleted(selectedSubject.name, selectedChapter, score, questions.size)
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(if (currentIndex < questions.lastIndex) "Next Question" else "Finish & Save Score")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            null -> Unit
        }

        if (quizAttempts.isNotEmpty()) {
            item {
                Text("Recent Quiz Scores", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(quizAttempts.take(8), key = { it.id }) { attempt ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(attempt.chapterName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(attempt.subjectName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                        Text(
                            text = "${attempt.score}/${attempt.totalQuestions}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BookmarkedTopicsTab(
    subjects: List<SubjectDefinition>,
    notes: List<StudyNote>,
    onOpenNote: (String) -> Unit,
    onTogglePin: (StudyNote) -> Unit,
    onToggleFavourite: (StudyNote) -> Unit,
    onToggleBookmark: (StudyNote) -> Unit
) {
    val bookmarked = notes.filter { it.bookmarked }
    val colorMap = subjects.associate { it.id to it.accentColor }

    if (bookmarked.isEmpty()) {
        EmptyNotesIllustration(
            title = "No bookmarked topics yet",
            subtitle = "Tap the bookmark icon on any note to save it here for quick last-minute exam revision."
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(bookmarked, key = { it.id }) { note ->
                StudyNoteCard(
                    note = note,
                    accentColor = colorMap[note.subjectId] ?: MaterialTheme.colorScheme.primary,
                    onClick = { onOpenNote(note.id) },
                    onTogglePin = { onTogglePin(note) },
                    onToggleFavourite = { onToggleFavourite(note) },
                    onToggleBookmark = { onToggleBookmark(note) }
                )
            }
        }
    }
}

@Composable
fun ChapterTrackerTab(
    subjects: List<SubjectDefinition>,
    chapterProgress: List<ChapterProgress>,
    onToggleChapterCompleted: (subjectId: String, sectionName: String, chapterName: String, completed: Boolean) -> Unit
) {
    val completedSet = remember(chapterProgress) {
        chapterProgress.filter { it.completed }
            .map { CbseCurriculum.progressDocId(it.subjectId, it.sectionName, it.chapterName) }
            .toSet()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        subjects.forEach { subject ->
            val chapters = subject.allChapters()
            val done = chapters.count { (sec, chap) ->
                completedSet.contains(CbseCurriculum.progressDocId(subject.id, sec, chap))
            }
            val pct = if (chapters.isNotEmpty()) done.toFloat() / chapters.size else 0f

            item(key = "header_${subject.id}") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(subject.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("$done / ${chapters.size} (${(pct * 100).toInt()}%)", style = MaterialTheme.typography.labelLarge, color = subject.accentColor)
                        }
                        LinearProgressIndicator(
                            progress = { pct },
                            modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(999.dp)),
                            color = subject.accentColor
                        )
                    }
                }
            }

            items(chapters, key = { "${subject.id}_${it.first}_${it.second}" }) { (sec, chap) ->
                val isDone = completedSet.contains(CbseCurriculum.progressDocId(subject.id, sec, chap))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onToggleChapterCompleted(subject.id, sec, chap, !isDone) }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onToggleChapterCompleted(subject.id, sec, chap, !isDone) },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            if (isDone) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                            contentDescription = if (isDone) "Completed" else "Incomplete",
                            tint = if (isDone) subject.accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(chap, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (sec.isNotBlank()) {
                            Text(sec, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
