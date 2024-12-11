package com.example.evaluacionnacional;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.evaluacionnacional.databinding.ActivityHomeloginBinding;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

public class Homelogin extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityHomeloginBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseStorage firebaseStorage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityHomeloginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarHomelogin.toolbar);



        // Configuración de la barra lateral (Navigation Drawer)
        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;

        // Configuración de la navegación con NavController y AppBarConfiguration
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow)
                .setOpenableLayout(drawer)
                .build();


        // Obtener el NavController para gestionar la navegación entre fragmentos
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_homelogin);

        // Configuración de la barra de navegación
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        // Inicializar FirebaseAuth y FirebaseStorage
        mAuth = FirebaseAuth.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();

        // Mostrar los datos del usuario en el NavigationView
        displayUserInfo(navigationView);

        // Agregar listener para manejar el "Cerrar sesión"
        navigationView.setNavigationItemSelectedListener(item -> {
            // Si el usuario selecciona "Cerrar sesión"
            if (item.getItemId() == R.id.nav_cerrarsesion) {
                signOut(); // Cerrar sesión
                return true;
            }
            // La navegación entre los fragmentos será gestionada automáticamente por NavController
            return NavigationUI.onNavDestinationSelected(item, navController);
        });
    }

    // Mostrar la información del usuario en el NavigationView
    private void displayUserInfo(NavigationView navigationView) {
        View headerView = navigationView.getHeaderView(0);
        TextView userNameTextView = headerView.findViewById(R.id.userNameTextView);
        TextView userEmailTextView = headerView.findViewById(R.id.userEmailTextView);
        ImageView userImageView = headerView.findViewById(R.id.userImageView);

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            // Mostrar el correo
            String email = user.getEmail();
            userEmailTextView.setText(email);

            // Mostrar el nombre de usuario si está disponible
            String displayName = user.getDisplayName();
            if (displayName != null && !displayName.isEmpty()) {
                userNameTextView.setText(displayName);
            } else {
                userNameTextView.setText("Usuario");
            }

            // Cargar la foto desde Firebase Storage usando el UID
            loadProfileImage(user.getUid(), userImageView);
        }
    }

    // Cargar la imagen del perfil desde Firebase Storage
    private void loadProfileImage(String uid, ImageView imageView) {
        if (uid == null) return;

        // Referencia a la foto en Firebase Storage usando el UID
        StorageReference profileImageRef = firebaseStorage.getReference()
                .child("profile_pics/" + uid + ".jpg");

        // Obtener URL de descarga y cargar la imagen con Picasso
        profileImageRef.getDownloadUrl().addOnSuccessListener(uri -> {
            // Usar Picasso para cargar la imagen
            Picasso.get()
                    .load(uri)    // Cargar la imagen desde la URI obtenida
                    .placeholder(R.drawable.usuario)  // Imagen mientras se carga
                    .error(R.drawable.usuario)       // Imagen si hay error
                    .into(imageView);  // Asignar la imagen al ImageView

        }).addOnFailureListener(e -> {
            // Mostrar un mensaje en caso de error
            Toast.makeText(this, "Error al cargar la imagen de perfil", Toast.LENGTH_SHORT).show();
        });
    }

    // Método para cerrar sesión
    private void signOut() {
        // Cerrar sesión del usuario
        FirebaseAuth.getInstance().signOut();

        // Redirigir al usuario a la pantalla de inicio de sesión (o cualquier otra actividad que desees)
        Intent intent = new Intent(Homelogin.this, MainActivity.class); // Cambia a tu actividad de login
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish(); // Finaliza la actividad actual
        Toast.makeText(Homelogin.this, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflar el menú de opciones
        getMenuInflater().inflate(R.menu.homelogin, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        // Gestionar la navegación cuando se presiona el botón de "Up"
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_homelogin);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration) || super.onSupportNavigateUp();
    }
}
