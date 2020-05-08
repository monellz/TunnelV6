package com.example.tunnelv6;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.tunnelv6.R;

import java.io.File;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    public TextView ip_text;
    public TextView route_text;
    public TextView[] dns_text;
    public TextView upload_time_text;
    public TextView upload_len_text;
    public TextView download_time_text;
    public TextView download_len_text;

    public Button connect_btn;
    public Button disconnect_btn;


    String[] ip_info;

    private void init_view() {
        ip_text = findViewById(R.id.ip_text);
        route_text = findViewById(R.id.route_text);
        dns_text = new TextView[3];
        dns_text[0] = findViewById(R.id.dns1_text);
        dns_text[1] = findViewById(R.id.dns2_text);
        dns_text[2] = findViewById(R.id.dns3_text);
        upload_time_text = findViewById(R.id.upload_time_text);
        upload_len_text = findViewById(R.id.upload_len_text);
        download_time_text = findViewById(R.id.download_time_text);
        download_len_text = findViewById(R.id.download_len_text);

        connect_btn = findViewById(R.id.connect_btn);
        disconnect_btn = findViewById(R.id.disconnect_btn);

        ip_text.setText("");
        route_text.setText("");
        for (TextView t: dns_text) {
            t.setText("");
        }
        upload_time_text.setText("");
        upload_len_text.setText("");
        download_time_text.setText("");
        download_len_text.setText("");


        connect_btn.setClickable(false);
        disconnect_btn.setClickable(false);

        connect_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connect_btn.setClickable(false);
                disconnect_btn.setClickable(true);

                final File app_dir = new File(getApplicationInfo().dataDir);
                new Thread(new FrontEnd(MainActivity.this, app_dir.getAbsolutePath())).start();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        backend_entry(app_dir.getAbsolutePath());
                    }
                }).start();
            }
        });
    }

    private void init_log() {
        File app_dir = new File(Environment.getExternalStorageDirectory().getAbsoluteFile(), getApplication().getPackageName());
        if (!app_dir.exists()) {
            Logger.w(app_dir.getAbsolutePath() + " not exist, now create it");
            if (!app_dir.mkdirs()) {
                Logger.e("cannot create app_dir");
            }
        }
        Logger.init(app_dir.getAbsolutePath());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init_view();
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, Constants.RW_EXTERNAL_STORAGE_REQUEST_CODE);
        } else {
            init_log();
            connect_btn.setClickable(true);
            disconnect_btn.setClickable(false);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case Constants.RW_EXTERNAL_STORAGE_REQUEST_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Logger.i("rw permission granted!");
                    init_log();
                    connect_btn.setClickable(true);
                    disconnect_btn.setClickable(false);
                } else {
                    Logger.e("rw permission not granted!");
                    connect_btn.setClickable(false);
                    disconnect_btn.setClickable(false);
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }

    public void startVPN(String[] info) {
        ip_info = info;
        Intent vpn_intent = VpnService.prepare(MainActivity.this);
        if (vpn_intent != null) {
            startActivityForResult(vpn_intent, Constants.VPN_REQUEST_CODE);
        } else {
            onActivityResult(Constants.VPN_REQUEST_CODE, RESULT_OK, null);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.VPN_REQUEST_CODE && resultCode == RESULT_OK) {
            Intent intent = new Intent (this, TunnelVpn.class);
            intent.putExtra("info", ip_info);
            Logger.i("start service!");
            startService(intent);
        }
    }
    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native void backend_entry(String dir);
}
