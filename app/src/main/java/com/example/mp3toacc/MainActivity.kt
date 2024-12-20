package com.example.mp3toacc

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.arthenica.ffmpegkit.FFmpegKit
import com.example.mp3toacc.databinding.ActivityMainBinding
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var selectedFileUri: Uri? = null
    private var selectedFileName: String = ""
    private var outputFileUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSelectFile.setOnClickListener {
            openFilePicker()
        }

        binding.btnConvert.setOnClickListener {
            openSaveFilePicker()
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "audio/mpeg"
        }
        startActivityForResult(intent, 100)
    }

    private fun openSaveFilePicker() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "audio/aac"
            putExtra(Intent.EXTRA_TITLE, "${selectedFileName.removeSuffix(".mp3")}.aac")
        }
        startActivityForResult(intent, 200)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == Activity.RESULT_OK) {
            selectedFileUri = data?.data
            selectedFileUri?.let {
                selectedFileName = getFileName(it)
                binding.tvFileName.text = "Archivo seleccionado: $selectedFileName"
                binding.btnConvert.isEnabled = true
            }
        } else if (requestCode == 200 && resultCode == Activity.RESULT_OK) {
            outputFileUri = data?.data
            outputFileUri?.let {
                convertToAAC()
            }
        }
    }

    private fun getFileName(uri: Uri): String {
        var name = ""
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                name = it.getString(nameIndex)
            }
        }
        return name
    }

    private fun convertToAAC() {
        binding.progressBar.visibility = View.VISIBLE

        val inputStream: InputStream? = selectedFileUri?.let { contentResolver.openInputStream(it) }
        val inputFile = copyInputStreamToTempFile(inputStream!!)

        val command = "-i ${inputFile.absolutePath} -c:a aac ${cacheDir}/output.aac"

        FFmpegKit.executeAsync(command) { session ->
            val returnCode = session.returnCode
            runOnUiThread {
                binding.progressBar.visibility = View.GONE
                if (returnCode.isValueSuccess) {
                    saveConvertedFileToUserLocation()
                } else {
                    Toast.makeText(this, "Error en la conversión", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun saveConvertedFileToUserLocation() {
        val inputStream = contentResolver.openInputStream(Uri.fromFile(File("${cacheDir}/output.aac")))
        val outputStream = outputFileUri?.let { contentResolver.openOutputStream(it) }

        inputStream?.use { input ->
            outputStream?.use { output ->
                input.copyTo(output)
                Toast.makeText(this, "Archivo guardado con éxito", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun copyInputStreamToTempFile(inputStream: InputStream): File {
        val tempFile = File(cacheDir, "temp_audio.mp3")
        FileOutputStream(tempFile).use { output ->
            inputStream.copyTo(output)
        }
        return tempFile
    }
}
