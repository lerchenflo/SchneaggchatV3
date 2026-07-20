package org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses

import kotlinx.serialization.Serializable

@Serializable
data class SubmitGameScoreRequest(
    val gameId: String,
    val difficulty: String,
    val score: Long,
    val timeMillis: Long,
)

@Serializable
data class GameScoreResponse(
    val id: String,
    val gameId: String,
    val difficulty: String,
    val userId: String,
    val score: Long,
    val timeMillis: Long,
    val achievedAt: Long,
)

@Serializable
data class HighscoreEntryResponse(
    val rank: Int,
    val userId: String,
    val username: String,
    val score: Long,
    val timeMillis: Long,
    val achievedAt: Long,
)

@Serializable
data class HighscoresResponse(
    val gameId: String,
    val difficulty: String,
    val period: String = "ALL_TIME",
    // Top 20; the requester is appended with their true rank when placed below that.
    val entries: List<HighscoreEntryResponse>,
)

@Serializable
data class GlobalRankingEntryResponse(
    val rank: Int,
    val userId: String,
    val username: String,
    // Sum of percentile points: up to 100 per (game, difficulty) board played.
    val points: Long,
    val boardsPlayed: Int,
    val gamesPlayed: Int,
)

@Serializable
data class GlobalRankingResponse(
    val period: String = "ALL_TIME",
    // Top 20; the requester is appended with their true rank when placed below that.
    val entries: List<GlobalRankingEntryResponse>,
)
