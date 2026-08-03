package com.indianservers.AIbiology

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.Pose
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.indianservers.AIbiology.data.ModelRepository
import com.indianservers.AIbiology.databinding.ActivityAnatomyArBinding
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale
import io.github.sceneview.node.ModelNode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import kotlin.math.max
import kotlin.math.sqrt

class AnatomyArActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAnatomyArBinding
    private var installRequested = false
    private var sceneConfigured = false
    private var fatalShown = false
    private var anchorNode: AnchorNode? = null
    private var modelNode: ModelNode? = null
    private var modelLoadJob: Job? = null
    private lateinit var modelFile: File
    private var modelSizeMeters = 0.25f
    private var initialModelScale: Scale? = null
    private var latestSession: Session? = null
    private var latestCameraPose: Pose? = null
    private var stableTrackingFrames = 0
    private var automaticPlacementAttempted = false
    private var placementMode = PlacementMode.AUTO
    private var latestPlacementPose: Pose? = null
    private var measurementStartPose: Pose? = null
    private var rotationStepDegrees = 15f
    private var modelLocked = false
    private var modelVisible = true
    private var lastStatus = ""
    private var lastStatusAt = 0L

    private val cameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) ensureArReady() else showFatal(
            "Camera access is required for the augmented reality viewer. " +
                "You can enable it later in Android Settings."
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        binding = ActivityAnatomyArBinding.inflate(layoutInflater)
        setContentView(binding.root)

        modelFile = File(intent.getStringExtra(EXTRA_MODEL_PATH).orEmpty())
        modelSizeMeters = intent.getFloatExtra(EXTRA_MODEL_SIZE_METERS, 0.25f)
            .coerceIn(0.05f, 2.0f)
        binding.arTitle.text = intent.getStringExtra(EXTRA_MODEL_TITLE) ?: "Human Anatomy"
        configureActions()

        if (!modelFile.isFile) {
            showFatal("Download this anatomy model before opening it in AR.")
            return
        }
        if (!runCatching { ModelRepository.isValidGlb(modelFile) }.getOrDefault(false)) {
            showFatal(
                "This downloaded model is incomplete or damaged. " +
                    "Delete it from the App Library and download it again."
            )
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            ensureArReady()
        } else {
            cameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    override fun onResume() {
        super.onResume()
        if (::modelFile.isInitialized &&
            modelFile.isFile &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            ensureArReady()
        }
    }

    override fun onDestroy() {
        modelLoadJob?.cancel()
        if (::binding.isInitialized) {
            binding.arSceneView.onSessionUpdated = null
            clearAnchor()
        }
        super.onDestroy()
    }

    private fun configureActions() {
        binding.arClose.setOnClickListener { finish() }
        binding.arShare.setOnClickListener { AppActions.shareApp(this) }
        binding.arPoweredBy.text = AppActions.copyrightNotice(this)
        binding.arPoweredBy.setOnClickListener { AppActions.openIndianServers(this) }
        binding.arPrivacy.setOnClickListener {
            startActivity(
                android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    Uri.parse("https://policies.google.com/privacy")
                )
            )
        }
        binding.rotateX.setOnClickListener { rotateModel(x = rotationStepDegrees) }
        binding.rotateY.setOnClickListener { rotateModel(y = rotationStepDegrees) }
        binding.rotateZ.setOnClickListener { rotateModel(z = rotationStepDegrees) }
        binding.arSmaller.setOnClickListener { resizeModel(0.9f) }
        binding.arLarger.setOnClickListener { resizeModel(1.1f) }
        binding.arRecenter.setOnClickListener { recenterModel() }
        binding.arPlaceNow.setOnClickListener { placeModelNow() }
        binding.arActualSize.setOnClickListener { restoreActualSize() }
        binding.arResetPose.setOnClickListener { resetPose() }
        binding.arLockModel.setOnClickListener { toggleModelLock() }
        binding.arRotationStep.setOnClickListener { cycleRotationStep() }
        binding.arToggleModel.setOnClickListener { toggleModelVisibility() }
        binding.arPlacementMode.setOnClickListener { showPlacementModeSelector() }
        binding.arMeasure.setOnClickListener { captureMeasurementPoint() }
        binding.arHelp.setOnClickListener { showHelp() }
        updateScaleLabel()
    }

    private fun ensureArReady() {
        if (sceneConfigured || fatalShown || isFinishing || isDestroyed) return
        try {
            val availability = ArCoreApk.getInstance().checkAvailability(this)
            when {
                availability.isTransient -> {
                    updateStatus("Checking AR support...", force = true)
                    binding.root.postDelayed(::ensureArReady, AR_AVAILABILITY_RETRY_MS)
                }
                !availability.isSupported -> showFatal(
                    "This device does not support Google Play Services for AR."
                )
                else -> when (
                    ArCoreApk.getInstance().requestInstall(this, !installRequested)
                ) {
                    ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                        installRequested = true
                        updateStatus(
                            "Installing Google Play Services for AR...",
                            force = true
                        )
                    }
                    ArCoreApk.InstallStatus.INSTALLED -> configureScene()
                }
            }
        } catch (error: Throwable) {
            showFatal(arErrorMessage(error))
        }
    }

    private fun configureScene() {
        if (sceneConfigured || isFinishing || isDestroyed) return
        sceneConfigured = true
        try {
            binding.arSceneView.apply {
                configureSession { session, config ->
                    configureBestSupportedSession(session, config)
                }
                lifecycle = this@AnatomyArActivity.lifecycle
                planeRenderer.isEnabled = true
                onSessionUpdated = { session, frame ->
                    handleSessionFrame(session, frame)
                }
            }
        } catch (error: Throwable) {
            sceneConfigured = false
            showFatal(arErrorMessage(error))
        }
    }

    private fun configureBestSupportedSession(session: Session, config: Config) {
        config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
        config.instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP
        config.depthMode =
            if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                Config.DepthMode.AUTOMATIC
            } else {
                Config.DepthMode.DISABLED
            }
        config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
        if (!session.isSupported(config)) {
            config.depthMode = Config.DepthMode.DISABLED
            config.instantPlacementMode = Config.InstantPlacementMode.DISABLED
            config.lightEstimationMode = Config.LightEstimationMode.AMBIENT_INTENSITY
        }
        if (!session.isSupported(config)) {
            config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
            config.lightEstimationMode = Config.LightEstimationMode.DISABLED
        }
        if (!session.isSupported(config)) {
            config.planeFindingMode = Config.PlaneFindingMode.DISABLED
            config.instantPlacementMode = Config.InstantPlacementMode.DISABLED
            config.depthMode = Config.DepthMode.DISABLED
            config.lightEstimationMode = Config.LightEstimationMode.DISABLED
        }
    }

    private fun handleSessionFrame(
        session: Session,
        frame: Frame
    ) {
        val trackingState = frame.camera.trackingState
        val cameraPose = frame.camera.pose
        latestSession = session
        latestCameraPose = cameraPose
        latestPlacementPose =
            if (trackingState == TrackingState.TRACKING) findPlacementPose(frame, cameraPose)
            else null
        updatePlacementIndicator(latestPlacementPose != null)
        when (trackingState) {
            TrackingState.TRACKING -> {
                stableTrackingFrames = (stableTrackingFrames + 1)
                    .coerceAtMost(STABLE_TRACKING_FRAMES * 3)
                if (
                    anchorNode == null &&
                    stableTrackingFrames >= STABLE_TRACKING_FRAMES &&
                    !automaticPlacementAttempted &&
                    (
                        latestPlacementPose != null ||
                            (
                                placementMode == PlacementMode.AUTO &&
                                    stableTrackingFrames >= STABLE_TRACKING_FRAMES * 2
                                )
                        )
                ) {
                    automaticPlacementAttempted = true
                    updateStatus("Tracking ready - placing model...", force = true)
                    placeModel(session, cameraPose)
                    return
                }
                if (anchorNode == null) {
                    updateStatus(
                        if (stableTrackingFrames < STABLE_TRACKING_FRAMES) {
                            "Move slowly while AR stabilizes..."
                        } else if (latestPlacementPose == null) {
                            "Aim the center marker at a ${placementMode.surfaceLabel}."
                        } else {
                            "${placementMode.displayName} surface ready - placing model"
                        }
                    )
                } else if (modelNode == null) {
                    updateStatus("Loading downloaded model...")
                } else {
                    updateStatus(
                        if (modelLocked) {
                            "Model locked - walk around to inspect"
                        } else {
                            "Tracking - pinch to resize, drag to rotate"
                        }
                    )
                }
            }
            TrackingState.PAUSED -> {
                stableTrackingFrames = 0
                updateStatus("Move the phone slowly to restore tracking")
            }
            TrackingState.STOPPED -> {
                stableTrackingFrames = 0
                updateStatus("AR tracking stopped. Reopen AR to continue.", force = true)
            }
        }
    }

    private fun placeModelNow() {
        val session = latestSession
        val cameraPose = latestCameraPose
        if (session == null || cameraPose == null || stableTrackingFrames == 0) {
            updateStatus("Move the phone slowly until tracking is ready.", force = true)
            haptic()
            return
        }
        if (placementMode != PlacementMode.AUTO && latestPlacementPose == null) {
            updateStatus(
                "Aim the center marker at a ${placementMode.surfaceLabel}, then tap Place.",
                force = true
            )
            haptic()
            return
        }
        automaticPlacementAttempted = true
        placeModel(session, cameraPose, replaceExisting = anchorNode != null)
    }

    private fun placeModel(
        session: Session,
        cameraPose: Pose,
        replaceExisting: Boolean = false
    ) {
        if (anchorNode != null && !replaceExisting) return
        if (replaceExisting) clearAnchor()
        try {
            val distance = (modelSizeMeters * 1.35f).coerceIn(
                MIN_VIEWING_DISTANCE_METERS,
                MAX_VIEWING_DISTANCE_METERS
            )
            val target = latestPlacementPose?.translation
                ?: cameraPose.transformPoint(floatArrayOf(0f, 0f, -distance))
            val anchorPose = Pose.makeTranslation(target[0], target[1], target[2])
            val anchor = session.createAnchor(anchorPose)
            addModelAnchor(anchor)
            haptic()
        } catch (error: Throwable) {
            stableTrackingFrames = 0
            updateStatus(
                "Could not place the model yet. Move slowly and tap Place again.",
                force = true
            )
        }
    }

    private fun addModelAnchor(anchor: com.google.ar.core.Anchor) {
        if (isFinishing || isDestroyed) {
            runCatching { anchor.detach() }
            return
        }
        val newAnchorNode = try {
            AnchorNode(binding.arSceneView.engine, anchor).also {
                binding.arSceneView.addChildNode(it)
                anchorNode = it
            }
        } catch (error: Throwable) {
            runCatching { anchor.detach() }
            updateStatus("AR placement failed. Tap Place to retry.", force = true)
            return
        }
        modelLoadJob?.cancel()
        modelLoadJob = lifecycleScope.launch {
            try {
                updateStatus("Loading downloaded model...", force = true)
                val instance = binding.arSceneView.modelLoader.loadModelInstance(
                    Uri.fromFile(modelFile).toString()
                ) ?: throw IllegalStateException("The GLB model could not be decoded.")
                if (anchorNode !== newAnchorNode) return@launch
                val node = ModelNode(
                    modelInstance = instance,
                    scaleToUnits = modelSizeMeters,
                    centerOrigin = Position(0f, 0f, 0f)
                ).apply {
                    isEditable = true
                    isPositionEditable = false
                    isRotationEditable = true
                    isScaleEditable = true
                }
                val originalScale = node.scale
                initialModelScale = Scale(originalScale.x, originalScale.y, originalScale.z)
                val minimumScale = max(originalScale.x * 0.25f, 0.00001f)
                val maximumScale = max(originalScale.x * 4.0f, minimumScale)
                node.editableScaleRange = minimumScale..maximumScale
                node.onScaleEnd = { _, _ -> updateScaleLabel() }
                newAnchorNode.addChildNode(node)
                modelNode = node
                modelLocked = false
                modelVisible = true
                binding.arLockModel.text = "Lock"
                binding.arToggleModel.text = "Hide"
                binding.arPlaceNow.text = "Bring back"
                updateScaleLabel()
                updateStatus("Tracking - pinch to resize, drag to rotate", force = true)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (outOfMemory: OutOfMemoryError) {
                clearAnchor()
                showFatal(
                    "This model is too detailed for the available memory on this device. " +
                        "Close other apps, restart the phone, or use the standard 3D viewer."
                )
            } catch (error: Throwable) {
                clearAnchor()
                showFatal(
                    "The model could not be opened safely in AR. ${error.message.orEmpty()}"
                        .trim()
                )
            }
        }
    }

    private fun recenterModel() {
        clearAnchor()
        binding.arPlaceNow.text = "Place"
        if (latestSession != null && latestCameraPose != null && stableTrackingFrames > 0) {
            placeModelNow()
        } else {
            stableTrackingFrames = 0
            updateStatus("Move slowly, then tap Place to recenter.", force = true)
            haptic()
        }
    }

    private fun clearAnchor() {
        modelLoadJob?.cancel()
        modelLoadJob = null
        modelNode = null
        initialModelScale = null
        anchorNode?.let { node ->
            runCatching { binding.arSceneView.removeChildNode(node) }
            runCatching { node.destroy() }
        }
        anchorNode = null
        if (::binding.isInitialized) binding.arPlaceNow.text = "Place"
        updateScaleLabel()
    }

    private fun rotateModel(x: Float = 0f, y: Float = 0f, z: Float = 0f) {
        val node = modelNode ?: return modelNotReady()
        if (modelLocked) return updateStatus("Unlock the model before rotating.", force = true)
        val current = node.rotation
        node.rotation = Rotation(current.x + x, current.y + y, current.z + z)
        haptic()
    }

    private fun resizeModel(factor: Float) {
        val node = modelNode ?: return modelNotReady()
        if (modelLocked) return updateStatus("Unlock the model before resizing.", force = true)
        val original = initialModelScale ?: return
        val next = node.scale * factor
        val minimum = original.x * 0.25f
        val maximum = original.x * 4.0f
        if (next.x in minimum..maximum) {
            node.scale = next
            updateScaleLabel()
            haptic()
        }
    }

    private fun restoreActualSize() {
        val node = modelNode ?: return modelNotReady()
        val original = initialModelScale ?: return
        node.scale = Scale(original.x, original.y, original.z)
        updateScaleLabel()
        updateStatus("Actual anatomical size restored.", force = true)
        haptic()
    }

    private fun resetPose() {
        val node = modelNode ?: return modelNotReady()
        node.rotation = Rotation(0f, 0f, 0f)
        restoreActualSize()
        updateStatus("Rotation and scale reset.", force = true)
    }

    private fun toggleModelLock() {
        val node = modelNode ?: return modelNotReady()
        modelLocked = !modelLocked
        node.isEditable = !modelLocked
        binding.arLockModel.text = if (modelLocked) "Unlock" else "Lock"
        updateStatus(
            if (modelLocked) "Model locked in place." else "Model gestures enabled.",
            force = true
        )
        haptic()
    }

    private fun toggleModelVisibility() {
        val node = modelNode ?: return modelNotReady()
        modelVisible = !modelVisible
        node.isVisible = modelVisible
        binding.arToggleModel.text = if (modelVisible) "Hide" else "Show"
        updateStatus(
            if (modelVisible) "Model visible." else "Model hidden - tracking remains active.",
            force = true
        )
        haptic()
    }

    private fun cycleRotationStep() {
        rotationStepDegrees = when (rotationStepDegrees) {
            5f -> 15f
            15f -> 45f
            else -> 5f
        }
        val step = rotationStepDegrees.toInt()
        binding.arRotationStep.text = "Step $step°"
        binding.arRotationLabel.text = "ROTATE · $step° STEP"
        haptic()
    }

    private fun findPlacementPose(frame: Frame, cameraPose: Pose): Pose? {
        val width = binding.arSceneView.width
        val height = binding.arSceneView.height
        if (width <= 0 || height <= 0) return null
        return frame.hitTest(width / 2f, height / 2f)
            .firstOrNull { hit ->
                val plane = hit.trackable as? Plane ?: return@firstOrNull false
                if (plane.trackingState != TrackingState.TRACKING ||
                    !plane.isPoseInPolygon(hit.hitPose)
                ) {
                    return@firstOrNull false
                }
                val verticalDifference = cameraPose.ty() - hit.hitPose.ty()
                when (placementMode) {
                    PlacementMode.AUTO -> true
                    PlacementMode.FLOOR ->
                        plane.type == Plane.Type.HORIZONTAL_UPWARD_FACING &&
                            verticalDifference >= 1.0f
                    PlacementMode.WALL -> plane.type == Plane.Type.VERTICAL
                    PlacementMode.TABLETOP ->
                        plane.type == Plane.Type.HORIZONTAL_UPWARD_FACING &&
                            verticalDifference in 0.2f..1.25f
                }
            }
            ?.hitPose
    }

    private fun updatePlacementIndicator(surfaceReady: Boolean) {
        if (!::binding.isInitialized) return
        binding.arPlacementIndicator.setBackgroundResource(
            if (surfaceReady) {
                R.drawable.bg_ar_indicator_ready
            } else {
                R.drawable.bg_ar_indicator_pending
            }
        )
        binding.arPlacementIndicator.contentDescription =
            if (surfaceReady) {
                "${placementMode.displayName} placement surface ready"
            } else {
                "Aim at a ${placementMode.surfaceLabel}"
            }
        binding.arPlacementHint.text =
            if (surfaceReady) "${placementMode.displayName.uppercase()} READY"
            else placementMode.displayName.uppercase()
        val showMarker = anchorNode == null || measurementStartPose != null
        binding.arPlacementIndicator.visibility = if (showMarker) View.VISIBLE else View.GONE
        binding.arPlacementHint.visibility = if (showMarker) View.VISIBLE else View.GONE
    }

    private fun showPlacementModeSelector() {
        val modes = PlacementMode.entries
        AlertDialog.Builder(this)
            .setTitle("AR placement surface")
            .setSingleChoiceItems(
                modes.map { "${it.displayName} - ${it.description}" }.toTypedArray(),
                modes.indexOf(placementMode)
            ) { dialog, index ->
                placementMode = modes[index]
                automaticPlacementAttempted = false
                latestPlacementPose = null
                binding.arPlacementMode.text = "Mode: ${placementMode.displayName}"
                binding.arPlacementHint.text = placementMode.displayName.uppercase()
                updateStatus(
                    "Aim the center marker at a ${placementMode.surfaceLabel}.",
                    force = true
                )
                updatePlacementIndicator(false)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun captureMeasurementPoint() {
        val point = latestPlacementPose
        if (point == null) {
            updateStatus("Aim the center marker at a tracked surface first.", force = true)
            haptic()
            return
        }
        val start = measurementStartPose
        if (start == null) {
            measurementStartPose = point
            binding.arMeasure.text = "Set end"
            binding.arMeasurementLabel.visibility = View.VISIBLE
            binding.arMeasurementLabel.text = "Start saved - aim at the end point"
            updateStatus("Move the center marker to the end point and tap Set end.", force = true)
        } else {
            val dx = point.tx() - start.tx()
            val dy = point.ty() - start.ty()
            val dz = point.tz() - start.tz()
            val distanceMeters = sqrt(dx * dx + dy * dy + dz * dz)
            val heightMeters = kotlin.math.abs(dy)
            val relativeScale = (distanceMeters / modelSizeMeters * 100f)
                .coerceIn(0f, 9_999f)
            binding.arMeasurementLabel.text = String.format(
                Locale.getDefault(),
                "Distance %.1f cm  |  Height %.1f cm  |  %.0f%% of model",
                distanceMeters * 100f,
                heightMeters * 100f,
                relativeScale
            )
            binding.arMeasure.text = "Measure again"
            measurementStartPose = null
            updateStatus("Measurement complete.", force = true)
            updatePlacementIndicator(anchorNode == null)
        }
        haptic()
    }

    private fun updateScaleLabel() {
        if (!::binding.isInitialized) return
        val current = modelNode?.scale?.x
        val original = initialModelScale?.x
        val percent =
            if (current != null && original != null && original > 0f) {
                ((current / original) * 100f).toInt().coerceIn(25, 400)
            } else {
                100
            }
        binding.arScaleLabel.text = "SCALE · $percent%"
    }

    private fun showHelp() {
        AlertDialog.Builder(this)
            .setTitle("Using the AR anatomy viewer")
            .setMessage(
                "1. Move the phone slowly until tracking stabilizes.\n" +
                "2. Tap Place to anchor the model directly in front of you.\n" +
                    "3. Drag to rotate and pinch to resize.\n" +
                    "4. Use Actual size for anatomical scale.\n" +
                    "5. Lock prevents accidental gesture changes.\n" +
                    "6. Recenter removes the current anchor so you can place it again.\n\n" +
                    "Choose Auto, Floor, Wall, or Tabletop placement with Mode. " +
                    "For measurements, aim the center marker at the start and end points.\n\n" +
                    "For best performance, use good lighting and keep the camera moving slowly."
            )
            .setPositiveButton("Got it", null)
            .show()
    }

    private fun modelNotReady() {
        updateStatus("Wait for the model to finish loading.", force = true)
        haptic()
    }

    private fun haptic() {
        binding.arControls.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    private fun updateStatus(message: String, force: Boolean = false) {
        if (!::binding.isInitialized || isDestroyed) return
        val now = SystemClock.elapsedRealtime()
        if (force || message != lastStatus || now - lastStatusAt >= STATUS_REFRESH_MS) {
            lastStatus = message
            lastStatusAt = now
            binding.arStatus.text = message
        }
    }

    private fun showFatal(message: String) {
        if (fatalShown || isFinishing || isDestroyed) return
        fatalShown = true
        AlertDialog.Builder(this)
            .setTitle("AR unavailable")
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("Close") { _, _ -> finish() }
            .show()
    }

    private fun arErrorMessage(error: Throwable): String {
        val detail = error.message?.takeIf(String::isNotBlank)
        return detail ?: "ARCore could not start on this device. " +
            "Update Google Play Services for AR and try again."
    }

    companion object {
        const val EXTRA_MODEL_PATH = "model_path"
        const val EXTRA_MODEL_TITLE = "model_title"
        const val EXTRA_MODEL_SIZE_METERS = "model_size_meters"

        private const val AR_AVAILABILITY_RETRY_MS = 350L
        private const val STABLE_TRACKING_FRAMES = 18
        private const val STATUS_REFRESH_MS = 1_000L
        private const val MIN_VIEWING_DISTANCE_METERS = 0.65f
        private const val MAX_VIEWING_DISTANCE_METERS = 2.2f

        fun liveSizeMeters(title: String): Float {
            val normalized = title.lowercase()
            return when {
                "heart" in normalized -> 0.13f
                "liver" in normalized -> 0.27f
                "lung" in normalized -> 0.34f
                "kidney" in normalized -> 0.12f
                "brain" in normalized -> 0.17f
                "eye" in normalized -> 0.05f
                "ear" in normalized -> 0.07f
                "tooth" in normalized -> 0.05f
                "skull" in normalized -> 0.23f
                "skeleton" in normalized ||
                    "body" in normalized ||
                    "system" in normalized ||
                    "human" in normalized -> 1.72f
                else -> 0.45f
            }
        }
    }

    private enum class PlacementMode(
        val displayName: String,
        val surfaceLabel: String,
        val description: String
    ) {
        AUTO("Auto", "surface", "uses the best visible surface"),
        FLOOR("Floor", "floor", "uses a low horizontal plane"),
        WALL("Wall", "wall", "uses a vertical plane"),
        TABLETOP("Tabletop", "tabletop", "uses a raised horizontal plane")
    }
}
