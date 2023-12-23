/**
 * DevicesFragment: Fragment for displaying a list of devices in a RecyclerView.
 *
 * This fragment is responsible for displaying a list of devices within a RecyclerView.
 * It utilizes the DevicesAdapter to manage the data and views of devices.
 * The DevicesFragment provides methods to set the current room and handles the creation
 * and setup of the RecyclerView and its associated adapter.
 *
 */
package com.example.alexucana.fragments.devices;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.alexucana.R; // Import for accessing resources from the res folder

/**
 * The DevicesFragment class represents a Fragment for displaying a list of devices in a RecyclerView.
 */
public class DevicesFragment extends Fragment {

    // Reference to the DevicesAdapter for managing device data
    private DevicesAdapter devicesAdapter;

    // Current room identifier
    private String currentRoom;

    /**
     * Called to create the view hierarchy associated with the fragment.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container          If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return The root view for the fragment's layout.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.devices_list, container, false);
        // Inflates the XML layout file associated with this fragment.
        // The layout will be displayed as the user interface of this fragment.
        // The "container" parameter is the parent view to which the layout will be attached.
        // The "false" argument means the layout should not be attached to the parent view immediately.

        RecyclerView rcv = view.findViewById(R.id.rcv_devices);
        // Get the reference to the room recycler view

        // Set the layout manager for the RecyclerView
        rcv.setLayoutManager(new LinearLayoutManager(getContext()));

        // Set the DevicesAdapter for the RecyclerView
        devicesAdapter = new DevicesAdapter(getActivity());
        rcv.setAdapter(devicesAdapter);
        view.findViewById(R.id.btn_add_device).setOnClickListener(v -> devicesAdapter.addDevice());
        return view;
    }

    /**
     * Called immediately after onCreateView() to perform any initialization steps.
     *
     * @param view               The created view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        this.devicesAdapter.setCurrentRoom(currentRoom);
    }

    /**
     * Set the current room for the DevicesFragment.
     *
     * @param roomId The identifier of the current room.
     */
    public void setRoom(String roomId) {
        this.currentRoom = roomId;
    }
}
