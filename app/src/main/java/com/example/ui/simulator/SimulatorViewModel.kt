package com.example.ui.simulator

import android.webkit.JsResult
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

sealed class KaiOSKey(val keyCode: Int, val keyName: String) {
    object SoftLeft : KaiOSKey(117, "SoftLeft")
    object SoftRight : KaiOSKey(118, "SoftRight")
    object ArrowUp : KaiOSKey(38, "ArrowUp")
    object ArrowDown : KaiOSKey(40, "ArrowDown")
    object ArrowLeft : KaiOSKey(37, "ArrowLeft")
    object ArrowRight : KaiOSKey(39, "ArrowRight")
    object Enter : KaiOSKey(13, "Enter")
    object Backspace : KaiOSKey(8, "Backspace")
    object Key0 : KaiOSKey(48, "0")
    object Key1 : KaiOSKey(49, "1")
    object Key2 : KaiOSKey(50, "2")
    object Key3 : KaiOSKey(51, "3")
    object Key4 : KaiOSKey(52, "4")
    object Key5 : KaiOSKey(53, "5")
    object Key6 : KaiOSKey(54, "6")
    object Key7 : KaiOSKey(55, "7")
    object Key8 : KaiOSKey(56, "8")
    object Key9 : KaiOSKey(57, "9")
    object Star : KaiOSKey(42, "*")
    object Hash : KaiOSKey(35, "#")
}

data class KaiOSEvent(val key: KaiOSKey, val isDown: Boolean)

data class AlertData(val message: String, val result: JsResult)

enum class DisplayMode {
    PORTRAIT_240X320,
    LANDSCAPE_320X240
}

enum class DpadMappingMode {
    STANDARD,
    ROTATED_CCW, // Keypad on Right (UP -> Right, RIGHT -> Down, DOWN -> Left, LEFT -> Up)
    ROTATED_CW   // Keypad on Left (UP -> Left, RIGHT -> Up, DOWN -> Right, LEFT -> Down)
}

class SimulatorViewModel : ViewModel() {
    private val _url = MutableStateFlow("kaios://home")
    val url: StateFlow<String> = _url.asStateFlow()

    private val _keyEvents = MutableSharedFlow<KaiOSEvent>(extraBufferCapacity = 50)
    val keyEvents = _keyEvents.asSharedFlow()

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    private val _alertData = MutableStateFlow<AlertData?>(null)
    val alertData: StateFlow<AlertData?> = _alertData.asStateFlow()

    private val _displayMode = MutableStateFlow(DisplayMode.PORTRAIT_240X320)
    val displayMode: StateFlow<DisplayMode> = _displayMode.asStateFlow()

    private val _dpadMappingMode = MutableStateFlow(DpadMappingMode.STANDARD)
    val dpadMappingMode: StateFlow<DpadMappingMode> = _dpadMappingMode.asStateFlow()

    fun updateUrl(newUrl: String) {
        _url.value = newUrl
    }

    fun setDisplayMode(mode: DisplayMode) {
        _displayMode.value = mode
        if (mode == DisplayMode.LANDSCAPE_320X240) {
            // Default to Rotated CCW when switching to Landscape since most games assume rotated keypad
            _dpadMappingMode.value = DpadMappingMode.ROTATED_CCW
        } else {
            _dpadMappingMode.value = DpadMappingMode.STANDARD
        }
    }

    fun setDpadMappingMode(mode: DpadMappingMode) {
        _dpadMappingMode.value = mode
    }

    fun onKeyEvent(key: KaiOSKey, isDown: Boolean) {
        val mappedKey = when (_dpadMappingMode.value) {
            DpadMappingMode.STANDARD -> key
            DpadMappingMode.ROTATED_CCW -> {
                when (key) {
                    KaiOSKey.ArrowUp -> KaiOSKey.ArrowRight
                    KaiOSKey.ArrowRight -> KaiOSKey.ArrowDown
                    KaiOSKey.ArrowDown -> KaiOSKey.ArrowLeft
                    KaiOSKey.ArrowLeft -> KaiOSKey.ArrowUp
                    else -> key
                }
            }
            DpadMappingMode.ROTATED_CW -> {
                when (key) {
                    KaiOSKey.ArrowUp -> KaiOSKey.ArrowLeft
                    KaiOSKey.ArrowRight -> KaiOSKey.ArrowUp
                    KaiOSKey.ArrowDown -> KaiOSKey.ArrowRight
                    KaiOSKey.ArrowLeft -> KaiOSKey.ArrowDown
                    else -> key
                }
            }
        }
        _keyEvents.tryEmit(KaiOSEvent(mappedKey, isDown))
    }

    fun addLog(msg: String) {
        _logs.update { (it + msg).takeLast(100) }
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    fun showAlert(message: String, result: JsResult) {
        _alertData.value = AlertData(message, result)
    }

    fun dismissAlert() {
        _alertData.value?.result?.confirm()
        _alertData.value = null
    }
}
