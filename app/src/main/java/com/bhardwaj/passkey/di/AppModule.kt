package com.bhardwaj.passkey.di

import android.content.Context
import androidx.room.Room
import com.bhardwaj.passkey.data.database.PassKeyDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
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
            "passkey_database"
        ).build()
    }

    @Singleton
    @Provides
    fun providePreviewDao(db: PassKeyDatabase) = db.previewDao()

    @Singleton
    @Provides
    fun provideDetailsDao(db: PassKeyDatabase) = db.detailsDao()
}