package com.workguard.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.workguard.auth.AuthEvent
import com.workguard.auth.AuthScreen
import com.workguard.auth.AuthSessionViewModel
import com.workguard.auth.AuthViewModel
import com.workguard.core.model.enums.CameraFacing
import com.workguard.core.model.enums.FaceContext
import com.workguard.core.security.GpsReading
import com.workguard.face.FaceScanScreen
import com.workguard.face.FaceScanViewModel
import com.workguard.patrol.PatrolScreen
import com.workguard.payroll.PayrollDetailScreen
import com.workguard.payroll.PayrollScreen
import com.workguard.task.TaskCameraScreen
import com.workguard.task.TaskCompleteScreen
import com.workguard.task.TaskEvent
import com.workguard.task.TaskStartScreen
import com.workguard.task.TaskStep
import com.workguard.task.TaskViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.navArgument
import kotlinx.coroutines.flow.collect

@Composable
fun AppNavGraph(
    startDestination: String = Routes.Auth,
    initialHomeRoute: String? = null,
    onHomeRouteConsumed: (() -> Unit)? = null
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.Auth) {
            val viewModel: AuthViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()

            LaunchedEffect(Unit) {
                viewModel.events.collect { event ->
                    if (event is AuthEvent.LoggedIn) {
                        navController.navigate(Routes.HomeRoot) {
                            popUpTo(Routes.Auth) { inclusive = true }
                        }
                    }
                }
            }

            AuthScreen(
                state = state,
                onCompanyCodeChange = viewModel::onCompanyCodeChange,
                onEmployeeCodeChange = viewModel::onEmployeeCodeChange,
                onPasswordChange = viewModel::onPasswordChange,
                onLoginClick = viewModel::onLoginClicked
            )
        }

        composable(Routes.HomeRoot) {
            val context = LocalContext.current
            val sessionViewModel: AuthSessionViewModel = hiltViewModel()
            HomeNavGraph(
                onTaskClick = { navController.navigate(Routes.Patrol) },
                onLogout = {
                    sessionViewModel.logout()
                    stopTrackingService(context)
                    navController.navigate(Routes.Auth) {
                        popUpTo(Routes.HomeRoot) { inclusive = true }
                    }
                },
                initialRoute = initialHomeRoute,
                onRouteConsumed = onHomeRouteConsumed
            )
        }

        composable(Routes.TaskStart) { backStackEntry ->
            val taskEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Routes.TaskStart)
            }
            val viewModel: TaskViewModel = hiltViewModel(taskEntry)
            val state by viewModel.state.collectAsState()
            val faceResult by backStackEntry.savedStateHandle
                .getStateFlow(Routes.FaceResultKey, "")
                .collectAsState()

            LaunchedEffect(faceResult) {
                if (faceResult.isNotBlank()) {
                    viewModel.onFaceScanCompleted()
                    backStackEntry.savedStateHandle[Routes.FaceResultKey] = ""
                }
            }

            LaunchedEffect(Unit) {
                viewModel.events.collect { event ->
                    if (event is TaskEvent.RequireFaceScan) {
                        navController.navigate(Routes.faceScan(event.context))
                    }
                }
            }

            LaunchedEffect(state.currentStep) {
                if (state.currentStep == TaskStep.CAMERA) {
                    navController.navigate(Routes.TaskCamera)
                }
            }

            val gpsReading = remember { GpsReading(accuracyMeters = 10f, isMocked = false) }

            TaskStartScreen(
                state = state,
                onStartClick = { viewModel.onStartTaskRequested(gpsReading) }
            )
        }

        composable(Routes.TaskCamera) { backStackEntry ->
            val taskEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Routes.TaskStart)
            }
            val viewModel: TaskViewModel = hiltViewModel(taskEntry)
            val state by viewModel.state.collectAsState()
            val faceResult by backStackEntry.savedStateHandle
                .getStateFlow(Routes.FaceResultKey, "")
                .collectAsState()

            LaunchedEffect(faceResult) {
                if (faceResult.isNotBlank()) {
                    viewModel.onFaceScanCompleted()
                    backStackEntry.savedStateHandle[Routes.FaceResultKey] = ""
                }
            }

            LaunchedEffect(Unit) {
                viewModel.events.collect { event ->
                    if (event is TaskEvent.RequireFaceScan) {
                        navController.navigate(Routes.faceScan(event.context))
                    }
                }
            }

            LaunchedEffect(state.currentStep) {
                if (state.currentStep == TaskStep.COMPLETE) {
                    navController.navigate(Routes.TaskComplete)
                }
            }

            val gpsReading = remember { GpsReading(accuracyMeters = 10f, isMocked = false) }

            TaskCameraScreen(
                state = state,
                onCaptureClick = { viewModel.onUploadMediaRequested(cameraFacing = CameraFacing.BACK, gpsReading = gpsReading) }
            )
        }

        composable(Routes.TaskComplete) { backStackEntry ->
            val taskEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Routes.TaskStart)
            }
            val viewModel: TaskViewModel = hiltViewModel(taskEntry)
            val state by viewModel.state.collectAsState()
            val faceResult by backStackEntry.savedStateHandle
                .getStateFlow(Routes.FaceResultKey, "")
                .collectAsState()

            LaunchedEffect(faceResult) {
                if (faceResult.isNotBlank()) {
                    viewModel.onFaceScanCompleted()
                    backStackEntry.savedStateHandle[Routes.FaceResultKey] = ""
                }
            }

            LaunchedEffect(Unit) {
                viewModel.events.collect { event ->
                    if (event is TaskEvent.RequireFaceScan) {
                        navController.navigate(Routes.faceScan(event.context))
                    }
                }
            }

            LaunchedEffect(state.currentStep) {
                if (state.currentStep == TaskStep.DONE) {
                    navController.navigate(Routes.HomeRoot) {
                        popUpTo(Routes.HomeRoot) { inclusive = true }
                    }
                }
            }

            val gpsReading = remember { GpsReading(accuracyMeters = 10f, isMocked = false) }

            TaskCompleteScreen(
                state = state,
                onCompleteClick = { viewModel.onCompleteTaskRequested(gpsReading) }
            )
        }

        composable(
            route = Routes.FaceScan,
            arguments = listOf(navArgument("context") { type = NavType.StringType })
        ) { backStackEntry ->
            val viewModel: FaceScanViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()
            val contextValue = backStackEntry.arguments?.getString("context")
            val context = FaceContext.values().firstOrNull { it.name == contextValue } ?: FaceContext.ATTENDANCE

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

        composable(Routes.Patrol) { backStackEntry ->
            val viewModel: com.workguard.patrol.PatrolViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()
            val faceResult by backStackEntry.savedStateHandle
                .getStateFlow(Routes.FaceResultKey, "")
                .collectAsState()

            LaunchedEffect(faceResult) {
                if (faceResult.isNotBlank()) {
                    viewModel.onFaceScanCompleted()
                    backStackEntry.savedStateHandle[Routes.FaceResultKey] = ""
                }
            }

            LaunchedEffect(Unit) {
                viewModel.events.collect { event ->
                    if (event is com.workguard.patrol.PatrolEvent.RequireFaceScan) {
                        navController.navigate(Routes.faceScan(event.context))
                    }
                }
            }

            PatrolScreen(
                state = state,
                onStartPatrol = viewModel::onStartPatrolRequested,
                onPointSelected = viewModel::onPointSelected,
                onPhotoCaptured = viewModel::onPhotoCaptured,
                onCancelCapture = viewModel::onCancelCapture,
                onClearError = viewModel::clearError
            )
        }

        composable(Routes.Payroll) {
            PayrollScreen()
        }

        composable(Routes.PayrollDetail) {
            PayrollDetailScreen()
        }

    }
}

private fun stopTrackingService(context: android.content.Context) {
    val intent = android.content.Intent()
        .setClassName(context, "com.workguard.tracking.TrackingService")
    context.stopService(intent)
}
