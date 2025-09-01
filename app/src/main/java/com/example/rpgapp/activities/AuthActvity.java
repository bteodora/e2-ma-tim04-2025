package com.example.rpgapp.activities;

import android.content.Intent; // <-- VAŽAN IMPORT
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.rpgapp.R;
import com.google.firebase.auth.FirebaseAuth; // <-- VAŽAN IMPORT

public class AuthActvity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            Intent intent = new Intent(AuthActvity.this, HomeActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        setContentView(R.layout.activity_auth);
    }
}