package io.cadence.music.ui.theme

import androidx.compose.ui.graphics.Color

// ── Brand palette (Cadence Signup Flow design) ───────────────────────────────
val CadenceBg        = Color(0xFF0B1220)
val CadenceSurface   = Color(0xFF111A2E)
val CadenceSurfaceHi = Color(0xFF16213A)
val CadenceBorder    = Color(0x14FFFFFF)   // ~8% white
val CadenceBorderHi  = Color(0x24FFFFFF)   // ~14% white

val CadenceText      = Color(0xFFF4F6FB)
val CadenceTextMute  = Color(0x9EF4F6FB)   // ~62%
val CadenceTextDim   = Color(0x66F4F6FB)   // ~40%

val CadenceBlue      = Color(0xFF4F8BFF)
val CadenceBlueDim   = Color(0x294F8BFF)   // ~16%
val CadenceBlueDimHi = Color(0x474F8BFF)   // ~28%
val CadenceBlueDeep  = Color(0xFF3870E6)

val CadenceOrange    = Color(0xFFFF8A3D)
val CadenceOrangeDim = Color(0x2EFF8A3D)   // ~18%
val CadenceOrangeDimHi = Color(0x52FF8A3D) // ~32%
val CadenceOrangeDeep = Color(0xFFE67630)

val CadenceRed       = Color(0xFFFF5C6B)

// ── Scene accent tints (dark background washes) ─────────────────────────────
val SceneRunning    = Color(0xFF2E1A00)
val SceneCycling    = Color(0xFF1A2200)
val SceneWalking    = Color(0xFF0A2010)
val SceneCommuting  = Color(0xFF081525)
val SceneWorkout    = Color(0xFF250A20)
val SceneFocus      = Color(0xFF0A1520)
val SceneParty      = Color(0xFF251008)
val SceneResting    = Color(0xFF0D0820)
val SceneDefault    = Color(0xFF0A0A10)

// ── Glow / accent colors per scene ──────────────────────────────────────────
val GlowRunning     = CadenceOrange
val GlowCycling     = Color(0xFFB2D732)
val GlowWalking     = Color(0xFF56C96D)
val GlowCommuting   = CadenceBlue
val GlowWorkout     = Color(0xFFE040FB)
val GlowFocus       = Color(0xFF26C6DA)
val GlowParty       = CadenceOrange
val GlowResting     = Color(0xFF9575CD)
val GlowDefault     = CadenceBlue

// ── Legacy surface hierarchy (mapped to brand surfaces) ──────────────────────
val Surface0    = CadenceBg
val Surface1    = CadenceSurface
val Surface2    = CadenceSurfaceHi
val SurfaceBorder = CadenceBorder

// ── Text tokens ─────────────────────────────────────────────────────────────
val TextPrimary   = CadenceText
val TextSecondary = CadenceTextMute
val TextTertiary  = CadenceTextDim

// ── Feedback ────────────────────────────────────────────────────────────────
val FeedbackLike    = Color(0xFF56C96D)
val FeedbackDislike = CadenceRed
val FeedbackNeutral = Color(0x66EEEEF5)

// ── Static palette ───────────────────────────────────────────────────────────
val ErrorRed     = CadenceRed
val WarningAmber = CadenceOrange
