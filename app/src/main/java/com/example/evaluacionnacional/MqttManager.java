package com.example.evaluacionnacional;

import android.content.Context;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

public class MqttManager {

    private static final String TAG = "MqttManager";
    private MqttClient client;
    private String brokerUrl = "tcp://broker.hivemq.com:1883"; // Broker MQTT
    private String clientId = MqttClient.generateClientId();
    private MqttConnectOptions options;
    private Context context;

    private Set<String> receivedMessageIds = new HashSet<>(); // Para evitar procesar mensajes duplicados
    private boolean isSubscribed = false; // Para controlar las suscripciones

    public MqttManager(Context context) {
        this.context = context;
    }

    // Método para conectar al broker MQTT
    public void connect() {
        try {
            client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());
            options = new MqttConnectOptions();
            options.setCleanSession(false); // No usar sesión limpia
            options.setConnectionTimeout(10); // Tiempo de espera para la conexión (en segundos)
            options.setKeepAliveInterval(60); // Intervalo para mantener la conexión viva (en segundos)

            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    Log.d(TAG, "Conexión perdida: " + cause.getMessage());
                    // Si se pierde la conexión, puedes intentar reconectar
                    reconnect();
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    String msgContent = new String(message.getPayload());
                    String messageId = generateMessageId(msgContent); // Generamos un ID único por mensaje

                    if (!receivedMessageIds.contains(messageId)) {
                        // Si el mensaje no ha sido recibido antes, agregarlo a la lista de procesados
                        receivedMessageIds.add(messageId);
                        Log.d(TAG, "Mensaje recibido: " + msgContent);
                        // Aquí puedes procesar el mensaje (por ejemplo, actualizar la interfaz)
                    } else {
                        Log.d(TAG, "Mensaje duplicado ignorado: " + msgContent);
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    Log.d(TAG, "Mensaje entregado con éxito: " + token.getMessageId());
                }
            });

            client.connect(options);  // Intentar conectar al broker
            Log.d(TAG, "Conectado al broker MQTT");

        } catch (Exception e) {
            Log.e(TAG, "Error al conectar con el broker MQTT", e);
        }
    }

    // Método para desconectar del broker MQTT
    public void disconnect() {
        try {
            if (client != null && client.isConnected()) {
                client.disconnect();
                Log.d(TAG, "Desconectado del broker");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al desconectar del broker", e);
        }
    }

    // Método para suscribirse a un tópico (solo si no estamos suscritos previamente)
    public void subscribeToTopic(String topic) {
        try {
            if (client != null && client.isConnected() && !isSubscribed) {
                client.subscribe(topic);
                isSubscribed = true; // Marcamos como suscrito
                Log.d(TAG, "Suscrito al tópico: " + topic);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al suscribirse al tópico " + topic, e);
        }
    }

    // Método para publicar un mensaje
    public void publishMessage(String topic, String messageContent, String senderEmail, String timestamp) {
        try {
            if (client != null && client.isConnected()) {  // Verificar si el cliente está conectado
                JSONObject message = new JSONObject();
                message.put("message", messageContent);
                message.put("sender", senderEmail);
                message.put("timestamp", timestamp);

                MqttMessage mqttMessage = new MqttMessage();
                mqttMessage.setPayload(message.toString().getBytes());

                Log.d(TAG, "Publicando mensaje: " + message.toString());
                client.publish(topic, mqttMessage);  // Publicar mensaje
            } else {
                Log.e(TAG, "El cliente no está conectado. Intentando reconectar...");
                reconnect();  // Intentar reconectar
                publishMessage(topic, messageContent, senderEmail, timestamp);  // Reintentar publicar el mensaje
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al enviar el mensaje", e);
        }
    }

    // Método para generar un ID único para cada mensaje (usando el contenido y la fecha)
    private String generateMessageId(String messageContent) {
        return Integer.toHexString(messageContent.hashCode());  // Generar un ID basado en el hash del contenido
    }

    // Método para intentar reconectar automáticamente si la conexión se pierde
    private void reconnect() {
        if (client != null && !client.isConnected()) {
            try {
                Log.d(TAG, "Intentando reconectar...");
                client.connect(options);  // Intentar reconectar al broker
                Log.d(TAG, "Reconectado al broker MQTT");
            } catch (Exception e) {
                Log.e(TAG, "Error al reconectar con el broker MQTT", e);
            }
        }
    }

    // Método para configurar el callback que maneja los mensajes entrantes
    public void setCallback(MqttCallback callback) {
        if (client != null) {
            client.setCallback(callback);
        }
    }
}
