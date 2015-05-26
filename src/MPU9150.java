import com.pi4j.io.i2c.I2CDevice;

import java.io.IOException;

/**
 * Created by Peter Lesti on 2015.05.10..
 */
public class MPU9150 {
    public static final int ADDRESS = 0x68;

    public static final int REG_DEVID = 0x00; // Device ID
    public static final int REG_ACCEL_CONFIG = 0x1C;
    public static final int REG_FIFOENABLE = 0x23;
    public static final int REG_INT_STATUS = 0x3A;
    public static final int REG_ACCEL_XOUT_H = 0x3B;
    public static final int REG_TEMP_OUT_H = 0x41;
    public static final int REG_GYRO_XOUT_H = 0x43;
    public static final int REG_USERCONTROL = 0x6A;
    public static final int REG_PWR_MGMT_1 = 0x6B;
    public static final int REG_PWR_MGMT_2 = 0x6C;
    public static final int REG_FIFO_COUNT_H = 0x72;
    public static final int REG_FIFO_COUNT_L = 0x73;

    public static final int RANGE_2_G = 0x00; // +/-  2G (default)
    public static final int RANGE_4_G = 0x01; // +/-  4G
    public static final int RANGE_8_G = 0x02; // +/-  8G
    public static final int RANGE_16_G = 0x03; // +/- 16G

    public static final int WAKEUP_FREQUENCY_1_25HZ = 0x00;
    public static final int WAKEUP_FREQUENCY_5HZ = 0x01;
    public static final int WAKEUP_FREQUENCY_20HZ = 0x02;
    public static final int WAKEUP_FREQUENCY_40HZ = 0x03;

    public static final int POWER_MODE_ON = 0x00;
    public static final int POWER_MODE_SLEEP = 0x01;
    public static final int POWER_MODE_CYCLE = 0x02;

    private I2CDevice sensor;

    public int mode = 0;

    public MPU9150(I2CDevice device) throws IOException {
        // Check device id
//        int devid = device.read(REG_DEVID);
//        if (devid != 133) {
//            return;
//        }

        sensor = device;
    }

    /**
     * Get device id from device memory address 0x00
     * @return device id in decimal
     * @throws IOException
     */
    public int getDeviceID() throws IOException {
        return sensor.read(REG_DEVID);
    }

    public I2CDevice getDevice() {
        return sensor;
    }

    /**
     * Set Full Scale Range for accelerometer output overriding
     * Register 1C bit4-3. This temporarily activates on mode, because
     * range can be set only in this mode!
     * @param range selected scale (e.g. {@link #RANGE_2_G RANGE_2_G})
     * @throws IOException
     */
    public void setRange(int range) throws IOException {
        // Save current mode, by default it is 0 (POWER_MODE_ON)
        int temp = mode;
        // Switch to on mode
        setPowerMode(POWER_MODE_ON);
        // Set range
        int format = (sensor.read(REG_ACCEL_CONFIG) & 0xE7) | ((range & 0x03) << 3);
        sensor.write(REG_ACCEL_CONFIG, (byte) format);
        // Restore original mode
        setPowerMode(temp);
    }

    /**
     * Change the device power management like low power mode (cycle), or
     * sleep. For low power mode you can set wake-up frequency.
     * See {@link #setWakeUpFrequency(int) setWakeUpFrequency}
     * @param _mode (e.g. {@link #POWER_MODE_SLEEP POWER_MODE_SLEEP})
     * @throws IOException
     */
    public void setPowerMode(int _mode) throws IOException {
        int format;

        if (_mode == POWER_MODE_ON) {
            format = (byte) (sensor.read(REG_PWR_MGMT_1) & 0x9F);
            sensor.write(REG_PWR_MGMT_1, (byte) format);
            mode = _mode;
        }
        else if (_mode == POWER_MODE_SLEEP) {
            format = (byte) (sensor.read(REG_PWR_MGMT_1) & 0x9F) | 0x40;
            sensor.write(REG_PWR_MGMT_1, (byte) format);
            mode = _mode;
        }
        else if (_mode == POWER_MODE_CYCLE) {
            format = (byte) (sensor.read(REG_PWR_MGMT_1) & 0x9F) | 0x20;
            sensor.write(REG_PWR_MGMT_1, (byte) format);
            mode = _mode;
        }
    }

    /**
     * Set Wake-up frequency for CYCLE power mode. See {@link #setPowerMode(int) setPowerMode}
     * @param frequency update frequency (e.g. {@link #WAKEUP_FREQUENCY_20HZ WAKEUP_FREQUENCY_20HZ})
     * @throws IOException
     */
    public void setWakeUpFrequency(int frequency) throws IOException {
        int format = (sensor.read(REG_PWR_MGMT_2) & 0x3F) | (frequency << 6);
        sensor.write(REG_PWR_MGMT_2, (byte) format);
    }

    /**
     * Read acceleration sensor data to the given array
     * @param store sensor data to store in. Length should be 6.
     * @return number of bytes read
     * @throws IOException
     */
    public int readAcceleration(int[] store) throws IOException {
        if (store.length != 3) {
            throw new IndexOutOfBoundsException("Store array must be 3 double");
        }
        byte[] accelData = new byte[6];
        // Data must be read in one burst or multiple byte read
        // because the vector will be erased from sensor memory on any
        // (single or multiple) read. See documentation..
        int read = sensor.read(REG_ACCEL_XOUT_H, accelData, 0, 6);

        store[0] = (accelData[0] << 8) | (accelData[1] & 0xFF);
        store[1] = (accelData[2] << 8) | (accelData[3] & 0xFF);
        store[2] = (accelData[4] << 8) | (accelData[5] & 0xFF);

        return read;
    }

    /**
     * Read gyroscope sensor data to the given array
     * @param store sensor data to store in. Length should be 6.
     * @return number of bytes read
     * @throws IOException
     */
    public int readGyroscope(int[] store) throws IOException {
        if (store.length != 3) {
            throw new IndexOutOfBoundsException("Store array must be 3 double");
        }
        byte[] gyroData = new byte[6];
        // Data must be read in one burst or multiple byte read
        // because the vector will be erased from sensor memory on any
        // (single or multiple) read. See documentation..
        int read = sensor.read(REG_GYRO_XOUT_H, gyroData, 0, 6);

        store[0] = (gyroData[0] << 8) | (gyroData[1] & 0xFF);
        store[1] = (gyroData[2] << 8) | (gyroData[3] & 0xFF);
        store[2] = (gyroData[4] << 8) | (gyroData[5] & 0xFF);

        return read;
    }

    /**
     * Read gyroscope sensor data to the given array
     * @return temperature in celsius
     * @throws IOException
     */
    public double readTemperature() throws IOException {
        byte[] temp = new byte[2];
        // Data must be read in one burst or multiple byte read
        // because the vector will be erased from sensor memory on any
        // (single or multiple) read. See documentation..
        sensor.read(REG_TEMP_OUT_H, temp, 0, 2);

        double celsius = ( ((temp[0] << 8) | (temp[1] & 0xFF)) / 340.0) + 35;
//        return ((temp[0] << 8) | (temp[1] & 0xFF));
        return celsius;
    }

    /**
     * Check if data is ready to read.
     * @return
     */
    public boolean isDataReady() throws IOException {
        return ((sensor.read(MPU9150.REG_INT_STATUS) & 0x01) != 0);
    }

    /**
     * Reset Device.
     * Warning! This will reset the memory to defaults
     */
    public void reset() throws IOException {
        sensor.write(REG_PWR_MGMT_1, (byte) 0x80);
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
