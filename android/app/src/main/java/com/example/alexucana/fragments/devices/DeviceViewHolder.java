/**
 * DeviceViewHolder: ViewHolder for managing individual views in a RecyclerView for devices.
 *
 * This class is responsible for managing individual views within a RecyclerView for devices.
 * It holds references to the TextViews displaying the device name and data.
 * The class provides methods to set the name, data, and a long click listener for the item view.
 *
 */
package com.example.alexucana.fragments.devices;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.alexucana.R;
import com.example.alexucana.models.Device;

/**
 * The DeviceViewHolder class represents a ViewHolder for managing individual views in a RecyclerView for devices.
 */
public class DeviceViewHolder extends RecyclerView.ViewHolder {

    // TextViews to display device name and data
    private final TextView tv_device_name;
    private final TextView tv_device_data;

    /**
     * Constructor for the DeviceViewHolder class.
     *
     * @param itemView The view representing an item within the RecyclerView.
     */
    public DeviceViewHolder(@NonNull View itemView) {
        super(itemView);
        tv_device_name = itemView.findViewById(R.id.tv_device_name);
        tv_device_data = itemView.findViewById(R.id.tv_device_data);
    }

    /**
     * Method to set the device name in the corresponding TextView.
     *
     * @param name The name of the device.
     */
    public void setName(String name) {
        tv_device_name.setText(name);
    }

    /**
     * Method to set the device data in the corresponding TextView.
     *
     * @param data The data of the device.
     */
    public void setData(Object data) {
        tv_device_data.setText(data.toString());
    }

    /**
     * Method to set a long click listener on the item view.
     *
     * @param listener The long click listener to be set on the item view.
     */
    public void setItemLongClickListener(View.OnLongClickListener listener) {
        itemView.setOnLongClickListener(listener); // Set a long click listener on the item view
    }
}
