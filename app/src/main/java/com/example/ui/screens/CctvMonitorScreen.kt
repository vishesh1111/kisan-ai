package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CctvMonitorScreen(
    onBack: () -> Unit,
    onOpenLog: () -> Unit
) {
    var isPlaying by remember { mutableStateOf(false) }
    var useMountingVideo by remember { mutableStateOf(true) }
    var heatDetected by remember { mutableStateOf(false) }

    // Fake the ML detection after 4 seconds of video playing (applies to both cameras)
    LaunchedEffect(isPlaying, useMountingVideo) {
        heatDetected = false
        if (isPlaying) {
            delay(4000)
            heatDetected = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        TopAppBar(
            title = { Text("CowCatcherAI Monitor") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Video Player Mock
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black)
                    .border(
                        width = if (heatDetected) 3.dp else 1.dp,
                        color = if (heatDetected) Color.Red else Color.DarkGray,
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                // VideoView for both thumbnail preview and playback
                // We use key(isPlaying) to force AndroidView to recreate when state changes,
                // avoiding VideoView seekTo/start bugs.
                androidx.compose.runtime.key(isPlaying, useMountingVideo) {
                    androidx.compose.ui.viewinterop.AndroidView(
                        factory = { context ->
                            android.widget.VideoView(context).apply {
                                val rawId = if (useMountingVideo) com.example.R.raw.cctv_mounting else com.example.R.raw.cctv
                                val uri = android.net.Uri.parse("android.resource://${context.packageName}/$rawId")
                                setVideoURI(uri)
                                setOnPreparedListener { mp ->
                                    mp.isLooping = true
                                    if (isPlaying) {
                                        start()
                                    } else {
                                        seekTo(100)
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                if (!isPlaying) {
                    // Semi-transparent overlay to make play button and text more visible over the thumbnail
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
                    
                    IconButton(
                        onClick = { isPlaying = true },
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(32.dp))
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Text(
                        "Click to Start Camera Feed",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp)
                    )
                } else {
                    // LIVE indicator
                    Text(
                        "LIVE REC 🔴",
                        color = Color.Red,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
                    )
                    
                    if (heatDetected) {
                        // AI Bounding Box Overlay
                        Box(
                            modifier = Modifier
                                .size(240.dp, 160.dp)
                                .border(3.dp, Color.Red)
                        ) {
                            Text(
                                "Mounting (92%)",
                                color = Color.White,
                                modifier = Modifier
                                    .background(Color.Red)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                    .align(Alignment.TopStart),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Camera Controls
            if (isPlaying) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    OutlinedButton(
                        onClick = { 
                            useMountingVideo = !useMountingVideo 
                            heatDetected = false 
                        },
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (useMountingVideo) "Switch to Normal Camera" else "Switch to Camera 2 (Action)")
                    }
                }
            }

            // Alerts Section
            AnimatedVisibility(visible = heatDetected) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0F0))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "URGENT: Heat Detected!",
                                color = Color.Red,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Cow #42 has shown mounting behavior.",
                            color = Color.Black,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onOpenLog,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                        ) {
                            Text("Save Event & View Animal")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            
            // Status Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Model: CowCatcherV15", color = Color.Gray, fontSize = 12.sp)
                Text("Status: AI Active", color = Color(0xFF4CAF50), fontSize = 12.sp)
            }
        }
    }
}
