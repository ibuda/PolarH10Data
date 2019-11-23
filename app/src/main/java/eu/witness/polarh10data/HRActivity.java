package eu.witness.polarh10data;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import polar.com.sdk.api.PolarBleApi;
import polar.com.sdk.api.PolarBleApiCallback;
import polar.com.sdk.api.PolarBleApiDefaultImpl;
import polar.com.sdk.api.model.PolarDeviceInfo;
import polar.com.sdk.api.model.PolarHrData;
import polar.com.sdk.api.errors.PolarInvalidArgument;

public class HRActivity extends AppCompatActivity{

    TextView textViewHR, textViewFW, textViewStatus;
    int reconnectRetries = 0;
    Button exportButton;
    private String TAG = "Polar_HRActivity";
    public PolarBleApi api;
    private Context classContext = this;
    private String DEVICE_ID;
    StringBuilder hrData =  new StringBuilder();
    final SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hr);
        DEVICE_ID = getIntent().getStringExtra("id");
        textViewHR = findViewById(R.id.info2);
        textViewStatus = findViewById(R.id.info3);
        textViewFW = findViewById(R.id.fw2);




        exportButton = findViewById(R.id.button);
        exportButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                exportData(hrData);
            }
        });

        hrData.append("TimeStamp,hr,rrs,rrsMs");

        api = PolarBleApiDefaultImpl.defaultImplementation(this,
                PolarBleApi.FEATURE_BATTERY_INFO |
                        PolarBleApi.FEATURE_DEVICE_INFO |
                        PolarBleApi.FEATURE_HR);



        api.setApiCallback(new PolarBleApiCallback() {
            @Override
            public void blePowerStateChanged(boolean b) {
                Log.d(TAG, "--------------------- BluetoothStateChanged " + b);
            }

            @Override
            public void deviceConnected(PolarDeviceInfo s) {
                Log.d(TAG, "Device connected " + s.deviceId);
                Toast.makeText(classContext, R.string.connected,
                        Toast.LENGTH_SHORT).show();
                textViewStatus.setText("Connected: " + reconnectRetries);
                appendData(1);
                reconnectRetries += 1;
            }

            @Override
            public void deviceConnecting(PolarDeviceInfo polarDeviceInfo) {
                appendData(0);
                textViewStatus.setText("Connecting");
            }

            @Override
            public void deviceDisconnected(PolarDeviceInfo s) {
                Log.d(TAG, "-----------------------Device disconnected " + s);
                Toast.makeText(classContext, R.string.disconnected,
                        Toast.LENGTH_SHORT).show();
                textViewStatus.setText("Disconnected");
//                appendData(-1);
//                try {
//                    reconnectRetries += 1;
//                    api.setAutomaticReconnection(true);
//                    api.connectToDevice(DEVICE_ID);
//                } catch (PolarInvalidArgument a){
//                    a.printStackTrace();
//                }
            }

            @Override
            public void ecgFeatureReady(String s) {
//                Log.d(TAG, "ECG Feature ready " + s);
            }

            @Override
            public void accelerometerFeatureReady(String s) {
//                Log.d(TAG, "ACC Feature ready " + s);
            }

            @Override
            public void ppgFeatureReady(String s) {
//                Log.d(TAG, "PPG Feature ready " + s);
            }

            @Override
            public void ppiFeatureReady(String s) {
//                Log.d(TAG, "PPI Feature ready " + s);
            }

            @Override
            public void biozFeatureReady(String s) {

            }

            @Override
            public void hrFeatureReady(String s) {
//                Log.d(TAG, "HR Feature ready " + s);
            }

            @Override
            public void disInformationReceived(String s, UUID u, String s1) {
                if( u.equals(UUID.fromString("00002a28-0000-1000-8000-00805f9b34fb"))) {
                    String msg = "Firmware: " + s1.trim();
                    Log.d(TAG, "Firmware: " + s + " " + s1.trim());
                    textViewFW.setText(msg + "\n");
                }
            }

            @Override
            public void batteryLevelReceived(String s, int i) {
//                String msg = "ID: " + s + "\nBattery level: " + i;
//                Log.d(TAG, "Battery level " + s + " " + i);
//                Toast.makeText(classContext, msg, Toast.LENGTH_LONG).show();
//                textViewFW.append(msg + "\n");
            }

            @Override
            public void hrNotificationReceived(String s,
                                               PolarHrData polarHrData) {
                List<Integer> rrsMs = polarHrData.rrsMs;
                appendData(polarHrData);

                String msg = polarHrData.hr + "\n";
                for (int i : rrsMs) {
                    msg += i + ",";
                }
                if (msg.endsWith(",")) {
                    msg = msg.substring(0, msg.length() - 1);
                }
                textViewHR.setText(msg);
            }

            @Override
            public void polarFtpFeatureReady(String s) {
//                Log.d(TAG, "Polar FTP ready " + s);
            }
        });

        try {
            api.connectToDevice(DEVICE_ID);
            api.setAutomaticReconnection(true);
        } catch (PolarInvalidArgument a){
            a.printStackTrace();
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        api.shutDown();
        api = null;
    }

    private void appendData(PolarHrData pData) {
        Date date = new Date();
        // Adding info to hrData
        List<Integer> rrsMs = pData.rrsMs;
        List<Integer> rrs = pData.rrs;
        hrData.append("\n" + formatter.format(date) + "," + pData.hr + "," + rrs.get(0) + "," + rrsMs.get(0));
    }

    private void appendData(int code) {
        Date date = new Date();
        // Adding info to hrData
        hrData.append("\n" + formatter.format(date) + "," + code +",,");
    }


    private void exportData(StringBuilder sb){
        try{
            // saving data to local file
            FileOutputStream out = openFileOutput("data.csv", Context.MODE_PRIVATE);
            out.write((sb.toString()).getBytes());
            out.close();

            // exporting
            Context context = getApplicationContext();
            File filelocation = new File(getFilesDir(), "data.csv");
            Uri path = FileProvider.getUriForFile(context, "eu.witness.polarh10data.fileprovider", filelocation);
            Intent fileIntent = new Intent(Intent.ACTION_SEND);
            fileIntent.setType("text/csv");
            fileIntent.putExtra(Intent.EXTRA_SUBJECT, "Data");
            fileIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            fileIntent.putExtra(Intent.EXTRA_STREAM, path);
            startActivity(Intent.createChooser(fileIntent, "Upload Data"));
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
}
