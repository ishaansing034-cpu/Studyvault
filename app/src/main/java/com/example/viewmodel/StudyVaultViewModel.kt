package com.example.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AiStudyRepository
import com.example.data.FileAttachmentManager
import com.example.data.PreferencesRepository
import com.example.data.StudyVaultRepository
import com.example.model.AiOrganizationResult
import com.example.model.AiSmartSearchResult
import com.example.model.AttachmentType
import com.example.model.CbseCurriculum
import com.example.model.ChapterProgress
import com.example.model.CustomChapter
import com.example.model.CustomSubject
import com.example.model.FlashcardItem
import com.example.model.HindiCourse
import com.example.model.NoteAttachment
import com.example.model.NoteType
import com.example.model.QuizAttempt
import com.example.model.RevisionQuestion
import com.example.model.StudyNote
import com.example.model.SubjectDefinition
import com.example.model.UiState
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardStats(
    val totalNotes: Int = 0,
    val favouriteCount: Int = 0,
    val completedChapters: Int = 0,
    val totalChapters: Int = 1,
    val studyProgressPercent: Int = 0
)

class StudyVaultViewModel(
    private val repository: StudyVaultRepository,
    private val aiRepository: AiStudyRepository,
    private val fileManager: FileAttachmentManager,
    private val preferencesRepository: PreferencesRepository,
    private val currentUserId: String,
    userDisplayName: String,
    userEmail: String?,
    userPhotoUrl: String?
) : ViewModel() {

    private companion object {
        const val TAG = "StudyVaultVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }

    val hindiCourse: StateFlow<HindiCourse> = preferencesRepository.hindiCourse
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), HindiCourse.COURSE_A)

    val notesState: StateFlow<UiState<List<StudyNote>>> = repository.observeNotes(currentUserId)
        .map<List<StudyNote>, UiState<List<StudyNote>>> { list ->
            val sorted = list.sortedWith(
                compareByDescending<StudyNote> { it.pinned }
                    .thenByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
            )
            UiState.Success(sorted)
        }
        .catch { error ->
            emit(UiState.Error(unwrapFirestoreError(error, "Failed to load notes")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    val customSubjects: StateFlow<List<CustomSubject>> = repository.observeCustomSubjects(currentUserId)
        .catch { error ->
            unwrapFirestoreError(error, "Custom subjects stream error")
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    val customChapters: StateFlow<List<CustomChapter>> = repository.observeCustomChapters(currentUserId)
        .catch { error ->
            unwrapFirestoreError(error, "Custom chapters stream error")
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    val chapterProgressList: StateFlow<List<ChapterProgress>> = repository.observeChapterProgress(currentUserId)
        .catch { error ->
            unwrapFirestoreError(error, "Chapter progress stream error")
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    val flashcards: StateFlow<List<FlashcardItem>> = repository.observeFlashcards(currentUserId)
        .map { list -> list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) } }
        .catch { error ->
            unwrapFirestoreError(error, "Flashcards stream error")
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    val revisionQuestions: StateFlow<List<RevisionQuestion>> = repository.observeRevisionQuestions(currentUserId)
        .map { list -> list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) } }
        .catch { error ->
            unwrapFirestoreError(error, "Revision questions stream error")
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    val quizAttempts: StateFlow<List<QuizAttempt>> = repository.observeQuizAttempts(currentUserId)
        .map { list -> list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) } }
        .catch { error ->
            unwrapFirestoreError(error, "Quiz attempts stream error")
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    val allSubjects: StateFlow<List<SubjectDefinition>> = combine(
        hindiCourse,
        customSubjects,
        customChapters
    ) { course, cSubs, cChaps ->
        CbseCurriculum.mergeSubjects(course, cSubs, cChaps)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        CbseCurriculum.coreSubjects(HindiCourse.COURSE_A)
    )

    val dashboardStats: StateFlow<DashboardStats> = combine(
        notesState,
        allSubjects,
        chapterProgressList
    ) { notesUi, subjects, progress ->
        val notes = (notesUi as? UiState.Success)?.data ?: emptyList()
        val totalChapters = subjects.sumOf { sub -> sub.sections.sumOf { it.chapters.size } }.coerceAtLeast(1)
        val completedIds = progress.filter { it.completed }.map {
            CbseCurriculum.progressDocId(it.subjectId, it.sectionName, it.chapterName)
        }.toSet()
        val completedCount = subjects.sumOf { sub ->
            sub.sections.sumOf { sec ->
                sec.chapters.count { chap ->
                    completedIds.contains(CbseCurriculum.progressDocId(sub.id, sec.name, chap))
                }
            }
        }
        val percent = ((completedCount.toFloat() / totalChapters.toFloat()) * 100f).toInt().coerceIn(0, 100)
        DashboardStats(
            totalNotes = notes.size,
            favouriteCount = notes.count { it.favourite },
            completedChapters = completedCount,
            totalChapters = totalChapters,
            studyProgressPercent = percent
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), DashboardStats())

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _aiOrganizerState = MutableStateFlow<UiState<AiOrganizationResult>?>(null)
    val aiOrganizerState: StateFlow<UiState<AiOrganizationResult>?> = _aiOrganizerState.asStateFlow()

    private val _smartSearchState = MutableStateFlow<UiState<Pair<AiSmartSearchResult?, List<StudyNote>>>?>(null)
    val smartSearchState: StateFlow<UiState<Pair<AiSmartSearchResult?, List<StudyNote>>>?> = _smartSearchState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureUserProfile(
                userId = currentUserId,
                displayName = userDisplayName,
                email = userEmail,
                photoUrl = userPhotoUrl,
                defaultCourse = hindiCourse.value
            )
        }
    }

    private fun unwrapFirestoreError(error: Throwable, fallback: String): String {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, fallback, rootError)
        return rootError.localizedMessage ?: fallback
    }

    fun clearMessage() {
        _actionMessage.value = null
    }

    fun clearAiOrganizerState() {
        _aiOrganizerState.value = null
    }

    fun setHindiCourse(course: HindiCourse) {
        viewModelScope.launch {
            preferencesRepository.setHindiCourse(course)
            repository.updateUserHindiCourse(currentUserId, course)
            _actionMessage.value = "Switched Hindi curriculum to ${course.label}"
        }
    }

    suspend fun importPickedAttachment(uri: Uri, type: AttachmentType): Result<NoteAttachment> {
        return fileManager.savePickedFile(uri, currentUserId, type)
    }

    fun saveNote(
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
        aiTopic: String?,
        onSuccess: (String) -> Unit
    ) {
        if (title.isBlank()) {
            _actionMessage.value = "Please enter a note title."
            return
        }
        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.saveNote(
                userId = currentUserId,
                existingNoteId = existingNoteId,
                title = title,
                description = description,
                content = content,
                subjectId = subjectId,
                subjectName = subjectName,
                sectionName = sectionName,
                chapterName = chapterName,
                noteType = noteType,
                tags = tags,
                attachments = attachments,
                pinned = pinned,
                favourite = favourite,
                bookmarked = bookmarked,
                aiTopic = aiTopic
            )
            _isSaving.value = false
            result.onSuccess { savedId ->
                _actionMessage.value = if (existingNoteId == null) "Note saved to StudyVault" else "Note updated"
                onSuccess(savedId)
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Failed to save note."
            }
        }
    }

    fun toggleNoteFlag(note: StudyNote, togglePin: Boolean = false, toggleFav: Boolean = false, toggleBookmark: Boolean = false) {
        viewModelScope.launch {
            val result = repository.toggleNoteFlags(
                userId = currentUserId,
                noteId = note.id,
                pinned = if (togglePin) !note.pinned else null,
                favourite = if (toggleFav) !note.favourite else null,
                bookmarked = if (toggleBookmark) !note.bookmarked else null
            )
            result.onFailure {
                _actionMessage.value = it.localizedMessage ?: "Update failed."
            }
        }
    }

    fun deleteNote(note: StudyNote, onDeleted: () -> Unit) {
        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.deleteNote(currentUserId, note.id)
            _isSaving.value = false
            result.onSuccess {
                note.attachments.forEach { fileManager.deleteLocalFile(it.localPath) }
                _actionMessage.value = "Note deleted"
                onDeleted()
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Failed to delete note."
            }
        }
    }

    fun analyzeNoteWithAi(
        title: String,
        description: String,
        content: String,
        attachments: List<NoteAttachment>
    ) {
        if (title.isBlank() && content.isBlank() && attachments.isEmpty()) {
            _actionMessage.value = "Add a title, text, PDF, or image first so AI can organize it."
            return
        }
        viewModelScope.launch {
            _aiOrganizerState.value = UiState.Loading
            try {
                val res = aiRepository.organizeNote(
                    title = title,
                    description = description,
                    noteBody = content,
                    attachments = attachments,
                    subjects = allSubjects.value
                )
                _aiOrganizerState.value = UiState.Success(res)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI Note Organizer failed", e)
                _aiOrganizerState.value = UiState.Error(e.localizedMessage ?: "AI organization failed.")
            }
        }
    }

    fun runSmartSearch(query: String) {
        val trimmed = query.trim()
        val notes = (notesState.value as? UiState.Success)?.data ?: emptyList()
        if (trimmed.isEmpty()) {
            _smartSearchState.value = UiState.Success(null to notes)
            return
        }

        // Instant local matches first
        val localFiltered = localSearchFilter(trimmed, notes)
        viewModelScope.launch {
            _smartSearchState.value = UiState.Loading
            try {
                if (notes.isEmpty()) {
                    _smartSearchState.value = UiState.Success(null to emptyList())
                    return@launch
                }
                val aiRes = aiRepository.smartSearch(trimmed, notes)
                val noteById = notes.associateBy { it.id }
                val aiMatched = aiRes.matchedNoteIds.mapNotNull { noteById[it] }
                val combined = (aiMatched + localFiltered).distinctBy { it.id }
                _smartSearchState.value = UiState.Success(aiRes to combined)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "AI Smart Search fallback to local", e)
                _smartSearchState.value = UiState.Success(null to localFiltered)
            }
        }
    }

    private fun localSearchFilter(query: String, notes: List<StudyNote>): List<StudyNote> {
        val tokens = query.lowercase().split(Regex("\\s+")).filter { it.length >= 2 }
        if (tokens.isEmpty()) return notes
        return notes.filter { note ->
            val haystack = listOf(
                note.title,
                note.description,
                note.content,
                note.subjectName,
                note.sectionName,
                note.chapterName,
                note.resolvedNoteType().label,
                note.aiTopic.orEmpty(),
                note.tags.joinToString(" ")
            ).joinToString(" ").lowercase()
            tokens.any { token -> haystack.contains(token) }
        }
    }

    fun addCustomSubject(name: String, sectionsText: String, colorHex: String) {
        if (name.isBlank()) {
            _actionMessage.value = "Subject name cannot be empty."
            return
        }
        val sections = sectionsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            .ifEmpty { listOf("General") }
        viewModelScope.launch {
            repository.addCustomSubject(currentUserId, name, sections, colorHex)
                .onSuccess { _actionMessage.value = "Added subject \"$name\"" }
                .onFailure { _actionMessage.value = it.localizedMessage ?: "Could not add subject." }
        }
    }

    fun addCustomChapter(subjectId: String, sectionName: String, chapterName: String) {
        if (chapterName.isBlank()) {
            _actionMessage.value = "Chapter name cannot be empty."
            return
        }
        viewModelScope.launch {
            repository.addCustomChapter(currentUserId, subjectId, sectionName, chapterName)
                .onSuccess { _actionMessage.value = "Added chapter \"$chapterName\"" }
                .onFailure { _actionMessage.value = it.localizedMessage ?: "Could not add chapter." }
        }
    }

    fun toggleChapterCompleted(subjectId: String, sectionName: String, chapterName: String, completed: Boolean) {
        viewModelScope.launch {
            repository.setChapterCompleted(currentUserId, subjectId, sectionName, chapterName, completed)
                .onFailure { _actionMessage.value = it.localizedMessage ?: "Could not update chapter progress." }
        }
    }

    fun addFlashcard(subjectName: String, chapterName: String, question: String, answer: String, noteId: String? = null) {
        if (question.isBlank() || answer.isBlank()) {
            _actionMessage.value = "Enter both front and back of flashcard."
            return
        }
        viewModelScope.launch {
            repository.addFlashcard(currentUserId, subjectName, chapterName, question, answer, noteId)
                .onSuccess { _actionMessage.value = "Flashcard saved to Revision Center" }
                .onFailure { _actionMessage.value = it.localizedMessage ?: "Failed to save flashcard." }
        }
    }

    fun toggleFlashcardMastered(card: FlashcardItem) {
        viewModelScope.launch {
            repository.toggleFlashcardMastered(currentUserId, card.id, !card.mastered)
                .onFailure { _actionMessage.value = it.localizedMessage ?: "Failed to update flashcard." }
        }
    }

    fun deleteFlashcard(card: FlashcardItem) {
        viewModelScope.launch {
            repository.deleteFlashcard(currentUserId, card.id)
                .onFailure { _actionMessage.value = it.localizedMessage ?: "Failed to delete flashcard." }
        }
    }

    fun addRevisionQuestion(
        subjectName: String,
        chapterName: String,
        marks: Int,
        question: String,
        answer: String,
        noteId: String? = null
    ) {
        if (question.isBlank() || answer.isBlank()) {
            _actionMessage.value = "Enter both question and answer."
            return
        }
        viewModelScope.launch {
            repository.addRevisionQuestion(currentUserId, subjectName, chapterName, marks, question, answer, noteId)
                .onSuccess { _actionMessage.value = "Question saved to Revision Center" }
                .onFailure { _actionMessage.value = it.localizedMessage ?: "Failed to save question." }
        }
    }

    fun deleteRevisionQuestion(question: RevisionQuestion) {
        viewModelScope.launch {
            repository.deleteRevisionQuestion(currentUserId, question.id)
                .onFailure { _actionMessage.value = it.localizedMessage ?: "Failed to delete question." }
        }
    }

    fun saveQuizAttempt(subjectName: String, chapterName: String, score: Int, total: Int) {
        viewModelScope.launch {
            repository.saveQuizAttempt(currentUserId, subjectName, chapterName, score, total)
        }
    }
}
