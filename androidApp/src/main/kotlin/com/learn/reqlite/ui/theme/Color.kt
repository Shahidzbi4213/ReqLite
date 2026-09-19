package com.learn.reqlite.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

// Primary & Accent Palettes
val PrimaryLight = Color(0xFF006399)
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFCEE5FF)
val OnPrimaryContainerLight = Color(0xFF001D32)

val SecondaryLight = Color(0xFF51606F)
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFD4E4F6)
val OnSecondaryContainerLight = Color(0xFF0D1D2A)

val TertiaryLight = Color(0xFF68587A)
val OnTertiaryLight = Color(0xFFFFFFFF)
val TertiaryContainerLight = Color(0xFFEFDCFF)
val OnTertiaryContainerLight = Color(0xFF231533)

val BackgroundLight = Color(0xFFF8F9FF)
val OnBackgroundLight = Color(0xFF191C20)
val SurfaceLight = Color(0xFFF8F9FF)
val OnSurfaceLight = Color(0xFF191C20)
val SurfaceVariantLight = Color(0xFFDEE3EB)
val OnSurfaceVariantLight = Color(0xFF42474E)
val OutlineLight = Color(0xFF72777F)
val OutlineVariantLight = Color(0xFFC2C7CF)

// Dark Palette
val PrimaryDark = Color(0xFF97CBFF)
val OnPrimaryDark = Color(0xFF003353)
val PrimaryContainerDark = Color(0xFF004A75)
val OnPrimaryContainerDark = Color(0xFFCEE5FF)

val SecondaryDark = Color(0xFFB8C8DA)
val OnSecondaryDark = Color(0xFF233240)
val SecondaryContainerDark = Color(0xFF3A4857)
val OnSecondaryContainerDark = Color(0xFFD4E4F6)

val TertiaryDark = Color(0xFFD3BBEC)
val OnTertiaryDark = Color(0xFF392A49)
val TertiaryContainerDark = Color(0xFF504061)
val OnTertiaryContainerDark = Color(0xFFEFDCFF)

val BackgroundDark = Color(0xFF111418)
val OnBackgroundDark = Color(0xFFE1E2E8)
val SurfaceDark = Color(0xFF111418)
val OnSurfaceDark = Color(0xFFE1E2E8)
val SurfaceVariantDark = Color(0xFF42474E)
val OnSurfaceVariantDark = Color(0xFFC2C7CF)
val OutlineDark = Color(0xFF8C9199)
val OutlineVariantDark = Color(0xFF42474E)

// Method Color Palette (Semantic REST Verbs)
data class MethodColorSet(
    val container: Color,
    val onContainer: Color,
    val border: Color
)

object MethodColors {
    val Get = MethodColorSet(
        container = Color(0xFF064E3B),
        onContainer = Color(0xFF6EE7B7),
        border = Color(0xFF10B981)
    )
    val GetLight = MethodColorSet(
        container = Color(0xFFD1FAE5),
        onContainer = Color(0xFF065F46),
        border = Color(0xFF34D399)
    )

    val Post = MethodColorSet(
        container = Color(0xFF1E3A8A),
        onContainer = Color(0xFF93C5FD),
        border = Color(0xFF3B82F6)
    )
    val PostLight = MethodColorSet(
        container = Color(0xFFDBEAFE),
        onContainer = Color(0xFF1E40AF),
        border = Color(0xFF60A5FA)
    )

    val Put = MethodColorSet(
        container = Color(0xFF78350F),
        onContainer = Color(0xFFFDE68A),
        border = Color(0xFFF59E0B)
    )
    val PutLight = MethodColorSet(
        container = Color(0xFFFEF3C7),
        onContainer = Color(0xFF92400E),
        border = Color(0xFFFBBF24)
    )

    val Delete = MethodColorSet(
        container = Color(0xFF7F1D1D),
        onContainer = Color(0xFFFECACA),
        border = Color(0xFFEF4444)
    )
    val DeleteLight = MethodColorSet(
        container = Color(0xFFFEE2E2),
        onContainer = Color(0xFF991B1B),
        border = Color(0xFFF87171)
    )

    val Patch = MethodColorSet(
        container = Color(0xFF581C87),
        onContainer = Color(0xFFE9D5FF),
        border = Color(0xFFA855F7)
    )
    val PatchLight = MethodColorSet(
        container = Color(0xFFF3E8FF),
        onContainer = Color(0xFF6B21A8),
        border = Color(0xFFC084FC)
    )

    val Default = MethodColorSet(
        container = Color(0xFF374151),
        onContainer = Color(0xFFE5E7EB),
        border = Color(0xFF9CA3AF)
    )
    val DefaultLight = MethodColorSet(
        container = Color(0xFFF3F4F6),
        onContainer = Color(0xFF374151),
        border = Color(0xFFD1D5DB)
    )

    fun forMethod(method: String, isDark: Boolean): MethodColorSet {
        return when (method.uppercase()) {
            "GET" -> if (isDark) Get else GetLight
            "POST" -> if (isDark) Post else PostLight
            "PUT" -> if (isDark) Put else PutLight
            "DELETE" -> if (isDark) Delete else DeleteLight
            "PATCH" -> if (isDark) Patch else PatchLight
            else -> if (isDark) Default else DefaultLight
        }
    }
}
