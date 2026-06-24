package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.widget.*;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import java.util.*;

import retrofit2.*;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import android.hardware.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.gms.location.*;

public class MainActivity extends AppCompatActivity {

    TextView tvLocation;
    ImageButton btnCamera;
    ImageView imageView;
    Switch switchData;
    Button btnAction;

    FusedLocationProviderClient fusedLocationClient;

    static final int CAMERA_REQUEST = 100;

    SensorManager sensorManager;
    Sensor accelerometer;

    float lastX = 0, lastY = 0, lastZ = 0;

    SharedPreferences sp;

    List<Post> postList = new ArrayList<>();
    boolean firstSwitch = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvLocation = findViewById(R.id.tvLocation);
        btnCamera = findViewById(R.id.btnCamera);
        imageView = findViewById(R.id.imageView);
        switchData = findViewById(R.id.switchData);
        btnAction = findViewById(R.id.btnAction);

        sp = getSharedPreferences("data", MODE_PRIVATE);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        requestLocationPermission();
        requestCameraPermission();
        requestContactPermission();

        getLocation();

        // CAMERA
        btnCamera.setOnClickListener(v -> {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {

                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent, CAMERA_REQUEST);
            }
        });

        // SWITCH (SIMPLIFIED - NO DATABASE)
        switchData.setOnCheckedChangeListener((buttonView, isChecked) -> {

            if (isChecked) {

                if (firstSwitch) {
                    loadPosts(); // samo učitavanje
                    firstSwitch = false;
                } else {
                    if (!postList.isEmpty()) {
                        Toast.makeText(this,
                                postList.get(0).title,
                                Toast.LENGTH_SHORT).show();
                    }
                }

            } else {

                sp.edit().putString("tekst", tvLocation.getText().toString()).apply();

                String name = getFirstContact();
                tvLocation.setText(name);
            }
        });

        // BUTTON = ACCELEROMETER
        btnAction.setOnClickListener(v ->
                btnAction.setText("X:" + lastX + " Y:" + lastY + " Z:" + lastZ)
        );

        // SENSOR
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        createChannel();
    }

    // ---------------- RETROFIT ----------------

    private void loadPosts() {

        ApiService api = RetrofitClient.getClient().create(ApiService.class);

        api.getPosts().enqueue(new Callback<List<Post>>() {
            @Override
            public void onResponse(Call<List<Post>> call, Response<List<Post>> response) {

                if (response.isSuccessful() && response.body() != null) {

                    postList.clear();

                    int size = Math.min(10, response.body().size());

                    for (int i = 0; i < size; i++) {
                        postList.add(response.body().get(i));
                    }

                    Toast.makeText(MainActivity.this,
                            "Učitano postova",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Post>> call, Throwable t) {
                Toast.makeText(MainActivity.this,
                        "API error",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ---------------- SENSOR ----------------

    SensorEventListener sensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {

            lastX = event.values[0];
            lastY = event.values[1];
            lastZ = event.values[2];
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };

    // ---------------- CONTACT ----------------

    private String getFirstContact() {

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return "No permission";
        }

        Cursor cursor = getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null, null, null, null
        );

        if (cursor != null && cursor.moveToFirst()) {

            String name = cursor.getString(
                    cursor.getColumnIndexOrThrow(
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                    )
            );

            cursor.close();
            return name;
        }

        return "No contact";
    }

    // ---------------- LOCATION ----------------

    private void getLocation() {

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;

        fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
        ).addOnSuccessListener(location -> {

            if (location != null) {
                tvLocation.setText("Lat:" + location.getLatitude() +
                        "\nLng:" + location.getLongitude());
            }
        });
    }

    // ---------------- CAMERA ----------------

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CAMERA_REQUEST && resultCode == RESULT_OK && data != null) {

            Bitmap photo = (Bitmap) data.getExtras().get("data");
            imageView.setImageBitmap(photo);

            Toast.makeText(this,
                    "Slikano (sensor X:" + lastX + ")",
                    Toast.LENGTH_SHORT).show();
        }
    }

    // ---------------- NOTIF ----------------

    private void createChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel = new NotificationChannel(
                    "channel1",
                    "Kolokvijum",
                    NotificationManager.IMPORTANCE_HIGH
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private void showNotification() {

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, "channel1")
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle("Kolokvijum")
                        .setContentText("Nema više postova!")
                        .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManagerCompat.from(this).notify(1, builder.build());
    }

    // ---------------- PERMISSIONS ----------------

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA}, 2);
    }

    private void requestContactPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_CONTACTS}, 3);
    }
}