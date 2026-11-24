package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CreatorTier
import com.example.data.model.PublicProfile
import com.example.data.model.UiState
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldMint
import com.example.ui.theme.PrismViolet
import com.example.ui.viewmodel.NovaraViewModel

@Composable
fun ProfileCreatorScreen(
    viewModel: NovaraViewModel,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val myProfileState by viewModel.myProfileState.collectAsStateWithLifecycle()
    val postsState by viewModel.postsState.collectAsStateWithLifecycle()
    val savedState by viewModel.savedPostsState.collectAsStateWithLifecycle()
    val tipsState by viewModel.receivedTipsState.collectAsStateWithLifecycle()
    val reportsState by viewModel.reportsState.collectAsStateWithLifecycle()
    val publicProfilesState by viewModel.publicProfilesState.collectAsStateWithLifecycle()
    val coachState by viewModel.coachResponse.collectAsStateWithLifecycle()
    val exportedJson by viewModel.exportedJson.collectAsStateWithLifecycle()

    var selectedSection by remember { mutableIntStateOf(0) } // 0=Studio/Economy, 1=Saved, 2=Privacy/Admin
    var showEditProfile by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var coachPrompt by remember { mutableStateOf("How should I grow my audience and subscriber conversion this week?") }

    val profile = (myProfileState as? UiState.Success)?.data
    val myPosts = (postsState as? UiState.Success)?.data?.filter { it.authorId == viewModel.userId }.orEmpty()
    val savedPosts = (savedState as? UiState.Success)?.data.orEmpty()
    val tips = (tipsState as? UiState.Success)?.data.orEmpty()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header Profile Card
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(ElectricCyan, PrismViolet))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = profile?.avatarInitials ?: "NV",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = profile?.displayName ?: "Novara Creator",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                if (profile?.verifiedBadge == true) {
                                    Spacer(Modifier.width(6.dp))
                                    Icon(
                                        Icons.Rounded.Verified,
                                        contentDescription = "Verified",
                                        tint = ElectricCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Text(
                                text = "@${profile?.handle ?: "creator"} • ${profile?.creatorTier ?: "CREATOR"}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = { showEditProfile = true },
                            modifier = Modifier.testTag("edit_profile_button")
                        ) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Edit profile")
                        }
                        IconButton(
                            onClick = onSignOut,
                            modifier = Modifier.testTag("sign_out_button")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = "Sign out")
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = profile?.bio ?: "Loading creator bio...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatBadge("${myPosts.size}", "Posts")
                        StatBadge("${profile?.followersCount ?: 0}", "Followers")
                        StatBadge("${profile?.followingCount ?: 0}", "Following")
                        StatBadge(
                            "$${String.format("%.2f", (profile?.tipsEarnedCents ?: 0L) / 100.0)}",
                            "Earned"
                        )
                    }
                }
            }
        }

        // Hub Tabs
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedSection == 0,
                    onClick = { selectedSection = 0 },
                    label = { Text("Creator & AI") }
                )
                FilterChip(
                    selected = selectedSection == 1,
                    onClick = { selectedSection = 1 },
                    label = { Text("Saved (${savedPosts.size})") }
                )
                FilterChip(
                    selected = selectedSection == 2,
                    onClick = { selectedSection = 2 },
                    label = { Text(if (viewModel.isCurrentUserAdmin) "Privacy & Admin" else "Privacy") }
                )
            }
        }

        if (selectedSection == 0) {
            // Creator Economy Dashboard
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.MonetizationOn, contentDescription = null, tint = EmeraldMint)
                            Spacer(Modifier.width(8.dp))
                            Text("Creator Economy (95% / 5% Split)", style = MaterialTheme.typography.titleMedium)
                        }
                        Text(
                            "Subscription Tier: $${String.format("%.2f", profile?.subscriptionPriceUsd ?: 4.99)}/mo • Total Net Tips: $${String.format("%.2f", (profile?.tipsEarnedCents ?: 0L) / 100.0)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = EmeraldMint
                        )
                        if (tips.isEmpty()) {
                            Text(
                                "No tips received yet. Publish engaging posts or shorts to earn digital gifts from supporters.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            tips.take(5).forEach { tip ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("${tip.senderName} (${tip.giftType})", style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            "+$${String.format("%.2f", tip.creatorNetCents / 100.0)}",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = EmeraldMint
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // AI Creator Coach
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.width(8.dp))
                            Text("AI Creator Coach (Gemini 3.8 Flash)", style = MaterialTheme.typography.titleMedium)
                        }
                        OutlinedTextField(
                            value = coachPrompt,
                            onValueChange = { coachPrompt = it },
                            label = { Text("Ask your AI coach") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = { viewModel.askCreatorCoach(coachPrompt) },
                            modifier = Modifier.testTag("ask_creator_coach_button")
                        ) {
                            Text("Analyze My Performance")
                        }
                        when (val c = coachState) {
                            is UiState.Loading -> CircularProgressIndicator(modifier = Modifier.size(22.dp))
                            is UiState.Error -> Text(c.message, color = MaterialTheme.colorScheme.error)
                            is UiState.Success -> Text(c.data, style = MaterialTheme.typography.bodyMedium)
                            null -> {}
                        }
                    }
                }
            }
        } else if (selectedSection == 1) {
            if (savedPosts.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Rounded.Bookmark, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(8.dp))
                            Text("No saved posts yet", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            } else {
                items(savedPosts, key = { it.postId }) { saved ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                "${saved.authorName} • ${saved.mediaType}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(saved.previewText, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        } else {
            // Privacy & Data Export & Deletion
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Security, Privacy & Data Rights", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Signed in as: ${viewModel.userEmail ?: viewModel.userId}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = { viewModel.exportMyData() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Export My Data (JSON)")
                        }
                        Button(
                            onClick = { showDeleteConfirm = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Delete Account Data")
                        }
                    }
                }
            }

            // Admin Moderation Console (for Verified Admin)
            if (viewModel.isCurrentUserAdmin) {
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.AdminPanelSettings, contentDescription = null, tint = ElectricCyan)
                                Spacer(Modifier.width(8.dp))
                                Text("Admin Moderation & Verification", style = MaterialTheme.typography.titleMedium)
                            }
                            val reports = (reportsState as? UiState.Success)?.data.orEmpty()
                            Text("Moderation Reports (${reports.size})", style = MaterialTheme.typography.labelLarge)
                            reports.take(5).forEach { rep ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("${rep.targetType}: ${rep.reason} [${rep.status}]", style = MaterialTheme.typography.bodyMedium)
                                        if (rep.status == "OPEN") {
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                TextButton(onClick = { viewModel.resolveReportAsAdmin(rep.id, "RESOLVED") }) {
                                                    Text("Resolve")
                                                }
                                                TextButton(onClick = { viewModel.resolveReportAsAdmin(rep.id, "DISMISSED") }) {
                                                    Text("Dismiss")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            val creators = (publicProfilesState as? UiState.Success)?.data.orEmpty()
                            Text("Creator Verification", style = MaterialTheme.typography.labelLarge)
                            creators.take(5).forEach { creator ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("@${creator.handle}", style = MaterialTheme.typography.bodyMedium)
                                    TextButton(onClick = { viewModel.verifyProfileAsAdmin(creator.userId, !creator.verifiedBadge) }) {
                                        Text(if (creator.verifiedBadge) "Revoke Badge" else "Verify")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditProfile && profile != null) {
        EditProfileDialog(
            profile = profile,
            onDismiss = { showEditProfile = false },
            onSave = { name, handle, bio, tier, price ->
                viewModel.updateProfile(name, handle, bio, profile.interests, tier, price)
                showEditProfile = false
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Account Data?") },
            text = { Text("This deletes your private and public Novara profile records and signs you out.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteAccountData(onDeleted = onSignOut)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    exportedJson?.let { json ->
        AlertDialog(
            onDismissRequest = { viewModel.clearExportedJson() },
            title = { Text("Your Novara Data Export") },
            text = { Text(json.take(1200), style = MaterialTheme.typography.labelSmall) },
            confirmButton = {
                Button(onClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/json"
                        putExtra(Intent.EXTRA_TEXT, json)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Export Novara JSON"))
                    viewModel.clearExportedJson()
                }) { Text("Share JSON") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.clearExportedJson() }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun StatBadge(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EditProfileDialog(
    profile: PublicProfile,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, Double) -> Unit
) {
    var name by remember { mutableStateOf(profile.displayName) }
    var handle by remember { mutableStateOf(profile.handle) }
    var bio by remember { mutableStateOf(profile.bio) }
    var tier by remember { mutableStateOf(profile.creatorTier) }
    var price by remember { mutableStateOf(profile.subscriptionPriceUsd.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Customize Profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(60) },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = handle,
                    onValueChange = { handle = it.take(32) },
                    label = { Text("Handle") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it.take(300) },
                    label = { Text("Bio") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Subscription Price ($ USD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(CreatorTier.STANDARD, CreatorTier.CREATOR).forEach { t ->
                        FilterChip(
                            selected = tier == t.name,
                            onClick = { tier = t.name },
                            label = { Text(t.label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(name, handle, bio, tier, price.toDoubleOrNull() ?: 4.99)
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
