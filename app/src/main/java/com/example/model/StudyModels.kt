package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class NoteType(val label: String) {
    NOTES("Study Notes"),
    FORMULA("Formulas"),
    DEFINITION("Definitions"),
    FIVE_MARK_QA("5-Mark Questions"),
    IMPORTANT_QA("Important Q&A");

    companion object {
        fun fromRaw(raw: String?): NoteType =
            entries.firstOrNull { it.name == raw } ?: NOTES
    }
}

enum class HindiCourse(val label: String, val subtitle: String) {
    COURSE_A("Course A", "Kshitij-2 & Kritika-2"),
    COURSE_B("Course B", "Sparsh-2 & Sanchayan-2");

    companion object {
        fun fromRaw(raw: String?): HindiCourse =
            entries.firstOrNull { it.name == raw } ?: COURSE_A
    }
}

enum class AttachmentType {
    IMAGE,
    PDF;

    companion object {
        fun fromRaw(raw: String?): AttachmentType =
            entries.firstOrNull { it.name == raw } ?: IMAGE
    }
}

@Keep
@IgnoreExtraProperties
data class NoteAttachment(
    val id: String = "",
    val name: String = "",
    val type: String = AttachmentType.IMAGE.name,
    val localPath: String = "",
    val sizeBytes: Long = 0L
) {
    fun attachmentType(): AttachmentType = AttachmentType.fromRaw(type)

    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "name" to name,
        "type" to type,
        "localPath" to localPath,
        "sizeBytes" to sizeBytes
    )
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val hindiCourse: String = HindiCourse.COURSE_A.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun course(): HindiCourse = HindiCourse.fromRaw(hindiCourse)
}

@Keep
@IgnoreExtraProperties
data class StudyNote(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val content: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val sectionName: String = "",
    val chapterName: String = "",
    val noteType: String = NoteType.NOTES.name,
    val tags: List<String> = emptyList(),
    val attachments: List<NoteAttachment> = emptyList(),
    val pinned: Boolean = false,
    val favourite: Boolean = false,
    val bookmarked: Boolean = false,
    val aiTopic: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun resolvedNoteType(): NoteType = NoteType.fromRaw(noteType)
}

@Keep
@IgnoreExtraProperties
data class CustomSubject(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val sections: List<String> = emptyList(),
    val colorHex: String = "#8B5CF6",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CustomChapter(
    @DocumentId val id: String = "",
    val userId: String = "",
    val subjectId: String = "",
    val sectionName: String = "",
    val name: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChapterProgress(
    @DocumentId val id: String = "",
    val userId: String = "",
    val subjectId: String = "",
    val sectionName: String = "",
    val chapterName: String = "",
    val completed: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FlashcardItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val subjectName: String = "",
    val chapterName: String = "",
    val question: String = "",
    val answer: String = "",
    val mastered: Boolean = false,
    val noteId: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RevisionQuestion(
    @DocumentId val id: String = "",
    val userId: String = "",
    val subjectName: String = "",
    val chapterName: String = "",
    val marks: Long = 5L,
    val question: String = "",
    val answer: String = "",
    val noteId: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class QuizAttempt(
    @DocumentId val id: String = "",
    val userId: String = "",
    val subjectName: String = "",
    val chapterName: String = "",
    val score: Long = 0L,
    val totalQuestions: Long = 5L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class AiOrganizationResult(
    val detectedSubject: String,
    val detectedSection: String,
    val detectedChapter: String,
    val detectedTopic: String,
    val detectedNoteType: NoteType,
    val suggestedTags: List<String>,
    val confidence: String,
    val uncertaintyReason: String
)

data class AiSmartSearchResult(
    val interpretedIntent: String,
    val targetSubject: String?,
    val targetChapter: String?,
    val targetNoteType: NoteType?,
    val keywords: List<String>,
    val matchedNoteIds: List<String>
)

data class GeneratedMcq(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

data class GeneratedFlashcard(
    val question: String,
    val answer: String
)

data class GeneratedImportantQuestion(
    val marks: Int,
    val question: String,
    val answer: String
)
