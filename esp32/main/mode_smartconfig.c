#include <stdlib.h>
#include <sys/param.h>
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "freertos/event_groups.h"
#include "esp_log.h"
#include "driver/gpio.h"
#include "esp_http_client.h"
#include "global_variables.h"
#include "project_config.h"
#include "esp_smartconfig.h"
#include "esp_wifi.h"
#include "utils.h"

#define TAG_INIT "Smartconfig mode init"
static EventGroupHandle_t event_group_handle;
static const int CONNECTED_BIT = BIT0;
static const int ESPTOUCH_DONE_BIT = BIT1;

static bool introduce_new_device()
{
    char devices_map_url[151] = {0};
    snprintf(devices_map_url, 151, "%s/%s/devices_map.json", DB_ROOT_URL, room_id);
    esp_http_client_config_t config = {
        .url = devices_map_url,
    };

    esp_http_client_handle_t client = esp_http_client_init(&config);

    ESP_ERROR_CHECK(esp_http_client_set_method(client, HTTP_METHOD_POST));
    ESP_ERROR_CHECK(esp_http_client_set_header(client, "Content-Type", "application/json"));
    ESP_ERROR_CHECK(esp_http_client_open(client, strlen(DATA_MODEL)));
    int len = esp_http_client_write(client, DATA_MODEL, strlen(DATA_MODEL));
    if (len < 0)
    {
        esp_http_client_cleanup(client);
        return false;
    }
    char buffer[65] = {0};
    len = esp_http_client_fetch_headers(client); // this will block till response is available
    if (len < 0)
    {
        esp_http_client_cleanup(client);
        return false;
    }
    if(esp_http_client_get_status_code(client)!=200){
         esp_http_client_cleanup(client);
        return false;
    }

    len = esp_http_client_read_response(client, buffer, 64);

    char *pos = strstr(buffer, ":");
    if (pos == NULL)
    {
        ESP_LOGE("Code", "null pos");
        esp_http_client_cleanup(client);
        return false;
    }

    pos++;
    // Move past the ":"
    char *start = strchr(pos, '\"');
    char *end = strchr(start + 1, '\"'); // Find the closing quote

    if (start == NULL || end == NULL)
    {
        esp_http_client_cleanup(client);
        return false;
    }
    *end = '\0'; // Properly terminate the string
    strncpy(device_id, start + 1, sizeof(device_id) - 1);
    device_id[sizeof(device_id) - 1] = '\0'; // Ensure null-termination
    ESP_LOGI("Device id: ", "%s", device_id);
    esp_http_client_cleanup(client);
    return true;
}

// TASK: smartconfig
static void smartconfig_task(void *parm)
{
    EventBits_t uxBits;
    ESP_ERROR_CHECK(esp_smartconfig_set_type(SC_TYPE_ESPTOUCH_V2));
    smartconfig_start_config_t cfg = SMARTCONFIG_START_CONFIG_DEFAULT();
    ESP_ERROR_CHECK(esp_smartconfig_start(&cfg));
    while (1)
    {
        uxBits = xEventGroupWaitBits(event_group_handle, ESPTOUCH_DONE_BIT | CONNECTED_BIT, true, false, 100);
        if (uxBits & CONNECTED_BIT)
        {
            ESP_LOGI(TAG_INIT, "WiFi Connected to ap");
            if (introduce_new_device() && write_nvs())
            {
                ESP_LOGI(TAG_INIT, "init ok, restarting");
                esp_restart();
            }
            else
            {
                ESP_LOGI(TAG_INIT, "init failed, delete everything");
                interrupt_hard_reset();
            }
        }
        if (uxBits & ESPTOUCH_DONE_BIT)
        {
            // smartconfig module sends the ip and stuff (ACK_DONE) to the phone to anounce it connected to wifi successfully
            // then the wifi event handler will fire this block on SC_EVENT_SEND_ACK_DONE
            ESP_LOGI(TAG_INIT, "smartconfig over");
            esp_smartconfig_stop();
        }
    }
}

