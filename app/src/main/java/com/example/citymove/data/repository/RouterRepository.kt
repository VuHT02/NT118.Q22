package com.example.citymove.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.citymove.data.model.RouteModel
import com.example.citymove.data.model.RouteStatus
import com.example.citymove.data.model.TransportType
import com.google.firebase.firestore.FirebaseFirestore

class RouteRepository {
    private val db = FirebaseFirestore.getInstance()

    fun getRoutes(type: TransportType): LiveData<List<RouteModel>> {
        val liveData = MutableLiveData<List<RouteModel>>()
        db.collection("routes")
            .whereEqualTo("type", type.name)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val routes = snapshot.documents.mapNotNull { doc ->
                    RouteModel(
                        id              = doc.id,
                        type            = TransportType.fromString(doc.getString("type") ?: ""),
                        lineCode        = doc.getString("lineCode") ?: "",
                        name            = doc.getString("name") ?: "",
                        startStation    = doc.getString("startStation") ?: "",
                        endStation      = doc.getString("endStation") ?: "",
                        distanceKm      = doc.getDouble("distanceKm") ?: 0.0,
                        stationCount    = doc.getLong("stationCount")?.toInt() ?: 0,
                        durationMinutes = doc.getLong("durationMinutes")?.toInt() ?: 0,
                        status          = RouteStatus.valueOf(doc.getString("status") ?: "ACTIVE"),
                        lineColor       = doc.getString("lineColor") ?: "#2196F3",
                        price           = doc.getLong("price")?.toInt(),
                        expectedOpenYear = doc.getLong("expectedOpenYear")?.toInt()
                    )
                }
                liveData.postValue(routes)
            }
        return liveData
    }
}