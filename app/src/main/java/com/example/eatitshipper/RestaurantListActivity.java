package com.example.eatitshipper;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.EditText;
import android.widget.Toast;

import com.example.eatitshipper.Adapter.MyRestaurantAdapter;
import com.example.eatitshipper.Callback.IRestaurantCallbackListener;
import com.example.eatitshipper.Common.Common;
import com.example.eatitshipper.EventBus.RestaurantSelectEvent;
import com.example.eatitshipper.Model.RestaurantModel;
import com.example.eatitshipper.Model.ShipperUserModel;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import dmax.dialog.SpotsDialog;
import io.paperdb.Paper;

public class RestaurantListActivity extends AppCompatActivity implements IRestaurantCallbackListener {

    @SuppressLint("NonConstantResourceId")
    @BindView(R.id.recycler_restaurant)
    RecyclerView recycler_restaurant;
    AlertDialog dialog;
    LayoutAnimationController layoutAnimationController;
    MyRestaurantAdapter adapter;

    DatabaseReference serverRef;
    IRestaurantCallbackListener listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restaurant_list);

        initViews();
        loadAllRestaurants();
    }

    private void loadAllRestaurants() {
        dialog.dismiss();
        List<RestaurantModel> tempList = new ArrayList<>();
        DatabaseReference restaurantRef = FirebaseDatabase.getInstance().getReference(Common.RESTAURANT_REF);
        restaurantRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot restaurantSnapshot : snapshot.getChildren()) {
                        RestaurantModel model = restaurantSnapshot.getValue(RestaurantModel.class);
                        model.setUid(restaurantSnapshot.getKey());
                        tempList.add(model);
                    }
                    if (tempList.size() > 0) {
                        listener.onRestaurantLoadSuccess(tempList);
                    } else
                        listener.onRestaurantLoadFailed("Restaurant list empty");
                } else {
                    listener.onRestaurantLoadFailed("Restaurant list doesn't exist");
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onRestaurantLoadFailed(error.getMessage());
            }
        });
    }

    private void initViews() {
        ButterKnife.bind(this);
        listener = this;

        dialog = new SpotsDialog.Builder().setContext(RestaurantListActivity.this).setCancelable(false).setMessage("Please Wait").build();
        dialog.show();
        layoutAnimationController = AnimationUtils.loadLayoutAnimation(RestaurantListActivity.this, R.anim.layout_item_from_left);
        LinearLayoutManager layoutManager = new LinearLayoutManager(RestaurantListActivity.this);
        layoutManager.setOrientation(RecyclerView.VERTICAL);
        recycler_restaurant.setLayoutManager(layoutManager);
        recycler_restaurant.addItemDecoration(new DividerItemDecoration(RestaurantListActivity.this, layoutManager.getOrientation()));
    }

    @Override
    public void onRestaurantLoadSuccess(List<RestaurantModel> restaurantModelList) {
        dialog.dismiss();
        adapter = new MyRestaurantAdapter(this, restaurantModelList);
        recycler_restaurant.setAdapter(adapter);
        recycler_restaurant.setLayoutAnimation(layoutAnimationController);
    }

    @Override
    public void onRestaurantLoadFailed(String message) {
        Toast.makeText(this, ""+message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onRestaurantSelectedEvent(RestaurantSelectEvent event) {
        if (event != null) {

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                checkServerUserFromFirebase(user, event.getRestaurantModel());
            }
        }
    }

    private void checkServerUserFromFirebase(FirebaseUser user, RestaurantModel restaurantModel) {
        dialog.show();

        serverRef = FirebaseDatabase.getInstance().getReference(Common.RESTAURANT_REF)
                .child(restaurantModel.getUid())
                .child(Common.SHIPPER_REF);
        serverRef.child(user.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            ShipperUserModel userModel = snapshot.getValue(ShipperUserModel.class);
                            if (userModel.isActive()) {
                                goToHomeActivity(userModel, restaurantModel);
                             } else {
                                Toast.makeText(RestaurantListActivity.this, "You must be enabled from server app", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            dialog.dismiss();
                            showRegisterDialog(user, restaurantModel.getUid());
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(RestaurantListActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();

                    }
                });
    }

    private void showRegisterDialog(FirebaseUser user, String uid) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Register");
        builder.setMessage("Please fill information");
        builder.setCancelable(false);

        View itemView = LayoutInflater.from(this).inflate(R.layout.layout_register, null);
        TextInputLayout phone_input_layout = itemView.findViewById(R.id.phone_input_layout);
        EditText edt_name = itemView.findViewById(R.id.edt_name);
        EditText edt_phone = itemView.findViewById(R.id.edt_phone);

        //set Data
        if (user.getPhoneNumber() == null || TextUtils.isEmpty(user.getPhoneNumber())) {
            phone_input_layout.setHint("Email");
            edt_phone.setText(user.getEmail());
            edt_name.setText(user.getDisplayName());
        }
        else
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

            serverRef = FirebaseDatabase.getInstance().getReference(Common.RESTAURANT_REF)
                    .child(uid)
                    .child(Common.SHIPPER_REF);
            serverRef.child(shipperUserModel.getUid()).setValue(shipperUserModel)
                    .addOnFailureListener(e -> {
                        dialog.dismiss();
                        Toast.makeText(RestaurantListActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    dialog.dismiss();
                    Toast.makeText(RestaurantListActivity.this, "Registration successful, admin will contact you active soon", Toast.LENGTH_SHORT).show();
                    //goToHomeActivity(serverUserModel);


                }
            });


        });

        builder.setView(itemView);
        AlertDialog registerDialog = builder.create();
        registerDialog.show();
    }

    private void goToHomeActivity(ShipperUserModel userModel, RestaurantModel restaurantModel) {
        dialog.dismiss();

        String jsonEncode = new Gson().toJson(restaurantModel);
        Paper.init(this);
        Paper.book().write(Common.RESTAURANT_SAVE, jsonEncode);

        Common.currentShipperUser = userModel;
        startActivity(new Intent(RestaurantListActivity.this, HomeActivity.class));
        finish();

    }
}