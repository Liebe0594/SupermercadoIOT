package com.example.supermercadoiot;

import androidx.annotation.NonNull;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

public class FirebaseManager {

    private DatabaseReference dbRef;
    private InterfaceMqtt callback; // Usamos tu misma interfaz para avisar al Main
    private Gson gson = new Gson();

    // Ruta donde guardaremos todo: supermercado -> sucursal1 -> caja01
    private static final String RUTA_RAIZ = "supermercado/sucursal1/caja01";

    public FirebaseManager(InterfaceMqtt callback) {
        this.callback = callback;
        // Obtenemos referencia a la base de datos
        dbRef = FirebaseDatabase.getInstance().getReference(RUTA_RAIZ);
    }

    // Escuchar cambios en tiempo real
    public void conectar() {
        // Escuchar cambios en el CARRITO
        dbRef.child("carrito").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Firebase devuelve los datos como Objeto.
                // Los convertimos a JSON para pasárselo a tu MainActivity tal cual como lo hacía MQTT.
                Object valor = snapshot.getValue();
                if (valor != null) {
                    String json = gson.toJson(valor);
                    // Avisamos al Main: "Llegó data al tópico 'carrito'"
                    callback.alRecibirMensaje("carrito", json);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Escuchar cambios en el ESTADO
        dbRef.child("estado").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String estado = snapshot.getValue(String.class);
                if (estado != null) {
                    callback.alRecibirMensaje("estado", estado);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Confirmamos conexión visualmente
        callback.alCambiarEstado(true);
    }

    // Método para guardar datos (Equivalente a publicar)
    public void guardar(String subnodo, Object datos) {
        dbRef.child(subnodo).setValue(datos);
    }
}