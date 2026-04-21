package com.android.mr.githubexplorer.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.mr.githubexplorer.graphql.SearchUserQuery
import com.android.mr.githubexplorer.ui.components.GithubExplorerScaffold

@Composable
fun SearchScreen(
    onNavigateToProfile: (login: String) -> Unit,
    viewModel: SearchViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var loginInput by remember { mutableStateOf("") }

    GithubExplorerScaffold {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "GitHub Explorer",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = loginInput,
                onValueChange = { loginInput = it },
                label = { Text("GitHub username") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { viewModel.search(loginInput.trim()) },
                modifier = Modifier.fillMaxWidth(),
                enabled = loginInput.isNotBlank()
            ) {
                Text("Search")
            }

            Spacer(modifier = Modifier.height(24.dp))

            when (val state = uiState) {
                is SearchUiState.Idle -> Unit
                is SearchUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }
                is SearchUiState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                is SearchUiState.Success -> {
                    UserResultCard(
                        user = state.user,
                        onClick = { onNavigateToProfile(state.user.login) }
                    )
                }
            }
        }
    }
}

@Composable
private fun UserResultCard(
    user: SearchUserQuery.User,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = user.name ?: user.login,
                style = MaterialTheme.typography.titleLarge
            )
            user.bio?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = it, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                Text("Followers: ${user.followers.totalCount}")
                Spacer(modifier = Modifier.width(16.dp))
                Text("Following: ${user.following.totalCount}")
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tap to view profile →",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
