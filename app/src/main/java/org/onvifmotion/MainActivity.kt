package org.onvifmotion

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.FileNotFoundException


const val RTSP_URL = "org.onvifmotion.RTSP_URL"

class MainActivity : AppCompatActivity(), OnvifListener {
    private val separator = "<!^^!>"
    private val storageFile = "cameras.data"

    private var logins: List<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        try {
            logins = File(filesDir, storageFile).readLines()
        } catch (ex: FileNotFoundException) {
        }
    }

    override fun requestPerformed(response: OnvifResponse) {
        Log.d("INFO", response.parsingUIMessage)
        when {
            !response.success -> {
                toast("⛔️ Request failed: ${response.request.type}")
            }
        // if GetServices have been completed, we request the device information
            response.request.type == OnvifRequest.Type.GetServices -> currentDevice.getDeviceInformation()
        // if GetDeviceInformation have been completed, we request the profiles
            response.request.type == OnvifRequest.Type.GetDeviceInformation -> {
                explanationTextView.text = response.parsingUIMessage
                currentDevice.getProfiles()

            }
        // if GetProfiles have been completed, we request the Stream URI
            response.request.type == OnvifRequest.Type.GetProfiles -> {
                currentDevice.getStreamURI()

            }
        // if GetStreamURI have been completed, we're ready to play the video
            response.request.type == OnvifRequest.Type.GetStreamURI -> {
                connectButton.text = getString(R.string.Play)
            }
        }
    }

    fun loadLogins(view: View) {
        var selectedItem: Int = -1
        AlertDialog.Builder(this)
                .setTitle("Saved Logins")
                .setSingleChoiceItems(logins!!.map { it.split(separator)[0] }.toTypedArray(), selectedItem, { _: DialogInterface, which: Int -> selectedItem = which })
                .setPositiveButton("OK", { _, _ ->
                    run {
                        if (selectedItem >= 0)
                            setCredentials(logins!![selectedItem].split(separator))
                    }
                })
                .setNegativeButton("Cancel", { dialog, _ ->
                    dialog.dismiss()
                })
                .show()
    }

    private fun setCredentials(parts: List<String>) {
        ipAddress.setText(parts[0])
        login.setText(parts[1])
        password.setText(parts[2])
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
            if (areCredentialsFilled()) {
                // Create ONVIF device with user inputs and retrieve camera informations
                currentDevice = OnvifDevice(ipAddress.text.toString(), login.text.toString(), password.text.toString())
                currentDevice.listener = this
                currentDevice.getServices()

            } else {
                toast("Please enter an IP Address login and password")
            }
        }
    }

    fun saveLogin(view: View) {
        if (areCredentialsFilled()) {
            AlertDialog.Builder(this).setTitle("Save login")
                    .setMessage("Do you want to save the camera login?")
                    .setPositiveButton("Yes", { _, _ ->
                        run {
                            saveToFile()
                            toast("Login Saved")
                        }
                    })
                    .setNegativeButton("No", { _, _ -> toast("Login not saved") })
                    .show()
        } else {
            toast("Please fill the IP Address, login and password")
        }
    }

    private fun areCredentialsFilled(): Boolean {
        return ipAddress.text.isNotEmpty() &&
                login.text.isNotEmpty() &&
                password.text.isNotEmpty()
    }

    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()

    private fun saveToFile() {
        val serializedData = ipAddress.text.toString() + separator + login.text.toString() + separator + password.text.toString()
        File(filesDir, storageFile).writeText(serializedData)
    }
}
