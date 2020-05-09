package com.example.tunnelv6;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

@SuppressLint("Registered")
public class TunnelVpn extends VpnService {
    ParcelFileDescriptor pfd;
    private BroadcastReceiver stopBr = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("stop_kill".equals(intent.getAction())) {
                stopself();
            }
        }
    };
    @Override
    public void onCreate() {
        super.onCreate();
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.registerReceiver(stopBr, new IntentFilter("stop_kill"));
        Logger.i("vpn on create");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Logger.i("vpn on start command");
        String[] info = intent.getStringArrayExtra("info");
        if (info == null) {
            Logger.e("info == null ?????");
            return super.onStartCommand(intent, flags, startId);
        }
        assert info != null;
        final int vpn_fd = establishVPN(info);
        Logger.i("establish vpn_fd: "+ String.valueOf(vpn_fd));

        new Thread(new Runnable() {
            @Override
            public void run() {
                //open pipe and write vpn_fd
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
                bb.putInt(vpn_fd);
                byte[] buf = bb.array();
                Logger.i("buf: " + Arrays.toString(buf));
                try {
                    ip_info_frontend.write(buf);
                    ip_info_frontend.flush();
                    f_out.flush();
                    ip_info_frontend.close();
                    f_out.close();
                } catch (IOException e) {
                    Logger.e(e.getMessage(), e);
                }
                Logger.i("vpn_fd write done");
                //while (true) {}
            }
        }).start();

        return super.onStartCommand(intent, flags, startId);
    }

    public int establishVPN(String[] info) {
        int sockfd = 0;
        try {
            sockfd = Integer.parseInt(info[5]);
        } catch (NumberFormatException ignored) {
            Logger.e("sockfd not valid: " + info[5]);
            Logger.e(ignored.getMessage(), ignored);
        }

        this.protect(sockfd);
        Logger.i("vpn protect sockfd: " + String.valueOf(sockfd));
        Builder builder = new Builder();

        builder.setSession(getString(R.string.app_name));

        builder.addAddress(info[0], 32);
        builder.setMtu(1500);
        builder.addRoute("0.0.0.0", 0);
        builder.addDnsServer(info[2]);
        builder.addDnsServer(info[3]);
        builder.addDnsServer(info[4]);

        Logger.i("vpn has prepared, comming to build");
        pfd = builder.establish();
        assert pfd != null;
        int fd = pfd.getFd();
        Logger.i("vpn fd: " + String.valueOf(fd));
        return fd;
    }

    public void stopself() {
        Logger.w("vpn onDestroy");
        try {
            pfd.close();
        } catch (IOException e) {
            Logger.w("????close err");
            Logger.e(e.getMessage(), e);
        }
        Logger.i("vpn close pfd");
        super.stopSelf();
    }
}
