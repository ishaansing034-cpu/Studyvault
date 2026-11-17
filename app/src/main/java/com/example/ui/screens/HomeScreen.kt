package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.CbseCurriculum
import com.example.model.ChapterProgress
import com.example.model.StudyNote
import com.example.model.SubjectDefinition
import com.example.model.UiState
import com.example.ui.components.EmptyNotesIllustration
import com.example.ui.components.StudyNoteCard
import com.example.ui.components.SubjectCard
import com.example.ui.components.UserInitialAvatar
import com.example.viewmodel.DashboardStats

@Composable
fun HomeScreen(
    studentName: String,
    stats: DashboardStats,
    subjects: List<SubjectDefinition>,
    notesState: UiState<List<StudyNote>>,
    chapterProgress: List<ChapterProgress>,
    onOpenSmartSearch: () -> Unit,
    onQuickAddNote: () -> Unit,
    onOpenSubject: (String) -> Unit,
    onOpenNote: (String) -> Unit,
    onOpenRevisionCenter: () -> Unit,
    onTogglePin: (StudyNote) -> Unit,
    onToggleFavourite: (StudyNote) -> Unit,
    onToggleBookmark: (StudyNote) -> Unit,
    modifier: Modifier = Modifier
) {
    val notes = (notesState as? UiState.Success)?.data ?: emptyList()
    val favouriteNotes = notes.filter { it.favourite }
    val recentNotes = notes.take(10)
    val completedSet = chapterProgress.filter { it.completed }.map {
        CbseCurriculum.progressDocId(it.subjectId, it.sectionName, it.chapterName)
    }.toSet()
    val subjectColorMap = subjects.associate { it.id to it.accentColor }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. Welcome Hero Header
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(172.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_study),
                        contentDescription = "StudyVault Dashboard Banner",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xE60B1120),
                                        Color(0xB3131C31),
                                        Color(0x664C1D95)
                                    )
                                )
                            )
                            .padding(18.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Welcome back,",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color(0xFFC4B5FD)
                                    )
                                    Text(
                                        text = studentName.ifBlank { "Class 10 Scholar" },
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                UserInitialAvatar(name = studentName)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = onQuickAddNote,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("home_quick_add_note"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Quick Add Note", maxLines = 1)
                                }
                                FilledTonalButton(
                                    onClick = onOpenRevisionCenter,
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("home_revision_center_btn"),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Filled.Quiz, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Revise", maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Smart Search Bar
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(onClick = onOpenSmartSearch)
                    .testTag("home_smart_search_bar"),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                            RoundedCornerShape(18.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search notes",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Ask Smart Search: \"5-mark Qs in History\"...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = "AI Powered Search",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // 3. Stats Summary: Total Notes Saved & Study Progress %
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Filled.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Notes Saved",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "${stats.totalNotes}",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("stat_total_notes")
                        )
                        Text(
                            text = "${stats.favouriteCount} favourites",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(onClick = onOpenRevisionCenter),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Filled.TrendingUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Study Progress",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "${stats.studyProgressPercent}%",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.testTag("stat_study_progress")
                        )
                        LinearProgressIndicator(
                            progress = { stats.studyProgressPercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(999.dp)),
                            color = MaterialTheme.colorScheme.secondary,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    }
                }
            }
        }

        // 4. Five Subject Cards (+ any custom subjects)
        item {
            Text(
                text = "Class 10 Subjects",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(subjects, key = { it.id }) { subject ->
                    val subjectNotesCount = notes.count { it.subjectId == subject.id }
                    val chapters = subject.allChapters()
                    val doneCount = chapters.count { (sec, chap) ->
                        completedSet.contains(CbseCurriculum.progressDocId(subject.id, sec, chap))
                    }
                    SubjectCard(
                        subject = subject,
                        noteCount = subjectNotesCount,
                        completedChapters = doneCount,
                        totalChapters = chapters.size,
                        onClick = { onOpenSubject(subject.id) },
                        modifier = Modifier.width(210.dp)
                    )
                }
            }
        }

        // 5. Favourite Notes
        if (favouriteNotes.isNotEmpty()) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = Color(0xFFEC4899),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Favourite Notes",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(favouriteNotes, key = { "fav_${it.id}" }) { note ->
                        val accent = subjectColorMap[note.subjectId] ?: MaterialTheme.colorScheme.primary
                        Surface(
                            modifier = Modifier
                                .width(230.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { onOpenNote(note.id) }
                                .testTag("fav_carousel_${note.id}"),
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = note.subjectName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = accent,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = note.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = note.chapterName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // 6. Recently Added Notes
        item {
            Text(
                text = "Recently Added Notes",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        when (notesState) {
            is UiState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            is UiState.Error -> {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = notesState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
            is UiState.Success -> {
                if (recentNotes.isEmpty()) {
                    item {
                        EmptyNotesIllustration(
                            title = "Your Class 10 StudyVault is ready",
                            subtitle = "Tap Quick Add Note to write formulas, upload handwritten photos, or attach PDFs."
                        )
                    }
                } else {
                    items(recentNotes, key = { it.id }) { note ->
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
    }
}
