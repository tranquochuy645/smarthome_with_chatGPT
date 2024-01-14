/**
 * OpenAiController: Class for managing communication with OpenAI's GPT-3.5-turbo API.
 * This class handles the communication with the OpenAI API, particularly GPT-3.5-turbo.
 * It constructs requests, handles server-sent events (SSE), and executes functions based on responses.
 * The class maintains a message log and includes methods for appending responses and executing functions
 * as part of the conversation context.
 *
 */
package com.example.alexucana.controllers;

import android.net.Uri;
import android.util.Log;

import com.example.alexucana.config;
import com.example.alexucana.fragments.alex.AlexFragment;
import com.example.alexucana.utils.SseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.internal.sse.RealEventSource;

/**
 * The OpenAiController class represents a class for managing communication with OpenAI's GPT-3.5-turbo API.
 */
public class OpenAiController {

    public static final String TAG = OpenAiController.class.getSimpleName();
    private static final int TIMEOUT_SECONDS = 60;
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final String token = config.OPENAI_API_KEY;

    private JSONArray functionsAsset; // The static template
    public final SseHandler.OnContentChunkHandler onContentChunk;

    private final DataManager dbController;
    private final OkHttpClient httpClient;
    private final JSONArray msgLog;
    private String lastResponse = "";

    /**
     * Interface for loading JSON data.
     */
    public interface JsonLoader {
        String apply(String name);
    }

    /**
     * Interface for handling copied commands.
     */
    interface OnCopiedCommand {
        void apply();
    }
    public OnCopiedCommand sayOk;
    /**
     * Constructor for the OpenAiController class.
     *
     * @param fn_1       The content chunk handler for server-sent events.
     * @param jsonLoader The JSON loader for loading data.
     */
    public OpenAiController(SseHandler.OnContentChunkHandler fn_1,  JsonLoader jsonLoader) {
        msgLog = new JSONArray();
        functionsAsset = new JSONArray();
        dbController = DataManager.getInstance();

        onContentChunk = (content) -> {
            fn_1.apply(content);
            if(content==null) return;
            lastResponse += content;
        };

        httpClient = new OkHttpClient.Builder()
                .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();

        try {
            JSONObject systemMsg = new JSONObject(jsonLoader.apply("system_msg"));
            msgLog.put(systemMsg);
            functionsAsset = new JSONArray(jsonLoader.apply("functions"));
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    /**
     * Call the OpenAI API with the given question.
     *
     * @param question The user's question.
     */
    public void callOpenAiApi(String question) {
        // OkHttpClient
        JSONObject jsonBody = new JSONObject();
        JSONObject newMsg = new JSONObject();
        while (msgLog.length() > 2) {
            msgLog.remove(1);
        }
        try {
            newMsg.put("role", "user");
            newMsg.put("content", question + "\n Current data: " + dbController.getDataAsJson().toString());
            msgLog.put(newMsg);

            jsonBody.put("model", "gpt-3.5-turbo");
            jsonBody.put("messages", msgLog);
            jsonBody.put("functions", functionsAsset);
            jsonBody.put("function_call", "auto");
            jsonBody.put("max_tokens", 3000);
            jsonBody.put("temperature", 0.8);
            jsonBody.put("n", 1);
            jsonBody.put("stream", true);
        } catch (JSONException e) {
            Log.e(TAG, "JSON Exception: " + e.getMessage());
        }

        RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .header("Authorization", "Bearer " + token)
                .header("Accept", "text/event-stream")
                .post(body)
                .build();

        Log.d("Req body", jsonBody.toString());

        RealEventSource realEventSource =
                new RealEventSource(
                        request,
                        new SseHandler(
                                this::appendResponseToMsgLog,
                                onContentChunk,
                                this::execFunction)
                );

        realEventSource.connect(httpClient);

        // Fire the request
        httpClient.newCall(request);

        // Logging for debug
        Log.d(TAG, "Sent a question: " + question);

        JSONObject currentData = dbController.getDataAsJson();
        if (currentData != null) {
            Log.d(TAG, currentData.toString());
        } else {
            Log.d(TAG, "No data from firebase");
        }
    }

    /**
     * Execute a function based on its name and arguments.
     *
     * @param fnName            The name of the function.
     * @param argsAsJsonString  The arguments for the function in JSON format.
     * @throws JSONException    If there is an issue with JSON parsing.
     */
    private void execFunction(String fnName, String argsAsJsonString) throws JSONException {
        switch (fnName) {
            case "set_device_data":
                onContentChunk.apply("Đã rõ");
                onContentChunk.apply("!");
                setDeviceData(argsAsJsonString);
                break;
            // Add more cases for additional function calls
            default:
                Log.e(TAG, "Function call error, unexpected function name: " + fnName);
                break;
        }
    }

    /**
     * Set device data based on the provided arguments.
     *
     * @param args  The arguments in JSON format.
     * @throws JSONException    If there is an issue with JSON parsing.
     */
    private void setDeviceData(String args) throws JSONException {
        if (sayOk != null) sayOk.apply();

        final JSONObject tmp = new JSONObject(args);
        final String roomId = tmp.optString("room_id");
        final String deviceId = tmp.optString("device_id");
        final String newData = tmp.optString("new_data");
        Uri uri = Uri.parse(newData);

        Set<String> queryParameterNames = uri.getQueryParameterNames();
        Map<String, Object> newDataMap = new HashMap<>();
        for (String paramName : queryParameterNames) {
            String paramValue = uri.getQueryParameter(paramName);
            assert paramValue != null;
            if (paramValue.equalsIgnoreCase("true") || paramValue.equalsIgnoreCase("false")) {
                newDataMap.put(paramName, Boolean.parseBoolean(paramValue));
            } else if (paramValue.matches("-?\\d+(\\.\\d+)?")) {
                if (paramValue.contains(".")) {
                    newDataMap.put(paramName, Double.parseDouble(paramValue));
                } else {
                    newDataMap.put(paramName, Long.parseLong(paramValue));
                }
            } else {
                newDataMap.put(paramName, paramValue);
            }
        }
        dbController.setDevice(roomId, deviceId, newDataMap);
    }

    /**
     * Append the last response to the message log.
     */
    private void appendResponseToMsgLog() {
        if (lastResponse.isEmpty()) return;
        JSONObject newMsg = new JSONObject();
        try {
            newMsg.put("role", "assistant");
            newMsg.put("content", lastResponse);
            lastResponse = "";
            msgLog.put(newMsg);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
