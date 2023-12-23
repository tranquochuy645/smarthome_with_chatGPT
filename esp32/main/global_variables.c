#include "stdint.h"
#include "stdbool.h"
uint8_t ssid[33] = {0};
uint8_t password[65] = {0};
uint8_t room_id[37] = {0};
uint8_t bssid[6];         // this one is not terminated by null ( not a string )
bool bssid_set = false;
char device_id[33] = {0};