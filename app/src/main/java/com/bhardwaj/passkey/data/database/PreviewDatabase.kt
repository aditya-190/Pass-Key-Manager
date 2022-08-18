package com.bhardwaj.passkey.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.bhardwaj.passkey.data.dao.PreviewDao
import com.bhardwaj.passkey.data.entity.Preview

@Database(entities = [Preview::class], version = 1, exportSchema = false)
abstract class PreviewDatabase : RoomDatabase() {
    abstract fun previewDao(): PreviewDao
}