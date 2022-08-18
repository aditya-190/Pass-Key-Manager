package com.bhardwaj.passkey.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.bhardwaj.passkey.data.dao.DetailsDao
import com.bhardwaj.passkey.data.entity.Details

@Database(entities = [Details::class], version = 1, exportSchema = false)
abstract class DetailsDatabase : RoomDatabase() {
    abstract fun detailsDao(): DetailsDao
}