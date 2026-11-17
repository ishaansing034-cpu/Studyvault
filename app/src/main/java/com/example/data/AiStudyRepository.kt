package com.example.data

import android.graphics.Bitmap
import com.example.model.AiOrganizationResult
import com.example.model.AiSmartSearchResult
import com.example.model.AttachmentType
import com.example.model.GeneratedFlashcard
import com.example.model.GeneratedImportantQuestion
import com.example.model.GeneratedMcq
import com.example.model.NoteAttachment
import com.example.model.NoteType
import com.example.model.StudyNote
import com.example.model.SubjectDefinition
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

class AiStudyRepository(
    private val fileManager: FileAttachmentManager
) {
    private val generalModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash"
        )
    }

    private val organizerSchema = Schema.obj(
        properties = mapOf(
            "detectedSubject" to Schema.string(description = "Detected subject name from the CBSE Class 10 curriculum list"),
            "detectedSection" to Schema.string(description = "Detected sub-section e.g. Physics, History, First Flight, Course A"),
            "detectedChapter" to Schema.string(description = "Best matching chapter name"),
            "detectedTopic" to Schema.string(description = "Concise topic name inside the chapter"),
            "detectedNoteType" to Schema.enumeration(
                values = NoteType.entries.map { it.name },
                description = "Category of note"
            ),
            "suggestedTags" to Schema.array(
                items = Schema.string(),
                description = "3 to 5 concise lowercase study tags"
            ),
            "confidence" to Schema.enumeration(
                values = listOf("HIGH", "MEDIUM", "LOW"),
                description = "Confidence in the chapter classification"
            ),
            "uncertaintyReason" to Schema.string(description = "Explanation if chapter confidence is MEDIUM or LOW, or empty string if HIGH")
        )
    )

    private val organizerModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = organizerSchema
            }
        )
    }

    private val searchSchema = Schema.obj(
        properties = mapOf(
            "interpretedIntent" to Schema.string(description = "Brief human-readable summary of what the student is searching for"),
            "targetSubject" to Schema.string(description = "Target subject name if specified, else empty string", nullable = true),
            "targetChapter" to Schema.string(description = "Target chapter name if specified, else empty string", nullable = true),
            "targetNoteType" to Schema.string(description = "One of NOTES, FORMULA, DEFINITION, FIVE_MARK_QA, IMPORTANT_QA, or empty string", nullable = true),
            "keywords" to Schema.array(items = Schema.string(), description = "Key search terms"),
            "matchedNoteIds" to Schema.array(items = Schema.string(), description = "IDs of matching notes ordered by relevance")
        ),
        optionalProperties = listOf("targetSubject", "targetChapter", "targetNoteType")
    )

    private val searchModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = searchSchema
            }
        )
    }

    private val mcqSchema = Schema.obj(
        properties = mapOf(
            "questions" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "question" to Schema.string(),
                        "options" to Schema.array(items = Schema.string(), description = "Exactly 4 options"),
                        "correctIndex" to Schema.integer(description = "0-based index of correct option (0..3)"),
                        "explanation" to Schema.string()
                    )
                )
            )
        )
    )

    private val mcqModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = mcqSchema
            }
        )
    }

    private val flashcardsSchema = Schema.obj(
        properties = mapOf(
            "flashcards" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "question" to Schema.string(),
                        "answer" to Schema.string()
                    )
                )
            )
        )
    )

    private val flashcardsModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = flashcardsSchema
            }
        )
    }

    private val importantQuestionsSchema = Schema.obj(
        properties = mapOf(
            "questions" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "marks" to Schema.integer(description = "CBSE marks weightage: 2, 3, or 5"),
                        "question" to Schema.string(),
                        "answer" to Schema.string(description = "Structured CBSE board exam marking-scheme answer")
                    )
                )
            )
        )
    )

    private val importantQuestionsModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = importantQuestionsSchema
            }
        )
    }

    suspend fun organizeNote(
        title: String,
        description: String,
        noteBody: String,
        attachments: List<NoteAttachment>,
        subjects: List<SubjectDefinition>
    ): AiOrganizationResult {
        val curriculumCatalog = subjects.joinToString("\n") { sub ->
            val secs = sub.sections.joinToString("; ") { sec ->
                "${sec.name}: [${sec.chapters.joinToString(", ")}]"
            }
            "- ${sub.name} -> $secs"
        }

        val bitmaps = mutableListOf<Bitmap>()
        val pdfParts = mutableListOf<ByteArray>()

        for (att in attachments.take(3)) {
            when (att.attachmentType()) {
                AttachmentType.IMAGE -> {
                    fileManager.decodeSampledBitmap(att.localPath, maxDim = 1024)?.let { bitmaps.add(it) }
                }
                AttachmentType.PDF -> {
                    val bytes = fileManager.readBytesBounded(att.localPath, maxBytes = 8 * 1024 * 1024)
                    if (bytes != null) {
                        pdfParts.add(bytes)
                    } else {
                        fileManager.renderPdfPage(att.localPath, 0, 1024)?.first?.let { bitmaps.add(it) }
                    }
                }
            }
        }

        val prompt = buildString {
            appendLine("You are an expert CBSE Class 10 Study Note Organizer.")
            appendLine("Analyze the student's note text and any attached handwritten note images or PDFs.")
            appendLine("Classify it into the exact CBSE Class 10 subject, section, and chapter below:")
            appendLine(curriculumCatalog)
            appendLine()
            appendLine("Student Input:")
            appendLine("Title: $title")
            appendLine("Description: $description")
            appendLine("Content: ${noteBody.take(4000)}")
            appendLine()
            appendLine("If the chapter cannot be determined with high certainty, set confidence to MEDIUM or LOW and explain why in uncertaintyReason.")
        }

        val inputContent = content {
            text(prompt)
            bitmaps.forEach { image(it) }
            pdfParts.forEach { inlineData(it, "application/pdf") }
        }

        val response = withTimeout(60_000L) {
            organizerModel.generateContent(inputContent)
        }
        val jsonText = response.text ?: throw IllegalStateException("AI returned an empty response.")
        val json = JSONObject(jsonText)
        val tagsArray = json.optJSONArray("suggestedTags")
        val tags = if (tagsArray != null) {
            (0 until tagsArray.length()).mapNotNull { idx ->
                tagsArray.optString(idx).trim().takeIf { it.isNotEmpty() }
            }
        } else emptyList()

        return AiOrganizationResult(
            detectedSubject = json.optString("detectedSubject", "Science"),
            detectedSection = json.optString("detectedSection", ""),
            detectedChapter = json.optString("detectedChapter", ""),
            detectedTopic = json.optString("detectedTopic", title.ifBlank { "Key Concept" }),
            detectedNoteType = NoteType.fromRaw(json.optString("detectedNoteType")),
            suggestedTags = tags.take(6),
            confidence = json.optString("confidence", "MEDIUM"),
            uncertaintyReason = json.optString("uncertaintyReason", "")
        )
    }

    suspend fun smartSearch(
        query: String,
        notes: List<StudyNote>
    ): AiSmartSearchResult {
        val compactIndex = notes.take(80).joinToString("\n") { note ->
            val tagsStr = note.tags.joinToString(",")
            "ID=${note.id} | Subject=${note.subjectName} | Section=${note.sectionName} | Chapter=${note.chapterName} | Type=${note.noteType} | Title=${note.title} | Tags=[$tagsStr] | Snippet=${note.content.take(180).replace("\n", " ")}"
        }

        val prompt = """
            You are a smart natural-language search engine for a CBSE Class 10 student's StudyVault.
            Query: "$query"
            
            Saved Notes Index:
            $compactIndex
            
            Identify the student's intent, targetSubject (if any), targetChapter (if any), targetNoteType (one of NOTES, FORMULA, DEFINITION, FIVE_MARK_QA, IMPORTANT_QA if implied), keywords, and return the matching note IDs ordered by relevance.
        """.trimIndent()

        val response = withTimeout(45_000L) {
            searchModel.generateContent(prompt)
        }
        val jsonText = response.text ?: throw IllegalStateException("AI returned an empty search response.")
        val json = JSONObject(jsonText)

        val kwArray = json.optJSONArray("keywords")
        val keywords = if (kwArray != null) {
            (0 until kwArray.length()).mapNotNull { kwArray.optString(it).takeIf { s -> s.isNotBlank() } }
        } else emptyList()

        val idArray = json.optJSONArray("matchedNoteIds")
        val ids = if (idArray != null) {
            (0 until idArray.length()).mapNotNull { idArray.optString(it).takeIf { s -> s.isNotBlank() } }
        } else emptyList()

        val rawType = json.optString("targetNoteType", "").takeIf { it.isNotBlank() }
        val noteType = rawType?.let { raw -> NoteType.entries.firstOrNull { it.name == raw } }

        return AiSmartSearchResult(
            interpretedIntent = json.optString("interpretedIntent", "Results for \"$query\""),
            targetSubject = json.optString("targetSubject", "").takeIf { it.isNotBlank() },
            targetChapter = json.optString("targetChapter", "").takeIf { it.isNotBlank() },
            targetNoteType = noteType,
            keywords = keywords,
            matchedNoteIds = ids
        )
    }

    fun streamStudyAssistant(
        prompt: String,
        relevantNotes: List<StudyNote>,
        useOnlySavedNotes: Boolean
    ): Flow<String> {
        val contextNotes = relevantNotes.take(8).joinToString("\n\n") { note ->
            """
            --- NOTE: "${note.title}" (${note.subjectName} • ${note.chapterName} • ${note.resolvedNoteType().label}) ---
            Description: ${note.description}
            Content: ${note.content.take(2500)}
            """.trimIndent()
        }

        val fullPrompt = buildString {
            appendLine("You are StudyVault AI, a warm, crystal-clear CBSE Class 10 board exam study mentor.")
            if (useOnlySavedNotes) {
                appendLine("IMPORTANT: Answer using ONLY the student's saved notes provided below. Cite the note titles you used. If the saved notes do not contain the answer, clearly tell the student which topic is missing from their vault.")
            } else {
                appendLine("Ground your response in the student's saved notes below when available, and supplement clearly with accurate CBSE Class 10 NCERT concepts.")
            }
            appendLine()
            if (contextNotes.isNotBlank()) {
                appendLine("STUDENT SAVED NOTES:")
                appendLine(contextNotes)
                appendLine()
            } else {
                appendLine("(No saved notes selected/available for this topic.)")
                appendLine()
            }
            appendLine("STUDENT REQUEST:")
            appendLine(prompt)
        }

        return generalModel.generateContentStream(fullPrompt).map { chunk ->
            chunk.text.orEmpty()
        }
    }

    suspend fun generateMcqs(
        subjectName: String,
        chapterName: String,
        sourceNote: StudyNote?,
        count: Int = 5
    ): List<GeneratedMcq> {
        val prompt = buildString {
            appendLine("Generate $count CBSE Class 10 board-exam style multiple choice questions (4 options each).")
            appendLine("Subject: $subjectName | Chapter: $chapterName")
            if (sourceNote != null) {
                appendLine("Base the questions closely on this student note:")
                appendLine("Title: ${sourceNote.title}")
                appendLine(sourceNote.content.take(3500))
            }
        }
        val response = withTimeout(60_000L) {
            mcqModel.generateContent(prompt)
        }
        val json = JSONObject(response.text ?: throw IllegalStateException("Empty MCQ response."))
        val arr = json.getJSONArray("questions")
        return (0 until arr.length()).map { i ->
            val item = arr.getJSONObject(i)
            val optsArr = item.getJSONArray("options")
            val opts = (0 until optsArr.length()).map { optsArr.getString(it) }
            GeneratedMcq(
                question = item.getString("question"),
                options = opts,
                correctIndex = item.optInt("correctIndex", 0).coerceIn(0, (opts.size - 1).coerceAtLeast(0)),
                explanation = item.optString("explanation", "")
            )
        }
    }

    suspend fun generateFlashcards(
        subjectName: String,
        chapterName: String,
        sourceNote: StudyNote?,
        count: Int = 5
    ): List<GeneratedFlashcard> {
        val prompt = buildString {
            appendLine("Generate $count concise, high-recall CBSE Class 10 study flashcards (short question/term -> crisp answer/formula/definition).")
            appendLine("Subject: $subjectName | Chapter: $chapterName")
            if (sourceNote != null) {
                appendLine("Use this student note as primary source:")
                appendLine("Title: ${sourceNote.title}")
                appendLine(sourceNote.content.take(3500))
            }
        }
        val response = withTimeout(60_000L) {
            flashcardsModel.generateContent(prompt)
        }
        val json = JSONObject(response.text ?: throw IllegalStateException("Empty flashcards response."))
        val arr = json.getJSONArray("flashcards")
        return (0 until arr.length()).map { i ->
            val item = arr.getJSONObject(i)
            GeneratedFlashcard(
                question = item.getString("question"),
                answer = item.getString("answer")
            )
        }
    }

    suspend fun generateImportantQuestions(
        subjectName: String,
        chapterName: String,
        sourceNote: StudyNote?
    ): List<GeneratedImportantQuestion> {
        val prompt = buildString {
            appendLine("Generate 4 high-yield CBSE Class 10 board exam questions (mix of 2-mark, 3-mark, and 5-mark) with clear point-wise marking-scheme answers.")
            appendLine("Subject: $subjectName | Chapter: $chapterName")
            if (sourceNote != null) {
                appendLine("Source note:")
                appendLine("Title: ${sourceNote.title}")
                appendLine(sourceNote.content.take(3500))
            }
        }
        val response = withTimeout(60_000L) {
            importantQuestionsModel.generateContent(prompt)
        }
        val json = JSONObject(response.text ?: throw IllegalStateException("Empty questions response."))
        val arr = json.getJSONArray("questions")
        return (0 until arr.length()).map { i ->
            val item = arr.getJSONObject(i)
            GeneratedImportantQuestion(
                marks = item.optInt("marks", 3).coerceIn(2, 5),
                question = item.getString("question"),
                answer = item.getString("answer")
            )
        }
    }
}
