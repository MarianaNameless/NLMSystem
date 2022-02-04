package com.example.probe;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class ServiceReceiver extends BroadcastReceiver {
    OkHttpClient client = new OkHttpClient();
    private final String url = "http://192.168.2.9:5000";
    private ArrayList<String> allIPAddresses = new ArrayList<String>();
    private String listToStr;
    HashMap<String, String> IPsAndTimes = new HashMap<String, String>();

    @Override
    public void onReceive(Context context, Intent intent) {

        sendRequest();
         Log.d("Receiver", String.valueOf(Calendar.getInstance().getTime()));

    }
    //Get the IP addresses for gateway
    void sendRequest(){
        String method = "/get_data";
        String fullUrl = url + method;
        Request request = new Request.Builder().url(fullUrl).build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                ResponseBody body = response.body();  //get data from gateway
                listToStr = Objects.requireNonNull(body.string());
                try {
                    JSONArray allkeys = new JSONArray(listToStr); //put data into an arraylist
                    for (int i = 0; i < allkeys.length(); i++){
                        allIPAddresses.add(allkeys.getString(i));
                    }
                    if (!response.isSuccessful()) {
                        response.close();
                        body.close();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                for (String IP:allIPAddresses){
                    try {
                        String time = ping(IP);  //ping all the IP addresses
                        IPsAndTimes.put(IP, time);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (!IPsAndTimes.isEmpty()){ //if echoservers exist, send data to gateway
                    postRequest();
                }else {
                    Log.d("Message", "There are no echoservers");
                }
                body.close();
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.d("Connection", "failed");
                e.printStackTrace();
            }
        });

    }
    // Post the collected data to gateway
    void postRequest() {
        String method = "/post_data";
        String fullUrl = url + method;
        RequestBody formBody = new FormBody.Builder().add("value", String.valueOf(IPsAndTimes)).build();
        Request request = new Request.Builder().url(fullUrl).post(formBody).build();
        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                Log.d("message", "Post request");
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.d("Post", "failed");
                e.printStackTrace();
            }
        });
    }

    //The ping process
    String ping(String host) throws IOException {
        Process p = new ProcessBuilder("sh").redirectErrorStream(true).start();
        DataOutputStream os = new DataOutputStream(p.getOutputStream());
        os.writeBytes("ping -c 1 " + host + "\n");
        os.flush();
        os.writeBytes("exit\n");
        os.flush();
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        StringBuilder pingResults = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            pingResults.append(line);
        }
        if (String.valueOf(pingResults).contains("rtt")){ //extract the time it took to ping from ping statistics
            int index = pingResults.indexOf("rtt");
            String partialResults = pingResults.substring(index);
            String[] strings = partialResults.split(" ");
            partialResults = strings[3];
            strings = partialResults.split("/");
            return strings[0];
        }else { //some echoservers might not respond
            return "not responding";
        }
    }
}
