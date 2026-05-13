package com.example.astryx.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import com.example.astryx.databinding.FragmentDialogSelectionListBinding
import com.example.astryx.ui.adapters.ImageSelectionAdapter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SelectImageSourceDialog : BottomSheetDialogFragment() {

    private var _binding: FragmentDialogSelectionListBinding? = null
    private val binding get() = _binding!!

    private var images: List<String> = emptyList()
    private var title: String = ""
    private var onImageSelected: ((String) -> Unit)? = null
    private var onPlusClick: (() -> Unit)? = null

    fun setArguments(title: String, images: List<String>) {
        this.title = title
        this.images = images
    }

    fun setOnImageSelectedListener(listener: (String) -> Unit) {
        this.onImageSelected = listener
    }

    fun setOnPlusClickListener(listener: () -> Unit) {
        this.onPlusClick = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDialogSelectionListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.dialogSelectTitle.text = title

        binding.itemRecyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = ImageSelectionAdapter(
                images = images,
                onPlusClick = {
                    onPlusClick?.invoke()
                    dismiss()
                },
                onImageSelected = { imageUrl ->
                    onImageSelected?.invoke(imageUrl)
                    dismiss()
                }
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "SelectDialogImageSource"

        fun newInstance(title: String, images: List<String>): SelectImageSourceDialog {
            return SelectImageSourceDialog().apply {
                setArguments(title, images)
            }
        }
    }

}