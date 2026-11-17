package com.example

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.FolderCopy
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.AiStudyRepository
import com.example.data.FileAttachmentManager
import com.example.data.PreferencesRepository
import com.example.data.StudyVaultRepository
import com.example.firebase.authStateFlow
import com.example.firebase.configureAppCheck
import com.example.model.StudyNote
import com.example.model.UiState
import com.example.ui.screens.AddEditNoteScreen
import com.example.ui.screens.AiAssistantScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.NoteDetailScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.RevisionCenterScreen
import com.example.ui.screens.SmartSearchScreen
import com.example.ui.screens.SubjectsScreen
import com.example.ui.theme.StudyVaultTheme
import com.example.ui.theme.ThemeMode
import com.example.viewmodel.AiAssistantViewModel
import com.example.viewmodel.StudyVaultViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

private object Routes {
    const val HOME = "home"
    const val SUBJECTS = "subjects"
    const val ADD_NOTE = "add_note"
    const val AI_ASSISTANT = "ai_assistant"
    const val PROFILE = "profile"
    const val SMART_SEARCH = "smart_search"
    const val REVISION_CENTER = "revision_center"
    const val NOTE_DETAIL = "note_detail/{noteId}"

    fun noteDetail(noteId: String) = "note_detail/${Uri.encode(noteId)}"
    fun addNote(
        noteId: String? = null,
        subjectId: String? = null,
        section: String? = null,
        chapter: String? = null
    ): String {
        val params = listOfNotNull(
            noteId?.let { "noteId=${Uri.encode(it)}" },
            subjectId?.let { "subjectId=${Uri.encode(it)}" },
            section?.let { "section=${Uri.encode(it)}" },
            chapter?.let { "chapter=${Uri.encode(it)}" }
        )
        return if (params.isEmpty()) ADD_NOTE else "$ADD_NOTE?${params.joinToString("&")}"
    }
}

private data class BottomTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val bottomTabs = listOf(
    BottomTab(Routes.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
    BottomTab(Routes.SUBJECTS, "Subjects", Icons.Filled.FolderCopy, Icons.Outlined.FolderCopy, "nav_subjects"),
    BottomTab(Routes.ADD_NOTE, "Add Note", Icons.Filled.AddCircle, Icons.Outlined.AddCircleOutline, "nav_add_note"),
    BottomTab(Routes.AI_ASSISTANT, "Assistant", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome, "nav_ai_assistant"),
    BottomTab(Routes.PROFILE, "Profile", Icons.Filled.Person, Icons.Outlined.Person, "nav_profile")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val prefsRepo = remember(context) { PreferencesRepository(context.applicationContext) }
            val themeMode by prefsRepo.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.DARK)

            StudyVaultTheme(themeMode = themeMode) {
                AppNavigation(
                    prefsRepo = prefsRepo,
                    themeMode = themeMode
                )
            }
        }
    }
}

@Composable
fun AppNavigation(
    prefsRepo: PreferencesRepository,
    themeMode: ThemeMode
) {
    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            AuthenticatedStudyVault(
                user = user,
                prefsRepo = prefsRepo,
                themeMode = themeMode
            )
        }
    }
}

