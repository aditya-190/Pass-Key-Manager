package com.bhardwaj.passkey.di

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import com.bhardwaj.passkey.BuildConfig
import com.bhardwaj.passkey.data.local.PassKeyDatabase
import com.bhardwaj.passkey.data.repository.DataStoreRepository
import com.bhardwaj.passkey.data.repository.PasskeyRepository
import com.bhardwaj.passkey.data.repository.passkeyDataStore
import com.bhardwaj.passkey.domain.repository.PasskeyRepositoryImpl
import com.bhardwaj.passkey.utils.Constants
import com.bhardwaj.passkey.utils.MIGRATION_1_2
import com.bhardwaj.passkey.utils.MIGRATION_2_3
import com.bhardwaj.passkey.utils.ResourceSecretKeywords
import com.bhardwaj.passkey.utils.SecretKeywords
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(appContext: Application): PassKeyDatabase = Room.databaseBuilder(
        context = appContext,
        klass = PassKeyDatabase::class.java,
        name = Constants.PASS_KEY_DATABASE
    )
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
        // FIXME(5.7.0): a single build-time constant, identical for every install and recoverable from the APK,
        //  so the database is encrypted against a filesystem thief and nothing else. 5.7.0 replaces it with a
        //  per-install random key wrapped by AndroidKeyStore; this value then only opens and re-keys old vaults.
        .openHelperFactory(factory = SupportOpenHelperFactory(BuildConfig.LEGACY_PASS_PHRASE.toByteArray()))
        .build()

    @Provides
    @Singleton
    fun provideTodoRepository(db: PassKeyDatabase): PasskeyRepository =
        PasskeyRepositoryImpl(db, db.previewDao, db.detailsDao)

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> = context.passkeyDataStore

    @Provides
    @Singleton
    fun provideDataStoreRepository(dataStore: DataStore<Preferences>) = DataStoreRepository(dataStore)

    @Provides
    @Singleton
    fun provideSecretKeywords(keywords: ResourceSecretKeywords): SecretKeywords = keywords
}
