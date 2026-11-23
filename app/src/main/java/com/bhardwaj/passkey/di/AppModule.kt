package com.bhardwaj.passkey.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.bhardwaj.passkey.data.datastore.BackupReminderStoreImpl
import com.bhardwaj.passkey.data.datastore.PreferencesRepositoryImpl
import com.bhardwaj.passkey.data.datastore.passkeyDataStore
import com.bhardwaj.passkey.data.local.VaultDatabaseProvider
import com.bhardwaj.passkey.data.repository.AutofillTrustStoreImpl
import com.bhardwaj.passkey.data.repository.PasskeyRepositoryImpl
import com.bhardwaj.passkey.data.security.DefaultKeyDerivation
import com.bhardwaj.passkey.data.security.KeyDerivation
import com.bhardwaj.passkey.domain.repository.AutofillTrustStore
import com.bhardwaj.passkey.domain.repository.BackupReminderStore
import com.bhardwaj.passkey.domain.repository.PasskeyRepository
import com.bhardwaj.passkey.domain.repository.PreferencesRepository
import com.bhardwaj.passkey.utils.ResourceSecretKeywords
import com.bhardwaj.passkey.utils.SecretKeywords
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    // PasskeyDatabase is deliberately NOT provided here any more. It can only be built once a data
    // encryption key exists, which requires the user to have authenticated, so it is owned by
    // VaultDatabaseProvider and opened on unlock.

    @Provides
    @Singleton
    fun providePasskeyRepository(vault: VaultDatabaseProvider): PasskeyRepository = PasskeyRepositoryImpl(vault)

    @Provides
    @Singleton
    fun provideKeyDerivation(impl: DefaultKeyDerivation): KeyDerivation = impl

    @Provides
    @Singleton
    fun provideAutofillTrustStore(impl: AutofillTrustStoreImpl): AutofillTrustStore = impl

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> = context.passkeyDataStore

    @Provides
    @Singleton
    fun providePreferencesRepository(dataStore: DataStore<Preferences>): PreferencesRepository =
        PreferencesRepositoryImpl(dataStore)

    @Provides
    @Singleton
    fun provideBackupReminderStore(dataStore: DataStore<Preferences>): BackupReminderStore =
        BackupReminderStoreImpl(dataStore)

    @Provides
    @Singleton
    fun provideSecretKeywords(keywords: ResourceSecretKeywords): SecretKeywords = keywords
}
