package org.onvifmotion

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.rvirin.onvif.R
import org.onvifmotion.OnvifRequest.Type.*
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar

const val RTSP_URL = "org.onvifmotion.RTSP_URL"

/**
 * Main activity of this demo project. It allows the user to type his camera IP address,
 * login and password.
 */
class MainActivity : AppCompatActivity(), OnvifListener {

    private val mLoaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            val TAG = "MainActivity"
            when (status) {
                LoaderCallbackInterface.SUCCESS -> {
                    Log.i(TAG, "OpenCV Manager Connected")
                    //from now onwards, you can use OpenCV API
                    val m = Mat(5, 10, CvType.CV_8UC1, Scalar(0.0))
                }
                LoaderCallbackInterface.INIT_FAILED -> Log.i(TAG, "Init Failed")
                LoaderCallbackInterface.INSTALL_CANCELED -> Log.i(TAG, "Install Cancelled")
                LoaderCallbackInterface.INCOMPATIBLE_MANAGER_VERSION -> Log.i(TAG, "Incompatible Version")
                LoaderCallbackInterface.MARKET_ERROR -> Log.i(TAG, "Market Error")
                else -> {
                    Log.i(TAG, "OpenCV Manager Install")
                    super.onManagerConnected(status)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        //initialize OpenCV manager
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        findViewById<EditText>(R.id.ipAddress).setText("195.60.68.239")
//        findViewById<EditText>(R.id.login).setText("operator")
//        findViewById<EditText>(R.id.password).setText("Onv!f2018")

//        findViewById<EditText>(R.id.ipAddress).setText("60.191.94.122:8086")
//        findViewById<EditText>(R.id.login).setText("admin")
//        findViewById<EditText>(R.id.password).setText("admin321")

//        findViewById<EditText>(R.id.ipAddress).setText("123.157.208.28")
//        findViewById<EditText>(R.id.login).setText("admin")
//        findViewById<EditText>(R.id.password).setText("abcd1234")

//        findViewById<EditText>(R.id.ipAddress).setText("61.164.52.166:88")
//        findViewById<EditText>(R.id.login).setText("admin")
//        findViewById<EditText>(R.id.password).setText("Uniview2018")

//        findViewById<EditText>(R.id.ipAddress).setText("193.159.244.134")
//        findViewById<EditText>(R.id.login).setText("service")
//        findViewById<EditText>(R.id.password).setText("Xbks8tr8vT")

        findViewById<EditText>(R.id.ipAddress).setText("193.159.244.132")
        findViewById<EditText>(R.id.login).setText("service")
        findViewById<EditText>(R.id.password).setText("Xbks8tr8vT")

    }

    override fun requestPerformed(response: OnvifResponse) {

        Log.d("INFO", response.parsingUIMessage)

        if (!response.success) {
            Log.e("ERROR", "request failed: ${response.request.type} \n Response: ${response.error}")
            toast("⛔️ Request failed: ${response.request.type}")
        }
        // if GetServices have been completed, we request the device information
        else if (response.request.type == GetServices) {
            currentDevice.getDeviceInformation()
        }
        // if GetDeviceInformation have been completed, we request the profiles
        else if (response.request.type == GetDeviceInformation) {

            val textView = findViewById<TextView>(R.id.explanationTextView)
            textView.text = response.parsingUIMessage
            toast("Device information retrieved 👍")
            currentDevice.getProfiles()

        }
        // if GetProfiles have been completed, we request the Stream URI
        else if (response.request.type == GetProfiles) {
            val profilesCount = currentDevice.mediaProfiles.count()
            toast("$profilesCount profiles retrieved 😎")

            currentDevice.getStreamURI()

        }
        // if GetStreamURI have been completed, we're ready to play the video
        else if (response.request.type == GetStreamURI) {

            val button = findViewById<TextView>(R.id.button)
            button.text = getString(R.string.Play)

            toast("Stream URI retrieved,\nready for the movie 🍿")
        }
    }

    fun buttonClicked(view: View) {

        // If we were able to retrieve information from the camera, and if we have a rtsp uri,
        // We open StreamActivity and pass the rtsp URI
        if (currentDevice.isConnected) {
            currentDevice.rtspURI?.let { uri ->
                val intent = Intent(this, StreamActivity::class.java).apply {
                    putExtra(RTSP_URL, uri)
                }
                startActivity(intent)
            } ?: run {
                toast("RTSP URI haven't been retrieved")
            }
        } else {

            // get the information type by the user to create the Onvif device
            val ipAddress = (findViewById<EditText>(R.id.ipAddress)).text.toString()
            val login = (findViewById<EditText>(R.id.login)).text.toString()
            val password = (findViewById<EditText>(R.id.password)).text.toString()

            if (ipAddress.isNotEmpty() &&
                    login.isNotEmpty() &&
                    password.isNotEmpty()) {

                // Create ONVIF device with user inputs and retrieve camera informations
                currentDevice = OnvifDevice(ipAddress, login, password)
                currentDevice.listener = this
                currentDevice.getServices()

            } else {
                toast("Please enter an IP Address login and password")
            }
        }
    }

    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
}
