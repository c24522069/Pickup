package com.example.pickup.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.pickup.R
import com.example.pickup.data.repository.PracticeRepository
import com.example.pickup.data.repository.TuningRepository
import com.example.pickup.screens.AddEditSessionScreen
import com.example.pickup.screens.MetronomeScreen
import com.example.pickup.screens.PracticeLogScreen
import com.example.pickup.screens.TunerScreen
import com.example.pickup.screens.TuningDetailScreen
import com.example.pickup.screens.TuningLibraryScreen
import com.example.pickup.viewmodel.PracticeLogViewModel
import com.example.pickup.viewmodel.TuningViewModel
import kotlinx.serialization.Serializable

// Routes. Each class is one destination; its properties are the arguments.
// Type-safe Navigation Compose is not in the labs (Week 4 uses an enum + when).
// Reference: https://developer.android.com/guide/navigation/use-graph/navigate
// Reference: https://developer.android.com/develop/ui/compose/navigation
// Reference: GenAi see prompt(s) (2) in report
@Serializable object TuningsGraph
@Serializable object TuningList
@Serializable data class TuningDetail(val tuningId: Int)
@Serializable object Tuner
@Serializable object Metronome
@Serializable object PracticeLog
@Serializable data class AddEditSession(val sessionId: Int? = null, val tuningId: Int? = null)

// Bottom bar destinations: enum + NavigationSuiteScaffold as in NavApp (Week 4)
enum class TopLevelDestination(@param:StringRes val label: Int, val route: Any) {
    TUNINGS(R.string.nav_tunings, TuningsGraph),
    TUNER(R.string.nav_tuner, Tuner),
    METRONOME(R.string.nav_metronome, Metronome),
    LOG(R.string.nav_log, PracticeLog)
}

@Composable
private fun TopLevelDestination.icon(): Painter = when (this) {
    TopLevelDestination.TUNINGS -> rememberVectorPainter(Icons.AutoMirrored.Filled.List)
    TopLevelDestination.TUNER -> painterResource(R.drawable.ic_tuner) // vector asset
    TopLevelDestination.METRONOME -> rememberVectorPainter(Icons.Filled.Timer)
    TopLevelDestination.LOG -> rememberVectorPainter(Icons.Filled.DateRange)
}

// Switch tab the standard way: clear back to the start, keep each tab's state.
private fun NavHostController.navigateToTab(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun AppNavHost(
    tuningRepository: TuningRepository,
    practiceRepository: PracticeRepository
) {
    val navController = rememberNavController()
    // Activity-scoped ViewModels, created with Factories as in Lab 8
    val tuningViewModel: TuningViewModel =
        viewModel(factory = TuningViewModel.Factory(tuningRepository, practiceRepository))
    val practiceViewModel: PracticeLogViewModel =
        viewModel(factory = PracticeLogViewModel.Factory(practiceRepository, tuningRepository))

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val tuningError by tuningViewModel.error.collectAsStateWithLifecycle()
    val practiceError by practiceViewModel.error.collectAsStateWithLifecycle()

    // Show database errors in a snackbar, as in Lab 8's NotesApp
    LaunchedEffect(tuningError, practiceError) {
        (tuningError ?: practiceError)?.let {
            snackbarHostState.showSnackbar(context.getString(it))
            tuningViewModel.clearError()
            practiceViewModel.clearError()
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val onForm = currentDestination?.hasRoute(AddEditSession::class) == true

    NavigationSuiteScaffold(
        // The form is full screen, like the design, so the bar is hidden there
        layoutType = if (onForm) NavigationSuiteType.None
        else NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(currentWindowAdaptiveInfo()),
        navigationSuiteItems = {
            TopLevelDestination.entries.forEach { destination ->
                item(
                    icon = { Icon(destination.icon(), contentDescription = null) },
                    label = { Text(stringResource(destination.label)) },
                    // hierarchy also matches screens inside a tab, e.g. TuningDetail
                    selected = currentDestination?.hierarchy?.any {
                        it.hasRoute(destination.route::class)
                    } == true,
                    onClick = { navController.navigateToTab(destination) }
                )
            }
        }
    ) {
        Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = TuningsGraph,
                modifier = Modifier.fillMaxSize().padding(innerPadding)
            ) {
                // The Tunings tab has two screens, so they share a nested graph
                navigation<TuningsGraph>(startDestination = TuningList) {
                    composable<TuningList> {
                        TuningLibraryScreen(
                            viewModel = tuningViewModel,
                            onOpenTuning = { navController.navigate(TuningDetail(it)) }
                        )
                    }
                    composable<TuningDetail> { entry ->
                        val route = entry.toRoute<TuningDetail>()
                        TuningDetailScreen(
                            tuningId = route.tuningId,
                            viewModel = tuningViewModel,
                            onBack = { navController.popBackStack() },
                            onTune = { id ->
                                tuningViewModel.selectTunerTuning(id)
                                navController.navigateToTab(TopLevelDestination.TUNER)
                            },
                            onLogPractice = { id -> navController.navigate(AddEditSession(tuningId = id)) }
                        )
                    }
                }
                composable<Tuner> { TunerScreen(tuningViewModel) }
                composable<Metronome> { MetronomeScreen() }
                composable<PracticeLog> {
                    PracticeLogScreen(
                        viewModel = practiceViewModel,
                        snackbarHostState = snackbarHostState,
                        onAddSession = { navController.navigate(AddEditSession()) },
                        onEditSession = { id -> navController.navigate(AddEditSession(sessionId = id)) }
                    )
                }
                composable<AddEditSession> { entry ->
                    val route = entry.toRoute<AddEditSession>()
                    AddEditSessionScreen(
                        sessionId = route.sessionId,
                        initialTuningId = route.tuningId,
                        tuningViewModel = tuningViewModel,
                        practiceViewModel = practiceViewModel,
                        onClose = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
