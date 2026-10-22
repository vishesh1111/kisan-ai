package com.example.data.model

/**
 * Result provenance tracking the origin and veracity of analytical and meteorological intelligence.
 * States:
 * - LIVE: Generated live in real-time from AI vision model / satellite telemetry.
 * - OFFLINE_ESTIMATE: Estimated using offline heuristic algorithms when offline or without API key.
 * - ERROR: Request failed due to server error (HTTP non-200) or unparseable response. Never canned.
 */
enum class ResultProvenance {
    LIVE,
    OFFLINE_ESTIMATE,
    ERROR;

    val badgeLabel: String
        get() = when (this) {
            LIVE -> "LIVE"
            OFFLINE_ESTIMATE -> "OFFLINE ESTIMATE"
            ERROR -> "ERROR"
        }

    val plainLanguageExplanation: String
        get() = when (this) {
            LIVE -> "Real-time AI model / satellite telemetry analysis."
            OFFLINE_ESTIMATE -> "Estimated using offline agronomic heuristics. Live service is unavailable."
            ERROR -> "Service request failed due to a server error. No simulated data is shown."
        }
}
