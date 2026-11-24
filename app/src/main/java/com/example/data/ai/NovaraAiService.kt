package com.example.data.ai

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class AiPostAssistResult(
    val caption: String,
    val category: String,
    val hashtags: List<String>,
    val safetyNote: String,
    val safeToPublish: Boolean
)

data class AiDiscoveryInsight(
    val summary: String,
    val recommendedTopics: List<String>,
    val suggestedQuery: String
)

class NovaraAiService {

    private val generalModel by lazy {
        Firebase.ai.generativeModel("gemini-3.8-flash")
    }

    private val postAssistSchema = Schema.obj(
        properties = mapOf(
            "caption" to Schema.string(description = "Polished, engaging social post caption under 400 characters"),
            "category" to Schema.enumeration(
                values = listOf("Tech", "Design", "Creator", "Music", "Education", "Lifestyle"),
                description = "Best matching content category"
            ),
            "hashtags" to Schema.array(
                items = Schema.string(),
                description = "3 to 5 concise lowercase hashtags without # prefix"
            ),
            "safetyNote" to Schema.string(description = "Brief constructive tone/safety feedback"),
            "safeToPublish" to Schema.boolean(description = "True if free of spam, harassment, or harmful content")
        )
    )

    private val postAssistModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = postAssistSchema
            }
        )
    }

    private val discoverySchema = Schema.obj(
        properties = mapOf(
            "summary" to Schema.string(description = "Concise synthesis of what matches the user's discovery intent"),
            "recommendedTopics" to Schema.array(
                items = Schema.string(),
                description = "3 to 5 discovery tags or topics"
            ),
            "suggestedQuery" to Schema.string(description = "Refined keyword query for filtering posts or communities")
        )
    )

    private val discoveryModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = discoverySchema
            }
        )
    }

    suspend fun generatePostAssist(
        draftText: String,
        mediaTypeLabel: String,
        attachedBitmap: Bitmap? = null
    ): Result<AiPostAssistResult> {
        return try {
            val response = withTimeout(TIMEOUT_MS) {
                if (attachedBitmap != null) {
                    postAssistModel.generateContent(
                        content {
                            image(attachedBitmap)
                            text(
                                """
                                You are Novara's AI Co-Creator. Analyze the attached image and the user's draft ($mediaTypeLabel):
                                Draft: "$draftText"
                                Return a polished, authentic social caption, category, 3-5 hashtags (no #), and a quick safety/quality check.
                                """.trimIndent()
                            )
                        }
                    )
                } else {
                    postAssistModel.generateContent(
                        """
                        You are Novara's AI Co-Creator. Improve this $mediaTypeLabel draft while preserving the creator's authentic voice:
                        Draft: "$draftText"
                        Return a polished caption, best category, 3-5 hashtags (no #), and a quick safety/quality check.
                        """.trimIndent()
                    )
                }
            }
            val jsonString = response.text ?: throw IllegalStateException("Empty AI response.")
            val json = JSONObject(jsonString)
            val tagsArray = json.getJSONArray("hashtags")
            val tags = (0 until tagsArray.length()).map { tagsArray.getString(it).trim().removePrefix("#") }
            Result.success(
                AiPostAssistResult(
                    caption = json.getString("caption"),
                    category = json.optString("category", "Tech"),
                    hashtags = tags.filter { it.isNotBlank() }.take(5),
                    safetyNote = json.optString("safetyNote", "Looks clear and constructive."),
                    safeToPublish = json.optBoolean("safeToPublish", true)
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI post assist timed out", e)
            Result.failure(IllegalStateException("Novara AI timed out after 60s. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI post assist failed", e)
            Result.failure(e)
        }
    }

    suspend fun translateText(text: String, targetLanguage: String): Result<String> {
        return try {
            val response = withTimeout(TIMEOUT_MS) {
                generalModel.generateContent(
                    """
                    Translate the following social post or chat message naturally into $targetLanguage.
                    Preserve tone, emojis, and meaning. Output ONLY the translated text with no extra preamble.
                    Text: "$text"
                    """.trimIndent()
                )
            }
            val translated = response.text?.trim()
            if (translated.isNullOrBlank()) {
                Result.failure(IllegalStateException("Translation returned empty output."))
            } else {
                Result.success(translated)
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI translation timed out", e)
            Result.failure(IllegalStateException("Translation timed out. Please retry."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI translation failed", e)
            Result.failure(e)
        }
    }

    suspend fun analyzeDiscoveryIntent(
        query: String,
        availableCategories: List<String>
    ): Result<AiDiscoveryInsight> {
        return try {
            val response = withTimeout(TIMEOUT_MS) {
                discoveryModel.generateContent(
                    """
                    You are Novara's Semantic Discovery Engine.
                    User search query: "$query"
                    Platform categories: ${availableCategories.joinToString(", ")}
                    Provide a concise 1-2 sentence insight, 3-4 topic keywords, and a concise single-word or short keyword filter.
                    """.trimIndent()
                )
            }
            val json = JSONObject(response.text ?: throw IllegalStateException("Empty discovery response"))
            val topicsArr = json.getJSONArray("recommendedTopics")
            val topics = (0 until topicsArr.length()).map { topicsArr.getString(it) }
            Result.success(
                AiDiscoveryInsight(
                    summary = json.getString("summary"),
                    recommendedTopics = topics,
                    suggestedQuery = json.optString("suggestedQuery", query)
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI discovery timed out", e)
            Result.failure(IllegalStateException("Discovery insight timed out."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI discovery failed", e)
            Result.failure(e)
        }
    }

    fun streamCreatorCoach(
        creatorName: String,
        postsCount: Int,
        totalLikes: Long,
        followersCount: Long,
        tipsEarnedCents: Long,
        question: String
    ): Flow<String> = flow {
        val prompt = """
            You are Novara's AI Creator Coach advising $creatorName.
            Real creator metrics:
            - Published posts: $postsCount
            - Total likes received: $totalLikes
            - Followers: $followersCount
            - Tips & gifts earned: $${String.format("%.2f", tipsEarnedCents / 100.0)} (95% creator share)
            
            Creator's question: "$question"
            Give practical, concise, actionable growth & monetization coaching in 3 short bullet points.
        """.trimIndent()

        val accumulated = StringBuilder()
        withTimeout(TIMEOUT_MS) {
            generalModel.generateContentStream(prompt).collect { chunk ->
                val part = chunk.text.orEmpty()
                if (part.isNotEmpty()) {
                    accumulated.append(part)
                    emit(accumulated.toString())
                }
            }
        }
        if (accumulated.isBlank()) {
            throw IllegalStateException("AI Creator Coach returned an empty response.")
        }
    }

    private companion object {
        const val TAG = "NovaraAiService"
        const val TIMEOUT_MS = 60_000L
    }
}
