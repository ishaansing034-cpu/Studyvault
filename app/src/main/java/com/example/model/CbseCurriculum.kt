package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.EnglishAccent
import com.example.ui.theme.HindiAccent
import com.example.ui.theme.MathsAccent
import com.example.ui.theme.ScienceAccent
import com.example.ui.theme.SocialAccent

data class SubjectSection(
    val name: String,
    val chapters: List<String>
)

data class SubjectDefinition(
    val id: String,
    val name: String,
    val subtitle: String,
    val accentColor: Color,
    val isCustom: Boolean = false,
    val sections: List<SubjectSection>
) {
    fun allChapters(): List<Pair<String, String>> =
        sections.flatMap { section -> section.chapters.map { chapter -> section.name to chapter } }
}

object CbseCurriculum {

    val mathsSubject = SubjectDefinition(
        id = "maths",
        name = "Maths",
        subtitle = "NCERT Class 10 • 14 Chapters",
        accentColor = MathsAccent,
        sections = listOf(
            SubjectSection(
                name = "NCERT Chapters",
                chapters = listOf(
                    "Real Numbers",
                    "Polynomials",
                    "Pair of Linear Equations in Two Variables",
                    "Quadratic Equations",
                    "Arithmetic Progressions",
                    "Triangles",
                    "Coordinate Geometry",
                    "Introduction to Trigonometry",
                    "Some Applications of Trigonometry",
                    "Circles",
                    "Areas Related to Circles",
                    "Surface Areas and Volumes",
                    "Statistics",
                    "Probability"
                )
            )
        )
    )

    val scienceSubject = SubjectDefinition(
        id = "science",
        name = "Science",
        subtitle = "Physics • Chemistry • Biology",
        accentColor = ScienceAccent,
        sections = listOf(
            SubjectSection(
                name = "Physics",
                chapters = listOf(
                    "Light – Reflection and Refraction",
                    "The Human Eye and the Colourful World",
                    "Electricity",
                    "Magnetic Effects of Electric Current"
                )
            ),
            SubjectSection(
                name = "Chemistry",
                chapters = listOf(
                    "Chemical Reactions and Equations",
                    "Acids, Bases and Salts",
                    "Metals and Non-metals",
                    "Carbon and its Compounds"
                )
            ),
            SubjectSection(
                name = "Biology",
                chapters = listOf(
                    "Life Processes",
                    "Control and Coordination",
                    "How do Organisms Reproduce",
                    "Heredity",
                    "Our Environment"
                )
            )
        )
    )

    val socialScienceSubject = SubjectDefinition(
        id = "social_science",
        name = "Social Science",
        subtitle = "History • Geo • Pol Sci • Eco",
        accentColor = SocialAccent,
        sections = listOf(
            SubjectSection(
                name = "History",
                chapters = listOf(
                    "The Rise of Nationalism in Europe",
                    "Nationalism in India",
                    "The Making of a Global World",
                    "The Age of Industrialisation",
                    "Print Culture and the Modern World"
                )
            ),
            SubjectSection(
                name = "Geography",
                chapters = listOf(
                    "Resources and Development",
                    "Forest and Wildlife Resources",
                    "Water Resources",
                    "Agriculture",
                    "Minerals and Energy Resources",
                    "Manufacturing Industries",
                    "Lifelines of National Economy"
                )
            ),
            SubjectSection(
                name = "Political Science",
                chapters = listOf(
                    "Power-sharing",
                    "Federalism",
                    "Gender, Religion and Caste",
                    "Political Parties",
                    "Outcomes of Democracy"
                )
            ),
            SubjectSection(
                name = "Economics",
                chapters = listOf(
                    "Development",
                    "Sectors of the Indian Economy",
                    "Money and Credit",
                    "Globalisation and the Indian Economy"
                )
            )
        )
    )

