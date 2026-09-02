package com.r2h.spatiallink

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.r2h.spatiallink.diagnostics.DiagnosticsViewModel
import com.r2h.spatiallink.diagnostics.DiagnosticsViewModelFactory
import com.r2h.spatiallink.nearby.NearbyViewModel
import com.r2h.spatiallink.nearby.NearbyViewModelFactory
import com.r2h.spatiallink.ui.theme.SpatialLinkTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val diagnosticsViewModel: DiagnosticsViewModel by viewModels {
        val app = application as SpatialLinkApplication
        DiagnosticsViewModelFactory(
            capabilityDetector = app.container.capabilityDetector,
            permissionStateReader = app.container.permissionStateReader,
            identityRepository = app.container.identityRepository,
            dispatcherProvider = app.container.dispatcherProvider,
        )
    }

    private val nearbyViewModel: NearbyViewModel by viewModels {
        val app = application as SpatialLinkApplication
        NearbyViewModelFactory(
            controllerFactory = app.container.discoveryControllerFactory,
            permissionReader = app.container.discoveryPermissionReader,
            dispatcherProvider = app.container.dispatcherProvider,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SpatialLinkTheme {
                var showSplash by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    delay(1800)
                    showSplash = false
                }

                if (showSplash) {
                    Image(
                        painter = painterResource(R.drawable.splash),
                        contentDescription = "Splash Screen",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Image(
                            painter = painterResource(R.drawable.background),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )

                        SpatialLinkRoot(
                            diagnosticsViewModel = diagnosticsViewModel,
                            nearbyViewModel = nearbyViewModel,
                        )
                    }
                }
            }
        }
    }
}
