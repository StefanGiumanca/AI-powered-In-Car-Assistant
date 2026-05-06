package com.example.davaroutes.map

import android.graphics.Bitmap
import com.google.android.libraries.mapsplatform.turnbyturn.model.Maneuver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class NavigationInstruction(
    val instruction: String = "Preparing navigation",
    val distanceMeters: Int? = null,
    val roadName: String? = null,
    val maneuver: Int = Maneuver.UNKNOWN,
    val maneuverBitmap: Bitmap? = null
)

object NavigationInstructionStore {
    private val _instruction = MutableStateFlow(NavigationInstruction())
    val instruction: StateFlow<NavigationInstruction> = _instruction

    fun update(instruction: NavigationInstruction) {
        _instruction.value = instruction
    }

    fun reset() {
        _instruction.value = NavigationInstruction()
    }
}
