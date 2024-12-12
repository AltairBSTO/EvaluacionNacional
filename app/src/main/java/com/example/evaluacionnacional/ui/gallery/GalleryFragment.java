package com.example.evaluacionnacional.ui.gallery;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.evaluacionnacional.R;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
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

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflar el layout para este fragmento
        View root = inflater.inflate(R.layout.fragment_gallery, container, false);

        // Inicializar FirebaseAuth
        mAuth = FirebaseAuth.getInstance();

        // Inicializar Firebase Storage
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        // Obtener las vistas del layout
        EditText currentPasswordEditText = root.findViewById(R.id.currentPasswordEditText);
        EditText newPasswordEditText = root.findViewById(R.id.newPasswordEditText);
        Button confirmChangesButton = root.findViewById(R.id.confirmChangesButton);
        Button uploadImageButton = root.findViewById(R.id.selectPhotoButton); // Botón para seleccionar imagen
        ImageView profileImageView = root.findViewById(R.id.profileImageView); // ImageView para mostrar la imagen

        // Obtener el usuario autenticado
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            // Configurar el botón para cambiar la contraseña
            confirmChangesButton.setOnClickListener(v -> {
                // Obtener la contraseña actual y la nueva contraseña
                String currentPassword = currentPasswordEditText.getText().toString().trim();
                String newPassword = newPasswordEditText.getText().toString().trim();

                if (currentPassword.isEmpty()) {
                    Toast.makeText(getActivity(), "Por favor, ingresa tu contraseña actual", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (newPassword.isEmpty()) {
                    Toast.makeText(getActivity(), "Por favor, ingresa una nueva contraseña", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Llamar a la función para reautenticar y cambiar la contraseña
                reauthenticateUser(currentPassword, newPassword, user);
            });

            // Configurar el botón para subir una imagen
            uploadImageButton.setOnClickListener(v -> {
                openImagePicker(); // Método para abrir el selector de imágenes
            });
        }

        return root;
    }

    // Método para reautenticar al usuario con su contraseña actual
    private void reauthenticateUser(String currentPassword, String newPassword, FirebaseUser user) {
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);

        user.reauthenticate(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Llamar a la función para cambiar la contraseña
                        changePassword(user, newPassword);
                    } else {
                        Toast.makeText(getActivity(), "Reautenticación fallida", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Método para cambiar la contraseña
    private void changePassword(FirebaseUser user, String newPassword) {
        user.updatePassword(newPassword)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Contraseña actualizada correctamente
                        Toast.makeText(getActivity(), "Contraseña actualizada con éxito", Toast.LENGTH_SHORT).show();
                    } else {
                        // Error al cambiar la contraseña
                        Toast.makeText(getActivity(), "Error al actualizar la contraseña", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Método para abrir el selector de imágenes
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
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

            // Subir la imagen seleccionada
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                uploadImageToFirebase(user, selectedImageUri);
            }
        }
    }
}
