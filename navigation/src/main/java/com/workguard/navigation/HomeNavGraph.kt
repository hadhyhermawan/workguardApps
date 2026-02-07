package com.workguard.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.hilt.navigation.compose.hiltViewModel
import com.workguard.home.HomeScreen
import com.workguard.home.HomeViewModel
import com.workguard.face.FaceEnrollmentCaptureScreen
import com.workguard.face.FaceEnrollmentViewModel
import com.workguard.attendance.AttendanceViewModel
import com.workguard.attendance.AttendanceEvent
import com.workguard.core.model.enums.FaceContext
import com.workguard.face.FaceScanScreen
import com.workguard.face.FaceScanViewModel
import com.workguard.profile.ProfileScreen
import com.workguard.profile.ProfileViewModel

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun HomeNavGraph(
    onAttendanceClick: () -> Unit,
    onTaskClick: () -> Unit,
    onLogout: () -> Unit,
    initialRoute: String? = null,
    onRouteConsumed: (() -> Unit)? = null
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isChatRoute = currentRoute == Routes.Chat || currentRoute == Routes.ChatThread
    val showBottomBar = currentRoute == Routes.Home || currentRoute == Routes.Scan
    val showTopBar = currentRoute != null &&
        currentRoute != Routes.Home &&
        currentRoute != Routes.Scan &&
        currentRoute != Routes.FaceScan
    val topBarTitle = remember(currentRoute) {
        when (currentRoute) {
            Routes.Chat, Routes.ChatThread -> "Patrol Team"
            Routes.Scan -> "Scan"
            Routes.News -> "News"
            Routes.NewsDetail -> "News"
            Routes.Settings -> "Settings"
            Routes.FaceEnroll -> "Pendaftaran Wajah"
            Routes.FaceEnrollCapture -> "Pendaftaran Wajah"
            Routes.Profile -> "Profil"
            else -> ""
        }
    }
    val topBarSubtitle = remember(currentRoute) {
        if (isChatRoute) "32 anggota | 6 online" else null
    }
    val showChatActions = isChatRoute

    LaunchedEffect(initialRoute) {
        if (!initialRoute.isNullOrBlank()) {
            navController.navigate(initialRoute) { launchSingleTop = true }
            onRouteConsumed?.invoke()
        }
    }

    Scaffold(
        containerColor = Color(0xFFF4F7F8),
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = topBarTitle,
                                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                                color = UiTokens.Text
                            )
                            if (topBarSubtitle != null) {
                                Text(
                                    text = topBarSubtitle,
                                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                                    color = UiTokens.Muted
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.Outlined.ArrowBack,
                                contentDescription = "Kembali",
                                tint = UiTokens.Text
                            )
                        }
                    },
                    actions = {
                        if (showChatActions) {
                            IconButton(onClick = {}) {
                                Icon(
                                    imageVector = Icons.Outlined.Call,
                                    contentDescription = "Telepon",
                                    tint = UiTokens.Text
                                )
                            }
                            IconButton(onClick = {}) {
                                Icon(
                                    imageVector = Icons.Outlined.Videocam,
                                    contentDescription = "Telepon video",
                                    tint = UiTokens.Text
                                )
                            }
                            IconButton(onClick = {}) {
                                Icon(
                                    imageVector = Icons.Outlined.MoreVert,
                                    contentDescription = "Menu",
                                    tint = UiTokens.Text
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = UiTokens.Surface,
                        titleContentColor = UiTokens.Text
                    )
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                BottomNav(navController)
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.Scan,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.Home) {
                val viewModel: HomeViewModel = hiltViewModel()
                val state by viewModel.state.collectAsState()

                HomeScreen(
                    state = state,
                    onAttendanceClick = onAttendanceClick,
                    onTaskClick = onTaskClick,
                    onRefresh = { viewModel.loadHome() },
                    onLocationPermissionResult = viewModel::onLocationPermissionResult
                )
            }

            val chatContent: @Composable () -> Unit = {
                val viewModel: ChatViewModel = hiltViewModel()
                val state by viewModel.state.collectAsState()

                ChatScreen(
                    state = state,
                    onInputChange = viewModel::onInputChange,
                    onSendClick = viewModel::onSendClick
                )
            }

            composable(Routes.Chat) {
                chatContent()
            }

            composable(
                route = Routes.ChatThread,
                arguments = listOf(navArgument(Routes.ChatThreadArg) { type = NavType.StringType })
            ) {
                chatContent()
            }

            composable(Routes.Scan) { backStackEntry ->
                val viewModel: AttendanceViewModel = hiltViewModel()
                val state by viewModel.state.collectAsState()
                val faceResult by backStackEntry.savedStateHandle
                    .getStateFlow(Routes.FaceResultKey, "")
                    .collectAsState()

                LaunchedEffect(faceResult) {
                    if (faceResult.isNotBlank()) {
                        viewModel.onFaceScanCompleted()
                        backStackEntry.savedStateHandle.set(Routes.FaceResultKey, "")
                    }
                }

                LaunchedEffect(Unit) {
                    viewModel.loadTodayStatus()
                    viewModel.events.collect { event ->
                        if (event is AttendanceEvent.RequireFaceScan) {
                            navController.navigate(Routes.faceScan(event.context))
                        }
                    }
                }

                ScanScreen(
                    state = state,
                    onCheckInClick = viewModel::onCheckInClicked,
                    onCheckOutClick = viewModel::onCheckOutClicked,
                    onReload = viewModel::loadTodayStatus
                )
            }

            composable(Routes.News) {
                val viewModel: NewsViewModel = hiltViewModel()
                val state by viewModel.state.collectAsState()

                NewsScreen(
                    state = state,
                    onQueryChange = viewModel::onQueryChange,
                    onCategorySelected = viewModel::onCategorySelected,
                    onRetry = viewModel::refresh,
                    onItemClick = { item ->
                        if (item.id > 0) {
                            navController.navigate(Routes.newsDetail(item.id))
                        }
                    },
                    onFeaturedClick = { item ->
                        if (item.id > 0) {
                            navController.navigate(Routes.newsDetail(item.id))
                        }
                    }
                )
            }

            composable(Routes.NewsDetail) {
                val viewModel: NewsDetailViewModel = hiltViewModel()
                val state by viewModel.state.collectAsState()

                NewsDetailScreen(
                    state = state,
                    onRetry = viewModel::retry
                )
            }

            composable(Routes.Settings) {
                SettingsScreen(
                    onLogout = onLogout,
                    onFaceEnrollmentClick = { navController.navigate(Routes.FaceEnroll) },
                    onProfileClick = { navController.navigate(Routes.Profile) }
                )
            }

            composable(Routes.Profile) {
                val viewModel: ProfileViewModel = hiltViewModel()
                val state by viewModel.state.collectAsState()

                ProfileScreen(
                    state = state,
                    onRetry = viewModel::loadProfile
                )
            }

            composable(Routes.FaceEnroll) {
                val viewModel: FaceEnrollmentViewModel = hiltViewModel()
                val state by viewModel.state.collectAsState()

                FaceEnrollmentScreen(
                    state = state,
                    onStartEnrollment = { navController.navigate(Routes.FaceEnrollCapture) }
                )
            }

            composable(Routes.FaceEnrollCapture) {
                val parentEntry = remember(navController) {
                    navController.getBackStackEntry(Routes.FaceEnroll)
                }
                val viewModel: FaceEnrollmentViewModel = hiltViewModel(parentEntry)
                val state by viewModel.state.collectAsState()

                FaceEnrollmentCaptureScreen(
                    state = state,
                    onPhotoCaptured = viewModel::onPhotoCaptured,
                    onClearError = viewModel::clearError,
                    onCompleted = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.FaceScan,
                arguments = listOf(navArgument("context") { type = NavType.StringType })
            ) { backStackEntry ->
                val viewModel: FaceScanViewModel = hiltViewModel()
                val state by viewModel.state.collectAsState()
                val contextValue = backStackEntry.arguments?.getString("context")
                val context = FaceContext.values().firstOrNull { it.name == contextValue }
                    ?: FaceContext.ATTENDANCE

                LaunchedEffect(context) {
                    viewModel.setContext(context)
                }

                LaunchedEffect(state.isCompleted) {
                    if (state.isCompleted) {
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set(Routes.FaceResultKey, context.name)
                        navController.popBackStack()
                    }
                }

                FaceScanScreen(
                    state = state,
                    onConfirmScan = viewModel::onConfirmScan
                )
            }
        }
    }
}
