package com.simon.facecapture.camera


import android.annotation.SuppressLint
import android.content.*
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.ImageButton
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.video.*
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.simon.facecapture.R
import com.simon.facecapture.MainActivity
import com.simon.facecapture.camera.utils.camera.CameraManager
import com.simon.facecapture.databinding.CameraUiContainerBinding
import com.simon.facecapture.databinding.FragmentCameraBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.*


/** Milliseconds used for UI animations */
const val ANIMATION_FAST_MILLIS = 50L
const val ANIMATION_SLOW_MILLIS = 100L


@ExperimentalGetImage /**
 * Main fragment for this app. Implements all camera operations including:
 * - Viewfinder
 * - Photo taking
 * - Image analysis
 */
class CameraFragment : Fragment() {


    private var photoUri: String = ""

    private var fragmentCameraBinding: FragmentCameraBinding? = null

    private var cameraUiContainerBinding: CameraUiContainerBinding? = null

    private lateinit var outputDirectory: File

    private var cntxt: Context? = null


    private var imageCapture: ImageCapture? = null


    /** Blocking camera operations are performed using this executor */
    private lateinit var cameraExecutor: ExecutorService




    override fun onResume() {
        super.onResume()
        // Make sure that all permissions are still present, since the
        // user could have removed them while the app was in paused state.
        if (!PermissionsFragment.hasPermissions(requireContext())) {
            parentFragmentManager.commit {
                this.add(PermissionsFragment(),"PermissionsFragment")
            }

        }
    }

    override fun onDestroyView() {
        fragmentCameraBinding = null
        super.onDestroyView()

        // Shut down our background executor
        cameraExecutor.shutdown()


    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        fragmentCameraBinding = FragmentCameraBinding.inflate(inflater, container, false)
        createCameraManager()
        return fragmentCameraBinding?.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize our background executor
        cameraExecutor = Executors.newSingleThreadExecutor()



        // Determine the output directory
        outputDirectory = MainActivity.getOutputDirectory(requireContext())


        // Wait for the views to be properly laid out
        fragmentCameraBinding?.let {
            it.viewFinder.post {

                // Build UI controls
                updateCameraUi()

                cameraManager.startCamera()
                // Set up the camera and its use cases
                //setUpCamera()

            }
        }
    }
    private lateinit var cameraManager: CameraManager
    private fun createCameraManager() {

        fragmentCameraBinding?.apply {
            val screenAspectRatio =
                aspectRatio(root.width, root.height)


            cameraManager = CameraManager(
                this@CameraFragment.requireContext(),
                viewFinder,
                this@CameraFragment,
                graphicOverlayFinder, screenAspectRatio
            ){result,imagecaptures ->

                imageCapture = imagecaptures 

                if(result.size>1 ){
                    Toast.makeText(this@CameraFragment.requireContext(), "Please make sure you are the only one in frame", Toast.LENGTH_SHORT).show()
                    cameraUiContainerBinding?.cameraCaptureButton?.isEnabled = false
                }
                else if( result.isEmpty()){
                    cameraUiContainerBinding?.cameraCaptureButton?.isEnabled = false
                }
                else {
                    val face = result[0]

                    val radius = min(cameraUiContainerBinding!!.overlayView.width, cameraUiContainerBinding!!.overlayView.height) / 2f
                    val centerX = cameraUiContainerBinding!!.overlayView.x
                    val centerY = cameraUiContainerBinding!!.overlayView.y
                    if(face != null) {
                        val boundingBox = face.boundingBox
                        val boxCenterX = boundingBox.centerX().toFloat()
                        val boxCenterY = boundingBox.centerY().toFloat()
                        val top = face.boundingBox.top
                        val left = face.boundingBox.left
                        val distance = sqrt((boxCenterX - centerX).pow(2) + (boxCenterY - centerY).pow(2))
                        Log.d("FRAME",distance.toString() )
                        Log.d("FRAME RADIUS",radius.toString() )
                        if (distance <= radius && distance < 300 && top > 20 && left>80) {
                            Log.d("FRAME","IN FRAME" )
                            cameraUiContainerBinding?.overlay?.setCircleColor(ContextCompat.getColor(requireContext(),
                                R.color.colorPrimary))
                            cameraUiContainerBinding?.cameraCaptureButton?.isEnabled = true
                        }
                        else{
                            Log.d("FRAME", "NOT IN FRAME")
                            cameraUiContainerBinding?.cameraCaptureButton?.isEnabled = false
                            cameraUiContainerBinding?.overlay?.setCircleColor(ContextCompat.getColor(requireContext(),
                                R.color.white))
                        }
                    }




                }
            }
        }
    }

