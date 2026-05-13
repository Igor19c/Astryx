package com.example.astryx.data.utils

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import androidx.core.graphics.drawable.toDrawable
import com.example.astryx.R
import com.example.astryx.databinding.ContainerAddToTripBtnsDestFragmentBinding
import com.example.astryx.databinding.ContainerBtnMyTripsFragmentBinding
import com.example.astryx.data.entities.Destination
import com.example.astryx.data.entities.Trip
import com.example.astryx.ui.dialogs.EditTripDialog
import com.example.astryx.ui.dialogs.SelectTripDialog
import com.example.astryx.data.viewmodels.TripViewModel
import com.example.astryx.data.viewmodels.UIViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class TripSelectionHelper(
    private val context: Context,
    private val tripViewModel: TripViewModel,
    private val uiViewModel: UIViewModel,
    private val onDestinationPending: ((Destination) -> Unit)? = null
) {

    fun handleAddToTrip(destination: Destination) {
        showTripSelectionActionDialog(destination)
    }

    fun handleFinalizeTrip(trip: Trip, onSuccess: (() -> Unit)? = null) {
        if (trip.canBePublished) {
            tripViewModel.setCurrentTrip(trip)
            tripViewModel.finalizeTrip()
            context.showCustomMessage(
                "Success",
                "Journey '${trip.title}' Finalized!"
            )
            onSuccess?.invoke()
        } else {
            when {
                trip.destinationIds.isEmpty() -> {
                    context.showCustomMessage(
                        "Error",
                        "Your trip is empty!"
                    )
                }
                trip.isPastTrip -> { showInvalidDateDialog(trip) }
            }
        }
    }

    fun showInvalidDateDialog(trip: Trip) {
        val dialogBinding = ContainerBtnMyTripsFragmentBinding.inflate(LayoutInflater.from(context))
        val dialog = AlertDialog.Builder(context)
            .setView(dialogBinding.root)
            .create()

        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

        dialogBinding.fixDatesBtnContainerTripFragment.setOnClickListener {
            tripViewModel.setCurrentTrip(trip)
            tripViewModel.setEditMode(true)

            uiViewModel.requestNavigation(EditTripDialog.newInstance(isEdit = true))
            dialog.dismiss()
        }

        dialogBinding.deleteTripBtnContainerTripFragment.setOnClickListener {
            handleDeleteTrip(trip) {
                uiViewModel.requestBack()
            }
            dialog.dismiss()
        }

        dialogBinding.cancelBtnContainerTripFragment.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    fun handleDeleteTrip(
        trip: Trip,
        onDeleted: (() -> Unit)? = null,
        onCancel: (() -> Unit)? = null
    ) {
        MaterialAlertDialogBuilder(context, R.style.CustomAlertDialog)
            .setTitle("Delete Trip")
            .setMessage("Are you sure you want to delete '${trip.title}'?")
            .setNegativeButton("Cancel") { _, _ ->
                onCancel?.invoke()
            }
            .setOnCancelListener {
                onCancel?.invoke()
            }
            .setPositiveButton("Delete") { _, _ ->
                tripViewModel.deleteTrip(trip.id)
                onDeleted?.invoke()
            }
            .show()
    }

    fun handleDeleteTripDestination(
        dest: Destination,
        onDeleted: (() -> Unit)? = null,
        onCancel: (() -> Unit)? = null
    ) {
        MaterialAlertDialogBuilder(context, R.style.CustomAlertDialog)
            .setTitle("Delete Destination")
            .setMessage("Are you sure you want to delete '${dest.title}'?")
            .setNegativeButton("Cancel") { _, _ ->
                onCancel?.invoke()
            }
            .setOnCancelListener {
                onCancel?.invoke()
            }
            .setPositiveButton("Delete") { _, _ ->
                tripViewModel.removeDestination(dest.id)
                onDeleted?.invoke()
            }
            .show()
    }

    private fun showTripSelectionActionDialog(destination: Destination) {
        val draftTrips = (tripViewModel.userTrips.value ?: emptyList()).filter { it.isDraft }
        val activeTrip =
            tripViewModel.currentTrip.value?.takeIf { it.id.isNotEmpty() && it.isDraft }
                ?: draftTrips.firstOrNull()

        val dialogBinding =
            ContainerAddToTripBtnsDestFragmentBinding.inflate(LayoutInflater.from(context))
        val dialog = AlertDialog.Builder(context)
            .setView(dialogBinding.root)
            .create()

        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

        // 1. Add to Active Trip
        if (activeTrip != null) {
            dialogBinding.addToCurrentTripBtnContainer.apply {
                visibility = View.VISIBLE
                text = "Add to Current: ${activeTrip.title}"
                setOnClickListener {
                    tripViewModel.addDestinationToTrip(activeTrip, destination)
                    dialog.dismiss()
                }
            }
        } else {
            dialogBinding.addToCurrentTripBtnContainer.visibility = View.GONE
        }

        // 2. Select from Existing
        if (draftTrips.isNotEmpty()) {
            dialogBinding.selectExistTripBtnContainer.apply {
                visibility = View.VISIBLE
                text = "Select from Other Trips"
                setOnClickListener {
                    uiViewModel.requestNavigation(SelectTripDialog.newInstance(destination))
                    dialog.dismiss()
                }
            }
        } else {
            dialogBinding.selectExistTripBtnContainer.visibility = View.GONE
        }

        // 3. Create New
        dialogBinding.createNewTripBtnContainer.setOnClickListener {
            tripViewModel.createNewTrip()
            onDestinationPending?.invoke(destination)

            uiViewModel.requestNavigation(EditTripDialog.newInstance())
            dialog.dismiss()
        }

        dialog.show()
    }

}
