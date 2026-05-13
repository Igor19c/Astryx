package com.example.astryx.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import com.example.astryx.R
import com.example.astryx.AstryxApplication
import com.example.astryx.databinding.FragmentDialogTripEditBinding
import com.example.astryx.data.entities.Trip
import com.example.astryx.data.viewmodels.TripUiState
import com.example.astryx.data.viewmodels.TripViewModel
import com.example.astryx.data.viewmodels.UserViewModel
import com.example.astryx.data.viewmodels.ViewModelFactory
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class EditTripDialog : BottomSheetDialogFragment() {

    private var _binding: FragmentDialogTripEditBinding? = null
    private val binding get() = _binding!!

    private val tripViewModel: TripViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application as AstryxApplication)
    }
    private val userViewModel: UserViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application as AstryxApplication)
    }

    private var onSaveSuccessListener: (() -> Unit)? = null

    fun setOnSaveSuccessListener(listener: () -> Unit) {
        this.onSaveSuccessListener = listener
    }

    private var startDateLong: Long? = null
    private var endDateLong: Long? = null
    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDialogTripEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val isEdit = arguments?.getBoolean("is_edit") ?: false
        val currentTrip = tripViewModel.currentTrip.value

        setupUI(isEdit, currentTrip)
        setupDatePickers()
        observeViewModel()
        
        binding.saveNewTripBtn.setOnClickListener {
            handleSave(isEdit)
        }

        binding.cancelNewTripBtn.setOnClickListener {
            dismiss()
        }
    }

    private fun setupUI(isEdit: Boolean, currentTrip: Trip?) {
        binding.dialogTitle.text = if (isEdit) "Edit Trip Details" else "New Trip"
        binding.saveNewTripBtn.text = if (isEdit) "Save Changes" else "Start Journey"

        if (isEdit && currentTrip != null) {
            binding.editTextNameEditTripFragment.setText(currentTrip.title)
            startDateLong = currentTrip.startDate
            endDateLong = currentTrip.endDate
            startDateLong?.let { binding.editStartDate.setText(dateFormatter.format(Date(it))) }
        }
    }

    private fun setupDatePickers() {
        val ONE_DAY_MS = 24 * 60 * 60 * 1000L
        val baseMinDate = MaterialDatePicker.todayInUtcMilliseconds() + ONE_DAY_MS

        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.timeInMillis = baseMinDate
        calendar.add(Calendar.YEAR, 5)
        val baseMaxDate = calendar.timeInMillis

        val openRangePicker = View.OnClickListener {
            val constraints = CalendarConstraints.Builder()
                .setStart(baseMinDate)
                .setEnd(baseMaxDate)
                .setOpenAt(startDateLong ?: baseMinDate)
                .setValidator(DateValidatorPointForward.from(baseMinDate))
                .build()

            val dateRangePicker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("Select Trip Dates")
                .setCalendarConstraints(constraints)
                .setTheme(R.style.CustomDatePickerStyle)
                .build()

            dateRangePicker.addOnPositiveButtonClickListener { selection ->
                startDateLong = selection.first
                endDateLong = selection.second

                startDateLong?.let { binding.editStartDate.setText(dateFormatter.format(Date(it))) }
            }

            dateRangePicker.show(parentFragmentManager, "TRIP_RANGE_PICKER")
        }

        binding.editStartDate.setOnClickListener(openRangePicker)
    }

    private fun observeViewModel() {
        tripViewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is TripUiState.Loading -> {
                    binding.saveNewTripBtn.isEnabled = false
                    binding.saveNewTripBtn.text = "Saving..."
                }

                is TripUiState.Success -> {
                    tripViewModel.resetUiState()
                    onSaveSuccessListener?.invoke()
                    dismiss() // נסגר רק כשהצליח
                }

                is TripUiState.Error -> {
                    binding.saveNewTripBtn.isEnabled = true
                    binding.saveNewTripBtn.text =
                        if (arguments?.getBoolean("is_edit") == true) "Save Changes" else "Start Journey"
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    tripViewModel.resetUiState()
                }

                is TripUiState.Idle -> {
                    binding.saveNewTripBtn.isEnabled = true
                }
            }
        }
    }

    private fun handleSave(isEdit: Boolean) {
        val title = binding.editTextNameEditTripFragment.text.toString().trim()

        if (title.isEmpty()) {
            binding.editTextNameEditTripFragment.error = "Name is required"
            return
        }
        if (startDateLong == null || endDateLong == null) {
            Toast.makeText(requireContext(), "Please select dates", Toast.LENGTH_SHORT).show()
            return
        }

        if (isEdit) {
            tripViewModel.updateTripDetails(title, startDateLong, endDateLong)
            tripViewModel.syncTripWithFirebase()
        } else {
            val uid = userViewModel.getCurrentUid() ?: return
            tripViewModel.saveTrip(uid, title, "New Journey", startDateLong!!, endDateLong!!)
        }

        onSaveSuccessListener?.invoke()
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(isEdit: Boolean = false): EditTripDialog {
            return EditTripDialog().apply {
                arguments = Bundle().apply {
                    putBoolean("is_edit", isEdit)
                }
            }
        }
    }
}
