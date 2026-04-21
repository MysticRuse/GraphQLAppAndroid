package com.android.mr.githubexplorer.network

import androidx.compose.runtime.Applier
import com.android.mr.githubexplorer.BuildConfig
import com.apollographql.apollo.ApolloClient

val apolloClient: ApolloClient = ApolloClient.Builder()
    .serverUrl("https://api.github.com/graphql")
    .addHttpHeader("Authorization", "Bearer ${BuildConfig.GITHUB_TOKEN}")
    .build()