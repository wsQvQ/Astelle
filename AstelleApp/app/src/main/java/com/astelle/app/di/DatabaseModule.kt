package com.astelle.app.di

import android.content.Context
import androidx.room.Room
import com.astelle.app.data.local.AstelleDatabase
import com.astelle.app.data.local.dao.FolderDao
import com.astelle.app.data.local.dao.NoteDao
import com.astelle.app.data.local.dao.PlanDayDao
import com.astelle.app.data.local.dao.TodoDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AstelleDatabase =
        Room.databaseBuilder(context, AstelleDatabase::class.java, AstelleDatabase.NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideNoteDao(db: AstelleDatabase): NoteDao = db.noteDao()

    @Provides
    fun provideFolderDao(db: AstelleDatabase): FolderDao = db.folderDao()

    @Provides
    fun providePlanDayDao(db: AstelleDatabase): PlanDayDao = db.planDayDao()

    @Provides
    fun provideTodoDao(db: AstelleDatabase): TodoDao = db.todoDao()
}
