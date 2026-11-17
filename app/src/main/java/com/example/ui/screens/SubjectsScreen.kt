package com.example.ui.screens

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.model.HindiCourse
import com.example.model.StudyNote
import com.example.model.SubjectDefinition
import com.example.ui.components.StudyNoteCard
import com.example.ui.components.subjectIconFor

@Composable
fun SubjectsScreen(
    subjects: List<SubjectDefinition>,
    selectedSubjectId: String?,
    hindiCourse: HindiCourse,
    notes: List<StudyNote>,
    chapterProgress: List<ChapterProgress>,
    onSelectSubject: (String) -> Unit,
    onSelectHindiCourse: (HindiCourse) -> Unit,
    onToggleChapterCompleted: (subjectId: String, sectionName: String, chapterName: String, completed: Boolean) -> Unit,
    onAddCustomSubject: (name: String, sections: String, colorHex: String) -> Unit,
    onAddCustomChapter: (subjectId: String, sectionName: String, chapterName: String) -> Unit,
    onAddNoteForChapter: (subjectId: String, sectionName: String, chapterName: String) -> Unit,
    onOpenNote: (String) -> Unit,
    onTogglePin: (StudyNote) -> Unit,
    onToggleFavourite: (StudyNote) -> Unit,
    onToggleBookmark: (StudyNote) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeSubject = subjects.firstOrNull { it.id == selectedSubjectId } ?: subjects.first()
    var activeSectionName by remember(activeSubject.id, hindiCourse) {
        mutableStateOf(activeSubject.sections.firstOrNull()?.name ?: "")
    }
    var expandedChapter by remember { mutableStateOf<String?>(null) }

    var showAddSubjectDialog by remember { mutableStateOf(false) }
    var showAddChapterDialog by remember { mutableStateOf(false) }

    val completedIds = remember(chapterProgress) {
        chapterProgress.filter { it.completed }
            .map { CbseCurriculum.progressDocId(it.subjectId, it.sectionName, it.chapterName) }
            .toSet()
    }

    val currentSection = activeSubject.sections.firstOrNull { it.name == activeSectionName }
        ?: activeSubject.sections.firstOrNull()

    val allChapters = activeSubject.allChapters()
    val subjectDoneCount = allChapters.count { (sec, chap) ->
        completedIds.contains(CbseCurriculum.progressDocId(activeSubject.id, sec, chap))
    }
    val subjectProgress = if (allChapters.isNotEmpty()) subjectDoneCount.toFloat() / allChapters.size else 0f

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header + Add Custom Subject
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Subjects & Chapters",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "CBSE Class 10 NCERT + Custom",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(
                    onClick = { showAddSubjectDialog = true },
                    modifier = Modifier.testTag("add_custom_subject_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Subject")
                }
            }
        }

        // Subject selector pills
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(subjects, key = { it.id }) { subject ->
                    val selected = subject.id == activeSubject.id
                    FilterChip(
                        selected = selected,
                        onClick = {
                            onSelectSubject(subject.id)
                            activeSectionName = subject.sections.firstOrNull()?.name ?: ""
                            expandedChapter = null
                        },
                        label = { Text(subject.name) },
                        leadingIcon = {
                            Icon(
                                imageVector = subjectIconFor(subject.id),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.testTag("subject_chip_${subject.id}")
                    )
                }
            }
        }

        // Subject Progress Banner
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            activeSubject.accentColor.copy(alpha = 0.35f),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activeSubject.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = activeSubject.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "$subjectDoneCount/${allChapters.size}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = activeSubject.accentColor
                        )
                    }
                    LinearProgressIndicator(
                        progress = { subjectProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(999.dp)),
                        color = activeSubject.accentColor,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )

                    // Hindi Course A / Course B Switcher
                    if (activeSubject.id == "hindi") {
                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("hindi_course_selector")
                        ) {
                            HindiCourse.entries.forEachIndexed { index, course ->
                                SegmentedButton(
                                    selected = hindiCourse == course,
                                    onClick = { onSelectHindiCourse(course) },
                                    shape = SegmentedButtonDefaults.itemShape(index, HindiCourse.entries.size),
                                    modifier = Modifier.testTag("hindi_course_${course.name}")
                                ) {
                                    Text(course.label, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section tabs (e.g., Physics / Chemistry / Biology, History / Geo / Pol Sci / Eco, etc.)
        if (activeSubject.sections.size > 1) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(activeSubject.sections, key = { it.name }) { section ->
                        FilterChip(
                            selected = section.name == currentSection?.name,
                            onClick = {
                                activeSectionName = section.name
                                expandedChapter = null
                            },
                            label = { Text(section.name) },
                            modifier = Modifier.testTag("section_chip_${section.name}")
                        )
                    }
                }
            }
        }

        // Add Custom Chapter row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentSection?.name ?: "Chapters",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = { showAddChapterDialog = true },
                    modifier = Modifier.testTag("add_custom_chapter_btn")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Chapter")
                }
            }
        }

        val chaptersInSection = currentSection?.chapters ?: emptyList()
        if (chaptersInSection.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Text(
                        text = "No chapters in this section yet. Tap + Add Chapter to create one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(chaptersInSection, key = { "${activeSubject.id}_${currentSection?.name}_$it" }) { chapterName ->
                val secName = currentSection?.name ?: ""
                val isCompleted = completedIds.contains(
                    CbseCurriculum.progressDocId(activeSubject.id, secName, chapterName)
                )
                val chapterNotes = notes.filter {
                    it.subjectId == activeSubject.id &&
                        it.chapterName.equals(chapterName, ignoreCase = true)
                }
                val isExpanded = expandedChapter == chapterName

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable {
                            expandedChapter = if (isExpanded) null else chapterName
                        }
                        .testTag("chapter_item_$chapterName"),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier
                            .border(
                                1.dp,
                                if (isCompleted) activeSubject.accentColor.copy(alpha = 0.45f)
                                else MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(18.dp)
                            )
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    onToggleChapterCompleted(
                                        activeSubject.id,
                                        secName,
                                        chapterName,
                                        !isCompleted
                                    )
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("toggle_chapter_done_$chapterName")
                            ) {
                                Icon(
                                    imageVector = if (isCompleted) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                    contentDescription = if (isCompleted) "Mark incomplete" else "Mark chapter complete",
                                    tint = if (isCompleted) activeSubject.accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = chapterName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${chapterNotes.size} saved note(s)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            OutlinedButton(
                                onClick = { onAddNoteForChapter(activeSubject.id, secName, chapterName) },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("add_note_for_$chapterName")
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(2.dp))
                                Text("Note", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        if (isExpanded) {
                            if (chapterNotes.isEmpty()) {
                                Text(
                                    text = "No notes in this chapter yet. Tap + Note to add notes, formulas, or PDFs.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 12.dp, bottom = 4.dp)
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    chapterNotes.forEach { note ->
                                        StudyNoteCard(
                                            note = note,
                                            accentColor = activeSubject.accentColor,
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
            }
        }
    }

    if (showAddSubjectDialog) {
        AddCustomSubjectDialog(
            onDismiss = { showAddSubjectDialog = false },
            onConfirm = { name, sections, colorHex ->
                onAddCustomSubject(name, sections, colorHex)
                showAddSubjectDialog = false
            }
        )
    }

    if (showAddChapterDialog) {
        AddCustomChapterDialog(
            subjectName = activeSubject.name,
            sectionName = currentSection?.name ?: "General",
            onDismiss = { showAddChapterDialog = false },
            onConfirm = { chapName ->
                onAddCustomChapter(activeSubject.id, currentSection?.name ?: "General", chapName)
                showAddChapterDialog = false
            }
        )
    }
}

@Composable
private fun AddCustomSubjectDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, sections: String, colorHex: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var sections by remember { mutableStateOf("Theory, Practical") }
    val colorOptions = listOf("#8B5CF6", "#38BDF8", "#10B981", "#F59E0B", "#EC4899")
    var selectedColor by remember { mutableStateOf(colorOptions.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Custom Subject") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Subject Name (e.g. Information Technology)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = sections,
                    onValueChange = { sections = it },
                    label = { Text("Sections (comma separated)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    colorOptions.forEach { hex ->
                        val c = androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(hex))
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(c)
                                .border(
                                    width = if (selectedColor == hex) 3.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = RoundedCornerShape(999.dp)
                                )
                                .clickable { selectedColor = hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, sections, selectedColor) },
                enabled = name.isNotBlank()
            ) {
                Text("Add Subject")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun AddCustomChapterDialog(
    subjectName: String,
    sectionName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var chapterName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Chapter to $subjectName") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Section: $sectionName",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = chapterName,
                    onValueChange = { chapterName = it },
                    label = { Text("Chapter Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(chapterName) },
                enabled = chapterName.isNotBlank()
            ) {
                Text("Add Chapter")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
