package com.zatiki.memocards

import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.zatiki.memocards.domain.ThemeName
import com.zatiki.memocards.domain.UiSettings
import com.zatiki.memocards.navigation.Routes
import com.zatiki.memocards.ui.components.MainTab
import com.zatiki.memocards.ui.components.MemoBottomBar
import com.zatiki.memocards.ui.screens.BookReaderScreen
import com.zatiki.memocards.ui.screens.DeckDetailScreen
import com.zatiki.memocards.ui.screens.DeckListScreen
import com.zatiki.memocards.ui.screens.NoteEditorScreen
import com.zatiki.memocards.ui.screens.ReviewScreen
import com.zatiki.memocards.ui.screens.SearchScreen
import com.zatiki.memocards.ui.screens.SettingsScreen
import com.zatiki.memocards.ui.screens.StatsScreen
import com.zatiki.memocards.ui.theme.MemoCardsTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        val repo = (application as MemoCardsApp).repository

        setContent {
            var settings by remember { mutableStateOf(UiSettings()) }
            var ready by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()
            val nav = rememberNavController()
            val backStack by nav.currentBackStackEntryAsState()
            val currentRoute = backStack?.destination?.route
            val showBottomBar = currentRoute in Routes.tabRoutes

            LaunchedEffect(Unit) {
                settings = repo.getUiSettings()
                ready = true
            }

            if (!ready) return@setContent

            MemoCardsTheme(settings = settings) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (showBottomBar) {
                            MemoBottomBar(
                                currentRoute = currentRoute,
                                onTab = { tab ->
                                    nav.navigate(tab.route) {
                                        popUpTo(nav.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                onContinueStudy = {
                                    scope.launch {
                                        val lastDeckId = repo.getLastReviewedDeckId()
                                        if (lastDeckId != null) {
                                            nav.navigate(Routes.DeckDetail.create(lastDeckId))
                                        } else {
                                            nav.navigate(Routes.DeckList.route) {
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    }
                                },
                            )
                        }
                    },
                ) { padding ->
                    NavHost(
                        navController = nav,
                        startDestination = Routes.DeckList.route,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                    ) {
                        composable(Routes.DeckList.route) {
                            DeckListScreen(
                                repo = repo,
                                settings = settings,
                                onToggleTheme = {
                                    scope.launch {
                                        val next =
                                            if (settings.theme.isDark) ThemeName.LIGHT
                                            else ThemeName.EMICH
                                        settings = repo.saveUiSettings(settings.copy(theme = next))
                                    }
                                },
                                onOpenDeck = { deck ->
                                    nav.navigate(Routes.DeckDetail.create(deck.id))
                                },
                                onOpenBook = { bookId ->
                                    nav.navigate(Routes.BookReader.create(bookId))
                                },
                            )
                        }

                        composable(Routes.Stats.route) {
                            StatsScreen(repo = repo)
                        }

                        composable(Routes.Search.route) {
                            SearchScreen(
                                repo = repo,
                                onOpenDeck = { deck ->
                                    nav.navigate(Routes.DeckDetail.create(deck.id))
                                },
                                onOpenNoteDeck = { deckId ->
                                    nav.navigate(Routes.DeckDetail.create(deckId))
                                },
                            )
                        }

                        composable(
                            route = Routes.DeckDetail.route,
                            arguments = listOf(navArgument("deckId") { type = NavType.LongType }),
                        ) { entry ->
                            val deckId = entry.arguments?.getLong("deckId") ?: return@composable
                            var deckName by remember { mutableStateOf("Mazo") }
                            LaunchedEffect(deckId) {
                                deckName = repo.getDeck(deckId)?.name ?: "Mazo"
                            }
                            DeckDetailScreen(
                                repo = repo,
                                deckId = deckId,
                                deckName = deckName,
                                onReview = { queueFilter ->
                                    nav.navigate(Routes.Review.create(deckId, queueFilter = queueFilter))
                                },
                                onPreviewReview = { days ->
                                    nav.navigate(Routes.Review.create(deckId, days))
                                },
                                onAddNote = { nav.navigate(Routes.NoteEditor.create(deckId)) },
                            )
                        }

                        composable(
                            route = Routes.NoteEditor.route,
                            arguments = listOf(navArgument("deckId") { type = NavType.LongType }),
                        ) { entry ->
                            val deckId = entry.arguments?.getLong("deckId") ?: return@composable
                            NoteEditorScreen(
                                repo = repo,
                                deckId = deckId,
                                onBack = { nav.popBackStack() },
                                onSaved = { nav.popBackStack() },
                            )
                        }

                        composable(
                            route = Routes.Review.route,
                            arguments = listOf(
                                navArgument("deckId") { type = NavType.LongType },
                                navArgument("advanceDays") {
                                    type = NavType.IntType
                                    defaultValue = 0
                                },
                                navArgument("queueFilter") {
                                    type = NavType.StringType
                                    defaultValue = "all"
                                },
                            ),
                        ) { entry ->
                            val deckId = entry.arguments?.getLong("deckId") ?: return@composable
                            val advanceDays = entry.arguments?.getInt("advanceDays") ?: 0
                            val queueFilter = entry.arguments?.getString("queueFilter") ?: "all"
                            var deckName by remember { mutableStateOf("Mazo") }
                            LaunchedEffect(deckId) {
                                deckName = repo.getDeck(deckId)?.name ?: "Mazo"
                            }
                            ReviewScreen(
                                repo = repo,
                                deckId = deckId,
                                deckName = deckName,
                                settings = settings,
                                advanceDays = advanceDays,
                                queueFilter = queueFilter,
                                onDone = { nav.popBackStack() },
                            )
                        }

                        composable(
                            route = Routes.BookReader.route,
                            arguments = listOf(navArgument("bookId") { type = NavType.LongType }),
                        ) { entry ->
                            val bookId = entry.arguments?.getLong("bookId") ?: return@composable
                            BookReaderScreen(
                                repo = repo,
                                bookId = bookId,
                                settings = settings,
                                onBack = { nav.popBackStack() },
                            )
                        }

                        composable(Routes.Settings.route) {
                            SettingsScreen(
                                repo = repo,
                                settings = settings,
                                showBack = false,
                                onBack = { nav.navigate(MainTab.Home.route) },
                                onThemeChange = { theme ->
                                    scope.launch {
                                        settings = repo.saveUiSettings(settings.copy(theme = theme))
                                    }
                                },
                                onFontScaleChange = { scale ->
                                    scope.launch {
                                        val clamped =
                                            ((scale * 100).toInt() / 100f).coerceIn(0.9f, 1.4f)
                                        settings =
                                            repo.saveUiSettings(settings.copy(fontScale = clamped))
                                    }
                                },
                                onLineHeightChange = { height ->
                                    scope.launch {
                                        val clamped =
                                            ((height * 100).toInt() / 100f).coerceIn(1.15f, 2.0f)
                                        settings =
                                            repo.saveUiSettings(settings.copy(lineHeight = clamped))
                                    }
                                },
                                onRatingLayoutChange = { layout ->
                                    scope.launch {
                                        settings =
                                            repo.saveUiSettings(settings.copy(ratingLayout = layout))
                                    }
                                },
                                onArcLabelModeChange = { mode ->
                                    scope.launch {
                                        settings =
                                            repo.saveUiSettings(settings.copy(arcLabelMode = mode))
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