// Wifi event handler // smartconfig mode
static void wifi_event_handler(void *arg, esp_event_base_t event_base, int32_t event_id, void *event_data)
{
    if (event_base == WIFI_EVENT && event_id == WIFI_EVENT_STA_START)
    {
        xTaskCreate(&smartconfig_task, "smartconfig_task", 4096, NULL, 3, NULL);
    }
    else if (event_base == WIFI_EVENT && event_id == WIFI_EVENT_STA_DISCONNECTED)
    {
        wifi_event_sta_disconnected_t *event = (wifi_event_sta_disconnected_t *)event_data;
        xEventGroupClearBits(event_group_handle, CONNECTED_BIT);
        if (WIFI_REASON_4WAY_HANDSHAKE_TIMEOUT == event->reason || WIFI_REASON_AUTH_FAIL == event->reason)
        {
            // wrong wifi credentials
            interrupt_hard_reset();
        }
        else
        {
            esp_wifi_connect();
        }
    }
    else if (event_base == IP_EVENT && event_id == IP_EVENT_STA_GOT_IP)
    {
        xEventGroupSetBits(event_group_handle, CONNECTED_BIT);
    }
    else if (event_base == SC_EVENT && event_id == SC_EVENT_SCAN_DONE)
    {
        ESP_LOGI(TAG_INIT, "Scan done");
    }
    else if (event_base == SC_EVENT && event_id == SC_EVENT_FOUND_CHANNEL)
    {
        ESP_LOGI(TAG_INIT, "Found channel");
    }
    else if (event_base == SC_EVENT && event_id == SC_EVENT_GOT_SSID_PSWD)
    {
        ESP_LOGI(TAG_INIT, "Got SSID and password");

        smartconfig_event_got_ssid_pswd_t *evt = (smartconfig_event_got_ssid_pswd_t *)event_data;
        if (evt->type != SC_TYPE_ESPTOUCH_V2)
        {
            // Only accept esptouch v2
            esp_restart();
            // won't reach here
            return;
        }
        memcpy(ssid, evt->ssid, sizeof(evt->ssid));
        memcpy(password, evt->password, sizeof(evt->password));

        ESP_LOGI(TAG_INIT, "SSID:%s", ssid);
        ESP_LOGI(TAG_INIT, "PASSWORD:%s", password);
        ESP_ERROR_CHECK(esp_smartconfig_get_rvd_data(room_id, sizeof(room_id)));
        ESP_LOGI(TAG_INIT, "RVD_DATA: %s", room_id);

        bssid_set = evt->bssid_set;
        if (bssid_set)
        {
            memcpy(bssid, evt->bssid, sizeof(bssid));
            ESP_LOGI(TAG_INIT, "BSSID: %02x:%02x:%02x:%02x:%02x:%02x", bssid[0], bssid[1], bssid[2], bssid[3], bssid[4], bssid[5]);
        }
        wifi_config_t wifi_config;
        bzero(&wifi_config, sizeof(wifi_config_t));
        memcpy(wifi_config.sta.ssid, ssid, sizeof(wifi_config.sta.ssid));
        memcpy(wifi_config.sta.password, password, sizeof(wifi_config.sta.password));
        wifi_config.sta.bssid_set = bssid_set;
        if (bssid_set)
        {
            memcpy(wifi_config.sta.bssid, bssid, sizeof(wifi_config.sta.bssid));
        }

        ESP_ERROR_CHECK(esp_wifi_disconnect());
        ESP_ERROR_CHECK(esp_wifi_set_config(WIFI_IF_STA, &wifi_config));
        esp_wifi_connect();
    }
    else if (event_base == SC_EVENT && event_id == SC_EVENT_SEND_ACK_DONE)
    {
        xEventGroupSetBits(event_group_handle, ESPTOUCH_DONE_BIT);
    }
}

// Smartconfig mode for main app, called when init data is not available ( not found in nvs "storage" namespace)
void init_smartconfig_mode()
{
    event_group_handle = xEventGroupCreate();
    ESP_ERROR_CHECK(esp_event_handler_register(WIFI_EVENT, ESP_EVENT_ANY_ID, &wifi_event_handler, NULL));
    ESP_ERROR_CHECK(esp_event_handler_register(IP_EVENT, IP_EVENT_STA_GOT_IP, &wifi_event_handler, NULL));
    ESP_ERROR_CHECK(esp_event_handler_register(SC_EVENT, ESP_EVENT_ANY_ID, &wifi_event_handler, NULL));

    ESP_ERROR_CHECK(esp_wifi_set_mode(WIFI_MODE_STA));
    ESP_ERROR_CHECK(esp_wifi_start());
}
