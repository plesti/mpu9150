import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;

import java.io.IOException;

/**
 * Created by Peter Lesti on 2015.03.31..
 */
public class Main {
    private static DataReaderRunnable runnable;
    private static Thread thread;
    private static MPU9150 sensor;

    public static void main(String[] args) {
        System.out.println("Program start!");
        System.out.println("Press Enter to finish!");

        try {
            // Get the bus 0 of the Raspberry
            I2CBus bus = I2CFactory.getInstance(I2CBus.BUS_0);
            System.out.println("[ OK ] Bus connected!");

            I2CDevice device = bus.getDevice(MPU9150.ADDRESS);
            sensor = new MPU9150(device);
            System.out.println("[ OK ] Device connected!");

            // Set power on to allow accelerator config
            sensor.setPowerMode(MPU9150.POWER_MODE_ON);
            // Set full scale range
            sensor.setRange(MPU9150.RANGE_2_G);
            // Activate low power wake-up mode
            sensor.setPowerMode(MPU9150.POWER_MODE_CYCLE);
            // Set data rate
            sensor.setWakeUpFrequency(MPU9150.WAKEUP_FREQUENCY_20HZ);

            System.out.println("[ OK ] Device configured!");

            // Stopwatch to measure reading time
            long time = System.currentTimeMillis();

            // Start reading sensor data
            startReading();

            // Wait until Enter button pressed
            System.in.read();

            // Stop reading thread
            stopReading();

            System.out.println("Reading finished in " + ((System.currentTimeMillis() - time) / 1000.0) + "sec");

            sensor.setPowerMode(MPU9150.POWER_MODE_SLEEP);
            System.out.println("[ OK ] Sleep mode on!");

        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Program ended!");
    }

    private static void stopReading() {
        if (thread != null) {
            runnable.makeStop();
            System.out.println("Stop measuring...");
            try {
                thread.join(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("Thread stopped!");
        }
    }

    private static void startReading() {
        runnable = new DataReaderRunnable();
        thread = new Thread(runnable);
        thread.start();
    }

    /**
     * Thread to read sensor data
     */
    static class DataReaderRunnable implements Runnable {
        boolean stopping = false;
        int number = 0;

        @Override
        public void run() {
            System.out.println("Thread started!");

            try {
                while (!stopping) {
                    if ((sensor.getDevice().read(MPU9150.REG_INT_STATUS) & 0x01) == 0) {
                        // Data not ready
                        continue;
                    }
                    // Initialize vector with zeros
                    byte[] accelData = new byte[6];

                    int datacount = sensor.getAcceleration(accelData);

                    // Print warning if reading was not complete
                    if (datacount != 6) {
                        System.out.println("Error reading acceleration data, < 6 bytes");
                    }

                    // Get acceleration in G
                    double x = ((accelData[0] << 8) | (accelData[1] & 0xFF)) / 16384.0;
                    double y = ((accelData[2] << 8) | (accelData[3] & 0xFF)) / 16384.0;
                    double z = ((accelData[4] << 8) | (accelData[5] & 0xFF)) / 16384.0;

                    // Print datas to stdout
                    String text = String.format("%d X: %.4f Y: %.4f Z: %.4f", number, x, y, z);
                    System.out.println(text);

                    number++;
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                System.out.println("Measured: " + number);
            }
        }

        public void makeStop() {
            stopping = true;
        }
    }


}
