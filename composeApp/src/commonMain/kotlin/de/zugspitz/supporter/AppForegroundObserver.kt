package de.zugspitz.supporter

import androidx.compose.runtime.Composable

@Composable
expect fun ObserveAppForeground(onForeground: () -> Unit)