    /**
     * Inflate camera controls and update the UI manually upon config changes to avoid removing
     * and re-adding the view finder from the view hierarchy; this provides a seamless rotation
     * transition on devices that support it.
     *
     * NOTE: The flag is supported starting in Android 8 but there still is a small flash on the
     * screen for devices that run Android 9 or below.
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        this.cntxt = context
    }

    override fun onDetach() {
        super.onDetach()
        this.cntxt = null
    }


    private fun observeCameraState(cameraInfo: CameraInfo) {
        cameraInfo.cameraState.observe(viewLifecycleOwner) { cameraState ->
            run {
                when (cameraState.type) {
                    CameraState.Type.PENDING_OPEN -> {
                        // Ask the user to close other camera apps
                        Toast.makeText(
                            context,
                            "CameraState: Pending Open",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    CameraState.Type.OPENING -> {
                        // Show the Camera UI
                        Toast.makeText(
                            context,
                            "CameraState: Opening",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    CameraState.Type.OPEN -> {
                        // Setup Camera resources and begin processing
                        Toast.makeText(
                            context,
                            "CameraState: Open",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    CameraState.Type.CLOSING -> {
                        // Close camera UI
                        Toast.makeText(
                            context,
                            "CameraState: Closing",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    CameraState.Type.CLOSED -> {
                        // Free camera resources
                        Toast.makeText(
                            context,
                            "CameraState: Closed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            cameraState.error?.let { error ->
                when (error.code) {
                    // Open errors
                    CameraState.ERROR_STREAM_CONFIG -> {
                        // Make sure to setup the use cases properly
                        Toast.makeText(
                            context,
                            "Stream config error",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    // Opening errors
                    CameraState.ERROR_CAMERA_IN_USE -> {
                        // Close the camera or ask user to close another camera app that's using the
                        // camera
                        Toast.makeText(
                            context,
                            "Camera in use",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    CameraState.ERROR_MAX_CAMERAS_IN_USE -> {
                        // Close another open camera in the app, or ask the user to close another
                        // camera app that's using the camera
                        Toast.makeText(
                            context,
                            "Max cameras in use",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    CameraState.ERROR_OTHER_RECOVERABLE_ERROR -> {
                        Toast.makeText(
                            context,
                            "Other recoverable error",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    // Closing errors
                    CameraState.ERROR_CAMERA_DISABLED -> {
                        // Ask the user to enable the device's cameras
                        Toast.makeText(
                            context,
                            "Camera disabled",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    CameraState.ERROR_CAMERA_FATAL_ERROR -> {
                        // Ask the user to reboot the device to restore camera function
                        Toast.makeText(
                            context,
                            "Fatal error",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    // Closed errors
                    CameraState.ERROR_DO_NOT_DISTURB_MODE_ENABLED -> {
                        // Ask the user to disable the "Do Not Disturb" mode, then reopen the camera
                        Toast.makeText(
                            context,
                            "Do not disturb mode enabled",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    /**
     *  [androidx.camera.core.ImageAnalysis.Builder] requires enum value of
     *  [androidx.camera.core.AspectRatio]. Currently it has values of 4:3 & 16:9.
     *
     *  Detecting the most suitable ratio for dimensions provided in @params by counting absolute
     *  of preview ratio to one of the provided values.
     *
     *  @param width - preview width
     *  @param height - preview height
     *  @return suitable aspect ratio
     */
    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }


    /** Method used to re-draw the camera UI controls, called every time configuration changes. */
    private fun updateCameraUi() {

        fragmentCameraBinding?.let { binding ->


            // Remove previous UI if any
            cameraUiContainerBinding?.root?.let {
                binding.root.removeView(it)
            }

            cameraUiContainerBinding = CameraUiContainerBinding.inflate(
                LayoutInflater.from(requireContext()),
                binding.root,
                true
            )


            cameraUiContainerBinding?.flipButton?.setOnClickListener {
                cameraManager.changeCameraSelector()
            }

            // Listener for button used to capture photo
            cameraUiContainerBinding?.cameraCaptureButton?.setOnClickListener {
                takePhoto()
            }

        }
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        imageCapture?.let { imageCapture ->

            // Create output file to hold the image
            val photoFile = createFile(outputDirectory, FILENAME, PHOTO_EXTENSION)

            // Create output options object which contains file + metadata
            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile)
                .build()

            // Setup image capture listener which is triggered after photo has been taken
            imageCapture.takePicture(
                outputOptions, cameraExecutor, object : ImageCapture.OnImageSavedCallback {
                    override fun onError(exc: ImageCaptureException) {
                        Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                    }

                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        val savedUri: Uri = output.savedUri ?: Uri.fromFile(photoFile)
                        Log.d(TAG, "Photo capture succeeded: $savedUri")
                        photoUri = savedUri.toString()

                        // Implicit broadcasts will be ignored for devices running API level >= 24
                        // so if you only target API level 24+ you can remove this statement
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                            requireActivity().sendBroadcast(
                                Intent(android.hardware.Camera.ACTION_NEW_PICTURE, savedUri)
                            )
                        }

                        // If the folder selected is an external media directory, this is
                        // unnecessary but otherwise other apps will not be able to access our
                        // images unless we scan them using [MediaScannerConnection]
                        val mimeType = MimeTypeMap.getSingleton()
                            .getMimeTypeFromExtension(savedUri.toFile().extension)
                        MediaScannerConnection.scanFile(
                            context,
                            arrayOf(savedUri.toFile().absolutePath),
                            arrayOf(mimeType)
                        ) { _, uri ->
                            Log.d(TAG, "Image capture scanned into media store: $uri")
                        }
                    }
                })

            // We can only change the foreground Drawable using API level 23+ API
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                // Display flash animation to indicate that photo was captured
                fragmentCameraBinding?.let { binding ->
                    binding.root.postDelayed({
                        binding.root.foreground = ColorDrawable(Color.WHITE)
                        binding.root.postDelayed(
                            { binding.root.foreground = null }, ANIMATION_FAST_MILLIS
                        )
                    }, ANIMATION_SLOW_MILLIS)
                }
            }
        }
    }

    companion object {

        private const val TAG = "CameraFragment"
        const val FILENAME = "yyyy-MM-dd-HH-mm-ss-SSS"
        const val PHOTO_EXTENSION = ".jpg"
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0

        /** Helper function used to create a timestamped file */
        fun createFile(baseFolder: File, format: String, extension: String) =
            File(
                baseFolder, SimpleDateFormat(format, Locale.US)
                    .format(System.currentTimeMillis()) + extension
            )
    }
}
