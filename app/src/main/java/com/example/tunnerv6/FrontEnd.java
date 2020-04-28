package com.example.tunnerv6;

import android.widget.TextView;

public class FrontEnd implements Runnable{
    TextView tv;
    FrontEnd(TextView t) {
        tv = t;
    }
    @Override
    public void run() {
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
                @Override
                public void run() {
                    tv.setText(String.valueOf(finalA));
                }
            });
            a++;
        }
    }
}
