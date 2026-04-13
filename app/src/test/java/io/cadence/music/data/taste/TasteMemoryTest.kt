package io.cadence.music.data.taste

import io.cadence.music.data.model.UserTasteMemory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TasteMemoryTest {

    // ── buildTasteContextFrom ─────────────────────────────────────────────────

    @Test
    fun `returns empty string when feedback count is below minimum`() {
        val memory = UserTasteMemory(
            genreScores = mapOf("electronic" to 0.8f),
            feedbackCount = 2,
        )
        val result = TasteMemoryRepositoryImpl.buildTasteContextFrom(memory)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `returns context string when feedback count meets minimum`() {
        val memory = UserTasteMemory(
            genreScores = mapOf("electronic" to 0.8f),
            feedbackCount = 3,
        )
        val result = TasteMemoryRepositoryImpl.buildTasteContextFrom(memory)
        assertTrue(result.isNotEmpty())
        assertTrue(result.contains("electronic"))
    }

    @Test
    fun `preferred genres appear with positive sign`() {
        val memory = UserTasteMemory(
            genreScores = mapOf("jazz" to 0.6f, "rock" to 0.3f),
            feedbackCount = 5,
        )
        val result = TasteMemoryRepositoryImpl.buildTasteContextFrom(memory)
        assertTrue(result.contains("Preferred genres"))
        assertTrue(result.contains("jazz (+0.60)"))
        assertTrue(result.contains("rock (+0.30)"))
    }

    @Test
    fun `avoided genres appear under avoid section`() {
        val memory = UserTasteMemory(
            genreScores = mapOf("jazz" to -0.5f),
            feedbackCount = 4,
        )
        val result = TasteMemoryRepositoryImpl.buildTasteContextFrom(memory)
        assertTrue(result.contains("Avoid genres"))
        assertTrue(result.contains("jazz (-0.50)"))
    }

    @Test
    fun `scores below display threshold are omitted`() {
        val memory = UserTasteMemory(
            genreScores = mapOf("folk" to 0.10f),  // below 0.20 threshold
            feedbackCount = 5,
        )
        val result = TasteMemoryRepositoryImpl.buildTasteContextFrom(memory)
        assertFalse(result.contains("folk"))
    }

    @Test
    fun `non-genre tags appear under preferred and avoid tags sections`() {
        val memory = UserTasteMemory(
            tagScores = mapOf("energetic" to 0.75f, "melancholic" to -0.40f),
            feedbackCount = 4,
        )
        val result = TasteMemoryRepositoryImpl.buildTasteContextFrom(memory)
        assertTrue(result.contains("Preferred tags"))
        assertTrue(result.contains("energetic (+0.75)"))
        assertTrue(result.contains("Avoid tags"))
        assertTrue(result.contains("melancholic (-0.40)"))
    }

    @Test
    fun `genre tags are not duplicated in preferred tags section`() {
        val memory = UserTasteMemory(
            genreScores = mapOf("electronic" to 0.8f),
            tagScores   = mapOf("electronic" to 0.8f, "energetic" to 0.7f),
            feedbackCount = 4,
        )
        val result = TasteMemoryRepositoryImpl.buildTasteContextFrom(memory)
        // "electronic" should appear in Preferred genres but NOT in Preferred tags
        val genresLine  = result.lines().firstOrNull { it.contains("Preferred genres") } ?: ""
        val tagsLine    = result.lines().firstOrNull { it.contains("Preferred tags") } ?: ""
        assertTrue(genresLine.contains("electronic"))
        assertFalse(tagsLine.contains("electronic"))
        assertTrue(tagsLine.contains("energetic"))
    }

    @Test
    fun `scene context entries appear when meaningful`() {
        val memory = UserTasteMemory(
            contextGenreScores = mapOf("RUNNING:electronic" to 0.9f),
            feedbackCount = 4,
        )
        val result = TasteMemoryRepositoryImpl.buildTasteContextFrom(memory)
        assertTrue(result.contains("Scene context"))
        assertTrue(result.contains("RUNNING:electronic"))
    }

    @Test
    fun `honour disclaimer always appears in non-empty context`() {
        val memory = UserTasteMemory(
            genreScores = mapOf("pop" to 0.5f),
            feedbackCount = 5,
        )
        val result = TasteMemoryRepositoryImpl.buildTasteContextFrom(memory)
        assertTrue(result.contains("Honour these unless overridden"))
    }

    // ── EMA constant sanity check ─────────────────────────────────────────────

    @Test
    fun `ALPHA constant is within expected learning range`() {
        assertTrue(TasteMemoryRepositoryImpl.ALPHA in 0.1f..0.5f)
    }

    @Test
    fun `repeated positive signals drive score toward 1`() {
        var score = 0f
        repeat(20) { score = score * (1f - TasteMemoryRepositoryImpl.ALPHA) + 1f * TasteMemoryRepositoryImpl.ALPHA }
        assertTrue("Expected score near 1 after many positive signals, got $score", score > 0.95f)
    }

    @Test
    fun `repeated negative signals drive score toward -1`() {
        var score = 0f
        repeat(20) { score = score * (1f - TasteMemoryRepositoryImpl.ALPHA) + (-1f) * TasteMemoryRepositoryImpl.ALPHA }
        assertTrue("Expected score near -1 after many negative signals, got $score", score < -0.95f)
    }

    @Test
    fun `mixed signals converge near zero`() {
        var score = 0f
        repeat(30) { i ->
            val signal = if (i % 2 == 0) 1f else -1f
            score = score * (1f - TasteMemoryRepositoryImpl.ALPHA) + signal * TasteMemoryRepositoryImpl.ALPHA
        }
        assertEquals(0f, score, 0.15f)
    }
}
