package com.example.imagepro;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class register extends AppCompatActivity {
    EditText editTextEmail,editTextPassword,editTextConfirmPassword;
    TextView textViewLogin;
    Button buttonRegister;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);
        buttonRegister = findViewById(R.id.buttonRegister);
        textViewLogin = findViewById(R.id.loginediText);

        buttonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Récupérer les informations saisies
                String email = editTextEmail.getText().toString();
                String password = editTextPassword.getText().toString();
                String confirmPassword = editTextConfirmPassword.getText().toString();

                // Vérifier que les mots de passe correspondent
                if (!password.equals(confirmPassword)) {
                    Toast.makeText(register.this, "Les mots de passe ne correspondent pas", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Enregistrer l'utilisateur
                if (registerUser(email, password)) {
                    Toast.makeText(register.this, "Inscription réussie", Toast.LENGTH_SHORT).show();
                    Intent loginIntent = new Intent(register.this, MainActivity.class);
                    startActivity(loginIntent);
                    finish();
                } else {
                    Toast.makeText(register.this, "Inscription échouée", Toast.LENGTH_SHORT).show();
                }
            }
        });

        textViewLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Naviguer vers l'activité de connexion
                finish();
            }
        });
    }

    private boolean registerUser(String email, String password) {
        DatabaseHelper db=new DatabaseHelper(this);
        DatabaseHelper databaseHelper=new DatabaseHelper(this);
        databaseHelper.insertUser(email,password);
        return true;

    }

}
