package com.workguard.attendance

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
fun AttendanceScreen(
    state: AttendanceState,
    onCheckInClick: () -> Unit,
    onCheckOutClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.user),
            contentDescription = null,
            modifier = Modifier.size(140.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("Attendance")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onCheckInClick, enabled = !state.isLoading) {
            Text("Check In")
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onCheckOutClick, enabled = !state.isLoading) {
            Text("Check Out")
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text("Status: ${state.lastStatus}")
        if (state.errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(state.errorMessage)
        }
    }
}
