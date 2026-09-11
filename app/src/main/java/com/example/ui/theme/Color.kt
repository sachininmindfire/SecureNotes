package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Primary Material You seed tokens
val SeedIndigo = Color(0xFF3F51B5)
val SeedTeal = Color(0xFF009688)

val PrimaryLight = Color(0xFF325DA8)
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFD8E2FF)
val OnPrimaryContainerLight = Color(0xFF001A41)

val PrimaryDark = Color(0xFFAEC6FF)
val OnPrimaryDark = Color(0xFF002E69)
val PrimaryContainerDark = Color(0xFF13448F)
val OnPrimaryContainerDark = Color(0xFFD8E2FF)

val SurfaceLight = Color(0xFFF9F9FF)
val OnSurfaceLight = Color(0xFF191C20)
val SurfaceContainerLight = Color(0xFFF0F3FA)
val SurfaceContainerHighLight = Color(0xFFE8EDF4)

val SurfaceDark = Color(0xFF111318)
val OnSurfaceDark = Color(0xFFE2E2E9)
val SurfaceContainerDark = Color(0xFF1D2024)
val SurfaceContainerHighDark = Color(0xFF272A2F)

// Note Card Color Tones (Material You accents)
data class NoteColorStyle(
    val key: String,
    val name: String,
    val lightBg: Color,
    val lightText: Color,
    val darkBg: Color,
    val darkText: Color,
    val borderLight: Color,
    val borderDark: Color
)

val NoteColorPalettes = listOf(
    NoteColorStyle(
        key = "default",
        name = "Default",
        lightBg = Color(0xFFF8FAFC),
        lightText = Color(0xFF1E293B),
        darkBg = Color(0xFF1E242E),
        darkText = Color(0xFFE2E8F0),
        borderLight = Color(0xFFE2E8F0),
        borderDark = Color(0xFF334155)
    ),
    NoteColorStyle(
        key = "indigo",
        name = "Indigo",
        lightBg = Color(0xFFEEF2FF),
        lightText = Color(0xFF312E81),
        darkBg = Color(0xFF1E1B4B),
        darkText = Color(0xFFE0E7FF),
        borderLight = Color(0xFFC7D2FE),
        borderDark = Color(0xFF3730A3)
    ),
    NoteColorStyle(
        key = "emerald",
        name = "Emerald",
        lightBg = Color(0xFFECFDF5),
        lightText = Color(0xFF064E3B),
        darkBg = Color(0xFF062B21),
        darkText = Color(0xFFA7F3D0),
        borderLight = Color(0xFFA7F3D0),
        borderDark = Color(0xFF065F46)
    ),
    NoteColorStyle(
        key = "amber",
        name = "Amber",
        lightBg = Color(0xFFFFFBEB),
        lightText = Color(0xFF78350F),
        darkBg = Color(0xFF332008),
        darkText = Color(0xFFFDE68A),
        borderLight = Color(0xFFFDE68A),
        borderDark = Color(0xFF92400E)
    ),
    NoteColorStyle(
        key = "sky",
        name = "Sky",
        lightBg = Color(0xFFF0F9FF),
        lightText = Color(0xFF0C4A6E),
        darkBg = Color(0xFF0C243C),
        darkText = Color(0xFFBAE6FD),
        borderLight = Color(0xFFBAE6FD),
        borderDark = Color(0xFF0369A1)
    ),
    NoteColorStyle(
        key = "rose",
        name = "Rose",
        lightBg = Color(0xFFFFF1F2),
        lightText = Color(0xFF881337),
        darkBg = Color(0xFF3B111A),
        darkText = Color(0xFFFECDD3),
        borderLight = Color(0xFFFECDD3),
        borderDark = Color(0xFF9F1239)
    ),
    NoteColorStyle(
        key = "violet",
        name = "Violet",
        lightBg = Color(0xFFF5F3FF),
        lightText = Color(0xFF4C1D95),
        darkBg = Color(0xFF281845),
        darkText = Color(0xFFDDD6FE),
        borderLight = Color(0xFFDDD6FE),
        borderDark = Color(0xFF5B21B6)
    )
)

fun getNoteColorStyle(key: String): NoteColorStyle {
    return NoteColorPalettes.find { it.key == key } ?: NoteColorPalettes[0]
}
