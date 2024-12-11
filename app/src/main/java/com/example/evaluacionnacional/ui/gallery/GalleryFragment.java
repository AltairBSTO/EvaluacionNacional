package com.example.evaluacionnacional.ui.gallery;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.evaluacionnacional.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

public class GalleryFragment extends Fragment {

    private FirebaseAuth mAuth;
    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri selectedImageUri;
    private FirebaseStorage storage;
    private StorageReference storageReference;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        // Inflar el layout para este fragmento
        View root = inflater.inflate(R.layout.fragment_gallery, container, false);

        // Inicializar FirebaseAuth y FirebaseStorage
        mAuth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        // Obtener las vistas del layout
        EditText userNameEditText = root.findViewById(R.id.userNameEditText);
        EditText userEmailEditText = root.findViewById(R.id.userEmailEditText);
        ImageView profileImageView = root.findViewById(R.id.profileImageView);
        Button confirmChangesButton = root.findViewById(R.id.confirmChangesButton);
        Button selectPhotoButton = root.findViewById(R.id.selectPhotoButton); // Botón para cambiar foto

        // Obtener el usuario autenticado
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            // Mostrar nombre de usuario en el EditText
            String displayName = user.getDisplayName();
            if (displayName != null && !displayName.isEmpty()) {
                userNameEditText.setText(displayName);
            } else {
                userNameEditText.setText("Nombre de Usuario");
            }

            // Mostrar correo electrónico en el EditText
            String email = user.getEmail();
            userEmailEditText.setText(email);

            // Cargar la foto de perfil
            String photoUrl = user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : null;
            if (photoUrl != null) {
                // Usar Picasso para cargar la foto
                Picasso.get()
                        .load(photoUrl)
                        .placeholder(R.drawable.usuario)  // Imagen por defecto mientras carga
                        .error(R.drawable.usuario)  // Imagen en caso de error
                        .into(profileImageView);
            } else {
                // Imagen predeterminada si no hay foto
                profileImageView.setImageResource(R.drawable.usuario);
            }

            // Configurar el botón para confirmar cambios
            confirmChangesButton.setOnClickListener(v -> {
                // Obtener los valores de los campos EditText
                String newName = userNameEditText.getText().toString();
                String newEmail = userEmailEditText.getText().toString();

                // Lógica para confirmar los cambios de datos del perfil
                confirmChanges(user, newName, newEmail);
            });

            // Configurar el botón para seleccionar una nueva foto de perfil
            selectPhotoButton.setOnClickListener(v -> {
                // Intent para abrir la galería y seleccionar una imagen
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, PICK_IMAGE_REQUEST);
            });
        } else {
            // Si el usuario no está autenticado, mostrar mensaje de error
            Toast.makeText(getActivity(), "No hay usuario autenticado", Toast.LENGTH_SHORT).show();
        }

        return root;
    }

    // Método para confirmar los cambios de datos del perfil
    private void confirmChanges(FirebaseUser user, String newName, String newEmail) {
        // Actualizar nombre de usuario
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(newName)
                .build();

        user.updateProfile(profileUpdates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Nombre actualizado correctamente
                        Toast.makeText(getActivity(), "Nombre actualizado", Toast.LENGTH_SHORT).show();
                    }
                });

        // Actualizar correo electrónico
        user.updateEmail(newEmail).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Correo actualizado correctamente
                Toast.makeText(getActivity(), "Correo actualizado", Toast.LENGTH_SHORT).show();
            } else {
                // Error al actualizar el correo
                Toast.makeText(getActivity(), "Error al actualizar correo", Toast.LENGTH_SHORT).show();
            }
        });

        // Si hay una nueva foto seleccionada
        if (selectedImageUri != null) {
            uploadImageToFirebase(user, selectedImageUri);
        }
    }

    // Método para subir la imagen de perfil a Firebase Storage
    private void uploadImageToFirebase(FirebaseUser user, Uri imageUri) {
        // Usa el UID del usuario para crear una ruta única para la imagen
        StorageReference fileReference = storageReference.child("profile_pics/" + user.getUid() + ".jpg");

        fileReference.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> fileReference.getDownloadUrl().addOnSuccessListener(uri -> {
                    // Actualiza el perfil del usuario con la URL de la imagen
                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                            .setPhotoUri(uri)
                            .build();

                    user.updateProfile(profileUpdates).addOnCompleteListener(profileTask -> {
                        if (profileTask.isSuccessful()) {
                            Toast.makeText(getActivity(), "Foto de perfil actualizada", Toast.LENGTH_SHORT).show();
                        }
                    });
                }))
                .addOnFailureListener(e -> Toast.makeText(getActivity(), "Error al subir la imagen", Toast.LENGTH_SHORT).show());
    }

    // Manejar la selección de imagen desde la galería
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == getActivity().RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            ImageView profileImageView = getView().findViewById(R.id.profileImageView);
            profileImageView.setImageURI(selectedImageUri); // Mostrar la imagen seleccionada
        }
    }
}

