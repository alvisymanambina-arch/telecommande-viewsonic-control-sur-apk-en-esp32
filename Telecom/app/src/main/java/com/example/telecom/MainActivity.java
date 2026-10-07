package com.example.telecom;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private String esp32Ip = "192.168.4.1";

    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;
    private final Map<Integer, String> btnMap = new HashMap<>();

    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private volatile Network wifiNetwork = null;

    private final Map<String, Boolean> inFlight = new ConcurrentHashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        System.setProperty("http.keepAlive", "false");

        prefs = getPreferences(Context.MODE_PRIVATE);
        esp32Ip = prefs.getString("esp32_ip", "192.168.4.1");

        initBtnMap();
        setupAllButtons();
        setupIpConfig();
        requestWifiNetwork();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
        if (connectivityManager != null && networkCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (Exception ignored) {
            }
        }
    }

    private void initBtnMap() {
        btnMap.put(R.id.btn_power, "POWER");
        btnMap.put(R.id.btn_pic, "CAPTURE");
        btnMap.put(R.id.btn_apps, "WINDOWS");
        btnMap.put(R.id.btn_pencil, "PENCIL");
        btnMap.put(R.id.btn_home, "HOME");
        btnMap.put(R.id.btn_back, "BACK");
        btnMap.put(R.id.btn_vol_up, "VOL_UP");
        btnMap.put(R.id.btn_vol_down, "VOL_DOWN");
        btnMap.put(R.id.btn_mute, "MUTE");
        btnMap.put(R.id.btn_source, "SOURCE");
        btnMap.put(R.id.btn_doc, "MENU_PC");
        btnMap.put(R.id.btn_exit, "EXIT_PC");
        btnMap.put(R.id.btn_menu, "SETTINGS");
        btnMap.put(R.id.btn_ok, "OK");
        btnMap.put(R.id.btn_up, "UP");
        btnMap.put(R.id.btn_down, "DOWN");
        btnMap.put(R.id.btn_left, "LEFT");
        btnMap.put(R.id.btn_right, "RIGHT");
        btnMap.put(R.id.btn_screen, "FREEZE");
        btnMap.put(R.id.btn_br_up, "BR_UP");
        btnMap.put(R.id.btn_br_down, "BR_DOWN");
        btnMap.put(R.id.btn_monitor_toggle, "BLANK");
        btnMap.put(R.id.btn_aspect, "ASPECT");
        btnMap.put(R.id.btn_info, "INFO");
        btnMap.put(R.id.btn_0, "0"); btnMap.put(R.id.btn_1, "1"); btnMap.put(R.id.btn_2, "2");
        btnMap.put(R.id.btn_3, "3"); btnMap.put(R.id.btn_4, "4"); btnMap.put(R.id.btn_5, "5");
        btnMap.put(R.id.btn_6, "6"); btnMap.put(R.id.btn_7, "7"); btnMap.put(R.id.btn_8, "8");
        btnMap.put(R.id.btn_9, "9");
        btnMap.put(R.id.btn_red, "RED"); btnMap.put(R.id.btn_green, "GREEN");
        btnMap.put(R.id.btn_yellow, "YELLOW"); btnMap.put(R.id.btn_blue, "BLUE");
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupAllButtons() {
        RepeatListener repeatListener = new RepeatListener(400, 200, v -> {
            String val = btnMap.get(v.getId());
            if (val != null) {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                sendCommand(val);
            }
        });

        for (Integer id : btnMap.keySet()) {
            View v = findViewById(id);
            if (v != null) v.setOnTouchListener(repeatListener);
        }
    }

    private void requestWifiNetwork() {
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkRequest request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                wifiNetwork = network;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                    connectivityManager.bindProcessToNetwork(network);
                }
                mainHandler.post(() -> Toast.makeText(MainActivity.this,
                        "Connect sur ESP32 Wifi", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onLost(@NonNull Network network) {
                if (network.equals(wifiNetwork)) {
                    wifiNetwork = null;
                    mainHandler.post(() -> Toast.makeText(MainActivity.this,
                            "Connection perdu sur ESP32 Wifi", Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onUnavailable() {
                mainHandler.post(() -> Toast.makeText(MainActivity.this,
                        "Ne trouve  pas  Wifi ESP32 - Connecte bien sur wifi ", Toast.LENGTH_LONG).show());
            }
        };

        connectivityManager.requestNetwork(request, networkCallback);
    }

    private void setupIpConfig() {
        findViewById(R.id.logo).setOnLongClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("ESP32 IP Config");
            final EditText input = new EditText(this);
            input.setText(esp32Ip);
            builder.setView(input);
            builder.setPositiveButton("Save", (dialog, which) -> {
                String newIp = input.getText().toString().trim();
                if (!newIp.isEmpty()) {
                    esp32Ip = newIp;
                    prefs.edit().putString("esp32_ip", esp32Ip).apply();
                    Toast.makeText(this, "IP config : " + esp32Ip, Toast.LENGTH_SHORT).show();
                }
            });
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
            builder.show();
            return true;
        });
    }

    private void sendCommand(String val) {
        if (val == null) {
            return;
        }

        final String sanitizedCommand = val.trim().toUpperCase();
        if (sanitizedCommand.isEmpty()) {
            return;
        }

        if (wifiNetwork == null) {
            mainHandler.post(() -> Toast.makeText(this,
                    "Non Connecte sur Wifi ESP32", Toast.LENGTH_SHORT).show());
            return;
        }

        if (Boolean.TRUE.equals(inFlight.putIfAbsent(sanitizedCommand, true))) {
            return;
        }

        final Network network = wifiNetwork;
        final String safeHost = sanitizeIp(esp32Ip);
        executor.execute(() -> {
            HttpURLConnection urlConnection = null;
            try {
                String encodedCommand = URLEncoder.encode(sanitizedCommand, StandardCharsets.UTF_8.name());
                URL url = new URL("http://" + safeHost + "/cmd?val=" + encodedCommand);
                urlConnection = (HttpURLConnection) network.openConnection(url);
                urlConnection.setConnectTimeout(800);
                urlConnection.setReadTimeout(800);
                urlConnection.setRequestMethod("GET");
                urlConnection.setRequestProperty("Connection", "close");

                int responseCode = urlConnection.getResponseCode();
                if (responseCode != 200) {
                    Log.e("Telecom", "Error: " + responseCode);
                    mainHandler.post(() ->
                            Toast.makeText(this, "Erreur: " + responseCode, Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                Log.e("Telecom", "Error: " + e.getMessage());
                mainHandler.post(() ->
                        Toast.makeText(this, "Hors service: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } finally {
                if (urlConnection != null) urlConnection.disconnect();
                inFlight.remove(sanitizedCommand);
            }
        });
    }

    private String sanitizeIp(String ip) {
        if (ip == null) {
            return "192.168.4.1";
        }

        String cleanIp = ip.trim();
        if (cleanIp.startsWith("http://")) {
            cleanIp = cleanIp.substring("http://".length());
        }
        if (cleanIp.startsWith("https://")) {
            cleanIp = cleanIp.substring("https://".length());
        }
        if (cleanIp.isEmpty()) {
            return "192.168.4.1";
        }
        return cleanIp;
    }

    public static class RepeatListener implements View.OnTouchListener {
        private final Handler handler = new Handler(Looper.getMainLooper());
        private final int initialInterval;
        private final int normalInterval;
        private final View.OnClickListener clickListener;
        private View downView;
        private final Runnable handlerRunnable = new Runnable() {
            @Override
            public void run() {
                if (downView != null) {
                    handler.postDelayed(this, normalInterval);
                    clickListener.onClick(downView);
                }
            }
        };

        public RepeatListener(int initialInterval, int normalInterval, View.OnClickListener clickListener) {
            this.initialInterval = initialInterval;
            this.normalInterval = normalInterval;
            this.clickListener = clickListener;
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    handler.removeCallbacks(handlerRunnable);
                    downView = view;
                    downView.setPressed(true);
                    clickListener.onClick(view);
                    handler.postDelayed(handlerRunnable, initialInterval);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    handler.removeCallbacks(handlerRunnable);
                    if (downView != null) {
                        downView.setPressed(false);
                        downView = null;
                    }
                    return true;
            }
            return false;
        }
    }
}