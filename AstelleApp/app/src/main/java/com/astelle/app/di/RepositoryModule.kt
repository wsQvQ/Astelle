package com.astelle.app.di

import com.astelle.app.data.repository.NoteRepositoryImpl
import com.astelle.app.data.repository.PlanRepositoryImpl
import com.astelle.app.domain.repository.NoteRepository
import com.astelle.app.domain.repository.PlanRepository
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
    abstract fun bindPlanRepository(impl: PlanRepositoryImpl): PlanRepository
}
