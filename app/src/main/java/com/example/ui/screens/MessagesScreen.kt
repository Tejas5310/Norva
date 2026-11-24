package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.rounded.AddComment
import androidx.compose.material.icons.rounded.AutoDelete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.PublicProfile
import com.example.data.model.UiState
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldMint
import com.example.ui.theme.PrismViolet
import com.example.ui.viewmodel.NovaraViewModel

@Composable
fun MessagesScreen(viewModel: NovaraViewModel) {
    val conversationsState by viewModel.conversationsState.collectAsStateWithLifecycle()
    val selectedConversationId by viewModel.selectedConversationId.collectAsStateWithLifecycle()
    val messagesState by viewModel.selectedMessages.collectAsStateWithLifecycle()
    val profilesState by viewModel.publicProfilesState.collectAsStateWithLifecycle()
    val messageTranslations by viewModel.messageTranslations.collectAsStateWithLifecycle()
    val translatingIds by viewModel.translatingIds.collectAsStateWithLifecycle()
    val localPrefs by viewModel.localPreferences.collectAsStateWithLifecycle()

    var showNewChatPicker by remember { mutableStateOf(false) }

    val activeConversation = (conversationsState as? UiState.Success)?.data
        ?.firstOrNull { it.id == selectedConversationId }

    if (activeConversation != null) {
        BackHandler { viewModel.openConversation(null) }
        ConversationDetailView(
            conversation = activeConversation,
            currentUserId = viewModel.userId,
            messagesState = messagesState,
            messageTranslations = messageTranslations,
            translatingIds = translatingIds,
            preferredLanguage = localPrefs.preferredLanguage,
            onBack = { viewModel.openConversation(null) },
            onToggleDisappearing = { viewModel.toggleDisappearing(activeConversation) },
            onSend = { text, reply -> viewModel.sendMessage(activeConversation, text, reply) },
            onEdit = { msgId, updated -> viewModel.editMessage(activeConversation.id, msgId, updated) },
            onReact = { msg, emoji -> viewModel.toggleMessageReaction(activeConversation.id, msg, emoji) },
            onTranslate = { msg -> viewModel.translateMessage(msg) }
        )
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Connect & Translate",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Encrypted chats • Instant AI translation (${localPrefs.preferredLanguage})",
                        style = MaterialTheme.typography.labelMedium,
                        color = EmeraldMint
                    )
                }
                IconButton(
                    onClick = { showNewChatPicker = true },
                    modifier = Modifier.testTag("new_chat_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AddComment,
                        contentDescription = "Start new conversation",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            when (val state = conversationsState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is UiState.Error -> {
                    Text(
                        state.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                is UiState.Success -> {
                    if (state.data.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Rounded.Lock,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(Modifier.height(10.dp))
                                    Text("No conversations yet", style = MaterialTheme.typography.titleLarge)
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "Message any creator globally — Novara translates across languages in real time.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(14.dp))
                                    Button(onClick = { showNewChatPicker = true }) {
                                        Text("Start Encrypted Chat")
                                    }
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(state.data, key = { it.id }) { conv ->
                                ConversationCard(
                                    conversation = conv,
                                    onClick = { viewModel.openConversation(conv.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNewChatPicker) {
        val others = (profilesState as? UiState.Success)?.data
            ?.filter { it.userId != viewModel.userId }
            .orEmpty()
        NewChatDialog(
            profiles = others,
            onDismiss = { showNewChatPicker = false },
            onSelect = { profile ->
                showNewChatPicker = false
                viewModel.startDirectMessage(profile) {}
            }
        )
    }
}

@Composable
private fun ConversationCard(conversation: Conversation, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(ElectricCyan, PrismViolet))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    conversation.title.take(2).uppercase(),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        conversation.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (conversation.encryptedBadge) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Rounded.Lock,
                            contentDescription = "Encrypted",
                            tint = EmeraldMint,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Text(
                    conversation.lastMessageText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ConversationDetailView(
    conversation: Conversation,
    currentUserId: String,
    messagesState: UiState<List<ChatMessage>>,
    messageTranslations: Map<String, String>,
    translatingIds: Set<String>,
    preferredLanguage: String,
    onBack: () -> Unit,
    onToggleDisappearing: () -> Unit,
    onSend: (String, String?) -> Unit,
    onEdit: (String, String) -> Unit,
    onReact: (ChatMessage, String) -> Unit,
    onTranslate: (ChatMessage) -> Unit
) {
    var draft by remember { mutableStateOf("") }
    var replyTarget by remember { mutableStateOf<String?>(null) }
    var editingMessage by remember { mutableStateOf<ChatMessage?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Chat Bar
        Surface(color = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 2.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to inbox")
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(conversation.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                    Text(
                        if (conversation.disappearingEnabled) "Disappearing mode ON • E2EE"
                        else "E2E Encrypted Channel",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (conversation.disappearingEnabled) MaterialTheme.colorScheme.tertiary else EmeraldMint
                    )
                }
                IconButton(onClick = onToggleDisappearing) {
                    Icon(
                        Icons.Rounded.AutoDelete,
                        contentDescription = "Toggle disappearing messages",
                        tint = if (conversation.disappearingEnabled) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Messages List
        Box(modifier = Modifier.weight(1f)) {
            when (messagesState) {
                is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                is UiState.Error -> Text(messagesState.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
                is UiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(messagesState.data, key = { it.id }) { msg ->
                            val isMe = msg.senderId == currentUserId
                            MessageBubble(
                                message = msg,
                                isMe = isMe,
                                translatedText = messageTranslations[msg.id],
                                isTranslating = msg.id in translatingIds,
                                preferredLanguage = preferredLanguage,
                                onReply = { replyTarget = msg.text },
                                onEdit = { editingMessage = msg },
                                onReact = { emoji -> onReact(msg, emoji) },
                                onTranslate = { onTranslate(msg) }
                            )
                        }
                    }
                }
            }
        }

        // Reply Preview
        replyTarget?.let { rep ->
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Replying to: $rep",
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { replyTarget = null }) { Text("Clear") }
                }
            }
        }

        // Input Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it.take(1000) },
                placeholder = { Text("Write a message...") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_message_input"),
                singleLine = true
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    if (draft.isNotBlank()) {
                        onSend(draft, replyTarget)
                        draft = ""
                        replyTarget = null
                    }
                },
                enabled = draft.isNotBlank(),
                modifier = Modifier.testTag("chat_send_button")
            ) {
                Icon(Icons.Rounded.Send, contentDescription = "Send")
            }
        }
    }

    editingMessage?.let { target ->
        var editedText by remember(target.id) { mutableStateOf(target.text) }
        AlertDialog(
            onDismissRequest = { editingMessage = null },
            title = { Text("Edit Message") },
            text = {
                OutlinedTextField(
                    value = editedText,
                    onValueChange = { editedText = it.take(1000) },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editedText.isNotBlank()) {
                            onEdit(target.id, editedText)
                            editingMessage = null
                        }
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { editingMessage = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    isMe: Boolean,
    translatedText: String?,
    isTranslating: Boolean,
    preferredLanguage: String,
    onReply: () -> Unit,
    onEdit: () -> Unit,
    onReact: (String) -> Unit,
    onTranslate: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = if (isMe) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.widthIn(max = 310.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (!message.replyToText.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.background.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        Text(
                            text = "↩ ${message.replyToText}",
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                }
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (message.editedFlag) {
                    Text(
                        text = "(edited)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (translatedText != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "[$preferredLanguage] $translatedText",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                if (message.reactions.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(message.reactions.joinToString(" "), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onTranslate, enabled = !isTranslating, modifier = Modifier.size(36.dp)) {
                if (isTranslating) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Rounded.Translate, contentDescription = "Translate message", modifier = Modifier.size(16.dp))
                }
            }
            IconButton(onClick = onReply, modifier = Modifier.size(36.dp)) {
                Icon(Icons.AutoMirrored.Rounded.Reply, contentDescription = "Reply", modifier = Modifier.size(16.dp))
            }
            if (isMe) {
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                }
            }
            Text(
                text = "🔥",
                modifier = Modifier
                    .clickable { onReact("🔥") }
                    .padding(horizontal = 6.dp)
            )
            Text(
                text = "❤️",
                modifier = Modifier
                    .clickable { onReact("❤️") }
                    .padding(horizontal = 6.dp)
            )
        }
    }
}

@Composable
private fun NewChatDialog(
    profiles: List<PublicProfile>,
    onDismiss: () -> Unit,
    onSelect: (PublicProfile) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Start Encrypted Conversation") },
        text = {
            if (profiles.isEmpty()) {
                Text("No other discoverable creators signed up yet.")
            } else {
                LazyColumn(
                    modifier = Modifier.height(220.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(profiles, key = { it.userId }) { profile ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(profile) }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(profile.displayName, style = MaterialTheme.typography.titleMedium)
                                Text("@${profile.handle}", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
