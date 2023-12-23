
#include "app_init.h"
#include "dht11.h"
#include "ledc.h"

void app_main(void)
{
    app_init(); // block till successfully initialized or trigger restart if failure

    ///////////////////// other components setup ///////////////////////
    DHT11_init(GPIO_NUM_19);
    ledc_init();
    // sensors data updating logic can run here 
    // or go to mode_working.c and put them in run_when_wifi_connected_task(void *parm)
    // controllable events handling is in controllable_event_handler.c file

    ///////////////////// will return //////////////////////
}
