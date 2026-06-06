package org.dsqrwym.localload.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.dsqrwym.localload.engine.metrics.TestReport
import org.dsqrwym.localload.ui.viewmodel.LoadTestViewModel
import org.dsqrwym.localload.util.roundToDisplay

@Composable
fun LoadTestScreen(
    viewModel: LoadTestViewModel
) {
    val state by viewModel.state.collectAsState()
    val report by viewModel.report.collectAsState()

    LazyColumn(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        item {
            Text("LocalLoad")
            Spacer(Modifier.height(16.dp))

            TextField(
                value = state.url,
                onValueChange = viewModel::onUrlChange,
                placeholder = { Text("https://example.com") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
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
        }

        item {
            StatusCard(title = "Status", value = if (state.isRunning) "RUNNING" else "IDLE")
        }

        item {
            MetricsCard(
                totalRequests = state.totalRequests,
                currentRps = state.currentRps,
                avgLatencyMs = state.avgLatencyMs,
                errorRate = state.errorRate
            )
            Spacer(Modifier.height(12.dp))

            state.lastLatencyMs?.let {
                Text("Last Latency: $it ms")
            }

            state.message?.let {
                Text(it)
            }
        }
        report?.let {
            item {
                ReportScreen(
                    report = it,
                    onClose = {
                        viewModel.clearReport()
                    }
                )
            }
        }
    }
}

@Composable
private fun MetricsCard(
    totalRequests: Long,
    currentRps: Double,
    avgLatencyMs: Double,
    errorRate: Double
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Metrics")
            HorizontalDivider()
            Text("Total Requests: $totalRequests")
            Text("Current RPS: ${currentRps.roundToDisplay()}")
            Text("Average Latency: ${avgLatencyMs.roundToDisplay()} ms")
            Text("Error Rate: ${errorRate.roundToDisplay()} %")
        }
    }
}

@Composable
private fun StatusCard(
    title: String,
    value: String
) {

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(title)
            Spacer(Modifier.height(4.dp))
            Text(value)
        }
    }
}

@Composable
private fun ReportCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )

            HorizontalDivider()

            content()
        }
    }
}

@Composable
fun ReportScreen(
    report: TestReport,
    onClose: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Text(
            text = "Test Report",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(8.dp))

        ReportCard(title = "Latency") {
            ReportRow("P50", "${report.p50LatencyMs} ms")
            ReportRow("P95", "${report.p95LatencyMs} ms")
            ReportRow("P99", "${report.p99LatencyMs} ms")
            ReportRow("Avg", "${report.avgLatencyMs.roundToDisplay()} ms")
            ReportRow("Max", "${report.maxLatencyMs} ms")
        }

        ReportCard(title = "Throughput") {
            ReportRow("Avg RPS", report.avgRps.roundToDisplay())
            ReportRow("Max RPS", report.maxRps.toString())
            ReportRow("Min RPS", report.minRps.toString())
        }

        ReportCard(title = "Errors") {
            ReportRow("Success Rate", "${(report.successRate * 100).roundToDisplay()} %")
            ReportRow("Error Rate", "${(report.errorRate * 100).roundToDisplay()} %")
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Close")
        }
    }
}

@Composable
private fun ReportRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        Text(value)
    }
}