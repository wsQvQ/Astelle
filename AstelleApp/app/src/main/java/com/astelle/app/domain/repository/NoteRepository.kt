package com.astelle.app.domain.repository

import com.astelle.app.domain.model.Note
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun observeNotes(): Flow<List<Note>>
    fun observeNote(id: String): Flow<Note?>
    suspend fun getNote(id: String): Note?
    fun search(query: String): Flow<List<Note>>
    suspend fun upsert(note: Note)
    suspend fun delete(id: String)
    suspend fun setPinned(id: String, pinned: Boolean)
    suspend fun setFavorite(id: String, favorite: Boolean)
    suspend fun setArchived(id: String, archived: Boolean)
}
