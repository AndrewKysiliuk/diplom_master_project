package com.example.dipapprev2;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BtActivity extends AppCompatActivity {
    private final int DIALOG = 1;

    ListView pairDev;
    ListView findDev;
    BluetoothAdapter bluetoothAdapter;
    private ArrayAdapter<BluetoothDevice> listAdapter;
    private final List<BluetoothDevice> discoveredDevices = new ArrayList<BluetoothDevice>();
    private final List<BluetoothDevice> pairTmp = new ArrayList<BluetoothDevice>();

    private ArrayAdapter<String> pairAdapter;
    private BluetoothDevice mDevice;

    private BroadcastReceiver discoverDevicesReceiver;
    private BroadcastReceiver discoveryFinishedReceiver;
    private ProgressDialog progressDialog;

    TabHost tabHost;
    Menu menu;
    String dName;
    Button saveBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bt);

        initTab();

        pairDev = (ListView) findViewById(R.id.pairDev);
        pairDev.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        findDev = (ListView) findViewById(R.id.findDev);

        btInit();

        pairDev.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                mDevice = pairTmp.get(position);
            }
        });
        findDev.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {

                mDevice = discoveredDevices.get(position);
                dName = mDevice.getName();
                showDialog(DIALOG);
            }
        });

        saveBtn = (Button)findViewById(R.id.saveBtn);
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mDevice != null) {
                    Intent result = new Intent();
                    result.putExtra("btDevice", mDevice);
                    setResult(RESULT_OK, result);
                    finish();
                }
                else{
                    Toast.makeText(BtActivity.this, "Виберіть пристрій для підключення!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void initTab() {
        tabHost = (TabHost) findViewById(R.id.tabHost);
        tabHost.setup();

        TabHost.TabSpec tabSpec = tabHost.newTabSpec("pair");
        tabSpec.setIndicator(getResources().getString(R.string.pairDevice));
        tabSpec.setContent(R.id.pairDev);
        tabHost.addTab(tabSpec);

        tabSpec = tabHost.newTabSpec("scan");
        tabSpec.setIndicator(getResources().getString(R.string.findDevice));
        tabSpec.setContent(R.id.findDev);
        tabHost.addTab(tabSpec);

        tabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            public void onTabChanged(String tabId) {
                switch (tabId) {
                    case "pair":
                        if (menu != null) {
                            menu.findItem(R.id.scanItem).setVisible(false);
                        }

                        break;
                    case "scan":
                        if (menu != null) {
                            menu.findItem(R.id.scanItem).setVisible(true);
                        }
                        break;
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        this.menu = menu;
        getMenuInflater().inflate(R.menu.bt_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.scanItem:
                if (bluetoothAdapter.isEnabled() == false)
                    Toast.makeText(this, "Для початку сканування увімкніть Bluetooth", Toast.LENGTH_SHORT).show();
                else {
                    bluetoothScan();
                    findDev.setAdapter(listAdapter);
                    listAdapter.notifyDataSetChanged();
                }
                break;
            case R.id.btOnItem:
                startActivityForResult(new Intent().setAction(Settings.ACTION_BLUETOOTH_SETTINGS), 0);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (bluetoothAdapter.isEnabled())
            pairInitialize();
    }

    protected Dialog onCreateDialog(int id) {
        if (id == DIALOG) {
            AlertDialog.Builder adb = new AlertDialog.Builder(this);
            adb.setTitle("Створення пари:");
            adb.setMessage("Створити пару з пристроєм " + dName);
            adb.setIcon(android.R.drawable.ic_dialog_info);
            adb.setPositiveButton("Так", myClickListener);
            adb.setNegativeButton("Ні", myClickListener);
            return adb.create();
        }
        return super.onCreateDialog(id);
    }

    Dialog.OnClickListener myClickListener = new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case Dialog.BUTTON_POSITIVE:
                    try {
                        Method method = mDevice.getClass().getMethod("createBond", (Class[]) null);
                        method.invoke(mDevice, (Object[]) null);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Toast.makeText(BtActivity.this, "Пару з пристроєм " + dName + " створено!", Toast.LENGTH_SHORT).show();
                    pairInitialize();
                    findDev.setAdapter(null);
                    dName = null;
                    break;
                case Dialog.BUTTON_NEGATIVE:
                    Toast.makeText(BtActivity.this, "Пару з пристроєм " + dName + " не створено!", Toast.LENGTH_SHORT).show();
                    dName = null;
                    break;
            }
        }
    };

    private void btInit() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            BluetoothManager mbluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            bluetoothAdapter = mbluetoothManager.getAdapter();
        } else {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth не підтримується!!!", Toast.LENGTH_SHORT).show();
            finish();
        }
        if (bluetoothAdapter.isEnabled())
            pairInitialize();
    }

    private void bluetoothScan() {
        discoveredDevices.clear();

        if (discoverDevicesReceiver == null) {
            discoverDevicesReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();

                    if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        if (!discoveredDevices.contains(device))
                            discoveredDevices.add(device);
                    }
                }
            };
        }
        if (discoveryFinishedReceiver == null) {
            discoveryFinishedReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    findDev.setEnabled(true);
                    if (progressDialog != null)
                        progressDialog.dismiss();
                    Toast.makeText(getBaseContext(), "Пощук завершено. Виберіть пристрій для з'єднання...", Toast.LENGTH_LONG).show();
                    unregisterReceiver(discoveryFinishedReceiver);
                }
            };
        }

        registerReceiver(discoverDevicesReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        registerReceiver(discoveryFinishedReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));

        findDev.setEnabled(false);
        progressDialog = ProgressDialog.show(this, "Пошук пристроїв", "Зачекайте...");

        bluetoothAdapter.startDiscovery();

        listAdapter = new ArrayAdapter<BluetoothDevice>(getBaseContext(), android.R.layout.simple_list_item_1, discoveredDevices) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                final BluetoothDevice device = getItem(position);
                ((TextView) view.findViewById(android.R.id.text1)).setText(device.getName());
                return view;
            }
        };
    }

    private void pairInitialize() {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            pairAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_checked);
            for (BluetoothDevice device : pairedDevices) {
                pairAdapter.add(device.getName() + "\n" + device.getAddress());
                pairTmp.add(device);
            }
        }
        pairDev.setAdapter(pairAdapter);
    }

    @Override
    public void onPause() {
        super.onPause();
        bluetoothAdapter.cancelDiscovery();
        if (discoverDevicesReceiver != null) {
            try {
                unregisterReceiver(discoverDevicesReceiver);
            } catch (Exception e) {
                Toast.makeText(this, "Неможливо відключити ресивер" + discoverDevicesReceiver, Toast.LENGTH_SHORT).show();
            }
        }
    }
}