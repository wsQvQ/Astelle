package com.astelle.app.di

import com.astelle.app.data.repository.FolderRepositoryImpl
import com.astelle.app.data.repository.NoteRepositoryImpl
import com.astelle.app.data.repository.TaskRepositoryImpl
import com.astelle.app.domain.repository.FolderRepository
import com.astelle.app.domain.repository.NoteRepository
import com.astelle.app.domain.repository.TaskRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindNoteRepository(impl: NoteRepositoryImpl): NoteRepository

    @Binds
    @Singleton
    abstract fun bindFolderRepository(impl: FolderRepositoryImpl): FolderRepository

    @Binds
    @Singleton
    abstract fun bindTaskRepository(impl: TaskRepositoryImpl): TaskRepository
}
