package com.bringg.example.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.bringg.example.R
import com.bringg.example.databinding.FragmentVehicleDetailsBinding
import com.google.android.material.textfield.TextInputLayout
import driver_sdk.customer.CustomerVehicle

class VehicleDetailsFragment : DialogFragment() {
    private var _binding: FragmentVehicleDetailsBinding? = null
    private val binding get() = _binding!!

    lateinit var onResult: ((CustomerVehicle?) -> Unit)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Theme_AppCompat_Light_Dialog_Alert)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentVehicleDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initViews() = with(binding) {
        buttonArrive.setOnClickListener {
            onResult(createCustomerVehicle())
            dismiss()
        }

        buttonSkip.setOnClickListener {
            onResult(null)
            dismiss()
        }
    }

    private fun createCustomerVehicle() = with(binding) {
        val id = textInputVehicleId.textOrNull()?.toLongOrNull()
        val licensePlate = textInputVehicleLicensePlate.textOrNull()
        val color = textInputVehicleColor.textOrNull()
        val model = textInputVehicleModel.textOrNull()
        val parkingSpot = textInputVehicleParkingSpot.textOrNull()
        val save = checkBoxSaveVehicle.isChecked
        CustomerVehicle(
            id = id,
            licensePlate = licensePlate,
            color = color,
            model = model,
            parkingSpot = parkingSpot,
            saveVehicle = save
        )
    }

    companion object {
        const val TAG = "VehicleDetailsDialogFragment"
    }

    private fun TextInputLayout.textOrNull(): String? {
        val text = editText?.text?.toString()
        return if (text.isNullOrEmpty()) null else text
    }
}