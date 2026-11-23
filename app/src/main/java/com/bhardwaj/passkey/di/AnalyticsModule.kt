package com.bhardwaj.passkey.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.bhardwaj.passkey.analytics.AnalyticsTracker
import com.bhardwaj.passkey.analytics.ConsentApplier
import com.bhardwaj.passkey.analytics.FirebaseAnalyticsTracker
import com.bhardwaj.passkey.analytics.FirebaseConsentMode
import com.bhardwaj.passkey.analytics.FirebasePerformanceSink
import com.bhardwaj.passkey.consent.ConsentStore
import com.bhardwaj.passkey.consent.DataStoreConsentStore
import com.bhardwaj.passkey.crash.CrashReporter
import com.bhardwaj.passkey.crash.FirebaseCrashReporter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

/** CPU-bound work kept off the main thread (password analysis); a test passes its own dispatcher. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class WorkDispatcher

@Module
@InstallIn(SingletonComponent::class)
object AnalyticsModule {
    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    @WorkDispatcher
    fun provideWorkDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Provides
    @Singleton
    fun provideCrashReporter(@ApplicationContext context: Context): CrashReporter =
        FirebaseCrashReporter.create(context)

    @Provides
    @Singleton
    fun provideAnalyticsTracker(@ApplicationContext context: Context, crashReporter: CrashReporter): AnalyticsTracker =
        FirebaseAnalyticsTracker.create(context, crashReporter)

    @Provides
    @Singleton
    fun provideConsentStore(dataStore: DataStore<Preferences>): ConsentStore = DataStoreConsentStore(dataStore)

    @Provides
    @Singleton
    fun provideConsentApplier(
        @ApplicationContext context: Context,
        store: ConsentStore,
        analytics: AnalyticsTracker,
        crashReporter: CrashReporter,
        @ApplicationScope scope: CoroutineScope
    ): ConsentApplier = ConsentApplier(
        consent = store.consent,
        analytics = analytics,
        crashReporter = crashReporter,
        consentMode = FirebaseConsentMode.create(context),
        performance = FirebasePerformanceSink.create(context),
        scope = scope
    )
}
