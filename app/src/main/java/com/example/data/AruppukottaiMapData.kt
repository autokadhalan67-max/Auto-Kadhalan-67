package com.example.data

import kotlin.math.sqrt

data class MapLocation(
    val id: String,
    val name: String,
    val description: String,
    val x: Float, // Normalized X (0.0 to 1.0)
    val y: Float  // Normalized Y (0.0 to 1.0)
)

data class TalukRoad(
    val name: String,
    val points: List<MapLocation>
)

object AruppukottaiMapData {
    val locations = listOf(
        MapLocation("bus_stand", "Aruppukottai Bus Stand", "Central Junction & Rickshaw Hub", 0.50f, 0.45f),
        MapLocation("railway_station", "Railway Station", "Western Terminal, Railway Feeder Road", 0.20f, 0.52f),
        MapLocation("palayampatti", "Palayampatti Junction", "North-West Heritage & Residential Entry", 0.28f, 0.20f),
        MapLocation("sb_kounder", "SBK College Road", "Northern Education Belt & SB Kounder St", 0.48f, 0.25f),
        MapLocation("kovilankulam", "Kovilankulam Temple", "Eastern Divine Zone & Rural Link", 0.82f, 0.48f),
        MapLocation("pandalgudi", "Pandalgudi Tollgate", "Southern Rural Hub, Tuticorin Highway", 0.65f, 0.82f),
        MapLocation("athipatti", "Athipatti Industrial", "South-Western Spinning Mills & Estates", 0.32f, 0.72f),
        MapLocation("gopalapuram", "Gopalapuram Colony", "Residential Colony and Park Area", 0.68f, 0.28f),
        MapLocation("bypass_junction", "Madurai-Tuticorin NH Junction", "Expressway Bypass Crossing", 0.55f, 0.60f)
    )

    val keyRoads = listOf(
        TalukRoad(
            "NH-38 National Highway Bypass",
            listOf(
                locations.first { it.id == "palayampatti" },
                locations.first { it.id == "sb_kounder" },
                locations.first { it.id == "gopalapuram" },
                locations.first { it.id == "kovilankulam" }
            )
        ),
        TalukRoad(
            "Bypass-Pandalgudi Link",
            listOf(
                locations.first { it.id == "kovilankulam" },
                locations.first { it.id == "bypass_junction" },
                locations.first { it.id == "pandalgudi" }
            )
        ),
        TalukRoad(
            "Virudhunagar-Aruppukottai Main Road",
            listOf(
                locations.first { it.id == "railway_station" },
                locations.first { it.id == "bus_stand" },
                locations.first { it.id == "kovilankulam" }
            )
        ),
        TalukRoad(
            "Athipatti Feeder Road",
            listOf(
                locations.first { it.id == "railway_station" },
                locations.first { it.id == "athipatti" },
                locations.first { it.id == "bypass_junction" },
                locations.first { it.id == "bus_stand" }
            )
        ),
        TalukRoad(
            "SBK Town Road",
            listOf(
                locations.first { it.id == "sb_kounder" },
                locations.first { it.id == "bus_stand" }
            )
        )
    )

    fun calculateDistanceKm(from: MapLocation, to: MapLocation): Double {
        val dx = (from.x - to.x) * 8.5 // Simulated Aruppukottai scale in Km
        val dy = (from.y - to.y) * 8.5
        return sqrt((dx * dx + dy * dy).toDouble())
    }

    fun calculateFare(from: MapLocation, to: MapLocation): Double {
        val distance = calculateDistanceKm(from, to)
        return kotlin.math.max(40.0, 40.0 + (distance * 15.0)) // Base Rs 40 + Rs 15 per km (Auto Tariff)
    }
}
