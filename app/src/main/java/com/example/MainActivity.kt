package com.example

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.NoteEntity
import com.example.security.BiometricHelper
import com.example.ui.components.BiometricLockOverlay
import com.example.ui.screens.NoteDetailScreen
import com.example.ui.screens.NoteListScreen
import com.example.ui.theme.SecureNotesTheme
import com.example.ui.viewmodel.NotesViewModel
import com.example.ui.viewmodel.ScreenDestination

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SecureNotesTheme {
                val viewModel: NotesViewModel = viewModel()
                val isMasterLockEnabled by viewModel.securityManager.isMasterLockEnabled.collectAsStateWithLifecycle()
                val isAppUnlocked by viewModel.securityManager.isAppUnlocked.collectAsStateWithLifecycle()
                val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()

                // File picker for importing .secnotes or .zip backup files
                val importBackupLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument()
                ) { uri: Uri? ->
                    uri?.let {
                        viewModel.importNotesFromUri(this@MainActivity, it)
                    }
                }

                // If Master Lock is enabled and app is not yet unlocked in this session
                if (isMasterLockEnabled && !isAppUnlocked) {
                    BiometricLockOverlay(
                        onUnlockClick = {
                            BiometricHelper.authenticate(
                                activity = this@MainActivity,
                                title = "Unlock SecureNotes",
                                subtitle = "Scan fingerprint, face, or enter PIN",
                                onSuccess = {
                                    viewModel.securityManager.markAppUnlocked()
                                },
                                onError = { error ->
                                    Toast.makeText(this@MainActivity, "Verification failed: $error", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    )

                    // Auto-prompt on launch if master locked
                    LaunchedEffect(Unit) {
                        BiometricHelper.authenticate(
                            activity = this@MainActivity,
                            title = "Unlock SecureNotes",
                            subtitle = "Scan fingerprint, face, or enter PIN",
                            onSuccess = {
                                viewModel.securityManager.markAppUnlocked()
                            },
                            onError = { /* Let user tap the button manually */ }
                        )
                    }
                } else {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        AnimatedContent(
                            targetState = currentScreen,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "ScreenTransition"
                        ) { screen ->
                            when (screen) {
                                is ScreenDestination.NoteList -> {
                                    NoteListScreen(
                                        viewModel = viewModel,
                                        onNoteClickWithAuth = { note ->
                                            if (note.isLocked && !viewModel.securityManager.isNoteUnlocked(note.id)) {
                                                // Prompt biometric verification
                                                BiometricHelper.authenticate(
                                                    activity = this@MainActivity,
                                                    title = "Unlock Note",
                                                    subtitle = "Authenticate to view '${note.title.ifBlank { "Encrypted Note" }}'",
                                                    onSuccess = {
                                                        viewModel.securityManager.unlockNote(note.id)
                                                        viewModel.navigateToDetail(note.id)
                                                    },
                                                    onError = { error ->
                                                        Toast.makeText(this@MainActivity, "Authentication failed", Toast.LENGTH_SHORT).show()
                                                    }
                                                )
                                            } else {
                                                viewModel.navigateToDetail(note.id)
                                            }
                                        },
                                        onImportFileRequested = {
                                            importBackupLauncher.launch(arrayOf("*/*"))
                                        },
                                        onToggleMasterLock = { enable ->
                                            BiometricHelper.authenticate(
                                                activity = this@MainActivity,
                                                title = if (enable) "Enable Master Lock" else "Disable Master Lock",
                                                subtitle = "Verify identity to change security settings",
                                                onSuccess = {
                                                    viewModel.securityManager.setMasterLockEnabled(enable)
                                                    val msg = if (enable) "Master Lock Enabled" else "Master Lock Disabled"
                                                    Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                                                },
                                                onError = {
                                                    Toast.makeText(this@MainActivity, "Verification failed", Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        }
                                    )
                                }

                                is ScreenDestination.NoteDetail -> {
                                    BackHandler {
                                        viewModel.navigateBackToList()
                                    }

                                    NoteDetailScreen(
                                        noteId = screen.noteId,
                                        viewModel = viewModel,
                                        onBack = {
                                            viewModel.navigateBackToList()
                                        },
                                        onTriggerBiometricLockChange = { locking, onResult ->
                                            BiometricHelper.authenticate(
                                                activity = this@MainActivity,
                                                title = if (locking) "Lock this Note" else "Unlock Note",
                                                subtitle = "Verify your identity",
                                                onSuccess = { onResult(true) },
                                                onError = { onResult(false) }
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
