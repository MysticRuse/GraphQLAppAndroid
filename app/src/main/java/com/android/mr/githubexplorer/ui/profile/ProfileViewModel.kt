package com.android.mr.githubexplorer.ui.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.mr.githubexplorer.graphql.FollowUserMutation
import com.android.mr.githubexplorer.graphql.GetUserProfileQuery
import com.android.mr.githubexplorer.graphql.UnfollowUserMutation
import com.android.mr.githubexplorer.graphql.fragment.RepoFields
import com.android.mr.githubexplorer.network.apolloClient
import com.apollographql.apollo.api.Optional
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Plain UI model - the screen never touches Apollo Generated types directly
data class RepoUiModel(
    val name: String,
    val description: String?,
    val stargazerCount: Int,
    val language: String?,
    val languageColor: String?,
    val url: String
)

// Single data class state (easier to do partial updates than a sealed interface
data class ProfileUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val userId: String = "",
    val login: String = "",
    val name: String? = null,
    val followerCount: Int = 0,
    val viewerIsFollowing: Boolean = false,
    val pinnedRepos: List<RepoUiModel> = emptyList(),
    val repos: List<RepoUiModel> = emptyList(),
    val hasNextPage: Boolean = false,
    val endCursor: String? = null,
    val isLoadingMore: Boolean = false,
    val isFollowLoading: Boolean = false
)

/**
 * Key concepts here:
 *
 * - Optional.Absent vs Optional.Present(cursor) — this is how Apollo distinguishes
 * "don't send this variable" from "send null". On first load you send no cursor;
 * on each page you send the endCursor from the previous response.
 * - SavedStateHandle["login"] — Compose Navigation automatically populates this from the nav route
 * profile/{login}. No boilerplate factory needed.
 * - RepoFields.toUiModel() — RepoFields is the interface Apollo generated from your fragment.
 * Both pinned repos (onRepository) and paginated repos implement it, so one extension function covers both.
 */
class ProfileViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    // Navigation passes the login as a nav argument — SavedStateHandle picks it up automatically
    private val login: String = checkNotNull(savedStateHandle["login"])

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()


    init { loadProfile() }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val response = apolloClient
                .query(GetUserProfileQuery(login = login, after = Optional.Absent))
                .execute()

            val user = response.data?.user
            if (user == null || response.hasErrors()) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = response.errors?.firstOrNull()?.message ?: "Failed to load profile"
                    )
                }
                return@launch
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    userId = user.id,
                    login = user.login,
                    name = user.name,
                    followerCount = user.followers.totalCount,
                    viewerIsFollowing = user.viewerIsFollowing,
                    pinnedRepos = user.pinnedItems.nodes
                        ?.filterNotNull()
                        ?.mapNotNull { node -> node.onRepository?.repoFields?.toUiModel() }
                        ?: emptyList(),
                    repos = user.repositories.nodes
                        ?.filterNotNull()
                        ?.map { repo -> repo.repoFields.toUiModel() }
                        ?: emptyList(),
                    hasNextPage = user.repositories.pageInfo.hasNextPage,
                    endCursor = user.repositories.pageInfo.endCursor
                )
            }
        }
    }

    // Called when the user taps "Load more" at the bottom of the repo list
    fun loadMore() {
        val current = _uiState.value
        if (!current.hasNextPage || current.isLoadingMore) return

        _uiState.update { it.copy(isLoadingMore = true) }

        viewModelScope.launch {
            val response = apolloClient
                .query(
                    GetUserProfileQuery(
                        login = login,
                        after = Optional.Present(current.endCursor)  // pass the cursor
                    )
                )
                .execute()

            val repos = response.data?.user?.repositories
            _uiState.update {
                it.copy(
                    isLoadingMore = false,
                    // Append new page to existing list
                    repos = it.repos + (repos?.nodes
                        ?.filterNotNull()
                        ?.map { repo -> repo.repoFields.toUiModel() }
                        ?: emptyList()),
                    hasNextPage = repos?.pageInfo?.hasNextPage ?: false,
                    endCursor = repos?.pageInfo?.endCursor
                )
            }
        }
    }

    // Called when the user taps the Follow button
    fun toggleFollow() {
        val current = _uiState.value
        if (current.isFollowLoading || current.userId.isEmpty()) return

        val wasFollowing = current.viewerIsFollowing
        val previousCount = current.followerCount

        // Optimistic Update - flip UI immediately, disable button to prevent double tap
        _uiState.update {
            it.copy(
                viewerIsFollowing = !wasFollowing,
                followerCount = if (wasFollowing) previousCount - 1 else previousCount + 1,
                isFollowLoading = true
            )
        }

        viewModelScope.launch {
            // Fire the correct mutation based on what the state WAS before we flipped it
            val hasErrors = if (wasFollowing) {
                val response = apolloClient
                    .mutation(UnfollowUserMutation(userId = current.userId))
                    .execute()
                response.hasErrors() || response.data == null
            } else {
                val response = apolloClient
                    .mutation(FollowUserMutation(userId = current.userId))
                    .execute()
                response.hasErrors() || response.data == null
            }

            _uiState.update {
                if (hasErrors) {
                    // Server rejected it - rollback to what it was before
                    it.copy(
                        viewerIsFollowing = wasFollowing,
                        followerCount = previousCount,
                        isFollowLoading = false,
                        error = if (wasFollowing) "Failed to unfollow" else "Failed to follow"
                    )
                } else {
                    it.copy(isFollowLoading = false)
                }
            }
        }
    }
}

// Map Apollo fragment type → plain UI model (keeps Compose screen free of Apollo types)
private fun RepoFields.toUiModel() = RepoUiModel(
    name = name,
    description = description,
    stargazerCount = stargazerCount,
    language = primaryLanguage?.name,
    languageColor = primaryLanguage?.color,
    url = url.toString()
)
