package com.workguard.di

import com.workguard.attendance.data.AttendanceRepository
import com.workguard.attendance.data.AttendanceRepositoryImpl
import com.workguard.auth.data.AuthRepository
import com.workguard.auth.data.AuthRepositoryImpl
import com.workguard.core.datastore.AuthDataStore
import com.workguard.core.datastore.FaceSessionStore
import com.workguard.core.datastore.SharedPrefsAuthDataStore
import com.workguard.core.datastore.InMemoryFaceSessionStore
import com.workguard.core.location.AndroidLocationProvider
import com.workguard.core.location.LocationProvider
import com.workguard.core.security.DefaultDeviceInfoProvider
import com.workguard.core.security.DeviceInfoProvider
import com.workguard.core.util.Clock
import com.workguard.core.util.SystemClock
import com.workguard.core.util.DeviceIdProvider
import com.workguard.core.util.AndroidDeviceIdProvider
import com.workguard.face.data.FaceRepository
import com.workguard.face.data.FaceRepositoryImpl
import com.workguard.home.data.HomeRepository
import com.workguard.home.data.HomeRepositoryImpl
import com.workguard.navigation.data.ChatRepository
import com.workguard.navigation.data.ChatRepositoryImpl
import com.workguard.navigation.data.NewsRepository
import com.workguard.navigation.data.NewsRepositoryImpl
import com.workguard.patrol.data.PatrolRepository
import com.workguard.patrol.data.PatrolRepositoryImpl
import com.workguard.profile.data.ProfileRepository
import com.workguard.profile.data.ProfileRepositoryImpl
import com.workguard.task.data.TaskRepository
import com.workguard.task.data.TaskRepositoryImpl
import com.workguard.tracking.data.MonitoringRepository
import com.workguard.tracking.data.MonitoringRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindAttendanceRepository(impl: AttendanceRepositoryImpl): AttendanceRepository

    @Binds
    @Singleton
    abstract fun bindFaceRepository(impl: FaceRepositoryImpl): FaceRepository

    @Binds
    @Singleton
    abstract fun bindTaskRepository(impl: TaskRepositoryImpl): TaskRepository

    @Binds
    @Singleton
    abstract fun bindPatrolRepository(impl: PatrolRepositoryImpl): PatrolRepository

    @Binds
    @Singleton
    abstract fun bindMonitoringRepository(impl: MonitoringRepositoryImpl): MonitoringRepository

    @Binds
    @Singleton
    abstract fun bindHomeRepository(impl: HomeRepositoryImpl): HomeRepository

    @Binds
    @Singleton
    abstract fun bindNewsRepository(impl: NewsRepositoryImpl): NewsRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository

    @Binds
    @Singleton
    abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindAuthDataStore(impl: SharedPrefsAuthDataStore): AuthDataStore

    @Binds
    @Singleton
    abstract fun bindFaceSessionStore(impl: InMemoryFaceSessionStore): FaceSessionStore

    @Binds
    @Singleton
    abstract fun bindDeviceInfoProvider(impl: DefaultDeviceInfoProvider): DeviceInfoProvider

    @Binds
    @Singleton
    abstract fun bindLocationProvider(impl: AndroidLocationProvider): LocationProvider

    @Binds
    @Singleton
    abstract fun bindClock(impl: SystemClock): Clock

    @Binds
    @Singleton
    abstract fun bindDeviceIdProvider(impl: AndroidDeviceIdProvider): DeviceIdProvider
}
