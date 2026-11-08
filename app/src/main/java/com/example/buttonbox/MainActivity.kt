package com.example.buttonbox

import android.hardware.input.InputManager
import android.os.Bundle
import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
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

typealias ButtonDeviceId = Int
typealias Keycode = Int
typealias Note = Int
typealias NoteMap = Array<HashMap<Keycode, Array<Note>>>

enum class MidiCode(val v: Int) {
    NOTE_ON(0x90),
    NOTE_OFF(0x80),
    CHANGE(0xC0)
}

const val INSTRUMENT: Int = 0
const val VELOCITY: Int = 96

// TODO icl, I don't know if I like any of the instruments on this synth, look for another one?

class MainActivity : ComponentActivity(), MidiDriver.OnMidiStartListener, InputManager.InputDeviceListener {
    lateinit var inputManager: InputManager
    lateinit var midi: MidiDriver
    var settingsOpenMLD: MutableLiveData<Boolean> = MutableLiveData(true)
    var devicesMLD: MutableLiveData<List<InputDevice>> = MutableLiveData(emptyList())
    var leftDeviceMLD: MutableLiveData<InputDevice?> = MutableLiveData(null)
    var rightDeviceMLD: MutableLiveData<InputDevice?> = MutableLiveData(null)
    var noteMap: NoteMap = offsetMap(usNoteMap(), 12) // TODO have this be selectable ig
    var notePressed: Array<Int> = Array(128, init = { 0 })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inputManager = getSystemService(INPUT_SERVICE) as InputManager
        inputManager.registerInputDeviceListener(this, null)
        generateDevicesList()
        midi = MidiDriver.getInstance()
        midi.setOnMidiStartListener(this)
        enableEdgeToEdge()
        setContent {
            val leftDevice by leftDeviceMLD.observeAsState(null)
            val rightDevice by rightDeviceMLD.observeAsState(null)
            val settingsOpen by settingsOpenMLD.observeAsState(false)
            ButtonBoxTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (!settingsOpen) Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Tap anywhere on the screen to open settings")
                    }
                    else Column (modifier = Modifier.padding(innerPadding)) {
                        Row {
                            OutlinedButton(onClick = { settingsOpenMLD.value = false }) { Text("Close settings") }
                        }
                        Row {
                            DeviceMenu(
                                "Select right-hand device",
                                modifier = Modifier.align(Alignment.CenterVertically),
                                devicesMLD = devicesMLD,
                                onSelectDeviceId = { rightDeviceMLD.value = it }
                            )
                            Text (
                                rightDevice?.name ?: "No device selected",
                                modifier = Modifier
                                    .align(Alignment.CenterVertically)
                                    .padding(horizontal = 10.dp)
                            )
                        }
                        Row {
                            DeviceMenu(
                                "Select left-hand device",
                                modifier = Modifier.align(Alignment.CenterVertically),
                                devicesMLD = devicesMLD,
                                onSelectDeviceId = { leftDeviceMLD.value = it }
                            )
                            Text (
                                leftDevice?.name ?: "No device selected",
                                modifier = Modifier
                                    .align(Alignment.CenterVertically)
                                    .padding(horizontal = 10.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    protected override fun onResume() {
        super.onResume()
        midi.start()
    }

    protected override fun onPause() {
        super.onPause()
        midi.stop()
    }

    fun writeMidiChange(c: Int, data: Int): Boolean {
        val msg = ByteArray(2)
        msg[0] = (MidiCode.CHANGE.v + c).toByte()
        msg[1] = data.toByte()
        return midi.write(msg)
    }

    fun writeMidiNote(isOn: Boolean, c: Int, note: Note, vel: Int): Boolean {
        val msg = ByteArray(3)
        val code = if (isOn) MidiCode.NOTE_ON.v else MidiCode.NOTE_OFF.v
        msg[0] = (code + c).toByte()
        msg[1] = note.toByte()
        msg[2] = vel.toByte()
        return midi.write(msg)
    }

    fun generateDevicesList() {
        devicesMLD.value = inputManager.inputDeviceIds
            .map { id -> inputManager.getInputDevice(id)!! }
            .filter { device -> device.keyboardType == InputDevice.KEYBOARD_TYPE_ALPHABETIC }
        // Potentially over-filtering
    }

    override fun onMidiStart() {
        writeMidiChange(0, INSTRUMENT) // Set instrument
        // TODO be able to pick different instruments?
    }

    override fun onInputDeviceAdded(deviceId: Int) {
        generateDevicesList()
    }

    override fun onInputDeviceChanged(deviceId: Int) {
        generateDevicesList()
    }

    override fun onInputDeviceRemoved(deviceId: Int) {
        if (deviceId == leftDeviceMLD.value?.id) {
            leftDeviceMLD.value = null
        }
        if (deviceId == rightDeviceMLD.value?.id) {
            rightDeviceMLD.value = null
        }
        generateDevicesList()
    }

    fun getButtonDeviceId(event: KeyEvent): ButtonDeviceId {
        if (event.deviceId == leftDeviceMLD.value?.id)  { return 0 }
        if (event.deviceId == rightDeviceMLD.value?.id) { return 1 }
        return -1
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        super.onTouchEvent(event)
        if (event.actionMasked == MotionEvent.ACTION_MOVE) settingsOpenMLD.value = true
        return false
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
//        Log.d("DBG", "keycode:" + keyCode + " device:" + event.device.name
//                + " rep:" + event.repeatCount)
        val btDevId = getButtonDeviceId(event)
        if (btDevId == -1) { return false }
        if (event.repeatCount > 0) { return true }
        noteMap[btDevId][keyCode]?.forEach { onNoteDown(it) }
        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        val btDevId = getButtonDeviceId(event)
        if (btDevId == -1) { return false }
        noteMap[btDevId][keyCode]?.forEach { onNoteUp(it) }
        return true
    }

    fun onNoteDown(note: Note) {
        if (notePressed[note] == 0) { writeMidiNote(true, 0, note, VELOCITY) }
        notePressed[note] += 1
    }

    fun onNoteUp(note: Note) {
        notePressed[note] -= 1
        if (notePressed[note] < 0) { notePressed[note] = 0 }
        if (notePressed[note] == 0) { writeMidiNote(false, 0, note, VELOCITY) }
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

fun offsetMap(noteMap: NoteMap, offset: Int): NoteMap {
    val newNoteMap = noteMap.copyOf()
    newNoteMap.forEach {
        deviceMap -> deviceMap.forEach {
            (_, notes) -> notes.forEachIndexed {
                i, _ -> notes[i] += offset
            }
        }
    }
    return newNoteMap
}

fun ukNoteMap(): NoteMap {
    TODO()
}

fun usNoteMap(): NoteMap {
    return arrayOf(
        hashMapOf( // Left-hand device
                                               KeyEvent.KEYCODE_BACKSLASH to arrayOf(10),
            KeyEvent.KEYCODE_DEL to arrayOf(11),                                  KeyEvent.KEYCODE_ENTER to arrayOf(12),
                                               KeyEvent.KEYCODE_RIGHT_BRACKET to arrayOf(13),                       KeyEvent.KEYCODE_SHIFT_RIGHT to arrayOf(14),
            KeyEvent.KEYCODE_EQUALS to arrayOf(14),                               KeyEvent.KEYCODE_APOSTROPHE to arrayOf(15),
                                               KeyEvent.KEYCODE_LEFT_BRACKET to arrayOf(16),                        KeyEvent.KEYCODE_SLASH to arrayOf(17),
            KeyEvent.KEYCODE_MINUS to arrayOf(17),                                KeyEvent.KEYCODE_SEMICOLON to arrayOf(18),
                                               KeyEvent.KEYCODE_P to arrayOf(19),                                   KeyEvent.KEYCODE_PERIOD to arrayOf(20),
            KeyEvent.KEYCODE_0 to arrayOf(20),                                    KeyEvent.KEYCODE_L to arrayOf(21),
                                               KeyEvent.KEYCODE_O to arrayOf(22),                                   KeyEvent.KEYCODE_COMMA to arrayOf(23),
            KeyEvent.KEYCODE_9 to arrayOf(23),                                    KeyEvent.KEYCODE_K to arrayOf(24),
                                               KeyEvent.KEYCODE_I to arrayOf(25),                                   KeyEvent.KEYCODE_M to arrayOf(26),
            KeyEvent.KEYCODE_8 to arrayOf(26),                                    KeyEvent.KEYCODE_J to arrayOf(27),
                                               KeyEvent.KEYCODE_U to arrayOf(28),                                   KeyEvent.KEYCODE_N to arrayOf(29),
            KeyEvent.KEYCODE_7 to arrayOf(29),                                    KeyEvent.KEYCODE_H to arrayOf(30),
                                               KeyEvent.KEYCODE_Y to arrayOf(31),                                   KeyEvent.KEYCODE_B to arrayOf(32),
            KeyEvent.KEYCODE_6 to arrayOf(32),                                    KeyEvent.KEYCODE_G to arrayOf(33),
                                               KeyEvent.KEYCODE_T to arrayOf(34),                                   KeyEvent.KEYCODE_V to arrayOf(35),
            KeyEvent.KEYCODE_5 to arrayOf(35),                                    KeyEvent.KEYCODE_F to arrayOf(36),
                                               KeyEvent.KEYCODE_R to arrayOf(37),                                   KeyEvent.KEYCODE_C to arrayOf(38),
            KeyEvent.KEYCODE_4 to arrayOf(38),                                    KeyEvent.KEYCODE_D to arrayOf(39),
                                               KeyEvent.KEYCODE_E to arrayOf(40),                                   KeyEvent.KEYCODE_X to arrayOf(41),
            KeyEvent.KEYCODE_3 to arrayOf(41),                                    KeyEvent.KEYCODE_S to arrayOf(42),
                                               KeyEvent.KEYCODE_W to arrayOf(43),                                   KeyEvent.KEYCODE_Z to arrayOf(44),
            KeyEvent.KEYCODE_2 to arrayOf(44),                                    KeyEvent.KEYCODE_A to arrayOf(45),
                                               KeyEvent.KEYCODE_Q to arrayOf(46),                                   KeyEvent.KEYCODE_SHIFT_LEFT to arrayOf(47),
            KeyEvent.KEYCODE_1 to arrayOf(47),                                    KeyEvent.KEYCODE_CAPS_LOCK to arrayOf(48),
                                               KeyEvent.KEYCODE_TAB to arrayOf(49),
            KeyEvent.KEYCODE_GRAVE to arrayOf(50),
        ), hashMapOf( // Right-hand device
                                               KeyEvent.KEYCODE_BACKSLASH to arrayOf(79),
            KeyEvent.KEYCODE_DEL to arrayOf(77),                                  KeyEvent.KEYCODE_ENTER to arrayOf(78),
                                               KeyEvent.KEYCODE_RIGHT_BRACKET to arrayOf(76),                       KeyEvent.KEYCODE_SHIFT_RIGHT to arrayOf(77),
            KeyEvent.KEYCODE_EQUALS to arrayOf(74),                               KeyEvent.KEYCODE_APOSTROPHE to arrayOf(75),
                                               KeyEvent.KEYCODE_LEFT_BRACKET to arrayOf(73),                        KeyEvent.KEYCODE_SLASH to arrayOf(74),
            KeyEvent.KEYCODE_MINUS to arrayOf(71),                                KeyEvent.KEYCODE_SEMICOLON to arrayOf(72),
                                               KeyEvent.KEYCODE_P to arrayOf(70),                                   KeyEvent.KEYCODE_PERIOD to arrayOf(71),
            KeyEvent.KEYCODE_0 to arrayOf(68),                                    KeyEvent.KEYCODE_L to arrayOf(69),
                                               KeyEvent.KEYCODE_O to arrayOf(67),                                   KeyEvent.KEYCODE_COMMA to arrayOf(68),
            KeyEvent.KEYCODE_9 to arrayOf(65),                                    KeyEvent.KEYCODE_K to arrayOf(66),
                                               KeyEvent.KEYCODE_I to arrayOf(64),                                   KeyEvent.KEYCODE_M to arrayOf(65),
            KeyEvent.KEYCODE_8 to arrayOf(62),                                    KeyEvent.KEYCODE_J to arrayOf(63),
                                               KeyEvent.KEYCODE_U to arrayOf(61),                                   KeyEvent.KEYCODE_N to arrayOf(62),
            KeyEvent.KEYCODE_7 to arrayOf(59),                                    KeyEvent.KEYCODE_H to arrayOf(60),
                                               KeyEvent.KEYCODE_Y to arrayOf(58),                                   KeyEvent.KEYCODE_B to arrayOf(59),
            KeyEvent.KEYCODE_6 to arrayOf(56),                                    KeyEvent.KEYCODE_G to arrayOf(57),
                                               KeyEvent.KEYCODE_T to arrayOf(55),                                   KeyEvent.KEYCODE_V to arrayOf(56),
            KeyEvent.KEYCODE_5 to arrayOf(53),                                    KeyEvent.KEYCODE_F to arrayOf(54),
                                               KeyEvent.KEYCODE_R to arrayOf(52),                                   KeyEvent.KEYCODE_C to arrayOf(53),
            KeyEvent.KEYCODE_4 to arrayOf(50),                                    KeyEvent.KEYCODE_D to arrayOf(51),
                                               KeyEvent.KEYCODE_E to arrayOf(49),                                   KeyEvent.KEYCODE_X to arrayOf(50),
            KeyEvent.KEYCODE_3 to arrayOf(47),                                    KeyEvent.KEYCODE_S to arrayOf(48),
                                               KeyEvent.KEYCODE_W to arrayOf(46),                                   KeyEvent.KEYCODE_Z to arrayOf(47),
            KeyEvent.KEYCODE_2 to arrayOf(44),                                    KeyEvent.KEYCODE_A to arrayOf(45),
                                               KeyEvent.KEYCODE_Q to arrayOf(43),                                   KeyEvent.KEYCODE_SHIFT_LEFT to arrayOf(44),
            KeyEvent.KEYCODE_1 to arrayOf(41),                                    KeyEvent.KEYCODE_CAPS_LOCK to arrayOf(42),
                                               KeyEvent.KEYCODE_TAB to arrayOf(40),
            KeyEvent.KEYCODE_GRAVE to arrayOf(38),
        ))
}