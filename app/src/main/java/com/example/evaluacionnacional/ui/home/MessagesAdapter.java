package com.example.evaluacionnacional.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.evaluacionnacional.R;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.MessageViewHolder> {

    private List<Message> messages;

    // Constructor para recibir la lista de mensajes
    public MessagesAdapter(List<Message> messages) {
        this.messages = messages;
    }

    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Inflar el layout de cada item (mensaje)
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message, parent, false); // 'item_message' es el layout para cada mensaje
        return new MessageViewHolder(view);
    }


    @Override
    public void onBindViewHolder(MessageViewHolder holder, int position) {
        // Obtener el mensaje para esta posición
        Message message = messages.get(position);

        // Asignar los valores a los elementos de la vista
        holder.messageContent.setText(message.getContent()); // Cambiar getMessageContent() por getContent()
        holder.sender.setText(message.getSender());

        // Formatear y mostrar la hora del mensaje
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        holder.timestamp.setText(sdf.format(message.getTimestamp()));
    }


    @Override
    public int getItemCount() {
        return messages.size(); // Retorna el tamaño de la lista de mensajes
    }

    // ViewHolder que se usa para cada item del RecyclerView
    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageContent, sender, timestamp;

        public MessageViewHolder(View itemView) {
            super(itemView);
            // Inicializa las vistas
            messageContent = itemView.findViewById(R.id.textViewMessageContent);
            sender = itemView.findViewById(R.id.textViewSender);
            timestamp = itemView.findViewById(R.id.textViewTimestamp);
        }
    }
}
