package de.zugspitz.supporter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

@Composable
actual fun ObserveAppForeground(onForeground: () -> Unit) {
    val activity = MainActivity.currentActivity
    DisposableEffect(activity, onForeground) {
        if (activity == null) {
            return@DisposableEffect onDispose {}
        }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                onForeground()
            }
        }
        activity.lifecycle.addObserver(observer)
        onDispose {
            activity.lifecycle.removeObserver(observer)
        }
    }
}
