package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.PostMediaType
import com.example.data.model.PostVisibility
import com.example.data.model.UiState
import com.example.ui.theme.EmeraldMint
import com.example.ui.viewmodel.NovaraViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreatePostScreen(
    viewModel: NovaraViewModel,
    onPostPublished: () -> Unit
) {
    val context = LocalContext.current
    val aiAssistState by viewModel.aiPostAssistState.collectAsStateWithLifecycle()

    var mediaType by remember { mutableStateOf(PostMediaType.TEXT) }
    var visibility by remember { mutableStateOf(PostVisibility.PUBLIC) }
    var content by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Tech") }
    var tagsInput by remember { mutableStateOf("") }
    var soundTrackTitle by remember { mutableStateOf("") }
    var videoDurationInput by remember { mutableStateOf("45") }
    var collaboratorHandle by remember { mutableStateOf("") }
    var pollOption1 by remember { mutableStateOf("") }
    var pollOption2 by remember { mutableStateOf("") }
    var pollOption3 by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var usedAiAssist by remember { mutableStateOf(false) }

    val categories = listOf("Tech", "Design", "Creator", "Music", "Education", "Lifestyle")

    // Zero-permission Android Photo Picker (Google Play policy compliant)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            selectedImageUri = uri
            if (uri != null && mediaType == PostMediaType.TEXT) {
                mediaType = PostMediaType.PHOTO
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Novara Creator Studio",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Publish multi-format stories, shorts, polls, and AI-enhanced creations.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Format Selector
        Text("Format", style = MaterialTheme.typography.labelLarge)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PostMediaType.entries.forEach { type ->
                FilterChip(
                    selected = mediaType == type,
                    onClick = { mediaType = type },
                    label = { Text(type.label) },
                    modifier = Modifier.testTag("create_format_${type.name.lowercase()}")
                )
            }
        }

        // Photo Picker trigger for Photo or general posts
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier.testTag("pick_photo_button")
            ) {
                Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (selectedImageUri == null) "Attach Photo" else "Change Photo")
            }
            if (selectedImageUri != null) {
                OutlinedButton(onClick = { selectedImageUri = null }) {
                    Text("Remove")
                }
            }
        }

        selectedImageUri?.let { uri ->
            AsyncImage(
                model = uri,
                contentDescription = "Selected post media preview",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )
        }

        // Post Content Input
        OutlinedTextField(
            value = content,
            onValueChange = { content = it.take(2000) },
            label = { Text("Share your story, insight, or video caption...") },
            minLines = 4,
            maxLines = 8,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("create_post_content_input")
        )

        // Novara AI Co-Creator Panel
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Novara AI Co-Creator", style = MaterialTheme.typography.titleMedium)
                    }
                    Button(
                        onClick = {
                            val bmp = selectedImageUri?.let { loadScaledBitmap(context, it) }
                            viewModel.runAiPostAssist(content, mediaType.label, bmp)
                        },
                        modifier = Modifier.testTag("ai_cocreator_button")
                    ) {
                        Text("Enhance")
                    }
                }

                when (val state = aiAssistState) {
                    is UiState.Loading -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Analyzing draft & media with Gemini 3.8 Flash...")
                        }
                    }
                    is UiState.Error -> {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                    }
                    is UiState.Success -> {
                        val res = state.data
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (res.safeToPublish) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                                contentDescription = null,
                                tint = if (res.safeToPublish) EmeraldMint else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(res.safetyNote, style = MaterialTheme.typography.bodyMedium)
                        }
                        Text(
                            "Suggested Caption: ${res.caption}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "Hashtags: ${res.hashtags.joinToString(" ") { "#$it" }}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Button(
                            onClick = {
                                content = res.caption
                                category = res.category
                                tagsInput = res.hashtags.joinToString(", ")
                                usedAiAssist = true
                                viewModel.clearAiPostAssist()
                            }
                        ) {
                            Text("Apply AI Suggestion")
                        }
                    }
                    null -> {
                        Text(
                            "Tap Enhance for AI captioning, multimodal photo analysis, smart hashtags, and safety check.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Video Metadata
        AnimatedVisibility(visible = mediaType == PostMediaType.SHORT_VIDEO || mediaType == PostMediaType.LONG_VIDEO) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = soundTrackTitle,
                    onValueChange = { soundTrackTitle = it.take(120) },
                    label = { Text("Licensed Sound / Music Track") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = videoDurationInput,
                    onValueChange = { videoDurationInput = it.filter { ch -> ch.isDigit() }.take(5) },
                    label = { Text("Duration (seconds)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Poll Builder
        AnimatedVisibility(visible = mediaType == PostMediaType.POLL) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Poll Choices", style = MaterialTheme.typography.labelLarge)
                OutlinedTextField(
                    value = pollOption1,
                    onValueChange = { pollOption1 = it.take(80) },
                    label = { Text("Option 1") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = pollOption2,
                    onValueChange = { pollOption2 = it.take(80) },
                    label = { Text("Option 2") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = pollOption3,
                    onValueChange = { pollOption3 = it.take(80) },
                    label = { Text("Option 3 (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Category & Tags & Collab
        Text("Category", style = MaterialTheme.typography.labelLarge)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            categories.forEach { cat ->
                FilterChip(
                    selected = category == cat,
                    onClick = { category = cat },
                    label = { Text(cat) }
                )
            }
        }

        OutlinedTextField(
            value = tagsInput,
            onValueChange = { tagsInput = it },
            label = { Text("Hashtags (comma separated)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = collaboratorHandle,
            onValueChange = { collaboratorHandle = it.take(32) },
            label = { Text("Collaborator @handle (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Text("Visibility", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PostVisibility.entries.forEach { vis ->
                FilterChip(
                    selected = visibility == vis,
                    onClick = { visibility = vis },
                    label = { Text(vis.label) }
                )
            }
        }

        Button(
            onClick = {
                val parsedTags = tagsInput.split(",", " ")
                    .map { it.trim().removePrefix("#") }
                    .filter { it.isNotBlank() }
                val pollOpts = if (mediaType == PostMediaType.POLL) {
                    listOf(pollOption1, pollOption2, pollOption3).filter { it.isNotBlank() }
                } else emptyList()
                val duration = if (mediaType == PostMediaType.SHORT_VIDEO || mediaType == PostMediaType.LONG_VIDEO) {
                    videoDurationInput.toLongOrNull() ?: 30L
                } else 0L

                viewModel.createPost(
                    content = content,
                    mediaType = mediaType.name,
                    visibility = visibility.name,
                    category = category,
                    tags = parsedTags,
                    mediaUrl = selectedImageUri?.toString(),
                    videoDurationSec = duration,
                    soundTrackTitle = soundTrackTitle.ifBlank { null },
                    pollOptions = pollOpts,
                    collaboratorHandle = collaboratorHandle.ifBlank { null },
                    aiGenerated = usedAiAssist,
                    onPublished = {
                        content = ""
                        tagsInput = ""
                        selectedImageUri = null
                        usedAiAssist = false
                        onPostPublished()
                    }
                )
            },
            enabled = content.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("publish_post_button"),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Rounded.Send, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Publish to Novara", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(16.dp))
    }
}

private fun loadScaledBitmap(context: Context, uri: Uri): Bitmap? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val raw = BitmapFactory.decodeStream(stream) ?: return null
            val maxDim = 1024
            if (raw.width <= maxDim && raw.height <= maxDim) {
                raw
            } else {
                val ratio = minOf(maxDim.toFloat() / raw.width, maxDim.toFloat() / raw.height)
                Bitmap.createScaledBitmap(
                    raw,
                    (raw.width * ratio).toInt().coerceAtLeast(1),
                    (raw.height * ratio).toInt().coerceAtLeast(1),
                    true
                )
            }
        }
    } catch (_: Exception) {
        null
    }
}
