#include "esp_event.h"
#include "esp_log.h"
#include "esp_wifi.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "esp_http_client.h"
#include "project_config.h"
#include "ledc.h"
#include "dht11.h"
#include "utils.h"
#include "esp_system.h"
#include "global_variables.h"
#include "controllable_event_handler.h"

#define TAG_INIT "Working mode init"
#define TAG_WORKER "Worker task"

// TASK: cron job updating data to database
static void run_when_wifi_connected_task(void *parm)
{
    char sensors_url[182];
    snprintf(sensors_url, sizeof(sensors_url), "%s/%s/devices_map/%s/sensors.json", DB_ROOT_URL, room_id, device_id);
    while (1)
    {
        vTaskDelay(2000); // update data to database every 10 seconds
        // Must delay to share resources with other tasks
        struct dht11_reading dht_buffer = DHT11_read();
        if (dht_buffer.status != 0) // -1 means failed
            continue;
        char jsonString[128];
        snprintf(jsonString, sizeof(jsonString),
                 "{ \"temperature(°C)\" : %d , \"humidity(%%)\" : %d }",
                 dht_buffer.temperature,
                 dht_buffer.humidity);
        esp_http_client_config_t config = {
            .url = sensors_url,
        };
        esp_http_client_handle_t client = esp_http_client_init(&config);
        esp_http_client_set_method(client, HTTP_METHOD_PATCH);
        esp_http_client_set_header(client, "Content-Type", "application/json");
        esp_http_client_set_post_field(client, jsonString, strlen(jsonString));
        esp_http_client_perform(client);
        if (esp_http_client_get_status_code(client) == 401)
        {
            // permission denied -> user has deleted this device
            interrupt_hard_reset();
        }
        else
        {
            esp_http_client_close(client);
            esp_http_client_cleanup(client);
        }
    }
}

// TASK: listener for data changes on the cloud realtime database
static void rtdb_listening_task(void *parm)
{
    char controllable_url[182];
    snprintf(controllable_url, sizeof(controllable_url), "%s/%s/devices_map/%s/controllable.json", DB_ROOT_URL, room_id, device_id);
    ESP_LOGI(TAG_WORKER, "Start listening to rtdb, url = %s", controllable_url);
    while (1)
    {
        /* code */
        char output_buffer[129] = {0}; // Buffer to store response of http request
        esp_http_client_config_t config = {
            .url = controllable_url,
        };
        esp_http_client_handle_t client = esp_http_client_init(&config);
        esp_http_client_set_header(client, "Accept", "text/event-stream");

        esp_err_t err = esp_http_client_open(client, 0);
        if (err != ESP_OK)
        {
            ESP_LOGE(TAG_WORKER, "Failed to open HTTP connection: %s", esp_err_to_name(err));
        }
        else
        {
            int content_length = esp_http_client_fetch_headers(client);
            if (content_length < 0)
            {
                ESP_LOGE(TAG_WORKER, "HTTP client fetch headers failed");
            }
            else
            {
                int data_len = esp_http_client_read_response(client, output_buffer, 128);
                int status_code = esp_http_client_get_status_code(client);
                if (status_code == 200)
                {

                    // IMPORTANT
                    // The initial timeout is by default 5000ms, when opening a new connection, 5000ms is good enough
                    // But esp_http_client_read_response only returns when the connection is timeout, or full response is available
                    // Since we are handling SSE, where the chunks are coming forever, the workaround to reduce latency is setting timeout to a lower value (e.g. 500ms) AFTER the first response chunk (connection opened successfully)
                    esp_http_client_set_timeout_ms(client, 500); // set timeout to 500ms

                    controllable_event_handler(output_buffer);
                    memset(output_buffer, 0, sizeof(output_buffer));
                    ESP_LOGI(TAG_WORKER, "SSE handler ready");
                    while (esp_http_client_is_chunked_response(client)) // infinite thread blocking
                    {
                        data_len = esp_http_client_read_response(client, output_buffer, 129);
                        if (data_len == 0)
                        {
                            // data_len == 0 means no chunk
                            continue;
                        }
                        if (data_len == 30)
                        {
                            // data_len of keep-alive event length is always 30
                            memset(output_buffer, 0, sizeof(output_buffer)); // Clear the buffer
                            continue;
                        }
                        controllable_event_handler(output_buffer);
                        memset(output_buffer, 0, sizeof(output_buffer)); // Clear the buffer
                    }
                }
                else if (status_code == 404)
                {
                    // not found -> user has deleted this device
                    interrupt_hard_reset();
                }
                else
                {
                    ESP_LOGE(TAG_WORKER, "Failed to read response");
                }
            }
        }
        esp_http_client_close(client);
        esp_http_client_cleanup(client);
        ESP_LOGI(TAG_WORKER, "HTTP connection closed");
        vTaskDelay(1000);
    }
    // vTaskDelete(NULL);
}

