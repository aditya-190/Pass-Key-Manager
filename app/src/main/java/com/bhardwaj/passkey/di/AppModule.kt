package com.bhardwaj.passkey.di

import android.content.Context
import androidx.room.Room
import com.bhardwaj.passkey.BuildConfig
import com.bhardwaj.passkey.data.database.PassKeyDatabase
import com.bhardwaj.passkey.utils.Constants.Companion.PASS_KEY_DATABASE
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.sqlcipher.database.SupportFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Singleton
    @Provides
    fun providePassKeyDatabase(@ApplicationContext context: Context): PassKeyDatabase {
        return Room.databaseBuilder(
            context,
            PassKeyDatabase::class.java,
            PASS_KEY_DATABASE
        ).build()//openHelperFactory(SupportFactory(BuildConfig.PASS_PHRASE.toByteArray())).build()
    }

    @Singleton
    @Provides
    fun providePreviewDao(db: PassKeyDatabase) = db.previewDao()

    @Singleton
    @Provides
    fun provideDetailsDao(db: PassKeyDatabase) = db.detailsDao()
}