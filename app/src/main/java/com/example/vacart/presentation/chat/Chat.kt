package com.example.vacart.presentation.chat

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.vacart.ble.server.ServerViewModel
import no.nordicsemi.android.common.permissions.ble.RequireBluetooth

@Composable
fun Chat() {
    Text(text = "Chat Screen")
    RequireBluetooth {
        val serverViewModel: ServerViewModel = hiltViewModel()
        val serverViewState by serverViewModel.serverViewState.collectAsState()
    }
}