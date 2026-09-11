package com.example.security

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SecurityManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("secure_notes_security_prefs", Context.MODE_PRIVATE)

    private val _isMasterLockEnabled = MutableStateFlow(
        prefs.getBoolean("key_master_lock_enabled", false)
    )
    val isMasterLockEnabled: StateFlow<Boolean> = _isMasterLockEnabled.asStateFlow()

    // Tracks if master app lock has been authenticated for this app session
    private val _isAppUnlocked = MutableStateFlow(!_isMasterLockEnabled.value)
    val isAppUnlocked: StateFlow<Boolean> = _isAppUnlocked.asStateFlow()

    // Set of individual note IDs that have been unlocked in this session
    private val _unlockedNoteIds = MutableStateFlow<Set<Long>>(emptySet())
    val unlockedNoteIds: StateFlow<Set<Long>> = _unlockedNoteIds.asStateFlow()

    fun setMasterLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("key_master_lock_enabled", enabled).apply()
        _isMasterLockEnabled.value = enabled
        if (!enabled) {
            _isAppUnlocked.value = true
        }
    }

    fun markAppUnlocked() {
        _isAppUnlocked.value = true
    }

    fun lockApp() {
        if (_isMasterLockEnabled.value) {
            _isAppUnlocked.value = false
        }
        _unlockedNoteIds.value = emptySet()
    }

    fun unlockNote(noteId: Long) {
        _unlockedNoteIds.value = _unlockedNoteIds.value + noteId
    }

    fun lockNote(noteId: Long) {
        _unlockedNoteIds.value = _unlockedNoteIds.value - noteId
    }

    fun isNoteUnlocked(noteId: Long): Boolean {
        return _unlockedNoteIds.value.contains(noteId)
    }
}
