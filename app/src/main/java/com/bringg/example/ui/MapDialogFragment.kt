package com.bringg.example.ui

import android.app.Activity
import android.content.Context
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.bringg.example.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import driver_sdk.models.Task


class MapDialogFragment : DialogFragment() {
    private lateinit var listener : MapInteractionCallback
    private var task: Task? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view: View = inflater.inflate(R.layout.fragment_map_dialog, container, false)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val taskId = arguments?.getLong("task_id")
        task = listener.getActiveTask(taskId!!)
    }

    override fun onAttach(p0: Activity) {
        super.onAttach(p0)
        contextImplementsInterface(p0)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        contextImplementsInterface(context)
    }

    private fun contextImplementsInterface(context: Context) {
        if (context is MapInteractionCallback) {
            listener = context
        } else throw Exception("maps fragment must implement MapInteractionCallback")
    }

    private fun addMarkersForWps(task: Task, googleMap: GoogleMap) {
        val markers = mutableListOf<MarkerOptions>()
        // create map markers for waypoints location
        for (waypoint in task.wayPoints) {
            val markerOption = MarkerOptions().position(LatLng(waypoint.lat, waypoint.lng)).title("Wp id: " + waypoint.id)
            markers.add(markerOption)
        }
        // create map markers for current location
        val currLocation = listener.getCurrentLocation()
        if(currLocation != null) {
            val latLng = LatLng(currLocation.latitude, currLocation.longitude)
            markers.add(MarkerOptions().position(latLng).title("Current location"))
        }
        // display markers on map
        val builder = LatLngBounds.Builder()
        for (marker in markers) {
            googleMap.addMarker(marker).showInfoWindow()
            builder.include(marker.position)
        }
        googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100))
    }

    private val callback = OnMapReadyCallback { googleMap ->
        /**
         * Manipulates the map once available.
         * This callback is triggered when the map is ready to be used.
         * This is where we can add markers or lines, add listeners or move the camera.
         * In this case, we just add a marker near Sydney, Australia.
         * If Google Play services is not installed on the device, the user will be prompted to
         * install it inside the SupportMapFragment. This method will only be triggered once the
         * user has installed Google Play services and returned to the app.
         */

        googleMap.uiSettings.isZoomControlsEnabled = true;

        if (task != null) {
            addMarkersForWps(task!!, googleMap)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(taskId: Long) =
            MapDialogFragment().apply {
                arguments = Bundle().apply {
                    putLong("task_id", taskId)
                }
            }
    }

    interface MapInteractionCallback {
        fun getActiveTask(taskId: Long): Task?
        fun getCurrentLocation(): Location?
    }
}