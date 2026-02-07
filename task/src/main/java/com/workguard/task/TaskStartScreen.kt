package com.workguard.task

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TaskStartScreen(
    state: TaskState,
    onStartClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Task Start")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onStartClick, enabled = !state.isLoading) {
            Text("Start Task")
        }
        if (state.errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(state.errorMessage)
        }
    }
}
