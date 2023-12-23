package com.example.alexucana;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.View;
import android.widget.ImageButton;
import androidx.fragment.app.Fragment;
import com.example.alexucana.fragments.alex.AlexFragment;
import com.example.alexucana.fragments.devices.DevicesFragment;
import com.example.alexucana.fragments.rooms.RoomsFragment;
import java.util.ArrayList;

/**
 * The MainActivity class is the main entry point of the application and manages the overall UI flow.
 */
public class MainActivity extends AppCompatActivity {

    /**
     * Package name for Google Text-to-Speech application.
     */
    public static final String STT_PACKAGE_NAME = "com.google.android.googlequicksearchbox";

    // Fragments for managing UI components
    private RoomsFragment roomsFrag; // Holds a list of rooms
    private DevicesFragment devicesFrag; // Holds a list of devices in one room
    private AlexFragment alexFrag; // Holds the conversation chat box with Alex

    // UI components
    private ImageButton btnBack; // Back button for navigation

    // Intent for speech recognition
    private final Intent speechRecognizer = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN");

    // Activity result launcher for handling speech-to-text results
    private ActivityResultLauncher<Intent> arLauncher;

    /**
     * Called when the activity is first created.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down,
     *                           this Bundle contains the data it most recently supplied in
     *                           onSaveInstanceState(Bundle). Note: Otherwise, it is null.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Request necessary permissions
        PermissionUtils.requestAllPermissions(this);

        // Initialize activity result launcher for speech-to-text
        arLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), this::sttResultHandler);

        // Initialize fragments and UI components
        roomsFrag = new RoomsFragment(this::openOneRoom);
        alexFrag = new AlexFragment(this);
        devicesFrag = new DevicesFragment();

        // Set the layout for the activity
        setContentView(R.layout.activity_main);

        // Initialize UI components
        btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> backHome());

        // Set click listener for the speak button
        findViewById(R.id.btn_speak).setOnClickListener(v -> startSpeechToText());

        // Initially load the RoomsFragment
        backHome();
    }

    /**
     * Handles the result of permission requests.
     *
     * @param requestCode  The request code passed to requestPermissions().
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Check if all permissions are granted
        if (requestCode != PermissionUtils.REQUEST_CODE_ALL_PERMISSIONS) return;
        if (PermissionUtils.checkAllPermissionsGranted(grantResults)) return;

        // If any permission is not granted, inform the user and close the app or take appropriate action.
        finish();
    }

    /**
     * Loads a specified fragment into the container.
     *
     * @param whichFrag The fragment to be loaded.
     */
    private void loadFragment(Fragment whichFrag) {
        getSupportFragmentManager().beginTransaction().replace(R.id.root, whichFrag).commit();
    }

    /**
     * Initiates the speech-to-text functionality.
     */
    private void startSpeechToText() {
        try {
            arLauncher.launch(speechRecognizer);
        } catch (ActivityNotFoundException err) {
            // If speech-to-text is not available, prompt the user to install or update the required app
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + STT_PACKAGE_NAME)));
            } catch (ActivityNotFoundException err2) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + STT_PACKAGE_NAME)));
            }
        }
    }

    /**
     * Opens a specific room by loading the DevicesFragment.
     *
     * @param roomUuid The unique identifier of the selected room.
     */
    private void openOneRoom(String roomUuid) {
        // Load DevicesFragment for the selected room
        loadFragment(devicesFrag);
        devicesFrag.setRoom(roomUuid);
        // Enable and make the back button visible
        btnBack.setEnabled(true);
        btnBack.setVisibility(View.VISIBLE);
    }

    /**
     * Handles the result of the speech-to-text functionality.
     *
     * @param res The result containing data returned from the speech-to-text activity.
     */
    private void sttResultHandler(ActivityResult res) {
        // Check if the result is OK
        if (res.getResultCode() != RESULT_OK) return;

        // Get the speech-to-text results from the intent
        Intent rawData = res.getData();
        if (rawData == null) return;

        // Get the recognized speech results
        ArrayList<String> arrResults = rawData.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
        if (arrResults == null) return;

        // Load AlexFragment and initiate a conversation with Alex
        loadFragment(alexFrag);
        alexFrag.askAlex(arrResults.get(0));

        // Enable and make the back button visible
        btnBack.setEnabled(true);
        btnBack.setVisibility(View.VISIBLE);
    }

    /**
     * Navigates back to the home fragment (RoomsFragment).
     */
    public void backHome() {
        // Load RoomsFragment
        loadFragment(roomsFrag);

        // Disable and hide the back button
        btnBack.setEnabled(false);
        btnBack.setVisibility(View.INVISIBLE);
    }
}
