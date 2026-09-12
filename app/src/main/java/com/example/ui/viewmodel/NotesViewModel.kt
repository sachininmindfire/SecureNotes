package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.NoteEntity
import com.example.data.repository.NoteRepository
import com.example.data.transfer.NoteTransferManager
import com.example.security.SecurityManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

sealed class ScreenDestination {
    object NoteList : ScreenDestination()
    data class NoteDetail(val noteId: Long?) : ScreenDestination() // null = new note
}

class NotesViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    val repository = NoteRepository(database.noteDao(), application)
    val securityManager = SecurityManager(application)

    // Current navigation destination
    private val _currentScreen = MutableStateFlow<ScreenDestination>(ScreenDestination.NoteList)
    val currentScreen: StateFlow<ScreenDestination> = _currentScreen.asStateFlow()

    // Search and filters
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>("All")
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _filterOnlyPinned = MutableStateFlow(false)
    val filterOnlyPinned: StateFlow<Boolean> = _filterOnlyPinned.asStateFlow()

    private val _filterOnlyLocked = MutableStateFlow(false)
    val filterOnlyLocked: StateFlow<Boolean> = _filterOnlyLocked.asStateFlow()

    private val appPrefs = application.getSharedPreferences("secure_notes_app_prefs", Context.MODE_PRIVATE)

    // UI View layout: true = 2-column grid, false = 1-column list (persisted)
    private val _isGridView = MutableStateFlow(
        appPrefs.getBoolean("key_is_grid_view", true)
    )
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    // Gemini API Key (persisted securely on-device in app preferences)
    private val _geminiApiKey = MutableStateFlow(
        appPrefs.getString("key_gemini_api_key", "") ?: ""
    )
    val geminiApiKey: StateFlow<String> = _geminiApiKey.asStateFlow()

    fun setGeminiApiKey(key: String) {
        val trimmed = key.trim()
        appPrefs.edit().putString("key_gemini_api_key", trimmed).apply()
        _geminiApiKey.value = trimmed
        _userMessage.value = if (trimmed.isNotEmpty()) "Gemini API key saved" else "Gemini API key removed"
    }

    fun getGeminiApiKey(): String {
        return _geminiApiKey.value
    }

    // Multi-selection state
    private val _selectedNoteIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedNoteIds: StateFlow<Set<Long>> = _selectedNoteIds.asStateFlow()

    // Toast/Snackbar notifications
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedInitialNotesIfEmpty()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val filteredNotes: StateFlow<List<NoteEntity>> = combine(
        _searchQuery.flatMapLatest { query ->
            if (query.isBlank()) repository.activeNotes else repository.searchNotes(query)
        },
        _selectedCategory,
        _filterOnlyPinned,
        _filterOnlyLocked
    ) { notes, category, onlyPinned, onlyLocked ->
        notes.filter { note ->
            val matchesCategory = (category == null || category == "All" || note.category.equals(category, ignoreCase = true))
            val matchesPinned = if (onlyPinned) note.isPinned else true
            val matchesLocked = if (onlyLocked) note.isLocked else true
            matchesCategory && matchesPinned && matchesLocked
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelect(category: String?) {
        _selectedCategory.value = category
    }

    fun toggleFilterOnlyPinned() {
        _filterOnlyPinned.value = !_filterOnlyPinned.value
    }

    fun toggleFilterOnlyLocked() {
        _filterOnlyLocked.value = !_filterOnlyLocked.value
    }

    fun toggleViewLayout() {
        val next = !_isGridView.value
        _isGridView.value = next
        appPrefs.edit().putBoolean("key_is_grid_view", next).apply()
    }

    fun navigateToDetail(noteId: Long?) {
        _currentScreen.value = ScreenDestination.NoteDetail(noteId)
    }

    fun navigateBackToList() {
        _currentScreen.value = ScreenDestination.NoteList
    }

    // Selection actions
    fun toggleNoteSelection(noteId: Long) {
        val current = _selectedNoteIds.value
        _selectedNoteIds.value = if (current.contains(noteId)) {
            current - noteId
        } else {
            current + noteId
        }
    }

    fun selectAll(notes: List<NoteEntity>) {
        _selectedNoteIds.value = notes.map { it.id }.toSet()
    }

    fun clearSelection() {
        _selectedNoteIds.value = emptySet()
    }

    fun deleteSelectedNotes() {
        viewModelScope.launch {
            val ids = _selectedNoteIds.value.toList()
            if (ids.isNotEmpty()) {
                repository.deleteNotesByIds(ids)
                _selectedNoteIds.value = emptySet()
                _userMessage.value = "Deleted ${ids.size} notes"
            }
        }
    }

    // Note operations
    fun saveNote(
        id: Long?,
        title: String,
        content: String,
        category: String,
        colorKey: String,
        isPinned: Boolean,
        isLocked: Boolean,
        imagePaths: List<String>
    ) {
        viewModelScope.launch {
            val joinedImages = imagePaths.filter { it.isNotBlank() }.joinToString(",")
            val now = System.currentTimeMillis()

            if (id == null || id == 0L) {
                val newNote = NoteEntity(
                    title = title,
                    content = content,
                    category = category,
                    colorKey = colorKey,
                    isPinned = isPinned,
                    isLocked = isLocked,
                    imagePaths = joinedImages,
                    createdTimestamp = now,
                    modifiedTimestamp = now
                )
                val newId = repository.insertNote(newNote)
                if (isLocked) {
                    securityManager.unlockNote(newId)
                }
            } else {
                val existing = repository.getNoteByIdOnce(id)
                val updatedNote = NoteEntity(
                    id = id,
                    title = title,
                    content = content,
                    category = category,
                    colorKey = colorKey,
                    isPinned = isPinned,
                    isLocked = isLocked,
                    imagePaths = joinedImages,
                    createdTimestamp = existing?.createdTimestamp ?: now,
                    modifiedTimestamp = now
                )
                repository.updateNote(updatedNote)
            }
            _currentScreen.value = ScreenDestination.NoteList
            _userMessage.value = "Note saved"
        }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.deleteNote(note)
            _userMessage.value = "Note deleted"
            if (_currentScreen.value is ScreenDestination.NoteDetail) {
                _currentScreen.value = ScreenDestination.NoteList
            }
        }
    }

    fun togglePin(note: NoteEntity) {
        viewModelScope.launch {
            repository.updateNote(note.copy(isPinned = !note.isPinned, modifiedTimestamp = System.currentTimeMillis()))
        }
    }

    fun toggleLock(note: NoteEntity) {
        viewModelScope.launch {
            val newLocked = !note.isLocked
            repository.updateNote(note.copy(isLocked = newLocked, modifiedTimestamp = System.currentTimeMillis()))
            if (newLocked) {
                securityManager.unlockNote(note.id)
            }
        }
    }

    suspend fun saveImageFromUri(uri: Uri): String? {
        return repository.saveImageFromUri(uri)
    }

    fun getImageFile(filename: String): File {
        return repository.getImageFile(filename)
    }

    // Transfer and export
    fun exportNotesPackage(context: Context, selectedOnly: Boolean) {
        viewModelScope.launch {
            val notesToExport = if (selectedOnly) {
                repository.getNotesByIds(_selectedNoteIds.value.toList())
            } else {
                repository.getAllNotesOnce()
            }

            if (notesToExport.isEmpty()) {
                _userMessage.value = "No notes to export"
                return@launch
            }

            try {
                val zipFile = NoteTransferManager.createExportPackage(context, notesToExport, repository)
                NoteTransferManager.shareFile(context, zipFile, "Share SecureNotes Backup")
                _userMessage.value = "Exported ${notesToExport.size} notes"
                clearSelection()
            } catch (e: Exception) {
                e.printStackTrace()
                _userMessage.value = "Export failed: ${e.localizedMessage}"
            }
        }
    }

    fun exportNotesMarkdown(context: Context, selectedOnly: Boolean) {
        viewModelScope.launch {
            val notesToExport = if (selectedOnly) {
                repository.getNotesByIds(_selectedNoteIds.value.toList())
            } else {
                repository.getAllNotesOnce()
            }

            if (notesToExport.isEmpty()) {
                _userMessage.value = "No notes to export"
                return@launch
            }

            try {
                val mdFile = NoteTransferManager.createMarkdownExportFile(context, notesToExport)
                NoteTransferManager.shareFile(context, mdFile, "Share Markdown Notes")
                _userMessage.value = "Exported ${notesToExport.size} notes as Markdown"
                clearSelection()
            } catch (e: Exception) {
                e.printStackTrace()
                _userMessage.value = "Export failed: ${e.localizedMessage}"
            }
        }
    }

    fun importNotesFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            val result = NoteTransferManager.importPackage(context, uri, repository)
            result.onSuccess { count ->
                _userMessage.value = "Successfully imported $count notes"
            }.onFailure { error ->
                _userMessage.value = "Import failed: ${error.localizedMessage}"
            }
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }
}
