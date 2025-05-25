package com.bhardwaj.passkey.di

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.bhardwaj.passkey.BuildConfig
import com.bhardwaj.passkey.data.local.PassKeyDatabase
import com.bhardwaj.passkey.data.repository.DataStoreRepository
import com.bhardwaj.passkey.data.repository.PasskeyRepository
import com.bhardwaj.passkey.domain.repository.PasskeyRepositoryImpl
import com.bhardwaj.passkey.utils.Constants
import com.bhardwaj.passkey.utils.MIGRATION_1_2
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(appContext: Application): PassKeyDatabase {
        return Room.databaseBuilder(
            context = appContext,
            klass = PassKeyDatabase::class.java,
            name = Constants.PASS_KEY_DATABASE
        )
            .addMigrations(MIGRATION_1_2)
            .openHelperFactory(factory = SupportOpenHelperFactory(BuildConfig.PASS_PHRASE.toByteArray()))
            .build()
    }

    @Provides
    @Singleton
    fun provideTodoRepository(db: PassKeyDatabase): PasskeyRepository {
        return PasskeyRepositoryImpl(db.previewDao, db.detailsDao)
    }

    @Provides
    @Singleton
    fun provideDataStoreRepository(
        @ApplicationContext context: Context
    ) = DataStoreRepository(context = context)
}