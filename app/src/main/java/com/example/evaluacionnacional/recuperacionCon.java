package com.example.evaluacionnacional;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class recuperacionCon extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private EditText emailEditText;
    private Button sendButton;
    private TextView backToLoginTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recuperacion_con);

        // Inicializar FirebaseAuth
        mAuth = FirebaseAuth.getInstance();

        // Asociar vistas
        emailEditText = findViewById(R.id.emailEditText);
        sendButton = findViewById(R.id.sendButton);
        backToLoginTextView = findViewById(R.id.backToLoginTextView);

        // Configurar el botón "Enviar"
        sendButton.setOnClickListener(v -> sendPasswordResetEmail());

        // Configurar el texto "Volver a iniciar sesión"
        backToLoginTextView.setOnClickListener(v -> {
            // Aquí puedes agregar la lógica para volver a la pantalla de inicio de sesión
            finish(); // Si prefieres simplemente cerrar esta actividad
        });
    }

    // Método para enviar el correo de restablecimiento de contraseña
    private void sendPasswordResetEmail() {
        String email = emailEditText.getText().toString().trim();

        // Validar el correo electrónico
        if (email.isEmpty()) {
            emailEditText.setError("Por favor ingresa un correo electrónico");
            return;
        }

        if (!email.contains("@")) {
            emailEditText.setError("Por favor ingresa un correo electrónico válido");
            return;
        }

        // Enviar el correo de restablecimiento de contraseña
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Mostrar mensaje de éxito
                        Toast.makeText(recuperacionCon.this, "Correo de restablecimiento enviado", Toast.LENGTH_SHORT).show();
                        finish(); // Puedes cerrar la actividad o redirigir al login
                    } else {
                        // Mostrar mensaje de error
                        Toast.makeText(recuperacionCon.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}
