#include <math.h>
#include "ledc.h"
#include "driver/ledc.h"
#include "esp_err.h"
#include "esp_log.h"
#include <string.h>


esp_err_t ledc_init()
{
	ledc_timer_config_t ledc_timer = {
		.duty_resolution = LEDC_DUTY_RES, // resolution of PWM duty
		.freq_hz = LEDC_FREQUENCY,		  // frequency of PWM signal
		.speed_mode = LEDC_MODE,		  // timer mode
		.timer_num = LEDC_TIMER,		  // timer index
		.clk_cfg = LEDC_AUTO_CLK,		  // Auto select the source clock
	};
	ledc_timer_config(&ledc_timer);

	ledc_channel_config_t channel_red = {
		.channel = LEDC_CHANNEL_0,
		.duty = 0,
		.gpio_num = OUTPUT_RED,
		.speed_mode = LEDC_MODE,
		.hpoint = 0,
		.timer_sel = LEDC_TIMER_0};
	ledc_channel_config_t channel_green = {
		.channel = LEDC_CHANNEL_1,
		.duty = 0,
		.gpio_num = OUTPUT_GREEN,
		.speed_mode = LEDC_MODE,
		.hpoint = 0,
		.timer_sel = LEDC_TIMER_0};
	ledc_channel_config_t channel_blue = {
		.channel = LEDC_CHANNEL_2,
		.duty = 0,
		.gpio_num = OUTPUT_BLUE,
		.speed_mode = LEDC_MODE,
		.hpoint = 0,
		.timer_sel = LEDC_TIMER_0};
	ledc_channel_config(&channel_red);
	ledc_channel_config(&channel_green);
	ledc_channel_config(&channel_blue);

	return ESP_OK;
}


uint32_t hex_color_to_uint32(char *hexColor)
{
	 if (hexColor == NULL || hexColor[0] != '0' || hexColor[1] != 'x')
    {
        return 0; // Invalid hex color format, return an appropriate error code
    }

    uint32_t color = (uint32_t)strtol(hexColor, NULL, 16);

    // Ensure that the color value is within the valid range
    if (color > 0xFFFFFF)
    {
        return 0; // Invalid color value, return an appropriate error code
    }

    return color;
}
void ledc_set_color(uint32_t color)
{
	// Extract red, green, and blue components from the 24-bit color value
	uint8_t red = (color >> 16) & 0xFF;
	uint8_t green = (color >> 8) & 0xFF;
	uint8_t blue = color & 0xFF;

	// // Common cathode
	// int scaled = red * FACTOR;
	// ledc_set_duty(LEDC_MODE, LEDC_CHANNEL_0, scaled);
	// ledc_update_duty(LEDC_MODE, LEDC_CHANNEL_0);
	// scaled = green * FACTOR;
	// ledc_set_duty(LEDC_MODE, LEDC_CHANNEL_1, scaled);
	// ledc_update_duty(LEDC_MODE, LEDC_CHANNEL_1);
	// scaled = blue * FACTOR;
	// ledc_set_duty(LEDC_MODE, LEDC_CHANNEL_2, scaled);
	// ledc_update_duty(LEDC_MODE, LEDC_CHANNEL_2);

	// // Common anode
	// Reverse the scaling for common anode LED
	int scaled = (255 - red) * FACTOR;
	ledc_set_duty(LEDC_MODE, LEDC_CHANNEL_0, scaled);
	ledc_update_duty(LEDC_MODE, LEDC_CHANNEL_0);
	scaled = (255 - green) * FACTOR;
	ledc_set_duty(LEDC_MODE, LEDC_CHANNEL_1, scaled);
	ledc_update_duty(LEDC_MODE, LEDC_CHANNEL_1);
	scaled = (255 - blue) * FACTOR;
	ledc_set_duty(LEDC_MODE, LEDC_CHANNEL_2, scaled);
	ledc_update_duty(LEDC_MODE, LEDC_CHANNEL_2);
}

void ledc_get_color(uint32_t *color)
{
	int factor = (int)pow(2, LEDC_DUTY_RES) / 256;

	int red_scaled = ledc_get_duty(LEDC_MODE, LEDC_CHANNEL_0);
	int green_scaled = ledc_get_duty(LEDC_MODE, LEDC_CHANNEL_1);
	int blue_scaled = ledc_get_duty(LEDC_MODE, LEDC_CHANNEL_2);

	// Scale the values back to the range [0, 255]
	uint8_t red = red_scaled / factor;
	uint8_t green = green_scaled / factor;
	uint8_t blue = blue_scaled / factor;

	// Compose the 24-bit color value
	*color = ((uint32_t)red << 16) | ((uint32_t)green << 8) | blue;
}