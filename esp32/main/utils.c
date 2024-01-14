#include "utils.h"
#include "esp_log.h"
#include "global_variables.h"
#include "nvs_flash.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"

#define TAG "Utils"


static void clear_nvs_and_reset_task()
{

    ESP_LOGI("ISR", "Button pressed! Clearing NVS and restarting...");

    // Open NVS
    nvs_handle_t nvs_handle;
    ESP_ERROR_CHECK(nvs_open("storage", NVS_READWRITE, &nvs_handle));
    // Erase all entries
    ESP_ERROR_CHECK(nvs_erase_all(nvs_handle));
    ESP_ERROR_CHECK(nvs_commit(nvs_handle));

    // Close NVS
    nvs_close(nvs_handle);

    // Reset the ESP32
    esp_restart();
}
bool read_nvs()
{
    nvs_handle_t nvs_handle;
    esp_err_t err;

    // Open NVS
    err = nvs_open("storage", NVS_READONLY, &nvs_handle);
    if (err != ESP_OK)
    {
        ESP_LOGE(TAG, "Error opening NVS");
        return false;
    }
    size_t allocated_size = sizeof(ssid) + 1;
    // Read SSID from NVS
    err = nvs_get_str(nvs_handle, "ssid", (char *)ssid, &allocated_size);
    if (err == ESP_OK && allocated_size > 0)
    {
        ESP_LOGI(TAG, "Read SSID from NVS: %s", ssid);
    }
    else
    {
        ESP_LOGE(TAG, "SSID not found in NVS");
        nvs_close(nvs_handle);
        return false;
    }

    // Read password from NVS
    allocated_size = sizeof(password) + 1;
    err = nvs_get_str(nvs_handle, "password", (char *)password, &allocated_size);
    if (err == ESP_OK && allocated_size > 0)
    {
        ESP_LOGI(TAG, "Read password from NVS: %s", password);
    }
    else
    {
        ESP_LOGE(TAG, "Password not found in NVS");
        nvs_close(nvs_handle);
        return false;
    }

    // Read room_id from NVS
    allocated_size = sizeof(room_id) + 1;
    err = nvs_get_str(nvs_handle, "room_id", (char *)room_id, &allocated_size);
    if (err == ESP_OK && allocated_size > 0)
    {
        ESP_LOGI(TAG, "Read room_id from NVS: %s", room_id);
    }
    else
    {
        ESP_LOGE(TAG, "room_id not found in NVS");
        nvs_close(nvs_handle);
        return false;
    }

    // Read device_id from NVS
    allocated_size = sizeof(device_id) + 1;
    err = nvs_get_str(nvs_handle, "device_id", (char *)device_id, &allocated_size);
    if (err == ESP_OK && allocated_size > 0)
    {
        ESP_LOGI(TAG, "Read device_id from NVS: %s", device_id);
    }
    else
    {
        ESP_LOGE(TAG, "device_id not found in NVS");
        nvs_close(nvs_handle);
        return false;
    }

    // Read bssid from NVS
    allocated_size = sizeof(bssid) + 1;
    // because bssid is not terminated by null ( last byte != 0x00 )
    // I cheat by adding 1 to the length
    err = nvs_get_str(nvs_handle, "bssid", (char *)bssid, &allocated_size);
    if (err == ESP_OK && allocated_size == sizeof(bssid) + 1)
    // since this is very tricky, hardcoded length check is necessary
    {
        ESP_LOGI(TAG, "Read bssid from NVS: %02x:%02x:%02x:%02x:%02x:%02x", bssid[0], bssid[1], bssid[2], bssid[3], bssid[4], bssid[5]);
        bssid_set = true;
    }
    else
    { // otherwise, no bssid is just fine

        ESP_LOGE(TAG, "bssid is not set, real size: %d", allocated_size);
        bssid_set = false;
        // it is fine if there is no bssid available
    }

    // Close NVS
    nvs_close(nvs_handle);
    return true;
}

bool write_nvs()
{

    nvs_handle_t nvs_handle;
    esp_err_t err;

    // Open NVS
    err = nvs_open("storage", NVS_READWRITE, &nvs_handle);
    if (err != ESP_OK)
    {
        ESP_LOGE(TAG, "Error opening NVS");
        return false;
    }

    // Write SSID to NVS
    err = nvs_set_str(nvs_handle, "ssid", (const char *)ssid);
    if (err != ESP_OK)
    {
        ESP_LOGE(TAG, "Error writing SSID to NVS");
        nvs_close(nvs_handle);
        return false;
    }

    // Write password to NVS
    err = nvs_set_str(nvs_handle, "password", (const char *)password);
    if (err != ESP_OK)
    {
        ESP_LOGE(TAG, "Error writing password to NVS");
        nvs_close(nvs_handle);
        return false;
    }
    // Write bssid to NVS
    if (bssid_set)
    {
        err = nvs_set_str(nvs_handle, "bssid", (const char *)bssid);
        if (err != ESP_OK)
        {
            ESP_LOGE(TAG, "Error writing bssid to NVS");
            nvs_close(nvs_handle);
            return false;
        }
        ESP_LOGI(TAG, "Wrote  BSSID: %02x:%02x:%02x:%02x:%02x:%02x", bssid[0], bssid[1], bssid[2], bssid[3], bssid[4], bssid[5]);
    }
    else
    {
        err = nvs_erase_key(nvs_handle, "bssid");
        if (err != ESP_OK)
        {
            ESP_LOGE(TAG, "Error erasing old bssid in NVS");
            nvs_close(nvs_handle);
            return false;
        }
    }

    // Write room_id to NVS
    err = nvs_set_str(nvs_handle, "room_id", (const char *)room_id);
    if (err != ESP_OK)
    {
        ESP_LOGE(TAG, "Error writing room_id to NVS");
        nvs_close(nvs_handle);
        return false;
    }

    // Write device_id to NVS
    err = nvs_set_str(nvs_handle, "device_id", (const char *)device_id);
    if (err != ESP_OK)
    {
        ESP_LOGE(TAG, "Error writing device_id to NVS");
        nvs_close(nvs_handle);
        return false;
    }

    // Commit the changes
    err = nvs_commit(nvs_handle);
    // Close NVS
    nvs_close(nvs_handle);
    if (err != ESP_OK)
    {
        ESP_LOGE(TAG, "Error committing NVS changes");
        return false;
    }
    ESP_LOGI(TAG, "Wrote data to NVS successfully: room_id = %s", room_id);
    return true;
}

void interrupt_hard_reset()
{
    xTaskCreate(&clear_nvs_and_reset_task, "clear_nvs_and_reset_task", 2048, NULL, configMAX_PRIORITIES, NULL);
}
