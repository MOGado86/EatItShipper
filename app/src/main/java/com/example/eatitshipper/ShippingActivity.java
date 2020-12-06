package com.example.eatitshipper;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.eatitshipper.Common.Common;
import com.example.eatitshipper.Model.ShippingOrderModel;
import com.firebase.ui.auth.data.model.Resource;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.text.SimpleDateFormat;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.paperdb.Paper;

public class ShippingActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    private Marker shipperMarker;
    private ShippingOrderModel shippingOrderModel;

    @SuppressLint("NonConstantResourceId")
    @BindView(R.id.txt_order_number)
    TextView txt_order_number;
    @SuppressLint("NonConstantResourceId")
    @BindView(R.id.txt_name)
    TextView txt_name;
    @SuppressLint("NonConstantResourceId")
    @BindView(R.id.txt_address)
    TextView txt_address;
    @SuppressLint("NonConstantResourceId")
    @BindView(R.id.txt_date)
    TextView txt_date;

    @SuppressLint("NonConstantResourceId")
    @BindView(R.id.btn_start_trip)
    MaterialButton btn_start_trip;
    @SuppressLint("NonConstantResourceId")
    @BindView(R.id.btn_call)
    MaterialButton btn_call;
    @SuppressLint("NonConstantResourceId")
    @BindView(R.id.btn_done)
    MaterialButton btn_done;
    @SuppressLint("NonConstantResourceId")
    @BindView(R.id.img_food_image)
    ImageView img_food_image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shipping);

        ButterKnife.bind(this);
        buildLocationRequest();
        buildLocationCallback();

        Dexter.withActivity(ShippingActivity.this)
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
                        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                                .findFragmentById(R.id.map);
                        mapFragment.getMapAsync(ShippingActivity.this::onMapReady);

                        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(ShippingActivity.this);
                        if (ActivityCompat.checkSelfPermission(ShippingActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(ShippingActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            return;
                        }
                        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
                        Toast.makeText(ShippingActivity.this, "You must enable this location permission", Toast.LENGTH_SHORT).show();

                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {

                    }
                }).check();

        setShippingOrder();

    }

    private void setShippingOrder() {
        Paper.init(this);
        String data = Paper.book().read(Common.SHIPPING_ORDER_DATA);

        if (!TextUtils.isEmpty(data)) {
            shippingOrderModel = new Gson().fromJson(data, new TypeToken<ShippingOrderModel>(){}.getType());
            if (shippingOrderModel != null) {
                Common.setSpanStringColor("Name : ", shippingOrderModel.getOrderModel().getUserName(),
                        txt_name, Color.parseColor("#333639"));

                txt_date.setText(new StringBuilder().append(new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").
                        format(shippingOrderModel.getOrderModel().getCreateDate())));

                Common.setSpanStringColor("No : ", shippingOrderModel.getOrderModel().getKey(),
                        txt_order_number, Color.parseColor("#673ab7"));

                Common.setSpanStringColor("Address : ", shippingOrderModel.getOrderModel().getShippingAddress(),
                        txt_address, Color.parseColor("#795548"));

                Glide.with(this)
                        .load(shippingOrderModel.getOrderModel().getCartItemList().get(0).getFoodImg())
                        .into(img_food_image);
            }

        }

    }

    private void buildLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                // Add a marker in Sydney and move the camera
                LatLng locationShipper = new LatLng(locationResult.getLastLocation().getLatitude(),
                        locationResult.getLastLocation().getLongitude());

                if (shipperMarker == null) {
                    int width, height;
                    height = width = 80;
                    BitmapDrawable bitmapDrawable = (BitmapDrawable) ContextCompat.getDrawable(ShippingActivity.this, R.drawable.shipper);
                    Bitmap resized = Bitmap.createScaledBitmap(bitmapDrawable.getBitmap(), width, height, false);
                    shipperMarker = mMap.addMarker(new MarkerOptions().
                            icon(BitmapDescriptorFactory.fromBitmap(resized)).position(locationShipper).title("You"));
                } else {
                    shipperMarker.setPosition(locationShipper);
                }
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(locationShipper, 15));

            }
        };
    }

    private void buildLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(15000);
        locationRequest.setFastestInterval(10000);
        locationRequest.setSmallestDisplacement(20f);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.getUiSettings().setZoomControlsEnabled(true);

        try {
            boolean success = googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_theme));

            if (!success) {
                Log.e("Style failed", "onMapReady: Style parsing failed");
            }
        } catch (Resources.NotFoundException exception) {
            Log.e("Style failed", "onMapReady: Resource not found");
        }
    }

    @Override
    protected void onDestroy() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        super.onDestroy();

    }

}