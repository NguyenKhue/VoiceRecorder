package com.khue.voicerecorder

import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.khue.voicerecorder.ui.theme.VoiceRecorderTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream


class MainActivity : ComponentActivity() {

    private var byteArrayOutputStream = ByteArrayOutputStream()

    private var descriptors: Array<ParcelFileDescriptor> = ParcelFileDescriptor.createPipe()
    private var parcelRead = ParcelFileDescriptor(descriptors[0])
    private var parcelWrite = ParcelFileDescriptor(descriptors[1])

    private var inputStream: InputStream = ParcelFileDescriptor.AutoCloseInputStream(parcelRead)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val recorder = MediaRecorder()
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        recorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB)
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
        recorder.setOutputFile(parcelWrite.fileDescriptor)
        recorder.prepare()

        setContent {
            VoiceRecorderTheme {
                // A surface container using the 'background' color from the theme
                val scope = rememberCoroutineScope()
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column {
                        Button(onClick = {
                            scope.launch(Dispatchers.IO) {
                                try {
                                    recorder.start()
                                    var read: Int
                                    val data = ByteArray(16384)

                                    var count = 0

                                    while (inputStream.read(data, 0, data.size)
                                            .also {
                                                read = it
                                                Log.d("AudioRecorder", "read $it")
                                            } != -1
                                    ) {
                                        byteArrayOutputStream.write(data, 0, read)
                                        Log.d("AudioRecorder", "record count ${count++}")
                                    }
                                    Log.d("AudioRecorder", "record stop")
                                    byteArrayOutputStream.flush()
                                } catch (e: Exception) {
                                    Log.e("AudioRecorder", "record error ${e.message}")
                                }
                            }
                        }) {
                            Text("Start")
                        }
                        Button(onClick = {
                            scope.launch(Dispatchers.IO) {
                                recorder.stop()
                                recorder.reset()
                                recorder.release()
                                Log.d("AudioRecorder", "record : ${byteArrayOutputStream.toByteArray().size}")
                            }
                        }) {
                            Text("Stop")
                        }

                        Button(onClick = {
                            scope.launch(Dispatchers.IO) {
                                playMp3(byteArrayOutputStream.toByteArray())
                            }
                        }) {
                            Text("Play")
                        }
                    }
                }
            }
        }
    }

    private fun playMp3(mp3SoundByteArray: ByteArray) {
        try {
            val path: File = File(cacheDir.absolutePath + "/musicfile.3gp")
            val fos = FileOutputStream(path)
            fos.write(mp3SoundByteArray)
            fos.close()
            val mediaPlayer = MediaPlayer()
            val fis = FileInputStream(path)
            mediaPlayer.setDataSource(cacheDir.absolutePath + "/musicfile.3gp")
            mediaPlayer.prepare()
            mediaPlayer.start()
        } catch (ex: IOException) {
            val s = ex.toString()
            ex.printStackTrace()
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    VoiceRecorderTheme {
        Greeting("Android")
    }
}