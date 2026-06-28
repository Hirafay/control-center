package com.rafay.controlcenter

import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader

/**
 * Root command runner. This is WHY the toggles work where third-party apps fail:
 * normal apps can't flip system state, but `su -c` can.
 */
object Root {

    /** Fire-and-forget. Returns true if the command exited 0. */
    fun run(cmd: String): Boolean = try {
        val p = Runtime.getRuntime().exec("su")
        DataOutputStream(p.outputStream).use { out ->
            out.writeBytes("$cmd\n")
            out.writeBytes("exit\n")
            out.flush()
        }
        p.waitFor() == 0
    } catch (e: Exception) {
        false
    }

    /** Run and capture stdout (trimmed). Used to read current toggle state. */
    fun query(cmd: String): String = try {
        val p = Runtime.getRuntime().exec("su")
        DataOutputStream(p.outputStream).use { out ->
            out.writeBytes("$cmd\n")
            out.writeBytes("exit\n")
            out.flush()
        }
        val text = BufferedReader(InputStreamReader(p.inputStream)).readText()
        p.waitFor()
        text.trim()
    } catch (e: Exception) {
        ""
    }

    /** Quick check so the UI can warn if root was denied. */
    fun isAvailable(): Boolean = query("id").contains("uid=0")
}
