#include "esp_err.h"


#define LEDC_CHANNEL_RED LEDC_CHANNEL_0
#define LEDC_CHANNEL_GREEN LEDC_CHANNEL_1
#define LEDC_CHANNEL_BLUE LEDC_CHANNEL_2

#define LEDC_TIMER LEDC_TIMER_0
#define LEDC_MODE LEDC_LOW_SPEED_MODE
#define LEDC_OUTPUT_IO (5) // Define the output GPIO
#define LEDC_CHANNEL LEDC_CHANNEL_0
#define LEDC_DUTY_RES LEDC_TIMER_13_BIT // Set duty resolution to 13 bits
#define LEDC_DUTY (4096)				// Set duty to 50%. (2 ** 13) * 50% = 4096
#define LEDC_FREQUENCY (4000)			// Frequency in Hertz. Set frequency at 4 kHz

#define FACTOR 32.1254901961 // (2^13)/(2^8-1)

#define OUTPUT_RED GPIO_NUM_25 // rgb stuff
#define OUTPUT_GREEN GPIO_NUM_33
#define OUTPUT_BLUE GPIO_NUM_32

uint32_t hex_color_to_uint32(char *hexColor);
esp_err_t ledc_init();
void ledc_set_color(uint32_t color);
void ledc_get_color(uint32_t *color);
