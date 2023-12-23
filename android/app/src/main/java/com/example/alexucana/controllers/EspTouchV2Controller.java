/**
 * EspTouchV2Controller: Class for managing ESP-Touch V2 provisioning.
 *
 * This class provides functionality for starting and stopping ESP-Touch V2 provisioning,
 * handling responses, and managing the ESP-Provisioner instance.
 *
 */
package com.example.alexucana.controllers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.espressif.iot.esptouch2.provision.EspProvisioner;
import com.espressif.iot.esptouch2.provision.EspProvisioningListener;
import com.espressif.iot.esptouch2.provision.EspProvisioningRequest;
import com.espressif.iot.esptouch2.provision.EspProvisioningResult;

import java.io.UnsupportedEncodingException;

/**
 * The EspTouchV2Controller class represents a class for managing ESP-Touch V2 provisioning.
 */
public class EspTouchV2Controller {

    public static final String ESPTOUCH_ENCODING_CHARSET = "UTF-8";
    public static final String TAG = EspTouchV2Controller.class.getSimpleName();

    /**
     * Interface for handling ESP-Touch V2 provisioning responses.
     */
    public interface ResponseHandler {
        void apply(@Nullable EspProvisioningResult result);
    }

    private static ResponseHandler resHandler;

    private static EspProvisioner provisioner;

    /**
     * ESP-Touch V2 provisioning listener.
     */
    private static final EspProvisioningListener listener = new EspProvisioningListener() {

        @Override
        public void onStart() {
            Log.d(TAG, "Start provisioning...");
        }

        @Override
        public void onResponse(EspProvisioningResult result) {
            Log.d(TAG, "Got response: " + result.address);
            if (resHandler != null) {
                resHandler.apply(result);
            }
            Log.d(TAG, "Success, dismissing provisioner");
            dismiss();
        }

        @Override
        public void onStop() {
            Log.d(TAG, "Stopped provisioning");
            if (resHandler != null) {
                resHandler.apply(null);
            }
            dismiss();
        }

        @Override
        public void onError(Exception e) {
            Log.d(TAG, "Err: " + e.getMessage());
            if (resHandler != null) {
                resHandler.apply(null);
            }
            dismiss();
        }
    };

    /**
     * Set the response handler for ESP-Touch V2 provisioning.
     *
     * @param fn The response handler.
     */
    public static void setResponseHandler(ResponseHandler fn) {
        resHandler = fn;
    }

    /**
     * Start ESP-Touch V2 provisioning.
     *
     * @param context    The application context.
     * @param ssid       The SSID of the Wi-Fi network.
     * @param bssid      The BSSID of the Wi-Fi network.
     * @param pwd        The password of the Wi-Fi network.
     * @param customData The custom data for provisioning.
     */
    public static void startProvisioning(Context context, String ssid, String bssid, String pwd, String customData) {
        if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
            ssid = ssid.substring(1, ssid.length() - 1);
        }
        byte[] _ssid = getBytesByString(ssid);
        byte[] _bssid = parseBssid2bytes(bssid);
        byte[] _pwd = getBytesByString(pwd);
        byte[] _customData = getBytesByString(customData);
        if (_customData.length == 0 || _customData.length > EspProvisioningRequest.RESERVED_LENGTH_MAX)
            throw new RuntimeException("Custom data is weird");

        EspProvisioningRequest request = new EspProvisioningRequest.Builder(context)
                .setSSID(_ssid)
                .setBSSID(_bssid)
                .setPassword(_pwd)
                .setReservedData(_customData)
                .build();
        if (provisioner == null) {
            provisioner = new EspProvisioner(context);
        } else if (provisioner.isProvisioning()) {
            provisioner.stopProvisioning();
        }
        provisioner.startProvisioning(request, listener);
    }

    /**
     * Dismiss the ESP-Provisioner instance.
     */
    public static void dismiss() {
        if (provisioner == null) return;
        if (provisioner.isProvisioning()) {
            provisioner.stopProvisioning();
        }
        provisioner.close();
        provisioner = null;
    }

    /**
     * Convert a string to bytes using the specified charset.
     *
     * @param string The string to convert.
     * @return The byte array.
     */
    public static byte[] getBytesByString(String string) {
        try {
            return string.getBytes(ESPTOUCH_ENCODING_CHARSET);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("The charset is invalid");
        }
    }

    /**
     * Parse BSSID string to bytes.
     *
     * @param bssid The BSSID string.
     * @return The byte array representing the BSSID.
     */
    public static byte[] parseBssid2bytes(String bssid) {
        String[] bssidSplits = bssid.split(":");
        byte[] result = new byte[bssidSplits.length];
        for (int i = 0; i < bssidSplits.length; i++) {
            result[i] = (byte) Integer.parseInt(bssidSplits[i], 16);
        }
        return result;
    }
}
