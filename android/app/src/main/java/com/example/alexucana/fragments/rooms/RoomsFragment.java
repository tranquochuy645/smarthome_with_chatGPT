/**
 * RoomsFragment: Fragment for displaying a list of rooms.
 *
 * This fragment displays a list of rooms using a RecyclerView. It uses the RoomsAdapter
 * to manage the data and handle user interactions. The layout includes a button to add a new room.
 * The RoomsAdapter.RoomClickHandler is used to handle room item click events.
 *
 */
package com.example.alexucana.fragments.rooms;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.alexucana.R; // Import for accessing resources from the res folder

/**
 * The RoomsFragment class represents a Fragment for displaying a list of rooms.
 */
public class RoomsFragment extends Fragment {

    // Reference to the RoomsAdapter for managing room data and interactions
    private RoomsAdapter roomsAdapter;

    // Callback handler for handling room item clicks
    private final RoomsAdapter.RoomClickHandler openOneRoom;

    /**
     * Constructor for RoomsFragment.
     *
     * @param fn The RoomsAdapter.RoomClickHandler to handle room item clicks.
     */
    public RoomsFragment(RoomsAdapter.RoomClickHandler fn) {
        this.openOneRoom = fn;
    }

    /**
     * Called to create the content view for this fragment.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate views.
     * @param container          The parent view that the fragment's UI should be attached to.
     * @param savedInstanceState A Bundle object containing the fragment's previously saved state.
     * @return The inflated View for the fragment's UI.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the XML layout file associated with this fragment.
        // The layout will be displayed as the user interface of this fragment.
        // The "container" parameter is the parent view to which the layout will be attached.
        // The "false" argument means the layout should not be attached to the parent view immediately.
        View view = inflater.inflate(R.layout.rooms_list, container, false);

        // Get the reference to the room recycler view
        RecyclerView rcv = view.findViewById(R.id.rcv_rooms);

        // Set the layout manager for the RecyclerView
        rcv.setLayoutManager(new LinearLayoutManager(getContext()));

        // Set the RoomsAdapter for the RecyclerView
        roomsAdapter = new RoomsAdapter(getContext(), openOneRoom);
        rcv.setAdapter(roomsAdapter);

        // Set a click listener for the "Add Room" button to add a new room
        view.findViewById(R.id.btn_add_room).setOnClickListener(v -> roomsAdapter.addRoom());

        return view;
    }
}
