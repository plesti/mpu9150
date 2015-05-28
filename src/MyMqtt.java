import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

/**
 * Created by QPAKSSD on 2015.05.15..
 */
public class MyMqtt {
    public static final int QOS             = 2;
    public static final String MQTT_BROKER  = "tcp://url:1883";
    public static final String CLIENT_ID    = "Raspby";
    public static final String DATA_TOPIC   = "mpu9150/data";
    public static final String CONFIG_TOPIC = "mpu9150/config";

    static MemoryPersistence persistence;
    private MqttClient sampleClient;

    public MyMqtt(MqttCallback callback) {
        persistence = new MemoryPersistence();

        try {
            sampleClient = new MqttClient(MyMqtt.MQTT_BROKER, MyMqtt.CLIENT_ID, persistence);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            System.out.println("Connecting to broker: " + MyMqtt.MQTT_BROKER);
            sampleClient.connect(connOpts);
            System.out.println("Connected MQTT");
            sampleClient.setCallback(callback);
            sampleClient.subscribe(CONFIG_TOPIC);
            System.out.println("Subscribed to " + CONFIG_TOPIC);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void stopMqtt() {
        try {
            sampleClient.unsubscribe(CONFIG_TOPIC);
            sampleClient.disconnect();
        } catch (MqttException e) {
            e.printStackTrace();
        }
        System.out.println("Disconnected MQTT");
    }

    public void sendMqtt(String topic, String msg) {
        if (sampleClient == null)
            return;

        MqttMessage message = new MqttMessage(msg.getBytes());
        message.setQos(MyMqtt.QOS);
        try {
            sampleClient.publish(topic, message);
            System.out.println("Message sent!");
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
