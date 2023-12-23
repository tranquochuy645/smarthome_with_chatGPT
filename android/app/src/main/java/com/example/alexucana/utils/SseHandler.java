/**
 * SseHandler: Handles Server-Sent Events (SSE) communication for streaming chat responses.
 *
 * This class extends the EventSourceListener to handle events and errors during SSE communication.
 * It processes incoming data chunks, identifies content or function calls, and triggers corresponding callbacks.
 * SSE is used for streaming responses from the OpenAI API.
 *
 */
package com.example.alexucana.utils;

import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;

/**
 * The SseHandler class handles Server-Sent Events (SSE) communication for streaming chat responses.
 */
public class SseHandler extends EventSourceListener {

    // Tag for logging purposes
    public static final String TAG = SseHandler.class.getSimpleName();

    // Callback interfaces for handling different types of events
    public interface OnContentChunkHandler {
        void apply(String character);
    }

    public interface OnFunctionCallHandler {
        void apply(String argumentsAsJsonString, String functionName) throws JSONException;
    }

    public interface OnStreamStoppedHandler {
        void apply();
    }

    // Callback handlers
    private final OnStreamStoppedHandler streamStoppedHandler;
    private final OnContentChunkHandler contentChunkHandler;
    private final OnFunctionCallHandler functionCallHandler;

    // Caches for function call details
    private String functionArgumentsCache = "";
    private String functionNameCache = "";

    /**
     * Constructor for SseHandler.
     *
     * @param streamStoppedHandler   Callback for handling the end of the streaming process.
     * @param contentChunkHandler    Callback for processing content chunks.
     * @param functionCallHandler    Callback for processing function calls.
     */
    public SseHandler(OnStreamStoppedHandler streamStoppedHandler,
                      OnContentChunkHandler contentChunkHandler,
                      OnFunctionCallHandler functionCallHandler) {
        this.streamStoppedHandler = streamStoppedHandler;
        this.contentChunkHandler = contentChunkHandler;
        this.functionCallHandler = functionCallHandler;
    }

    /**
     * This method is called when a new event is received in the Server-Sent Events (SSE) protocol.
     *
     * @param eventSource The EventSource instance.
     * @param id          The event identifier.
     * @param type        The event type.
     * @param data        The event data.
     */
    @Override
    public void onEvent(@NonNull EventSource eventSource, String id, String type, @NonNull String data) {
        // Check if the received data indicates the end of the streaming process
        if (data.equals("[DONE]")) {
            // Signal the Text-to-Speech (TTS) engine to stop
            streamStoppedHandler.apply();
            return;
        }

        try {
            Log.d(TAG, "Received chunk: " + data);
            // Destructuring data based on the provided chunk format: https://platform.openai.com/docs/api-reference/chat/streaming

            // Parse the received JSON data to extract relevant information
            JSONObject choice = new JSONObject(data)
                    .getJSONArray("choices")
                    .getJSONObject(0);

            // Get the finish reason from the choice
            String finishReason = choice.optString("finish_reason");

            // Check if the finish reason indicates a function call
            if (finishReason.equals("function_call")) {
                // Log that the stream has stopped
                Log.d(TAG, "STREAM STOPPED");

                // Trigger the callback for processing the function call
                functionCallHandler.apply(functionNameCache, functionArgumentsCache);

                // Log details of the fired function
                Log.d(TAG, "Fired a function: " + functionNameCache + "\n " + functionArgumentsCache);

                // Clean up caches after processing the function
                functionArgumentsCache = "";
                functionNameCache = "";
                return;
            }

            // If the finish reason is not a function call, proceed to process content or potential function call

            // Extract the delta object from the choice
            JSONObject delta = choice.getJSONObject("delta");

            // Try to get content chunk from the delta
            String contentChunk = delta.optString("content");

            // Check if content chunk is null or not found mapping or empty string
            // Then the chunk should have a function call
            if (contentChunk.isEmpty() || contentChunk.equals("null")) {
                // Attempt to extract a function call from the delta
                // Get the function call object from the delta
                JSONObject functionCall = delta.getJSONObject("function_call");
                // WILL CRASH HERE IF NO FUNCTION CALL -> TO THE CATCH BLOCK
                // Try to get the expected full function name on the first chunk
                functionNameCache += functionCall.optString("name");
                // Append function arguments to the cache
                functionArgumentsCache += functionCall.optString("arguments");
                return;
            }
            // Trigger the callback for processing the content chunk
            contentChunkHandler.apply(contentChunk);
        } catch (JSONException e) {
            // This is fine, just a tricky if else
        }
    }

    /**
     * This method is called when an error occurs during the Server-Sent Events (SSE) communication.
     *
     * @param eventSource The EventSource instance.
     * @param err         The error that occurred.
     * @param res         The HTTP response associated with the error.
     */
    @Override
    public void onFailure(@NonNull EventSource eventSource, Throwable err, Response res) {
        // Log an error message indicating that the request has failed
        Log.e(TAG, "Request failed");

        // Check if the response object is null
        if (res == null) return;

        // Log the HTTP error code from the response
        Log.e(TAG, "Error code: " + res.code());

        // Check if the response body is null
        if (res.body() == null) return;

        try {
            // Log the response body as a string
            Log.e(TAG, "Response body: " + res.body().string());
        } catch (IOException e) {
            // If an IOException occurs while reading the response body, throw a RuntimeException
            throw new RuntimeException(e);
        }

        // Check if the error object is null
        if (err == null) return;

        // Log the error message from the error object
        Log.e(TAG, "Message: " + err.getMessage());

        // Create a StringBuilder to build the exception stack trace
        StringBuilder trace = new StringBuilder();

        // Iterate over the elements in the exception stack trace
        for (StackTraceElement element : err.getStackTrace()) {
            // Append each stack trace element to the StringBuilder with a newline
            trace.append(element.toString()).append("\n");
        }

        // Log the complete exception stack trace
        Log.e(TAG, "Exception stack trace: \n" + trace);
    }
}
