package com.example.ui.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.PreferencesManager
import com.example.core.ThemeModePreference
import com.example.core.UserLocalPreferences
import com.example.data.ai.AiDiscoveryInsight
import com.example.data.ai.AiPostAssistResult
import com.example.data.ai.NovaraAiService
import com.example.data.model.ChatMessage
import com.example.data.model.Community
import com.example.data.model.CommunityDiscussion
import com.example.data.model.Conversation
import com.example.data.model.CreatorTip
import com.example.data.model.FeedFilter
import com.example.data.model.FollowRelation
import com.example.data.model.GiftType
import com.example.data.model.ModerationReport
import com.example.data.model.Post
import com.example.data.model.PostComment
import com.example.data.model.PrivateUser
import com.example.data.model.PublicProfile
import com.example.data.model.SavedPost
import com.example.data.model.Story
import com.example.data.model.UiState
import com.example.data.repository.NovaraRepository
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class NovaraViewModel(
    private val repository: NovaraRepository,
    private val aiService: NovaraAiService,
    private val preferencesManager: PreferencesManager,
    private val currentUserId: String,
    private val currentUserEmail: String?,
    private val isAdminUser: Boolean
) : ViewModel() {

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val events: SharedFlow<String> = _events.asSharedFlow()

    val localPreferences: StateFlow<UserLocalPreferences> = preferencesManager.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UserLocalPreferences())

    private fun <T> Flow<T>.toUiStateFlow(label: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "Error observing $label", root)
                emit(UiState.Error(root.message ?: "Failed to load $label"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = UiState.Loading
            )

    val privateUserState: StateFlow<UiState<PrivateUser?>> =
        repository.observePrivateUser(currentUserId).toUiStateFlow("account")

    val myProfileState: StateFlow<UiState<PublicProfile?>> =
        repository.observeMyProfile(currentUserId).toUiStateFlow("profile")

    val publicProfilesState: StateFlow<UiState<List<PublicProfile>>> =
        repository.observePublicProfiles(currentUserId).toUiStateFlow("creators")

    val postsState: StateFlow<UiState<List<Post>>> =
        repository.observePublicPosts(currentUserId).toUiStateFlow("feed")

    val storiesState: StateFlow<UiState<List<Story>>> =
        repository.observeStories(currentUserId).toUiStateFlow("stories")

    val savedPostsState: StateFlow<UiState<List<SavedPost>>> =
        repository.observeSavedPosts(currentUserId).toUiStateFlow("savedPosts")

    val followsState: StateFlow<UiState<List<FollowRelation>>> =
        repository.observeFollows(currentUserId).toUiStateFlow("follows")

    val communitiesState: StateFlow<UiState<List<Community>>> =
        repository.observeCommunities(currentUserId).toUiStateFlow("communities")

    val conversationsState: StateFlow<UiState<List<Conversation>>> =
        repository.observeConversations(currentUserId).toUiStateFlow("conversations")

    val receivedTipsState: StateFlow<UiState<List<CreatorTip>>> =
        repository.observeReceivedTips(currentUserId).toUiStateFlow("tips")

    val reportsState: StateFlow<UiState<List<ModerationReport>>> =
        repository.observeReports(currentUserId, isAdminUser).toUiStateFlow("reports")

    // Active detail observations
    private val _selectedPostIdForComments = MutableStateFlow<String?>(null)
    val selectedPostComments: StateFlow<UiState<List<PostComment>>> =
        _selectedPostIdForComments.flatMapLatest { postId ->
            if (postId.isNullOrBlank()) flowOf<UiState<List<PostComment>>>(UiState.Success(emptyList()))
            else repository.observePostComments(currentUserId, postId).map<List<PostComment>, UiState<List<PostComment>>> { UiState.Success(it) }
        }.catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            emit(UiState.Error((firestoreError ?: error).message ?: "Failed to load comments"))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Success(emptyList()))

    private val _selectedCommunityId = MutableStateFlow<String?>(null)
    val selectedCommunityDiscussions: StateFlow<UiState<List<CommunityDiscussion>>> =
        _selectedCommunityId.flatMapLatest { commId ->
            if (commId.isNullOrBlank()) flowOf<UiState<List<CommunityDiscussion>>>(UiState.Success(emptyList()))
            else repository.observeCommunityDiscussions(currentUserId, commId).map<List<CommunityDiscussion>, UiState<List<CommunityDiscussion>>> { UiState.Success(it) }
        }.catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            emit(UiState.Error((firestoreError ?: error).message ?: "Failed to load discussions"))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Success(emptyList()))

    private val _selectedConversationId = MutableStateFlow<String?>(null)
    val selectedConversationId: StateFlow<String?> = _selectedConversationId.asStateFlow()

    val selectedMessages: StateFlow<UiState<List<ChatMessage>>> =
        _selectedConversationId.flatMapLatest { convId ->
            if (convId.isNullOrBlank()) flowOf<UiState<List<ChatMessage>>>(UiState.Success(emptyList()))
            else repository.observeMessages(currentUserId, convId).map<List<ChatMessage>, UiState<List<ChatMessage>>> { UiState.Success(it) }
        }.catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            emit(UiState.Error((firestoreError ?: error).message ?: "Failed to load messages"))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Success(emptyList()))

    // Feed filter & locally tracked liked post IDs for instant badge feedback
    private val _feedFilter = MutableStateFlow(FeedFilter.FOR_YOU)
    val feedFilter: StateFlow<FeedFilter> = _feedFilter.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _likedPostIds = MutableStateFlow<Set<String>>(emptySet())
    val likedPostIds: StateFlow<Set<String>> = _likedPostIds.asStateFlow()

    // AI states
    private val _postTranslations = MutableStateFlow<Map<String, String>>(emptyMap())
    val postTranslations: StateFlow<Map<String, String>> = _postTranslations.asStateFlow()

    private val _messageTranslations = MutableStateFlow<Map<String, String>>(emptyMap())
    val messageTranslations: StateFlow<Map<String, String>> = _messageTranslations.asStateFlow()

    private val _translatingIds = MutableStateFlow<Set<String>>(emptySet())
    val translatingIds: StateFlow<Set<String>> = _translatingIds.asStateFlow()

    private val _aiPostAssistState = MutableStateFlow<UiState<AiPostAssistResult>?>(null)
    val aiPostAssistState: StateFlow<UiState<AiPostAssistResult>?> = _aiPostAssistState.asStateFlow()

    private val _aiDiscoveryState = MutableStateFlow<UiState<AiDiscoveryInsight>?>(null)
    val aiDiscoveryState: StateFlow<UiState<AiDiscoveryInsight>?> = _aiDiscoveryState.asStateFlow()

    private val _coachResponse = MutableStateFlow<UiState<String>?>(null)
    val coachResponse: StateFlow<UiState<String>?> = _coachResponse.asStateFlow()

    private val _exportedJson = MutableStateFlow<String?>(null)
    val exportedJson: StateFlow<String?> = _exportedJson.asStateFlow()

    val isCurrentUserAdmin: Boolean get() = isAdminUser
    val userId: String get() = currentUserId
    val userEmail: String? get() = currentUserEmail

    fun ensureProvisioned(user: FirebaseUser) {
        viewModelScope.launch {
            repository.ensureUserProvisioned(user)
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Failed to initialize profile") }
        }
    }

    fun setFeedFilter(filter: FeedFilter) {
        _feedFilter.value = filter
    }

    fun setCategoryFilter(category: String?) {
        _selectedCategory.value = category
    }

    fun openComments(postId: String?) {
        _selectedPostIdForComments.value = postId
    }

    fun openCommunity(communityId: String?) {
        _selectedCommunityId.value = communityId
    }

    fun openConversation(conversationId: String?) {
        _selectedConversationId.value = conversationId
    }

    fun clearAiPostAssist() {
        _aiPostAssistState.value = null
    }

    fun clearExportedJson() {
        _exportedJson.value = null
    }

    // --- FEED & CONTENT ACTIONS ---

    fun createPost(
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
        aiGenerated: Boolean,
        onPublished: () -> Unit
    ) {
        val profile = (myProfileState.value as? UiState.Success)?.data ?: PublicProfile(
            userId = currentUserId,
            displayName = "Novara Creator",
            handle = "novara_${currentUserId.take(4)}"
        )
        viewModelScope.launch {
            repository.createPost(
                authorProfile = profile,
                content = content,
                mediaType = mediaType,
                visibility = visibility,
                category = category,
                tags = tags,
                mediaUrl = mediaUrl,
                videoDurationSec = videoDurationSec,
                soundTrackTitle = soundTrackTitle,
                pollOptions = pollOptions,
                collaboratorHandle = collaboratorHandle,
                aiGenerated = aiGenerated
            ).onSuccess {
                _events.tryEmit("Published to Novara!")
                _aiPostAssistState.value = null
                onPublished()
            }.onFailure {
                _events.tryEmit(it.localizedMessage ?: "Failed to publish post")
            }
        }
    }

    fun toggleLike(post: Post) {
        viewModelScope.launch {
            repository.toggleLike(post)
                .onSuccess { liked ->
                    _likedPostIds.value = if (liked) {
                        _likedPostIds.value + post.id
                    } else {
                        _likedPostIds.value - post.id
                    }
                }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Could not update like") }
        }
    }

    fun repost(postId: String) {
        viewModelScope.launch {
            repository.repost(postId)
                .onSuccess { _events.tryEmit("Reposted to your network") }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Repost failed") }
        }
    }

    fun votePoll(post: Post, optionIndex: Int) {
        viewModelScope.launch {
            repository.votePoll(post, optionIndex)
                .onSuccess { _events.tryEmit("Vote recorded") }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Vote failed") }
        }
    }

    fun addComment(postId: String, text: String) {
        val profile = (myProfileState.value as? UiState.Success)?.data ?: PublicProfile(
            userId = currentUserId,
            displayName = "Novara User",
            handle = "novara_${currentUserId.take(4)}"
        )
        viewModelScope.launch {
            repository.addComment(postId, profile, text)
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Comment failed") }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            repository.deletePost(postId)
                .onSuccess { _events.tryEmit("Post deleted") }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Delete failed") }
        }
    }

    fun toggleSavePost(post: Post, currentlySaved: Boolean) {
        viewModelScope.launch {
            repository.toggleSavePost(post, currentlySaved)
                .onSuccess {
                    _events.tryEmit(if (currentlySaved) "Removed from saved" else "Saved to library")
                }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Save failed") }
        }
    }

    fun createStory(caption: String, moodTag: String, livePulse: Boolean, gradientIndex: Long) {
        val profile = (myProfileState.value as? UiState.Success)?.data ?: PublicProfile(
            userId = currentUserId,
            displayName = "Creator",
            handle = "novara_${currentUserId.take(4)}"
        )
        viewModelScope.launch {
            repository.createStory(profile, caption, moodTag, livePulse, gradientIndex)
                .onSuccess { _events.tryEmit("Story shared live!") }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Story failed") }
        }
    }

    // --- FOLLOW, COMMUNITIES & MESSAGING ---

    fun toggleFollow(target: PublicProfile, currentlyFollowing: Boolean) {
        viewModelScope.launch {
            repository.toggleFollow(target, currentlyFollowing)
                .onSuccess {
                    _events.tryEmit(
                        if (currentlyFollowing) "Unfollowed @${target.handle}"
                        else "Following @${target.handle}"
                    )
                }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Follow action failed") }
        }
    }

    fun createCommunity(name: String, description: String, category: String, activeRoomTopic: String?) {
        viewModelScope.launch {
            repository.createCommunity(name, description, category, activeRoomTopic)
                .onSuccess { _events.tryEmit("Community created!") }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Community creation failed") }
        }
    }

    fun toggleJoinCommunity(community: Community) {
        viewModelScope.launch {
            repository.toggleJoinCommunity(community)
                .onSuccess {
                    val joined = currentUserId !in community.memberUids
                    _events.tryEmit(if (joined) "Joined ${community.name}" else "Left ${community.name}")
                }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Could not update membership") }
        }
    }

    fun postDiscussion(communityId: String, title: String, body: String, pollQuestion: String?) {
        val name = (myProfileState.value as? UiState.Success)?.data?.displayName ?: "Member"
        viewModelScope.launch {
            repository.postDiscussion(communityId, name, title, body, pollQuestion)
                .onSuccess { _events.tryEmit("Discussion posted") }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Failed to post discussion") }
        }
    }

    fun startDirectMessage(otherProfile: PublicProfile, onOpened: (String) -> Unit) {
        val myProfile = (myProfileState.value as? UiState.Success)?.data ?: PublicProfile(
            userId = currentUserId,
            displayName = "Novara User",
            handle = "novara_${currentUserId.take(4)}"
        )
        viewModelScope.launch {
            repository.startConversation(myProfile, otherProfile)
                .onSuccess { convId ->
                    _selectedConversationId.value = convId
                    onOpened(convId)
                }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Could not open conversation") }
        }
    }

    fun sendMessage(conversation: Conversation, text: String, replyToText: String? = null) {
        val myName = (myProfileState.value as? UiState.Success)?.data?.displayName ?: "You"
        viewModelScope.launch {
            repository.sendMessage(conversation, myName, text, replyToText)
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Message failed") }
        }
    }

    fun editMessage(conversationId: String, messageId: String, newText: String) {
        viewModelScope.launch {
            repository.editMessage(conversationId, messageId, newText)
                .onSuccess { _events.tryEmit("Message edited") }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Edit failed") }
        }
    }

    fun toggleMessageReaction(conversationId: String, message: ChatMessage, emoji: String) {
        viewModelScope.launch {
            repository.toggleMessageReaction(conversationId, message, emoji)
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Reaction failed") }
        }
    }

    fun toggleDisappearing(conversation: Conversation) {
        viewModelScope.launch {
            repository.toggleDisappearingMessages(conversation)
                .onSuccess {
                    _events.tryEmit(
                        if (!conversation.disappearingEnabled) "Disappearing mode enabled"
                        else "Disappearing mode disabled"
                    )
                }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Toggle failed") }
        }
    }

    // --- CREATOR ECONOMY, PROFILE, SETTINGS & MODERATION ---

    fun sendCreatorTip(recipient: PublicProfile, giftType: GiftType, note: String) {
        val myName = (myProfileState.value as? UiState.Success)?.data?.displayName ?: "Supporter"
        viewModelScope.launch {
            repository.sendCreatorTip(myName, recipient, giftType, note)
                .onSuccess {
                    _events.tryEmit("Sent ${giftType.label} to ${recipient.displayName} (95% goes to creator)")
                }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Tip failed") }
        }
    }

    fun updateProfile(
        displayName: String,
        handle: String,
        bio: String,
        interests: List<String>,
        creatorTier: String,
        subscriptionPriceUsd: Double
    ) {
        viewModelScope.launch {
            repository.updateProfile(displayName, handle, bio, interests, creatorTier, subscriptionPriceUsd)
                .onSuccess { _events.tryEmit("Profile updated") }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Profile update failed") }
        }
    }

    fun updateSettings(
        themeMode: ThemeModePreference,
        preferredLanguage: String,
        feedDiversity: Int,
        privateAccount: Boolean
    ) {
        viewModelScope.launch {
            preferencesManager.setThemeMode(themeMode)
            preferencesManager.setPreferredLanguage(preferredLanguage)
            preferencesManager.setFeedDiversity(feedDiversity)
            repository.updatePrivatePreferences(preferredLanguage, themeMode.name, feedDiversity, privateAccount)
                .onSuccess { _events.tryEmit("Settings saved") }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Could not sync settings") }
        }
    }

    fun submitReport(targetType: String, targetId: String, reason: String) {
        viewModelScope.launch {
            repository.submitReport(targetType, targetId, reason)
                .onSuccess { _events.tryEmit("Report submitted for moderation review") }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Report failed") }
        }
    }

    fun resolveReportAsAdmin(reportId: String, status: String) {
        viewModelScope.launch {
            repository.resolveReportAsAdmin(reportId, status)
                .onSuccess { _events.tryEmit("Report marked $status") }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Admin update failed") }
        }
    }

    fun verifyProfileAsAdmin(targetUid: String, verified: Boolean) {
        viewModelScope.launch {
            repository.verifyProfileAsAdmin(targetUid, verified)
                .onSuccess { _events.tryEmit("Verification status updated") }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Verification update failed") }
        }
    }

    fun exportMyData() {
        viewModelScope.launch {
            repository.exportMyDataJson()
                .onSuccess { _exportedJson.value = it }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Data export failed") }
        }
    }

    fun deleteAccountData(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteAccountRecords()
                .onSuccess {
                    _events.tryEmit("Account data removed")
                    onDeleted()
                }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Account deletion failed") }
        }
    }

    // --- FIREBASE AI LOGIC (GEMINI 3.8 FLASH) ---

    fun runAiPostAssist(draft: String, mediaTypeLabel: String, bitmap: Bitmap?) {
        if (draft.isBlank() && bitmap == null) {
            _events.tryEmit("Enter a draft or attach a photo for Novara AI to analyze.")
            return
        }
        viewModelScope.launch {
            _aiPostAssistState.value = UiState.Loading
            aiService.generatePostAssist(draft, mediaTypeLabel, bitmap)
                .onSuccess { _aiPostAssistState.value = UiState.Success(it) }
                .onFailure { e ->
                    _aiPostAssistState.value = UiState.Error(e.localizedMessage ?: "AI Co-Creator error")
                }
        }
    }

    fun translatePost(post: Post) {
        val targetLang = localPreferences.value.preferredLanguage
        viewModelScope.launch {
            _translatingIds.value = _translatingIds.value + post.id
            aiService.translateText(post.content, targetLang)
                .onSuccess { translated ->
                    _postTranslations.value = _postTranslations.value + (post.id to translated)
                }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Translation failed") }
            _translatingIds.value = _translatingIds.value - post.id
        }
    }

    fun translateMessage(message: ChatMessage) {
        val targetLang = localPreferences.value.preferredLanguage
        viewModelScope.launch {
            _translatingIds.value = _translatingIds.value + message.id
            aiService.translateText(message.text, targetLang)
                .onSuccess { translated ->
                    _messageTranslations.value = _messageTranslations.value + (message.id to translated)
                }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Translation failed") }
            _translatingIds.value = _translatingIds.value - message.id
        }
    }

    fun runAiDiscovery(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _aiDiscoveryState.value = UiState.Loading
            aiService.analyzeDiscoveryIntent(
                query,
                listOf("Tech", "Design", "Creator", "Music", "Education", "Lifestyle")
            ).onSuccess {
                _aiDiscoveryState.value = UiState.Success(it)
            }.onFailure { e ->
                _aiDiscoveryState.value = UiState.Error(e.localizedMessage ?: "AI discovery failed")
            }
        }
    }

    fun askCreatorCoach(question: String) {
        if (question.isBlank()) return
        val profile = (myProfileState.value as? UiState.Success)?.data ?: PublicProfile(displayName = "Creator")
        val allPosts = (postsState.value as? UiState.Success)?.data.orEmpty()
        val myPosts = allPosts.filter { it.authorId == currentUserId }
        val totalLikes = myPosts.sumOf { it.likesCount }

        viewModelScope.launch {
            _coachResponse.value = UiState.Loading
            try {
                aiService.streamCreatorCoach(
                    creatorName = profile.displayName.ifBlank { "Creator" },
                    postsCount = myPosts.size,
                    totalLikes = totalLikes,
                    followersCount = profile.followersCount,
                    tipsEarnedCents = profile.tipsEarnedCents,
                    question = question
                ).collect { streamed ->
                    _coachResponse.value = UiState.Success(streamed)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI Creator Coach failed", e)
                _coachResponse.value = UiState.Error(e.localizedMessage ?: "Creator Coach unavailable")
            }
        }
    }

    private companion object {
        const val TAG = "NovaraVM"
        const val STOP_TIMEOUT_MS = 5000L
    }
}
