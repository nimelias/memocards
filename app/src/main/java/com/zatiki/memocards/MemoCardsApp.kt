package com.zatiki.memocards

import android.app.Application
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.zatiki.memocards.data.CrashLog
import com.zatiki.memocards.data.MemoDatabase
import com.zatiki.memocards.data.MemoRepository

class MemoCardsApp : Application() {
    lateinit var repository: MemoRepository
        private set

    override fun onCreate() {
        super.onCreate()
        CrashLog.install(this)
        val db = Room.databaseBuilder(this, MemoDatabase::class.java, "memocards.db")
            .addMigrations(MIGRATION_6_7)
            .fallbackToDestructiveMigration()
            .build()
        repository = MemoRepository(db.dao())
    }

    companion object {
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE decks ADD COLUMN subject TEXT")
                db.execSQL("ALTER TABLE books ADD COLUMN subject TEXT")
                db.execSQL("ALTER TABLE books ADD COLUMN annotations_json TEXT NOT NULL DEFAULT '[]'")
            }
        }
    }
}
