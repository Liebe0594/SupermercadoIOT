package com.example.supermercadoiot;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements InterfaceMqtt {

    // --- LOS DOS MOTORES ---
    private FirebaseManager firebaseManager; // Para datos (Carrito)
    private MqttManager mqttManager;         // Para comandos (Sistema)

    private TextView lblEstado;
    private RecyclerView recycler;
    private ProductoAdapter adapter;
    private Gson gson = new Gson();
    private List<Producto> listaSimulada = new ArrayList<>();

    // RUTAS Y TÓPICOS
    // Firebase usa nombres cortos (nodos)
    private static final String NODO_CARRITO = "carrito";
    private static final String NODO_ESTADO = "estado";

    // MQTT usa rutas largas
    private static final String TOPIC_SISTEMA = "supermercado/sucursal1/caja01/sistema";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lblEstado = findViewById(R.id.lblEstado);
        recycler = findViewById(R.id.recyclerProductos);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProductoAdapter();
        recycler.setAdapter(adapter);

        // 1. INICIAR FIREBASE
        firebaseManager = new FirebaseManager(this);
        firebaseManager.conectar();

        // 2. INICIAR MQTT (HiveMQ)
        mqttManager = new MqttManager(getApplicationContext(), this);
        mqttManager.conectar();

        configurarBotones();
    }

    private void configurarBotones() {
        // --- BOTONES DE PRODUCTOS (Usan FIREBASE) ---
        findViewById(R.id.btnAddTest).setOnClickListener(v -> {
            int idAzar = (int)(Math.random() * 1000);
            Producto p = new Producto(String.valueOf(idAzar), "Prod Híbrido #" + idAzar, 1500);
            listaSimulada.add(p);
            guardarEnFirebase();
        });

        findViewById(R.id.btnDeleteLast).setOnClickListener(v -> {
            if (!listaSimulada.isEmpty()) {
                listaSimulada.remove(listaSimulada.size() - 1);
                guardarEnFirebase();
            }
        });

        // --- BOTONES DE SISTEMA (Usan MQTT - HiveMQ) ---

        // REINICIAR
        findViewById(R.id.btnReboot).setOnClickListener(v -> {
            // Enviamos señal rápida por MQTT
            mqttManager.publicar(TOPIC_SISTEMA, "REBOOT");
            // Limpiamos la base de datos localmente
            listaSimulada.clear();
            guardarEnFirebase();
            Toast.makeText(this, "Enviando REBOOT a HiveMQ...", Toast.LENGTH_SHORT).show();
        });

        // APAGAR
        findViewById(R.id.btnShutdown).setOnClickListener(v -> {
            mqttManager.publicar(TOPIC_SISTEMA, "SHUTDOWN");
        });

        // ENCENDER
        findViewById(R.id.btnPowerOn).setOnClickListener(v -> {
            mqttManager.publicar(TOPIC_SISTEMA, "ONLINE");
        });
    }

    private void guardarEnFirebase() {
        CarritoPayload payload = new CarritoPayload();
        payload.items = listaSimulada;
        firebaseManager.guardar(NODO_CARRITO, payload);
    }

    // --- ESCUCHA CENTRALIZADA (AQUI LLEGA TODO) ---
    @Override
    public void alRecibirMensaje(String source, String mensaje) {
        runOnUiThread(() -> {
            try {
                // A. ¿ES DEL CARRITO (FIREBASE)?
                if (source.equals(NODO_CARRITO)) {
                    CarritoPayload carrito = gson.fromJson(mensaje, CarritoPayload.class);
                    if (carrito != null && carrito.getItems() != null) {
                        listaSimulada = carrito.getItems();
                        adapter.actualizarLista(listaSimulada);
                    } else {
                        listaSimulada = new ArrayList<>();
                        adapter.actualizarLista(listaSimulada);
                    }
                }

                // B. ¿ES UN COMANDO DE SISTEMA (MQTT O FIREBASE)?
                // Verificamos si el tópico contiene la palabra "sistema" o si viene del nodo "estado"
                if (source.contains("sistema") || source.equals(NODO_ESTADO)) {
                    Log.i("HYBRID", "Comando recibido: " + mensaje);
                    switch (mensaje) {
                        case "REBOOT":
                            lblEstado.setText("REINICIANDO...");
                            lblEstado.setBackgroundColor(Color.parseColor("#FF9800")); // Naranja
                            break;
                        case "SHUTDOWN":
                            lblEstado.setText("APAGADO (MQTT)");
                            lblEstado.setBackgroundColor(Color.parseColor("#D32F2F")); // Rojo
                            break;
                        case "ONLINE":
                            lblEstado.setText("SISTEMA ONLINE");
                            lblEstado.setBackgroundColor(Color.parseColor("#4CAF50")); // Verde
                            break;
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void alCambiarEstado(boolean conectado) {
        // Este método lo llaman ambos managers, así que solo confirmamos visualmente
        runOnUiThread(() -> {
            if(conectado) {
                // Si al menos uno conecta, mostramos verde.
                // Idealmente podríamos tener dos indicadores, pero esto basta por ahora.
                lblEstado.setText("CONECTADO (HÍBRIDO)");
                lblEstado.setBackgroundColor(Color.parseColor("#4CAF50"));
            }
        });
    }

    private static class CarritoPayload {
        List<Producto> items;
        public List<Producto> getItems() { return items; }
    }
}