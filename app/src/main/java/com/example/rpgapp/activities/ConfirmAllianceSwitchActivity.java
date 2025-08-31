package com.example.rpgapp.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.rpgapp.database.AllianceRepository;
import com.example.rpgapp.database.UserRepository;

public class ConfirmAllianceSwitchActivity extends AppCompatActivity {

    public static final String EXTRA_NEW_ALLIANCE_ID = "EXTRA_NEW_ALLIANCE_ID";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String newAllianceId = getIntent().getStringExtra(EXTRA_NEW_ALLIANCE_ID);
        if (newAllianceId == null) {
            finish();
            return;
        }

        AllianceRepository allianceRepository = AllianceRepository.getInstance(this);

        new AlertDialog.Builder(this)
                .setTitle("Switch Alliance?")
                .setMessage("You are already in an alliance. Accepting this invite will make you leave your current one. Are you sure?")
                .setPositiveButton("Accept & Leave", (dialog, which) -> {
                    allianceRepository.acceptAllianceInvite(newAllianceId, new UserRepository.RequestCallback() {
                        @Override
                        public void onSuccess() {
                            Intent openAppIntent = new Intent(ConfirmAllianceSwitchActivity.this, HomeActivity.class);
                            openAppIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            openAppIntent.putExtra("NAVIGATE_TO", "ALLIANCE");
                            startActivity(openAppIntent);

                            finish();
                        }
                        @Override
                        public void onFailure(Exception e) {
                            Toast.makeText(ConfirmAllianceSwitchActivity.this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });
                })
                .setNegativeButton("Cancel", (dialog, which) -> finish())
                .setOnCancelListener(dialog -> finish())
                .show();
    }
}