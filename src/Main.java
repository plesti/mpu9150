import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by Peter Lesti on 2015.03.31..
 */
public class Main implements MqttCallback {
    private static DataReaderRunnable runnable;
    private static Thread thread;
    private static MPU9150 sensor;
    private static MyMqtt myMqtt;

    public static void main(String[] args) {
        System.out.println("Program start!");
        System.out.println("Press Enter to finish!");

        Main main = new Main();
        // Set Main as callback
        myMqtt = new MyMqtt(main);

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

            myMqtt.stopMqtt();
            System.out.println("[ OK ] MQTT closed!");

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

    @Override
    public void connectionLost(Throwable cause) {

    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        String msg = new String(message.getPayload());
        try {
            HashMap<String, Integer> data = jsonToMap(msg);

            for (String key : data.keySet()) {
                System.out.println("key: " + key);

                if (key.equals("range")) {
                    sensor.setRange(data.get(key));
                }
                else if(key.equals("power")) {
                    sensor.setPowerMode(data.get(key));
                }
                else if(key.equals("freq")) {
                    sensor.setWakeUpFrequency(data.get(key));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {

    }

    public static HashMap<String, Integer> jsonToMap(String t) throws JSONException {

        HashMap map = new HashMap();
        JSONObject jObject = new JSONObject(t);
        Iterator<?> keys = jObject.keys();

        while( keys.hasNext() ){
            String key = (String)keys.next();
            Integer value = jObject.getInt(key);
            map.put(key, value);
        }

//        System.out.println("map : "+map);
        return map;
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

                    int datacount = sensor.readAcceleration(accelData);
                    double temp = sensor.readTemperature();

                    // Print warning if reading was not complete
                    if (datacount != 6) { System.out.println("Error reading acceleration data, < 6 bytes"); }

                    // Get acceleration in G
                    double x = accelData[0] / 16384.0;
                    double y = accelData[1] / 16384.0;
                    double z = accelData[2] / 16384.0;

                    // Build mqtt payload
                    JSONObject jsonObject = new JSONObject();

                    jsonObject.put("id", number)
                            .put("accx", x)
                            .put("accy", y)
                            .put("accz", z)
                            .put("temp", temp);

                    if (sensor.mode == MPU9150.POWER_MODE_ON) {
                        int[] gyrodata = new int[3];
                        sensor.readGyroscope(gyrodata);
                        jsonObject.put("gyrox", gyrodata[0]);
                        jsonObject.put("gyroy", gyrodata[1]);
                        jsonObject.put("gyroz", gyrodata[2]);
                    }

                    String json = jsonObject.toString();
                    // Print datas to stdout
                    String text = String.format("%d X: %.4f Y: %.4f Z: %.4f temp:%f", number, x, y, z, temp);
                    // Publish
                    myMqtt.sendMqtt(MyMqtt.DATA_TOPIC, json);
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
