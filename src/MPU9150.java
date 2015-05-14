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


    public MPU9150(I2CDevice device) {
        try {
            int devid = device.read(REG_DEVID); // 133
            if (devid != 133) {
                return;
            }
            sensor = device;
        } catch (IOException e) {
            e.printStackTrace();
        }
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
     * Register 1C bit4-3
     * @param range selected scale (e.g. {@link #RANGE_2_G RANGE_2_G})
     * @throws IOException
     */
    public void setRange(int range) throws IOException {
        int format = (byte) (sensor.read(REG_ACCEL_CONFIG) | ((range & 0x03) << 3));
        sensor.write(REG_ACCEL_CONFIG, (byte) format);
    }

    /**
     * Change the device power management like low power mode (cycle), or
     * sleep. For low power mode you can set wake-up frequency.
     * See {@link #setWakeUpFrequency(int) setWakeUpFrequency}
     * @param mode (e.g. {@link #POWER_MODE_SLEEP POWER_MODE_SLEEP})
     * @throws IOException
     */
    public void setPowerMode(int mode) throws IOException {
        int format;

        if (mode == POWER_MODE_ON) {
            format = (byte) (sensor.read(REG_PWR_MGMT_1) & 0x9F);
            sensor.write(REG_PWR_MGMT_1, (byte) format);
        }
        else if (mode == POWER_MODE_SLEEP) {
            format = (byte) (sensor.read(REG_PWR_MGMT_1) & 0x9F) | 0x40;
            sensor.write(REG_PWR_MGMT_1, (byte) format);
        }
        else if (mode == POWER_MODE_CYCLE) {
            format = (byte) (sensor.read(REG_PWR_MGMT_1) & 0x9F) | 0x20;
            sensor.write(REG_PWR_MGMT_1, (byte) format);
        }
    }

    /**
     * Set Wake-up frequency for CYCLE power mode. See {@link #setPowerMode(int) setPowerMode}
     * @param frequency update frequency (e.g. {@link #WAKEUP_FREQUENCY_20HZ WAKEUP_FREQUENCY_20HZ})
     * @throws IOException
     */
    public void setWakeUpFrequency(int frequency) throws IOException {
        int format = sensor.read(REG_PWR_MGMT_2) | (frequency << 6);
        sensor.write(REG_PWR_MGMT_2, (byte) format);
    }

    /**
     * Read acceleration sensor data to array
     * @param store sensor data to store in. Length should be 6.
     * @return number of bytes read
     * @throws IOException
     */
    public int getAcceleration(byte[] store) throws IOException {
        if (store.length != 6) {
            throw new IndexOutOfBoundsException("acceldata must be 6 byte");
        }

        // Data must be read in one burst or multiple byte read
        // because the vector will be erased from sensor memory on any
        // (single or multiple) read. See documentation..
        return sensor.read(REG_ACCEL_XOUT_H, store, 0, 6);
    }



}
