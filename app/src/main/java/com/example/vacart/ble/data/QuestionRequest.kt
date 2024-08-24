package com.example.vacart.ble.data

import android.bluetooth.BluetoothDevice
import com.example.vacart.proto.OpCodeProto
import com.example.vacart.proto.RequestProto
import no.nordicsemi.android.ble.data.Data
import no.nordicsemi.android.ble.response.ReadResponse

/**
 * This class decodes the received packet using Protobuf.
 */
class Request : ReadResponse() {
    var userJoined: Players? = null
    var question: Question? = null
    var answerId: Int? = null
    var isGameOver: Boolean? = null
    var result: Results? = null
    var nameResult: NameResult? = null

    override fun onDataReceived(device: BluetoothDevice, data: Data) {
        val bytes = data.value!!
        val request = RequestProto.ADAPTER.decode(bytes)
        when (request.opCode) {
            OpCodeProto.PLAYERS -> { userJoined = request.players?.toPlayers() }
            OpCodeProto.NEW_QUESTION -> { question = request.question?.toQuestion() }
            OpCodeProto.RESPONSE -> { answerId = request.answerId }
            OpCodeProto.GAME_OVER -> { isGameOver = request.isGameOver }
            OpCodeProto.RESULT -> { result = request.results?.toResults() }
            OpCodeProto.ERROR -> { nameResult = NameResult(
                isEmptyName = request.isEmptyName,
                isDuplicateName = request.isDuplicateName,
            ) }
            else -> {}
        }
    }
}