// Wifi event handler // operation mode
static void wifi_event_handler(void *arg, esp_event_base_t event_base, int32_t event_id, void *event_data)
{
    TaskHandle_t rtdb_listener = NULL;
    TaskHandle_t main_loop = NULL;
    if (event_base == WIFI_EVENT && event_id == WIFI_EVENT_STA_DISCONNECTED)
    {
        if (rtdb_listener != NULL)
        {
            vTaskDelete(rtdb_listener);
        }
        if (main_loop != NULL)
        {
            vTaskDelete(main_loop);
        }
        wifi_event_sta_disconnected_t *event = (wifi_event_sta_disconnected_t *)event_data;
        if (WIFI_REASON_4WAY_HANDSHAKE_TIMEOUT == event->reason || WIFI_REASON_AUTH_FAIL == event->reason)
        {
            // wrong wifi credentials
            interrupt_hard_reset();
        }
        else
        {
            ESP_LOGI(TAG_INIT, "WiFi disconnected, reconnecting...");
            esp_wifi_connect();
        }
    }
    else if (event_base == IP_EVENT && event_id == IP_EVENT_STA_GOT_IP)
    {
        // xEventGroupSetBits(s_wifi_event_group, CONNECTED_BIT);
        ESP_LOGI(TAG_INIT, "WiFi connected to ap");
        if (rtdb_listener == NULL) // safety check
        {
            // Very important to reduce latency of realtime data update
            // set priority high e.g: configMAX_PRIORITIES - 1 (~24)
            xTaskCreate(&rtdb_listening_task, "rtdb_listening_task", 4096, NULL, 24, &rtdb_listener);
        }
        if (main_loop == NULL) // safety check
        {
            xTaskCreate(&run_when_wifi_connected_task, "run_when_wifi_connected_task", 4096, NULL, 1, &main_loop);
        }
    }
}

// Normal mode (got wifi, and database url)
void init_working_mode()
{
    ESP_ERROR_CHECK(esp_event_handler_register(WIFI_EVENT, ESP_EVENT_ANY_ID, &wifi_event_handler, NULL));
    ESP_ERROR_CHECK(esp_event_handler_register(IP_EVENT, IP_EVENT_STA_GOT_IP, &wifi_event_handler, NULL));

    wifi_config_t wifi_config;
    bzero(&wifi_config, sizeof(wifi_config_t));
    wifi_config.sta.bssid_set = bssid_set;
    if (bssid_set)
    {
        memcpy(wifi_config.sta.bssid, bssid, sizeof(wifi_config.sta.bssid));
    }
    memcpy(wifi_config.sta.ssid, ssid, sizeof(wifi_config.sta.ssid));
    memcpy(wifi_config.sta.password, password, sizeof(wifi_config.sta.password));

    ESP_ERROR_CHECK(esp_wifi_set_mode(WIFI_MODE_STA));
    ESP_ERROR_CHECK(esp_wifi_set_config(WIFI_IF_STA, &wifi_config));
    ESP_ERROR_CHECK(esp_wifi_start());

    ESP_ERROR_CHECK(esp_wifi_disconnect());
    esp_wifi_connect();
}