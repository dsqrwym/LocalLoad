package org.dsqrwym.localload

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.dsqrwym.localload.engine.LoadTestController
import org.dsqrwym.localload.engine.execution.KtorExecutor
import org.dsqrwym.localload.engine.http.executor.HttpConfig
import org.dsqrwym.localload.engine.http.executor.HttpProvider
import org.dsqrwym.localload.engine.scheduler.SchedulerImpl
import org.dsqrwym.localload.ui.LoadTestScreen
import org.dsqrwym.localload.ui.viewmodel.LoadTestViewModel

@Composable
@Preview
fun App() {
    MaterialTheme {
        val client = HttpProvider(HttpConfig()).client
        val executor = KtorExecutor(client)
        val scheduler = SchedulerImpl()
        val controller = LoadTestController(scheduler, executor)
        val viewModel = LoadTestViewModel(controller)
        LoadTestScreen(viewModel)
    }
}