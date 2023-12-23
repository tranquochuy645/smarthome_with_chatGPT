/**
 * DevicesAdapter: Adapter for managing the data and views of devices in a RecyclerView.
 *
 * This adapter is responsible for managing the data and views of devices within a RecyclerView.
 * It handles the creation and binding of DeviceViewHolder instances and provides methods for
 * adding, removing devices, and updating the dataset when the data changes.
 *
 */
package com.example.alexucana.fragments.devices;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.alexucana.R;
import com.example.alexucana.controllers.EspTouchV2Controller;
import com.example.alexucana.controllers.DataManager;
import com.example.alexucana.models.Device;
import com.example.alexucana.utils.PopUpDialogHelper;

import java.util.ArrayList;

/**
 * The DevicesAdapter class represents an adapter for managing the data and views of devices in a RecyclerView.
 */
public class DevicesAdapter extends RecyclerView.Adapter<DeviceViewHolder> {

    // Tag for logging purposes
    public static final String TAG = DevicesAdapter.class.getSimpleName();

    // Reference to the associated activity
    public Activity activity;

    // Reference to the DataManager for managing device data
    private final DataManager dbController;

    // List to hold device data
    private ArrayList<Device> data;

    // Current room identifier
    private String currentRoom;

    /**
     * Constructor for the DevicesAdapter class.
     *
     * @param activity The associated activity.
     */
    public DevicesAdapter(Activity activity) {
        this.activity = activity;

        // Set up the EspTouchV2Controller response handler on the UI thread
        EspTouchV2Controller.setResponseHandler(result -> activity.runOnUiThread(() -> {
            if (result == null) {
                // Handle provisioning failure
                PopUpDialogHelper.showDialog(
                        activity,
                        PopUpDialogHelper.TYPE_FAILED,
                        true,
                        "Failed!"
                );
            } else {
                // Handle provisioning success
                PopUpDialogHelper.showDialog(
                        activity,
                        PopUpDialogHelper.TYPE_SUCCESS,
                        true,
                        "Success!\n"
                                + "bssid: " + result.bssid + "\n"
                                + "ipv4: " + result.address.getHostAddress()
                );
            }
        }));

        // Initialize the DataManager
        dbController = DataManager.getInstance();
        dbController.setOnDataChanged(this::onDataChanged);

        // Initialize the data list with devices for the current room
        data = dbController.getDevicesList(this.currentRoom);

        // Notify the adapter if the data is not empty
        if (!data.isEmpty()) {
            notifyDataSetChanged();
        }
    }

    /**
     * Set the current room for the DevicesAdapter.
     *
     * @param roomId The identifier of the current room.
     */
    public void setCurrentRoom(String roomId) {
        this.currentRoom = roomId;
        onDataChanged();
    }

    /**
     * Update the data when it changes.
     */
    private void onDataChanged() {
        this.data = dbController.getDevicesList(this.currentRoom);
        notifyDataSetChanged();
    }

    /**
     * Create a new DeviceViewHolder instance.
     *
     * @param parent   The parent view group.
     * @param viewType The type of the view.
     * @return A new DeviceViewHolder instance.
     */
    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(this.activity).inflate(R.layout.card_device_0, parent, false);
        return new DeviceViewHolder(view);
    }

    /**
     * Bind device data to the DeviceViewHolder.
     *
     * @param holder   The DeviceViewHolder instance.
     * @param position The position of the item in the data list.
     */
    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        Device device = data.get(position);
        holder.setName(device.name);
        holder.setData(device.data);
        holder.setItemLongClickListener(
                v -> {
                    removeDevice(position); // Handle device deletion
                    return true;
                }
        );
    }

    /**
     * Get the total number of devices in the data list.
     *
     * @return The total number of devices.
     */
    @Override
    public int getItemCount() {
        return data.size();
    }

    /**
     * Add a new device by displaying an AlertDialog to get user input.
     */
    public void addDevice() {
        // Use an AlertDialog to get user input
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Enter WiFi Password");
        final LinearLayout wrapper = new LinearLayout(activity);
        wrapper.setOrientation(LinearLayout.VERTICAL);

        // Automatically populate the SSID and BSSID fields
        WifiManager wifiManager = (WifiManager) activity.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        Log.d(TAG, "Wifi info: " + wifiInfo.toString());
        final String ssid = wifiInfo.getSSID();
        if (ssid.isEmpty()) return;
        final String bssid = wifiInfo.getBSSID();
        if (bssid.isEmpty()) return;

        final EditText ssidInput = new EditText(activity);
        ssidInput.setInputType(InputType.TYPE_CLASS_TEXT);
        ssidInput.setHint("WiFi SSID");
        ssidInput.setText(ssid);
        ssidInput.setEnabled(false);
        wrapper.addView(ssidInput);

        final EditText bssidInput = new EditText(activity);
        bssidInput.setInputType(InputType.TYPE_CLASS_TEXT);
        bssidInput.setHint("WiFi BSSID");
        bssidInput.setText(bssid);
        bssidInput.setEnabled(false);
        wrapper.addView(bssidInput);

        final EditText passwordInput = new EditText(activity);
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordInput.setHint("WiFi Password");
        wrapper.addView(passwordInput);

        builder.setView(wrapper);

        // Set up the buttons
        builder.setPositiveButton("OK", (dialog, which) -> {
            final String password = passwordInput.getText().toString();
            if (currentRoom.isEmpty() || password.isEmpty()) return;
            EspTouchV2Controller.startProvisioning(activity, ssid, bssid, password, currentRoom);
            PopUpDialogHelper.showDialog(activity, PopUpDialogHelper.TYPE_PENDING, false, "Connecting to device ... ");
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    /**
     * Remove a device at the specified position.
     *
     * @param position The position of the device in the data list.
     */
    private void removeDevice(final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Confirm Deletion");
        builder.setMessage("Are you sure you want to delete this device?");

        builder.setPositiveButton("Yes", (dialog, which) -> {
            if (position < 0 || position >= data.size()) return;
            dbController.removeDevice(currentRoom, data.get(position).id);
        });

        builder.setNegativeButton("No", (dialog, which) -> dialog.cancel());

        builder.create().show();
    }
}
