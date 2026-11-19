package com.devekene.healthtracker.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface HealthDestination {
    @Serializable
    data object Checkup : HealthDestination

    @Serializable
    data object Progress : HealthDestination

    @Serializable
    data object History : HealthDestination

    @Serializable
    data object Backup : HealthDestination
}
