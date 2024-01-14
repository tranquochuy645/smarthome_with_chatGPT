#include "string.h"
#include "ledc.h"
#include "esp_log.h"
#include "esp_system.h"
// Function: read the chunk buffer from firebase and decide what to do with it
void controllable_event_handler(const char *buffer)
{
    // Ignore if null data; this means it is a keep-alive event type
    if (strstr(buffer, "null") != NULL)
        return;

    char *pos = strstr(buffer, "0x");
    if (pos != NULL)
    {
        // Allocate memory for local_cache and copy data
        char *local_cache = strdup(pos);
        if (local_cache != NULL)
        { // Find the end of the hex color string (before the next \")
            char *quotePos = strstr(local_cache, "\"");
            if (quotePos != NULL)
            {
                *quotePos = '\0'; // Null-terminate the string at the quote position
                // TODO: Now local_cache contains the hex color string
                ESP_LOGI("controllable_event_handler", "Received hex color: %s", local_cache);
                ledc_set_color(hex_color_to_uint32(local_cache));
            }
            else
            {
                ESP_LOGE("controllable_event_handler", "Memory allocation failed");
            }
            free(local_cache);
        }
        else
        {
            ESP_LOGE("controllable_event_handler", "Invalid event format: %s", buffer);
        }
    }
}
