package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.core.PreferencesManager
import com.example.core.ThemeModePreference
import com.example.core.configureAppCheck
import com.example.data.ai.NovaraAiService
import com.example.data.repository.NovaraRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOutUser
import com.example.ui.screens.CreatePostScreen
import com.example.ui.screens.DiscoverScreen
import com.example.ui.screens.HomeFeedScreen
import com.example.ui.screens.MessagesScreen
import com.example.ui.screens.ProfileCreatorScreen
import com.example.ui.theme.NovaraTheme
import com.example.ui.viewmodel.NovaraViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

private enum class NovaraTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    FEED("Feed", Icons.Rounded.Home, Icons.Outlined.Home),
    DISCOVER("Discover", Icons.Rounded.Explore, Icons.Outlined.Explore),
    CREATE("Create", Icons.Rounded.AddCircle, Icons.Outlined.AddCircleOutline),
    CONNECT("Connect", Icons.Rounded.ChatBubble, Icons.Outlined.ChatBubbleOutline),
    HUB("Hub", Icons.Rounded.Person, Icons.Outlined.Person)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        val prefsManager = PreferencesManager(applicationContext)

        setContent {
            val localPrefs by prefsManager.preferencesFlow.collectAsStateWithLifecycle(
                initialValue = com.example.core.UserLocalPreferences()
            )
            val isDark = when (localPrefs.themeMode) {
                ThemeModePreference.SYSTEM -> isSystemInDarkTheme()
                ThemeModePreference.DARK -> true
                ThemeModePreference.LIGHT -> false
            }

            NovaraTheme(darkTheme = isDark) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(prefsManager = prefsManager)
                }
            }
        }
    }
}

@Composable
fun AppNavigation(
    prefsManager: PreferencesManager,
    auth: FirebaseAuth = Firebase.auth
) {
    var currentUser by remember { mutableStateOf(auth.currentUser) }
    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = { currentUser = auth.currentUser })
    } else {
        key(user.uid) {
            NovaraAuthenticatedShell(user = user, prefsManager = prefsManager)
        }
    }
}

@Composable
private fun NovaraAuthenticatedShell(
    user: FirebaseUser,
    prefsManager: PreferencesManager
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val isAdmin = user.isEmailVerified && user.email == "tejashpatel02010@gmail.com"

    val viewModel: NovaraViewModel = viewModel(
        key = user.uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                NovaraViewModel(
                    repository = NovaraRepository(db),
                    aiService = NovaraAiService(),
                    preferencesManager = prefsManager,
                    currentUserId = user.uid,
                    currentUserEmail = user.email,
                    isAdminUser = isAdmin
                )
            }
        }
    )

    LaunchedEffect(user.uid) {
        viewModel.ensureProvisioned(user)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    var currentTab by remember { mutableStateOf(NovaraTab.FEED) }

    if (currentTab != NovaraTab.FEED) {
        BackHandler { currentTab = NovaraTab.FEED }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar(modifier = Modifier.navigationBarsPadding()) {
                        NovaraTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("nav_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail {
                        NovaraTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("rail_${tab.name.lowercase()}")
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(modifier = Modifier.fillMaxSize().widthIn(max = 720.dp)) {
                        when (currentTab) {
                            NovaraTab.FEED -> HomeFeedScreen(
                                viewModel = viewModel,
                                onNavigateToCreate = { currentTab = NovaraTab.CREATE }
                            )
                            NovaraTab.DISCOVER -> DiscoverScreen(
                                viewModel = viewModel,
                                onOpenMessagesTab = { currentTab = NovaraTab.CONNECT }
                            )
                            NovaraTab.CREATE -> CreatePostScreen(
                                viewModel = viewModel,
                                onPostPublished = { currentTab = NovaraTab.FEED }
                            )
                            NovaraTab.CONNECT -> MessagesScreen(viewModel = viewModel)
                            NovaraTab.HUB -> ProfileCreatorScreen(
                                viewModel = viewModel,
                                onSignOut = {
                                    signOutUser(credentialManager, scope)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
