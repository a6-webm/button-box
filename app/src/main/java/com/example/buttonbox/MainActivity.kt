package com.example.buttonbox

import android.hardware.input.InputManager
import android.os.Bundle
import android.view.InputDevice
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.MutableLiveData
import com.example.buttonbox.ui.theme.ButtonBoxTheme
import org.billthefarmer.mididriver.MidiDriver

class MainActivity : ComponentActivity(), MidiDriver.OnMidiStartListener, InputManager.InputDeviceListener {
    lateinit var inputManager: InputManager
    var devicesMLD: MutableLiveData<List<InputDevice>> = MutableLiveData(emptyList())
    var leftDeviceMLD: MutableLiveData<InputDevice?> = MutableLiveData(null)
    var rightDeviceMLD: MutableLiveData<InputDevice?> = MutableLiveData(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inputManager = getSystemService(INPUT_SERVICE) as InputManager
        generateDevicesList()
        enableEdgeToEdge()
        setContent {
            val leftDevice by leftDeviceMLD.observeAsState(null)
            val rightDevice by rightDeviceMLD.observeAsState(null)
            ButtonBoxTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column (modifier = Modifier.padding(innerPadding)) {
                        Row {
                            DeviceMenu(
                                "Select right-hand device",
                                modifier = Modifier.align(Alignment.CenterVertically),
                                devicesMLD = devicesMLD,
                                onSelectDeviceId = { leftDeviceMLD.value = it }
                            )
                            Text (
                                leftDevice?.name ?: "No device selected",
                                modifier = Modifier.align(Alignment.CenterVertically)
                                    .padding(horizontal = 10.dp)
                            )
                        }
                        Row {
                            DeviceMenu(
                                "Select left-hand device",
                                modifier = Modifier.align(Alignment.CenterVertically),
                                devicesMLD = devicesMLD,
                                onSelectDeviceId = { rightDeviceMLD.value = it }
                            )
                            Text (
                                rightDevice?.name ?: "No device selected",
                                modifier = Modifier.align(Alignment.CenterVertically)
                                    .padding(horizontal = 10.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    fun generateDevicesList() {
        // TODO filter by keyboards?
        devicesMLD.value = inputManager.inputDeviceIds.map { id -> inputManager.getInputDevice(id)!! }
    }

    override fun onMidiStart() { // TODO midi stuff innit
//        for (i in 0..MainActivity.buttons.size) writeChange(MainActivity.change + i, instrument) // Set instrument
    }

    override fun onInputDeviceAdded(deviceId: Int) {
        generateDevicesList()
    }

    override fun onInputDeviceChanged(deviceId: Int) {
        generateDevicesList()
    }

    override fun onInputDeviceRemoved(deviceId: Int) { // TODO de-select device when unplugged
        generateDevicesList()
    }
}

@Composable
fun DeviceMenu(
    text: String, modifier: Modifier = Modifier, devicesMLD: MutableLiveData<List<InputDevice>>,
    onSelectDeviceId: (InputDevice) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val devices by devicesMLD.observeAsState()

    Box(modifier) {
        OutlinedButton(onClick = { expanded = !expanded }) { Text(text) }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            devices?.forEach { device ->
                DropdownMenuItem(
                    text = { Text(device.name) },
                    onClick = {
                        onSelectDeviceId(device)
                        expanded = false
                    }
                )
            }
        }
    }
}