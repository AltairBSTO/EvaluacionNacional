package com.example.evaluacionnacional.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.evaluacionnacional.MqttManager;
import com.example.evaluacionnacional.R;
import com.google.firebase.auth.FirebaseAuth;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerViewChats;
    private EditText editTextMessage;
    private Button buttonSend;
    private ChatAdapter chatAdapter;
    private List<ChatModel> chatList;
    private MqttManager mqttManager;
    private FirebaseAuth auth;

    // Un conjunto para almacenar los IDs de los mensajes procesados
    private Set<String> processedMessageIds = new HashSet<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerViewChats = view.findViewById(R.id.recyclerViewChats);
        recyclerViewChats.setLayoutManager(new LinearLayoutManager(getContext()));

        editTextMessage = view.findViewById(R.id.editTextMessage);
        buttonSend = view.findViewById(R.id.buttonSend);

        chatList = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatList);
        recyclerViewChats.setAdapter(chatAdapter);

        auth = FirebaseAuth.getInstance();
        mqttManager = new MqttManager(getContext());

        // Establece la conexión MQTT
        mqttManager.connect();
        mqttManager.setCallback(new MqttCallback() {
            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                onMqttMessageReceived(message.toString());
            }

            @Override
            public void connectionLost(Throwable cause) {
                // Manejar la reconexión si se pierde la conexión
                reconnectToMqtt();
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                // Aquí podrías manejar el evento de confirmación de entrega si lo necesitas
            }
        });

        mqttManager.subscribeToTopic("Topico/General");

        buttonSend.setOnClickListener(v -> sendMessage());
    }

    // Método para enviar un mensaje
    private void sendMessage() {
        String messageContent = editTextMessage.getText().toString().trim();
        if (messageContent.isEmpty()) {
            Toast.makeText(getContext(), "Escribe un mensaje", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserEmail = auth.getCurrentUser() != null ? auth.getCurrentUser().getEmail() : "unknown";
        String timestamp = String.valueOf(System.currentTimeMillis());

        // Crear un ID único para el mensaje
        String messageId = generateMessageId(messageContent, currentUserEmail, timestamp);

        // Verificar si el mensaje ya existe en el conjunto de mensajes procesados
        if (!processedMessageIds.contains(messageId)) {
            // Crear el mensaje en formato JSON
            try {
                JSONObject messageJson = new JSONObject();
                messageJson.put("message", messageContent);
                messageJson.put("sender", currentUserEmail);
                messageJson.put("timestamp", timestamp);

                // Enviar el mensaje a través de MQTT
                mqttManager.publishMessage("Topico/General", messageContent, currentUserEmail, timestamp);

                // Agregar el mensaje a la lista local
                ChatModel chat = new ChatModel(messageContent, currentUserEmail, timestamp);
                chatList.add(chat);

                // Notificar al adaptador que la lista ha cambiado
                chatAdapter.notifyItemInserted(chatList.size() - 1);

                // Desplazar el RecyclerView al último mensaje
                recyclerViewChats.scrollToPosition(chatList.size() - 1);

                // Limpiar el campo de texto
                editTextMessage.setText("");

                // Añadir el ID del mensaje al conjunto de mensajes procesados
                processedMessageIds.add(messageId);
            } catch (JSONException e) {
                e.printStackTrace();
                Toast.makeText(getContext(), "Error al enviar el mensaje", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getContext(), "Este mensaje ya fue enviado", Toast.LENGTH_SHORT).show();
        }
    }

    // Generar un ID único para el mensaje basado en el contenido, el emisor y el timestamp
    private String generateMessageId(String messageContent, String senderEmail, String timestamp) {
        return Integer.toHexString((messageContent + senderEmail + timestamp).hashCode());
    }

    // Manejar el mensaje recibido
    private void onMqttMessageReceived(String messageContent) {
        try {
            // Procesar el mensaje JSON
            JSONObject jsonMessage = new JSONObject(messageContent);
            String senderEmail = jsonMessage.getString("sender");
            String message = jsonMessage.getString("message");
            String timestamp = jsonMessage.getString("timestamp");

            // Crear un ID único para el mensaje recibido
            String messageId = generateMessageId(message, senderEmail, timestamp);

            // Verificar si el mensaje ya está en la lista
            if (!processedMessageIds.contains(messageId)) {
                // Agregar el mensaje recibido a la lista de chats
                ChatModel chat = new ChatModel(message, senderEmail, timestamp);

                // Actualizar la interfaz de usuario en el hilo principal
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        chatList.add(chat);
                        chatAdapter.notifyItemInserted(chatList.size() - 1);
                        recyclerViewChats.scrollToPosition(chatList.size() - 1);
                    }
                });

                // Añadir el ID del mensaje al conjunto de mensajes procesados
                processedMessageIds.add(messageId);
            }

        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error al procesar el mensaje", Toast.LENGTH_SHORT).show();
        }
    }

    // Método para manejar la reconexión MQTT
    private void reconnectToMqtt() {
        Toast.makeText(getContext(), "Conexión perdida, intentando reconectar...", Toast.LENGTH_SHORT).show();
        mqttManager.connect();  // Reconectar automáticamente
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mqttManager.disconnect();  // Desconectar al salir del fragmento
    }
}
