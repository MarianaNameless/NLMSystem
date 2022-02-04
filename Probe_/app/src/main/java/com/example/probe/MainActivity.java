package com.example.probe;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {


    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView textView = findViewById(R.id.textview);

        //Check if device is connected to a network
        if (!isConnected()) {
            textView.setText("Please, connect to the Internet");
        } else {
            //Show device's IP address
            OkHttpClient okHttpClient = new OkHttpClient();
            Request request = new Request.Builder().url("http://192.168.2.9:5000/").build();
            okHttpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            textView.setText("Did not connect to server");
                        }
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    textView.setText(response.body().string());
                }

            });
            startService(new Intent(this, PingingService.class));
        }

    }

    public boolean isConnected() {
        boolean connected = false;
        try {
            ConnectivityManager cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo nInfo = cm.getActiveNetworkInfo();
            connected = nInfo != null && nInfo.isAvailable() && nInfo.isConnected();
            return connected;
        } catch (Exception e) {
            Log.e("Connectivity Exception", e.getMessage());
        }
        return connected;
    }



        @Override
        protected void onStart() {
            //Start the service
            super.onStart();
            Intent serviceIntent = new Intent(this, PingingService.class);

            class ServiceRunnable implements Runnable {
                Intent intent;

                ServiceRunnable(Intent intent) {
                    this.intent = intent;
                }

                @Override
                public void run() {
                    startService(serviceIntent);
                }
            }
            ServiceRunnable runnable = new ServiceRunnable(serviceIntent);
            new Thread(runnable).start();
        }


    }