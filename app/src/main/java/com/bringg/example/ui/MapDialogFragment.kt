package com.bringg.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.LiveData
import com.bringg.example.ExampleNotificationProvider
import com.bringg.example.R
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import driver_sdk.ActiveCustomerSdkFactory
import driver_sdk.customer.SdkSettings
import driver_sdk.logging.BringgLog
import driver_sdk.models.TaskState
import driver_sdk.models.tasks.Waypoint


class MapDialogFragment : DialogFragment() {
    private lateinit var activeTaskLiveData: LiveData<TaskState>
    private val markers = mutableListOf<MarkerOptions>()

    private val callback = OnMapReadyCallback { googleMap ->
        googleMap.uiSettings.isZoomControlsEnabled = true;
        displayMarkersOnMap(googleMap)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view: View = inflater.inflate(R.layout.fragment_map_dialog, container, false)
        val sdkInstance = ActiveCustomerSdkFactory.init(requireContext(), ExampleNotificationProvider(requireContext()), SdkSettings.Builder().build())
        activeTaskLiveData = sdkInstance.activeTask()
        getCurrentLocation()
        return view
    }

     private fun getCurrentLocation() {
         val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        // async request a single location update
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            BringgLog.error(TAG, "can't get last location, ACCESS_COARSE_LOCATION permission not granted, returning null location")
            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
                    mapFragment?.getMapAsync(callback)
                    markers.add(MarkerOptions().position(LatLng(location.latitude, location.longitude)).title("Current location"))
                }
            }
    }

    private fun addMarkersForWps() {
        val waypoints = activeTaskLiveData.value?.task?.wayPoints
        if (waypoints != null) {
            for (wp in waypoints) {
                val markerOption = MarkerOptions().position(LatLng(waypoint.lat, waypoint.lng)).title("Wp id: " + waypoint.id)
                markers.add(markerOption)
            }
        }
    }

    private fun displayMarkersOnMap(googleMap: GoogleMap) {
        addMarkersForWps()
        val builder = LatLngBounds.Builder()
        for (marker in markers) {
            googleMap.addMarker(marker).showInfoWindow()
            builder.include(marker.position)
        }
        googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 200))
    }

    companion object {
        private const val TAG = "MapDialogFragment"
    }
}