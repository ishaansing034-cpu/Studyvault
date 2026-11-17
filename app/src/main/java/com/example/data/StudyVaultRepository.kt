package com.example.data

import android.content.Context
import com.example.R
import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
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
import com.example.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class StudyVaultRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing StudyVault.")
    }

    private fun <T> observeUserSubcollection(
        userId: String,
        collectionName: String,
        clazz: Class<T>
    ): Flow<List<T>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user changed." }
        val path = "users/$uid/$collectionName"
        emitAll(
            db.collection("users").document(uid).collection(collectionName)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(clazz, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeNotes(userId: String): Flow<List<StudyNote>> =
        observeUserSubcollection(userId, "notes", StudyNote::class.java)

    fun observeCustomSubjects(userId: String): Flow<List<CustomSubject>> =
        observeUserSubcollection(userId, "customSubjects", CustomSubject::class.java)

    fun observeCustomChapters(userId: String): Flow<List<CustomChapter>> =
        observeUserSubcollection(userId, "customChapters", CustomChapter::class.java)

    fun observeChapterProgress(userId: String): Flow<List<ChapterProgress>> =
        observeUserSubcollection(userId, "chapterProgress", ChapterProgress::class.java)

    fun observeFlashcards(userId: String): Flow<List<FlashcardItem>> =
        observeUserSubcollection(userId, "flashcards", FlashcardItem::class.java)

    fun observeRevisionQuestions(userId: String): Flow<List<RevisionQuestion>> =
        observeUserSubcollection(userId, "revisionQuestions", RevisionQuestion::class.java)

    fun observeQuizAttempts(userId: String): Flow<List<QuizAttempt>> =
        observeUserSubcollection(userId, "quizAttempts", QuizAttempt::class.java)

    suspend fun ensureUserProfile(
        userId: String,
        displayName: String,
        email: String?,
        photoUrl: String?,
        defaultCourse: HindiCourse
    ): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            val docRef = db.collection("users").document(uid)
            val existing = docRef.get().await()
            val safeName = displayName.trim().take(100).ifBlank { "CBSE Student" }
            val safeEmail = email?.trim()?.take(200)
            val safePhoto = photoUrl?.trim()?.take(500)

            if (!existing.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "email" to safeEmail,
                    "photoUrl" to safePhoto,
                    "hindiCourse" to defaultCourse.name,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).await()
            } else {
                val updates = mapOf(
                    "displayName" to safeName,
                    "email" to safeEmail,
                    "photoUrl" to safePhoto,
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateUserHindiCourse(userId: String, course: HindiCourse): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            val docRef = db.collection("users").document(uid)
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "hindiCourse" to course.name,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveNote(
        userId: String,
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
    ): Result<String> {
        val cleanTitle = title.trim().take(160)
        require(cleanTitle.isNotEmpty()) { "Note title cannot be empty." }
        val cleanDescription = description.trim().take(600)
        val cleanContent = content.take(50000)
        val cleanSubjectId = subjectId.trim().take(80).ifBlank { "custom" }
        val cleanSubjectName = subjectName.trim().take(80).ifBlank { "General" }
        val cleanSectionName = sectionName.trim().take(80)
        val cleanChapterName = chapterName.trim().take(160).ifBlank { "General" }
        val cleanTags = tags.map { it.trim().take(40) }.filter { it.isNotEmpty() }.distinct().take(15)
        val cleanAttachments = attachments.take(10).map { it.toMap() }
        val cleanAiTopic = aiTopic?.trim()?.take(200)?.takeIf { it.isNotEmpty() }

        var docPath = "users/$userId/notes"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            val notesCol = db.collection("users").document(uid).collection("notes")

            if (existingNoteId.isNullOrBlank()) {
                val docRef = notesCol.document()
                docPath = docRef.path
                val payload = mapOf(
                    "userId" to uid,
                    "title" to cleanTitle,
                    "description" to cleanDescription,
                    "content" to cleanContent,
                    "subjectId" to cleanSubjectId,
                    "subjectName" to cleanSubjectName,
                    "sectionName" to cleanSectionName,
                    "chapterName" to cleanChapterName,
                    "noteType" to noteType.name,
                    "tags" to cleanTags,
                    "attachments" to cleanAttachments,
                    "pinned" to pinned,
                    "favourite" to favourite,
                    "bookmarked" to bookmarked,
                    "aiTopic" to cleanAiTopic,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).await()
                Result.success(docRef.id)
            } else {
                val docRef = notesCol.document(existingNoteId)
                docPath = docRef.path
                val updates = mutableMapOf<String, Any>(
                    "title" to cleanTitle,
                    "description" to cleanDescription,
                    "content" to cleanContent,
                    "subjectId" to cleanSubjectId,
                    "subjectName" to cleanSubjectName,
                    "sectionName" to cleanSectionName,
                    "chapterName" to cleanChapterName,
                    "noteType" to noteType.name,
                    "tags" to cleanTags,
                    "attachments" to cleanAttachments,
                    "pinned" to pinned,
                    "favourite" to favourite,
                    "bookmarked" to bookmarked,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (cleanAiTopic != null) {
                    updates["aiTopic"] = cleanAiTopic
                }
                docRef.update(updates).await()
                Result.success(existingNoteId)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(
                e,
                if (existingNoteId.isNullOrBlank()) OperationType.CREATE else OperationType.UPDATE,
                docPath
            )
            Result.failure(e)
        }
    }

    suspend fun toggleNoteFlags(
        userId: String,
        noteId: String,
        pinned: Boolean? = null,
        favourite: Boolean? = null,
        bookmarked: Boolean? = null
    ): Result<Unit> {
        val docPath = "users/$userId/notes/$noteId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            require(noteId.isNotBlank())
            val updates = mutableMapOf<String, Any>("updatedAt" to FieldValue.serverTimestamp())
            if (pinned != null) updates["pinned"] = pinned
            if (favourite != null) updates["favourite"] = favourite
            if (bookmarked != null) updates["bookmarked"] = bookmarked
            db.collection("users").document(uid).collection("notes").document(noteId)
                .update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteNote(userId: String, noteId: String): Result<Unit> {
        val docPath = "users/$userId/notes/$noteId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            require(noteId.isNotBlank())
            db.collection("users").document(uid).collection("notes").document(noteId)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun addCustomSubject(
        userId: String,
        name: String,
        sections: List<String>,
        colorHex: String
    ): Result<String> {
        val cleanName = name.trim().take(80)
        require(cleanName.isNotEmpty()) { "Subject name cannot be empty." }
        val cleanSections = sections.map { it.trim().take(60) }.filter { it.isNotEmpty() }.distinct().take(10)
        val cleanColor = colorHex.trim().take(12).ifBlank { "#8B5CF6" }
        var docPath = "users/$userId/customSubjects"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val docRef = db.collection("users").document(uid).collection("customSubjects").document()
            docPath = docRef.path
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "name" to cleanName,
                    "sections" to cleanSections,
                    "colorHex" to cleanColor,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun addCustomChapter(
        userId: String,
        subjectId: String,
        sectionName: String,
        chapterName: String
    ): Result<Unit> {
        val cleanChapter = chapterName.trim().take(160)
        require(cleanChapter.isNotEmpty()) { "Chapter name cannot be empty." }
        val cleanSubjectId = subjectId.trim().take(80)
        val cleanSection = sectionName.trim().take(80)
        var docPath = "users/$userId/customChapters"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val docRef = db.collection("users").document(uid).collection("customChapters").document()
            docPath = docRef.path
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "subjectId" to cleanSubjectId,
                    "sectionName" to cleanSection,
                    "name" to cleanChapter,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun setChapterCompleted(
        userId: String,
        subjectId: String,
        sectionName: String,
        chapterName: String,
        completed: Boolean
    ): Result<Unit> {
        val docId = CbseCurriculum.progressDocId(subjectId, sectionName, chapterName)
        val docPath = "users/$userId/chapterProgress/$docId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val docRef = db.collection("users").document(uid)
                .collection("chapterProgress").document(docId)
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                docRef.update(
                    mapOf(
                        "completed" to completed,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "subjectId" to subjectId.take(80),
                        "sectionName" to sectionName.take(80),
                        "chapterName" to chapterName.take(160),
                        "completed" to completed,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun addFlashcard(
        userId: String,
        subjectName: String,
        chapterName: String,
        question: String,
        answer: String,
        noteId: String? = null
    ): Result<Unit> {
        val cleanQ = question.trim().take(1000)
        val cleanA = answer.trim().take(3000)
        require(cleanQ.isNotEmpty() && cleanA.isNotEmpty()) { "Question and answer cannot be empty." }
        var docPath = "users/$userId/flashcards"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val docRef = db.collection("users").document(uid).collection("flashcards").document()
            docPath = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "subjectName" to subjectName.trim().take(80).ifBlank { "General" },
                "chapterName" to chapterName.trim().take(160).ifBlank { "General" },
                "question" to cleanQ,
                "answer" to cleanA,
                "mastered" to false,
                "noteId" to noteId?.trim()?.take(128)?.takeIf { it.isNotEmpty() },
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleFlashcardMastered(
        userId: String,
        cardId: String,
        mastered: Boolean
    ): Result<Unit> {
        val docPath = "users/$userId/flashcards/$cardId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            db.collection("users").document(uid).collection("flashcards").document(cardId)
                .update(mapOf("mastered" to mastered, "updatedAt" to FieldValue.serverTimestamp()))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteFlashcard(userId: String, cardId: String): Result<Unit> {
        val docPath = "users/$userId/flashcards/$cardId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            db.collection("users").document(uid).collection("flashcards").document(cardId)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun addRevisionQuestion(
        userId: String,
        subjectName: String,
        chapterName: String,
        marks: Int,
        question: String,
        answer: String,
        noteId: String? = null
    ): Result<Unit> {
        val cleanQ = question.trim().take(1000)
        val cleanA = answer.trim().take(4000)
        require(cleanQ.isNotEmpty() && cleanA.isNotEmpty()) { "Question and answer cannot be empty." }
        val boundedMarks = marks.coerceIn(1, 10)
        var docPath = "users/$userId/revisionQuestions"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val docRef = db.collection("users").document(uid).collection("revisionQuestions").document()
            docPath = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "subjectName" to subjectName.trim().take(80).ifBlank { "General" },
                "chapterName" to chapterName.trim().take(160).ifBlank { "General" },
                "marks" to boundedMarks,
                "question" to cleanQ,
                "answer" to cleanA,
                "noteId" to noteId?.trim()?.take(128)?.takeIf { it.isNotEmpty() },
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteRevisionQuestion(userId: String, questionId: String): Result<Unit> {
        val docPath = "users/$userId/revisionQuestions/$questionId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            db.collection("users").document(uid).collection("revisionQuestions").document(questionId)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveQuizAttempt(
        userId: String,
        subjectName: String,
        chapterName: String,
        score: Int,
        totalQuestions: Int
    ): Result<Unit> {
        var docPath = "users/$userId/quizAttempts"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val docRef = db.collection("users").document(uid).collection("quizAttempts").document()
            docPath = docRef.path
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "subjectName" to subjectName.trim().take(80).ifBlank { "General" },
                    "chapterName" to chapterName.trim().take(160).ifBlank { "General" },
                    "score" to score.coerceIn(0, 100),
                    "totalQuestions" to totalQuestions.coerceIn(1, 100),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }
}
