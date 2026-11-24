package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.Community
import com.example.data.model.CommunityDiscussion
import com.example.data.model.PublicProfile
import com.example.data.model.UiState
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldMint
import com.example.ui.theme.PrismViolet
import com.example.ui.viewmodel.NovaraViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    viewModel: NovaraViewModel,
    onOpenMessagesTab: () -> Unit
) {
    val profilesState by viewModel.publicProfilesState.collectAsStateWithLifecycle()
    val followsState by viewModel.followsState.collectAsStateWithLifecycle()
    val communitiesState by viewModel.communitiesState.collectAsStateWithLifecycle()
    val aiDiscoveryState by viewModel.aiDiscoveryState.collectAsStateWithLifecycle()
    val discussionsState by viewModel.selectedCommunityDiscussions.collectAsStateWithLifecycle()

    var query by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Creators & People, 1 = Communities
    var showCreateCommunity by remember { mutableStateOf(false) }
    var activeCommunity by remember { mutableStateOf<Community?>(null) }
    var tipTarget by remember { mutableStateOf<PublicProfile?>(null) }

    val followedIds = (followsState as? UiState.Success)?.data?.map { it.targetUserId }?.toSet().orEmpty()

    Column(modifier = Modifier.fillMaxSize()) {
        // Header + Search
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text(
                text = "Discover & Communities",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search people, topics, communities...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("discover_search_input")
                )
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .height(54.dp)
                        .clickable(enabled = query.isNotBlank()) { viewModel.runAiDiscovery(query) }
                        .testTag("ai_discover_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.AutoAwesome,
                            contentDescription = "AI Semantic Discovery",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("AI", color = Color.White, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        // AI Discovery Result Card
        when (val ai = aiDiscoveryState) {
            is UiState.Loading -> {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Novara AI analyzing semantic intent...", style = MaterialTheme.typography.bodyMedium)
                }
            }
            is UiState.Error -> {
                Text(
                    text = ai.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            is UiState.Success -> {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Novara AI Discovery Insight", style = MaterialTheme.typography.labelLarge)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(ai.data.summary, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(ai.data.recommendedTopics) { topic ->
                                FilterChip(
                                    selected = false,
                                    onClick = { query = topic },
                                    label = { Text(topic) }
                                )
                            }
                        }
                    }
                }
            }
            null -> {}
        }

        // Section Switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                label = { Text("Creators & People") },
                modifier = Modifier.testTag("discover_tab_creators")
            )
            FilterChip(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                label = { Text("Communities") },
                modifier = Modifier.testTag("discover_tab_communities")
            )
            Spacer(Modifier.weight(1f))
            if (selectedTab == 1) {
                TextButton(
                    onClick = { showCreateCommunity = true },
                    modifier = Modifier.testTag("create_community_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("New Group")
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Creator Spotlight Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(132.dp)
                        .clip(RoundedCornerShape(22.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_creator_spotlight),
                        contentDescription = "Creator spotlight showcase",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color.Black.copy(alpha = 0.78f), Color.Transparent)
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Column(modifier = Modifier.align(Alignment.CenterStart)) {
                            Text(
                                "CREATOR ECONOMY 2.0",
                                style = MaterialTheme.typography.labelMedium,
                                color = ElectricCyan
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "95% Revenue Share • Global Communities",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                            Text(
                                "Connect across languages and monetize transparently.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }

            if (selectedTab == 0) {
                when (val state = profilesState) {
                    is UiState.Loading -> item { CircularProgressIndicator() }
                    is UiState.Error -> item { Text(state.message, color = MaterialTheme.colorScheme.error) }
                    is UiState.Success -> {
                        val filtered = state.data.filter {
                            query.isBlank() ||
                                it.displayName.contains(query, ignoreCase = true) ||
                                it.handle.contains(query, ignoreCase = true) ||
                                it.bio.contains(query, ignoreCase = true) ||
                                it.interests.any { tag -> tag.contains(query, ignoreCase = true) }
                        }
                        if (filtered.isEmpty()) {
                            item {
                                Text(
                                    "No matching creators found yet.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            items(filtered, key = { it.userId }) { profile ->
                                val isSelf = profile.userId == viewModel.userId
                                val isFollowing = profile.userId in followedIds
                                CreatorDiscoveryCard(
                                    profile = profile,
                                    isSelf = isSelf,
                                    isFollowing = isFollowing,
                                    onFollowToggle = { viewModel.toggleFollow(profile, isFollowing) },
                                    onMessage = {
                                        viewModel.startDirectMessage(profile) { onOpenMessagesTab() }
                                    },
                                    onTip = { tipTarget = profile }
                                )
                            }
                        }
                    }
                }
            } else {
                when (val state = communitiesState) {
                    is UiState.Loading -> item { CircularProgressIndicator() }
                    is UiState.Error -> item { Text(state.message, color = MaterialTheme.colorScheme.error) }
                    is UiState.Success -> {
                        val filtered = state.data.filter {
                            query.isBlank() ||
                                it.name.contains(query, ignoreCase = true) ||
                                it.description.contains(query, ignoreCase = true) ||
                                it.category.contains(query, ignoreCase = true)
                        }
                        if (filtered.isEmpty()) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainer,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            Icons.Rounded.Groups,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Text("No communities yet", style = MaterialTheme.typography.titleMedium)
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            "Launch a global interest, tech, or creator hub.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(Modifier.height(12.dp))
                                        Button(onClick = { showCreateCommunity = true }) {
                                            Text("Create Community")
                                        }
                                    }
                                }
                            }
                        } else {
                            items(filtered, key = { it.id }) { community ->
                                val isMember = viewModel.userId in community.memberUids
                                CommunityCard(
                                    community = community,
                                    isMember = isMember,
                                    onToggleJoin = { viewModel.toggleJoinCommunity(community) },
                                    onOpenHub = {
                                        activeCommunity = community
                                        viewModel.openCommunity(community.id)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateCommunity) {
        CreateCommunityDialog(
            onDismiss = { showCreateCommunity = false },
            onCreate = { name, desc, cat, room ->
                viewModel.createCommunity(name, desc, cat, room)
                showCreateCommunity = false
            }
        )
    }

    activeCommunity?.let { community ->
        ModalBottomSheet(
            onDismissRequest = {
                activeCommunity = null
                viewModel.openCommunity(null)
            }
        ) {
            CommunityHubSheet(
                community = community,
                discussionsState = discussionsState,
                onPostDiscussion = { title, body, poll ->
                    viewModel.postDiscussion(community.id, title, body, poll)
                }
            )
        }
    }

    tipTarget?.let { creator ->
        SendTipDialog(
            creator = creator,
            onDismiss = { tipTarget = null },
            onSend = { gift, note ->
                viewModel.sendCreatorTip(creator, gift, note)
                tipTarget = null
            }
        )
    }
}

@Composable
private fun CreatorDiscoveryCard(
    profile: PublicProfile,
    isSelf: Boolean,
    isFollowing: Boolean,
    onFollowToggle: () -> Unit,
    onMessage: () -> Unit,
    onTip: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(ElectricCyan, PrismViolet))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(profile.avatarInitials, color = Color.White, style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(profile.displayName, style = MaterialTheme.typography.titleMedium)
                        if (profile.verifiedBadge) {
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.Rounded.Verified,
                                contentDescription = "Verified",
                                tint = ElectricCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        "@${profile.handle} • ${profile.followersCount} followers",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!isSelf) {
                    if (isFollowing) {
                        OutlinedButton(onClick = onFollowToggle) { Text("Following") }
                    } else {
                        Button(onClick = onFollowToggle) { Text("Follow") }
                    }
                }
            }
            if (profile.bio.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    profile.bio,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (!isSelf) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onMessage, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Message")
                    }
                    OutlinedButton(onClick = onTip, modifier = Modifier.weight(1f)) {
                        Icon(
                            Icons.Rounded.CardGiftcard,
                            contentDescription = null,
                            tint = EmeraldMint,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Tip")
                    }
                }
            }
        }
    }
}

@Composable
private fun CommunityCard(
    community: Community,
    isMember: Boolean,
    onToggleJoin: () -> Unit,
    onOpenHub: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenHub() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(community.name, style = MaterialTheme.typography.titleLarge)
                    Text(
                        "${community.category} • ${community.membersCount} members",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (isMember) {
                    OutlinedButton(onClick = onToggleJoin) { Text("Joined") }
                } else {
                    Button(onClick = onToggleJoin) { Text("Join") }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(community.description, style = MaterialTheme.typography.bodyMedium)
            if (!community.activeRoomTopic.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        "Live Room: ${community.activeRoomTopic}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Forum, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Open Community Discussions", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun CreateCommunityDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, String, String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Tech") }
    var roomTopic by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Global Community") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(80) },
                    label = { Text("Community name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it.take(400) },
                    label = { Text("Purpose & guidelines") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it.take(40) },
                    label = { Text("Category (Tech, Design, Music...)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = roomTopic,
                    onValueChange = { roomTopic = it.take(120) },
                    label = { Text("Optional interactive social room topic") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name, desc, category, roomTopic.ifBlank { null }) },
                enabled = name.trim().length >= 2 && desc.trim().isNotBlank()
            ) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun CommunityHubSheet(
    community: Community,
    discussionsState: UiState<List<CommunityDiscussion>>,
    onPostDiscussion: (String, String, String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(community.name, style = MaterialTheme.typography.titleLarge)
        Text(community.description, style = MaterialTheme.typography.bodyMedium)

        when (discussionsState) {
            is UiState.Loading -> CircularProgressIndicator()
            is UiState.Error -> Text(discussionsState.message, color = MaterialTheme.colorScheme.error)
            is UiState.Success -> {
                if (discussionsState.data.isEmpty()) {
                    Text(
                        "No threads yet. Start the first discussion below!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(discussionsState.data, key = { it.id }) { disc ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(disc.title, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "by ${disc.authorName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(disc.body, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
        }

        OutlinedTextField(
            value = title,
            onValueChange = { title = it.take(120) },
            label = { Text("New thread title") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = body,
            onValueChange = { body = it.take(1000) },
            label = { Text("Share an idea or event announcement...") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                if (title.isNotBlank() && body.isNotBlank()) {
                    onPostDiscussion(title, body, null)
                    title = ""
                    body = ""
                }
            },
            enabled = title.isNotBlank() && body.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Post to Community")
        }
        Spacer(Modifier.height(16.dp))
    }
}
