package com.r2h.spatiallink.nearby

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.r2h.spatiallink.designsystem.FieldNode
import com.r2h.spatiallink.designsystem.SpatialAtmosphere
import com.r2h.spatiallink.designsystem.SpatialCommand
import com.r2h.spatiallink.designsystem.SpatialField
import com.r2h.spatiallink.designsystem.SpatialFrame
import com.r2h.spatiallink.designsystem.SpatialLinkColors
import com.r2h.spatiallink.designsystem.SpatialLinkFonts
import com.r2h.spatiallink.designsystem.StatusMark
import com.r2h.spatiallink.discovery.DiscoveryPermission
import kotlinx.coroutines.flow.collect

@Composable
fun NearbyRoute(
    viewModel: NearbyViewModel,
    onClosed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { viewModel.onRuntimePermissionsResult() },
    )
    val bluetoothLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            viewModel.onBluetoothEnableResult(result.resultCode == Activity.RESULT_OK)
        },
    )

    BackHandler { viewModel.requestClose() }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is NearbyEffect.RequestRuntimePermissions -> permissionLauncher.launch(
                    effect.permissions.map(DiscoveryPermission::manifestName).toTypedArray(),
                )
                NearbyEffect.RequestBluetoothEnable -> bluetoothLauncher.launch(
                    Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
                )
                NearbyEffect.LeaveCompleted -> onClosed()
            }
        }
    }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                viewModel.onForegroundLost()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    NearbyScreen(
        state = state,
        onStart = viewModel::openField,
        onStop = viewModel::stopField,
        onRequestClose = viewModel::requestClose,
        modifier = modifier,
    )
}

@Composable
fun NearbyScreen(
    state: NearbyUiState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRequestClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val presentation = NearbyFieldStateMapper.map(state)
    val commandLabel = if (presentation.isRunning) "END FIELD" else "OPEN FIELD"
    val commandSupporting = if (presentation.isRunning) {
        "Stop anonymous presence"
    } else {
        "Enter the nearby field"
    }
    val statusDescription = "Nearby field status ${presentation.status.label}"

    SpatialAtmosphere(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 20.dp)
                .semantics { contentDescription = "Nearby field route" },
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "NEARBY FIELD",
                    color = SpatialLinkColors.WarmIvory,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = SpatialLinkFonts.Technical,
                        fontWeight = FontWeight.Light,
                        letterSpacing = 3.2.sp,
                    ),
                )
                SpatialFrame(
                    modifier = Modifier
                        .widthIn(min = 72.dp)
                        .semantics { contentDescription = "Close nearby field" },
                    onClick = onRequestClose,
                ) {
                    Text(
                        text = "CLOSE",
                        color = SpatialLinkColors.MutedIvory,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = SpatialLinkFonts.Technical,
                            fontWeight = FontWeight.Light,
                            letterSpacing = 1.9.sp,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
            StatusMark(
                status = presentation.status,
                modifier = Modifier.semantics { contentDescription = statusDescription },
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Anonymous presence, held briefly in the field.",
                color = SpatialLinkColors.MutedIvory,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = SpatialLinkFonts.Technical,
                    fontWeight = FontWeight.Light,
                ),
                modifier = Modifier.widthIn(max = 290.dp),
            )

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
                    .clipToBounds()
                    .semantics {
                        contentDescription = "Nearby spatial field ${presentation.status.label}"
                    },
                contentAlignment = Alignment.Center,
            ) {
                val fieldSize = maxOf(maxWidth * 1.38f, 360.dp)
                Box(modifier = Modifier.requiredSize(fieldSize)) {
                    SpatialField(
                        status = presentation.status,
                        modifier = Modifier.fillMaxSize(),
                    )
                    if (presentation.isRunning && presentation.presence.count > 0) {
                        FieldNode(
                            status = presentation.status,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .offset(y = (-fieldSize * 0.22f)),
                        )
                        FieldNode(
                            status = presentation.status,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .offset(
                                    x = (-fieldSize * 0.22f),
                                    y = fieldSize * 0.16f,
                                ),
                        )
                        FieldNode(
                            status = presentation.status,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .offset(
                                    x = fieldSize * 0.22f,
                                    y = fieldSize * 0.16f,
                                ),
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Anonymous presence count ${presentation.presence.count}"
                    },
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    text = presentation.presence.label,
                    color = SpatialLinkColors.WarmIvory,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = SpatialLinkFonts.Technical,
                        fontWeight = FontWeight.Light,
                        letterSpacing = 2.1.sp,
                    ),
                )
                presentation.presence.strengthLabel?.let { strength ->
                    Text(
                        text = "PRESENCE STRENGTH  $strength",
                        color = SpatialLinkColors.IceCyan,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = SpatialLinkFonts.Technical,
                            fontWeight = FontWeight.Light,
                            letterSpacing = 1.4.sp,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            SpatialCommand(
                label = commandLabel,
                supporting = commandSupporting,
                onClick = if (presentation.isRunning) onStop else onStart,
                modifier = Modifier.semantics {
                    contentDescription = commandLabel
                },
            )
        }
    }
}

private fun DiscoveryPermission.manifestName(): String = when (this) {
    DiscoveryPermission.LEGACY_FINE_LOCATION -> android.Manifest.permission.ACCESS_FINE_LOCATION
    DiscoveryPermission.BLUETOOTH_SCAN -> android.Manifest.permission.BLUETOOTH_SCAN
    DiscoveryPermission.BLUETOOTH_ADVERTISE -> android.Manifest.permission.BLUETOOTH_ADVERTISE
    DiscoveryPermission.BLUETOOTH_CONNECT -> android.Manifest.permission.BLUETOOTH_CONNECT
}
