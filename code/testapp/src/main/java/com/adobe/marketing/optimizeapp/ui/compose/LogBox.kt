package com.adobe.marketing.optimizeapp.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adobe.marketing.optimizeapp.R
import com.adobe.marketing.optimizeapp.models.LogEntry
import com.adobe.marketing.optimizeapp.viewmodels.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun LogBox(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel
) {
    var expanded by remember { mutableStateOf(false) }
    val logs = viewModel.logBoxManager.logs
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .background(color = Color(0xFF3949AB))
            .padding(8.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Logs",
                    fontSize = 18.sp,
                    color = Color.White,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                if (viewModel.logBoxManager.logs.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .clickable {
                                viewModel.logBoxManager.clearLogs()
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            modifier = Modifier
                                .padding(4.dp)
                                .size(14.dp),
                            painter = painterResource(R.drawable.ic_clear_logs),
                            tint = Color.White,
                            contentDescription = if (expanded) "Collapse" else "Expand"
                        )

                        Text(
                            text = "Clear Logs",
                            fontSize = 14.sp,
                            color = Color.White,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    val context = LocalContext.current
                    Row(
                        modifier = Modifier
                            .clickable {
                                viewModel.logBoxManager.shareLogs(context)
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            modifier = Modifier
                                .padding(4.dp)
                                .size(16.dp),
                            painter = painterResource(R.drawable.ic_share),
                            tint = Color.White,
                            contentDescription = if (expanded) "Collapse" else "Expand"
                        )

                        Text(
                            text = "Share",
                            fontSize = 14.sp,
                            color = Color.White,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))
                }

                Icon(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(24.dp)
                        .clickable {
                            viewModel.showLogs.value = false
                        },
                    painter = painterResource(R.drawable.ic_close),
                    tint = Color.White,
                    contentDescription = if (expanded) "Collapse" else "Expand"
                )
            }
            if (expanded && logs.isNotEmpty()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(logs.size) { index ->
                        LogView(logs[index])
                    }
                }

                LaunchedEffect(logs.size) {
                    if (logs.isNotEmpty()) {
                        coroutineScope.launch {
                            listState.animateScrollToItem(logs.size - 1)
                        }
                    }
                }
            } else if (logs.isEmpty()) {
                LogView(LogEntry("", "No logs available"))
            } else {
                LogView(logs.last(), 2)
            }
        }

        if (logs.isNotEmpty()) {
            Icon(
                modifier = Modifier
                    .background(
                        color = Color(0x50FFFFFF),
                        shape = CircleShape
                    )
                    .padding(4.dp)
                    .align(Alignment.BottomEnd)
                    .clickable {
                        expanded = !expanded
                    },
                painter = if (expanded)
                    painterResource(R.drawable.ic_compress)
                else
                    painterResource(R.drawable.ic_expand),
                tint = Color.White,
                contentDescription = if (expanded) "Collapse" else "Expand"
            )
        }
    }
}

@Composable
private fun LogView(log: LogEntry, maxLines: Int = Integer.MAX_VALUE) {
    Text(
        text = "${log.timestamp}: ${log.text}",
        color = Color.White,
        modifier = Modifier.padding(vertical = 2.dp),
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.body2,
        maxLines = maxLines
    )
}