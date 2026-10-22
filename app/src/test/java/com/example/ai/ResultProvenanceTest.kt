package com.example.ai

import com.example.data.model.Farm
import com.example.data.model.ResultProvenance
import com.example.data.network.WeatherService
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * JVM Unit Tests for Result Provenance Verification (Round 2 Acceptance Criteria).
 *
 * Test cases:
 * 1. Blank key gives OFFLINE_ESTIMATE and isGeminiVisionAnalyzed == false
 * 2. Mocked 200 gives LIVE and isGeminiVisionAnalyzed == true
 * 3. Mocked 500 gives ERROR (never canned data)
 * 4. Malformed JSON gives ERROR
 * 5. Weather non-200 response gives ERROR
 */
class ResultProvenanceTest {

    private lateinit var mockServer: MockWebServer

    @Before
    fun setUp() {
        mockServer = MockWebServer()
        mockServer.start()
        GeminiFarmAgent.baseUrl = mockServer.url("/v1beta/models/").toString()
        WeatherService.baseUrl = mockServer.url("/v1/forecast").toString()
    }

    @After
    fun tearDown() {
        mockServer.shutdown()
        GeminiFarmAgent.apiKeyProvider = { com.example.BuildConfig.GEMINI_API_KEY }
        GeminiFarmAgent.baseUrl = "https://generativelanguage.googleapis.com"
        WeatherService.baseUrl = "https://api.open-meteo.com/v1/forecast"
    }

    // TEST 1: Blank key gives OFFLINE_ESTIMATE and isGeminiVisionAnalyzed == false
    @Test
    fun blankKey_givesOfflineEstimate_andVisionAnalyzedFalse() = runTest {
        GeminiFarmAgent.apiKeyProvider = { "" }

        val pathology = GeminiFarmAgent.analyzeCropLeafWithGemini(null, "Wheat")
        assertEquals(ResultProvenance.OFFLINE_ESTIMATE, pathology.provenance)
        assertFalse("isGeminiVisionAnalyzed must be false when fallback is used", pathology.isGeminiVisionAnalyzed)

        val advisor = GeminiFarmAgent.askFarmAdvisor("How to spray nitrogen?", null, null, emptyList(), emptyList(), emptyList())
        assertEquals(ResultProvenance.OFFLINE_ESTIMATE, advisor.provenance)

        val growth = GeminiFarmAgent.analyzeCropGrowthVision(null, "Wheat")
        assertEquals(ResultProvenance.OFFLINE_ESTIMATE, growth.provenance)
    }

    // TEST 2: Mocked 200 gives LIVE and isGeminiVisionAnalyzed == true
    @Test
    fun mocked200_givesLive_andVisionAnalyzedTrue() = runTest {
        GeminiFarmAgent.apiKeyProvider = { "valid_test_api_key" }

        val validGeminiJson = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "{\n  \"diseaseName\": \"Tan Spot (Pyrenophora)\",\n  \"category\": \"Fungal Pathogen\",\n  \"confidence\": \"96% Match\",\n  \"severity\": \"Moderate\",\n  \"symptomsSummary\": \"Tan oval lesions with yellow halos on mid-leaf blades.\",\n  \"treatmentSteps\": [\"Apply Propiconazole 25% EC @ 1ml/L\"],\n  \"recommendedChemical\": \"Propiconazole 25% EC\",\n  \"organicRemedy\": \"Neem Oil 10,000 ppm\",\n  \"soilAndNutrientAdvice\": \"Apply 25kg MOP/acre\"\n}"
                      }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        mockServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(validGeminiJson)
        )

        val result = GeminiFarmAgent.analyzeCropLeafWithGemini(null, "Wheat")
        assertEquals(ResultProvenance.LIVE, result.provenance)
        assertTrue("isGeminiVisionAnalyzed must be true for live Gemini vision analysis", result.isGeminiVisionAnalyzed)
        assertEquals("Tan Spot (Pyrenophora)", result.diseaseName)
    }

    // TEST 3: Mocked 500 gives ERROR (never canned data)
    @Test
    fun mocked500_givesError_withoutCannedData() = runTest {
        GeminiFarmAgent.apiKeyProvider = { "valid_test_api_key" }

        mockServer.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error")
        )

        val result = GeminiFarmAgent.analyzeCropLeafWithGemini(null, "Wheat")
        assertEquals(ResultProvenance.ERROR, result.provenance)
        assertFalse(result.isGeminiVisionAnalyzed)
        assertNotNull(result.errorMessage)
        assertTrue(result.errorMessage!!.contains("500"))
    }

    // TEST 4: Malformed JSON gives ERROR
    @Test
    fun malformedJson_givesError() = runTest {
        GeminiFarmAgent.apiKeyProvider = { "valid_test_api_key" }

        mockServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("INVALID_NON_JSON_CORRUPTED_RESPONSE{{{")
        )

        val result = GeminiFarmAgent.analyzeCropLeafWithGemini(null, "Wheat")
        assertEquals(ResultProvenance.ERROR, result.provenance)
        assertFalse(result.isGeminiVisionAnalyzed)
    }

    // TEST 5: Weather non-200 response gives ERROR
    @Test
    fun weatherNon200_givesError() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(503)
                .setBody("Service Unavailable")
        )

        val testFarm = Farm(
            id = "test_f1",
            name = "Test Ludhiana Farm",
            location = "Punjab",
            latitude = 30.8987,
            longitude = 75.8573
        )

        val weather = WeatherService.fetchLiveWeatherForFarm(testFarm)
        assertEquals(ResultProvenance.ERROR, weather.provenance)
        assertNotNull(weather.errorMessage)
        assertTrue(weather.errorMessage!!.contains("503"))
    }
}
