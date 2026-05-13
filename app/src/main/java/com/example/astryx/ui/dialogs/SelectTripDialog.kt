package com.example.astryx.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.example.astryx.R
import com.example.astryx.AstryxApplication
import com.example.astryx.databinding.FragmentDialogTripSelectBinding
import com.example.astryx.data.entities.Destination
import com.example.astryx.data.entities.Trip
import com.example.astryx.data.utils.getColorFromAttr
import com.example.astryx.ui.adapters.TripPickerAdapter
import com.example.astryx.data.viewmodels.TripViewModel
import com.example.astryx.data.viewmodels.ViewModelFactory
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SelectTripDialog : BottomSheetDialogFragment() {

    private var onTripSelectedListener: ((Trip) -> Unit)? = null

    fun setOnTripSelectedListener(listener: (Trip) -> Unit) {
        this.onTripSelectedListener = listener
    }

    private var _binding: FragmentDialogTripSelectBinding? = null
    private val binding get() = _binding!!

    private val tripViewModel: TripViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application as AstryxApplication)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDialogTripSelectBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val destinationToAdd =
            arguments?.getSerializable("destination_to_add") as? Destination

        var trips: List<Trip>
        if (destinationToAdd != null) {
            trips = (tripViewModel.userTrips.value ?: emptyList()).filter { it.isDraft }
        } else
            trips = tripViewModel.userTrips.value ?: emptyList()

        var selectedTrip: Trip? = null

        val pickerAdapter = TripPickerAdapter(trips) { trip ->
            selectedTrip = trip
            binding.continueBtn.isEnabled = true
            binding.continueBtn.setBackgroundColor(requireContext().getColorFromAttr(R.attr.customColorPrimary))
        }

        binding.tripRecyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.tripRecyclerView.adapter = pickerAdapter

        binding.cancelBtnEditProfileFragment.setOnClickListener { dismiss() }

        binding.continueBtn.setOnClickListener {
            selectedTrip?.let { trip ->
                val destination = arguments?.getSerializable("destination_to_add") as? Destination
                if (destination != null)
                    tripViewModel.addDestinationToTrip(trip, destination)
                else
                    onTripSelectedListener?.invoke(trip)
                dismiss()
            }
        }
    }

    override fun onStart() {
        super.onStart()

        (dialog as? BottomSheetDialog)?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "SelectTripDialogFragment"
        fun newInstance(destination: Destination? = null) = SelectTripDialog().apply {
            arguments = Bundle().apply {
                putSerializable("destination_to_add", destination)
            }
        }
    }
}