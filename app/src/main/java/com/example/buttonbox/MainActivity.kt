package com.example.buttonbox

import android.hardware.input.InputManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.buttonbox.ui.theme.ButtonBoxTheme
import org.billthefarmer.mididriver.MidiDriver

class MainActivity : ComponentActivity(), MidiDriver.OnMidiStartListener, InputManager.InputDeviceListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ButtonBoxTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onMidiStart() {
//        for (i in 0..MainActivity.buttons.size) writeChange(MainActivity.change + i, instrument) // Set instrument
    }

    override fun onInputDeviceAdded(deviceId: Int) {
        TODO("Not yet implemented")
    }

    override fun onInputDeviceChanged(deviceId: Int) {
        TODO("Not yet implemented")
    }

    override fun onInputDeviceRemoved(deviceId: Int) {
        TODO("Not yet implemented")
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