    val englishSubject = SubjectDefinition(
        id = "english",
        name = "English",
        subtitle = "First Flight • Footprints • Writing • Grammar",
        accentColor = EnglishAccent,
        sections = listOf(
            SubjectSection(
                name = "First Flight",
                chapters = listOf(
                    "A Letter to God",
                    "Nelson Mandela: Long Walk to Freedom",
                    "Two Stories about Flying",
                    "From the Diary of Anne Frank",
                    "Glimpses of India",
                    "Mijbil the Otter",
                    "Madam Rides the Bus",
                    "The Sermon at Benares",
                    "The Proposal",
                    "Poems: Dust of Snow & Fire and Ice",
                    "Poems: A Tiger in the Zoo & How to Tell Wild Animals",
                    "Poems: The Ball Poem & Amanda!",
                    "Poems: The Trees & Fog",
                    "Poems: The Tale of Custard the Dragon & For Anne Gregory"
                )
            ),
            SubjectSection(
                name = "Footprints Without Feet",
                chapters = listOf(
                    "A Triumph of Surgery",
                    "The Thief's Story",
                    "The Midnight Visitor",
                    "A Question of Trust",
                    "Footprints Without Feet",
                    "The Making of a Scientist",
                    "The Necklace",
                    "Bholi",
                    "The Book That Saved the Earth"
                )
            ),
            SubjectSection(
                name = "Writing Skills",
                chapters = listOf(
                    "Formal Letter (Editor / Complaint)",
                    "Formal Letter (Order / Enquiry)",
                    "Analytical Paragraph"
                )
            ),
            SubjectSection(
                name = "Grammar",
                chapters = listOf(
                    "Tenses",
                    "Modals",
                    "Subject-Verb Concord",
                    "Reported Speech",
                    "Determiners"
                )
            )
        )
    )

    fun hindiSubject(course: HindiCourse): SubjectDefinition {
        val sections = if (course == HindiCourse.COURSE_A) {
            listOf(
                SubjectSection(
                    name = "क्षितिज भाग-2 (गद्य और काव्य)",
                    chapters = listOf(
                        "सूरदास के पद",
                        "राम-लक्ष्मण-परशुराम संवाद",
                        "आत्मकथ्य",
                        "उत्साह और अट नहीं रही है",
                        "यह दंतुरित मुसकान और फसल",
                        "संगतकार",
                        "नेताजी का चश्मा",
                        "बालगोबिन भगत",
                        "लखनवी अंदाज़",
                        "एक कहानी यह भी",
                        "नौबतखाने में इबादत",
                        "संस्कृति"
                    )
                ),
                SubjectSection(
                    name = "कृतिका भाग-2",
                    chapters = listOf(
                        "माता का अँचल",
                        "साना-साना हाथ जोड़ि",
                        "मैं क्यों लिखता हूँ?"
                    )
                ),
                SubjectSection(
                    name = "लेखन कौशल (Writing Formats)",
                    chapters = listOf(
                        "अनुच्छेद लेखन",
                        "औपचारिक एवं अनौपचारिक पत्र",
                        "स्ववृत्त लेखन (Bio-Data)",
                        "औपचारिक ई-मेल लेखन",
                        "विज्ञापन लेखन",
                        "संदेश लेखन"
                    )
                ),
                SubjectSection(
                    name = "व्याकरण (Course A)",
                    chapters = listOf(
                        "रचना के आधार पर वाक्य भेद",
                        "वाच्य",
                        "पद परिचय",
                        "अलंकार"
                    )
                )
            )
        } else {
            listOf(
                SubjectSection(
                    name = "स्पर्श भाग-2 (गद्य और पद्य)",
                    chapters = listOf(
                        "साखी (कबीर)",
                        "पद (मीरा)",
                        "मनुष्यता",
                        "पर्वत प्रदेश में पावस",
                        "तोप",
                        "कर चले हम फ़िदा",
                        "आत्मत्राण",
                        "बड़े भाई साहब",
                        "डायरी का एक पन्ना",
                        "तताँरा-वामीरो कथा",
                        "तीसरी कसम के शिल्पकार शैलेंद्र",
                        "अब कहाँ दूसरे के दुख से दुखी होने वाले",
                        "पतझर में टूटी पत्तियाँ",
                        "कारतूस"
                    )
                ),
                SubjectSection(
                    name = "संचयन भाग-2",
                    chapters = listOf(
                        "हरिहर काका",
                        "सपनों के-से दिन",
                        "टोपी शुक्ला"
                    )
                ),
                SubjectSection(
                    name = "लेखन कौशल (Writing Formats)",
                    chapters = listOf(
                        "अनुच्छेद लेखन",
                        "औपचारिक पत्र लेखन",
                        "सूचना लेखन",
                        "विज्ञापन लेखन",
                        "लघुकथा लेखन",
                        "औपचारिक ई-मेल लेखन"
                    )
                ),
                SubjectSection(
                    name = "व्याकरण (Course B)",
                    chapters = listOf(
                        "पदबंध",
                        "रचना के आधार पर वाक्य रूपांतरण",
                        "समास",
                        "मुहावरे"
                    )
                )
            )
        }

        return SubjectDefinition(
            id = "hindi",
            name = "Hindi",
            subtitle = "${course.label} • ${course.subtitle}",
            accentColor = HindiAccent,
            sections = sections
        )
    }

