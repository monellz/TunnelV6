package com.example.tunnelv6;

import android.annotation.SuppressLint;
import android.util.Log;
import android.os.Process;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Logger{
    private static File log_file = null;
    private static FileOutputStream fos = null;
    private static OutputStreamWriter os_writer = null;
    private static BufferedWriter writer = null;
    @SuppressLint("SimpleDateFormat")
    private static final SimpleDateFormat FILE_NAME_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
    @SuppressLint("SimpleDateFormat")
    private static final SimpleDateFormat LOG_TIME_FORMAT = new SimpleDateFormat("HH-mm-ss.S");
    static void init(String log_dir) {
        log_file = new File(log_dir, FILE_NAME_FORMAT.format(Calendar.getInstance().getTime()) + ".java.log");
        try {
            if (!log_file.exists()) {
                if (!log_file.createNewFile()) {
                    Log.e(Constants.TAG, "cannot create log: " + log_file.getAbsolutePath());
                } else {
                    Log.i(Constants.TAG, "create log: " + log_file.getAbsolutePath());
                }
            }
            fos = new FileOutputStream(log_file);
            os_writer = new OutputStreamWriter(fos);
            writer = new BufferedWriter(os_writer);

        } catch (IOException e) {
            Log.e(Constants.TAG, e.getMessage(), e);
        }
    }
    static void i(String msg) {
        Log.i(Constants.TAG, msg);
        if (writer != null) write(msg, "INFO", null);
    }
    static void e(String msg) {
        Log.e(Constants.TAG, msg);
        if (writer != null) write(msg, "ERROR", null);
    }

    static void e(String msg, Throwable throwable) {
        Log.e(Constants.TAG, msg, throwable);
        if (writer != null) write(msg, "ERROR", throwable);
    }

    static void w(String msg) {
        Log.w(Constants.TAG, msg);
        if (writer != null) write(msg, "WARN", null);
    }
    static void v(String msg) {
        Log.v(Constants.TAG, msg);
        if (writer != null) write(msg, "VERBOSE", null);
    }
    static void d(String msg) {
        Log.d(Constants.TAG, msg);
        if (writer != null) write(msg, "DEBUG", null);
    }
    private static void write(String msg, String level, Throwable throwable) {
        String time_stamp = LOG_TIME_FORMAT.format(Calendar.getInstance().getTime());
        try {
            writer.write(time_stamp + " " + String.valueOf(Process.myPid()) + "/" + String.valueOf(Process.myTid()) + " [" + level + "] " + Constants.TAG + ": " + msg);
            writer.newLine();
            writer.flush();
            os_writer.flush();
            fos.flush();
            if (throwable != null) save_crash(throwable);
        } catch (IOException e) {
            Log.e(Constants.TAG, e.getMessage(), e);
        }
    }

    private static void save_crash(Throwable throwable) throws IOException {
        StringWriter s_writer = new StringWriter();
        PrintWriter p_writer = new PrintWriter(s_writer);
        throwable.printStackTrace(p_writer);
        Throwable cause = throwable.getCause();
        while (cause != null) {
            cause.printStackTrace(p_writer);
            cause = cause.getCause();
        }
        p_writer.flush();
        p_writer.close();
        s_writer.flush();
        String crashInfo = writer.toString();
        s_writer.close();
        writer.write(crashInfo);
        writer.newLine();
        writer.flush();
        os_writer.flush();
        fos.flush();
    }
}
