package com.example.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.NoteEntity
import com.example.data.transfer.NoteTransferManager
import com.example.ui.components.NoteQrDialog
import com.example.ui.components.RichTextEditorToolbar
import com.example.ui.components.RichTextRenderer
import com.example.ui.theme.NoteColorPalettes
import com.example.ui.theme.getNoteColorStyle
import com.example.ui.viewmodel.NotesViewModel
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    noteId: Long?,
    viewModel: NotesViewModel,
    onBack: () -> Unit,
    onTriggerBiometricLockChange: (Boolean, (Boolean) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()

    var title by remember { mutableStateOf("") }
    var contentValue by remember { mutableStateOf(TextFieldValue("")) }
    var category by remember { mutableStateOf("General") }
    var colorKey by remember { mutableStateOf("default") }
    var isPinned by remember { mutableStateOf(false) }
    var isLocked by remember { mutableStateOf(false) }
    var attachedImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var isReadMode by remember { mutableStateOf(false) }

    var isLoaded by remember { mutableStateOf(false) }
    var showColorDialog by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }
    var fullScreenImage by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val categories = listOf("General", "Work", "Personal", "Ideas", "Finance", "Private")

    // Load initial note data if editing an existing note
    LaunchedEffect(noteId) {
        if (noteId != null && noteId > 0 && !isLoaded) {
            val existing = viewModel.repository.getNoteByIdOnce(noteId)
            existing?.let {
                title = it.title
                contentValue = TextFieldValue(it.content)
                category = it.category
                colorKey = it.colorKey
                isPinned = it.isPinned
                isLocked = it.isLocked
                attachedImages = it.getImageList()
            }
            isLoaded = true
        }
    }

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            scope.launch {
                val newPaths = mutableListOf<String>()
                uris.forEach { uri ->
                    val saved = viewModel.saveImageFromUri(uri)
                    if (saved != null) {
                        newPaths.add(saved)
                    }
                }
                attachedImages = attachedImages + newPaths
            }
        }
    }

    val noteColorStyle = getNoteColorStyle(colorKey)
    val screenBg = if (isDark) noteColorStyle.darkBg else noteColorStyle.lightBg
    val textColor = if (isDark) noteColorStyle.darkText else noteColorStyle.lightText

    fun doSave() {
        viewModel.saveNote(
            id = noteId,
            title = title.trim(),
            content = contentValue.text,
            category = category,
            colorKey = colorKey,
            isPinned = isPinned,
            isLocked = isLocked,
            imagePaths = attachedImages
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = screenBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (noteId == null || noteId == 0L) "New Note" else "Edit Note",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = textColor
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            doSave()
                        },
                        modifier = Modifier.testTag("btn_note_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Save and go back",
                            tint = textColor
                        )
                    }
                },
                actions = {
                    // Read / Edit toggle
                    IconButton(
                        onClick = { isReadMode = !isReadMode },
                        modifier = Modifier.testTag("btn_toggle_read_edit")
                    ) {
                        Icon(
                            imageVector = if (isReadMode) Icons.Default.Edit else Icons.Default.Visibility,
                            contentDescription = if (isReadMode) "Edit mode" else "Preview mode",
                            tint = textColor
                        )
                    }

                    // Lock toggle
                    IconButton(
                        onClick = {
                            if (!isLocked) {
                                // Request biometric confirmation to set lock
                                onTriggerBiometricLockChange(true) { success ->
                                    if (success) isLocked = true
                                }
                            } else {
                                onTriggerBiometricLockChange(false) { success ->
                                    if (success) isLocked = false
                                }
                            }
                        },
                        modifier = Modifier.testTag("btn_detail_toggle_lock")
                    ) {
                        Icon(
                            imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = if (isLocked) "Locked Note" else "Unlock Note",
                            tint = if (isLocked) MaterialTheme.colorScheme.error else textColor
                        )
                    }

                    // Pin toggle
                    IconButton(
                        onClick = { isPinned = !isPinned },
                        modifier = Modifier.testTag("btn_detail_toggle_pin")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pin Note",
                            tint = if (isPinned) MaterialTheme.colorScheme.primary else textColor.copy(alpha = 0.6f)
                        )
                    }

                    // QR Code Transfer
                    IconButton(
                        onClick = { showQrDialog = true },
                        modifier = Modifier.testTag("btn_detail_qr_share")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = "Show QR Code Transfer",
                            tint = textColor
                        )
                    }

                    // Android Sharesheet
                    IconButton(
                        onClick = {
                            val tempNote = NoteEntity(
                                id = noteId ?: 0L,
                                title = title,
                                content = contentValue.text,
                                category = category,
                                colorKey = colorKey,
                                imagePaths = attachedImages.joinToString(",")
                            )
                            NoteTransferManager.shareNoteText(context, tempNote)
                        },
                        modifier = Modifier.testTag("btn_detail_share")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Note",
                            tint = textColor
                        )
                    }

                    // Delete Note (if existing)
                    if (noteId != null && noteId > 0) {
                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.testTag("btn_detail_delete")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Note",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    // Save / Done
                    IconButton(
                        onClick = { doSave() },
                        modifier = Modifier.testTag("btn_save_note")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Save Note",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = screenBg)
            )
        },
        bottomBar = {
            if (!isReadMode) {
                RichTextEditorToolbar(
                    textFieldValue = contentValue,
                    onValueChange = { contentValue = it },
                    onAddImageClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onColorPickerClick = { showColorDialog = true },
                    isLocked = isLocked,
                    onToggleLock = {
                        if (!isLocked) {
                            onTriggerBiometricLockChange(true) { if (it) isLocked = true }
                        } else {
                            onTriggerBiometricLockChange(false) { if (it) isLocked = false }
                        }
                    }
                )
            }
        },
        contentWindowInsets = WindowInsets.statusBars.union(WindowInsets.navigationBars)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Category Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    val isCatSelected = category.equals(cat, ignoreCase = true)
                    FilterChip(
                        selected = isCatSelected,
                        onClick = { category = cat },
                        label = { Text(cat) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Note Title Input
            TextField(
                value = title,
                onValueChange = { title = it },
                placeholder = {
                    Text(
                        text = "Title",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = textColor.copy(alpha = 0.5f)
                    )
                },
                textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = textColor),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("note_title_input")
            )

            // Attached Images Carousel
            if (attachedImages.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(attachedImages) { filename ->
                        val imgFile = viewModel.getImageFile(filename)
                        Box(
                            modifier = Modifier
                                .size(width = 110.dp, height = 90.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { fullScreenImage = filename }
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(imgFile)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Attached photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Remove Image Button
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.65f),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(24.dp)
                                    .clickable {
                                        attachedImages = attachedImages.filter { it != filename }
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove photo",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Plus Add Image Button
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = textColor.copy(alpha = 0.08f),
                            modifier = Modifier
                                .size(width = 90.dp, height = 90.dp)
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = "Add photo",
                                    tint = textColor.copy(alpha = 0.7f),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Add",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = textColor.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            HorizontalDivider(
                color = textColor.copy(alpha = 0.12f),
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // Content Area: Either Rich Text Renderer or Interactive Editor
            if (isReadMode) {
                // Formatted Reading View with Interactive Checklist Toggles
                RichTextRenderer(
                    content = contentValue.text,
                    textColor = textColor,
                    onToggleChecklist = { lineIndex ->
                        val lines = contentValue.text.lines().toMutableList()
                        if (lineIndex in lines.indices) {
                            val line = lines[lineIndex]
                            if (line.trimStart().startsWith("- [ ] ")) {
                                lines[lineIndex] = line.replaceFirst("- [ ] ", "- [x] ")
                            } else if (line.trimStart().startsWith("- [x] ") || line.trimStart().startsWith("- [X] ")) {
                                lines[lineIndex] = line.replaceFirst(Regex("- \\[x\\] |- \\[X\\] "), "- [ ] ")
                            }
                            contentValue = TextFieldValue(lines.joinToString("\n"))
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 32.dp)
                )
            } else {
                // Interactive Editor
                TextField(
                    value = contentValue,
                    onValueChange = { contentValue = it },
                    placeholder = {
                        Text(
                            text = "Start typing your thoughts, checklists (- [ ]), notes, code blocks...\nUse the formatting toolbar below for instant rich styles.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = textColor.copy(alpha = 0.45f)
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = textColor, lineHeight = 24.sp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 240.dp)
                        .testTag("note_content_input")
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Color Swatch Dialog
    if (showColorDialog) {
        AlertDialog(
            onDismissRequest = { showColorDialog = false },
            title = { Text("Choose Note Color", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
            text = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 8.dp)
                ) {
                    NoteColorPalettes.forEach { palette ->
                        val isSelected = colorKey == palette.key
                        val swatchColor = if (isDark) palette.darkBg else palette.lightBg

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(swatchColor)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f),
                                    shape = CircleShape
                                )
                                .clickable {
                                    colorKey = palette.key
                                    showColorDialog = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = if (isDark) palette.darkText else palette.lightText,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showColorDialog = false }) {
                    Text("Done")
                }
            }
        )
    }

    // QR Optical Transfer Dialog
    if (showQrDialog) {
        NoteQrDialog(
            noteTitle = title,
            noteContent = contentValue.text,
            onDismiss = { showQrDialog = false },
            onShareText = {
                val tempNote = NoteEntity(
                    id = noteId ?: 0L,
                    title = title,
                    content = contentValue.text,
                    category = category,
                    colorKey = colorKey
                )
                NoteTransferManager.shareNoteText(context, tempNote)
            }
        )
    }

    // Full Screen Image Dialog
    if (fullScreenImage != null) {
        val imgFile = viewModel.getImageFile(fullScreenImage!!)
        Dialog(
            onDismissRequest = { fullScreenImage = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imgFile)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Full Screen Photo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(16.dp)
                )

                IconButton(
                    onClick = { fullScreenImage = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(44.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close preview", tint = Color.White)
                }
            }
        }
    }

    // Delete Confirmation
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Note?") },
            text = { Text("This will permanently remove this note and any attached images from your device.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        if (noteId != null && noteId > 0) {
                            scope.launch {
                                val existing = viewModel.repository.getNoteByIdOnce(noteId)
                                existing?.let { viewModel.deleteNote(it) }
                            }
                        }
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
