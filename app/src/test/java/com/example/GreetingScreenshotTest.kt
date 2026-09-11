package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.NoteEntity
import com.example.ui.components.NoteCard
import com.example.ui.theme.SecureNotesTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    val sampleNote = NoteEntity(
        id = 1L,
        title = "SecureNotes Quick Memo",
        content = "Offline-first note with biometric security and rich text formatting.",
        category = "General",
        colorKey = "indigo",
        isPinned = true,
        isLocked = false
    )

    composeTestRule.setContent {
      SecureNotesTheme {
        NoteCard(
            note = sampleNote,
            isUnlocked = true,
            isSelectionMode = false,
            isSelected = false,
            onNoteClick = {},
            onNoteLongClick = {},
            onToggleSelect = {},
            getImageFile = { File("/dev/null") }
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
