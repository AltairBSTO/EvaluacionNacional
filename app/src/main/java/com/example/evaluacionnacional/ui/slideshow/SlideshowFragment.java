package com.example.evaluacionnacional.ui.slideshow;

import com.example.evaluacionnacional.ui.home.Chat;
import com.example.evaluacionnacional.ui.home.Message;
import com.example.evaluacionnacional.ui.home.ChatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.evaluacionnacional.databinding.FragmentSlideshowBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

public class SlideshowFragment extends Fragment {

    private FragmentSlideshowBinding binding;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private RecyclerView recyclerView;
    private ContactAdapter adapter;
    private List<Contacto> contactList;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inicializar ViewBinding
        binding = FragmentSlideshowBinding.inflate(inflater, container, false);

        // Inicialización de Firebase y lista de contactos
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        contactList = new ArrayList<>();

        // Inicialización del RecyclerView
        recyclerView = binding.recyclerViewContacts;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Configuración del adaptador sin la opción de eliminar
        adapter = new ContactAdapter(contactList);

        // Manejar el clic en el contacto para abrir el chat
        adapter.setOnContactClickListener(contact -> openChat(contact));

        recyclerView.setAdapter(adapter);

        // Configurar el botón para verificar el correo
        binding.buttonAddContact.setOnClickListener(v -> checkIfEmailExists());

        // Cargar los contactos desde Firestore para el usuario autenticado
        loadContactsFromFirestore();

