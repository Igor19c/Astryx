package com.example.astryx.data.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.astryx.data.entities.Destination
import java.io.Serializable
import java.util.Stack

data class ExploreNavigationState(
    val parentId: String,
    val type: String?,
    val title: String?,
    val selectedChildIndex: Int
) : Serializable

class ExploreViewModel : ViewModel() {
    data class NavigationUiState(
        val progress: Int = 0,
        val planetActive: Boolean = false,
        val locationActive: Boolean = false
    )

    private val _navigationUiState = MutableLiveData<NavigationUiState>()
    val navigationUiState: LiveData<NavigationUiState> = _navigationUiState

    var exploreNavigationStack = Stack<ExploreNavigationState>()

    var currentExploreParentId: String = "root"
    var currentExploreParentType: String? = null
    var currentExploreParentTitle: String? = null
    var explorePendingSelectedIndex: Int? = null
    var exploreTargetIdToRestore: String? = null

    fun exploreTo(destination: Destination, currentSelectedIndex: Int) {
        exploreNavigationStack.push(
            ExploreNavigationState(
                currentExploreParentId,
                currentExploreParentType,
                currentExploreParentTitle,
                currentSelectedIndex
            )
        )

        currentExploreParentId = destination.id
        currentExploreParentType = destination.type
        currentExploreParentTitle = destination.title
        updateNavigationState(currentExploreParentId, currentExploreParentType)
    }

    fun exploreBack(): Boolean {
        return if (exploreNavigationStack.isNotEmpty()) {
            val previousState = exploreNavigationStack.pop()
            currentExploreParentId = previousState.parentId
            currentExploreParentType = previousState.type
            currentExploreParentTitle = previousState.title
            explorePendingSelectedIndex = previousState.selectedChildIndex
            updateNavigationState(currentExploreParentId, currentExploreParentType)
            true
        } else if (currentExploreParentId != "root") {
            exploreTargetIdToRestore = currentExploreParentId
            currentExploreParentId = "root"
            currentExploreParentType = null
            currentExploreParentTitle = null
            updateNavigationState(currentExploreParentId, null)
            true
        } else {
            false
        }
    }

    fun calculateInitialIndex(list: List<Destination>): Int {
        var index = explorePendingSelectedIndex ?: 0

        exploreTargetIdToRestore?.let { targetId ->
            val foundIndex = list.indexOfFirst { it.id == targetId }
            if (foundIndex != -1) {
                index = foundIndex
            }
            exploreTargetIdToRestore = null
        }

        return if (index in list.indices) index else 0
    }

    private fun updateNavigationState(parentId: String, parentType: String?) {
        val state = when {
            parentId == "root" -> NavigationUiState(15, planetActive = true)
            parentType == "PLANET" -> NavigationUiState(
                50,
                planetActive = true,
                locationActive = true
            )

            else -> NavigationUiState(100, planetActive = true, locationActive = true)
        }
        _navigationUiState.value = state
    }

}
