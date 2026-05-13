package com.example.astryx.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.astryx.R
import com.example.astryx.AstryxApplication
import com.example.astryx.databinding.FragmentDialogSelectionListBinding
import com.example.astryx.ui.adapters.ColorAdapter
import com.example.astryx.data.utils.SettingsManager
import com.example.astryx.data.utils.showLoading
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SelectAccentColorDialog : BottomSheetDialogFragment() {

    private var _binding: FragmentDialogSelectionListBinding? = null
    private val binding get() = _binding!!
    private lateinit var settingsManager: SettingsManager

    private var onColorSelected: ((Int) -> Unit)? = null

    fun setOnColorSelectedListener(listener: (Int) -> Unit) {
        this.onColorSelected = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDialogSelectionListBinding.inflate(inflater, container, false)
        settingsManager =
            (requireActivity().application as AstryxApplication).appContainer.settingsManager
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.dialogSelectTitle.text = "Select Accent Color"

        val themeOptions = listOf(
            Triple(
                getString(R.string.ac_indigo),
                R.color.accent_space_purple,
                R.style.Theme_Astryx
            ),
            Triple(
                getString(R.string.ac_blue),
                R.color.accent_space_blue,
                R.style.Theme_Astryx_Blue
            ),
            Triple(
                getString(R.string.ac_crimson),
                R.color.accent_space_red,
                R.style.Theme_Astryx_Red
            ),
            Triple(
                getString(R.string.ac_green),
                R.color.accent_space_green,
                R.style.Theme_Astryx_Green
            )
        )

        binding.itemRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = ColorAdapter(themeOptions.map { Pair(it.first, it.second) }) { index ->
                val selectedTheme = themeOptions[index].third
                onColorSelected?.invoke(selectedTheme)
                requireActivity().showLoading("Updating accent color...")
                dismiss()

            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "SelectDialogAccentColor"
        fun newInstance() = SelectAccentColorDialog()
    }
}
