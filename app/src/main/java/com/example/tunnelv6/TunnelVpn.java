package com.example.tunnelv6;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;

@SuppressLint("Registered")
public class TunnelVpn extends VpnService {
    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(Constants.TAG, "vpn on create");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(Constants.TAG, "vpn on start command");
        String[] info = intent.getStringArrayExtra("info");
        if (info == null) {
            Log.i(Constants.TAG, "info == null ?????");
            return super.onStartCommand(intent, flags, startId);
        }
        assert info != null;
        establishVPN(info);

        return super.onStartCommand(intent, flags, startId);
    }

    public void establishVPN(String[] info) {
        int sockfd = 0;
        try {
            sockfd = Integer.parseInt(info[5]);
        } catch (NumberFormatException ignored) {
            Log.e(Constants.TAG, "sockfd not valid: " + info[5]);
        }

        this.protect(sockfd);
        Log.i(Constants.TAG, "vpn protect sockfd: " + String.valueOf(sockfd));
        Builder builder = new Builder();

        builder.setSession(getString(R.string.app_name));

        builder.addAddress(info[0], 32);
        builder.addRoute("0.0.0.0", 0);
        builder.addDnsServer(info[2]);
        builder.addDnsServer(info[3]);
        builder.addDnsServer(info[4]);

        ParcelFileDescriptor pfd = builder.establish();
        assert pfd != null;
        int fd = pfd.getFd();
        Log.i(Constants.TAG, "vpn fd: " + String.valueOf(fd));
    }
}
