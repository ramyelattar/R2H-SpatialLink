package com.r2h.spatiallink.ble

internal class RecordingPlatformHandle : AndroidBlePlatformHandle {
    var stopCount: Int = 0
        private set

    override fun stop() {
        stopCount += 1
    }
}

internal class RecordingScannerPlatform : AndroidBleScannerPlatform {
    var startCount: Int = 0
        private set
    var stopCount: Int = 0
        private set
    var lastRequest: BleScanRequest? = null
        private set
    private var resultCallback: ((ByteArray, Int) -> Unit)? = null
    private var failureCallback: ((Int) -> Unit)? = null
    private val handle = RecordingPlatformHandle()

    override fun start(
        request: BleScanRequest,
        onResult: (ByteArray, Int) -> Unit,
        onFailure: (Int) -> Unit,
    ): AndroidBlePlatformHandle {
        startCount += 1
        lastRequest = request
        resultCallback = onResult
        failureCallback = onFailure
        return object : AndroidBlePlatformHandle {
            override fun stop() {
                handle.stop()
                stopCount += 1
            }
        }
    }

    fun emit(payload: ByteArray, rssiDbm: Int) {
        resultCallback?.invoke(payload, rssiDbm)
    }

    fun fail(errorCode: Int) {
        failureCallback?.invoke(errorCode)
    }
}

internal class RecordingAdvertiserPlatform : AndroidBleAdvertiserPlatform {
    var startCount: Int = 0
        private set
    var stopCount: Int = 0
        private set
    var lastSpec: LegacyAdvertisementSpec? = null
        private set
    var lastFailure: com.r2h.spatiallink.discovery.DiscoveryFailure? = null
        private set
    var extendedAdvertisingRequested: Boolean = false
        private set
    var startThrowable: Throwable? = null
    private var failureCallback: ((Int) -> Unit)? = null

    override fun start(
        spec: LegacyAdvertisementSpec,
        onFailure: (Int) -> Unit,
    ): AndroidBlePlatformHandle {
        startThrowable?.let { throw it }
        startCount += 1
        lastSpec = spec
        failureCallback = onFailure
        return object : AndroidBlePlatformHandle {
            override fun stop() {
                stopCount += 1
            }
        }
    }

    fun fail(errorCode: Int) {
        failureCallback?.invoke(errorCode)
    }
}
