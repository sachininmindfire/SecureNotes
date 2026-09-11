package com.example.data.transfer

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.NoteEntity
import com.example.data.repository.NoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object NoteTransferManager {

    /**
     * Packages notes and their attached images into a .secnotes (ZIP) archive.
     */
    suspend fun createExportPackage(
        context: Context,
        notes: List<NoteEntity>,
        repository: NoteRepository
    ): File = withContext(Dispatchers.IO) {
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val exportFile = File(exportDir, "SecureNotes_Backup_$timeStamp.secnotes")

        val jsonArray = JSONArray()
        val imageFilesToInclude = mutableSetOf<String>()

        notes.forEach { note ->
            val obj = JSONObject().apply {
                put("id", note.id)
                put("title", note.title)
                put("content", note.content)
                put("category", note.category)
                put("colorKey", note.colorKey)
                put("isPinned", note.isPinned)
                put("isLocked", note.isLocked)
                put("imagePaths", note.imagePaths)
                put("createdTimestamp", note.createdTimestamp)
                put("modifiedTimestamp", note.modifiedTimestamp)
            }
            jsonArray.put(obj)
            note.getImageList().forEach { filename ->
                imageFilesToInclude.add(filename)
            }
        }

        val rootJson = JSONObject().apply {
            put("version", 1)
            put("exportDate", System.currentTimeMillis())
            put("app", "SecureNotes")
            put("count", notes.size)
            put("notes", jsonArray)
        }

        ZipOutputStream(BufferedOutputStream(FileOutputStream(exportFile))).use { zos ->
            // 1. Write manifest.json
            val manifestEntry = ZipEntry("manifest.json")
            zos.putNextEntry(manifestEntry)
            zos.write(rootJson.toString(2).toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 2. Write attached images
            imageFilesToInclude.forEach { filename ->
                val imgFile = repository.getImageFile(filename)
                if (imgFile.exists()) {
                    val entry = ZipEntry("images/$filename")
                    zos.putNextEntry(entry)
                    FileInputStream(imgFile).use { fis ->
                        fis.copyTo(zos)
                    }
                    zos.closeEntry()
                }
            }
        }

        exportFile
    }

    /**
     * Imports notes and attached images from a .secnotes or .zip file.
     * Returns the count of notes imported.
     */
    suspend fun importPackage(
        context: Context,
        uri: Uri,
        repository: NoteRepository
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val imagesDir = File(context.filesDir, "note_images")
            if (!imagesDir.exists()) imagesDir.mkdirs()

            var manifestContent: String? = null
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("Cannot open file stream"))

            ZipInputStream(BufferedInputStream(inputStream)).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    val name = entry.name
                    if (name == "manifest.json") {
                        manifestContent = zis.bufferedReader(Charsets.UTF_8).readText()
                    } else if (name.startsWith("images/") && !entry.isDirectory) {
                        val imageName = name.removePrefix("images/")
                        if (imageName.isNotEmpty()) {
                            val targetFile = File(imagesDir, imageName)
                            FileOutputStream(targetFile).use { fos ->
                                zis.copyTo(fos)
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            if (manifestContent == null) {
                return@withContext Result.failure(Exception("Invalid backup file: manifest.json missing"))
            }

            val rootJson = JSONObject(manifestContent)
            val notesArray = rootJson.getJSONArray("notes")
            val importedNotes = mutableListOf<NoteEntity>()

            for (i in 0 until notesArray.length()) {
                val obj = notesArray.getJSONObject(i)
                val note = NoteEntity(
                    id = 0L, // Create new ID in local DB
                    title = obj.optString("title", "Untitled"),
                    content = obj.optString("content", ""),
                    category = obj.optString("category", "General"),
                    colorKey = obj.optString("colorKey", "default"),
                    isPinned = obj.optBoolean("isPinned", false),
                    isLocked = obj.optBoolean("isLocked", false),
                    imagePaths = obj.optString("imagePaths", ""),
                    createdTimestamp = obj.optLong("createdTimestamp", System.currentTimeMillis()),
                    modifiedTimestamp = obj.optLong("modifiedTimestamp", System.currentTimeMillis()),
                    isArchived = false
                )
                importedNotes.add(note)
            }

            if (importedNotes.isNotEmpty()) {
                repository.insertNotes(importedNotes)
            }

            Result.success(importedNotes.size)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Exports notes as a combined Markdown document file.
     */
    suspend fun createMarkdownExportFile(
        context: Context,
        notes: List<NoteEntity>
    ): File = withContext(Dispatchers.IO) {
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val mdFile = File(exportDir, "SecureNotes_Export_$timeStamp.md")

        val sb = StringBuilder()
        sb.append("# SecureNotes Export\n")
        sb.append("Generated on: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}\n")
        sb.append("Total Notes: ${notes.size}\n\n---\n\n")

        notes.forEachIndexed { index, note ->
            sb.append("## ${note.title.ifBlank { "Untitled Note" }}\n")
            sb.append("*Category: ${note.category} | Created: ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(note.createdTimestamp))}*\n\n")
            sb.append(note.content)
            sb.append("\n\n---\n\n")
        }

        FileOutputStream(mdFile).use { fos ->
            fos.write(sb.toString().toByteArray(Charsets.UTF_8))
        }

        mdFile
    }

    /**
     * Launches Android Sharesheet to send a file to another device via Quick Share, Bluetooth, Drive, etc.
     */
    fun shareFile(context: Context, file: File, title: String = "Share Notes Backup") {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = if (file.name.endsWith(".md")) "text/markdown" else "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            putExtra(Intent.EXTRA_TEXT, "SecureNotes backup file with ${file.name}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    /**
     * Shares note text directly via standard Android Sharesheet.
     */
    fun shareNoteText(context: Context, note: NoteEntity) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, note.title)
            putExtra(Intent.EXTRA_TEXT, "${note.title}\n\n${note.content}")
        }
        val chooser = Intent.createChooser(intent, "Share Note via...").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
