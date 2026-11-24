package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.core.ThemeModePreference
import com.example.data.model.FeedFilter
import com.example.data.model.GiftType
import com.example.data.model.Post
import com.example.data.model.PostComment
import com.example.data.model.PublicProfile
import com.example.data.model.Story
import com.example.data.model.UiState
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldMint
import com.example.ui.theme.PrismViolet
import com.example.ui.theme.PulseCoral
import com.example.ui.viewmodel.NovaraViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeFeedScreen(
    viewModel: NovaraViewModel,
    onNavigateToCreate: () -> Unit
) {
    val context = LocalContext.current
    val postsState by viewModel.postsState.collectAsStateWithLifecycle()
    val storiesState by viewModel.storiesState.collectAsStateWithLifecycle()
    val savedPostsState by viewModel.savedPostsState.collectAsStateWithLifecycle()
    val followsState by viewModel.followsState.collectAsStateWithLifecycle()
    val profilesState by viewModel.publicProfilesState.collectAsStateWithLifecycle()
    val feedFilter by viewModel.feedFilter.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val likedPostIds by viewModel.likedPostIds.collectAsStateWithLifecycle()
    val postTranslations by viewModel.postTranslations.collectAsStateWithLifecycle()
    val translatingIds by viewModel.translatingIds.collectAsStateWithLifecycle()
    val commentsState by viewModel.selectedPostComments.collectAsStateWithLifecycle()
    val localPrefs by viewModel.localPreferences.collectAsStateWithLifecycle()
    val privateUserState by viewModel.privateUserState.collectAsStateWithLifecycle()

    var showFeedControls by remember { mutableStateOf(false) }
    var showStoryCreator by remember { mutableStateOf(false) }
    var activeStory by remember { mutableStateOf<Story?>(null) }
    var commentTargetPost by remember { mutableStateOf<Post?>(null) }
    var reportTargetPost by remember { mutableStateOf<Post?>(null) }
    var tipTargetProfile by remember { mutableStateOf<PublicProfile?>(null) }

    val savedIds = (savedPostsState as? UiState.Success)?.data?.map { it.postId }?.toSet().orEmpty()
    val followedIds = (followsState as? UiState.Success)?.data?.map { it.targetUserId }?.toSet().orEmpty()
    val allProfiles = (profilesState as? UiState.Success)?.data.orEmpty()

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Brand & Feed Controls Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "NOVARA",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "AI Feed • ${localPrefs.feedDiversity}% Discovery",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = { showFeedControls = true },
                    modifier = Modifier.testTag("feed_controls_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = "Personalize feed algorithm",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(44.dp)
                        .clickable { onNavigateToCreate() }
                        .testTag("top_create_post_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Create post",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }

        // Feed Mode Chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(FeedFilter.entries) { filter ->
                FilterChip(
                    selected = feedFilter == filter,
                    onClick = { viewModel.setFeedFilter(filter) },
                    label = { Text(filter.label) },
                    modifier = Modifier.testTag("feed_filter_${filter.name.lowercase()}")
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Main Feed List
        when (val state = postsState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            is UiState.Success -> {
                val filteredPosts = remember(state.data, feedFilter, selectedCategory, followedIds, localPrefs.feedDiversity) {
                    val base = when (feedFilter) {
                        FeedFilter.FOR_YOU -> {
                            val diversity = localPrefs.feedDiversity
                            state.data.sortedByDescending { post ->
                                val followBoost = if (post.authorId in followedIds) (100 - diversity) else diversity / 2
                                val engagement = post.likesCount * 2 + post.commentsCount * 3 + post.repostsCount * 4
                                followBoost + engagement
                            }
                        }
                        FeedFilter.FOLLOWING -> state.data.filter {
                            it.authorId in followedIds || it.authorId == viewModel.userId
                        }
                        FeedFilter.SHORT_VIDEO -> state.data.filter { it.mediaType == "SHORT_VIDEO" }
                        FeedFilter.LONG_VIDEO -> state.data.filter { it.mediaType == "LONG_VIDEO" }
                        FeedFilter.TRENDING -> state.data.sortedByDescending {
                            it.likesCount + it.commentsCount * 2 + it.repostsCount * 3
                        }
                    }
                    if (selectedCategory != null) {
                        base.filter { it.category.equals(selectedCategory, ignoreCase = true) }
                    } else {
                        base
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Stories Strip
                    item {
                        val stories = (storiesState as? UiState.Success)?.data.orEmpty()
                        StoriesStrip(
                            stories = stories,
                            onAddStory = { showStoryCreator = true },
                            onSelectStory = { activeStory = it }
                        )
                    }

                    // Active category filter indicator
                    if (selectedCategory != null) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Topic: $selectedCategory",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                TextButton(onClick = { viewModel.setCategoryFilter(null) }) {
                                    Text("Clear")
                                }
                            }
                        }
                    }

                    if (filteredPosts.isEmpty()) {
                        item {
                            EmptyFeedCard(
                                filter = feedFilter,
                                onCreatePost = onNavigateToCreate
                            )
                        }
                    } else {
                        items(filteredPosts, key = { it.id }) { post ->
                            val isOwn = post.authorId == viewModel.userId
                            val authorProfile = allProfiles.firstOrNull { it.userId == post.authorId }
                            PostCard(
                                post = post,
                                isOwnPost = isOwn,
                                isLiked = post.id in likedPostIds,
                                isSaved = post.id in savedIds,
                                isTranslating = post.id in translatingIds,
                                translatedText = postTranslations[post.id],
                                preferredLanguage = localPrefs.preferredLanguage,
                                onLike = { viewModel.toggleLike(post) },
                                onComment = {
                                    commentTargetPost = post
                                    viewModel.openComments(post.id)
                                },
                                onRepost = { viewModel.repost(post.id) },
                                onSave = { viewModel.toggleSavePost(post, post.id in savedIds) },
                                onTranslate = { viewModel.translatePost(post) },
                                onVotePoll = { idx -> viewModel.votePoll(post, idx) },
                                onCategoryClick = { viewModel.setCategoryFilter(post.category) },
                                onShare = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, "${post.authorName} (@${post.authorHandle}) on Novara:\n${post.content}")
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Novara post"))
                                },
                                onTip = {
                                    if (authorProfile != null && !isOwn) {
                                        tipTargetProfile = authorProfile
                                    }
                                },
                                onReport = { reportTargetPost = post },
                                onDelete = { viewModel.deletePost(post.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Story Creator Dialog
    if (showStoryCreator) {
        CreateStoryDialog(
            onDismiss = { showStoryCreator = false },
            onPublish = { caption, mood, live ->
                viewModel.createStory(caption, mood, live, (0..5).random().toLong())
                showStoryCreator = false
            }
        )
    }

    // Active Story Viewer Dialog
    activeStory?.let { story ->
        AlertDialog(
            onDismissRequest = { activeStory = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(story.authorName, style = MaterialTheme.typography.titleLarge)
                    if (story.livePulse) {
                        Spacer(Modifier.width(8.dp))
                        Surface(color = PulseCoral, shape = RoundedCornerShape(8.dp)) {
                            Text(
                                "LIVE",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("@${story.authorHandle} • #${story.moodTag}", color = MaterialTheme.colorScheme.primary)
                    Text(story.caption, style = MaterialTheme.typography.bodyLarge)
                }
            },
            confirmButton = {
                TextButton(onClick = { activeStory = null }) { Text("Close") }
            }
        )
    }

    // Comments Bottom Sheet
    commentTargetPost?.let { post ->
        ModalBottomSheet(
            onDismissRequest = {
                commentTargetPost = null
                viewModel.openComments(null)
            }
        ) {
            CommentsSheetContent(
                post = post,
                commentsState = commentsState,
                onSendComment = { text -> viewModel.addComment(post.id, text) }
            )
        }
    }

    // Feed Personalization Sheet
    if (showFeedControls) {
        val priv = (privateUserState as? UiState.Success)?.data
        FeedPersonalizationSheet(
            diversity = localPrefs.feedDiversity,
            language = localPrefs.preferredLanguage,
            themeMode = localPrefs.themeMode,
            privateAccount = priv?.privateAccount ?: false,
            onDismiss = { showFeedControls = false },
            onSave = { div, lang, theme, privAcc ->
                viewModel.updateSettings(theme, lang, div, privAcc)
                showFeedControls = false
            }
        )
    }

    // Report Post Dialog
    reportTargetPost?.let { post ->
        ReportDialog(
            targetLabel = "post by @${post.authorHandle}",
            onDismiss = { reportTargetPost = null },
            onSubmit = { reason ->
                viewModel.submitReport("POST", post.id, reason)
                reportTargetPost = null
            }
        )
    }

    // Tip Creator Dialog
    tipTargetProfile?.let { creator ->
        SendTipDialog(
            creator = creator,
            onDismiss = { tipTargetProfile = null },
            onSend = { gift, note ->
                viewModel.sendCreatorTip(creator, gift, note)
                tipTargetProfile = null
            }
        )
    }
}

@Composable
private fun StoriesStrip(
    stories: List<Story>,
    onAddStory: () -> Unit,
    onSelectStory: (Story) -> Unit
) {
    val gradients = listOf(
        listOf(ElectricCyan, PrismViolet),
        listOf(PrismViolet, PulseCoral),
        listOf(EmeraldMint, ElectricCyan),
        listOf(PulseCoral, Color(0xFFF59E0B))
    )

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(70.dp)
                    .clickable { onAddStory() }
                    .testTag("add_story_button")
            ) {
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Add story or live pulse",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text("Your Pulse", style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        }
        items(stories, key = { it.id }) { story ->
            val colors = gradients[(story.gradientIndex.toInt().coerceAtLeast(0)) % gradients.size]
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(70.dp)
                    .clickable { onSelectStory(story) }
            ) {
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .border(2.5.dp, Brush.linearGradient(colors), CircleShape)
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = story.authorName.take(2).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (story.livePulse) "LIVE • ${story.moodTag}" else story.authorName,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (story.livePulse) PulseCoral else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun PostCard(
    post: Post,
    isOwnPost: Boolean,
    isLiked: Boolean,
    isSaved: Boolean,
    isTranslating: Boolean,
    translatedText: String?,
    preferredLanguage: String,
    onLike: () -> Unit,
    onComment: () -> Unit,
    onRepost: () -> Unit,
    onSave: () -> Unit,
    onTranslate: () -> Unit,
    onVotePoll: (Int) -> Unit,
    onCategoryClick: () -> Unit,
    onShare: () -> Unit,
    onTip: () -> Unit,
    onReport: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Author Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(ElectricCyan, PrismViolet))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = post.authorName.take(2).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = post.authorName,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (post.authorVerified) {
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Rounded.Verified,
                                    contentDescription = "Verified creator",
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            if (post.aiGenerated) {
                                Spacer(Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = "AI assisted post",
                                    tint = PrismViolet,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                        val collab = post.collaboratorHandle?.let { " + @$it" } ?: ""
                        Text(
                            text = "@${post.authorHandle}$collab",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                    modifier = Modifier.clickable { onCategoryClick() }
                ) {
                    Text(
                        text = post.category,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Media Preview (Photo / Short Video / Long Video)
            if (post.mediaType == "SHORT_VIDEO" || post.mediaType == "LONG_VIDEO") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (post.mediaType == "SHORT_VIDEO") 210.dp else 160.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF141E46), Color(0xFF2B1E5C), Color(0xFF0E344B))
                            )
                        )
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.align(Alignment.TopStart),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Videocam,
                            contentDescription = "Video badge",
                            tint = ElectricCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (post.mediaType == "SHORT_VIDEO") "NOVARA SHORT" else "LONG-FORM HD",
                            style = MaterialTheme.typography.labelMedium,
                            color = ElectricCyan
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .align(Alignment.Center)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Video stream preview",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = post.soundTrackTitle ?: "Original Novara Audio",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        if (post.videoDurationSec > 0) {
                            Text(
                                text = "${post.videoDurationSec}s",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            } else if (!post.mediaUrl.isNullOrBlank()) {
                AsyncImage(
                    model = post.mediaUrl,
                    contentDescription = "Attached post photo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(10.dp))
            }

            // Content Text
            Text(
                text = post.content,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            // AI Translation Box
            AnimatedVisibility(visible = translatedText != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Translated to $preferredLanguage via Novara AI",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = translatedText.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Interactive Poll
            if (post.pollOptions.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                val totalVotes = post.pollVotes.sum().coerceAtLeast(1L)
                post.pollOptions.forEachIndexed { idx, option ->
                    val count = post.pollVotes.getOrNull(idx) ?: 0L
                    val fraction = count.toFloat() / totalVotes.toFloat()
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clickable { onVotePoll(idx) }
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(option, style = MaterialTheme.typography.bodyMedium)
                                Text("$count votes", style = MaterialTheme.typography.labelSmall)
                            }
                            Spacer(Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { fraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                        }
                    }
                }
            }

            // Hashtags
            if (post.tags.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = post.tags.joinToString(" ") { "#$it" },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Action Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onLike) {
                        Icon(
                            imageVector = if (isLiked) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Like post",
                            tint = if (isLiked) PulseCoral else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text("${post.likesCount}", style = MaterialTheme.typography.labelMedium)

                    IconButton(onClick = onComment) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = "Comments",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text("${post.commentsCount}", style = MaterialTheme.typography.labelMedium)

                    IconButton(onClick = onRepost) {
                        Icon(
                            imageVector = Icons.Rounded.Repeat,
                            contentDescription = "Repost",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text("${post.repostsCount}", style = MaterialTheme.typography.labelMedium)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onTranslate, enabled = !isTranslating) {
                        if (isTranslating) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Translate,
                                contentDescription = "Translate post",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    if (!isOwnPost) {
                        IconButton(onClick = onTip) {
                            Icon(
                                imageVector = Icons.Rounded.CardGiftcard,
                                contentDescription = "Tip creator",
                                tint = EmeraldMint
                            )
                        }
                    }
                    IconButton(onClick = onSave) {
                        Icon(
                            imageVector = if (isSaved) Icons.Rounded.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Save post",
                            tint = if (isSaved) ElectricCyan else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onShare) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = "Share post",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = if (isOwnPost) onDelete else onReport) {
                        Icon(
                            imageVector = if (isOwnPost) Icons.Rounded.DeleteOutline else Icons.Rounded.Flag,
                            contentDescription = if (isOwnPost) "Delete post" else "Report post",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyFeedCard(filter: FeedFilter, onCreatePost: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "No ${filter.label} posts yet",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Be the first creator to publish in this stream using Novara AI Co-Creator.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onCreatePost) {
                Text("Create First Post")
            }
        }
    }
}

@Composable
private fun CommentsSheetContent(
    post: Post,
    commentsState: UiState<List<PostComment>>,
    onSendComment: (String) -> Unit
) {
    var commentText by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Text("Comments on @${post.authorHandle}", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))

        when (commentsState) {
            is UiState.Loading -> CircularProgressIndicator()
            is UiState.Error -> Text(commentsState.message, color = MaterialTheme.colorScheme.error)
            is UiState.Success -> {
                if (commentsState.data.isEmpty()) {
                    Text(
                        "No comments yet. Start the conversation!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(commentsState.data, key = { it.id }) { comment ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        "${comment.authorName} (@${comment.authorHandle})",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(comment.text, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = commentText,
                onValueChange = { commentText = it },
                placeholder = { Text("Write a thoughtful reply...") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    if (commentText.isNotBlank()) {
                        onSendComment(commentText)
                        commentText = ""
                    }
                }
            ) {
                Text("Post")
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun CreateStoryDialog(
    onDismiss: () -> Unit,
    onPublish: (String, String, Boolean) -> Unit
) {
    var caption by remember { mutableStateOf("") }
    var mood by remember { mutableStateOf("Building") }
    var livePulse by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Share Story or Live Pulse") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it.take(240) },
                    label = { Text("What's happening right now?") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = mood,
                    onValueChange = { mood = it.take(30) },
                    label = { Text("Mood / Topic tag") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Broadcast as Live Pulse")
                    Switch(checked = livePulse, onCheckedChange = { livePulse = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (caption.isNotBlank()) onPublish(caption, mood, livePulse) },
                enabled = caption.isNotBlank()
            ) { Text("Share") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedPersonalizationSheet(
    diversity: Int,
    language: String,
    themeMode: ThemeModePreference,
    privateAccount: Boolean,
    onDismiss: () -> Unit,
    onSave: (Int, String, ThemeModePreference, Boolean) -> Unit
) {
    var sliderVal by remember { mutableIntStateOf(diversity) }
    var selectedLang by remember { mutableStateOf(language) }
    var selectedTheme by remember { mutableStateOf(themeMode) }
    var priv by remember { mutableStateOf(privateAccount) }

    val languages = listOf("English", "Spanish", "French", "Japanese", "Hindi", "German")

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("User-Controlled Feed & AI", style = MaterialTheme.typography.titleLarge)
            Text(
                "Discovery vs. Following balance: $sliderVal%",
                style = MaterialTheme.typography.bodyMedium
            )
            Slider(
                value = sliderVal.toFloat(),
                onValueChange = { sliderVal = it.toInt() },
                valueRange = 0f..100f
            )
            Text("AI Translation Language", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(languages) { lang ->
                    FilterChip(
                        selected = selectedLang == lang,
                        onClick = { selectedLang = lang },
                        label = { Text(lang) }
                    )
                }
            }
            Text("Theme", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeModePreference.entries.forEach { mode ->
                    FilterChip(
                        selected = selectedTheme == mode,
                        onClick = { selectedTheme = mode },
                        label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Private profile (hide from discovery)")
                Switch(checked = priv, onCheckedChange = { priv = it })
            }
            Button(
                onClick = { onSave(sliderVal, selectedLang, selectedTheme, priv) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Apply Feed & Privacy Controls")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun ReportDialog(
    targetLabel: String,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report $targetLabel") },
        text = {
            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it.take(300) },
                label = { Text("Reason (min 3 characters)") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { if (reason.trim().length >= 3) onSubmit(reason) },
                enabled = reason.trim().length >= 3
            ) { Text("Submit Report") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun SendTipDialog(
    creator: PublicProfile,
    onDismiss: () -> Unit,
    onSend: (GiftType, String) -> Unit
) {
    var selectedGift by remember { mutableStateOf(GiftType.DIAMOND) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Support ${creator.displayName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Transparent 95/5 split: 95% goes directly to @${creator.handle}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = EmeraldMint
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    GiftType.entries.forEach { gift ->
                        val selected = selectedGift == gift
                        OutlinedButton(
                            onClick = { selectedGift = gift },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${if (selected) "● " else ""}${gift.label} — $${String.format("%.2f", gift.amountCents / 100.0)}",
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(200) },
                    label = { Text("Optional supporter note") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSend(selectedGift, note) }) {
                Text("Send ${selectedGift.label}")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
