#include <sys/param.h>
#include "freertos/FreeRTOS.h"
#include "esp_log.h"
#include "driver/gpio.h"
#include "esp_http_client.h"
#include "dht11.h"
#include "ledc.h"
#include "utils.h"
#include "esp_wifi.h"
#include "mode_smartconfig.h"
#include "mode_working.h"
#include "nvs_flash.h"
#include "project_config.h"
#define INPUT_RESET GPIO_NUM_0 // The boot button, not the interrupt button // This one is used to clear nvs storage then restart

void app_init()
{
    ESP_ERROR_CHECK(nvs_flash_init());

    // Configure boot button GPIO as input
    gpio_config_t io_conf = {};

    // Hard reset button
    io_conf.pin_bit_mask = (1ULL << INPUT_RESET);
    io_conf.mode = GPIO_MODE_INPUT;
    io_conf.intr_type = GPIO_INTR_LOW_LEVEL;
    io_conf.pull_up_en = GPIO_PULLUP_ENABLE;
    io_conf.pull_down_en = GPIO_PULLDOWN_DISABLE;
    ESP_ERROR_CHECK(gpio_config(&io_conf));

    // Install ISR service
    ESP_ERROR_CHECK(gpio_install_isr_service(0));

    // Hook ISR handler to the GPIO pin
    ESP_ERROR_CHECK(gpio_isr_handler_add(INPUT_RESET, interrupt_hard_reset, NULL));

    // https://www.esp32.com/viewtopic.php?f=2&p=118180

    //////////////////// default wifi init  //////////////////

    ESP_ERROR_CHECK(esp_netif_init());
    ESP_ERROR_CHECK(esp_event_loop_create_default());
    esp_netif_create_default_wifi_sta();

    wifi_init_config_t cfg = WIFI_INIT_CONFIG_DEFAULT();
    ESP_ERROR_CHECK(esp_wifi_init(&cfg));
    //////////////////// default wifi init  //////////////////

    if (read_nvs())
    {
        init_working_mode();
        // Will start another thread to listen for events from firebase realtime database
        // Then continue this thread
    }
    else
    {
        init_smartconfig_mode();
        // This will block the thread until wifi cred and database url is received
        // Then it will restart esp, re-run this main_app again
        // If wifi cred and database url is saved properly in nvs strorage
        // it won't fall into this else block in the next boot
    }
}