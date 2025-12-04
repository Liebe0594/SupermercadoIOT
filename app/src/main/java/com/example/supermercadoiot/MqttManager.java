package com.example.supermercadoiot;

import android.content.Context;
import android.util.Log;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import java.nio.charset.StandardCharsets;

public class MqttManager {

    private MqttClient cliente;
    private InterfaceMqtt callback;

    // --- CREDENCIALES HIVEMQ CLOUD
    private static final String SERVER_URI = "ssl://ff8c68175d2c46d29a6021a7123f122d.s1.eu.hivemq.cloud:8883";
    private static final String USERNAME = "JoseNeira";
    private static final String PASSWORD = "Jose123.";

    // Tópico raíz para suscribirse a todo
    private static final String TOPIC_WILDCARD = "supermercado/sucursal1/caja01/#";

    public MqttManager(Context context, InterfaceMqtt callback) {
        this.callback = callback;
    }

    public void conectar() {
        new Thread(() -> {
            try {

                String clientId = "AndroidHybrid_" + System.currentTimeMillis();


                cliente = new MqttClient(SERVER_URI, clientId, new MemoryPersistence());

                MqttConnectOptions opciones = new MqttConnectOptions();
                opciones.setUserName(USERNAME);
                opciones.setPassword(PASSWORD.toCharArray());
                opciones.setAutomaticReconnect(true);
                opciones.setCleanSession(true);
                opciones.setKeepAliveInterval(60);

                cliente.setCallback(new MqttCallback() {
                    @Override
                    public void connectionLost(Throwable cause) {
                        Log.e("MQTT", "Conexión Perdida: " + cause.getMessage());
                        callback.alCambiarEstado(false);
                    }

                    @Override
                    public void messageArrived(String topic, MqttMessage message) {
                        String payload = new String(message.getPayload());
                        Log.d("MQTT", "Llegó mensaje en [" + topic + "]: " + payload);
                        callback.alRecibirMensaje(topic, payload);
                    }

                    @Override
                    public void deliveryComplete(IMqttDeliveryToken token) {}
                });

                Log.i("MQTT", "Intentando conectar a HiveMQ...");
                cliente.connect(opciones);
                Log.i("MQTT", "¡Conectado!");

                // Nos suscribimos inmediatamente para escuchar los comandos
                cliente.subscribe(TOPIC_WILDCARD, 0);

                callback.alCambiarEstado(true);

            } catch (Exception e) {
                e.printStackTrace();
                Log.e("MQTT", "ERROR CRÍTICO AL CONECTAR: " + e.getMessage());
                callback.alCambiarEstado(false);
            }
        }).start();
    }

    public void publicar(String topic, String mensaje) {
        new Thread(() -> {
            try {
                if (cliente != null && cliente.isConnected()) {
                    MqttMessage message = new MqttMessage(mensaje.getBytes(StandardCharsets.UTF_8));
                    cliente.publish(topic, message);
                    Log.i("MQTT", "Enviado: " + mensaje);
                } else {
                    Log.w("MQTT", "No se pudo enviar, cliente desconectado.");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}