package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AiStudyRepository
import com.example.data.StudyVaultRepository
import com.example.model.GeneratedFlashcard
import com.example.model.GeneratedImportantQuestion
import com.example.model.GeneratedMcq
import com.example.model.StudyNote
import com.example.model.UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

data class AiChatMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val isStreaming: Boolean = false
)

enum class AssistantAction(val label: String, val promptPrefix: String) {
    SUMMARIZE("Summarize", "Provide a clear, structured revision summary of the selected note(s) with key takeaways."),
    EXPLAIN_SIMPLY("Explain Simply", "Explain the core concepts in simple, everyday Class 10 language with relatable real-life examples."),
    IMPORTANT_QS("Important Qs", "Generate high-yield CBSE Class 10 2-mark, 3-mark, and 5-mark board exam questions with concise marking-scheme answers."),
    CREATE_MCQS("Create MCQs", "Create 5 exam-style multiple choice questions with answers and brief explanations."),
    FLASHCARDS("Flashcards", "Convert the key formulas, dates, definitions, and concepts into crisp Q&A flashcards."),
    EASY_POINTS("Easy Points", "Convert long answers and paragraphs into short, easy-to-memorize bullet points and memory hooks.")
}

class AiAssistantViewModel(
    private val aiRepository: AiStudyRepository,
    private val vaultRepository: StudyVaultRepository,
    private val currentUserId: String
) : ViewModel() {

    private companion object {
        const val TAG = "AiAssistantVM"
    }

    private val _messages = MutableStateFlow<List<AiChatMessage>>(emptyList())
    val messages: StateFlow<List<AiChatMessage>> = _messages.asStateFlow()

    private val _selectedNote = MutableStateFlow<StudyNote?>(null)
    val selectedNote: StateFlow<StudyNote?> = _selectedNote.asStateFlow()

    private val _useOnlySavedNotes = MutableStateFlow(false)
    val useOnlySavedNotes: StateFlow<Boolean> = _useOnlySavedNotes.asStateFlow()

    private val _assistantError = MutableStateFlow<String?>(null)
    val assistantError: StateFlow<String?> = _assistantError.asStateFlow()

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    // Structured generators for interactive Revision Center integration
    private val _quizState = MutableStateFlow<UiState<List<GeneratedMcq>>?>(null)
    val quizState: StateFlow<UiState<List<GeneratedMcq>>?> = _quizState.asStateFlow()

    private val _generatedFlashcardsState = MutableStateFlow<UiState<List<GeneratedFlashcard>>?>(null)
    val generatedFlashcardsState: StateFlow<UiState<List<GeneratedFlashcard>>?> = _generatedFlashcardsState.asStateFlow()

    private val _generatedQuestionsState = MutableStateFlow<UiState<List<GeneratedImportantQuestion>>?>(null)
    val generatedQuestionsState: StateFlow<UiState<List<GeneratedImportantQuestion>>?> = _generatedQuestionsState.asStateFlow()

    private val _savedBanner = MutableStateFlow<String?>(null)
    val savedBanner: StateFlow<String?> = _savedBanner.asStateFlow()

    fun selectNote(note: StudyNote?) {
        _selectedNote.value = note
    }

    fun setUseOnlySavedNotes(onlySaved: Boolean) {
        _useOnlySavedNotes.value = onlySaved
    }

    fun clearBanner() {
        _savedBanner.value = null
    }

    fun clearChat() {
        _messages.value = emptyList()
        _assistantError.value = null
    }

    fun sendPrompt(prompt: String, allNotes: List<StudyNote>) {
        val cleanPrompt = prompt.trim()
        if (cleanPrompt.isEmpty() || _isStreaming.value) return

        val contextNotes = _selectedNote.value?.let { listOf(it) } ?: allNotes
        val userMsg = AiChatMessage(
            id = "u_${System.currentTimeMillis()}",
            isUser = true,
            text = cleanPrompt
        )
        val aiMsgId = "a_${System.currentTimeMillis() + 1}"
        val aiPlaceholder = AiChatMessage(
            id = aiMsgId,
            isUser = false,
            text = "",
            isStreaming = true
        )

        _assistantError.value = null
        _messages.value = _messages.value + userMsg + aiPlaceholder
        _isStreaming.value = true

        viewModelScope.launch {
            val builder = StringBuilder()
            try {
                withTimeout(60_000L) {
                    aiRepository.streamStudyAssistant(
                        prompt = cleanPrompt,
                        relevantNotes = contextNotes,
                        useOnlySavedNotes = _useOnlySavedNotes.value
                    ).collect { chunk ->
                        builder.append(chunk)
                        updateAiMessage(aiMsgId, builder.toString(), streaming = true)
                    }
                }
                if (builder.isBlank()) {
                    updateAiMessage(aiMsgId, "No response received. Please try again.", streaming = false)
                } else {
                    updateAiMessage(aiMsgId, builder.toString(), streaming = false)
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI Assistant stream timed out", e)
                _assistantError.value = "Request timed out after 60 seconds. Please try again."
                removePlaceholderIfEmpty(aiMsgId, builder.toString())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI Assistant stream failed", e)
                _assistantError.value = e.localizedMessage ?: "AI Assistant failed."
                removePlaceholderIfEmpty(aiMsgId, builder.toString())
            } finally {
                _isStreaming.value = false
            }
        }
    }

    fun runQuickAction(action: AssistantAction, allNotes: List<StudyNote>) {
        val note = _selectedNote.value
        val topicLabel = note?.let { "\"${it.title}\" (${it.subjectName} - ${it.chapterName})" }
            ?: "my saved Class 10 notes"
        sendPrompt("${action.promptPrefix}\n\nFocus on: $topicLabel", allNotes)

        when (action) {
            AssistantAction.CREATE_MCQS -> {
                generateInteractiveQuiz(
                    subjectName = note?.subjectName ?: "Science",
                    chapterName = note?.chapterName ?: "Electricity",
                    sourceNote = note
                )
            }
            AssistantAction.FLASHCARDS -> {
                generateFlashcardSet(
                    subjectName = note?.subjectName ?: "Science",
                    chapterName = note?.chapterName ?: "Electricity",
                    sourceNote = note
                )
            }
            AssistantAction.IMPORTANT_QS -> {
                generateImportantQuestionSet(
                    subjectName = note?.subjectName ?: "Science",
                    chapterName = note?.chapterName ?: "Electricity",
                    sourceNote = note
                )
            }
            else -> Unit
        }
    }

    fun generateInteractiveQuiz(subjectName: String, chapterName: String, sourceNote: StudyNote?) {
        viewModelScope.launch {
            _quizState.value = UiState.Loading
            try {
                val list = aiRepository.generateMcqs(subjectName, chapterName, sourceNote, count = 5)
                _quizState.value = UiState.Success(list)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "MCQ generation timed out", e)
                _quizState.value = UiState.Error("Quiz generation timed out. Please retry.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "MCQ generation failed", e)
                _quizState.value = UiState.Error(e.localizedMessage ?: "Failed to generate quiz.")
            }
        }
    }

    fun generateFlashcardSet(subjectName: String, chapterName: String, sourceNote: StudyNote?) {
        viewModelScope.launch {
            _generatedFlashcardsState.value = UiState.Loading
            try {
                val list = aiRepository.generateFlashcards(subjectName, chapterName, sourceNote, count = 5)
                _generatedFlashcardsState.value = UiState.Success(list)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Flashcard generation timed out", e)
                _generatedFlashcardsState.value = UiState.Error("Flashcard generation timed out.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Flashcard generation failed", e)
                _generatedFlashcardsState.value = UiState.Error(e.localizedMessage ?: "Failed to generate flashcards.")
            }
        }
    }

    fun generateImportantQuestionSet(subjectName: String, chapterName: String, sourceNote: StudyNote?) {
        viewModelScope.launch {
            _generatedQuestionsState.value = UiState.Loading
            try {
                val list = aiRepository.generateImportantQuestions(subjectName, chapterName, sourceNote)
                _generatedQuestionsState.value = UiState.Success(list)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Important Qs generation timed out", e)
                _generatedQuestionsState.value = UiState.Error("Question generation timed out.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Important Qs generation failed", e)
                _generatedQuestionsState.value = UiState.Error(e.localizedMessage ?: "Failed to generate questions.")
            }
        }
    }

    fun saveGeneratedFlashcardsToVault(
        cards: List<GeneratedFlashcard>,
        subjectName: String,
        chapterName: String,
        noteId: String?
    ) {
        viewModelScope.launch {
            var saved = 0
            cards.forEach { card ->
                val res = vaultRepository.addFlashcard(
                    userId = currentUserId,
                    subjectName = subjectName,
                    chapterName = chapterName,
                    question = card.question,
                    answer = card.answer,
                    noteId = noteId
                )
                if (res.isSuccess) saved++
            }
            _savedBanner.value = "Saved $saved flashcard(s) to Revision Center!"
        }
    }

    fun saveGeneratedQuestionsToVault(
        questions: List<GeneratedImportantQuestion>,
        subjectName: String,
        chapterName: String,
        noteId: String?
    ) {
        viewModelScope.launch {
            var saved = 0
            questions.forEach { q ->
                val res = vaultRepository.addRevisionQuestion(
                    userId = currentUserId,
                    subjectName = subjectName,
                    chapterName = chapterName,
                    marks = q.marks,
                    question = q.question,
                    answer = q.answer,
                    noteId = noteId
                )
                if (res.isSuccess) saved++
            }
            _savedBanner.value = "Saved $saved important question(s) to Revision Center!"
        }
    }

    private fun updateAiMessage(id: String, text: String, streaming: Boolean) {
        _messages.value = _messages.value.map { msg ->
            if (msg.id == id) msg.copy(text = text, isStreaming = streaming) else msg
        }
    }

    private fun removePlaceholderIfEmpty(id: String, accumulated: String) {
        if (accumulated.isBlank()) {
            _messages.value = _messages.value.filterNot { it.id == id }
        } else {
            updateAiMessage(id, accumulated, streaming = false)
        }
    }
}
