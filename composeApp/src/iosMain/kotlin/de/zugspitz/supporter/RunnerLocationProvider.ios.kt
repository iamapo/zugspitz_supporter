package de.zugspitz.supporter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLLocationAccuracyHundredMeters
import platform.Foundation.NSError
import platform.darwin.NSObject
import kotlin.time.Clock

@Composable
actual fun rememberRunnerLocationProvider(): RunnerLocationProvider = remember { IosRunnerLocationProvider() }

@OptIn(ExperimentalForeignApi::class)
private class IosRunnerLocationProvider : RunnerLocationProvider {
    private val updates = MutableSharedFlow<RunnerDeviceLocation>(replay = 1, extraBufferCapacity = 1)
    private val manager = CLLocationManager()
    private val delegate = LocationDelegate { location ->
        val coordinate = location.coordinate.useContents { latitude to longitude }
        updates.tryEmit(
            RunnerDeviceLocation(
                latitude = coordinate.first,
                longitude = coordinate.second,
                horizontalAccuracyMeters = location.horizontalAccuracy.takeIf { it >= 0.0 },
                timestampEpochMillis = Clock.System.now().toEpochMilliseconds(),
            ),
        )
    }

    override val locations: Flow<RunnerDeviceLocation> = updates

    init {
        manager.delegate = delegate
        manager.desiredAccuracy = kCLLocationAccuracyHundredMeters
        manager.distanceFilter = 50.0
        manager.pausesLocationUpdatesAutomatically = false
        manager.showsBackgroundLocationIndicator = true
    }

    override fun start() {
        LiveSharingLogger.d("Starting iOS runner location updates with background support.")
        manager.requestAlwaysAuthorization()
        manager.allowsBackgroundLocationUpdates = true
        manager.startUpdatingLocation()
    }

    override fun stop() {
        LiveSharingLogger.d("Stopping iOS runner location updates.")
        manager.stopUpdatingLocation()
        manager.allowsBackgroundLocationUpdates = false
    }
}

private class LocationDelegate(
    private val onLocation: (CLLocation) -> Unit,
) : NSObject(), CLLocationManagerDelegateProtocol {
    override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
        didUpdateLocations.lastOrNull()
            ?.let { it as? CLLocation }
            ?.let { location ->
                LiveSharingLogger.d("Received iOS runner location accuracy=${location.horizontalAccuracy}.")
                onLocation(location)
            }
    }

    override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
        LiveSharingLogger.e("iOS runner location update failed", Throwable(didFailWithError.localizedDescription))
    }
}
