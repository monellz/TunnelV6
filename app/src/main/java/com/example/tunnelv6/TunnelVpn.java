package com.example.tunnelv6;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.VpnService;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

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
            Log.e(Constants.TAG, "info == null ?????");
            return super.onStartCommand(intent, flags, startId);
        }
        assert info != null;
        final int vpn_fd = establishVPN(info);

        new Thread(new Runnable() {
            @Override
            public void run() {
                //open pipe and write vpn_fd
                File dir = new File(getApplicationInfo().dataDir);
                File frontend = new File(dir.getAbsoluteFile(), Constants.IP_INFO_FRONTEND);
                while (!frontend.exists()) {
                    Log.w(Constants.TAG, frontend.getAbsolutePath() + " not exist");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                FileOutputStream f_out = null;
                try {
                    f_out = new FileOutputStream(frontend);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                assert f_out != null;
                BufferedOutputStream ip_info_frontend = new BufferedOutputStream(f_out);
                ByteBuffer bb = ByteBuffer.allocate(4);
                bb.order(ByteOrder.LITTLE_ENDIAN);
                bb.putInt(vpn_fd);
                byte[] buf = bb.array();
                Log.i(Constants.TAG, "buf: " + Arrays.toString(buf));
                try {
                    ip_info_frontend.write(buf);
                    ip_info_frontend.flush();
                    ip_info_frontend.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(Constants.TAG, "pipe write error");
                }
                Log.i(Constants.TAG, "vpn_fd write done");
            }
        }).start();

        return super.onStartCommand(intent, flags, startId);
    }

    public int establishVPN(String[] info) {
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
        return fd;
    }
}
