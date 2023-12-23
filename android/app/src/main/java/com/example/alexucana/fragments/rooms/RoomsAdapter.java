/**
 * RoomsAdapter: Adapter for managing room data in a RecyclerView.
 *
 * This adapter is responsible for managing the data of the rooms and handling user interactions
 * within a RecyclerView. It uses a custom ViewHolder (RoomViewHolder) to display each room item.
 * The adapter provides methods for adding a new room, removing a room, and binding room data to the views.
 * The RoomClickHandler interface is used to handle room item click events.
 *
 */
package com.example.alexucana.fragments.rooms;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.alexucana.R; // Import for accessing resources from the res folder
import com.example.alexucana.controllers.DataManager;
import com.example.alexucana.models.Room;

import java.util.ArrayList;

/**
 * The RoomsAdapter class represents an adapter for managing room data in a RecyclerView.
 */
public class RoomsAdapter extends RecyclerView.Adapter<RoomViewHolder> {

    // Interface to handle room item clicks
    public interface RoomClickHandler {
        void apply(String roomId);
    }

    // Reference to the RoomClickHandler for handling room item clicks
    public RoomClickHandler roomClickHandler;

    // Reference to the application context
    public Context context;

    // Reference to the data manager for interacting with room data
    private final DataManager dbController;

    // List to hold room data
    private ArrayList<Room> data;

    /**
     * Constructor for RoomsAdapter.
     *
     * @param ctx The application context.
     * @param fn  The RoomClickHandler to handle room item clicks.
     */
    public RoomsAdapter(Context ctx, RoomClickHandler fn) {
        this.context = ctx;
        this.roomClickHandler = fn;
        this.dbController = DataManager.getInstance();
        this.data = dbController.getRoomsList();
        if (!data.isEmpty()) {
            notifyDataSetChanged();
        }
        dbController.setOnDataChanged(this::onDataChanged);
    }

    /**
     * Listener method called when data changes in the DataManager.
     * Refreshes the adapter data and notifies any observers of the change.
     */
    private void onDataChanged() {
        this.data = dbController.getRoomsList();
        notifyDataSetChanged();
    }

    /**
     * Inflates the view for each room item.
     *
     * @param parent   The ViewGroup into which the new View will be added.
     * @param viewType The view type of the new View.
     * @return A new RoomViewHolder that holds the view for each room item.
     */
    @NonNull
    @Override
    public RoomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(this.context).inflate(R.layout.card_room, parent, false);
        return new RoomViewHolder(view);
    }

    /**
     * Binds room data to the view holder.
     *
     * @param holder   The RoomViewHolder to bind data to.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull RoomViewHolder holder, int position) {
        Room room = data.get(position);
        holder.setName(room.name);
        holder.setDevicesCount(room.devices_count);

        // Set an item click listener for each room item
        holder.setItemClickListener(v -> roomClickHandler.apply(data.get(position).id));

        // Set an item long click listener for each room item
        holder.setItemLongClickListener(v -> {
            removeRoom(position); // Handle room deletion
            return true;
        });
    }

    /**
     * Returns the total number of rooms in the list.
     *
     * @return The total number of rooms.
     */
    @Override
    public int getItemCount() {
        return data.size();
    }

    /**
     * Method to add a new room.
     * Shows an AlertDialog to get the room name from the user and adds the new room to the data manager.
     */
    public void addRoom() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Enter Room Name");

        final EditText input = new EditText(context);
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String roomName = input.getText().toString();
            this.dbController.addRoom(roomName);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    /**
     * Method to remove a room at the specified position.
     * Shows a confirmation dialog before removing the room.
     *
     * @param position The position of the room to be removed.
     */
    private void removeRoom(final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Confirm Deletion");
        builder.setMessage("Are you sure you want to delete this room?");

        builder.setPositiveButton("Yes", (dialog, which) -> {
            if (position >= 0 && position < data.size()) {
                dbController.removeRoom(data.get(position).id);
            }
        });

        builder.setNegativeButton("No", null);

        builder.create().show();
    }
}
