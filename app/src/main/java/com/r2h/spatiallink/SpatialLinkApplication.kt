package com.r2h.spatiallink

import android.app.Application
import android.os.Build
import com.r2h.spatiallink.capabilities.AndroidCapabilityFactory
import com.r2h.spatiallink.capabilities.AndroidPermissionPlatformAccess
import com.r2h.spatiallink.capabilities.AndroidPermissionStateReader
import com.r2h.spatiallink.common.DefaultDispatcherProvider
import com.r2h.spatiallink.common.DispatcherProvider
import com.r2h.spatiallink.ble.AndroidBleDiscoveryFactory
import com.r2h.spatiallink.discovery.DiscoveryController
import com.r2h.spatiallink.discovery.DiscoveryControllerFactory
import com.r2h.spatiallink.discovery.SecureRandomDiscoverySessionIdSource
import com.r2h.spatiallink.discovery.V1DiscoveryFrameCodec
import com.r2h.spatiallink.identity.AndroidKeyStoreIdentityRepository
import com.r2h.spatiallink.identity.AndroidKeyStorePlatformAdapter
import com.r2h.spatiallink.model.DeviceCapabilityDetector
import com.r2h.spatiallink.model.DeviceIdentityRepository
import com.r2h.spatiallink.model.PermissionStateReader

class SpatialLinkApplication : Application() {
    val container: AppContainer by lazy(LazyThreadSafetyMode.NONE) {
        AppContainer(this)
    }
}

class AppContainer(application: Application) {
    val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider()
    private val bleDiscoveryFactory = AndroidBleDiscoveryFactory(application.applicationContext)
    private val discoveryScanner = bleDiscoveryFactory.createScanner()
    private val discoveryAdvertiser = bleDiscoveryFactory.createAdvertiser()
    val discoveryPermissionReader = bleDiscoveryFactory.permissionReader
    val discoveryControllerFactory: DiscoveryControllerFactory = DiscoveryControllerFactory {
        DiscoveryController(
            scanner = discoveryScanner,
            advertiser = discoveryAdvertiser,
            bluetoothStateReader = bleDiscoveryFactory.bluetoothStateReader,
            permissionReader = bleDiscoveryFactory.permissionReader,
            clock = com.r2h.spatiallink.ble.AndroidMonotonicClock(),
            sessionIdSource = SecureRandomDiscoverySessionIdSource(),
            frameCodec = V1DiscoveryFrameCodec,
        )
    }
    val capabilityDetector: DeviceCapabilityDetector = AndroidCapabilityFactory(
        context = application,
        dispatchers = dispatcherProvider,
    ).createDetector()
    val permissionStateReader: PermissionStateReader = AndroidPermissionStateReader(
        sdkInt = Build.VERSION.SDK_INT,
        targetSdk = application.applicationInfo.targetSdkVersion,
        access = AndroidPermissionPlatformAccess(application),
    )
    val identityRepository: DeviceIdentityRepository = AndroidKeyStoreIdentityRepository(
        alias = AndroidKeyStoreIdentityRepository.PRODUCTION_ALIAS,
        keyStore = AndroidKeyStorePlatformAdapter(),
        dispatchers = dispatcherProvider,
    )
}
