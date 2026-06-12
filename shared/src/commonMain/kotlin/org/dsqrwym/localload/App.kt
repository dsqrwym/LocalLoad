package org.dsqrwym.localload

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import io.github.composefluent.FluentTheme
import org.dsqrwym.localload.engine.LoadTestController
import org.dsqrwym.localload.engine.scheduler.SchedulerImpl
import org.dsqrwym.localload.ui.LoadTestScreen
import org.dsqrwym.localload.ui.viewmodel.LoadTestViewModel

@Composable
@Preview
fun App() {
    FluentTheme {
        val scheduler = SchedulerImpl()
        val controller = LoadTestController(scheduler)
        val viewModel = LoadTestViewModel(controller)
        LoadTestScreen(viewModel)
    }
}