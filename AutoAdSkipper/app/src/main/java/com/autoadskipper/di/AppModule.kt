package com.autoadskipper.di

import android.content.Context
import com.autoadskipper.accessibility.NoOpVisualDetector
import com.autoadskipper.accessibility.SkipButtonVisualDetector
import com.autoadskipper.data.AppListRepository
import com.autoadskipper.data.AppPreferencesRepository
import com.autoadskipper.data.DataStoreManager
import com.autoadskipper.data.StatisticsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDataStoreManager(@ApplicationContext context: Context): DataStoreManager =
        DataStoreManager(context)

    @Provides
    @Singleton
    fun provideStatisticsRepository(@ApplicationContext context: Context): StatisticsRepository =
        StatisticsRepository(context)

    @Provides
    @Singleton
    fun provideAppPreferencesRepository(@ApplicationContext context: Context): AppPreferencesRepository =
        AppPreferencesRepository(context)

    @Provides
    @Singleton
    fun provideAppListRepository(
        @ApplicationContext context: Context,
        appPreferencesRepository: AppPreferencesRepository,
        statisticsRepository: StatisticsRepository
    ): AppListRepository = AppListRepository(context, appPreferencesRepository, statisticsRepository)

    @Provides
    @Singleton
    fun provideSkipButtonVisualDetector(): SkipButtonVisualDetector = NoOpVisualDetector()
}