@Composable
private fun AuthenticatedStudyVault(
    user: FirebaseUser,
    prefsRepo: PreferencesRepository,
    themeMode: ThemeMode
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fileManager = remember(context) { FileAttachmentManager(context.applicationContext) }

    val vaultViewModel: StudyVaultViewModel = viewModel(
        key = "vault_${user.uid}",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val dbId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(dbId)
                val fm = FileAttachmentManager(app)
                val repo = StudyVaultRepository(db)
                val aiRepo = AiStudyRepository(fm)
                val prefs = PreferencesRepository(app)
                StudyVaultViewModel(
                    repository = repo,
                    aiRepository = aiRepo,
                    fileManager = fm,
                    preferencesRepository = prefs,
                    currentUserId = user.uid,
                    userDisplayName = user.displayName ?: "CBSE Student",
                    userEmail = user.email,
                    userPhotoUrl = user.photoUrl?.toString()
                )
            }
        }
    )

    val aiViewModel: AiAssistantViewModel = viewModel(
        key = "ai_${user.uid}",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val dbId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(dbId)
                val fm = FileAttachmentManager(app)
                AiAssistantViewModel(
                    aiRepository = AiStudyRepository(fm),
                    vaultRepository = StudyVaultRepository(db),
                    currentUserId = user.uid
                )
            }
        }
    )

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Routes.HOME

    val notesState by vaultViewModel.notesState.collectAsStateWithLifecycle()
    val notes = (notesState as? UiState.Success)?.data ?: emptyList()
    val subjects by vaultViewModel.allSubjects.collectAsStateWithLifecycle()
    val hindiCourse by vaultViewModel.hindiCourse.collectAsStateWithLifecycle()
    val chapterProgress by vaultViewModel.chapterProgressList.collectAsStateWithLifecycle()
    val flashcards by vaultViewModel.flashcards.collectAsStateWithLifecycle()
    val revisionQuestions by vaultViewModel.revisionQuestions.collectAsStateWithLifecycle()
    val quizAttempts by vaultViewModel.quizAttempts.collectAsStateWithLifecycle()
    val dashboardStats by vaultViewModel.dashboardStats.collectAsStateWithLifecycle()
    val isSaving by vaultViewModel.isSaving.collectAsStateWithLifecycle()
    val aiOrganizerState by vaultViewModel.aiOrganizerState.collectAsStateWithLifecycle()
    val smartSearchState by vaultViewModel.smartSearchState.collectAsStateWithLifecycle()
    val actionMessage by vaultViewModel.actionMessage.collectAsStateWithLifecycle()

    val aiMessages by aiViewModel.messages.collectAsStateWithLifecycle()
    val aiSelectedNote by aiViewModel.selectedNote.collectAsStateWithLifecycle()
    val aiOnlySaved by aiViewModel.useOnlySavedNotes.collectAsStateWithLifecycle()
    val aiStreaming by aiViewModel.isStreaming.collectAsStateWithLifecycle()
    val aiError by aiViewModel.assistantError.collectAsStateWithLifecycle()
    val aiFlashcardsState by aiViewModel.generatedFlashcardsState.collectAsStateWithLifecycle()
    val aiQuestionsState by aiViewModel.generatedQuestionsState.collectAsStateWithLifecycle()
    val aiQuizState by aiViewModel.quizState.collectAsStateWithLifecycle()
    val aiSavedBanner by aiViewModel.savedBanner.collectAsStateWithLifecycle()

    var selectedSubjectId by remember { mutableStateOf<String?>(null) }
    var revisionInitialTab by remember { mutableIntStateOf(0) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(actionMessage) {
        val msg = actionMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            vaultViewModel.clearMessage()
        }
    }

    val showBottomBar = currentRoute.startsWith(Routes.HOME) ||
        currentRoute.startsWith(Routes.SUBJECTS) ||
        currentRoute.startsWith(Routes.ADD_NOTE) ||
        currentRoute.startsWith(Routes.AI_ASSISTANT) ||
        currentRoute.startsWith(Routes.PROFILE)

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomTabs.forEach { tab ->
                        val selected = when (tab.route) {
                            Routes.ADD_NOTE -> currentRoute.startsWith(Routes.ADD_NOTE)
                            else -> currentRoute == tab.route
                        }
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                val target = if (tab.route == Routes.ADD_NOTE) Routes.addNote() else tab.route
                                navController.navigate(target) {
                                    popUpTo(Routes.HOME) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = tab.route != Routes.ADD_NOTE
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.label
                                )
                            },
                            label = {
                                Text(
                                    text = tab.label,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            modifier = Modifier.testTag(tab.testTag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxSize()
            ) {
                composable(Routes.HOME) {
                    HomeScreen(
                        studentName = user.displayName ?: "Class 10 Student",
                        stats = dashboardStats,
                        subjects = subjects,
                        notesState = notesState,
                        chapterProgress = chapterProgress,
                        onOpenSmartSearch = { navController.navigate(Routes.SMART_SEARCH) },
                        onQuickAddNote = { navController.navigate(Routes.addNote()) },
                        onOpenSubject = { subId ->
                            selectedSubjectId = subId
                            navController.navigate(Routes.SUBJECTS) {
                                popUpTo(Routes.HOME) { saveState = true }
                                launchSingleTop = true
                            }
                        },
                        onOpenNote = { noteId -> navController.navigate(Routes.noteDetail(noteId)) },
                        onOpenRevisionCenter = {
                            revisionInitialTab = 0
                            navController.navigate(Routes.REVISION_CENTER)
                        },
                        onTogglePin = { vaultViewModel.toggleNoteFlag(it, togglePin = true) },
                        onToggleFavourite = { vaultViewModel.toggleNoteFlag(it, toggleFav = true) },
                        onToggleBookmark = { vaultViewModel.toggleNoteFlag(it, toggleBookmark = true) }
                    )
                }

                composable(Routes.SUBJECTS) {
                    SubjectsScreen(
                        subjects = subjects,
                        selectedSubjectId = selectedSubjectId,
                        hindiCourse = hindiCourse,
                        notes = notes,
                        chapterProgress = chapterProgress,
                        onSelectSubject = { selectedSubjectId = it },
                        onSelectHindiCourse = { vaultViewModel.setHindiCourse(it) },
                        onToggleChapterCompleted = { subId, sec, chap, done ->
                            vaultViewModel.toggleChapterCompleted(subId, sec, chap, done)
                        },
                        onAddCustomSubject = { name, secs, hex ->
                            vaultViewModel.addCustomSubject(name, secs, hex)
                        },
                        onAddCustomChapter = { subId, sec, chap ->
                            vaultViewModel.addCustomChapter(subId, sec, chap)
                        },
                        onAddNoteForChapter = { subId, sec, chap ->
                            navController.navigate(Routes.addNote(subjectId = subId, section = sec, chapter = chap))
                        },
                        onOpenNote = { noteId -> navController.navigate(Routes.noteDetail(noteId)) },
                        onTogglePin = { vaultViewModel.toggleNoteFlag(it, togglePin = true) },
                        onToggleFavourite = { vaultViewModel.toggleNoteFlag(it, toggleFav = true) },
                        onToggleBookmark = { vaultViewModel.toggleNoteFlag(it, toggleBookmark = true) }
                    )
                }

                composable(
                    route = "${Routes.ADD_NOTE}?noteId={noteId}&subjectId={subjectId}&section={section}&chapter={chapter}",
                    arguments = listOf(
                        navArgument("noteId") { type = NavType.StringType; nullable = true; defaultValue = null },
                        navArgument("subjectId") { type = NavType.StringType; nullable = true; defaultValue = null },
                        navArgument("section") { type = NavType.StringType; nullable = true; defaultValue = null },
                        navArgument("chapter") { type = NavType.StringType; nullable = true; defaultValue = null }
                    )
                ) { backStack ->
                    val noteIdArg = backStack.arguments?.getString("noteId")
                    val subIdArg = backStack.arguments?.getString("subjectId")
                    val secArg = backStack.arguments?.getString("section")
                    val chapArg = backStack.arguments?.getString("chapter")
                    val existing = noteIdArg?.let { id -> notes.firstOrNull { it.id == id } }

                    AddEditNoteScreen(
                        existingNote = existing,
                        subjects = subjects,
                        initialSubjectId = subIdArg,
                        initialSectionName = secArg,
                        initialChapterName = chapArg,
                        isSaving = isSaving,
                        aiOrganizerState = aiOrganizerState,
                        fileManager = fileManager,
                        onImportAttachment = { uri, type -> vaultViewModel.importPickedAttachment(uri, type) },
                        onAnalyzeWithAi = { t, d, c, atts -> vaultViewModel.analyzeNoteWithAi(t, d, c, atts) },
                        onClearAiOrganizer = { vaultViewModel.clearAiOrganizerState() },
                        onSaveNote = { exId, t, d, c, sId, sName, sec, chap, nType, tgs, atts, pin, fav, bm, topic ->
                            vaultViewModel.saveNote(
                                existingNoteId = exId,
                                title = t,
                                description = d,
                                content = c,
                                subjectId = sId,
                                subjectName = sName,
                                sectionName = sec,
                                chapterName = chap,
                                noteType = nType,
                                tags = tgs,
                                attachments = atts,
                                pinned = pin,
                                favourite = fav,
                                bookmarked = bm,
                                aiTopic = topic,
                                onSuccess = { savedId ->
                                    navController.navigate(Routes.noteDetail(savedId)) {
                                        popUpTo(Routes.HOME)
                                    }
                                }
                            )
                        },
                        onDeleteNote = { note ->
                            vaultViewModel.deleteNote(note) {
                                navController.navigate(Routes.HOME) {
                                    popUpTo(Routes.HOME) { inclusive = true }
                                }
                            }
                        }
                    )
                }

                composable(Routes.AI_ASSISTANT) {
                    AiAssistantScreen(
                        notes = notes,
                        selectedNote = aiSelectedNote,
                        useOnlySavedNotes = aiOnlySaved,
                        messages = aiMessages,
                        isStreaming = aiStreaming,
                        errorMessage = aiError,
                        generatedFlashcardsState = aiFlashcardsState,
                        generatedQuestionsState = aiQuestionsState,
                        quizState = aiQuizState,
                        savedBanner = aiSavedBanner,
                        onSelectNote = { aiViewModel.selectNote(it) },
                        onToggleOnlySavedNotes = { aiViewModel.setUseOnlySavedNotes(it) },
                        onSendPrompt = { aiViewModel.sendPrompt(it, notes) },
                        onRunQuickAction = { aiViewModel.runQuickAction(it, notes) },
                        onSaveFlashcards = { cards ->
                            val ref = aiSelectedNote
                            aiViewModel.saveGeneratedFlashcardsToVault(
                                cards = cards,
                                subjectName = ref?.subjectName ?: "Science",
                                chapterName = ref?.chapterName ?: "General",
                                noteId = ref?.id
                            )
                        },
                        onSaveQuestions = { qs ->
                            val ref = aiSelectedNote
                            aiViewModel.saveGeneratedQuestionsToVault(
                                questions = qs,
                                subjectName = ref?.subjectName ?: "Science",
                                chapterName = ref?.chapterName ?: "General",
                                noteId = ref?.id
                            )
                        },
                        onOpenRevisionQuiz = {
                            revisionInitialTab = 1
                            navController.navigate(Routes.REVISION_CENTER)
                        },
                        onClearChat = { aiViewModel.clearChat() },
                        onClearBanner = { aiViewModel.clearBanner() }
                    )
                }

                composable(Routes.PROFILE) {
                    ProfileScreen(
                        displayName = user.displayName ?: "CBSE Student",
                        email = user.email,
                        stats = dashboardStats,
                        flashcardsCount = flashcards.size,
                        quizAttemptsCount = quizAttempts.size,
                        themeMode = themeMode,
                        hindiCourse = hindiCourse,
                        onThemeModeChange = { mode -> scope.launch { prefsRepo.setThemeMode(mode) } },
                        onHindiCourseChange = { vaultViewModel.setHindiCourse(it) },
                        onOpenRevisionCenter = {
                            revisionInitialTab = 0
                            navController.navigate(Routes.REVISION_CENTER)
                        }
                    )
                }

                composable(
                    route = Routes.NOTE_DETAIL,
                    arguments = listOf(navArgument("noteId") { type = NavType.StringType })
                ) { backStack ->
                    val noteId = backStack.arguments?.getString("noteId") ?: ""
                    val note: StudyNote? = notes.firstOrNull { it.id == noteId }
                    val accent = subjects.firstOrNull { it.id == note?.subjectId }?.accentColor
                        ?: androidx.compose.material3.MaterialTheme.colorScheme.primary

                    NoteDetailScreen(
                        note = note,
                        accentColor = accent,
                        fileManager = fileManager,
                        onBack = { navController.popBackStack() },
                        onEdit = { n -> navController.navigate(Routes.addNote(noteId = n.id)) },
                        onDelete = { n ->
                            vaultViewModel.deleteNote(n) { navController.popBackStack() }
                        },
                        onTogglePin = { vaultViewModel.toggleNoteFlag(it, togglePin = true) },
                        onToggleFavourite = { vaultViewModel.toggleNoteFlag(it, toggleFav = true) },
                        onToggleBookmark = { vaultViewModel.toggleNoteFlag(it, toggleBookmark = true) },
                        onAskAiAboutNote = { n ->
                            aiViewModel.selectNote(n)
                            navController.navigate(Routes.AI_ASSISTANT) {
                                popUpTo(Routes.HOME) { saveState = true }
                                launchSingleTop = true
                            }
                        }
                    )
                }

                composable(Routes.SMART_SEARCH) {
                    SmartSearchScreen(
                        subjects = subjects,
                        searchState = smartSearchState,
                        onSearch = { vaultViewModel.runSmartSearch(it) },
                        onBack = { navController.popBackStack() },
                        onOpenNote = { noteId -> navController.navigate(Routes.noteDetail(noteId)) },
                        onTogglePin = { vaultViewModel.toggleNoteFlag(it, togglePin = true) },
                        onToggleFavourite = { vaultViewModel.toggleNoteFlag(it, toggleFav = true) },
                        onToggleBookmark = { vaultViewModel.toggleNoteFlag(it, toggleBookmark = true) }
                    )
                }

                composable(Routes.REVISION_CENTER) {
                    RevisionCenterScreen(
                        initialTab = revisionInitialTab,
                        subjects = subjects,
                        notes = notes,
                        flashcards = flashcards,
                        revisionQuestions = revisionQuestions,
                        quizAttempts = quizAttempts,
                        chapterProgress = chapterProgress,
                        quizState = aiQuizState,
                        onBack = { navController.popBackStack() },
                        onAddFlashcard = { sub, chap, q, a -> vaultViewModel.addFlashcard(sub, chap, q, a) },
                        onToggleFlashcardMastered = { vaultViewModel.toggleFlashcardMastered(it) },
                        onDeleteFlashcard = { vaultViewModel.deleteFlashcard(it) },
                        onAddRevisionQuestion = { sub, chap, m, q, a ->
                            vaultViewModel.addRevisionQuestion(sub, chap, m, q, a)
                        },
                        onDeleteRevisionQuestion = { vaultViewModel.deleteRevisionQuestion(it) },
                        onGenerateQuiz = { sub, chap, note ->
                            aiViewModel.generateInteractiveQuiz(sub, chap, note)
                        },
                        onQuizCompleted = { sub, chap, s, t ->
                            vaultViewModel.saveQuizAttempt(sub, chap, s, t)
                        },
                        onOpenNote = { noteId -> navController.navigate(Routes.noteDetail(noteId)) },
                        onTogglePin = { vaultViewModel.toggleNoteFlag(it, togglePin = true) },
                        onToggleFavourite = { vaultViewModel.toggleNoteFlag(it, toggleFav = true) },
                        onToggleBookmark = { vaultViewModel.toggleNoteFlag(it, toggleBookmark = true) },
                        onToggleChapterCompleted = { subId, sec, chap, done ->
                            vaultViewModel.toggleChapterCompleted(subId, sec, chap, done)
                        },
                        onGenerateAiFlashcardsFromAssistant = {
                            navController.navigate(Routes.AI_ASSISTANT) {
                                popUpTo(Routes.HOME) { saveState = true }
                                launchSingleTop = true
                            }
                        }
                    )
                }
            }
        }
    }
}
