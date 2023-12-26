package com.example.imagepro;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class LoginActivity extends AppCompatActivity {

    private EditText editTextEmail, editTextPassword;
    private Button buttonLogin;
    private TextView textViewRegister;
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        databaseHelper = new DatabaseHelper(this);

        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        textViewRegister = findViewById(R.id.register);

        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Récupérer les informations saisies
                String email = editTextEmail.getText().toString().trim();
                String password = editTextPassword.getText().toString().trim();

                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Vérifier l'authentification
                boolean authenticateUser = databaseHelper.getUser(email, password);
                if (authenticateUser) {
                    Toast.makeText(LoginActivity.this, "Authentification réussie", Toast.LENGTH_SHORT).show();
                Intent intent=new Intent(getApplicationContext(),AboutActivity.class);
                startActivity(intent);



                } else {
                    Toast.makeText(LoginActivity.this, "Authentification échouée", Toast.LENGTH_SHORT).show();
                }
            }
        });

        textViewRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Naviguer vers l'activité d'inscription
                Intent registerIntent = new Intent(LoginActivity.this, register.class);
                startActivity(registerIntent);
            }
        });
    }
}