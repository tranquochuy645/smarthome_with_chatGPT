/**
 * DataManager: Singleton class for managing data in Firebase Realtime Database.
 * This class provides methods for fetching, synchronizing, and updating data in Firebase Realtime Database.
 * It includes functionality to retrieve lists of rooms and devices, as well as adding, removing, and updating data.
 *
 */
package com.example.alexucana.controllers;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.alexucana.config;
import com.example.alexucana.models.Device;
import com.example.alexucana.models.Room;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * The DataManager class represents a singleton class for managing data in Firebase Realtime Database.
 */
public class DataManager {

    public static final String TAG = DataManager.class.getSimpleName();
    private static DataManager instance;

    /**
     * Interface for handling data changes.
     */
    public interface OnDataChanged {
        void apply();
    }

    private OnDataChanged onDataChanged;
    private final DatabaseReference dbRef;
    private final Handler mainHandler;

    private final HashMap<String, HashMap<String, Object>> localDataCopy = new HashMap<>();

    private int roomsCount;

    /**
     * Private constructor for the singleton pattern.
     */
    private DataManager() {
        this.dbRef = FirebaseDatabase.getInstance().getReference().child(config.DATABASE_PATH);
        this.mainHandler = new Handler(Looper.getMainLooper());
        // Fetch initial data
        fetchData();
        this.dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                syncLocalData(snapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    /**
     * Get the singleton instance of DataManager.
     *
     * @return The DataManager instance.
     */
    public static DataManager getInstance() {
        if (instance == null) {
            instance = new DataManager();
        }
        return instance;
    }

    /**
     * Synchronize local data with the Firebase Realtime Database.
     *
     * @param snapshot The DataSnapshot from the database.
     */
    private void syncLocalData(DataSnapshot snapshot) {
        HashMap<String, HashMap<String, Object>> tmp = snapshot.getValue(
                new GenericTypeIndicator<
                        HashMap<
                                String, // Room id
                                HashMap< // Room props
                                        String, // key: [room_name | devices_map]
                                        Object
                                        >
                                >
                        >() {
                });
        if (tmp == null) return;
        Log.d(TAG, tmp.toString());
        roomsCount = tmp.size();
        mainHandler.post(() -> {
            localDataCopy.clear();
            localDataCopy.putAll(tmp);
            onDataChanged.apply();
        });
    }

    /**
     * Set the callback for data changes.
     *
     * @param cb The callback function.
     */
    public void setOnDataChanged(OnDataChanged cb) {
        this.onDataChanged = cb;
    }

    /**
     * Get a list of rooms.
     *
     * @return The list of Room objects.
     */
    public ArrayList<Room> getRoomsList() {
        // Create an ArrayList to store Room objects
        ArrayList<Room> roomsList = new ArrayList<>();

        // If localDataCopy is empty, fetch the data and return an empty array
        if (localDataCopy.isEmpty()) {
            fetchData();
            return roomsList;
        }

        // Iterate over the entry set in localDataCopy
        for (Map.Entry<String, HashMap<String, Object>> entry : localDataCopy.entrySet()) {
            Room room = new Room();
            HashMap<String, Object> val = entry.getValue();
            room.id = entry.getKey();
            try {
                room.name = Objects.requireNonNull(val.get("room_name")).toString();
            } catch (Exception err) {
                room.name = "Unnamed room";
            }
            try {
                room.devices_count = Integer.valueOf(((HashMap) val.get("devices_map")).size()).toString();
            } catch (Exception err) {
                room.devices_count = "0";
            }
            // Add the Room object to the ArrayList
            roomsList.add(room);
        }

        // Return the ArrayList of Room objects
        return roomsList;
    }

    /**
     * Get a list of devices for a specific room.
     *
     * @param roomId The ID of the room.
     * @return The list of Device objects.
     */
    public ArrayList<Device> getDevicesList(String roomId) {
        // Create an ArrayList to store Device objects
        ArrayList<Device> devicesList = new ArrayList<>();

        // If localDataCopy is null, fetch the data and return an empty array
        if (localDataCopy.isEmpty()) {
            fetchData();
            return devicesList;
        }
        try {
            HashMap<String, HashMap<String, Object>> devicesMap = (HashMap<String, HashMap<String, Object>>) localDataCopy.get(roomId).get("devices_map");
            if (devicesMap == null || devicesMap.isEmpty()) {
                return devicesList;
            }
            // Iterate over the entry set in roomData
            for (Map.Entry<String, HashMap<String, Object>> entry : devicesMap.entrySet()) {
                HashMap<String, Object> val = entry.getValue();
                Device device = new Device();
                device.id = entry.getKey();
                device.name = val.get("device_name").toString();
                device.data = "sensors: " + val.get("sensors").toString() + "\n"
                        + "controllable: " + val.get("controllable").toString();

                // Add the Device object to the ArrayList
                devicesList.add(device);
            }
        } catch (Exception err) {
            Log.e("DataManager", "weird data");
        }

        // Return the ArrayList of Device objects
        return devicesList;
    }

    /**
     * Get data as a JSON object.
     *
     * @return The data as a JSON object.
     */
    public JSONObject getDataAsJson() {
        return new JSONObject(localDataCopy);
    }

    /**
     * Fetch data from Firebase Realtime Database.
     */
    private void fetchData() {
        this.dbRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DataSnapshot snapshot = task.getResult();
                if (snapshot.exists()) {
                    syncLocalData(snapshot);
                }
            }
        });
    }

    /**
     * Add a new room to the database.
     *
     * @param roomName The name of the new room.
     */
    public void addRoom(String roomName) {
        HashMap<String, Object> updateMap = new HashMap<>();
        updateMap.put("room_name", roomName);
        dbRef.push().setValue(updateMap);
    }

    /**
     * Remove a room from the database.
     *
     * @param roomId The ID of the room to remove.
     */
    public void removeRoom(String roomId) {
        if (roomsCount < 2) {
            // If the current roomsCount is exactly 1, the next room deletion will cause the whole dbRef to disappear.
            // Thus, it won't trigger the value event listener to refresh the UI (no more room).
            // Handle this case by clearing the localDataCopy and then executing onDataChanged manually.
            dbRef.child(roomId).removeValue().addOnCompleteListener(
                    task -> {
                        if (task.isSuccessful()) {
                            this.localDataCopy.clear();
                            onDataChanged.apply();
                        }
                    }
            );
        } else {
            // Otherwise, just fire removeValue; the result will be handled by the value event listener.
            dbRef.child(roomId).removeValue();
        }
    }

    /**
     * Remove a device from a specific room in the database.
     *
     * @param roomId   The ID of the room.
     * @param deviceId The ID of the device to remove.
     */
    public void removeDevice(String roomId, String deviceId) {
        dbRef.child(roomId).child("devices_map").child(deviceId).removeValue();
    }

    /**
     * Set device data in a specific room in the database.
     *
     * @param roomId         The ID of the room.
     * @param deviceId       The ID of the device.
     * @param dataToUpdate   The data to update.
     */
    public void setDevice(String roomId, String deviceId, Map<String, Object> dataToUpdate) {
        DatabaseReference deviceRef = dbRef.child(roomId).child("devices_map").child(deviceId).child("controllable");
        for (String key : dataToUpdate.keySet()) {
            deviceRef.child(key).setValue(dataToUpdate.get(key));
        }
    }
}
