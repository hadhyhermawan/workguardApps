package com.workguard.di

import com.workguard.core.security.FaceSessionConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {
    @Provides
    @Singleton
    fun provideFaceSessionConfig(): FaceSessionConfig {
        val ttlMillis = 2 * 60 * 1000L
        return FaceSessionConfig(
            attendanceTtlMillis = ttlMillis,
            taskTtlMillis = ttlMillis
        )
    }
}
