package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.CropPathologyResult
import com.example.data.model.ResultProvenance
import com.example.ui.components.KisanTopAppBar
import com.example.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CropDoctorScreen(
    isAnalyzing: Boolean,
    diagnosisResult: String?,
    pathologyResult: CropPathologyResult?,
    onScanPhoto: (Bitmap?, String) -> Unit,
    onClearDiagnosis: () -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current

    var selectedCropName by remember { mutableStateOf("Wheat") }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isSampleImageUsed by remember { mutableStateOf(true) }

    val cropOptions = listOf("Wheat", "Paddy / Rice", "Cotton", "Mustard", "Potato", "Tomato", "Sugarcane")

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            capturedBitmap = bitmap
            isSampleImageUsed = false
            onClearDiagnosis()
        }
    }

    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    capturedBitmap = bitmap
                    isSampleImageUsed = false
                    onClearDiagnosis()
                }
            } catch (e: Exception) {
                // Ignore error
            }
        }
    }

    Scaffold(
        topBar = {
            KisanTopAppBar(
                title = "AI Crop Doctor & Leaf Scan",
                showBackButton = true,
                onBackClick = onBack
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 60.dp)
        ) {
            // Header Info Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SecondaryContainerGreen)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(PrimaryGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "AI Disease & Deficiency Detector",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGreen
                            )
                            Text(
                                text = "Scan leaf photo to identify fungal diseases, bacterial blights & nutrient deficiencies with AI.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // Crop Selection Row
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "1. Select Affected Crop",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(cropOptions) { crop ->
                            val isSelected = selectedCropName == crop
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (selectedCropName != crop) {
                                        selectedCropName = crop
                                        onClearDiagnosis()
                                    }
                                },
                                label = {
                                    Text(
                                        text = crop,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryGreen,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }
                    }
                }
            }

            // Camera Module & Photo Upload Card
            item {
                Card(
                    modifier = Modifier
                        .testTag("camera_module_card")
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "2. Scan Crop Leaf Photo",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Surface(
                                color = SecondaryContainerGreen,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.clickable {
                                    if (!isSampleImageUsed) {
                                        capturedBitmap = null
                                        isSampleImageUsed = true
                                        onClearDiagnosis()
                                    }
                                }
                            ) {
                                Text(
                                    text = if (isSampleImageUsed) "Sample Preview" else "Custom Scan (Tap to reset)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryGreen,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Image Preview Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.5.dp, PrimaryGreen, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (capturedBitmap != null) {
                                Image(
                                    bitmap = capturedBitmap!!.asImageBitmap(),
                                    contentDescription = "Captured Leaf",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.img_leaf_disease),
                                    contentDescription = "Leaf Sample",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            // Scanner Frame Overlay
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(12.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.75f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.FilterCenterFocus,
                                        contentDescription = null,
                                        tint = PrimaryGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "$selectedCropName Leaf • AI Vision Ready",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        // Camera & Gallery Control Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { cameraLauncher.launch(null) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, PrimaryGreen)
                            ) {
                                Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = null, tint = PrimaryGreen)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Open Camera", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                            }

                            OutlinedButton(
                                onClick = { galleryLauncher.launch("image/*") },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, PrimaryGreen)
                            ) {
                                Icon(imageVector = Icons.Default.Collections, contentDescription = null, tint = PrimaryGreen)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Upload Gallery", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                            }
                        }
                    }
                }
            }

            // Prominent "Analyze Symptoms with AI" Action Button
            item {
                Card(
                    modifier = Modifier
                        .testTag("analyze_symptoms_card_button")
                        .fillMaxWidth()
                        .clickable {
                            val bitmapToAnalyze = if (capturedBitmap != null) {
                                capturedBitmap
                            } else if (isSampleImageUsed) {
                                BitmapFactory.decodeResource(context.resources, R.drawable.img_leaf_disease)
                            } else {
                                null
                            }
                            onScanPhoto(bitmapToAnalyze, selectedCropName)
                        },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = PrimaryGreen),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isAnalyzing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = Color.White,
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Analyzing Leaf with AI API...",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Evaluating leaf spots, chlorosis & nutrient deficiency patterns...",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.85f),
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Analyze Symptoms with AI",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Tap to diagnose disease or nutrient deficiency with AI API",
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // AI Diagnosis Result: SHOWN AFTER ANALYZING
            item {
                if (pathologyResult != null) {
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + slideInVertically()
                    ) {
                        if (pathologyResult.provenance == ResultProvenance.ERROR) {
                            // Honest Error State - No Canned Data
                            Card(
                                modifier = Modifier
                                    .testTag("crop_doctor_error_card")
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, AlertRed)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    val isInvalidSubject = pathologyResult.diseaseName.startsWith("Invalid Subject")
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            color = AlertRed.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = if (isInvalidSubject) "INVALID IMAGE" else "ERROR",
                                                color = AlertRed,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                        Text(
                                            text = if (isInvalidSubject) "Non-Crop / Blank Image" else "HTTP Error / Parse Failure",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = AlertRed
                                        )
                                    }

                                    Text(
                                        text = pathologyResult.diseaseName,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AlertRed
                                    )

                                    Text(
                                        text = pathologyResult.symptomsSummary,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 18.sp
                                    )

                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = if (isInvalidSubject)
                                                "Vision Guard Notice: No agricultural crop foliage detected. AI did not generate a disease diagnosis for this non-crop image."
                                            else
                                                "Transparency Notice: ${pathologyResult.provenance.plainLanguageExplanation}",
                                            fontSize = 11.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            if (isInvalidSubject) {
                                                cameraLauncher.launch(null)
                                            } else {
                                                val bitmapToAnalyze = if (capturedBitmap != null) {
                                                    capturedBitmap
                                                } else if (isSampleImageUsed) {
                                                    BitmapFactory.decodeResource(context.resources, R.drawable.img_leaf_disease)
                                                } else {
                                                    null
                                                }
                                                onScanPhoto(bitmapToAnalyze, selectedCropName)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth().testTag("crop_doctor_retry_button")
                                    ) {
                                        Icon(if (isInvalidSubject) Icons.Default.CameraAlt else Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(if (isInvalidSubject) "Capture Real Leaf & Scan" else "Retry Diagnostic Scan", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            Card(
                                modifier = Modifier
                                    .testTag("crop_doctor_diagnosis_card")
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.5.dp,
                                    if (pathologyResult.provenance == ResultProvenance.LIVE) PrimaryGreen else Color(0xFFF57C00)
                                )
                            ) {
                                Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
                                    // Provenance Badge Header
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val isLive = pathologyResult.provenance == ResultProvenance.LIVE
                                        val badgeBg = if (isLive) PrimaryGreen.copy(alpha = 0.12f) else Color(0xFFFFF3E0)
                                        val badgeColor = if (isLive) PrimaryGreen else Color(0xFFE65100)

                                        Surface(
                                            color = badgeBg,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .clip(CircleShape)
                                                        .background(badgeColor)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (isLive) "LIVE AI VISION" else "OFFLINE ESTIMATE",
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = badgeColor
                                                )
                                            }
                                        }

                                        Text(
                                            text = if (isLive) "Gemini Vision Active" else "Offline Heuristics (Not Live)",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = pathologyResult.diseaseName,
                                                fontSize = 17.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = AlertRedText
                                            )
                                            Text(
                                                text = "${pathologyResult.category} • Severity: ${pathologyResult.severity}",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Surface(
                                            color = if (pathologyResult.provenance == ResultProvenance.LIVE) SecondaryContainerGreen else Color(0xFFFFF3E0),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text(
                                                text = pathologyResult.confidence,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (pathologyResult.provenance == ResultProvenance.LIVE) PrimaryGreen else Color(0xFFE65100),
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }

                                    // Plain language explanation callout
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = if (pathologyResult.provenance == ResultProvenance.LIVE)
                                                "Model Analyzed: Verified real-time diagnosis via Gemini Vision AI."
                                            else
                                                "Offline Estimate: Live AI is unavailable (airplane mode or missing key). Heuristic estimate based on crop profile. Not model-analyzed.",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(8.dp),
                                            lineHeight = 15.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                                    Spacer(modifier = Modifier.height(12.dp))

                                // Visual Symptoms Explanation by AI
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Visibility,
                                            contentDescription = null,
                                            tint = PrimaryGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "AI Visual Pathology Analysis",
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryGreen
                                        )
                                    }
                                    Text(
                                        text = pathologyResult.symptomsSummary,
                                        fontSize = 12.5.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 17.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = "AI Prescription & Actionable Treatment Plan",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Recommended Chemical Spray
                                RemedyCard(
                                    title = "🧪 Recommended Chemical Spray",
                                    description = pathologyResult.recommendedChemical
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Organic Remedy
                                RemedyCard(
                                    title = "🌿 Organic & Biological Remedy",
                                    description = pathologyResult.organicRemedy
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Soil & Nutrient Booster
                                RemedyCard(
                                    title = "🪴 Soil & Nutrient Booster",
                                    description = pathologyResult.soilAndNutrientAdvice
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = "Step-by-Step Field Instructions",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    pathologyResult.treatmentSteps.forEach { step ->
                                        TreatmentStepItem(text = step)
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (diagnosisResult != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("AI Scan Result", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                            Text(diagnosisResult, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, lineHeight = 18.sp)
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = PrimaryGreen,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = "Awaiting AI Leaf Scan",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Click 'Analyze Symptoms with AI' above to scan the crop leaf and receive real-time diagnosis & prescription.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RemedyCard(title: String, description: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, lineHeight = 16.sp)
        }
    }
}

@Composable
private fun TreatmentStepItem(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = PrimaryGreen,
            modifier = Modifier.size(16.dp).padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 12.5.sp,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 17.sp
        )
    }
}
