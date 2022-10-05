package com.bhardwaj.passkey.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.bhardwaj.passkey.data.dao.DetailsDao
import com.bhardwaj.passkey.data.dao.PreviewDao
import com.bhardwaj.passkey.data.entity.Details
import com.bhardwaj.passkey.data.entity.Preview

@Database(entities = [Preview::class, Details::class], version = 1, exportSchema = false)
abstract class PassKeyDatabase : RoomDatabase() {
    abstract fun previewDao(): PreviewDao
    abstract fun detailsDao(): DetailsDao
}