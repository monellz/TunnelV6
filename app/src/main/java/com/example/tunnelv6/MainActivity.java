package com.example.tunnelv6;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


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
    public EditText ipv6_addr_text;
    public EditText port_text;

    public Button connect_btn;
    public Button disconnect_btn;

    public FrontEnd frontend;

    String[] ip_info;

    private void setDisable(int id) {
        EditText label = findViewById(id);
        label.setClickable(false);
        label.setFocusable(false);
    }

    private void init_view() {
        setDisable(R.id.ip_label);
        setDisable(R.id.route_label);
        setDisable(R.id.dns_label);
        setDisable(R.id.upload_time_label);
        setDisable(R.id.upload_len_label);
        setDisable(R.id.download_time_label);
        setDisable(R.id.download_len_label);
        setDisable(R.id.ipv6_addr_label);
        setDisable(R.id.port_label);

        ipv6_addr_text = findViewById(R.id.ipv6_addr_text);
        port_text = findViewById(R.id.port_text);

        ipv6_addr_text.setText(Constants.IPV6_ADDR);
        port_text.setText(Constants.PORT);

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
                //check ipv6 addr and port
                Pattern addr = Pattern.compile("(([0-9a-fA-F]{1,4}:){7,7}[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,7}:|([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}|([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}|([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}|([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}|[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})|:((:[0-9a-fA-F]{1,4}){1,7}|:)|fe80:(:[0-9a-fA-F]{0,4}){0,4}%[0-9a-zA-Z]{1,}|::(ffff(:0{1,4}){0,1}:){0,1}((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])|([0-9a-fA-F]{1,4}:){1,4}:((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9]))");
                Matcher addr_m = addr.matcher(MainActivity.this.ipv6_addr_text.getText());
                if (!addr_m.matches()) {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("错误")
                            .setMessage("IPv6地址错误")
                            .setPositiveButton("Ok", null)
                            .show();
                    return;
                }
                Pattern port = Pattern.compile("^[0-9]+$");
                Matcher port_m = port.matcher(MainActivity.this.port_text.getText());
                if (!port_m.matches()) {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("错误")
                            .setMessage("端口错误")
                            .setPositiveButton("Ok", null)
                            .show();
                    return;
                }

                connect_btn.setClickable(false);
                disconnect_btn.setClickable(true);

                final File app_dir = new File(getApplicationInfo().dataDir);
                MainActivity.this.frontend = new FrontEnd(MainActivity.this, app_dir.getAbsolutePath());
                new Thread(MainActivity.this.frontend).start();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        backend_entry(app_dir.getAbsolutePath(), MainActivity.this.ipv6_addr_text.getText().toString(), Integer.parseInt(MainActivity.this.port_text.getText().toString()));
                    }
                }).start();
            }
        });

        disconnect_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connect_btn.setClickable(true);
                disconnect_btn.setClickable(false);

                //write -1 to backend
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        File dir = new File(getApplicationInfo().dataDir);
                        File frontend = new File(dir.getAbsoluteFile(), Constants.IP_INFO_FRONTEND);
                        while (!frontend.exists()) {
                            Logger.w(frontend.getAbsolutePath() + " not exist");
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                Logger.e(e.getMessage(), e);
                            }
                        }
                        FileOutputStream f_out = null;
                        try {
                            f_out = new FileOutputStream(frontend);
                        } catch (FileNotFoundException e) {
                            Logger.e(e.getMessage(), e);
                        }

                        assert f_out != null;
                        BufferedOutputStream ip_info_frontend = new BufferedOutputStream(f_out);
                        ByteBuffer bb = ByteBuffer.allocate(4);
                        bb.order(ByteOrder.LITTLE_ENDIAN);
                        bb.putInt(Constants.DISCONNECT_FLAG);
                        byte[] buf = bb.array();
                        //Logger.i("buf: " + Arrays.toString(buf));
                        try {
                            ip_info_frontend.write(buf);
                            ip_info_frontend.flush();
                            f_out.flush();
                            ip_info_frontend.close();
                            f_out.close();
                        } catch (IOException e) {
                            Logger.e(e.getMessage(), e);
                        }
                        Logger.i("disconnect flag write done");
                    }
                }).start();

                frontend.stop();
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
    public native void backend_entry(String dir, String ipv6_addr, int port);
}
