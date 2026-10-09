package com.example.model

data class DeadZonePreset(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val suggestedProfileType: ProfileType? = null
) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top

    companion object {
        val ALL_PRESETS = listOf(
            DeadZonePreset(
                id = "top_left_corner",
                name = "Top-Left Phantom Zone",
                description = "Status-bar corner drop down glitch & cracked edge",
                category = "Corner Cracks",
                left = 10,
                top = 10,
                right = 180,
                bottom = 130,
                suggestedProfileType = ProfileType.PORTRAIT
            ),
            DeadZonePreset(
                id = "top_right_crack",
                name = "Top-Right Spiderweb Crack",
                description = "Battery/signal area phantom taps from shattered glass",
                category = "Corner Cracks",
                left = 860,
                top = 10,
                right = 1070,
                bottom = 150,
                suggestedProfileType = ProfileType.PORTRAIT
            ),
            DeadZonePreset(
                id = "camera_notch_droop",
                name = "Camera Notch / Dynamic Island Ring",
                description = "Phantom pull-down gesture triggers around front sensor",
                category = "Sensors & Notches",
                left = 440,
                top = 0,
                right = 640,
                bottom = 110,
                suggestedProfileType = ProfileType.PORTRAIT
            ),
            DeadZonePreset(
                id = "left_edge_palm",
                name = "Left Curved Edge Palm Rejection",
                description = "Prevents false thumb fleshy palm taps when holding single-handed",
                category = "Edge Palm Rejection",
                left = 0,
                top = 450,
                right = 50,
                bottom = 980,
                suggestedProfileType = ProfileType.PORTRAIT
            ),
            DeadZonePreset(
                id = "right_edge_palm",
                name = "Right Edge Case Pinch Jitter",
                description = "Tight phone case pressing into curved glass digitizer",
                category = "Edge Palm Rejection",
                left = 1030,
                top = 450,
                right = 1080,
                bottom = 980,
                suggestedProfileType = ProfileType.PORTRAIT
            ),
            DeadZonePreset(
                id = "bottom_left_charger",
                name = "Bottom-Left USB Charging EMI Noise",
                description = "Ground loop static jitter near USB-C / lightning port",
                category = "Charging & Power Noise",
                left = 0,
                top = 1920,
                right = 320,
                bottom = 2220,
                suggestedProfileType = ProfileType.CHARGING_ONLY
            ),
            DeadZonePreset(
                id = "bottom_connector_jitter",
                name = "Bottom Connector Ground Jitter",
                description = "Phantom taps around charger cable entry during rapid charging",
                category = "Charging & Power Noise",
                left = 380,
                top = 2100,
                right = 700,
                bottom = 2300,
                suggestedProfileType = ProfileType.CHARGING_ONLY
            ),
            DeadZonePreset(
                id = "bottom_right_speaker",
                name = "Bottom-Right Port Surge Zone",
                description = "Static buildup from fast charger cable grounding",
                category = "Charging & Power Noise",
                left = 760,
                top = 1940,
                right = 1080,
                bottom = 2240,
                suggestedProfileType = ProfileType.CHARGING_ONLY
            ),
            DeadZonePreset(
                id = "bottom_nav_bar",
                name = "Navigation Bar Phantom Tap",
                description = "Accidental home/back button activations near bottom bezel",
                category = "Navigation & Gestures",
                left = 280,
                top = 2150,
                right = 800,
                bottom = 2340,
                suggestedProfileType = ProfileType.PORTRAIT
            ),
            DeadZonePreset(
                id = "water_damage_band",
                name = "Horizontal Digitizer Water Band",
                description = "Liquid condensation band glitch across middle row",
                category = "Hardware Damage",
                left = 0,
                top = 1120,
                right = 1080,
                bottom = 1240,
                suggestedProfileType = ProfileType.PORTRAIT
            ),
            DeadZonePreset(
                id = "landscape_thumb_rest",
                name = "Landscape Gaming Thumb Grip",
                description = "Shields accidental touch triggers in two-hand gaming grip",
                category = "Gaming & Landscape",
                left = 0,
                top = 620,
                right = 110,
                bottom = 960,
                suggestedProfileType = ProfileType.LANDSCAPE
            ),
            DeadZonePreset(
                id = "landscape_shoulder_sensor",
                name = "Landscape Shoulder Edge Glitch",
                description = "Corner accidental triggers while holding horizontally",
                category = "Gaming & Landscape",
                left = 1920,
                top = 0,
                right = 2280,
                bottom = 130,
                suggestedProfileType = ProfileType.LANDSCAPE
            )
        )
    }
}
