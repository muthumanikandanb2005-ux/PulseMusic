package org.simpmusic.aiservice

import com.maxrave.domain.data.model.metadata.Lyrics

class AiClient {
    private var aiService: AiService? = null
    var host = AIHost.GEMINI
        set(value) {
            field = value
            rebuildAiService()
        }
    var apiKey: String? = null
        set(value) {
            field = value
            rebuildAiService()
        }
    var customModelId: String? = null
        set(value) {
            field = value
            rebuildAiService()
        }
    var customBaseUrl: String? = null
        set(value) {
            field = value
            rebuildAiService()
        }
    var customHeaders: Map<String, String>? = null
        set(value) {
            field = value
            rebuildAiService()
        }

    companion object {
        const val DEFAULT_GEMINI_API_KEY = ""
    }

    private fun rebuildAiService() {
        val effectiveKey = apiKey?.ifEmpty { null } ?: DEFAULT_GEMINI_API_KEY
        aiService =
            AiService(
                aiHost = host,
                apiKey = effectiveKey,
                customModelId = customModelId,
                customBaseUrl = customBaseUrl,
                customHeaders = customHeaders,
            )
    }

    suspend fun translateLyrics(
        inputLyrics: Lyrics,
        targetLanguage: String,
    ): Result<Lyrics> =
        runCatching {
            val result =
                aiService?.translateLyrics(inputLyrics, targetLanguage)
                    ?: throw IllegalStateException("AI service is not initialized. Please set host and apiKey.")

            // Validate: check that at least some lines were actually translated
            val originalWords = inputLyrics.lines?.map { it.words } ?: emptyList()
            val translatedWords = result.lines?.map { it.words } ?: emptyList()
            val unchangedCount = originalWords.zip(translatedWords).count { (orig, trans) -> orig == trans }
            val translatableCount = originalWords.count { it.trim().isNotEmpty() && it.trim() != "♫" }

            // Reject if >80% of translatable lines are unchanged (likely same language or translation failed)
            if (translatableCount > 0 && unchangedCount.toFloat() / translatableCount > 0.8f) {
                throw IllegalStateException("Translation failed or returned empty lyrics or same language.")
            }

            result
        }
}