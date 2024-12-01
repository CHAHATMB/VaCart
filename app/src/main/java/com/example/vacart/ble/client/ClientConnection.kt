package com.example.vacart.ble.client

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.util.Log
import com.example.vacart.ble.data.NameResult
import com.example.vacart.ble.data.Question
import com.example.vacart.ble.data.Request
import com.example.vacart.ble.data.Results
import com.example.vacart.proto.OpCodeProto
import com.example.vacart.proto.RequestProto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.ktx.asResponseFlow
import no.nordicsemi.android.ble.ktx.suspend
import com.example.vacart.ble.data.Players
import com.example.vacart.ble.spec.DeviceSpecifications
import com.example.vacart.ble.spec.PacketMerger
import com.example.vacart.ble.spec.PacketSplitter

class ClientConnection(
    context: Context,
    private val scope: CoroutineScope,
    private val device: BluetoothDevice,
) : BleManager(context) {
    private val TAG = ClientConnection::class.java.simpleName
    private var characteristic: BluetoothGattCharacteristic? = null

    private val _userJoined = MutableSharedFlow<Players>()
    val userJoined = _userJoined.asSharedFlow()
    private val _question = MutableSharedFlow<Question>()
    val question = _question.asSharedFlow()
    private val _answer = MutableSharedFlow<Int>()
    val answer = _answer.asSharedFlow()
    private val _isGameOver = MutableSharedFlow<Boolean>()
    val isGameOver = _isGameOver.asSharedFlow()
    private val _result = MutableSharedFlow<Results>()
    val result = _result.asSharedFlow()
    private val _nameResult = MutableSharedFlow<NameResult>()
    val error = _nameResult.asSharedFlow()

    override fun log(priority: Int, message: String) {
        Log.println(priority, TAG, message)
    }

    override fun getMinLogPriority(): Int {
        return Log.VERBOSE
    }

        override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
            // Return false if a required service has not been discovered.
            gatt.getService(DeviceSpecifications.UUID_SERVICE_DEVICE)?.let { service ->
                characteristic = service.getCharacteristic(DeviceSpecifications.UUID_MSG_CHARACTERISTIC)
            }
            return characteristic != null
        }

        /**
         * Initialize the device by enabling notifications.
         */
        @OptIn(ExperimentalCoroutinesApi::class)
        override fun initialize() {
            requestMtu(512).enqueue() // request Mtu-512

            setNotificationCallback(characteristic)
                // Merges packets until the entire text is present in the stream [PacketMerger.merge].
                .merge(PacketMerger())
                .asResponseFlow<Request>()
                .onEach {
                    it.nameResult?.let { error -> _nameResult.emit(error) }
                    it.userJoined?.let { userJoined -> _userJoined.emit(userJoined) }
                    it.question?.let { question -> _question.emit(question) }
                    it.answerId?.let { answer -> _answer.emit(answer) }
                    it.isGameOver?.let { isGameOver -> _isGameOver.emit(isGameOver) }
                    it.result?.let { results -> _result.emit(results) }
                }
                .launchIn(scope)
            enableNotifications(characteristic).enqueue()
        }

        /**
         * This method is called when the services get invalidated, i.e. when the device is disconnected.
         * When device is disconnected, the reference to the characteristics becomes null.
         */
        override fun onServicesInvalidated() {
            characteristic = null
        }

    /**
     * Connects to the server.
     */
    suspend fun connect() {
        connect(device)
            .retry(4, 300)
            .useAutoConnect(false)
            .timeout(100_000)
            .suspend()
    }

    /**
     * Send selected answer id. The data is split into MTU size packets using
     * packet splitter [PacketSplitter.chunk] before sending it to the server.
     */
    suspend fun sendSelectedAnswer(answer: Int) {
        val result = RequestProto(OpCodeProto.RESULT, answerId = answer)
        val resultByteArray = result.encode()
        writeCharacteristic(
            characteristic,
            resultByteArray,
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        )
        .split(PacketSplitter())
        .suspend()
    }

    /**
     * Send device name. The data is split into MTU size packets using
     * packet splitter [PacketSplitter.chunk] before sending it to the server.
     */
    suspend fun sendPlayersName(name: String) {
        val playersName = RequestProto(OpCodeProto.NAME, name = name)
        val deviceNameByteArray = playersName.encode()
        writeCharacteristic(
            characteristic,
            deviceNameByteArray,
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        )
        .split(PacketSplitter())
        .suspend()
    }

    fun release() {
        cancelQueue()
        disconnect().enqueue()
    }

}