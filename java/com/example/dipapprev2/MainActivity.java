package com.example.dipapprev2;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private final int RECIEVE_MESSAGE = 1;
    private static final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothDevice device;
    private BluetoothSocket btSocket;
    private DataTransfer dataSend, dataCalibrate;

    public Button btnStart, btnClbrt;
    public TextView tEnter, tExit, inBus, tLat, tLong;

    private Handler handler;
    private StringBuilder sb = new StringBuilder();

    private LocationManager locationManager;
    public String lat, lon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        btnStart = (Button) findViewById(R.id.btnStart);
        btnClbrt = (Button) findViewById(R.id.btnClbrt);

        tEnter = (TextView) findViewById(R.id.tEnter);
        tExit = (TextView) findViewById(R.id.tExit);
        inBus = (TextView) findViewById(R.id.inBus);
        tLat = (TextView) findViewById(R.id.tLat);
        tLong = (TextView) findViewById(R.id.tLong);

        btnStart.setOnClickListener(this);
        btnClbrt.setOnClickListener(this);

        if (device == null) {
            btnStart.setEnabled(false);
            btnClbrt.setEnabled(false);
        }

        handler = new Handler() {
            public void handleMessage(android.os.Message msg) {
                switch (msg.what) {
                    case RECIEVE_MESSAGE:
                        byte[] readBuf = (byte[]) msg.obj;
                        String strIncom = new String(readBuf, 0, msg.arg1);
                        sb.append(strIncom);
                        int endOfLineIndex = sb.indexOf("\r\n");
                        String sbprint;
                        if (endOfLineIndex > 0) {
                            sbprint = sb.substring(0, endOfLineIndex);
                            sb.delete(0, sb.length());
                            String[] data = sbprint.split("@");
                            showAndSendData(data);
                        }
                        break;
                }
            }

        };
    }

    private void showAndSendData(String[] incomData) {
        if (incomData[0].compareTo("data") == 0) {
            PostData mPostData = new PostData();
            tEnter.setText("зайшло: " + String.valueOf(incomData[1]));
            tExit.setText("вийшло: " + String.valueOf(incomData[2]));
            inBus.setText("Всього в салоні: " + String.valueOf(incomData[3]));
            tLat.setText("широта: " + lat);
            tLong.setText("довгота: " + lon);
            SimpleDateFormat date = new SimpleDateFormat("yyyy-MM-dd");
            SimpleDateFormat time = new SimpleDateFormat("HH:mm:ss");
            String sDate = date.format(new Date());
            String sTime = time.format(new Date());
            mPostData.execute(sDate, sTime, lat, lon, incomData[1], incomData[2], incomData[3]);
        } else if (incomData[0].compareTo("calibrationFinish") == 0) {
            btnStart.setEnabled(true);
            btnClbrt.setEnabled(true);
            dataCalibrate.cancel();
            btSocket = null;
            Toast.makeText(this, "Калібрування завершено", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        if (device != null) {
            btnStart.setEnabled(true);
            btnClbrt.setEnabled(true);
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                1000 * 10, 5, locationListener);
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        locationManager.removeUpdates(locationListener);
    }

    public LocationListener locationListener = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            if (location == null)
                return;
            lat = String.valueOf(location.getLatitude());
            lon = String.valueOf(location.getLongitude());
        }

        @Override
        public void onProviderDisabled(String provider) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.btItem:
                Toast.makeText(this, "Bluetooth - Settings", Toast.LENGTH_SHORT).show();
                startActivityForResult(new Intent(this, BtActivity.class), 1);
                break;
            case R.id.gpsItem:
                Toast.makeText(this, "GPS - Settings", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            try {
                device = data.getParcelableExtra("btDevice");
                Toast.makeText(this, "Вибрано Bluetooth пристрій: " + device.getName(), Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else
            Toast.makeText(this, "Виберіть Bluetooth пристрій!!!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnStart:
                btConn();
                dataSend = new DataTransfer(btSocket, handler);
                dataSend.start();
                btnStart.setEnabled(false);
                btnClbrt.setEnabled(false);
                break;
            case R.id.btnClbrt:
                btConn();
                dataCalibrate = new DataTransfer(btSocket, handler);
                dataCalibrate.start();
                dataCalibrate.sendMessage("Calibration");
                btnStart.setEnabled(false);
                btnClbrt.setEnabled(false);
                break;
        }
    }

    public void btConn() {
        if (btSocket == null) {
            try {
                btSocket = device.createRfcommSocketToServiceRecord(uuid);
            } catch (IOException e) {
                Toast.makeText(this, "Fatal Error: socket create failed: " +
                        e.getMessage() + ".", Toast.LENGTH_SHORT).show();
            }
        }
        if (btSocket.isConnected()) {
            Toast.makeText(this, "...З'єднання встановлене і використовується...", Toast.LENGTH_SHORT).show();
            return;
        } else {
            try {
                btSocket.connect();
                Toast.makeText(this, "...Підключення встановлено...", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Toast.makeText(this, "Невдалося виконати підключення", Toast.LENGTH_SHORT).show();
                try {
                    btSocket.close();
                    btSocket = null;
                } catch (IOException e2) {
                    Toast.makeText(this, "Fatal Error: unable to close socket during connection failure"
                            + e2.getMessage() + ".", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dataSend.isAlive()) {
            dataSend.cancel();
        }
        try {
            btSocket.close();
            btSocket = null;
        } catch (IOException e2) {
            Toast.makeText(this, "Fatal Error: failed to close socket." +
                    e2.getMessage() + ".", Toast.LENGTH_SHORT).show();
        }
    }
}