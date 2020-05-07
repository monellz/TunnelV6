package com.example.tunnelv6;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.example.tunnelv6.R;

import java.io.File;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.e("TEST_TAG from java", "write not granted");

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                Log.i("TEST_TAG from java", "into should show");
            } else {
                Log.i("TEST_TAG from java", "into not should show");
                ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            }
        }
        */


        final File app_dir = new File(getApplicationInfo().dataDir);
        // Example of a call to a native method
        TextView tv = findViewById(R.id.sample_text);

        new Thread(new FrontEnd(tv, this, app_dir.getAbsolutePath())).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                backend_entry(app_dir.getAbsolutePath());
            }
        }).start();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                TextView tv = findViewById(R.id.sample_text);
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    Log.i("TEST_TAG from java", "granted!");
                    tv.setText("granted");
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Log.e("TEST_TAG from java", "not granted!");
                    tv.setText("not granted");
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }

    public void startVPN(String[] info) {
        Log.i(Constants.TAG, "into startVPN info: " + Arrays.toString(info));
        Intent vpn_intent = VpnService.prepare(MainActivity.this);
        if (vpn_intent != null) {
            Log.i(Constants.TAG, "intent != null");
            if (info == null) {
                Log.e(Constants.TAG, "in startVPN info == null??/");
            }

            vpn_intent.putExtra("info", info);

            startActivityForResult(vpn_intent, Constants.VPN_REQUEST_CODE);
            Log.i(Constants.TAG, "startActivityfor result done");
        } else {
            Log.i(Constants.TAG, "intent == null");
            Log.d(Constants.TAG, "(intent==null) info: "+ Arrays.toString(info));
            Intent data = new Intent(MainActivity.this, VpnService.class);

            data.putExtra("info", info);
            onActivityResult(Constants.VPN_REQUEST_CODE, RESULT_OK, data);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.VPN_REQUEST_CODE && resultCode == RESULT_OK) {
            Intent intent = new Intent (this, TunnelVpn.class);

            String[] info = data.getStringArrayExtra("info");
            if (info == null) {
                Log.e(Constants.TAG, "in onactivityresult info == null??/");
            }
            intent.putExtra("info", info);
            Log.i(Constants.TAG, "start service!!!");
            startService(intent);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native void backend_entry(String dir);
}
