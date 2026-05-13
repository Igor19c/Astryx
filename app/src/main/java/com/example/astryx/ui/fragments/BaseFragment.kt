package com.example.astryx.ui.fragments

import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.astryx.data.entities.UIConfig
import com.example.astryx.data.viewmodels.UIViewModel
import com.example.astryx.data.utils.LoadingType
import com.example.astryx.data.utils.hideLoading
import com.example.astryx.data.utils.showCustomMessage
import com.example.astryx.data.utils.showLoading

abstract class BaseFragment : Fragment() {

    protected val uiViewModel: UIViewModel by activityViewModels()

    abstract fun getUIConfig(): UIConfig

    override fun onStart() {
        super.onStart()
        if (!isHidden) {
            uiViewModel.updateUI(getUIConfig())
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            uiViewModel.updateUI(getUIConfig())
        }
    }

    fun showLoading(message: String? = null, type: LoadingType = LoadingType.LOTTIE) {
        requireActivity().showLoading(message, type)
    }

    fun hideLoading() {
        requireActivity().hideLoading()
    }

    fun showCustomMessage(title: String, body: String, duration: Long = 3000) {
        requireActivity().showCustomMessage(title, body, duration)
    }

    fun showError(message: String) {
        showCustomMessage("Error", message)
    }

}
