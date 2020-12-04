package com.example.eatitshipper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.example.eatitshipper.Common.Common;
import com.example.eatitshipper.Model.ShipperUserModel;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;
import java.util.List;

import dmax.dialog.SpotsDialog;

public class MainActivity extends AppCompatActivity {
    private static int APP_REQUEST_CODE = 7171;
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener listener;
    private AlertDialog dialog;
    private DatabaseReference serverRef;
    private List<AuthUI.IdpConfig> providers;


    public static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onStart() {
        super.onStart();
        firebaseAuth.addAuthStateListener(listener);
    }

    @Override
    protected void onStop() {
        if (listener != null)
            firebaseAuth.removeAuthStateListener(listener);
        super.onStop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    private void init() {
        providers = Arrays.asList(new AuthUI.IdpConfig.PhoneBuilder().build());

        serverRef = FirebaseDatabase.getInstance().getReference(Common.SHIPPER_REF);
        firebaseAuth = FirebaseAuth.getInstance();
        dialog = new SpotsDialog.Builder().setCancelable(false).setContext(this).build();

        listener = firebaseAuthLocal -> {
            FirebaseUser user = firebaseAuthLocal.getCurrentUser();
            if (user != null) {
                //Already Login
                checkServerUserFromFirebase(user);

            } else {
                phoneLogin();
            }
        };
    }

    private void checkServerUserFromFirebase(FirebaseUser user) {
        dialog.show();
        serverRef.child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    ShipperUserModel userModel = snapshot.getValue(ShipperUserModel.class);
                    if (userModel.isActive()) {
                        goToHomeActivity(userModel);
                    } else {
                        dialog.dismiss();
                        Toast.makeText(MainActivity.this, "You must be marked active by admin to use the app", Toast.LENGTH_SHORT).show();
                    }

                } else {
                    showRegisterDialog(user);
                    dialog.dismiss();

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                dialog.dismiss();
                Log.e(TAG, "onCancelled: " + error);
            }
        });
    }

    private void phoneLogin() {
        startActivityForResult(AuthUI.getInstance().
                        createSignInIntentBuilder().setAvailableProviders(providers).build(),
                APP_REQUEST_CODE);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == APP_REQUEST_CODE) {
            IdpResponse response = IdpResponse.fromResultIntent(data);
            if (resultCode == RESULT_OK) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            } else {
                Toast.makeText(this, "Failed to sign in!", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void showRegisterDialog(FirebaseUser user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Register");
        builder.setMessage("Please fill information");
        builder.setCancelable(false);

        View itemView = LayoutInflater.from(this).inflate(R.layout.layout_register, null);
        EditText edt_name = itemView.findViewById(R.id.edt_name);
        EditText edt_phone = itemView.findViewById(R.id.edt_phone);

        //set Data
        edt_phone.setText(user.getPhoneNumber());
        builder.setNegativeButton("CANCEL", (dialogInterface, i) -> dialogInterface.dismiss());
        builder.setPositiveButton("REGISTER", (dialogInterface, i) -> {
            if (TextUtils.isEmpty(edt_name.getText().toString())) {
                Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show();
                return;
            }


            ShipperUserModel shipperUserModel = new ShipperUserModel();
            shipperUserModel.setUid(user.getUid());
            shipperUserModel.setName(edt_name.getText().toString());
            shipperUserModel.setPhone(edt_phone.getText().toString());
            shipperUserModel.setActive(false);

            dialog.show();

            serverRef.child(shipperUserModel.getUid()).setValue(shipperUserModel)
                    .addOnFailureListener(e -> {
                        dialog.dismiss();
                        Toast.makeText(MainActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    dialog.dismiss();
                    Toast.makeText(MainActivity.this, "Registration successful, admin will contact you active soon", Toast.LENGTH_SHORT).show();
                    //goToHomeActivity(serverUserModel);


                }
            });


        });

        builder.setView(itemView);
        AlertDialog registerDialog = builder.create();
        registerDialog.show();

    }

    private void goToHomeActivity(ShipperUserModel shipperUserModel) {
        dialog.dismiss();
        Common.currentShipperUser = shipperUserModel;
        startActivity(new Intent(MainActivity.this, HomeActivity.class));
        finish();
    }

}