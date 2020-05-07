package com.example.tunnelv6;

import android.annotation.SuppressLint;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tunnelv6.R;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class FrontEnd implements Runnable{
    TextView tv;
    AppCompatActivity main;
    String dir;
    BufferedInputStream ip_info_backend;
    BufferedOutputStream ip_info_frontend;
    FrontEnd(TextView t, AppCompatActivity m, String d) {
        tv = t;
        dir = d;
        main = m;
    }

    private void file_init() {
        File backend = new File(dir, Constants.IP_INFO_BACKEND);
        File frontend = new File(dir, Constants.IP_INFO_FRONTEND);
        while (!backend.exists()) {
            Log.w(Constants.TAG, backend.getAbsolutePath() + " not exist");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        while (!frontend.exists()) {
            Log.w(Constants.TAG, frontend.getAbsolutePath() + " not exist");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        FileInputStream f_in = null;
        FileOutputStream f_out = null;
        try {
            f_in = new FileInputStream(backend);
            f_out = new FileOutputStream(frontend);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        assert f_in != null;
        assert f_out != null;
        ip_info_backend = new BufferedInputStream(f_in);
        ip_info_frontend = new BufferedOutputStream(f_out);
    }

    private String[] read_ip_metadata() {
        byte[] buf = new byte[Constants.PAGE_SIZE];
        int buf_len = 0;
        try {
            buf_len = ip_info_backend.read(buf);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String res = new String(Arrays.copyOfRange(buf, 0, buf_len - 1));
        return res.split(" ");
    }

    @SuppressLint("Assert")
    @Override
    public void run() {
        Log.i(Constants.TAG, "start");
        file_init();

        final String[] info = read_ip_metadata();
        for (String s: info) {
            Log.i(Constants.TAG, "\t" + s);
        }

        assert info.length == 5;
        //update text
        final TextView ip_view = main.findViewById(R.id.ip_text);
        final TextView route_view = main.findViewById(R.id.route_text);
        final TextView dns_view = main.findViewById(R.id.dns_text);

        ip_view.post(new Runnable() {
            @Override
            public void run() {
                ip_view.setText(info[0]);
            }
        });
        route_view.post(new Runnable() {
            @Override
            public void run() {
                route_view.setText(info[1]);
            }
        });
        dns_view.post(new Runnable() {
            @SuppressLint("SetTextI18n")
            @Override
            public void run() {
                dns_view.setText(info[2] + "\n" + info[3] + "\n" + info[4]);
            }
        });



        //flush the screen every 1 second
        int a = 1;
        while (true) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            final int finalA = a;
            tv.post(new Runnable() {
                @SuppressLint("SetTextI18n")
                @Override
                public void run() {
                    tv.setText("get num: " + String.valueOf(finalA));
                }
            });
            a++;
        }
    }
}