package com.android.mr.githubexplorer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A generic wrapper that ensures all screens respect the system bars (status bar/nav bar)
 * and applies standard screen padding.
 */
@Composable
fun GithubExplorerScaffold(
    topBar: @Composable () -> Unit = {},
    content: @Composable ColumnScope.(PaddingValues) -> Unit
) {
    Scaffold(
        topBar = topBar,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            content(innerPadding)
        }
    }
}
