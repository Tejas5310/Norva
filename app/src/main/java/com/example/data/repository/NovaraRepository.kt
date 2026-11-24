package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.data.model.ChatMessage
import com.example.data.model.Community
import com.example.data.model.CommunityDiscussion
import com.example.data.model.Conversation
import com.example.data.model.CreatorTip
import com.example.data.model.FollowRelation
import com.example.data.model.GiftType
import com.example.data.model.ModerationReport
import com.example.data.model.Post
import com.example.data.model.PostComment
import com.example.data.model.PrivateUser
import com.example.data.model.PublicProfile
import com.example.data.model.SavedPost
import com.example.data.model.Story
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

class NovaraRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("Google Sign-In required before accessing Novara.")

    private fun <T> Flow<T>.handleListenerErrors(uid: String, path: String): Flow<T> =
        this.catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val exceptionToLog = firestoreError ?: (error as? Exception)
            if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                handleFirestoreError(exceptionToLog, OperationType.LIST, path)
            }
            throw error
        }

    suspend fun ensureUserProvisioned(user: FirebaseUser): Result<Unit> {
        val uid = user.uid
        val privateRef = db.collection("users").document(uid)
        val publicRef = db.collection("publicProfiles").document(uid)
        return try {
            val privateSnap = privateRef.get().await()
            val displayName = (user.displayName ?: "Novara Creator").take(60).ifBlank { "Novara Creator" }
            if (!privateSnap.exists()) {
                val privatePayload = mapOf(
                    "userId" to uid,
                    "email" to (user.email ?: ""),
                    "displayName" to displayName,
                    "preferredLanguage" to "English",
                    "themeMode" to "DARK",
                    "feedDiversity" to 55L,
                    "privateAccount" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                privateRef.set(privatePayload).await()
            }

            val publicSnap = publicRef.get().await()
            if (!publicSnap.exists()) {
                val baseHandle = displayName.lowercase()
                    .replace(Regex("[^a-z0-9_]"), "")
                    .take(18)
                    .ifBlank { "novara" }
                val handle = "${baseHandle}_${uid.take(4)}".take(32)
                val initials = displayName.split(" ")
                    .filter { it.isNotBlank() }
                    .take(2)
                    .joinToString("") { it.first().uppercaseChar().toString() }
                    .ifBlank { "NV" }
                val publicPayload = mapOf(
                    "userId" to uid,
                    "handle" to handle,
                    "displayName" to displayName,
                    "bio" to "Exploring next-gen AI social creativity on Novara.",
                    "avatarInitials" to initials,
                    "accentColorHex" to "#00E5FF",
                    "interests" to listOf("Tech", "Design", "Creators"),
                    "creatorTier" to "CREATOR",
                    "verifiedBadge" to false,
                    "discoverable" to true,
                    "subscriptionPriceUsd" to 4.99,
                    "followersCount" to 0L,
                    "followingCount" to 0L,
                    "tipsEarnedCents" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                publicRef.set(publicPayload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$uid")
            Result.failure(e)
        }
    }

    fun observePrivateUser(uid: String): Flow<PrivateUser?> = flow {
        require(uid.isNotBlank()) { "User ID must not be blank" }
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { it.toObject(PrivateUser::class.java, ServerTimestampBehavior.ESTIMATE) }
                .handleListenerErrors(uid, "users/$uid")
        )
    }

    fun observeMyProfile(uid: String): Flow<PublicProfile?> = flow {
        require(uid.isNotBlank()) { "User ID must not be blank" }
        emitAll(
            db.collection("publicProfiles").document(uid)
                .snapshots()
                .map { it.toObject(PublicProfile::class.java, ServerTimestampBehavior.ESTIMATE) }
                .handleListenerErrors(uid, "publicProfiles/$uid")
        )
    }

    fun observePublicProfiles(uid: String): Flow<List<PublicProfile>> = flow {
        require(uid.isNotBlank()) { "User ID must not be blank" }
        emitAll(
            db.collection("publicProfiles")
                .whereEqualTo("discoverable", true)
                .limit(50)
                .snapshots()
                .map { it.toObjects(PublicProfile::class.java, ServerTimestampBehavior.ESTIMATE) }
                .handleListenerErrors(uid, "publicProfiles")
        )
    }

    fun observePublicPosts(uid: String): Flow<List<Post>> = flow {
        require(uid.isNotBlank()) { "User ID must not be blank" }
        emitAll(
            db.collection("posts")
                .whereEqualTo("visibility", "PUBLIC")
                .limit(60)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Post::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .handleListenerErrors(uid, "posts")
        )
    }

    fun observeStories(uid: String): Flow<List<Story>> = flow {
        require(uid.isNotBlank()) { "User ID must not be blank" }
        emitAll(
            db.collection("stories")
                .whereEqualTo("visibility", "PUBLIC")
                .limit(30)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Story::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .handleListenerErrors(uid, "stories")
        )
    }

    fun observeSavedPosts(uid: String): Flow<List<SavedPost>> = flow {
        require(uid.isNotBlank()) { "User ID must not be blank" }
        emitAll(
            db.collection("users").document(uid).collection("savedPosts")
                .snapshots()
                .map { snap ->
                    snap.toObjects(SavedPost::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .handleListenerErrors(uid, "users/$uid/savedPosts")
        )
    }

    fun observeFollows(uid: String): Flow<List<FollowRelation>> = flow {
        require(uid.isNotBlank()) { "User ID must not be blank" }
        emitAll(
            db.collection("follows")
                .whereEqualTo("followerId", uid)
                .snapshots()
                .map { it.toObjects(FollowRelation::class.java, ServerTimestampBehavior.ESTIMATE) }
                .handleListenerErrors(uid, "follows")
        )
    }

    fun observePostComments(uid: String, postId: String): Flow<List<PostComment>> = flow {
        require(uid.isNotBlank() && postId.isNotBlank()) { "IDs must not be blank" }
        emitAll(
            db.collection("posts").document(postId).collection("comments")
                .whereEqualTo("postId", postId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(PostComment::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt?.seconds ?: 0L }
                }
                .handleListenerErrors(uid, "posts/$postId/comments")
        )
    }

    fun observeCommunities(uid: String): Flow<List<Community>> = flow {
        require(uid.isNotBlank()) { "User ID must not be blank" }
        emitAll(
            db.collection("communities")
                .whereEqualTo("visibility", "PUBLIC")
                .limit(40)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Community::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.membersCount }
                }
                .handleListenerErrors(uid, "communities")
        )
    }

    fun observeCommunityDiscussions(uid: String, communityId: String): Flow<List<CommunityDiscussion>> = flow {
        require(uid.isNotBlank() && communityId.isNotBlank()) { "IDs must not be blank" }
        emitAll(
            db.collection("communities").document(communityId).collection("discussions")
                .whereEqualTo("communityId", communityId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(CommunityDiscussion::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .handleListenerErrors(uid, "communities/$communityId/discussions")
        )
    }

    fun observeConversations(uid: String): Flow<List<Conversation>> = flow {
        require(uid.isNotBlank()) { "User ID must not be blank" }
        emitAll(
            db.collection("conversations")
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Conversation::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt?.seconds ?: 0L }
                }
                .handleListenerErrors(uid, "conversations")
        )
    }

    fun observeMessages(uid: String, conversationId: String): Flow<List<ChatMessage>> = flow {
        require(uid.isNotBlank() && conversationId.isNotBlank()) { "IDs must not be blank" }
        emitAll(
            db.collection("conversations").document(conversationId).collection("messages")
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(ChatMessage::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt?.seconds ?: Long.MAX_VALUE }
                }
                .handleListenerErrors(uid, "conversations/$conversationId/messages")
        )
    }

    fun observeReceivedTips(uid: String): Flow<List<CreatorTip>> = flow {
        require(uid.isNotBlank()) { "User ID must not be blank" }
        emitAll(
            db.collection("tips")
                .whereEqualTo("recipientId", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(CreatorTip::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .handleListenerErrors(uid, "tips")
        )
    }

    fun observeReports(uid: String, isAdminUser: Boolean): Flow<List<ModerationReport>> = flow {
        require(uid.isNotBlank()) { "User ID must not be blank" }
        val query: Query = if (isAdminUser) {
            db.collection("reports").limit(50)
        } else {
            db.collection("reports").whereEqualTo("reporterId", uid).limit(25)
        }
        emitAll(
            query.snapshots()
                .map { snap ->
                    snap.toObjects(ModerationReport::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .handleListenerErrors(uid, "reports")
        )
    }

    // --- WRITE MUTATIONS ---

    suspend fun createPost(
        authorProfile: PublicProfile,
        content: String,
        mediaType: String,
        visibility: String,
        category: String,
        tags: List<String>,
        mediaUrl: String?,
        videoDurationSec: Long,
        soundTrackTitle: String?,
        pollOptions: List<String>,
        collaboratorHandle: String?,
        aiGenerated: Boolean
    ): Result<Unit> {
        var path = "posts"
        return try {
            val uid = requireUserId()
            val trimmed = content.trim()
            require(trimmed.length in 1..2000) { "Post content must be between 1 and 2000 characters." }

            val cleanOptions = pollOptions.map { it.trim() }.filter { it.isNotBlank() }.take(4)
            val initialVotes = List(cleanOptions.size) { 0L }

            val post = Post(
                authorId = uid,
                authorName = authorProfile.displayName.ifBlank { "Creator" }.take(60),
                authorHandle = authorProfile.handle.ifBlank { "creator" }.take(32),
                authorVerified = authorProfile.verifiedBadge,
                visibility = visibility,
                mediaType = mediaType,
                content = trimmed,
                mediaUrl = mediaUrl?.take(1000),
                videoDurationSec = videoDurationSec.coerceIn(0L, 36000L),
                soundTrackTitle = soundTrackTitle?.take(120),
                category = category.ifBlank { "Tech" }.take(40),
                tags = tags.map { it.trim().removePrefix("#") }.filter { it.isNotBlank() }.take(10),
                pollOptions = cleanOptions,
                pollVotes = initialVotes,
                collaboratorHandle = collaboratorHandle?.trim()?.removePrefix("@")?.take(32),
                aiGenerated = aiGenerated
            )
            val docRef = db.collection("posts").document()
            path = docRef.path
            docRef.set(post.toCreateMap()).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleLike(post: Post): Result<Boolean> {
        val postId = post.id
        var path = "posts/$postId/likes"
        return try {
            val uid = requireUserId()
            require(postId.isNotBlank()) { "Post ID missing" }
            val likeRef = db.collection("posts").document(postId).collection("likes").document(uid)
            val postRef = db.collection("posts").document(postId)
            path = likeRef.path

            val existingLike = likeRef.get().await()
            val postSnap = postRef.get().await()
            val currentLikes = postSnap.getLong("likesCount") ?: 0L
            val batch = db.batch()
            val nowLiked = !existingLike.exists()
            if (nowLiked) {
                batch.set(
                    likeRef,
                    mapOf("userId" to uid, "createdAt" to FieldValue.serverTimestamp())
                )
                batch.update(
                    postRef,
                    mapOf(
                        "likesCount" to (currentLikes + 1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.delete(likeRef)
                val nextCount = (currentLikes - 1L).coerceAtLeast(0L)
                batch.update(
                    postRef,
                    mapOf(
                        "likesCount" to nextCount,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(nowLiked)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun repost(postId: String): Result<Unit> {
        val path = "posts/$postId"
        return try {
            requireUserId()
            require(postId.isNotBlank()) { "Post ID missing" }
            db.collection("posts").document(postId).update(
                mapOf(
                    "repostsCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun votePoll(post: Post, optionIndex: Int): Result<Unit> {
        val path = "posts/${post.id}"
        return try {
            requireUserId()
            require(post.id.isNotBlank()) { "Post ID missing" }
            require(optionIndex in post.pollOptions.indices) { "Invalid poll option" }
            val updatedVotes = post.pollOptions.indices.map { idx ->
                val current = post.pollVotes.getOrNull(idx) ?: 0L
                if (idx == optionIndex) current + 1L else current
            }
            db.collection("posts").document(post.id).update(
                mapOf(
                    "pollVotes" to updatedVotes,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun addComment(postId: String, profile: PublicProfile, text: String): Result<Unit> {
        var path = "posts/$postId/comments"
        return try {
            val uid = requireUserId()
            val cleanText = text.trim()
            require(postId.isNotBlank() && cleanText.length in 1..500) {
                "Comment must be between 1 and 500 characters."
            }
            val commentRef = db.collection("posts").document(postId).collection("comments").document()
            val postRef = db.collection("posts").document(postId)
            path = commentRef.path

            val batch = db.batch()
            batch.set(
                commentRef,
                mapOf(
                    "postId" to postId,
                    "authorId" to uid,
                    "authorName" to profile.displayName.ifBlank { "Creator" }.take(60),
                    "authorHandle" to profile.handle.ifBlank { "creator" }.take(32),
                    "text" to cleanText,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                postRef,
                mapOf(
                    "commentsCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deletePost(postId: String): Result<Unit> {
        val path = "posts/$postId"
        return try {
            requireUserId()
            require(postId.isNotBlank())
            db.collection("posts").document(postId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleSavePost(post: Post, currentlySaved: Boolean): Result<Unit> {
        val uid = auth.currentUser?.uid ?: "unauthenticated"
        val path = "users/$uid/savedPosts/${post.id}"
        return try {
            val userId = requireUserId()
            require(post.id.isNotBlank())
            val ref = db.collection("users").document(userId).collection("savedPosts").document(post.id)
            if (currentlySaved) {
                ref.delete().await()
            } else {
                ref.set(
                    mapOf(
                        "postId" to post.id,
                        "authorName" to post.authorName.take(60),
                        "previewText" to post.content.take(300),
                        "mediaType" to post.mediaType,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun createStory(
        profile: PublicProfile,
        caption: String,
        moodTag: String,
        livePulse: Boolean,
        gradientIndex: Long
    ): Result<Unit> {
        var path = "stories"
        return try {
            val uid = requireUserId()
            val cleanCaption = caption.trim()
            require(cleanCaption.length in 1..240) { "Story caption must be 1..240 chars." }
            val docRef = db.collection("stories").document()
            path = docRef.path
            docRef.set(
                mapOf(
                    "authorId" to uid,
                    "authorName" to profile.displayName.ifBlank { "Creator" }.take(60),
                    "authorHandle" to profile.handle.ifBlank { "creator" }.take(32),
                    "caption" to cleanCaption,
                    "moodTag" to moodTag.ifBlank { "Pulse" }.take(30),
                    "visibility" to "PUBLIC",
                    "livePulse" to livePulse,
                    "gradientIndex" to gradientIndex.coerceIn(0L, 5L),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleFollow(
        targetProfile: PublicProfile,
        currentlyFollowing: Boolean
    ): Result<Unit> {
        var path = "follows"
        return try {
            val uid = requireUserId()
            require(targetProfile.userId.isNotBlank() && targetProfile.userId != uid) {
                "Cannot follow yourself."
            }
            val followId = "${uid}_${targetProfile.userId}"
            path = "follows/$followId"
            val followRef = db.collection("follows").document(followId)
            val targetRef = db.collection("publicProfiles").document(targetProfile.userId)
            val myRef = db.collection("publicProfiles").document(uid)

            val targetFollowers = targetRef.get().await().getLong("followersCount") ?: 0L
            val myFollowing = myRef.get().await().getLong("followingCount") ?: 0L

            val batch = db.batch()
            if (currentlyFollowing) {
                batch.delete(followRef)
                batch.update(
                    targetRef,
                    mapOf(
                        "followersCount" to (targetFollowers - 1L).coerceAtLeast(0L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.update(
                    myRef,
                    mapOf(
                        "followingCount" to (myFollowing - 1L).coerceAtLeast(0L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.set(
                    followRef,
                    mapOf(
                        "followerId" to uid,
                        "targetUserId" to targetProfile.userId,
                        "targetDisplayName" to targetProfile.displayName.take(60),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.update(
                    targetRef,
                    mapOf(
                        "followersCount" to (targetFollowers + 1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.update(
                    myRef,
                    mapOf(
                        "followingCount" to (myFollowing + 1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun createCommunity(
        name: String,
        description: String,
        category: String,
        activeRoomTopic: String?
    ): Result<Unit> {
        var path = "communities"
        return try {
            val uid = requireUserId()
            val cleanName = name.trim()
            val cleanDesc = description.trim()
            require(cleanName.length in 2..80 && cleanDesc.length in 1..400) {
                "Invalid community name or description."
            }
            val docRef = db.collection("communities").document()
            path = docRef.path
            val payload: Map<String, Any?> = mapOf(
                "ownerId" to uid,
                "name" to cleanName,
                "description" to cleanDesc,
                "category" to category.ifBlank { "Tech" }.take(40),
                "visibility" to "PUBLIC",
                "memberUids" to listOf(uid),
                "membersCount" to 1L,
                "activeRoomTopic" to activeRoomTopic?.trim()?.take(120)?.takeIf { it.isNotBlank() },
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            @Suppress("UNCHECKED_CAST")
            docRef.set(payload.filterValues { it != null } as Map<String, Any>).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleJoinCommunity(community: Community): Result<Unit> {
        val path = "communities/${community.id}"
        return try {
            val uid = requireUserId()
            require(community.id.isNotBlank())
            val isMember = uid in community.memberUids
            val nextMembers = if (isMember) {
                community.memberUids.filterNot { it == uid }
            } else {
                (community.memberUids + uid).distinct().take(100)
            }
            val nextCount = if (isMember) {
                (community.membersCount - 1L).coerceAtLeast(1L)
            } else {
                community.membersCount + 1L
            }
            db.collection("communities").document(community.id).update(
                mapOf(
                    "memberUids" to nextMembers,
                    "membersCount" to nextCount,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun postDiscussion(
        communityId: String,
        authorName: String,
        title: String,
        body: String,
        pollQuestion: String?
    ): Result<Unit> {
        var path = "communities/$communityId/discussions"
        return try {
            val uid = requireUserId()
            require(communityId.isNotBlank() && title.trim().length in 1..120 && body.trim().length in 1..1000) {
                "Discussion title and body are required."
            }
            val docRef = db.collection("communities").document(communityId)
                .collection("discussions").document()
            path = docRef.path
            val payload: Map<String, Any?> = mapOf(
                "communityId" to communityId,
                "authorId" to uid,
                "authorName" to authorName.ifBlank { "Member" }.take(60),
                "title" to title.trim(),
                "body" to body.trim(),
                "pollQuestion" to pollQuestion?.trim()?.take(200)?.takeIf { it.isNotBlank() },
                "createdAt" to FieldValue.serverTimestamp()
            )
            @Suppress("UNCHECKED_CAST")
            docRef.set(payload.filterValues { it != null } as Map<String, Any>).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun startConversation(
        myProfile: PublicProfile,
        otherProfile: PublicProfile
    ): Result<String> {
        var path = "conversations"
        return try {
            val uid = requireUserId()
            require(otherProfile.userId.isNotBlank() && otherProfile.userId != uid) {
                "Select another user to message."
            }
            val sortedIds = listOf(uid, otherProfile.userId).sorted()
            val convId = "dm_${sortedIds[0].take(20)}_${sortedIds[1].take(20)}"
            path = "conversations/$convId"
            val docRef = db.collection("conversations").document(convId)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                docRef.set(
                    mapOf(
                        "title" to "${myProfile.displayName.take(25)} & ${otherProfile.displayName.take(25)}",
                        "groupChat" to false,
                        "memberUids" to sortedIds,
                        "memberNames" to listOf(myProfile.displayName.take(30), otherProfile.displayName.take(30)),
                        "lastMessageText" to "End-to-end encrypted Novara channel opened",
                        "lastSenderId" to uid,
                        "disappearingEnabled" to false,
                        "encryptedBadge" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(convId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun sendMessage(
        conversation: Conversation,
        senderName: String,
        text: String,
        replyToText: String? = null
    ): Result<Unit> {
        var path = "conversations/${conversation.id}/messages"
        return try {
            val uid = requireUserId()
            val cleanText = text.trim()
            require(conversation.id.isNotBlank() && cleanText.length in 1..1000) {
                "Message must be 1..1000 characters."
            }
            val msgRef = db.collection("conversations").document(conversation.id)
                .collection("messages").document()
            val convRef = db.collection("conversations").document(conversation.id)
            path = msgRef.path

            val batch = db.batch()
            val msgPayload: Map<String, Any?> = mapOf(
                "conversationId" to conversation.id,
                "senderId" to uid,
                "senderName" to senderName.ifBlank { "User" }.take(60),
                "text" to cleanText,
                "replyToText" to replyToText?.take(300)?.takeIf { it.isNotBlank() },
                "reactions" to emptyList<String>(),
                "memberUids" to conversation.memberUids,
                "editedFlag" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            @Suppress("UNCHECKED_CAST")
            batch.set(msgRef, msgPayload.filterValues { it != null } as Map<String, Any>)
            batch.update(
                convRef,
                mapOf(
                    "lastMessageText" to cleanText.take(300),
                    "lastSenderId" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun editMessage(conversationId: String, messageId: String, newText: String): Result<Unit> {
        val path = "conversations/$conversationId/messages/$messageId"
        return try {
            requireUserId()
            val clean = newText.trim()
            require(clean.length in 1..1000)
            db.document(path).update(
                mapOf(
                    "text" to clean,
                    "editedFlag" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleMessageReaction(conversationId: String, message: ChatMessage, emoji: String): Result<Unit> {
        val path = "conversations/$conversationId/messages/${message.id}"
        return try {
            requireUserId()
            val updated = if (emoji in message.reactions) {
                message.reactions - emoji
            } else {
                (message.reactions + emoji).take(20)
            }
            db.document(path).update(
                mapOf(
                    "reactions" to updated,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleDisappearingMessages(conversation: Conversation): Result<Unit> {
        val path = "conversations/${conversation.id}"
        return try {
            requireUserId()
            db.document(path).update(
                mapOf(
                    "disappearingEnabled" to !conversation.disappearingEnabled,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun sendCreatorTip(
        senderName: String,
        recipient: PublicProfile,
        giftType: GiftType,
        note: String
    ): Result<Unit> {
        var path = "tips"
        return try {
            val uid = requireUserId()
            require(recipient.userId.isNotBlank() && recipient.userId != uid) {
                "Select another creator to support."
            }
            val amount = giftType.amountCents.toLong()
            val platformFee = (amount * 5L) / 100L
            val creatorNet = amount - platformFee

            val tipRef = db.collection("tips").document()
            val recipientRef = db.collection("publicProfiles").document(recipient.userId)
            path = tipRef.path

            val batch = db.batch()
            batch.set(
                tipRef,
                mapOf(
                    "senderId" to uid,
                    "senderName" to senderName.ifBlank { "Supporter" }.take(60),
                    "recipientId" to recipient.userId,
                    "recipientName" to recipient.displayName.ifBlank { "Creator" }.take(60),
                    "giftType" to giftType.name,
                    "amountCents" to amount,
                    "creatorNetCents" to creatorNet,
                    "platformFeeCents" to platformFee,
                    "note" to note.trim().take(200),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                recipientRef,
                mapOf(
                    "tipsEarnedCents" to FieldValue.increment(creatorNet),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateProfile(
        displayName: String,
        handle: String,
        bio: String,
        interests: List<String>,
        creatorTier: String,
        subscriptionPriceUsd: Double
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: "unauthenticated"
        val path = "publicProfiles/$uid"
        return try {
            val userId = requireUserId()
            val cleanName = displayName.trim().take(60).ifBlank { "Creator" }
            val cleanHandle = handle.trim().lowercase().replace(Regex("[^a-z0-9_]"), "").take(32)
            require(cleanHandle.length >= 2) { "Handle must be at least 2 alphanumeric characters." }
            val initials = cleanName.split(" ")
                .filter { it.isNotBlank() }
                .take(2)
                .joinToString("") { it.first().uppercaseChar().toString() }
                .ifBlank { "NV" }

            db.collection("publicProfiles").document(userId).update(
                mapOf(
                    "displayName" to cleanName,
                    "handle" to cleanHandle,
                    "bio" to bio.trim().take(300),
                    "avatarInitials" to initials,
                    "interests" to interests.map { it.trim() }.filter { it.isNotBlank() }.take(10),
                    "creatorTier" to creatorTier,
                    "subscriptionPriceUsd" to subscriptionPriceUsd.coerceIn(0.0, 999.0),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updatePrivatePreferences(
        preferredLanguage: String,
        themeMode: String,
        feedDiversity: Int,
        privateAccount: Boolean
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: "unauthenticated"
        val path = "users/$uid"
        return try {
            val userId = requireUserId()
            val batch = db.batch()
            batch.update(
                db.collection("users").document(userId),
                mapOf(
                    "preferredLanguage" to preferredLanguage.take(30),
                    "themeMode" to themeMode,
                    "feedDiversity" to feedDiversity.coerceIn(0, 100).toLong(),
                    "privateAccount" to privateAccount,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                db.collection("publicProfiles").document(userId),
                mapOf(
                    "discoverable" to !privateAccount,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun submitReport(targetType: String, targetId: String, reason: String): Result<Unit> {
        var path = "reports"
        return try {
            val uid = requireUserId()
            val cleanReason = reason.trim()
            require(cleanReason.length in 3..300) { "Please provide at least 3 characters for report reason." }
            val docRef = db.collection("reports").document()
            path = docRef.path
            docRef.set(
                mapOf(
                    "reporterId" to uid,
                    "targetType" to targetType,
                    "targetId" to targetId.take(128),
                    "reason" to cleanReason,
                    "status" to "OPEN",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun resolveReportAsAdmin(reportId: String, newStatus: String): Result<Unit> {
        val path = "reports/$reportId"
        return try {
            requireUserId()
            db.document(path).update(
                mapOf(
                    "status" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun verifyProfileAsAdmin(targetUid: String, verified: Boolean): Result<Unit> {
        val path = "publicProfiles/$targetUid"
        return try {
            requireUserId()
            db.document(path).update(
                mapOf(
                    "verifiedBadge" to verified,
                    "creatorTier" to if (verified) "VERIFIED_PRO" else "CREATOR",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun exportMyDataJson(): Result<String> {
        return try {
            val uid = requireUserId()
            val priv = db.collection("users").document(uid).get().await().data ?: emptyMap()
            val pub = db.collection("publicProfiles").document(uid).get().await().data ?: emptyMap()
            val posts = db.collection("posts").whereEqualTo("authorId", uid).get().await()
                .documents.map { it.data ?: emptyMap() }
            val json = JSONObject().apply {
                put("exportedAt", System.currentTimeMillis())
                put("privateAccount", JSONObject(priv.mapValues { it.value?.toString() ?: "" }))
                put("publicProfile", JSONObject(pub.mapValues { it.value?.toString() ?: "" }))
                put("authoredPostsCount", posts.size)
                put("posts", JSONArray(posts.map { p -> JSONObject(p.mapValues { it.value?.toString() ?: "" }) }))
            }
            Result.success(json.toString(2))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "users")
            Result.failure(e)
        }
    }

    suspend fun deleteAccountRecords(): Result<Unit> {
        val uid = auth.currentUser?.uid ?: "unauthenticated"
        return try {
            val userId = requireUserId()
            val batch = db.batch()
            batch.delete(db.collection("users").document(userId))
            batch.delete(db.collection("publicProfiles").document(userId))
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$uid")
            Result.failure(e)
        }
    }
}
