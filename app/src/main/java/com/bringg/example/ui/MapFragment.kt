package com.bringg.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import com.bringg.example.R
import com.bringg.example.databinding.FragmentMapBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.material.snackbar.Snackbar
import driver_sdk.ActiveCustomerSdkFactory


class MapFragment : Fragment() {

    private val mapLocations = ArrayList<LatLng>()
    private var userLocation: LatLng? = null
    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (ActivityCompat.checkSelfPermission(view.context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        initMapFragment()
    }

    @RequiresPermission(value = Manifest.permission.ACCESS_FINE_LOCATION)
    private fun initMapFragment() {
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync {
            it.uiSettings.isZoomControlsEnabled = true
            it.isMyLocationEnabled = true
            it.isBuildingsEnabled = true
            it.isTrafficEnabled = true
            observeWaypoints(it)
        }
    }

    private fun observeWaypoints(googleMap: GoogleMap) {
        val customerSdk = ActiveCustomerSdkFactory.customerSdk()
        customerSdk.currentLocation().observe(viewLifecycleOwner) {
            userLocation = LatLng(it.latitude, it.longitude)
            moveCamera(googleMap)
        }
        customerSdk.activeTask().observe(viewLifecycleOwner) {
            googleMap.clear()
            val circles =
                it.task?.wayPoints?.map { waypoint ->
                    CircleOptions()
                        .center(LatLng(waypoint.lat, waypoint.lng))
                        .fillColor(ColorUtils.setAlphaComponent(Color.BLUE, 150))
                        .strokeColor(Color.DKGRAY)
                        .radius(25.0)
                        .clickable(true)
                }
            circles?.forEachIndexed { index, circleOptions ->
                val circle = googleMap.addCircle(circleOptions)
                circle.zIndex = index.toFloat()
            }
            mapLocations.clear()
            if (circles != null) {
                mapLocations.addAll(circles.map { circleOptions -> circleOptions.center })
                moveCamera(googleMap)
            }
        }
    }

    private fun moveCamera(googleMap: GoogleMap) {
        val builder = LatLngBounds.Builder()
        mapLocations.forEach {
            builder.include(it)
        }
        userLocation?.let { builder.include(it) }
        googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100))
    }
}