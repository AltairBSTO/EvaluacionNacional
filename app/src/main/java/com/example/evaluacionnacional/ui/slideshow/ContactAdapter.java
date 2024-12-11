package com.example.evaluacionnacional.ui.slideshow;



import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.example.evaluacionnacional.R;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.List;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ContactViewHolder> {

    private List<Contacto> contactList;
    private OnContactClickListener listener;

    // Constructor del adaptador
    public ContactAdapter(List<Contacto> contactList) {
        this.contactList = contactList;
    }

    // Método para configurar el listener para los clics
    public void setOnContactClickListener(OnContactClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflar la vista de un contacto
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_content, parent, false);
        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        Contacto contact = contactList.get(position);
        holder.bind(contact);
    }

    @Override
    public int getItemCount() {
        return contactList.size();
    }

    // ViewHolder para los contactos
    public class ContactViewHolder extends RecyclerView.ViewHolder {
        private TextView nameTextView;
        private TextView emailTextView;
        private ImageView photoImageView;

        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.contactName); // Asegúrate de tener este TextView en tu item_layout.xml
            emailTextView = itemView.findViewById(R.id.contactEmail); // Asegúrate de tener este TextView
            photoImageView = itemView.findViewById(R.id.contactPhoto); // Asegúrate de tener este ImageView

            // Configuración del clic en el ítem
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onContactClick(contactList.get(getAdapterPosition()));
                }
            });
        }

        // Vincula los datos del contacto
        public void bind(Contacto contact) {
            nameTextView.setText(contact.getName());
            emailTextView.setText(contact.getEmail());

            // Cargar la imagen con Picasso
            if (contact.getPhotoUrl() != null && !contact.getPhotoUrl().isEmpty()) {
                Picasso.get()
                        .load(contact.getPhotoUrl())
                        .placeholder(R.drawable.ic_launcher_foreground) // Imagen por defecto en caso de que no haya URL
                        .into(photoImageView);
            } else {
                photoImageView.setImageResource(R.drawable.ic_launcher_foreground); // Imagen por defecto si no hay URL
            }
        }
    }

    // Interfaz para el listener del clic
    public interface OnContactClickListener {
        void onContactClick(Contacto contact);
    }
}
