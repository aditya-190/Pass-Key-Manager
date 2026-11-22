package com.bhardwaj.passkey.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.bhardwaj.passkey.BuildConfig
import com.bhardwaj.passkey.R
import com.bhardwaj.passkey.about.AppInfo
import com.bhardwaj.passkey.analytics.AnalyticsTracker
import com.bhardwaj.passkey.banner.AnnouncementRepository
import com.bhardwaj.passkey.config.ConfigProvider
import com.bhardwaj.passkey.config.RemoteConfigManager
import com.bhardwaj.passkey.core.Clock
import com.bhardwaj.passkey.crash.CrashReporter
import com.bhardwaj.passkey.legal.LegalSection
import com.bhardwaj.passkey.legal.LegalTexts
import com.bhardwaj.passkey.update.UpdateGateController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope

/** Studio baseline: Remote Config, force update, announcement banner, About and Legal. */
@Module
@InstallIn(SingletonComponent::class)
object BaselineModule {
    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.System

    @Provides
    @Singleton
    fun provideRemoteConfig(
        @ApplicationContext context: Context,
        analytics: AnalyticsTracker,
        crashReporter: CrashReporter,
        clock: Clock,
        @ApplicationScope scope: CoroutineScope
    ): RemoteConfigManager =
        RemoteConfigManager.create(context, analytics, crashReporter, clock, scope, BuildConfig.DEBUG)

    @Provides
    fun provideConfigProvider(remoteConfig: RemoteConfigManager): ConfigProvider = remoteConfig

    @Provides
    @Singleton
    fun provideUpdateGate(
        remoteConfig: RemoteConfigManager,
        dataStore: DataStore<Preferences>,
        analytics: AnalyticsTracker,
        clock: Clock,
        @ApplicationScope scope: CoroutineScope
    ): UpdateGateController =
        UpdateGateController(remoteConfig.config, BuildConfig.VERSION_CODE.toLong(), dataStore, analytics, clock, scope)

    @Provides
    @Singleton
    fun provideAnnouncements(
        remoteConfig: RemoteConfigManager,
        dataStore: DataStore<Preferences>,
        clock: Clock
    ): AnnouncementRepository =
        AnnouncementRepository(remoteConfig.config, dataStore, clock, BuildConfig.VERSION_CODE.toLong())

    @Provides
    @Singleton
    fun provideLegalTexts(): LegalTexts = LegalTexts(
        privacyExtras = listOf(LegalSection(R.string.legal_privacy_backup_heading, R.string.legal_privacy_backup_body)),
        termsExtras = listOf(LegalSection(R.string.legal_terms_safety_heading, R.string.legal_terms_safety_body))
    )

    @Provides
    @Singleton
    fun provideAppInfo(@ApplicationContext context: Context): AppInfo = AppInfo(
        appName = context.getString(R.string.app_name),
        versionName = BuildConfig.VERSION_NAME,
        versionCode = BuildConfig.VERSION_CODE.toLong(),
        packageName = BuildConfig.APPLICATION_ID
    )
}
