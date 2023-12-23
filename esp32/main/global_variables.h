#include "stdint.h"
#include "stdbool.h"

extern uint8_t ssid[33];
extern uint8_t password[65];
extern uint8_t room_id[37];
extern uint8_t bssid[6];         // this one is not terminated by null ( not a string )
extern bool bssid_set;
extern char device_id[33];