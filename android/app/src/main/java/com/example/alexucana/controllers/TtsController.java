/**
 * TtsController: Singleton class for managing Text-to-Speech (TTS) functionality.
 *
 * This class serves as a singleton for handling Text-to-Speech functionality in the application.
 * It provides methods to initialize the TextToSpeech engine, check if a TTS engine is installed,
 * and create a new instance of the TextToSpeech engine. The class also includes a callback method
 * for initializing the TTS engine and redirects the user to configure or install a TTS engine if needed.
 *
 */
package com.example.alexucana.controllers;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.speech.tts.TextToSpeech;

import com.example.alexucana.config;

import java.util.Locale;

/**
 * The TtsController class represents a singleton class for managing Text-to-Speech (TTS) functionality.
 */
public class TtsController {

    // Default language for Text-to-Speech
    public final Locale DEFAULT_LANG = new Locale(config.TTS_DEFAULT_LOCALE[0], config.TTS_DEFAULT_LOCALE[0]);

    // TextToSpeech engine instance
    private TextToSpeech engine;

    // Context of the application
    private final Context context;

    /**
     * Constructor for the TtsController class.
     *
     * @param ctx The context of the application.
     */
    public TtsController(Context ctx) {
        context = ctx;
        createEngine(); // Initialize the TextToSpeech engine
    }

    /**
     * Callback method for initializing the TextToSpeech engine.
     *
     * @param status The status of the TextToSpeech initialization.
     */
    private void initCallback(int status) {
        // Check if the TextToSpeech initialization is successful
        if (status == TextToSpeech.SUCCESS) {
            this.engine.setLanguage(DEFAULT_LANG); // Set the language for Text-to-Speech
        } else if (isTTSEngineInstalled()) {
            try {
                // Opening up the system settings for Text-to-Speech configuration
                context.startActivity(new Intent().setAction("com.android.settings.TTS_SETTINGS"));
            } catch (ActivityNotFoundException e) {
                // Handle the exception when the specific intent is not found
                // TODO: Throw a dialog prompt to instruct the user to configure TTS manually
            }
        } else {
            // Redirect user to an app manager to install a TTS engine
            // If not installed, guide the user to install a TTS engine from the Play Store
            final String appPackageName = "com.google.android.tts"; // Google TTS package name
            try {
                // Open the Google Play Store for TTS engine installation
                context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
            } catch (ActivityNotFoundException e) {
                // If Google Play Store is not available, open the default market app for installation
                context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
            }
        }
    }

    /**
     * Check if a Text-to-Speech engine is installed on the device.
     *
     * @return True if a TTS engine is installed, false otherwise.
     */
    private boolean isTTSEngineInstalled() {
        PackageManager pm = context.getPackageManager();
        Intent checkTTSIntent = new Intent();
        checkTTSIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        return pm.resolveActivity(checkTTSIntent, PackageManager.MATCH_DEFAULT_ONLY) != null;
    }

    /**
     * Create a new instance of the TextToSpeech engine.
     */
    private void createEngine() {
        engine = new TextToSpeech(context, this::initCallback);
    }

    /**
     * Get the instance of the TextToSpeech engine.
     *
     * @return The TextToSpeech engine instance.
     */
    public TextToSpeech getEngine() {
        return engine;
    }
}
