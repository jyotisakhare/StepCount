package com.jyoti.janacare;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.view.View;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by jsakhare on 24/04/15.
 */
public class MainApplication extends Application {

private static MainApplication instance;
private static Context appContext;
private static long maxMemorySize;
private static int maxCacheSize;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        this.setAppContext(getApplicationContext());
        maxMemorySize = Runtime.getRuntime().maxMemory();
        maxCacheSize = (int)(maxMemorySize / 8);
        DiskCacheService.initialize(getApplicationContext());


    }

    public static MainApplication getInstance(){
        return instance;
    }
    public static Context getAppContext() {
        return appContext;
    }
    public void setAppContext(Context mAppContext) {
        this.appContext = mAppContext;
    }


    void createShareForecastIntent(View rootView) {

        File screenShot = takeScreenShot(rootView);
        Intent shareintent = new Intent(Intent.ACTION_SEND);
        Uri imageUri = Uri.parse(screenShot.getPath());
        Log.d("image path", screenShot.getPath() + " " + imageUri);
        shareintent.putExtra(Intent.EXTRA_STREAM, imageUri);
        shareintent.setType("image/jpeg");
        startActivity(Intent.createChooser(shareintent, "Share images to.."));

    }
    private File takeScreenShot(View rootView){
        //View rootView = findViewById(android.R.id.content).getRootView();
        rootView.setDrawingCacheEnabled(true);
        Bitmap bitmap = rootView.getDrawingCache();
        File imagePath = new File(Environment.getExternalStorageDirectory() + "/screenshot.jpeg");
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(imagePath);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            Log.e("GREC", e.getMessage(), e);
        } catch (IOException e) {
            Log.e("GREC", e.getMessage(), e);
        }

        return imagePath;
    }
}
