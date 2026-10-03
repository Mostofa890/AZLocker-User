package com.my.Refiner.Cash.User;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CrashHandler implements Thread.UncaughtExceptionHandler {

    private Context context;
    private Thread.UncaughtExceptionHandler defaultHandler;

    public CrashHandler(Context context) {
        this.context = context;
        this.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        try {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            throwable.printStackTrace(pw);

            String errorText = "Time: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date())
                    + "\n\n" + sw.toString();

            // Download ফোল্ডারে সেভ করুন
            File file = new File(Environment.getExternalStorageDirectory(), "az_crash.txt");
            FileWriter writer = new FileWriter(file);
            writer.write(errorText);
            writer.close();

            Log.e("CrashHandler", errorText);
        } catch (Exception e) {
            Log.e("CrashHandler", "Failed to write crash log", e);
        }

        if (defaultHandler != null) {
            defaultHandler.uncaughtException(thread, throwable);
        }
    }
}
