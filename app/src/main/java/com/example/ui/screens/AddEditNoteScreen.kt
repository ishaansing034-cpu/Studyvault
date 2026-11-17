package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.data.FileAttachmentManager
import com.example.model.AiOrganizationResult
import com.example.model.AttachmentType
import com.example.model.NoteAttachment
import com.example.model.NoteType
import com.example.model.StudyNote
import com.example.model.SubjectDefinition
import com.example.model.UiState
import com.example.ui.components.PdfViewerDialog
import com.example.ui.components.RichTextToolbar
import com.example.ui.components.RichTextViewer
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditNoteScreen(
    existingNote: StudyNote?,
    subjects: List<SubjectDefinition>,
    initialSubjectId: String?,
    initialSectionName: String?,
    initialChapterName: String?,
    isSaving: Boolean,
    aiOrganizerState: UiState<AiOrganizationResult>?,
    fileManager: FileAttachmentManager,
    onImportAttachment: suspend (Uri, AttachmentType) -> Result<NoteAttachment>,
    onAnalyzeWithAi: (title: String, description: String, content: String, attachments: List<NoteAttachment>) -> Unit,
    onClearAiOrganizer: () -> Unit,
    onSaveNote: (
        existingNoteId: String?,
        title: String,
        description: String,
        content: String,
        subjectId: String,
        subjectName: String,
        sectionName: String,
        chapterName: String,
        noteType: NoteType,
        tags: List<String>,
        attachments: List<NoteAttachment>,
        pinned: Boolean,
        favourite: Boolean,
        bookmarked: Boolean,
        aiTopic: String?
    ) -> Unit,
    onDeleteNote: (StudyNote) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    val defaultSubject = remember(existingNote, initialSubjectId, subjects) {
        subjects.firstOrNull { it.id == (existingNote?.subjectId ?: initialSubjectId) }
            ?: subjects.first()
    }

    var title by remember(existingNote?.id) { mutableStateOf(existingNote?.title ?: "") }
    var description by remember(existingNote?.id) { mutableStateOf(existingNote?.description ?: "") }
    var contentTfv by remember(existingNote?.id) {
        mutableStateOf(TextFieldValue(existingNote?.content ?: ""))
    }
    var previewRichText by remember { mutableStateOf(false) }

    var selectedSubject by remember(existingNote?.id, defaultSubject.id) {
        mutableStateOf(defaultSubject)
    }
    var selectedSection by remember(existingNote?.id, defaultSubject.id, initialSectionName) {
        mutableStateOf(
            existingNote?.sectionName?.takeIf { it.isNotBlank() }
                ?: initialSectionName?.takeIf { it.isNotBlank() }
                ?: defaultSubject.sections.firstOrNull()?.name
                ?: ""
        )
    }
    var selectedChapter by remember(existingNote?.id, defaultSubject.id, initialChapterName) {
        mutableStateOf(
            existingNote?.chapterName?.takeIf { it.isNotBlank() }
                ?: initialChapterName?.takeIf { it.isNotBlank() }
                ?: defaultSubject.sections.firstOrNull()?.chapters?.firstOrNull()
                ?: "General"
        )
    }
    var selectedNoteType by remember(existingNote?.id) {
        mutableStateOf(existingNote?.resolvedNoteType() ?: NoteType.NOTES)
    }
    var aiTopic by remember(existingNote?.id) { mutableStateOf(existingNote?.aiTopic) }

    var tagInput by remember { mutableStateOf("") }
    val tags = remember(existingNote?.id) {
        mutableStateListOf<String>().apply {
            addAll(existingNote?.tags ?: emptyList())
        }
    }
    val attachments = remember(existingNote?.id) {
        mutableStateListOf<NoteAttachment>().apply {
            addAll(existingNote?.attachments ?: emptyList())
        }
    }

    var pinned by remember(existingNote?.id) { mutableStateOf(existingNote?.pinned ?: false) }
    var favourite by remember(existingNote?.id) { mutableStateOf(existingNote?.favourite ?: false) }
    var bookmarked by remember(existingNote?.id) { mutableStateOf(existingNote?.bookmarked ?: false) }

    var attachmentError by remember { mutableStateOf<String?>(null) }
    var isImporting by remember { mutableStateOf(false) }
    var previewPdfAttachment by remember { mutableStateOf<NoteAttachment?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    var subjectMenuExpanded by remember { mutableStateOf(false) }
    var sectionMenuExpanded by remember { mutableStateOf(false) }
    var chapterMenuExpanded by remember { mutableStateOf(false) }

    // Android Photo Picker (Zero-permission)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isImporting = true
                attachmentError = null
                onImportAttachment(uri, AttachmentType.IMAGE)
                    .onSuccess { if (attachments.size < 10) attachments.add(it) }
                    .onFailure { attachmentError = it.localizedMessage ?: "Failed to attach image." }
                isImporting = false
            }
        }
    }

    // Storage Access Framework PDF picker (Zero-permission)
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isImporting = true
                attachmentError = null
                onImportAttachment(uri, AttachmentType.PDF)
                    .onSuccess { if (attachments.size < 10) attachments.add(it) }
                    .onFailure { attachmentError = it.localizedMessage ?: "Failed to attach PDF." }
                isImporting = false
            }
        }
    }

    fun applyAiOrganization(result: AiOrganizationResult) {
        val matchedSub = subjects.firstOrNull {
            it.name.equals(result.detectedSubject, ignoreCase = true)
        } ?: subjects.firstOrNull {
            result.detectedSubject.contains(it.name, ignoreCase = true)
        } ?: selectedSubject

        selectedSubject = matchedSub
        val matchedSec = matchedSub.sections.firstOrNull {
            it.name.equals(result.detectedSection, ignoreCase = true)
        } ?: matchedSub.sections.firstOrNull { sec ->
            sec.chapters.any { it.equals(result.detectedChapter, ignoreCase = true) }
        } ?: matchedSub.sections.firstOrNull()

        if (matchedSec != null) {
            selectedSection = matchedSec.name
            val matchedChap = matchedSec.chapters.firstOrNull {
                it.equals(result.detectedChapter, ignoreCase = true)
            } ?: matchedSec.chapters.firstOrNull {
                it.contains(result.detectedChapter, ignoreCase = true) ||
                    result.detectedChapter.contains(it, ignoreCase = true)
            }
            selectedChapter = matchedChap ?: result.detectedChapter.ifBlank {
                matchedSec.chapters.firstOrNull() ?: "General"
            }
        } else if (result.detectedChapter.isNotBlank()) {
            selectedChapter = result.detectedChapter
        }

        selectedNoteType = result.detectedNoteType
        aiTopic = result.detectedTopic
        if (title.isBlank()) {
            title = result.detectedTopic
        }
        result.suggestedTags.forEach { t ->
            if (!tags.contains(t) && tags.size < 15) tags.add(t)
        }
        onClearAiOrganizer()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (existingNote == null) "Add Study Note" else "Edit Note",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            if (existingNote != null) {
                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.testTag("delete_note_btn")
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete note",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Title & Description
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Note Title *") },
            placeholder = { Text("e.g. Ohm's Law & Circuit Formulas") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("note_title_input")
        )

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Short Description (Optional)") },
            placeholder = { Text("Quick summary or board exam tip...") },
            maxLines = 2,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("note_description_input")
        )

        // Upload PDF & Handwritten Note Image + AI Note Organizer
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 1.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Attachments & AI Auto-Organizer",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("attach_image_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Photo", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("attach_pdf_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("PDF", maxLines = 1)
                    }
                    Button(
                        onClick = { onAnalyzeWithAi(title, description, contentTfv.text, attachments.toList()) },
                        enabled = aiOrganizerState !is UiState.Loading,
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("ai_organize_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("AI Organize", maxLines = 1)
                    }
                }

                if (isImporting) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text("Copying file to private StudyVault storage...", style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (!attachmentError.isNullOrBlank()) {
                    Text(
                        text = attachmentError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (attachments.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(attachments, key = { it.id }) { att ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                Row(
                                    modifier = Modifier
                                        .clickable {
                                            if (att.attachmentType() == AttachmentType.PDF) {
                                                previewPdfAttachment = att
                                            }
                                        }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (att.attachmentType() == AttachmentType.IMAGE) {
                                        Image(
                                            painter = rememberAsyncImagePainter(File(att.localPath)),
                                            contentDescription = att.name,
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            Icons.Filled.PictureAsPdf,
                                            contentDescription = "PDF file",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.width(110.dp)) {
                                        Text(
                                            text = att.name,
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (att.attachmentType() == AttachmentType.PDF) "Tap to preview" else "Image",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(
                                        onClick = { attachments.remove(att) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Filled.Close, contentDescription = "Remove attachment", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // AI Organizer Status & Confirmation Card
                when (aiOrganizerState) {
                    is UiState.Loading -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Text(
                                text = "Analyzing subject, chapter, topic & tags with Gemini...",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    is UiState.Error -> {
                        Text(
                            text = aiOrganizerState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    is UiState.Success -> {
                        val res = aiOrganizerState.data
                        val uncertain = res.confidence != "HIGH"
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (uncertain) MaterialTheme.colorScheme.tertiaryContainer
                            else MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (uncertain) {
                                        "Confirm Chapter Detection (${res.confidence} confidence)"
                                    } else {
                                        "AI Detected Classification"
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${res.detectedSubject} • ${res.detectedSection} • ${res.detectedChapter}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Topic: ${res.detectedTopic} | Type: ${res.detectedNoteType.label}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                if (res.uncertaintyReason.isNotBlank()) {
                                    Text(
                                        text = res.uncertaintyReason,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { applyAiOrganization(res) },
                                        modifier = Modifier.testTag("confirm_ai_organize_btn")
                                    ) {
                                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(if (uncertain) "Confirm & Apply" else "Apply")
                                    }
                                    TextButton(onClick = onClearAiOrganizer) {
                                        Text("Dismiss")
                                    }
                                }
                            }
                        }
                    }
                    null -> Unit
                }
            }
        }

        // Subject / Section / Chapter Pickers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(
                    onClick = { subjectMenuExpanded = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pick_subject_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(selectedSubject.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                DropdownMenu(
                    expanded = subjectMenuExpanded,
                    onDismissRequest = { subjectMenuExpanded = false }
                ) {
                    subjects.forEach { sub ->
                        DropdownMenuItem(
                            text = { Text(sub.name) },
                            onClick = {
                                selectedSubject = sub
                                val firstSec = sub.sections.firstOrNull()
                                selectedSection = firstSec?.name ?: ""
                                selectedChapter = firstSec?.chapters?.firstOrNull() ?: "General"
                                subjectMenuExpanded = false
                            }
                        )
                    }
                }
            }

            if (selectedSubject.sections.isNotEmpty()) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { sectionMenuExpanded = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pick_section_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(selectedSection.ifBlank { "Section" }, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    DropdownMenu(
                        expanded = sectionMenuExpanded,
                        onDismissRequest = { sectionMenuExpanded = false }
                    ) {
                        selectedSubject.sections.forEach { sec ->
                            DropdownMenuItem(
                                text = { Text(sec.name) },
                                onClick = {
                                    selectedSection = sec.name
                                    selectedChapter = sec.chapters.firstOrNull() ?: "General"
                                    sectionMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Box(modifier = Modifier.fillMaxWidth()) {
            val activeChapters = selectedSubject.sections
                .firstOrNull { it.name == selectedSection }?.chapters
                ?: selectedSubject.allChapters().map { it.second }
            OutlinedButton(
                onClick = { chapterMenuExpanded = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pick_chapter_btn"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Chapter: $selectedChapter", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            DropdownMenu(
                expanded = chapterMenuExpanded,
                onDismissRequest = { chapterMenuExpanded = false }
            ) {
                activeChapters.forEach { chap ->
                    DropdownMenuItem(
                        text = { Text(chap) },
                        onClick = {
                            selectedChapter = chap
                            chapterMenuExpanded = false
                        }
                    )
                }
            }
        }

        // Note Type Chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(NoteType.entries) { nt ->
                FilterChip(
                    selected = selectedNoteType == nt,
                    onClick = { selectedNoteType = nt },
                    label = { Text(nt.label) },
                    modifier = Modifier.testTag("note_type_${nt.name}")
                )
            }
        }

        // Rich Text Editor + Preview Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Rich Note Editor",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            FilterChip(
                selected = previewRichText,
                onClick = { previewRichText = !previewRichText },
                leadingIcon = { Icon(Icons.Filled.Visibility, contentDescription = null, modifier = Modifier.size(16.dp)) },
                label = { Text(if (previewRichText) "Edit Mode" else "Rich Preview") },
                modifier = Modifier.testTag("toggle_rich_preview")
            )
        }

        if (!previewRichText) {
            RichTextToolbar(
                value = contentTfv,
                onValueChange = { contentTfv = it }
            )
            OutlinedTextField(
                value = contentTfv,
                onValueChange = { contentTfv = it },
                label = { Text("Write notes, formulas, or key definitions...") },
                minLines = 7,
                maxLines = 16,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("note_content_input")
            )
        } else {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    RichTextViewer(rawText = contentTfv.text.ifBlank { "*Nothing written yet.*" })
                }
            }
        }

        // Tags Input & Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = tagInput,
                onValueChange = { tagInput = it },
                label = { Text("Add tag (e.g. board-2026)") },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("tag_input")
            )
            Button(
                onClick = {
                    val clean = tagInput.trim().removePrefix("#")
                    if (clean.isNotEmpty() && !tags.contains(clean) && tags.size < 15) {
                        tags.add(clean)
                        tagInput = ""
                    }
                },
                modifier = Modifier
                    .height(52.dp)
                    .testTag("add_tag_btn"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add tag")
            }
        }

        if (tags.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                tags.forEach { t ->
                    InputChip(
                        selected = false,
                        onClick = { tags.remove(t) },
                        label = { Text("#$t") },
                        trailingIcon = { Icon(Icons.Filled.Close, contentDescription = "Remove tag", modifier = Modifier.size(14.dp)) }
                    )
                }
            }
        }

        // Pin / Favourite / Bookmark Toggles
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                FlagSwitchRow(Icons.Filled.PushPin, "Pin to Top", pinned) { pinned = it }
                FlagSwitchRow(Icons.Filled.Favorite, "Mark as Favourite", favourite) { favourite = it }
                FlagSwitchRow(Icons.Filled.Bookmark, "Bookmark in Revision Center", bookmarked) { bookmarked = it }
            }
        }

        Button(
            onClick = {
                onSaveNote(
                    existingNote?.id,
                    title,
                    description,
                    contentTfv.text,
                    selectedSubject.id,
                    selectedSubject.name,
                    selectedSection,
                    selectedChapter,
                    selectedNoteType,
                    tags.toList(),
                    attachments.toList(),
                    pinned,
                    favourite,
                    bookmarked,
                    aiTopic
                )
            },
            enabled = !isSaving && title.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("save_note_btn"),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = if (existingNote == null) "Save Note to StudyVault" else "Update Note",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(16.dp))
    }

    previewPdfAttachment?.let { pdf ->
        PdfViewerDialog(
            attachment = pdf,
            fileManager = fileManager,
            onDismiss = { previewPdfAttachment = null }
        )
    }

    if (showDeleteConfirm && existingNote != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Note?") },
            text = { Text("This will permanently remove \"${existingNote.title}\" and its local attachments.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteNote(existingNote)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun FlagSwitchRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
