/**
 * AlexFragment: Fragment for interacting with an AI (Alex) using OpenAI API and TextToSpeech (TTS).
 *
 * This fragment is responsible for managing the interaction with an AI named Alex using the OpenAI API
 * for natural language processing. It also utilizes TextToSpeech (TTS) for audio responses. The class
 * initializes the OpenAiController and TtsController, and provides methods to ask questions to Alex,
 * handle TTS cleanup on fragment destruction, and manage the UI responses.
 *
 */
package com.example.alexucana.fragments.alex;

import android.app.Activity;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.example.alexucana.R;
import com.example.alexucana.controllers.OpenAiController;
import com.example.alexucana.controllers.TtsController;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * The AlexFragment class represents a fragment for interacting with an AI (Alex) using OpenAI API and TextToSpeech (TTS).
 */
public class AlexFragment extends Fragment {

    // OpenAiController for managing OpenAI API calls
    private final OpenAiController openAi;

    // TextToSpeech engine for audio responses
    private TextToSpeech ttsEngine;

    // String to store the full answer
    private String fullAns = "";

    // String to cache the sentence during TTS processing
    private String sentenceCache = "";

    // TextView to display the response in the UI
    private TextView tv_response;

    // Reference to the parent activity
    private final Activity parent;

    // Set of special characters
    private final Set<String> specialCharacters = new HashSet<>(Arrays.asList(".", ",", "?", "!"));

    /**
     * Constructor for the AlexFragment class.
     *
     * @param p The parent activity.
     */
    public AlexFragment(Activity p) {
        parent = p;
        openAi = new OpenAiController(this::appendWordToAnswer, this::loadJson);
    }

    /**
     * Inflate the fragment's view.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container          If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return The View for the fragment's UI.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.chat_box, container, false);
        tv_response = view.findViewById(R.id.tv_response);
        ttsEngine = new TtsController(getContext()).getEngine();
        return view;
    }

    /**
     * Clean up resources when the fragment is destroyed.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (ttsEngine != null) {
            ttsEngine.speak("", TextToSpeech.QUEUE_FLUSH, null); // Flush the queue
            ttsEngine.stop();
        }
    }

    /**
     * Ask a question to Alex.
     *
     * @param question The question to ask.
     */
    public void askAlex(String question) {
        fullAns = "";
        openAi.callOpenAiApi(question);
    }

    /**
     * Add a word to the TextToSpeech speaking queue.
     *
     * @param word The word to add to the speaking queue.
     */
    private void addToSpeakingQueue(String word) {
        if (ttsEngine != null) {
            ttsEngine.speak(word, TextToSpeech.QUEUE_ADD, null);
        } else {
            ttsEngine = new TtsController(getContext()).getEngine();
        }
    }

    /**
     * Append a word to the answer and update the UI.
     *
     * @param w The word to append.
     */
    private void appendWordToAnswer(String w) {
        if (specialCharacters.contains(w)) {
            addToSpeakingQueue(sentenceCache);
            sentenceCache = "";
        } else {
            sentenceCache += w;
        }
        parent.runOnUiThread(() -> {
            fullAns += w;
            tv_response.setText(fullAns);
        });
    }

    /**
     * Load a JSON file from the assets folder.
     *
     * @param name The name of the JSON file.
     * @return The content of the JSON file as a string.
     */
    private String loadJson(String name) {
        try {
            InputStream is = parent.getAssets().open(name + ".json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            return new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
