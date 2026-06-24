package com.example.kolokvijum2;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.*;

import java.io.IOException;
import java.util.*;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    TextView tvLocation, tvProximity;
    Button btnRecord;
    CheckBox cbStop, cbCountries;

    boolean isRecording = false;

    List<Post> countries = new ArrayList<>();

    FusedLocationProviderClient fusedLocationClient;

    SensorManager sensorManager;
    Sensor proximitySensor;

    MediaRecorder recorder;
    String filePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvLocation = findViewById(R.id.tvLocation);
        tvProximity = findViewById(R.id.tvProximity);
        btnRecord = findViewById(R.id.btnRecord);
        cbStop = findViewById(R.id.cbStop);
        cbCountries = findViewById(R.id.cbCountries);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        getLocation();
        setupProximity();

        btnRecord.setOnClickListener(v -> {

            if (isRecording) return;

            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO}, 1);
                return;
            }

            startRecording();
        });

        cbStop.setOnCheckedChangeListener((b, checked) -> {
            if (checked) stopRecording();
        });

        cbCountries.setOnCheckedChangeListener((b, checked) -> {

            if (checked) {

                if (countries.isEmpty()) {
                    loadCountries();
                } else {

                    countries.remove(countries.size() - 1);

                    Toast.makeText(this,
                            "Ostalo država: " + countries.size(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // ---------------- LOCATION ----------------

    private void getLocation() {

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
        ).addOnSuccessListener(location -> {

            if (location != null) {
                tvLocation.setText(
                        "Lat: " + location.getLatitude() +
                                "\nLng: " + location.getLongitude()
                );
            }
        });
    }

    // ---------------- RETROFIT ----------------

    private void loadCountries() {

        ApiService api = RetrofitClient.getClient().create(ApiService.class);

        api.getPosts().enqueue(new Callback<List<Post>>() {
            @Override
            public void onResponse(Call<List<Post>> call,
                                   Response<List<Post>> response) {

                if (response.isSuccessful() && response.body() != null) {

                    int size = Math.min(10, response.body().size());

                    for (int i = 0; i < size; i++) {
                        countries.add(response.body().get(i));
                    }

                    Toast.makeText(MainActivity.this,
                            "Učitano 10 država",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Post>> call, Throwable t) {
                Toast.makeText(MainActivity.this,
                        "Greška API",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ---------------- PROXIMITY ----------------

    private void setupProximity() {

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        sensorManager.registerListener(new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                tvProximity.setText("Proximity: " + event.values[0]);
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        }, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    // ---------------- AUDIO ----------------

    private void startRecording() {

        try {
            isRecording = true;

            filePath = getFilesDir().getAbsolutePath() + "/audio.3gp";

            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            recorder.setOutputFile(filePath);

            recorder.prepare();
            recorder.start();

            Toast.makeText(this, "Snimanje počelo", Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopRecording() {

        try {
            recorder.stop();
            recorder.release();
            recorder = null;

            isRecording = false;

            Toast.makeText(this,
                    "Snimljeno: " + filePath,
                    Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
