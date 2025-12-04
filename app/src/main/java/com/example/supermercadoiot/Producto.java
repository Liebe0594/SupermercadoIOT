package com.example.supermercadoiot; // Tu paquete

public class Producto {
    private String id;
    private String nombre;
    private int precio;

    // Constructor vacío necesario para Gson
    public Producto() {}

    public Producto(String id, String nombre, int precio) {
        this.id = id;
        this.nombre = nombre;
        this.precio = precio;
    }

    // Getters
    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public int getPrecio() { return precio; }

    // Para mostrarlo fácil en texto (debugging)
    @Override
    public String toString() {
        return nombre + " ($" + precio + ")";
    }
}