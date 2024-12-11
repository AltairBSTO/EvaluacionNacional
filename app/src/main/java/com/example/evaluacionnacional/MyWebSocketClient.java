package com.example.evaluacionnacional;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import java.net.URI;
import android.util.Log;

public class MyWebSocketClient extends WebSocketClient {

    private static final String TAG = "WebSocket";

    public MyWebSocketClient(URI serverUri) {
        super(serverUri);
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        Log.d(TAG, "Conexión WebSocket abierta");
    }

    @Override
    public void onMessage(String message) {
        Log.d(TAG, "Mensaje recibido: " + message);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        Log.d(TAG, "Conexión cerrada: " + reason);
    }

    @Override
    public void onError(Exception ex) {
        Log.e(TAG, "Error en WebSocket: ", ex);
    }
}
