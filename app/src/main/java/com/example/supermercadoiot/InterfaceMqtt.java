package com.example.supermercadoiot;

public interface InterfaceMqtt {

    void alCambiarEstado(boolean conectado);


    void alRecibirMensaje(String topic, String mensaje);
}