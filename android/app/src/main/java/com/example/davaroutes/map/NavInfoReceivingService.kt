package com.example.davaroutes.map

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.Process
import com.google.android.libraries.mapsplatform.turnbyturn.TurnByTurnManager
import com.google.android.libraries.mapsplatform.turnbyturn.model.NavInfo
import com.google.android.libraries.mapsplatform.turnbyturn.model.NavState

class NavInfoReceivingService : Service() {
    private lateinit var handlerThread: HandlerThread
    private lateinit var incomingMessenger: Messenger
    private lateinit var turnByTurnManager: TurnByTurnManager

    override fun onCreate() {
        super.onCreate()

        turnByTurnManager = TurnByTurnManager.createInstance()
        handlerThread = HandlerThread(
            "DavaRoutesNavInfo",
            Process.THREAD_PRIORITY_DEFAULT
        ).apply { start() }
        incomingMessenger = Messenger(IncomingNavInfoHandler(handlerThread.looper))
    }

    override fun onBind(intent: Intent): IBinder = incomingMessenger.binder

    override fun onDestroy() {
        handlerThread.quitSafely()
        super.onDestroy()
    }

    private inner class IncomingNavInfoHandler(looper: Looper) : Handler(looper) {
        override fun handleMessage(message: Message) {
            if (message.what == TurnByTurnManager.MSG_NAV_INFO) {
                val navInfo = turnByTurnManager.readNavInfoFromBundle(message.data)
                NavigationInstructionStore.update(navInfo.toNavigationInstruction())
                return
            }

            super.handleMessage(message)
        }
    }
}

private fun NavInfo.toNavigationInstruction(): NavigationInstruction {
    if (navState == NavState.REROUTING) {
        return NavigationInstruction(instruction = "Rerouting")
    }

    if (navState == NavState.STOPPED) {
        return NavigationInstruction(instruction = "Navigation stopped")
    }

    val step = currentStep ?: return NavigationInstruction(instruction = "Continue on route")

    return NavigationInstruction(
        instruction = step.fullInstructionText?.takeIf { it.isNotBlank() }
            ?: step.simpleRoadName?.takeIf { it.isNotBlank() }
            ?: "Continue",
        distanceMeters = distanceToCurrentStepMeters,
        roadName = step.simpleRoadName?.takeIf { it.isNotBlank() },
        maneuver = step.maneuver,
        maneuverBitmap = step.maneuverBitmap
    )
}
