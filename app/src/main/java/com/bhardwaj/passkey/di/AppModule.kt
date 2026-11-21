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
        .addMigrations(MIGRATION_1_2)
        .openHelperFactory(factory = SupportOpenHelperFactory(BuildConfig.PASS_PHRASE.toByteArray()))
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
}
