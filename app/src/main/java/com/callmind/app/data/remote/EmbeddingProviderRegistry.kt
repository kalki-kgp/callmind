package com.callmind.app.data.remote

import com.callmind.app.data.local.preferences.UserPreferences
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves the [EmbeddingProvider] the user has selected (cloud vs on-device).
 * Both the embedding worker and search engine go through here so a single
 * preference drives indexing and querying consistently.
 */
@Singleton
class EmbeddingProviderRegistry @Inject constructor(
    private val geminiEmbeddingService: GeminiEmbeddingService,
    private val localEmbeddingService: LocalEmbeddingService,
    private val userPreferences: UserPreferences
) {
    suspend fun current(): EmbeddingProvider {
        return when (userPreferences.embeddingProvider.first()) {
            PROVIDER_LOCAL -> localEmbeddingService
            else -> geminiEmbeddingService
        }
    }

    companion object {
        const val PROVIDER_CLOUD = "cloud"
        const val PROVIDER_LOCAL = "local"
    }
}
