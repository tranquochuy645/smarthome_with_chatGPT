/**
 * RoomViewHolder: RecyclerView ViewHolder for displaying room information in a list.
 *
 * This class extends RecyclerView.ViewHolder and represents a single item view in the RecyclerView.
 * It holds references to the TextViews used to display the room name and the count of devices.
 * Additionally, it provides methods to set the room name, devices count, and click listeners for the item.
 *
 */
package com.example.alexucana.fragments.rooms;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.alexucana.R; // Import for accessing resources from the res folder

/**
 * The RoomViewHolder class represents a single item view in the RecyclerView for displaying room information.
 */
public class RoomViewHolder extends RecyclerView.ViewHolder {

    // TextViews to display room name and the count of devices
    private final TextView tv_room_name, tv_devices_count;

    // Reference to the item view
    private final View itemView;

    /**
     * Constructor to initialize the views in the ViewHolder.
     *
     * @param itemView The root view of the item layout.
     */
    public RoomViewHolder(@NonNull View itemView) {
        super(itemView);
        this.itemView = itemView;

        // Find and assign the TextViews in the layout to local variables
        tv_room_name = itemView.findViewById(R.id.tv_room_name);
        tv_devices_count = itemView.findViewById(R.id.tv_devices_count);
    }

    /**
     * Set the room name in the corresponding TextView.
     *
     * @param name The name of the room to be displayed.
     */
    public void setName(String name) {
        tv_room_name.setText(name);
    }

    /**
     * Set the devices count in the corresponding TextView.
     *
     * @param count The count of devices to be displayed.
     */
    public void setDevicesCount(String count) {
        tv_devices_count.setText(count + " devices");
    }

    /**
     * Set a long click listener for the item.
     *
     * @param listener The long click listener to be set.
     */
    public void setItemLongClickListener(View.OnLongClickListener listener) {
        itemView.setOnLongClickListener(listener);
    }

    /**
     * Set a click listener for the item.
     *
     * @param listener The click listener to be set.
     */
    public void setItemClickListener(View.OnClickListener listener) {
        itemView.setOnClickListener(listener);
    }
}
