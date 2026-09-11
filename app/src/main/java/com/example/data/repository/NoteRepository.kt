package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.db.NoteDao
import com.example.data.model.NoteEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

class NoteRepository(
    private val noteDao: NoteDao,
    private val context: Context
) {
    val activeNotes: Flow<List<NoteEntity>> = noteDao.getAllActiveNotes()

    fun searchNotes(query: String): Flow<List<NoteEntity>> = noteDao.searchNotes(query)

    fun getNoteById(id: Long): Flow<NoteEntity?> = noteDao.getNoteById(id)

    suspend fun getNoteByIdOnce(id: Long): NoteEntity? = noteDao.getNoteByIdOnce(id)

    suspend fun getNotesByIds(ids: List<Long>): List<NoteEntity> = noteDao.getNotesByIds(ids)

    suspend fun getAllNotesOnce(): List<NoteEntity> = noteDao.getAllNotesOnce()

    suspend fun insertNote(note: NoteEntity): Long = noteDao.insertNote(note)

    suspend fun insertNotes(notes: List<NoteEntity>): List<Long> = noteDao.insertNotes(notes)

    suspend fun updateNote(note: NoteEntity) = noteDao.updateNote(note)

    suspend fun deleteNote(note: NoteEntity) {
        // Also delete attached images from disk
        withContext(Dispatchers.IO) {
            note.getImageList().forEach { filename ->
                deleteImageFile(filename)
            }
        }
        noteDao.deleteNote(note)
    }

    suspend fun deleteNotesByIds(ids: List<Long>) {
        withContext(Dispatchers.IO) {
            val notes = noteDao.getNotesByIds(ids)
            notes.forEach { note ->
                note.getImageList().forEach { filename ->
                    deleteImageFile(filename)
                }
            }
        }
        noteDao.deleteNotesByIds(ids)
    }

    suspend fun saveImageFromUri(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val imagesDir = File(context.filesDir, "note_images")
            if (!imagesDir.exists()) {
                imagesDir.mkdirs()
            }
            val filename = "img_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg"
            val destFile = File(imagesDir, filename)

            context.contentResolver.openInputStream(uri)?.use { input: InputStream ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            filename
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getImageFile(filename: String): File {
        val imagesDir = File(context.filesDir, "note_images")
        return File(imagesDir, filename)
    }

    private fun deleteImageFile(filename: String) {
        try {
            val file = getImageFile(filename)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun seedInitialNotesIfEmpty() = withContext(Dispatchers.IO) {
        val existing = noteDao.getAllNotesOnce()
        if (existing.isEmpty()) {
            val welcomeNote = NoteEntity(
                title = "Welcome to SecureNotes 🛡️",
                content = """
# Welcome to SecureNotes!

SecureNotes is your private, offline-first personal workspace built with **Material You** design and top-grade device security.

### Key Features
- [x] **Offline-First Storage**: Zero cloud reliance. Everything stays safely on your device in Room DB.
- [x] **Biometric Security**: Lock individual private notes or enable Master Lock with your fingerprint, face, or device PIN.
- [x] **Rich Text & Checklists**: Format notes with bold, italic, code blocks, quotes, and interactive checklists.
- [x] **Image Support**: Attach high-resolution photos and documents directly inside your notes.
- [x] **Cross-Device Transfer**: Export all or selected notes into a single `.secnotes` package and share via Quick Share or Bluetooth to any Android phone.

> "True privacy means your data never leaves your control."

Feel free to edit or delete this note, or tap **+** below to create your first secure note!
                """.trimIndent(),
                category = "General",
                colorKey = "indigo",
                isPinned = true,
                isLocked = false
            )

            val securityNote = NoteEntity(
                title = "Biometric Security & Privacy Guide 🔒",
                content = """
## How Security Works

SecureNotes leverages Android's hardware-backed **BiometricPrompt** system:

- **Lock Any Note**: Tap the padlock icon in the editor or card menu to lock sensitive notes (passwords, secret keys, personal journals).
- **Hidden Previews**: Content and images of locked notes remain concealed until verified.
- **Biometrics & PIN Support**: Seamlessly authenticate using your fingerprint, facial recognition, or lock screen PIN.
- **Master Lock**: Enable Master Lock in Settings to safeguard the entire app upon launch.

### Security Checklist
- [x] Biometric authentication active
- [x] Local AES-grade private storage
- [ ] Set your own secret note
                """.trimIndent(),
                category = "Private",
                colorKey = "emerald",
                isPinned = true,
                isLocked = true
            )

            val transferNote = NoteEntity(
                title = "Device-to-Device Sharing & Sideloading 📲",
                content = """
### Transferring to Another Android Phone

You can export or share notes to another Android device without cloud servers:

1. **Quick Share / Bluetooth**:
   Select notes (or tap Export All) -> Choose **SecureNotes Package (.secnotes)** -> Send via Quick Share or Bluetooth.
2. **Restore**:
   On the other device, tap **Import Notes** in the top menu, select the `.secnotes` file, and all notes and images are restored instantly!
3. **Easy Sideloading**:
   SecureNotes is designed to be sideloaded directly as an APK. Send the APK to friends or your other devices and tap install!
                """.trimIndent(),
                category = "Work",
                colorKey = "amber",
                isPinned = false,
                isLocked = false
            )

            noteDao.insertNotes(listOf(welcomeNote, securityNote, transferNote))
        }
    }
}
