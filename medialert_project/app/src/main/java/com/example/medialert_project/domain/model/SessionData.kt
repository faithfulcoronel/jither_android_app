package com.example.medialert_project.domain.model

data class SessionData(
    val accessToken: String,
    val refreshToken: String,
    val userId: String
)
