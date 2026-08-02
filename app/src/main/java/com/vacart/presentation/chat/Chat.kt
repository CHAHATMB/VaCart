package com.vacart.presentation.chat

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.vacart.ble.server.ServerViewModel
import no.nordicsemi.android.common.permissions.ble.RequireBluetooth

@Composable
fun Chat() {
    Text(text = "Chat Screen")
    RequireBluetooth {
        val serverViewModel: ServerViewModel = hiltViewModel()
        val serverViewState by serverViewModel.serverViewState.collectAsState()
    }
}