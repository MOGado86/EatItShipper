package com.example.eatitshipper;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.animation.LinearInterpolator;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.eatitshipper.Common.Common;
import com.example.eatitshipper.Common.LatLngInterpolator;
import com.example.eatitshipper.Common.MarkerAnimation;
import com.example.eatitshipper.Model.ShippingOrderModel;
import com.example.eatitshipper.Remote.IGoogleApi;
import com.example.eatitshipper.Remote.RetroFitClient;
import com.firebase.ui.auth.data.model.Resource;
import com.google.android.gms.common.api.Status;
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
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.SquareCap;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteFragment;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import net.cachapa.expandablelayout.ExpandableLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.paperdb.Paper;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class ShippingActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    private Marker shipperMarker;
    private ShippingOrderModel shippingOrderModel;

    private boolean isInit = false;
    private Location previousLocation = null;

    private Handler handler;
    private int index, next;
    private LatLng start, end;
    private float v;
    private double lat, lng;
    private Polyline blackPolyline, greyPolyline;
    private PolylineOptions polylineOptions, blackPolylineOptions;
    private List<LatLng> polylineList;
    private IGoogleApi iGoogleApi;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

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
    @BindView(R.id.btn_show)
    MaterialButton btn_show;
    @SuppressLint("NonConstantResourceId")
    @BindView(R.id.expandable_layout)
    ExpandableLayout expandable_layout;
    @SuppressLint("NonConstantResourceId")
    @BindView(R.id.img_food_image)
    ImageView img_food_image;
    private Polyline yellowPolyline;


    @SuppressLint("NonConstantResourceId")
    @OnClick(R.id.btn_show)
    void onShowClick() {
        if (expandable_layout.isExpanded()) {
            btn_show.setText("SHOW");
        } else {
            btn_show.setText("HIDE");
        }
        expandable_layout.toggle();
    }

    @SuppressLint("NonConstantResourceId")
    @OnClick(R.id.btn_call)
    void onCallClick() {
        if (shippingOrderModel != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                Dexter.withActivity(this)
                        .withPermission(Manifest.permission.CALL_PHONE)
                        .withListener(new PermissionListener() {
                            @Override
                            public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {

                            }

                            @Override
                            public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
                                Toast.makeText(ShippingActivity.this, "You must accept this permission to call user", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {

                            }
                        }).check();

                return;
            }
            Intent intent = new Intent(Intent.ACTION_CALL);
            intent.setData(Uri.parse("tel:" + shippingOrderModel.getOrderModel().getUserPhone()));
            startActivity(intent);
        }
    }
    AutocompleteSupportFragment places_fragment;
    PlacesClient placesClient;
    List<Place.Field> placeFields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG);
    private Polyline redPolyline;

    @SuppressLint("NonConstantResourceId")
    @OnClick(R.id.btn_start_trip)
    void onStartTripClick() {
        String data = Paper.book().read(Common.SHIPPING_ORDER_DATA);
        Paper.book().write(Common.TRIP_START, data);
        btn_start_trip.setEnabled(false);

        shippingOrderModel = new Gson().fromJson(data, new TypeToken<ShippingOrderModel>(){}.getType());
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(location -> compositeDisposable.add(iGoogleApi.getDirections("driving", "less_driving",
                        Common.buildLocationString(location),
                        shippingOrderModel.getOrderModel().getLat() +
                                "," +
                                shippingOrderModel.getOrderModel().getLng(), getString(R.string.google_maps_key))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(s -> {
                            String estimateTime = "UNKNOWN";
                            JSONObject jsonObject = new JSONObject(s);
                            JSONArray routes = jsonObject.getJSONArray("routes");
                            JSONObject object = routes.getJSONObject(0);
                            JSONArray legs = object.getJSONArray("legs");
                            JSONObject legsObject = legs.getJSONObject(0);

                            JSONObject time = legsObject.getJSONObject("duration");
                            estimateTime = time.getString("text");

                            Map<String, Object> update_data = new HashMap<>();
                            update_data.put("currentLat", location.getLatitude());
                            update_data.put("currentLng", location.getLongitude());
                            update_data.put("estimateTime", estimateTime);
                            FirebaseDatabase.getInstance().getReference(Common.RESTAURANT_REF)
                                    .child(Common.currentRestaurant.getUid())
                                    .child(Common.SHIPPING_ORDER_REF)
                                    .child(shippingOrderModel.getKey())
                                    .updateChildren(update_data)
                                    .addOnFailureListener(e -> Toast.makeText(ShippingActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show())
                                    .addOnSuccessListener(aVoid -> drawRoutes(data));

                        }, throwable -> Toast.makeText(ShippingActivity.this, throwable.getMessage(), Toast.LENGTH_SHORT).show()))).addOnFailureListener(e -> Toast.makeText(ShippingActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show());

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shipping);

        iGoogleApi = RetroFitClient.getInstance().create(IGoogleApi.class);

        initPlaces();
        setupAutoCompletePlaces();

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
                        mapFragment.getMapAsync(ShippingActivity.this);

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


    }

    private void setupAutoCompletePlaces() {
        places_fragment = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.places_autocomplete_fragment);
        places_fragment.setPlaceFields(placeFields);
        places_fragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                drawRoutes(place);

            }

            @Override
            public void onError(@NonNull Status status) {
                Log.d("PLACES API", "onError: " + status.getStatusMessage());
                Toast.makeText(ShippingActivity.this, "" + status.getStatusMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void drawRoutes(Place place) {
        mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
                .title(place.getName())
                .snippet(place.getAddress())
                .position(place.getLatLng()));

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationProviderClient.getLastLocation().addOnFailureListener(e -> Toast.makeText(ShippingActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show())
                .addOnSuccessListener(location -> {
                    String to = place.getLatLng().latitude +
                            "," +
                            place.getLatLng().longitude;
                    String from = location.getLongitude() +
                            "," +
                            location.getLongitude();

                    compositeDisposable.add(iGoogleApi.getDirections("driving", "less_driving",
                            from, to, getString(R.string.google_maps_key)).subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread()).subscribe(s -> {

                                try {
                                    JSONObject jsonObject = new JSONObject(s);
                                    JSONArray jsonArray = jsonObject.getJSONArray("routes");
                                    for (int i = 0; i < jsonArray.length(); i++) {
                                        JSONObject route = jsonArray.getJSONObject(i);
                                        JSONObject poly = route.getJSONObject("overview_polyline");
                                        String polyline = poly.getString("points");
                                        polylineList = Common.decodePoly(polyline);
                                    }

                                    polylineOptions = new PolylineOptions();
                                    polylineOptions.color(Color.YELLOW);
                                    polylineOptions.width(12);
                                    polylineOptions.startCap(new SquareCap());
                                    polylineOptions.jointType(JointType.ROUND);
                                    polylineOptions.addAll(polylineList);
                                    yellowPolyline = mMap.addPolyline(polylineOptions);
                                } catch (Exception e) {
                                    Toast.makeText(ShippingActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }, throwable -> Toast.makeText(ShippingActivity.this, "" + throwable.getMessage(), Toast.LENGTH_SHORT).show()));
                });
    }

    private void drawRoutes(String data) {
        ShippingOrderModel shippingOrderModel = new Gson().fromJson(data, new TypeToken<ShippingOrderModel>() {
        }.getType());

        //Add Box
        mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.box))
                .title(shippingOrderModel.getOrderModel().getUserName())
                .snippet(shippingOrderModel.getOrderModel().getShippingAddress())
                .position(new LatLng(shippingOrderModel.getOrderModel().getLat(), shippingOrderModel.getOrderModel().getLng())));

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationProviderClient.getLastLocation().addOnFailureListener(e -> Toast.makeText(ShippingActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show())
                .addOnSuccessListener(location -> {
                    String to = shippingOrderModel.getOrderModel().getLat() +
                            "," +
                            shippingOrderModel.getOrderModel().getLng();
                    String from = location.getLongitude() +
                            "," +
                            location.getLongitude();

                    compositeDisposable.add(iGoogleApi.getDirections("driving", "less_driving",
                            from, to, getString(R.string.google_maps_key)).subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread()).subscribe(s -> {

                                try {
                                    JSONObject jsonObject = new JSONObject(s);
                                    JSONArray jsonArray = jsonObject.getJSONArray("routes");
                                    for (int i = 0; i < jsonArray.length(); i++) {
                                        JSONObject route = jsonArray.getJSONObject(i);
                                        JSONObject poly = route.getJSONObject("overview_polyline");
                                        String polyline = poly.getString("points");
                                        polylineList = Common.decodePoly(polyline);
                                    }

                                    polylineOptions = new PolylineOptions();
                                    polylineOptions.color(Color.RED);
                                    polylineOptions.width(12);
                                    polylineOptions.startCap(new SquareCap());
                                    polylineOptions.jointType(JointType.ROUND);
                                    polylineOptions.addAll(polylineList);
                                    redPolyline = mMap.addPolyline(polylineOptions);
                                } catch (Exception e) {
                                    Toast.makeText(ShippingActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }, throwable -> Toast.makeText(ShippingActivity.this, "" + throwable.getMessage(), Toast.LENGTH_SHORT).show()));
                });
    }

    private void initPlaces() {
        Places.initialize(this, getString(R.string.google_maps_key));
        placesClient = Places.createClient(this);
    }

    private void setShippingOrder() {
        Paper.init(this);
        String data;
        if (TextUtils.isEmpty(Paper.book().read(Common.TRIP_START))) {
            btn_start_trip.setEnabled(true);
            data = Paper.book().read(Common.SHIPPING_ORDER_DATA);
        } else {
            btn_start_trip.setEnabled(false);
            data = Paper.book().read(Common.TRIP_START);
        }

        if (!TextUtils.isEmpty(data)) {

            drawRoutes(data);
            shippingOrderModel = new Gson().fromJson(data, new TypeToken<ShippingOrderModel>() {
            }.getType());
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

                updateLocation(locationResult.getLastLocation());

                if (shipperMarker == null) {
                    int width, height;
                    height = width = 80;
                    BitmapDrawable bitmapDrawable = (BitmapDrawable) ContextCompat.getDrawable(ShippingActivity.this, R.drawable.shippernew);
                    Bitmap resized = Bitmap.createScaledBitmap(bitmapDrawable.getBitmap(), width, height, false);
                    shipperMarker = mMap.addMarker(new MarkerOptions().
                            icon(BitmapDescriptorFactory.fromBitmap(resized)).position(locationShipper).title("You"));

                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(locationShipper, 18));
                }
                /*else {
                    shipperMarker.setPosition(locationShipper);
                }*/
                if (isInit && previousLocation != null) {

                    String from = previousLocation.getLatitude() +
                            "," +
                            previousLocation.getLongitude();

                    String to = locationShipper.latitude +
                            "," +
                            locationShipper.longitude;


                    moveMarkerAnimation(shipperMarker, from, to);
                    previousLocation = locationResult.getLastLocation();

                }

                if (!isInit) {
                    isInit = true;
                    previousLocation = locationResult.getLastLocation();
                }

            }
        };
    }

    private void updateLocation(Location lastLocation) {


        String data = Paper.book().read(Common.TRIP_START);
        if (!TextUtils.isEmpty(data)) {
            ShippingOrderModel shippingOrderModel = new Gson().fromJson(data, new TypeToken<ShippingOrderModel>() {}.getType());
            if (shippingOrderModel != null) {
                compositeDisposable.add(iGoogleApi.getDirections("driving", "less_driving",
                        Common.buildLocationString(lastLocation),
                        shippingOrderModel.getOrderModel().getLat() +
                                "," +
                                shippingOrderModel.getOrderModel().getLng(), getString(R.string.google_maps_key))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(s -> {
                            String estimateTime =  "UNKNOWN";
                            JSONObject jsonObject = new JSONObject(s);
                            JSONArray routes = jsonObject.getJSONArray("routes");
                            JSONObject object = routes.getJSONObject(0);
                            JSONArray legs = object.getJSONArray("legs");
                            JSONObject legsObject = legs.getJSONObject(0);

                            JSONObject time = legsObject.getJSONObject("duration");
                            estimateTime = time.getString("text");

                            Map<String, Object> update_data = new HashMap<>();
                            update_data.put("currentLat", lastLocation.getLatitude());
                            update_data.put("currentLng", lastLocation.getLongitude());
                            update_data.put("estimateTime", estimateTime);
                            FirebaseDatabase.getInstance().getReference(Common.RESTAURANT_REF)
                                    .child(Common.currentRestaurant.getUid())
                                    .child(Common.SHIPPING_ORDER_REF)
                                    .child(shippingOrderModel.getKey())
                                    .updateChildren(update_data)
                                    .addOnFailureListener(e -> Toast.makeText(ShippingActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show());

                        }, throwable -> Toast.makeText(ShippingActivity.this, throwable.getMessage(), Toast.LENGTH_SHORT).show()));
            }
        } else {
            Toast.makeText(this, "Please start your trip", Toast.LENGTH_SHORT).show();
        }
    }

    private void moveMarkerAnimation(Marker marker, String from, String to) {

        compositeDisposable.add(iGoogleApi.getDirections("driving", "less_driving",
                from, to, getString(R.string.google_maps_key)).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(returnResult -> {
                    Log.d("API RETURN", "moveMarkerAnimation: " + returnResult);
                    try {
                        JSONObject jsonObject = new JSONObject(returnResult);
                        JSONArray jsonArray = jsonObject.getJSONArray("routes");
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject route = jsonArray.getJSONObject(i);
                            JSONObject poly = route.getJSONObject("overview_polyline");
                            String polyline = poly.getString("points");
                            polylineList = Common.decodePoly(polyline);
                        }

                        polylineOptions = new PolylineOptions();
                        polylineOptions.color(Color.GRAY);
                        polylineOptions.width(5);
                        polylineOptions.startCap(new SquareCap());
                        polylineOptions.jointType(JointType.ROUND);
                        polylineOptions.addAll(polylineList);
                        greyPolyline = mMap.addPolyline(polylineOptions);

                        blackPolylineOptions = new PolylineOptions();
                        blackPolylineOptions.color(Color.BLACK);
                        blackPolylineOptions.width(5);
                        blackPolylineOptions.startCap(new SquareCap());
                        blackPolylineOptions.jointType(JointType.ROUND);
                        blackPolylineOptions.addAll(polylineList);
                        greyPolyline = mMap.addPolyline(blackPolylineOptions);

                        //Animator
                        ValueAnimator polylineAnimator = ValueAnimator.ofInt(0, 100);
                        polylineAnimator.setDuration(2000);
                        polylineAnimator.setInterpolator(new LinearInterpolator());
                        polylineAnimator.addUpdateListener(valueAnimator -> {
                            List<LatLng> points = greyPolyline.getPoints();
                            int percentValue = (int) valueAnimator.getAnimatedValue();
                            int size = points.size();
                            int newPoints = (int) (size * (percentValue / 100.0f));
                            List<LatLng> p = points.subList(0, newPoints);
                            blackPolyline.setPoints(p);
                        });
                        polylineAnimator.start();

                        //Bike moving
                        handler = new Handler();
                        index = -1;
                        next = 1;
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (index < polylineList.size() - 1) {
                                    index++;
                                    next = index + 1;
                                    start = polylineList.get(index);
                                    end = polylineList.get(next);
                                }
                                ValueAnimator valueAnimator = ValueAnimator.ofInt(0, 1);
                                valueAnimator.setDuration(1500);
                                valueAnimator.setInterpolator(new LinearInterpolator());
                                valueAnimator.addUpdateListener(valueAnimator1 -> {
                                    v = valueAnimator1.getAnimatedFraction();
                                    lng = v * end.longitude + (1 - v) * start.longitude;
                                    lat = v * end.latitude + (1 - v) * start.latitude;

                                    LatLng newPos = new LatLng(lat, lng);
                                    marker.setPosition(newPos);
                                    marker.setAnchor(0.5f, 0.5f);
                                    marker.setRotation(Common.getBearing(start, newPos));

                                    mMap.moveCamera(CameraUpdateFactory.newLatLng(marker.getPosition()));
                                });
                                valueAnimator.start();
                                if (index < polylineList.size() - 2) //reach destination
                                    handler.postDelayed(this, 1500);
                            }
                        }, 1500);
                    } catch (Exception e) {
                        Toast.makeText(this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }, throwable -> {
                    if (throwable != null)
                        Toast.makeText(this, "" + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                }));
    }

    private void buildLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(15000);
        locationRequest.setFastestInterval(10000);
        locationRequest.setSmallestDisplacement(20f);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        setShippingOrder();
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
        compositeDisposable.clear();
        super.onDestroy();

    }

}