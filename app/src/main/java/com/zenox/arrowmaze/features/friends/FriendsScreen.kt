package com.zenox.arrowmaze.features.friends

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PersonSearch
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zenox.arrowmaze.R
import com.zenox.arrowmaze.core.designsystem.components.ArrowMazeIconButton
import com.zenox.arrowmaze.core.designsystem.components.EmptyState
import com.zenox.arrowmaze.core.designsystem.components.ErrorState
import com.zenox.arrowmaze.core.designsystem.components.LoadingState
import com.zenox.arrowmaze.core.designsystem.tokens.SpacingTokens
import com.zenox.arrowmaze.features.friends.components.FriendRequestRow
import com.zenox.arrowmaze.features.friends.components.FriendRow
import com.zenox.arrowmaze.features.friends.components.PlayerSearchResult

/**
 * Root composable for the Friends screen. Three tabs:
 *
 * - **Friends** — accepted friends list with remove/block per-row overflow.
 * - **Requests** — incoming (accept / decline) and outgoing (cancel).
 * - **Search** — query field + result rows with an Add button.
 *
 * The incoming/outgoing request count is rendered on the Requests tab as a
 * badge so the user immediately sees pending activity.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    viewModel: FriendsViewModel = hiltViewModel(),
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.uiEvents.collect { event ->
            val msg = when (event) {
                is FriendsUiEvent.ShowToast -> event.message
                FriendsUiEvent.RequestSent -> "Friend request sent"
                FriendsUiEvent.RequestAccepted -> "Friend request accepted"
                FriendsUiEvent.RequestDeclined -> "Friend request declined"
                FriendsUiEvent.FriendRemoved -> "Friend removed"
                FriendsUiEvent.PlayerBlocked -> "Player blocked"
            }
            snackbarHostState.showSnackbar(msg)
        }
    }

    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Friends",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    ArrowMazeIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back_button),
                        onClick = onBack,
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            val tabs = listOf("Friends", "Requests", "Search")
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                tabs.forEachIndexed { index, label ->
                    Tab(
                        selected = index == selectedTab,
                        onClick = { selectedTab = index },
                        text = { Text(label) },
                    )
                }
            }

            when (val state = uiState) {
                is FriendsUiState.Loading -> LoadingState(message = "Loading friends…")
                is FriendsUiState.Error -> ErrorState(message = state.message)
                is FriendsUiState.Success -> {
                    when (selectedTab) {
                        0 -> FriendsTab(state = state, viewModel = viewModel)
                        1 -> RequestsTab(state = state, viewModel = viewModel)
                        2 -> SearchTab(state = state, viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
private fun FriendsTab(
    state: FriendsUiState.Success,
    viewModel: FriendsViewModel,
) {
    if (state.friends.isEmpty()) {
        EmptyState(
            icon = Icons.Rounded.Group,
            title = "No friends yet",
            subtitle = "Search for players to add them as friends.",
        )
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = SpacingTokens.md,
            vertical = SpacingTokens.md,
        ),
        verticalArrangement = Arrangement.spacedBy(SpacingTokens.sm),
    ) {
        items(items = state.friends, key = { it.uid }) { friend ->
            FriendRow(
                friend = friend,
                onRemove = { viewModel.removeFriend(it.uid) },
                onBlock = { viewModel.blockPlayer(it.uid) },
            )
        }
        item { Spacer(Modifier.height(SpacingTokens.xxl)) }
    }
}

@Composable
private fun RequestsTab(
    state: FriendsUiState.Success,
    viewModel: FriendsViewModel,
) {
    val incoming = state.incomingRequests
    val outgoing = state.outgoingRequests
    if (incoming.isEmpty() && outgoing.isEmpty()) {
        EmptyState(
            icon = Icons.Rounded.PersonAdd,
            title = "No pending requests",
            subtitle = "When someone sends you a friend request, it'll appear here.",
        )
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = SpacingTokens.md,
            vertical = SpacingTokens.md,
        ),
        verticalArrangement = Arrangement.spacedBy(SpacingTokens.sm),
    ) {
        if (incoming.isNotEmpty()) {
            item(key = "incoming_header") {
                SectionLabel("Incoming (${incoming.size})")
            }
            items(items = incoming, key = { "in-${it.id}" }) { req ->
                FriendRequestRow(
                    request = req,
                    isIncoming = true,
                    onAccept = { viewModel.acceptRequest(it.id) },
                    onDecline = { viewModel.declineRequest(it.id) },
                )
            }
        }
        if (outgoing.isNotEmpty()) {
            item(key = "outgoing_header") {
                SectionLabel("Outgoing (${outgoing.size})")
            }
            items(items = outgoing, key = { "out-${it.id}" }) { req ->
                FriendRequestRow(
                    request = req,
                    isIncoming = false,
                    onAccept = { /* no-op for outgoing */ },
                    onDecline = { viewModel.declineRequest(it.id) },
                )
            }
        }
        item { Spacer(Modifier.height(SpacingTokens.xxl)) }
    }
}

@Composable
private fun SearchTab(
    state: FriendsUiState.Success,
    viewModel: FriendsViewModel,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(SpacingTokens.md),
        verticalArrangement = Arrangement.spacedBy(SpacingTokens.md),
    ) {
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = viewModel::search,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search by name or player id") },
            leadingIcon = { Icon(Icons.Rounded.PersonSearch, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
        )

        if (state.searchQuery.isBlank()) {
            EmptyState(
                icon = Icons.Rounded.PersonSearch,
                title = "Find your friends",
                subtitle = "Type a name or player id above to search.",
            )
        } else if (state.isSearching) {
            LoadingState(message = "Searching…")
        } else if (state.searchResults.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.PersonSearch,
                title = "No results",
                subtitle = "Try a different name or id.",
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(SpacingTokens.sm),
            ) {
                items(items = state.searchResults, key = { it.uid }) { player ->
                    PlayerSearchResult(
                        player = player,
                        onSendRequest = { viewModel.sendRequest(it.uid) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(vertical = SpacingTokens.xs),
    )
}
