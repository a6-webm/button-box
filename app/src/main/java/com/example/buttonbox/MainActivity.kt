package com.example.buttonbox

import android.hardware.input.InputManager
import android.os.Bundle
import android.view.InputDevice
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.buttonbox.ui.theme.ButtonBoxTheme
import org.billthefarmer.mididriver.MidiDriver
import java.util.OptionalInt

class MainActivity : ComponentActivity(), MidiDriver.OnMidiStartListener, InputManager.InputDeviceListener {
    var inputManager = getSystemService(INPUT_SERVICE) as InputManager
    var devices: List<InputDevice> = emptyList()
    var selectedLeftDevice: InputDevice? = null
    var selectedRightDevice: InputDevice? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        generateDevicesList()
        enableEdgeToEdge()
        setContent {
            ButtonBoxTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    DeviceMenu(
                        "Select right-hand device",
                        Modifier.padding(innerPadding),
                        devices, // TODO does this recompose when changed? I assume not
                        onSelectDeviceId = { selectedLeftDevice = it }
                    )
                    Text (selectedLeftDevice?.name ?: "No device selected")
                    DeviceMenu(
                        "Select left-hand device",
                        Modifier.padding(innerPadding),
                        devices,
                        onSelectDeviceId = { selectedRightDevice = it }
                    )
                    Text (selectedRightDevice?.name ?: "No device selected")
                }
            }
        }
    }

    fun generateDevicesList() {
        devices = inputManager.inputDeviceIds.map { id -> inputManager.getInputDevice(id)!! }
    }

    override fun onMidiStart() {
//        for (i in 0..MainActivity.buttons.size) writeChange(MainActivity.change + i, instrument) // Set instrument
    }

    override fun onInputDeviceAdded(deviceId: Int) {
        generateDevicesList()
    }

    override fun onInputDeviceChanged(deviceId: Int) {
        generateDevicesList()
    }

    override fun onInputDeviceRemoved(deviceId: Int) {
        generateDevicesList()
    }
}

@Composable
fun DeviceMenu(
    text: String, modifier: Modifier = Modifier, devices: List<InputDevice>,
    onSelectDeviceId: (InputDevice) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedDeviceId by remember { mutableStateOf(OptionalInt::empty) }

    Box(modifier) {
        OutlinedButton(onClick = { expanded = !expanded }) { Text(text) }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            devices.forEach { device ->
                DropdownMenuItem(
                    text = { Text(device.name) },
                    onClick = { onSelectDeviceId(device) }
                )
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ButtonBoxTheme {
        Greeting("Android")
    }
}