package com.example.supermercadoiot;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements InterfaceMqtt {

    private MqttManager mqttManager;
    private TextView lblEstado;
    private RecyclerView recycler;
    private ProductoAdapter adapter;
    private Gson gson = new Gson();

    // 1. MEMORIA DE LA CAJA (Lista local)
    private List<Producto> listaSimulada = new ArrayList<>();

    // Tópicos MQTT
    private static final String TOPIC_CARRITO = "supermercado/sucursal1/caja01/carrito";
    private static final String TOPIC_ESTADO = "supermercado/sucursal1/caja01/estado";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Configurar Interfaz (UI)
        lblEstado = findViewById(R.id.lblEstado);
        recycler = findViewById(R.id.recyclerProductos);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProductoAdapter();
        recycler.setAdapter(adapter);

        // 2. Iniciar MQTT
        mqttManager = new MqttManager(getApplicationContext(), this);
        mqttManager.conectar();

        // 3. Configurar Botones
        configurarBotones();
    }

    private void configurarBotones() {
        // --- BOTÓN 1: SIMULAR ESCANEO (AÑADIR) ---
        findViewById(R.id.btnAddTest).setOnClickListener(v -> {
            // Creamos un producto aleatorio
            int idAzar = (int)(Math.random() * 1000);
            int precioAzar = (int)(Math.random() * 5000) + 500;
            Producto nuevoProd = new Producto(String.valueOf(idAzar), "Producto #" + idAzar, precioAzar);

            // Agregamos a memoria y enviamos
            listaSimulada.add(nuevoProd);
            enviarListaActualizada();
        });

        // --- BOTÓN 2: BORRAR ÚLTIMO ---
        findViewById(R.id.btnDeleteLast).setOnClickListener(v -> {
            if (!listaSimulada.isEmpty()) {
                listaSimulada.remove(listaSimulada.size() - 1);
                enviarListaActualizada();
            } else {
                Toast.makeText(this, "Carrito vacío", Toast.LENGTH_SHORT).show();
            }
        });

        // --- BOTÓN 3: REINICIAR CAJA ---
        findViewById(R.id.btnReboot).setOnClickListener(v -> {
            mqttManager.publicar(TOPIC_ESTADO, "REINICIANDO");
            // Al reiniciar, vaciamos la lista
            listaSimulada.clear();
            enviarListaActualizada();
            Toast.makeText(this, "Reiniciando sistema...", Toast.LENGTH_SHORT).show();
        });

        // --- BOTÓN 4: APAGAR SISTEMA ---
        findViewById(R.id.btnShutdown).setOnClickListener(v -> {
            mqttManager.publicar(TOPIC_ESTADO, "CERRADO");
            Toast.makeText(this, "Cerrando caja...", Toast.LENGTH_SHORT).show();
        });

        // --- BOTÓN 5: ENCENDER CAJA (El Nuevo) ---
        // Asegúrate de haber agregado el botón con id btnPowerOn en el XML
        findViewById(R.id.btnPowerOn).setOnClickListener(v -> {
            mqttManager.publicar(TOPIC_ESTADO, "ONLINE");
            // Al encender, empezamos limpios
            listaSimulada.clear();
            enviarListaActualizada();
            Toast.makeText(this, "Iniciando sistema...", Toast.LENGTH_SHORT).show();
        });
    }

    // Método para convertir la lista a JSON y enviarla a MQTT
    private void enviarListaActualizada() {
        CarritoPayload payload = new CarritoPayload();
        payload.items = listaSimulada;

        String json = gson.toJson(payload);
        mqttManager.publicar(TOPIC_CARRITO, json);
    }

    // --- MÉTODOS QUE RESPONDEN A MQTT ---
    @Override
    public void alRecibirMensaje(String topic, String mensaje) {
        runOnUiThread(() -> {
            try {
                // CASO A: Llega lista de productos
                if (topic.equals(TOPIC_CARRITO)) {
                    CarritoPayload carrito = gson.fromJson(mensaje, CarritoPayload.class);
                    if (carrito != null && carrito.getItems() != null) {
                        listaSimulada = carrito.getItems(); // Sincronizamos memoria
                        adapter.actualizarLista(listaSimulada);
                    } else {
                        // Si llega null o vacío
                        listaSimulada = new ArrayList<>();
                        adapter.actualizarLista(listaSimulada);
                    }
                }

                // CASO B: Cambio de Estado (Luces)
                if (topic.equals(TOPIC_ESTADO)) {
                    switch (mensaje) {
                        case "REINICIANDO":
                            lblEstado.setText("REINICIANDO...");
                            lblEstado.setBackgroundColor(Color.parseColor("#FF9800")); // Naranja
                            break;
                        case "CERRADO":
                            lblEstado.setText("CAJA CERRADA");
                            lblEstado.setBackgroundColor(Color.parseColor("#D32F2F")); // Rojo
                            break;
                        case "ONLINE":
                            lblEstado.setText("ONLINE");
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
        runOnUiThread(() -> {
            if (conectado) {
                // Suscribirse a los tópicos de interés
                mqttManager.suscribirse("supermercado/sucursal1/caja01/#");

                lblEstado.setText("ONLINE");
                lblEstado.setBackgroundColor(Color.parseColor("#4CAF50"));
            } else {
                lblEstado.setText("DESCONECTADO");
                lblEstado.setBackgroundColor(Color.GRAY);
            }
        });
    }

    // Clase auxiliar para que Gson entienda el formato { "items": [...] }
    private static class CarritoPayload {
        List<Producto> items;
        public List<Producto> getItems() { return items; }
    }
}