        return binding.getRoot(); // Retornar la vista raíz
    }

    // Método para verificar si el correo del contacto existe
    private void checkIfEmailExists() {
        String email = binding.editTextContactEmail.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(getContext(), "Por favor ingresa un correo.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Verificar si el correo existe en Firestore
        db.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Si el correo existe, agregar el contacto
                        for (DocumentSnapshot document : queryDocumentSnapshots) {
                            Contacto contact = document.toObject(Contacto.class);
                            if (contact != null) {
                                // Verificar si el contacto ya está guardado en Firestore
                                saveContactToFirestore(contact);
                            }
                        }
                    } else {
                        Toast.makeText(getContext(), "El correo no existe.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error al verificar el correo.", Toast.LENGTH_SHORT).show();
                });
    }

    // Método para guardar el contacto en Firestore
    private void saveContactToFirestore(Contacto contact) {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            // Verificar si el contacto ya existe en Firestore
            db.collection("contacts")
                    .document(user.getUid()) // UID del usuario autenticado
                    .collection("userContacts")
                    .whereEqualTo("email", contact.getEmail())
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (queryDocumentSnapshots.isEmpty()) {
                            // Agregar el contacto si no existe
                            db.collection("contacts")
                                    .document(user.getUid())
                                    .collection("userContacts")
                                    .add(contact)
                                    .addOnSuccessListener(documentReference -> {
                                        contactList.add(contact); // Añadir a la lista
                                        adapter.notifyDataSetChanged(); // Notificar al adaptador
                                        Toast.makeText(getContext(), "Contacto guardado correctamente.", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(getContext(), "Error al guardar el contacto.", Toast.LENGTH_SHORT).show();
                                    });
                        } else {
                            Toast.makeText(getContext(), "Este contacto ya está guardado.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Error al verificar el contacto.", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    // Método para abrir el chat con el contacto
    private void openChat(Contacto contact) {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            String currentUserEmail = user.getEmail();
            String contactEmail = contact.getEmail();

            // Crear un tópico único para el chat (tópico padre)
            String parentTopic = "chat/" + currentUserEmail + "/" + contactEmail;

            // Verificar si ya existe el chat con ese tópico en Firestore
            db.collection("chats")
                    .whereEqualTo("topic", parentTopic)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            // Si ya existe el chat, notificar al usuario
                            Toast.makeText(getContext(), "El chat ya existe.", Toast.LENGTH_SHORT).show();
                        } else {
                            // Si no existe, crear un nuevo chat (tópico padre)
                            createNewChat(parentTopic, contact);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Error al verificar el chat.", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    // Método para crear el tópico padre y permitir mensajes
    private void createNewChat(String parentTopic, Contacto contact) {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            String currentUserEmail = user.getEmail();
            String contactEmail = contact.getEmail();

            // Crear el objeto Chat para el tópico padre
            Chat newChat = new Chat(contact.getName(), parentTopic, currentUserEmail, contactEmail);

            // Crear el nuevo chat en Firestore (tópico padre)
            db.collection("chats")
                    .add(newChat)  // Añadimos el objeto Chat con todos los parámetros
                    .addOnSuccessListener(documentReference -> {
                        // Permitir que los tópicos individuales de ambos usuarios envíen mensajes al tópico padre
                        allowUserToSendMessage(currentUserEmail, contactEmail);

                        // Enviar un mensaje inicial indicando que el chat fue creado
                        sendInitialMessage(parentTopic, contact);  // Pasamos también el objeto contact

                        // Notificar al usuario que el chat ha sido creado
                        Toast.makeText(getContext(), "Nuevo chat creado.", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Error al crear el chat.", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    // Método para permitir que ambos usuarios envíen mensajes al tópico padre
    private void allowUserToSendMessage(String currentUserEmail, String contactEmail) {
        // Crear el documento para el tópico de usuario actual
        String userTopic = "chat/" + currentUserEmail + "/messages";
        db.collection("chats")
                .document(userTopic)
                .update("canSendMessage", true)  // Permitimos enviar mensajes
                .addOnSuccessListener(aVoid -> {
                    // Crear el documento para el tópico del contacto
                    String contactTopic = "chat/" + contactEmail + "/messages";
                    db.collection("chats")
                            .document(contactTopic)
                            .update("canSendMessage", true)  // Permitimos enviar mensajes
                            .addOnSuccessListener(aVoid1 -> {
                                // Los tópicos de ambos usuarios ahora pueden enviar mensajes
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(), "Error al actualizar el tópico del contacto.", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error al actualizar el tópico del usuario.", Toast.LENGTH_SHORT).show();
                });
    }

    // Método para enviar un mensaje inicial al chat cuando se crea
    private void sendInitialMessage(String parentTopic, Contacto contact) {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            String currentUserEmail = user.getEmail();
            String contactEmail = contact.getEmail(); // Usamos el objeto "contact" para obtener el correo del contacto
            String message = "Chat creado entre " + currentUserEmail + " y " + contactEmail;

            // Obtener el timestamp actual
            long timestamp = System.currentTimeMillis();

            // Crear el mensaje con el sender, message y timestamp
            Message initialMessage = new Message(currentUserEmail, message, timestamp);

            // Enviar el mensaje inicial al tópico
            db.collection("chats")
                    .document(parentTopic)
                    .collection("messages")
                    .add(initialMessage)
                    .addOnSuccessListener(documentReference -> {
                        // Mensaje inicial enviado correctamente
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Error al enviar el mensaje inicial.", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    // Método para cargar los contactos del usuario desde Firestore
    private void loadContactsFromFirestore() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            // Cargar contactos del usuario autenticado desde Firestore
            db.collection("contacts")
                    .document(user.getUid()) // Asegúrate de que este sea el UID correcto del usuario
                    .collection("userContacts")
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        contactList.clear(); // Limpiar la lista actual antes de cargar los nuevos contactos
                        for (DocumentSnapshot document : queryDocumentSnapshots) {
                            Contacto contact = document.toObject(Contacto.class);
                            if (contact != null) { // Verificar si el contacto no es nulo
                                contactList.add(contact); // Añadir el contacto a la lista
                            }
                        }
                        // Notificar al adaptador que se ha actualizado la lista
                        adapter.notifyDataSetChanged();
                        if (contactList.isEmpty()) {
                            Toast.makeText(getContext(), "No tienes contactos guardados.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Mostrar mensaje de error si no se puede cargar los contactos
                        Toast.makeText(getContext(), "Error al cargar los contactos.", Toast.LENGTH_SHORT).show();
                    });
        } else {
            // Mostrar un mensaje si no hay usuario autenticado
            Toast.makeText(getContext(), "No se pudo obtener el usuario.", Toast.LENGTH_SHORT).show();
        }
    }
}
