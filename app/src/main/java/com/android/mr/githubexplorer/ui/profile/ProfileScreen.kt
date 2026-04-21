package com.android.mr.githubexplorer.ui.profile

import android.graphics.Color.parseColor
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.mr.githubexplorer.ui.components.GithubExplorerScaffold
import androidx.core.graphics.toColorInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    GithubExplorerScaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.login) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            uiState.error != null -> {
                Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // User header
                    item {
                        UserHeader(
                            uiState = uiState,
                            onFollowClick = { viewModel.toggleFollow() }
                        )
                    }

                    // Pinned repos section
                    if (uiState.pinnedRepos.isNotEmpty()) {
                        item {
                            Text(
                                "Pinned",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        items(uiState.pinnedRepos) { repo ->
                            RepoCard(repo = repo)
                        }
                    }

                    // Repositories section
                    item {
                        Text(
                            "Repositories",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(uiState.repos) { repo ->
                        RepoCard(repo = repo)
                    }

                    // Pagination footer
                    item {
                        when {
                            uiState.isLoadingMore -> {
                                Box(
                                    Modifier.fillMaxWidth().padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                            uiState.hasNextPage -> {
                                Button(
                                    onClick = { viewModel.loadMore() },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Load more")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserHeader(uiState: ProfileUiState, onFollowClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        uiState.name?.let {
            Text(it, style = MaterialTheme.typography.headlineSmall)
        }
        Text("Followers: ${uiState.followerCount}")

        Button(
            onClick = onFollowClick,
            enabled = !uiState.isFollowLoading
        ) {
            if (uiState.isFollowLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(if (uiState.viewerIsFollowing) "Unfollow" else "Follow")
            }
        }

        HorizontalDivider()
    }
}

@Composable
private fun RepoCard(repo: RepoUiModel) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(repo.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            repo.description?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("★ ${repo.stargazerCount}", style = MaterialTheme.typography.labelMedium)
                repo.language?.let {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Language colour dot
                        repo.languageColor?.let { hex ->
                            Surface(
                                modifier = Modifier.size(10.dp),
                                shape = MaterialTheme.shapes.small,
                                color = runCatching {
                                    Color(hex.toColorInt())
                                }.getOrDefault(MaterialTheme.colorScheme.primary)
                            ) {}
                        }
                        Text(it, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
