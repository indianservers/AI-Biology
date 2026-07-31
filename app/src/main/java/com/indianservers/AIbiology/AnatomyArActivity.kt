package com.indianservers.AIbiology

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Pose
import com.google.ar.core.TrackingState
import com.indianservers.AIbiology.databinding.ActivityAnatomyArBinding
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.ModelNode
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.max

class AnatomyArActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAnatomyArBinding
    private var installRequested = false
    private var sceneConfigured = false
    private var anchorNode: AnchorNode? = null
    private var modelNode: ModelNode? = null
    private lateinit var modelFile: File
    private var modelSizeMeters = 0.25f

    private val cameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) ensureArReady() else showFatal(
            "Camera access is required for the augmented reality viewer."
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnatomyArBinding.inflate(layoutInflater)
        setContentView(binding.root)

        modelFile = File(intent.getStringExtra(EXTRA_MODEL_PATH).orEmpty())
        modelSizeMeters = intent.getFloatExtra(EXTRA_MODEL_SIZE_METERS, 0.25f)
            .coerceIn(0.05f, 2.0f)
        binding.arTitle.text = intent.getStringExtra(EXTRA_MODEL_TITLE) ?: "Human Anatomy"
        binding.arClose.setOnClickListener { finish() }
        binding.arShare.setOnClickListener { AppActions.shareApp(this) }
        binding.arPoweredBy.setOnClickListener { AppActions.openIndianServers(this) }
        binding.arPrivacy.setOnClickListener {
            startActivity(
                android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    Uri.parse("https://policies.google.com/privacy")
                )
            )
        }
        binding.rotateX.setOnClickListener { rotateModel(x = 15f) }
        binding.rotateY.setOnClickListener { rotateModel(y = 15f) }
        binding.rotateZ.setOnClickListener { rotateModel(z = 15f) }
        binding.arSmaller.setOnClickListener { resizeModel(0.9f) }
        binding.arLarger.setOnClickListener { resizeModel(1.1f) }
        binding.arRecenter.setOnClickListener { recenterModel() }

        if (!modelFile.isFile) {
            showFatal("Download this anatomy model before opening it in AR.")
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

    private fun ensureArReady() {
        if (sceneConfigured) return
        try {
            when (ArCoreApk.getInstance().requestInstall(this, !installRequested)) {
                ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                    installRequested = true
                    binding.arStatus.text = "Installing Google Play Services for AR..."
                }
                ArCoreApk.InstallStatus.INSTALLED -> configureScene()
            }
        } catch (error: Exception) {
            showFatal(error.message ?: "ARCore is unavailable on this device.")
        }
    }

    private fun configureScene() {
        if (sceneConfigured) return
        sceneConfigured = true
        binding.arSceneView.apply {
            lifecycle = this@AnatomyArActivity.lifecycle
            planeRenderer.isEnabled = false
            configureSession { _, config ->
                config.planeFindingMode = Config.PlaneFindingMode.DISABLED
                config.instantPlacementMode = Config.InstantPlacementMode.DISABLED
                config.depthMode = Config.DepthMode.DISABLED
                config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
            }
            onSessionUpdated = { session, frame ->
                if (frame.camera.trackingState == TrackingState.TRACKING) {
                    binding.arStatus.text =
                        if (anchorNode == null) "Positioning life-size model..."
                        else "6DOF tracking - walk around, pinch, or rotate"
                    if (anchorNode == null) {
                        val distance = max(1.0f, modelSizeMeters * 1.65f)
                        val pose = frame.camera.pose.compose(
                            Pose.makeTranslation(0f, 0f, -distance)
                        )
                        addModelAnchor(session.createAnchor(pose))
                    }
                } else {
                    binding.arStatus.text = "Move the phone slowly to start tracking"
                }
            }
        }
    }

    private fun addModelAnchor(anchor: com.google.ar.core.Anchor) {
        val newAnchorNode = AnchorNode(binding.arSceneView.engine, anchor)
        binding.arSceneView.addChildNode(newAnchorNode)
        anchorNode = newAnchorNode
        lifecycleScope.launch {
            binding.arStatus.text = "Loading downloaded model..."
            val instance = binding.arSceneView.modelLoader.loadModelInstance(
                Uri.fromFile(modelFile).path.orEmpty()
            )
            if (instance == null) {
                showFatal("The downloaded GLB could not be opened in AR.")
                return@launch
            }
            val node = ModelNode(
                modelInstance = instance,
                scaleToUnits = modelSizeMeters
            ).apply {
                isEditable = true
                isPositionEditable = false
                isRotationEditable = true
                isScaleEditable = true
            }
            newAnchorNode.addChildNode(node)
            modelNode = node
            binding.arStatus.text = "6DOF tracking - walk around, pinch, or rotate"
        }
    }

    private fun recenterModel() {
        anchorNode?.let {
            binding.arSceneView.removeChildNode(it)
            it.destroy()
        }
        anchorNode = null
        modelNode = null
        binding.arStatus.text = "Recentering in front of camera..."
    }

    private fun rotateModel(x: Float = 0f, y: Float = 0f, z: Float = 0f) {
        modelNode?.let { node ->
            val current = node.rotation
            node.rotation = Rotation(current.x + x, current.y + y, current.z + z)
        }
    }

    private fun resizeModel(factor: Float) {
        modelNode?.let { node -> node.scale = node.scale * factor }
    }

    private fun showFatal(message: String) {
        if (isFinishing) return
        AlertDialog.Builder(this)
            .setTitle("AR unavailable")
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("Close") { _, _ -> finish() }
            .show()
    }

    companion object {
        const val EXTRA_MODEL_PATH = "model_path"
        const val EXTRA_MODEL_TITLE = "model_title"
        const val EXTRA_MODEL_SIZE_METERS = "model_size_meters"

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
}
