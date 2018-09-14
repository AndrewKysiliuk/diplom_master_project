package com.example.dipapprev2;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class PostData extends AsyncTask<String,Void,String> {

    @Override
    protected String doInBackground(String... params) {
        String sendParams = "date=" + params[0]+"&time=" + params[1]+"&lat="+params[2]+"&long="+params[3]+
                "&enter="+params[4]+"&exit="+params[5]+"&inBus="+params[6];
        String mUrl = "http://dipproject.esy.es/index.php";
        HttpURLConnection mHttpURLConnection = null;
        try {
            URL url = new URL(mUrl);
            mHttpURLConnection = (HttpURLConnection) url.openConnection();
            mHttpURLConnection.setRequestMethod("POST");

            mHttpURLConnection.setDoOutput(true);
            mHttpURLConnection.setDoInput(true);

            OutputStream out = mHttpURLConnection.getOutputStream();
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));
            bufferedWriter.write(sendParams);

            bufferedWriter.flush();
            bufferedWriter.close();
            out.close();

            int responseCode = mHttpURLConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(mHttpURLConnection.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                Log.i("myLog", response.toString());
            } else {
                Log.i("myLog", "Запит POST не спрацював.");
            }
            mHttpURLConnection.disconnect();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}