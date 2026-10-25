package com.chdjalil.jalilgym.feature.nutrition.scan

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.chdjalil.jalilgym.core.designsystem.component.JgPrimaryButton
import com.chdjalil.jalilgym.core.domain.repository.BarcodeLookup
import com.chdjalil.jalilgym.core.domain.repository.FoodRepository
import com.chdjalil.jalilgym.feature.nutrition.R
import com.chdjalil.jalilgym.feature.nutrition.ui.BackBar
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.Executors
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ScanState {
    data object Scanning : ScanState

    data class LookingUp(val barcode: String) : ScanState

    data class Found(val foodId: String) : ScanState

    data class NotFound(val barcode: String) : ScanState

    data class Offline(val barcode: String) : ScanState
}

@HiltViewModel
class BarcodeScanViewModel @Inject constructor(private val foodRepository: FoodRepository) : ViewModel() {
    private val _state = MutableStateFlow<ScanState>(ScanState.Scanning)
    val state: StateFlow<ScanState> = _state.asStateFlow()

    /** Ignores further codes once one is being looked up. */
    fun onBarcode(barcode: String) {
        if (_state.value != ScanState.Scanning) return
        _state.value = ScanState.LookingUp(barcode)
        viewModelScope.launch {
            _state.value = when (val result = foodRepository.lookupBarcode(barcode)) {
                is BarcodeLookup.Found -> ScanState.Found(result.food.id)
                is BarcodeLookup.NotFound -> ScanState.NotFound(result.barcode)
                is BarcodeLookup.Offline -> ScanState.Offline(result.barcode)
            }
        }
    }

    fun retry() {
        _state.value = ScanState.Scanning
    }
}

/** Retail product codes only; QR codes and the like are ignored. */
private val scannerOptions = BarcodeScannerOptions.Builder()
    .setBarcodeFormats(Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E)
    .build()

@Composable
internal fun BarcodeScanScreen(
    onBack: () -> Unit,
    onFound: (foodId: String) -> Unit,
    onCreateFood: (barcode: String) -> Unit,
    viewModel: BarcodeScanViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasPermission = it }
    LaunchedEffect(Unit) { if (!hasPermission) permission.launch(Manifest.permission.CAMERA) }
    LaunchedEffect(state) { (state as? ScanState.Found)?.let { onFound(it.foodId) } }
    var manualCode by rememberSaveable { mutableStateOf("") }

    Scaffold(
        topBar = { BackBar(stringResource(R.string.feature_nutrition_scan_barcode), onBack) },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    !hasPermission -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.feature_nutrition_camera_needed), style = MaterialTheme.typography.bodyLarge)
                        OutlinedButton(onClick = { permission.launch(Manifest.permission.CAMERA) }) {
                            Text(stringResource(R.string.feature_nutrition_allow_camera))
                        }
                    }
                    state == ScanState.Scanning -> CameraPreview(onBarcode = viewModel::onBarcode)
                    state is ScanState.LookingUp || state is ScanState.Found -> CircularProgressIndicator()
                    state is ScanState.NotFound -> ScanResult(
                        message = stringResource(R.string.feature_nutrition_barcode_not_found, (state as ScanState.NotFound).barcode),
                        onCreate = { onCreateFood((state as ScanState.NotFound).barcode) },
                        onRetry = viewModel::retry,
                    )
                    state is ScanState.Offline -> ScanResult(
                        message = stringResource(R.string.feature_nutrition_barcode_offline),
                        onCreate = { onCreateFood((state as ScanState.Offline).barcode) },
                        onRetry = viewModel::retry,
                    )
                }
            }
            OutlinedTextField(
                value = manualCode,
                onValueChange = { manualCode = it.filter(Char::isDigit).take(14) },
                label = { Text(stringResource(R.string.feature_nutrition_type_barcode)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedButton(onClick = { viewModel.onBarcode(manualCode) }, enabled = manualCode.length >= 8) {
                Text(stringResource(R.string.feature_nutrition_look_up))
            }
            Text(
                stringResource(R.string.feature_nutrition_off_attribution),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ScanResult(message: String, onCreate: () -> Unit, onRetry: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(message, style = MaterialTheme.typography.bodyLarge)
        JgPrimaryButton(onClick = onCreate) { Text(stringResource(R.string.feature_nutrition_create_food)) }
        OutlinedButton(onClick = onRetry) { Text(stringResource(R.string.feature_nutrition_scan_again)) }
    }
}

/** CameraX preview with ML Kit barcode analysis on a background thread. */
@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraPreview(onBarcode: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember { BarcodeScanning.getClient(scannerOptions) }
    DisposableEffect(lifecycleOwner) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null
        providerFuture.addListener({
            val cameraProvider = providerFuture.get()
            provider = cameraProvider
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analysis.setAnalyzer(executor) { proxy ->
                val media = proxy.image
                if (media == null) {
                    proxy.close()
                    return@setAnalyzer
                }
                scanner.process(InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees))
                    .addOnSuccessListener { codes -> codes.firstNotNullOfOrNull { it.rawValue }?.let(onBarcode) }
                    .addOnCompleteListener { proxy.close() }
            }
            runCatching {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            }.onFailure { Log.w("BarcodeScan", "Camera unavailable", it) }
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            provider?.unbindAll()
            scanner.close()
            executor.shutdown()
        }
    }
    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
}
