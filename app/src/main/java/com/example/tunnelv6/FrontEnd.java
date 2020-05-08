package com.example.tunnelv6;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.net.VpnService;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.tunnelv6.R;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Vector;

public class FrontEnd implements Runnable{
    MainActivity main;
    String dir;
    BufferedInputStream ip_info_backend;

    FrontEnd(MainActivity m, String d) {
        dir = d;
        main = m;
    }

    private void file_init() {
        File backend = new File(dir, Constants.IP_INFO_BACKEND);
        while (!backend.exists()) {
            Logger.w(backend.getAbsolutePath() + " not exist");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Logger.e(e.getMessage(), e);
            }
        }


        FileInputStream f_in = null;
        try {
            f_in = new FileInputStream(backend);
        } catch (FileNotFoundException e) {
            Logger.e(e.getMessage(), e);
        }

        assert f_in != null;
        ip_info_backend = new BufferedInputStream(f_in);
    }

    private String[] read_ip_metadata() {
        byte[] buf = new byte[Constants.PAGE_SIZE];
        int buf_len = 0;
        try {
            buf_len = ip_info_backend.read(buf);
        } catch (IOException e) {
            Logger.e(e.getMessage(), e);
        }

        String res = new String(Arrays.copyOfRange(buf, 0, buf_len));
        Logger.i("receive: " + res);
        Logger.i("receive len: " + String.valueOf(buf_len));
        return res.split(" ");
    }

    private void setText(final TextView tv, final String s) {
        tv.post(new Runnable() {
            @Override
            public void run() {
                tv.setText(s);
            }
        });
    }

    @SuppressLint("Assert")
    @Override
    public void run() {
        Logger.i("frontend start");
        file_init();

        final String[] info = read_ip_metadata();
        for (String s: info) {
            Logger.i("\t" + s);
        }

        assert info.length == 6;

        //update text
        main.ip_text.post(new Runnable() {
            @Override
            public void run() {
                main.ip_text.setText(info[0]);
            }
        });
        main.route_text.post(new Runnable() {
            @Override
            public void run() {
                main.route_text.setText(info[1]);
            }
        });
        assert main.dns_text.length == info.length - 2;
        for (int i = 0; i < main.dns_text.length; ++i) {
            final int final_i = i;
            main.dns_text[i].post(new Runnable() {
                @Override
                public void run() {
                    main.dns_text[final_i].setText(info[final_i + 2]);
                }
            });
        }

        //start vpn service
        main.startVPN(info);
        Logger.i("vpn start done");

        //flush the screen every 1 second
        while (true) {
            byte[] buf = new byte[Constants.PAGE_SIZE];
            int buf_len = 0;
            try {
                buf_len = ip_info_backend.read(buf);
            } catch (IOException e) {
                Logger.e(e.getMessage(), e);
            }

            Logger.i("receive ip_info len: " + buf_len + ", buf: " + Arrays.toString(Arrays.copyOfRange(buf, 0, buf_len)));
            String res = new String(Arrays.copyOfRange(buf, 0, buf_len));
            String[] infos = res.split(" ");
            //assert infos.length == 4;
            //int[] infos = new int[4];

            //upload_time upload_len download_time download_len
            assert infos.length == 4;

            //update text
            setText(main.upload_time_text, infos[0]);
            setText(main.upload_len_text, infos[1]);
            setText(main.download_time_text, infos[2]);
            setText(main.download_len_text, infos[3]);
        }
    }
}
