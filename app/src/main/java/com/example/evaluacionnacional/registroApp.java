package com.example.evaluacionnacional;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class registroApp extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int MAX_IMAGE_SIZE = 1024 * 1024; // 1 MB

    private ImageView profileImageView;
    private Uri profileImageUri;

    private EditText emailEditText, passwordEditText, confirmPasswordEditText, usernameEditText;
    private Button registerButton, selectImageButton;

    private FirebaseAuth mAuth;
    private StorageReference storageReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro_app);

        mAuth = FirebaseAuth.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference();

        profileImageView = findViewById(R.id.profileImageView);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        usernameEditText = findViewById(R.id.nameEditText);
        registerButton = findViewById(R.id.registerButton);
        selectImageButton = findViewById(R.id.selectImageButton);

        selectImageButton.setOnClickListener(v -> openGallery());
        registerButton.setOnClickListener(v -> registerUser());
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            profileImageUri = data.getData();

            // Validar tipo y tamaño de la imagen
            try {
                InputStream inputStream = getContentResolver().openInputStream(profileImageUri);
                if (inputStream != null) {
                    int fileSize = inputStream.available();
                    inputStream.close();

                    String mimeType = getContentResolver().getType(profileImageUri);
                    if (!"image/jpeg".equals(mimeType)) {
                        Toast.makeText(this, "La imagen debe ser JPG", Toast.LENGTH_SHORT).show();
                        profileImageUri = null;
                        return;
                    }

                    if (fileSize > MAX_IMAGE_SIZE) {
                        Toast.makeText(this, "La imagen debe ser menor a 1 MB", Toast.LENGTH_SHORT).show();
                        profileImageUri = null;
                        return;
                    }

                    profileImageView.setImageURI(profileImageUri);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Error al procesar la imagen", Toast.LENGTH_SHORT).show();
                profileImageUri = null;
            }
        }
    }

    private void registerUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();
        String username = usernameEditText.getText().toString().trim();

        // Validaciones de campos
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches() || !email.endsWith("@gmail.com")) {
            emailEditText.setError("Ingresa un correo válido de Gmail");
            return;
        }

        if (TextUtils.isEmpty(password) || !password.matches("^(?=.*[A-Z])(?=.*[@#$%^&+=!]).{6,}$")) {
            passwordEditText.setError("La contraseña debe tener al menos 6 caracteres, una mayúscula y un carácter especial");
            return;
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordEditText.setError("Las contraseñas no coinciden");
            return;
        }

        if (TextUtils.isEmpty(username)) {
            usernameEditText.setError("Ingresa un nombre de usuario");
            return;
        }

        if (profileImageUri == null) {
            Toast.makeText(this, "Selecciona una foto de perfil válida", Toast.LENGTH_SHORT).show();
            return;
        }

        // Registrar usuario en Firebase Authentication
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        uploadImageToFirebase(username);
                    } else {
                        Toast.makeText(this, "Error en el registro: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void uploadImageToFirebase(String username) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            // Usa el UID del usuario para crear una ruta única para la imagen
            StorageReference fileReference = storageReference.child("profile_pics/" + user.getUid() + ".jpg");

            fileReference.putFile(profileImageUri)
                    .addOnSuccessListener(taskSnapshot -> fileReference.getDownloadUrl().addOnSuccessListener(uri -> {
                        // Actualiza el perfil del usuario con la URL de la imagen
                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                .setDisplayName(username)
                                .setPhotoUri(uri)
                                .build();

                        user.updateProfile(profileUpdates).addOnCompleteListener(profileTask -> {
                            if (profileTask.isSuccessful()) {
                                saveUserDataToFirestore(user.getUid(), username, user.getEmail(), uri.toString());
                            }
                        });
                    }))
                    .addOnFailureListener(e -> Toast.makeText(this, "Error al subir la imagen", Toast.LENGTH_SHORT).show());
        }
    }

    private void saveUserDataToFirestore(String uid, String username, String email, String photoUrl) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Generar un tópico único basado en el hash del correo
        String userTopic = "chat/user_" + email.hashCode();

        // Crear un mapa con los datos del usuario
        Map<String, Object> userData = new HashMap<>();
        userData.put("username", username);
        userData.put("email", email);
        userData.put("photoUrl", photoUrl);
        userData.put("topic", userTopic); // Agregar el tópico generado

        // Guardar los datos en la colección 'users' usando el UID como ID del documento
        db.collection("users").document(uid)
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Usuario registrado con éxito", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al guardar los datos en Firestore: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }
}
