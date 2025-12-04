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

    // -----------------------------------------------------------
    // CONFIGURACIÓN DE HIVEMQ CLOUD (¡PON TUS DATOS AQUÍ!)
    // -----------------------------------------------------------
    private static final String SERVER_URI = "ssl://ff8c68175d2c46d29a6021a7123f122d.s1.eu.hivemq.cloud:8883";
    private static final String USERNAME = "JoseNeira";
    private static final String PASSWORD = "Jose123.";
    // -----------------------------------------------------------

    public MqttManager(Context context, InterfaceMqtt callback) {
        this.callback = callback;
    }

    public void conectar() {
        new Thread(() -> {
            try {
                String clientId = "Android_" + System.currentTimeMillis();
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
                        Log.e("MQTT", "Conexión perdida");
                        callback.alCambiarEstado(false);
                    }

                    @Override
                    public void messageArrived(String topic, MqttMessage message) {
                        String payload = new String(message.getPayload());
                        Log.d("MQTT", "Mensaje: " + payload);
                        callback.alRecibirMensaje(topic, payload);
                    }

                    @Override
                    public void deliveryComplete(IMqttDeliveryToken token) {}
                });

                cliente.connect(opciones);
                Log.i("MQTT", "¡Conectado!");
                callback.alCambiarEstado(true);

                // Suscripción automática inicial
                suscribirse("supermercado/sucursal1/caja01/#");

            } catch (Exception e) {
                e.printStackTrace();
                Log.e("MQTT", "Error conectando: " + e.getMessage());
                callback.alCambiarEstado(false);
            }
        }).start();
    }

    // --- ESTE ES EL MÉTODO QUE TE FALTABA ---
    public void suscribirse(String topic) {
        new Thread(() -> {
            try {
                if (cliente != null && cliente.isConnected()) {
                    cliente.subscribe(topic, 0);
                    Log.i("MQTT", "Suscrito a: " + topic);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void publicar(String topic, String mensajeJSON) {
        new Thread(() -> {
            try {
                if (cliente != null && cliente.isConnected()) {
                    MqttMessage message = new MqttMessage(mensajeJSON.getBytes(StandardCharsets.UTF_8));
                    cliente.publish(topic, message);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}