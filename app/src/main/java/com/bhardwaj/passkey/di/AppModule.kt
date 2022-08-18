package com.bhardwaj.passkey.di

import android.content.Context
import androidx.room.Room
import com.bhardwaj.passkey.data.database.DetailsDatabase
import com.bhardwaj.passkey.data.database.PreviewDatabase
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
    fun providePreviewDatabase(@ApplicationContext context: Context): PreviewDatabase {
        return Room.databaseBuilder(
            context,
            PreviewDatabase::class.java,
            "preview_database"
        ).build()
    }

    @Singleton
    @Provides
    fun provideDetailsDatabase(@ApplicationContext context: Context): DetailsDatabase {
        return Room.databaseBuilder(
            context,
            DetailsDatabase::class.java,
            "details_database"
        ).build()
    }

    @Singleton
    @Provides
    fun providePreviewDao(db: PreviewDatabase) = db.previewDao()

    @Singleton
    @Provides
    fun provideDetailsDao(db: DetailsDatabase) = db.detailsDao()
}