    fun coreSubjects(hindiCourse: HindiCourse): List<SubjectDefinition> = listOf(
        mathsSubject,
        scienceSubject,
        socialScienceSubject,
        englishSubject,
        hindiSubject(hindiCourse)
    )

    fun mergeSubjects(
        hindiCourse: HindiCourse,
        customSubjects: List<CustomSubject>,
        customChapters: List<CustomChapter>
    ): List<SubjectDefinition> {
        val base = coreSubjects(hindiCourse).map { subject ->
            val extraForSubject = customChapters.filter { it.subjectId == subject.id }
            if (extraForSubject.isEmpty()) {
                subject
            } else {
                val updatedSections = subject.sections.toMutableList()
                extraForSubject.groupBy { it.sectionName.ifBlank { "Custom Chapters" } }
                    .forEach { (sectionName, chapters) ->
                        val existingIdx = updatedSections.indexOfFirst {
                            it.name.equals(sectionName, ignoreCase = true)
                        }
                        if (existingIdx >= 0) {
                            val existing = updatedSections[existingIdx]
                            val mergedChapters = (existing.chapters + chapters.map { it.name }).distinct()
                            updatedSections[existingIdx] = existing.copy(chapters = mergedChapters)
                        } else {
                            updatedSections.add(
                                SubjectSection(
                                    name = sectionName,
                                    chapters = chapters.map { it.name }.distinct()
                                )
                            )
                        }
                    }
                subject.copy(sections = updatedSections)
            }
        }

        val customDefs = customSubjects.map { cs ->
            val chaptersForSubject = customChapters.filter { it.subjectId == cs.id }
            val sectionNames = if (cs.sections.isEmpty()) listOf("General") else cs.sections
            val sections = sectionNames.map { secName ->
                SubjectSection(
                    name = secName,
                    chapters = chaptersForSubject
                        .filter { it.sectionName.equals(secName, ignoreCase = true) || (secName == "General" && it.sectionName.isBlank()) }
                        .map { it.name }
                        .distinct()
                )
            }.toMutableList()

            chaptersForSubject
                .filter { ch -> sectionNames.none { it.equals(ch.sectionName, ignoreCase = true) } }
                .groupBy { it.sectionName.ifBlank { "General" } }
                .forEach { (sec, list) ->
                    sections.add(SubjectSection(sec, list.map { it.name }.distinct()))
                }

            val parsedColor = try {
                Color(android.graphics.Color.parseColor(cs.colorHex))
            } catch (_: Exception) {
                HindiAccent
            }

            SubjectDefinition(
                id = cs.id,
                name = cs.name,
                subtitle = "Custom Subject • ${sections.sumOf { it.chapters.size }} Chapters",
                accentColor = parsedColor,
                isCustom = true,
                sections = sections
            )
        }

        return base + customDefs
    }

    fun progressDocId(subjectId: String, sectionName: String, chapterName: String): String {
        val raw = "${subjectId}_${sectionName}_${chapterName}"
            .lowercase()
            .replace(Regex("[^a-z0-9_\\-]"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')
        return raw.take(120).ifBlank { "chapter_item" }
    }
}
