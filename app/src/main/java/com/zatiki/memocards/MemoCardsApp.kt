package com.zatiki.memocards

import android.app.Application
import androidx.room.Room
import com.zatiki.memocards.data.MemoDatabase
import com.zatiki.memocards.data.MemoRepository

class MemoCardsApp : Application() {
    lateinit var repository: MemoRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val db = Room.databaseBuilder(this, MemoDatabase::class.java, "memocards.db")
            .fallbackToDestructiveMigration()
            .build()
        repository = MemoRepository(db.dao())
    }
}
