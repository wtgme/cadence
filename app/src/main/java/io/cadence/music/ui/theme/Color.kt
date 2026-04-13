package io.cadence.music.ui.theme

import androidx.compose.ui.graphics.Color

// ── Scene accent tints (dark background washes) ─────────────────────────────
val SceneRunning    = Color(0xFF2E1A00)
val SceneWalking    = Color(0xFF0A2010)
val SceneCommuting  = Color(0xFF081525)
val SceneTraffic    = Color(0xFF250808)
val SceneResting    = Color(0xFF0D0820)
val SceneDefault    = Color(0xFF0A0A10)

// ── Glow / accent colors per scene ──────────────────────────────────────────
val GlowRunning     = Color(0xFFFF8C42)
val GlowWalking     = Color(0xFF56C96D)
val GlowCommuting   = Color(0xFF42A5F5)
val GlowTraffic     = Color(0xFFEF5350)
val GlowResting     = Color(0xFF9575CD)
val GlowDefault     = Color(0xFF4CAF50)

// ── Surface hierarchy ────────────────────────────────────────────────────────
val Surface0    = Color(0xFF080810)   // deepest background
val Surface1    = Color(0xFF111118)   // cards / sheets
val Surface2    = Color(0xFF1A1A26)   // elevated cards
val SurfaceBorder = Color(0x1AFFFFFF) // subtle 10% white border

// ── Text tokens ─────────────────────────────────────────────────────────────
val TextPrimary   = Color(0xFFEEEEF5)
val TextSecondary = Color(0x99EEEEF5) // 60% alpha
val TextTertiary  = Color(0x4DEEEEF5) // 30% alpha

// ── Feedback ────────────────────────────────────────────────────────────────
val FeedbackLike    = Color(0xFF56C96D)
val FeedbackDislike = Color(0xFFEF5350)
val FeedbackNeutral = Color(0x66EEEEF5)

// ── Static palette ───────────────────────────────────────────────────────────
val ErrorRed     = Color(0xFFCF6679)
val WarningAmber = Color(0xFFFFA726)
