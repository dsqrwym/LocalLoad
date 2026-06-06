package org.dsqrwym.localload.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.dsqrwym.localload.ui.viewmodel.LoadTestViewModel

@Composable
fun LoadTestScreen(viewModel: LoadTestViewModel) {

    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier.padding(16.dp)
    ) {

        Text("LocalLoad MVP")

        Spacer(Modifier.height(12.dp))

        TextField(
            value = state.url,
            onValueChange = { viewModel.onUrlChange(it) },
            placeholder = { Text("Enter URL") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        Row {
            Button(
                onClick = { viewModel.startTest() },
                enabled = !state.isRunning
            ) {
                Text("Start")
            }

            Spacer(Modifier.width(8.dp))

            Button(
                onClick = { viewModel.stopTest() },
                enabled = state.isRunning
            ) {
                Text("Stop")
            }
        }

        Spacer(Modifier.height(16.dp))

        Text("Status: ${if (state.isRunning) "RUNNING" else "IDLE"}")

        state.lastLatencyMs?.let {
            Text("Latency: ${it}ms")
        }

        state.lastSuccess?.let {
            Text("Success: $it")
        }

        state.message?.let {
            Spacer(Modifier.height(8.dp))
            Text(it)
        }
    }
}