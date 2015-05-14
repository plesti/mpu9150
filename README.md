# About
This java driver is a brief example of using the MPU-9150 9DOF to measure acceleration data through I2C interface.

# Hardware
* Raspberry Pi
* InvenSense MPU-9150 (https://www.sparkfun.com/products/11486)

# Instructions
1. Install Pi4j and Wiring Pi
2. Connect the sensor (+3,3V, GND, SDA, SCL, AD0 pins) to the Raspberry GPIO port
3. Check on which bus do you use in Main.java

`I2CBus bus = I2CFactory.getInstance(I2CBus.BUS_0);` 
4. Check the ADDRESS in MPU9150.java corresponding to your sensor address

`sudo i2cdetect..`