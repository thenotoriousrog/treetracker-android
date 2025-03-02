package org.greenstand.android.TreeTracker.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavLanguage: () -> Unit,
    onNavOrg: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TreeTracker") },
                navigationIcon = {
                    Text(
                        text = "Org",
                        modifier = Modifier.clickable(onClick = onNavOrg)
                    )
                },
                actions = {
                    Text(
                        text = "Language",
                        modifier = Modifier.clickable(onClick = onNavLanguage)
                    )
                }
            )
        }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            Button(
                modifier = Modifier.padding(16.dp),
                onClick = { /*TODO*/ }) {
                Text(
                    text = "Upload",
                )
            }
            Button(
                modifier = Modifier.padding(16.dp),
                onClick = { /*TODO*/ }) {
                Text(
                    text = "Track",
                )
            }
            Button(
                modifier = Modifier.padding(16.dp),
                onClick = { /*TODO*/ }) {
                Text(
                    text = "Messages",
                )
            }
        }
    }
}