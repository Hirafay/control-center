package com.rafay.controlcenter

import android.content.Context
import android.hardware.camera2.CameraManager

/**
 * Each control = a label + a way to read state + a way to flip it.
 * Most go through root (svc / cmd / settings). Flashlight uses the in-app
 * CameraManager because that's more reliable than a shell torch hack.
 *
 * NOTE (teacher mode): the bluetooth + mobile-data commands are the most
 * ROM-dependent. If one does nothing on ArtisanROM, swap the command string
 * here — that's the only line you'd touch.
 */
class Toggles(private val ctx: Context) {

    private val cameraManager = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var torchOn = false
    private val torchId: String? = runCatching {
        cameraManager.cameraIdList.firstOrNull { id ->
            cameraManager.getCameraCharacteristics(id)
                .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
    }.getOrNull()

    data class Control(
        val key: String,
        val label: String,
        val isOn: () -> Boolean,
        val toggle: () -> Unit
    )

    fun all(): List<Control> = listOf(
        Control("wifi", "Wi-Fi",
            isOn = { Root.query("settings get global wifi_on") == "1" },
            toggle = {
                val on = Root.query("settings get global wifi_on") == "1"
                Root.run(if (on) "svc wifi disable" else "svc wifi enable")
            }),

        Control("data", "Mobile Data",
            isOn = { Root.query("settings get global mobile_data") == "1" },
            toggle = {
                val on = Root.query("settings get global mobile_data") == "1"
                Root.run(if (on) "svc data disable" else "svc data enable")
            }),

        Control("bt", "Bluetooth",
            isOn = { Root.query("settings get global bluetooth_on") == "1" },
            toggle = {
                val on = Root.query("settings get global bluetooth_on") == "1"
                Root.run(if (on) "svc bluetooth disable" else "svc bluetooth enable")
            }),

        Control("airplane", "Airplane",
            isOn = { Root.query("settings get global airplane_mode_on") == "1" },
            toggle = {
                val on = Root.query("settings get global airplane_mode_on") == "1"
                Root.run(if (on) "cmd connectivity airplane-mode disable"
                         else "cmd connectivity airplane-mode enable")
            }),

        Control("rotate", "Rotation",
            isOn = { Root.query("settings get system accelerometer_rotation") == "1" },
            toggle = {
                val on = Root.query("settings get system accelerometer_rotation") == "1"
                Root.run("settings put system accelerometer_rotation ${if (on) 0 else 1}")
            }),

        Control("torch", "Flashlight",
            isOn = { torchOn },
            toggle = {
                torchId?.let {
                    torchOn = !torchOn
                    runCatching { cameraManager.setTorchMode(it, torchOn) }
                }
            })
    )

    /** Brightness 0..255. Read + set via root so we don't need WRITE_SETTINGS granted. */
    fun getBrightness(): Int = Root.query("settings get system screen_brightness").toIntOrNull() ?: 128
    fun setBrightness(value: Int) {
        val v = value.coerceIn(1, 255)
        Root.run("settings put system screen_brightness_mode 0")
        Root.run("settings put system screen_brightness $v")
    }
}
