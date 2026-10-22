package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaRecorder
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.KisanTopAppBar
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.pytorch.LiteModuleLoader
import java.io.File
import java.io.IOException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoultryScannerScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isRecording by remember { mutableStateOf(false) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var resultLabel by remember { mutableStateOf<String?>(null) }
    var resultConfidence by remember { mutableFloatStateOf(0f) }
    var selectedAudioFileName by remember { mutableStateOf<String?>(null) }
    var hasPermission by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableIntStateOf(0) }
    val maxRecordingSeconds = 45

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = getFileName(context, uri)
            selectedAudioFileName = fileName
            coroutineScope.launch {
                isAnalyzing = true
                resultLabel = null
                val audioData = decodeAudioUriToPcm(context, uri)
                if (audioData == null || audioData.isEmpty()) {
                    resultLabel = "ERR: Unable to decode audio file"
                    resultConfidence = 0f
                } else {
                    val result = analyzeAudioLocally(context, audioData)
                    resultLabel = result.first
                    resultConfidence = result.second
                }
                isAnalyzing = false
            }
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    Scaffold(
        topBar = {
            KisanTopAppBar(
                title = "Poultry Audio AI",
                showBackButton = true,
                onBackClick = onBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = InfoBlueContainer)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(InfoBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AI Cough & Distress Detector",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = InfoBlue
                        )
                        Text(
                            text = "Record up to 45 seconds of coop audio or upload an audio file. AI analyses flock acoustics for respiratory distress.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Record Button UI
            Box(
                modifier = Modifier
                    .size(190.dp)
                    .clip(CircleShape)
                    .background(if (isRecording) AlertRed.copy(alpha = 0.1f) else PrimaryGreen.copy(alpha = 0.1f))
                    .clickable(enabled = !isAnalyzing && hasPermission) {
                        if (isRecording) {
                            // User pressed stop manually before 45s
                            isRecording = false
                        } else {
                            selectedAudioFileName = null
                            coroutineScope.launch {
                                isRecording = true
                                recordingSeconds = 0
                                resultLabel = null

                                // Launch a live-timer coroutine in parallel
                                val timerJob = launch {
                                    while (isRecording && recordingSeconds < maxRecordingSeconds) {
                                        delay(1000)
                                        recordingSeconds++
                                        if (recordingSeconds >= maxRecordingSeconds) {
                                            isRecording = false
                                        }
                                    }
                                }

                                // Record audio (blocks until isRecording=false or max duration)
                                val audioData = recordAudioWithDuration(context, maxRecordingSeconds) { isRecording }

                                timerJob.cancel()
                                isRecording = false
                                isAnalyzing = true

                                // Analyze by sliding 1.5s windows over the full recording
                                val result = analyzeAudioLocally(context, audioData)
                                resultLabel = result.first
                                resultConfidence = result.second
                                isAnalyzing = false
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isRecording -> AlertRed
                                isAnalyzing -> WarningYellow
                                else -> PrimaryGreen
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isAnalyzing) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(56.dp))
                    } else {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = "Record",
                            tint = Color.White,
                            modifier = Modifier.size(60.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = when {
                    !hasPermission -> "Microphone permission required"
                    isRecording -> "Recording... ${recordingSeconds}s / ${maxRecordingSeconds}s  (Tap to stop early)"
                    isAnalyzing -> "Analyzing audio with AI..."
                    else -> "Tap to Listen to Flock"
                },
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (isRecording) AlertRed else Color.Gray,
                textAlign = TextAlign.Center
            )

            // Progress bar while recording
            if (isRecording) {
                LinearProgressIndicator(
                    progress = { recordingSeconds.toFloat() / maxRecordingSeconds.toFloat() },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    color = AlertRed,
                    trackColor = AlertRed.copy(alpha = 0.2f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Upload Audio File Option
            OutlinedButton(
                onClick = { audioPickerLauncher.launch("audio/*") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, InfoBlue),
                enabled = !isRecording && !isAnalyzing
            ) {
                Icon(imageVector = Icons.Default.AudioFile, contentDescription = null, tint = InfoBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Upload Audio File (WAV/MP3/M4A)", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = InfoBlue)
            }

            // Show selected filename if uploaded
            if (selectedAudioFileName != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                ) {
                    Icon(imageVector = Icons.Default.AudioFile, contentDescription = null, tint = InfoBlue, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Uploaded: $selectedAudioFileName",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = InfoBlue,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Results UI
            AnimatedVisibility(visible = resultLabel != null) {
                val isNoAudio = resultLabel?.startsWith("NO_AUDIO") == true
                val isHealthy = resultLabel == "Healthy"
                val isUnhealthy = resultLabel == "Unhealthy"
                val isNoise = resultLabel == "Noise"
                val isError = resultLabel?.startsWith("ERR") == true
                
                val debugRms = if (isNoAudio) resultLabel?.substringAfter("NO_AUDIO_") else null

                val bgColor = when {
                    isNoAudio || isNoise || isError -> Color(0xFFFFF3E0) // soft orange container
                    isHealthy -> SecondaryContainerGreen
                    else -> AlertRedContainer
                }
                val iconColor = when {
                    isNoAudio || isNoise || isError -> Color(0xFFE65100) // deep warning orange
                    isHealthy -> PrimaryGreen
                    else -> AlertRed
                }
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = bgColor)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = when {
                                isNoAudio -> Icons.Default.MicOff
                                isNoise -> Icons.Default.VolumeMute
                                isError -> Icons.Default.Info
                                isHealthy -> Icons.Default.CheckCircle
                                else -> Icons.Default.Warning
                            },
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = when {
                                isNoAudio -> "No Audio / Silence Detected"
                                isNoise -> "Ambient Noise Detected"
                                isError -> "Analysis Error"
                                else -> "Analysis: ${resultLabel?.uppercase()}"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = iconColor
                        )
                        if (isHealthy || isUnhealthy) {
                            Text(
                                text = "Confidence: ${(resultConfidence * 100).toInt()}%",
                                fontSize = 14.sp,
                                color = Color.DarkGray
                            )
                        } else if (isNoAudio && debugRms != null) {
                            Text(
                                text = "Audio Level (RMS): $debugRms (Too low)",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = when {
                                isNoAudio -> "No chicken vocalizations detected. The audio is silent or too quiet. Please hold the phone closer to the flock or upload a recording with clear bird sounds."
                                isNoise -> "Only ambient background noise was detected without distinctive bird vocalizations. Please record closer to the flock."
                                isError -> "Something went wrong decoding or analyzing the audio. Please try again."
                                isUnhealthy -> "Respiratory distress / coughing detected in the audio sample. Isolate affected birds and consult a vet."
                                else -> "Flock vocalizations sound normal. No widespread respiratory issues detected."
                            },
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

// ── Audio Constants ──
private const val SAMPLE_RATE = 22050
private const val WINDOW_SAMPLES = 33075 // 1.5 seconds per window

// ── Audio Recording Logic ──
private suspend fun recordAudioWithDuration(
    context: Context,
    maxSeconds: Int,
    isStillRecording: () -> Boolean
): FloatArray = withContext(Dispatchers.IO) {
    val maxSamples = SAMPLE_RATE * maxSeconds
    val minBufferSize = AudioRecord.getMinBufferSize(
        SAMPLE_RATE,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    )
    val chunkSize = maxOf(minBufferSize, SAMPLE_RATE / 4) // read in 250ms chunks
    val audioRecord = AudioRecord(
        MediaRecorder.AudioSource.MIC,
        SAMPLE_RATE,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT,
        chunkSize * 2
    )

    val allSamples = ArrayList<Float>(maxSamples)
    val shortChunk = ShortArray(chunkSize)

    try {
        audioRecord.startRecording()
        while (isStillRecording() && allSamples.size < maxSamples) {
            val read = audioRecord.read(shortChunk, 0, chunkSize)
            if (read <= 0) break
            for (i in 0 until read) {
                allSamples.add(shortChunk[i].toFloat() / 32768.0f)
            }
        }
    } catch (e: SecurityException) {
        e.printStackTrace()
    } finally {
        try {
            audioRecord.stop()
            audioRecord.release()
        } catch (_: Exception) {}
    }

    allSamples.toFloatArray()
}

// ── Helper to retrieve display filename from Uri ──
private fun getFileName(context: Context, uri: Uri): String {
    var name = "audio_file"
    try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                name = cursor.getString(nameIndex)
            }
        }
    } catch (_: Exception) {}
    return name
}

// ── Audio Decoding for Uploaded Files (MP3, WAV, M4A, AAC, OGG) ──
private suspend fun decodeAudioUriToPcm(context: Context, uri: Uri): FloatArray? = withContext(Dispatchers.IO) {
    // 1. Try fast direct WAV reader first (for uncompressed 16-bit PCM WAV)
    val directWav = tryDirectWavRead(context, uri)
    if (directWav != null && directWav.isNotEmpty()) {
        return@withContext directWav
    }

    // 2. Decode via MediaExtractor and MediaCodec
    var extractor: MediaExtractor? = null
    var codec: MediaCodec? = null
    try {
        extractor = MediaExtractor()
        extractor.setDataSource(context, uri, null)

        var audioTrackIndex = -1
        var trackFormat: MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
            if (mime.startsWith("audio/")) {
                audioTrackIndex = i
                trackFormat = format
                break
            }
        }

        if (audioTrackIndex == -1 || trackFormat == null) {
            return@withContext null
        }

        extractor.selectTrack(audioTrackIndex)
        val mime = trackFormat.getString(MediaFormat.KEY_MIME) ?: return@withContext null

        codec = MediaCodec.createDecoderByType(mime)
        codec.configure(trackFormat, null, null, 0)
        codec.start()

        val pcmList = ArrayList<Float>()
        val bufferInfo = MediaCodec.BufferInfo()
        var isEOS = false
        var outputSampleRate = if (trackFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
            trackFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        } else {
            SAMPLE_RATE
        }
        var outputChannelCount = if (trackFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
            trackFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        } else {
            1
        }
        val timeoutUs = 5000L

        val startTime = System.currentTimeMillis()
        val maxDecodeTimeMs = 15000L

        while (!isEOS && System.currentTimeMillis() - startTime < maxDecodeTimeMs) {
            val inIndex = codec.dequeueInputBuffer(timeoutUs)
            if (inIndex >= 0) {
                val inputBuffer = codec.getInputBuffer(inIndex)
                if (inputBuffer != null) {
                    val sampleSize = extractor.readSampleData(inputBuffer, 0)
                    if (sampleSize < 0) {
                        codec.queueInputBuffer(inIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    } else {
                        val presentationTimeUs = extractor.sampleTime
                        codec.queueInputBuffer(inIndex, 0, sampleSize, presentationTimeUs, 0)
                        extractor.advance()
                    }
                }
            }

            var outIndex = codec.dequeueOutputBuffer(bufferInfo, timeoutUs)
            while (outIndex >= 0) {
                val outputBuffer = codec.getOutputBuffer(outIndex)
                if (outputBuffer != null && bufferInfo.size > 0) {
                    outputBuffer.position(bufferInfo.offset)
                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size)

                    val shortBuf = outputBuffer.order(java.nio.ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                    val numShorts = shortBuf.remaining()

                    if (outputChannelCount > 1) {
                        var i = 0
                        while (i + outputChannelCount <= numShorts) {
                            var sum = 0f
                            for (c in 0 until outputChannelCount) {
                                sum += shortBuf.get(i + c).toFloat() / 32768.0f
                            }
                            pcmList.add(sum / outputChannelCount)
                            i += outputChannelCount
                        }
                    } else {
                        for (i in 0 until numShorts) {
                            pcmList.add(shortBuf.get().toFloat() / 32768.0f)
                        }
                    }
                }

                codec.releaseOutputBuffer(outIndex, false)

                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    isEOS = true
                    break
                }

                outIndex = codec.dequeueOutputBuffer(bufferInfo, timeoutUs)
            }

            if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                val newFormat = codec.outputFormat
                if (newFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                    outputSampleRate = newFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                }
                if (newFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                    outputChannelCount = newFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                }
            }
        }

        val rawArray = pcmList.toFloatArray()
        if (rawArray.isEmpty()) return@withContext null

        if (outputSampleRate != SAMPLE_RATE) {
            resampleLinear(rawArray, outputSampleRate, SAMPLE_RATE)
        } else {
            rawArray
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    } finally {
        try {
            codec?.stop()
            codec?.release()
        } catch (_: Exception) {}
        try {
            extractor?.release()
        } catch (_: Exception) {}
    }
}

// ── Direct WAV Parser Fallback (16-bit PCM) ──
private fun tryDirectWavRead(context: Context, uri: Uri): FloatArray? {
    try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val bytes = stream.readBytes()
            if (bytes.size < 44) return null
            val riff = String(bytes, 0, 4)
            val wave = String(bytes, 8, 4)
            if (riff != "RIFF" || wave != "WAVE") return null

            val buffer = java.nio.ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN)
            var offset = 12
            var channels = 1
            var sampleRate = 22050
            var bitsPerSample = 16
            var dataOffset = -1
            var dataSize = 0

            while (offset + 8 <= bytes.size) {
                val chunkId = String(bytes, offset, 4)
                val chunkSize = buffer.getInt(offset + 4)
                offset += 8
                if (chunkId == "fmt ") {
                    channels = buffer.getShort(offset + 2).toInt()
                    sampleRate = buffer.getInt(offset + 4)
                    bitsPerSample = buffer.getShort(offset + 14).toInt()
                } else if (chunkId == "data") {
                    dataOffset = offset
                    dataSize = chunkSize.coerceAtMost(bytes.size - offset)
                    break
                }
                offset += chunkSize
            }

            if (dataOffset == -1 || bitsPerSample != 16 || channels <= 0) return null

            buffer.position(dataOffset)
            val shortCount = dataSize / 2
            val rawPcm = ArrayList<Float>(shortCount / channels)

            if (channels > 1) {
                var s = 0
                while (s + channels <= shortCount) {
                    var sum = 0f
                    for (c in 0 until channels) {
                        sum += buffer.short.toFloat() / 32768.0f
                    }
                    rawPcm.add(sum / channels)
                    s += channels
                }
            } else {
                for (s in 0 until shortCount) {
                    rawPcm.add(buffer.short.toFloat() / 32768.0f)
                }
            }

            val floatArray = rawPcm.toFloatArray()
            return if (sampleRate != SAMPLE_RATE) {
                resampleLinear(floatArray, sampleRate, SAMPLE_RATE)
            } else {
                floatArray
            }
        }
    } catch (_: Exception) {}
    return null
}

// ── Linear Interpolation Resampler ──
private fun resampleLinear(input: FloatArray, fromRate: Int, toRate: Int): FloatArray {
    if (fromRate == toRate || input.isEmpty() || fromRate <= 0 || toRate <= 0) return input
    val ratio = fromRate.toDouble() / toRate.toDouble()
    val outputLength = (input.size / ratio).toInt()
    if (outputLength <= 0) return FloatArray(0)
    val output = FloatArray(outputLength)
    for (i in 0 until outputLength) {
        val srcIdx = i * ratio
        val index0 = srcIdx.toInt().coerceIn(0, input.size - 1)
        val index1 = (index0 + 1).coerceIn(0, input.size - 1)
        val frac = (srcIdx - index0).toFloat()
        output[i] = input[index0] * (1f - frac) + input[index1] * frac
    }
    return output
}

// ── On-Device ML Inference (Sliding Window) ──
private suspend fun analyzeAudioLocally(context: Context, audioData: FloatArray): Pair<String, Float> = withContext(Dispatchers.IO) {
    try {
        if (audioData.isEmpty()) {
            return@withContext Pair("NO_AUDIO_0.0000", 0f)
        }

        // 1. Calculate overall energy & peak amplitude across the full recording
        var sumSquares = 0.0
        var maxAmp = 0.0f
        for (sample in audioData) {
            val a = kotlin.math.abs(sample)
            if (a > maxAmp) maxAmp = a
            sumSquares += (sample * sample).toDouble()
        }
        val rms = kotlin.math.sqrt(sumSquares / audioData.size.coerceAtLeast(1)).toFloat()

        // 2. Reject silence / blank audio / room hiss
        // Audible bird vocalizations produce peak amplitude > 0.07f and RMS > 0.025f.
        if (maxAmp < 0.07f || rms < 0.025f) {
            val formattedRms = String.format("%.4f", rms)
            return@withContext Pair("NO_AUDIO_$formattedRms", 0f)
        }

        // 3. Load model once
        val modelPath = assetFilePath(context, "chicken_model_lite.ptl")
        val module = LiteModuleLoader.load(modelPath)

        // Pad if audio is shorter than 1.5s window
        val processedAudio = if (audioData.size < WINDOW_SAMPLES) {
            val padded = FloatArray(WINDOW_SAMPLES)
            System.arraycopy(audioData, 0, padded, 0, audioData.size)
            padded
        } else {
            audioData
        }

        // 4. Slide 1.5s windows with 50% overlap over the full recording
        val stepSize = WINDOW_SAMPLES / 2
        val classCounts = IntArray(3) // 0: Healthy, 1: Noise, 2: Unhealthy
        val classConfidences = FloatArray(3)
        var windowsAnalyzed = 0

        var offset = 0
        while (offset + WINDOW_SAMPLES <= processedAudio.size) {
            val window = processedAudio.copyOfRange(offset, offset + WINDOW_SAMPLES)
            offset += stepSize

            // Check window energy — skip quiet windows
            var wSumSq = 0.0
            var wMaxAmp = 0.0f
            for (s in window) {
                val a = kotlin.math.abs(s)
                if (a > wMaxAmp) wMaxAmp = a
                wSumSq += (s * s).toDouble()
            }
            val wRms = kotlin.math.sqrt(wSumSq / WINDOW_SAMPLES).toFloat()
            if (wMaxAmp < 0.06f || wRms < 0.02f) continue

            // Run inference on this window
            val tensor = org.pytorch.Tensor.fromBlob(window, longArrayOf(1, WINDOW_SAMPLES.toLong()))
            val outputTensor = module.forward(org.pytorch.IValue.from(tensor)).toTensor()
            val scores = outputTensor.dataAsFloatArray

            // Softmax
            var maxScore = scores[0]
            var maxIdx = 0
            for (i in 1 until 3) {
                if (scores[i] > maxScore) { maxScore = scores[i]; maxIdx = i }
            }
            var sumExp = 0f
            for (i in 0 until 3) sumExp += kotlin.math.exp((scores[i] - maxScore).toDouble()).toFloat()
            val conf = kotlin.math.exp((scores[maxIdx] - maxScore).toDouble()).toFloat() / sumExp

            classCounts[maxIdx]++
            classConfidences[maxIdx] += conf
            windowsAnalyzed++
        }

        if (windowsAnalyzed == 0) {
            val formattedRms = String.format("%.4f", rms)
            return@withContext Pair("NO_AUDIO_$formattedRms", 0f)
        }

        // Decision logic:
        // Index 2 = Unhealthy (respiratory distress, coughing)
        // Index 1 = Noise (ambient noise, motor, fan, non-vocalization)
        // Index 0 = Healthy (normal chicken vocalizations)
        val finalIdx = when {
            classCounts[2] > 0 -> 2 // Any distress detected -> Unhealthy
            classCounts[0] > classCounts[1] && classCounts[0] > 0 -> 0 // Healthy chicken sounds dominate
            classCounts[1] > 0 -> 1 // Only ambient noise detected
            else -> 1 // Default to Noise rather than false Healthy
        }
        val avgConf = if (classCounts[finalIdx] > 0)
            classConfidences[finalIdx] / classCounts[finalIdx]
        else 0f

        val classes = arrayOf("Healthy", "Noise", "Unhealthy")
        Pair(classes[finalIdx], avgConf)

    } catch (e: Exception) {
        e.printStackTrace()
        Pair("ERR: ${e.message?.take(30)}", 0f)
    }
}
