package com.manikandan.spotify.model.body

import com.manikandan.spotify.auth.SpotApiVault
import kotlinx.serialization.Serializable

@Serializable
data class SpotifyClientBody(
    val client_data: ClientData = ClientData(),
) {
    @Serializable
    data class ClientData(
        val client_version: String = SpotApiVault.defaultAppVersion(),
        val client_id: String = SpotApiVault.defaultClientId(),
        val js_sdk_data: JsSdkData = JsSdkData(),
    ) {
        @Serializable
        data class JsSdkData(
            val device_brand: String = "unknown",
            val device_model: String = "unknown",
            val os: String = "windows",
            val os_version: String = "10",
            val device_id: String = SpotApiVault.defaultDeviceId(),
            val device_type: String = "computer",
        )
    }
}
