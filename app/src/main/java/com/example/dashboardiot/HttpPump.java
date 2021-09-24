package com.example.dashboardiot;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by Admin on 9/13/2021.
 */

public class HttpPump extends AsyncTask<Void, Void, Void> {
    String data;
    @Override
    protected Void doInBackground(Void... voids) {
        try {
            URL url = new URL("https://io.adafruit.com/api/v2/phamdinhtrung/feeds/iot-pump");
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            InputStream inputStream = httpURLConnection.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line = "";
            while (line != null) {
                line = bufferedReader.readLine();
                //Log.d("MyApp", line);
                data = data + line;
            }
            data = data.split(",")[12].split("\"")[3];
            //Log.d("MyApp", x);



        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        Log.d("Http", "Button");
        MainActivity.btnPUMP.setChecked(data.equals("1")?true:false);
    }
}
