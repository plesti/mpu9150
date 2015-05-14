import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

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

            // Set power on to allow to config
            sensor.setPowerMode(MPU9150.POWER_MODE_ON);
            // Set full scale range of accelerator data
            sensor.setRange(MPU9150.RANGE_2_G);
            // Activate low power wake-up mode
            sensor.setPowerMode(MPU9150.POWER_MODE_CYCLE);
            // Set data rate
            sensor.setWakeUpFrequency(MPU9150.WAKEUP_FREQUENCY_1_25HZ);

            System.out.println("[ OK ] Device configured!");

            // Stopwatch to measure reading time
            long time = System.currentTimeMillis();

            // Start reading sensor data
            startReading();

            // Wait until Enter button pressed
            //  open up standard input
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            boolean t = true;
            while (t) {
                String read= br.readLine();
                if (read.equals("5hz")) {
                    sensor.setWakeUpFrequency(MPU9150.WAKEUP_FREQUENCY_5HZ);
                }
                else if (read.equals("1.25hz")) {
                    sensor.setWakeUpFrequency(MPU9150.WAKEUP_FREQUENCY_1_25HZ);
                }
                else if (read.equals("20hz")) {
                    sensor.setWakeUpFrequency(MPU9150.WAKEUP_FREQUENCY_20HZ);
                }
                else if (read.equals("cycle")) {
                    sensor.setPowerMode(MPU9150.POWER_MODE_CYCLE);
                }
                else if (read.equals("sleep")) {
                    sensor.setPowerMode(MPU9150.POWER_MODE_SLEEP);
                }
                else if (read.equals("on")) {
                    sensor.setPowerMode(MPU9150.POWER_MODE_ON);
                }
                else if (read.equals("2g")) {
                    sensor.setRange(MPU9150.RANGE_2_G);
                }
                else if (read.equals("4g")) {
                    sensor.setRange(MPU9150.RANGE_4_G);
                }
                else if (read.equals("r")) {
                    sensor.reset();
                }
                else if (read.equals("e")) {
                    break;
                }
            }


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
                    if (!sensor.isDataReady()) {
                        continue;
                    }

                    // Initialize vector with zeros
                    int[] accelData = new int[3];

                    int datacount = sensor.getAcceleration(accelData);

                    // Print warning if reading was not complete
                    if (datacount != 6) {
                        System.out.println("Error reading acceleration data, < 6 bytes");
                    }

                    // Get acceleration in G
                    double x = accelData[0] / 16384.0;
                    double y = accelData[1] / 16384.0;
                    double z = accelData[2] / 16384.0;

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
