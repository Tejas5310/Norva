package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

sealed interface AiUiState {
    data object Idle : AiUiState
    data object Loading : AiUiState
    data class Streaming(val text: String) : AiUiState
    data class Success(val text: String) : AiUiState
    data class Error(val message: String) : AiUiState
}

enum class FeedFilter(val label: String) {
    FOR_YOU("For You"),
    FOLLOWING("Following"),
    SHORT_VIDEO("Shorts"),
    LONG_VIDEO("Videos"),
    TRENDING("Trending")
}

enum class PostMediaType(val label: String) {
    TEXT("Text"),
    PHOTO("Photo"),
    SHORT_VIDEO("Short Video"),
    LONG_VIDEO("Long Video"),
    POLL("Poll")
}

enum class PostVisibility(val label: String) {
    PUBLIC("Public"),
    COMMUNITY("Community"),
    SUBSCRIBERS("Subscribers")
}

enum class CreatorTier(val label: String) {
    STANDARD("Member"),
    CREATOR("Creator"),
    VERIFIED_PRO("Verified Pro")
}

enum class GiftType(val label: String, val amountCents: Int) {
    SPARK("Spark", 100),
    DIAMOND("Diamond", 500),
    CROWN("Crown", 2500),
    SUBSCRIPTION("Monthly Pass", 999)
}

@Keep
@IgnoreExtraProperties
data class PrivateUser(
    @DocumentId val docId: String = "",
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val preferredLanguage: String = "English",
    val themeMode: String = "DARK",
    val feedDiversity: Long = 55L,
    val privateAccount: Boolean = false,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SavedPost(
    @DocumentId val docId: String = "",
    val postId: String = "",
    val authorName: String = "",
    val previewText: String = "",
    val mediaType: String = "TEXT",
    val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PublicProfile(
    @DocumentId val docId: String = "",
    val userId: String = "",
    val handle: String = "",
    val displayName: String = "",
    val bio: String = "",
    val avatarInitials: String = "NV",
    val accentColorHex: String = "#00E5FF",
    val interests: List<String> = emptyList(),
    val creatorTier: String = "STANDARD",
    val verifiedBadge: Boolean = false,
    val discoverable: Boolean = true,
    val subscriptionPriceUsd: Double = 4.99,
    val followersCount: Long = 0L,
    val followingCount: Long = 0L,
    val tipsEarnedCents: Long = 0L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FollowRelation(
    @DocumentId val id: String = "",
    val followerId: String = "",
    val targetUserId: String = "",
    val targetDisplayName: String = "",
    val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Post(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorHandle: String = "",
    val authorVerified: Boolean = false,
    val visibility: String = "PUBLIC",
    val mediaType: String = "TEXT",
    val content: String = "",
    val mediaUrl: String? = null,
    val videoDurationSec: Long = 0L,
    val soundTrackTitle: String? = null,
    val communityId: String? = null,
    val category: String = "Tech",
    val tags: List<String> = emptyList(),
    val pollOptions: List<String> = emptyList(),
    val pollVotes: List<Long> = emptyList(),
    val collaboratorHandle: String? = null,
    val aiGenerated: Boolean = false,
    val likesCount: Long = 0L,
    val commentsCount: Long = 0L,
    val repostsCount: Long = 0L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        val raw: Map<String, Any?> = mapOf(
            "authorId" to authorId,
            "authorName" to authorName,
            "authorHandle" to authorHandle,
            "authorVerified" to authorVerified,
            "visibility" to visibility,
            "mediaType" to mediaType,
            "content" to content,
            "mediaUrl" to mediaUrl?.takeIf { it.isNotBlank() },
            "videoDurationSec" to videoDurationSec,
            "soundTrackTitle" to soundTrackTitle?.takeIf { it.isNotBlank() },
            "communityId" to communityId?.takeIf { it.isNotBlank() },
            "category" to category,
            "tags" to tags.take(10),
            "pollOptions" to pollOptions.take(4),
            "pollVotes" to pollVotes.take(4),
            "collaboratorHandle" to collaboratorHandle?.takeIf { it.isNotBlank() },
            "aiGenerated" to aiGenerated,
            "likesCount" to 0L,
            "commentsCount" to 0L,
            "repostsCount" to 0L,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

@Keep
@IgnoreExtraProperties
data class PostComment(
    @DocumentId val id: String = "",
    val postId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorHandle: String = "",
    val text: String = "",
    val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Story(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorHandle: String = "",
    val caption: String = "",
    val moodTag: String = "Pulse",
    val visibility: String = "PUBLIC",
    val livePulse: Boolean = false,
    val gradientIndex: Long = 0L,
    val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Community(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val name: String = "",
    val description: String = "",
    val category: String = "Tech",
    val visibility: String = "PUBLIC",
    val memberUids: List<String> = emptyList(),
    val membersCount: Long = 1L,
    val activeRoomTopic: String? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CommunityDiscussion(
    @DocumentId val id: String = "",
    val communityId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val title: String = "",
    val body: String = "",
    val pollQuestion: String? = null,
    val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Conversation(
    @DocumentId val id: String = "",
    val title: String = "",
    val groupChat: Boolean = false,
    val memberUids: List<String> = emptyList(),
    val memberNames: List<String> = emptyList(),
    val lastMessageText: String = "",
    val lastSenderId: String = "",
    val disappearingEnabled: Boolean = false,
    val encryptedBadge: Boolean = true,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val replyToText: String? = null,
    val reactions: List<String> = emptyList(),
    val memberUids: List<String> = emptyList(),
    val editedFlag: Boolean = false,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CreatorTip(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val recipientId: String = "",
    val recipientName: String = "",
    val giftType: String = "SPARK",
    val amountCents: Long = 100L,
    val creatorNetCents: Long = 95L,
    val platformFeeCents: Long = 5L,
    val note: String = "",
    val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ModerationReport(
    @DocumentId val id: String = "",
    val reporterId: String = "",
    val targetType: String = "POST",
    val targetId: String = "",
    val reason: String = "",
    val status: String = "OPEN",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)
