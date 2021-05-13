package com.chapelhilldenham.test_app;


import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.UriPermission;
import android.net.Uri;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.OutputStream;
import java.util.List;

import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugins.GeneratedPluginRegistrant;

public class MainActivity extends FlutterActivity {
    String CHANNEL = "com.test_app";
    MethodChannel methodChannel = null;

    @Override
    public void configureFlutterEngine(@NonNull FlutterEngine flutterEngine) {
        GeneratedPluginRegistrant.registerWith(flutterEngine);
        methodChannel = new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), CHANNEL);
        methodChannel
                .setMethodCallHandler(
                        (methodCall, result) -> {


                            switch (methodCall.method) {
//                                this is called first to get sdcard location
                                case "takeCardUriPermission":
                                    takeCardUriPermission(methodCall.argument("storageType"));
                                    result.success(null);

                                    break;
                                case "saveFile":
//                          here, we get the path from the flutter side and write the
//                          file to sdcard
                                    String filepath = (String) methodCall.argument("filepath");

                                    final byte[] bytes = methodCall.argument("bytes");
//                                    Log.e("save to path", filepath);


                                    try {
                                        if (filepath == null || bytes == null)
                                            throw new Exception("Arguments Not found");
                                        DocumentFile documentFile = DocumentFile.fromTreeUri(getApplicationContext(), getUri());
                                        String[] parts = filepath.split("/");
                                        for (String part : parts) {
                                            DocumentFile nextfile = null;
                                            if (documentFile != null) {
                                                nextfile = documentFile.findFile(part);
                                            }
                                            if (nextfile != null) {
                                                documentFile = nextfile;
                                            }
                                        }
                                        if (documentFile != null && documentFile.isFile()) {
                                            OutputStream out = getContentResolver().openOutputStream(documentFile.getUri());
                                            out.write(bytes);
                                            out.close();
                                        } else {
                                            throw new Exception("File Not Found");
                                        }

                                        result.success(true);
                                    } catch (Exception e) {
//                                        Log.e("error res", e.getMessage());
                                        result.error("400", e.getMessage(), e);

                                        return;
                                    }
//                                
                                    break;
                            }

                        });
    }


    public void takeCardUriPermission(String storageType) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {

            StorageManager storageManager = (StorageManager) getSystemService(Context.STORAGE_SERVICE);
            List<StorageVolume> list =
                    storageManager.getStorageVolumes();
            StorageVolume storageVolume = null;
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).getDescription(this).toLowerCase()
                        .contains(storageType)) {
                    storageVolume = list.get(i);
                }
//                Log.e("Path", list.get(i).getDescription(this));

            }
            if (storageVolume != null) {
//                Log.e("sdCard", storageVolume.getDescription(this));
                Intent intent = storageVolume.createOpenDocumentTreeIntent();
                try {
                    startActivityForResult(intent, 4010);
                } catch (ActivityNotFoundException e) {
//                    Log.e("TUNE-IN ANDROID", "takeCardUriPermission: " + e);
                }
            }
        }
    }

    // this will pass the sdcard location to the flutter side
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 4010) {

            Uri uri = data.getData();

            grantUriPermission(getPackageName(), uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);

            final int takeFlags = data.getFlags() & (Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);

            getContentResolver().takePersistableUriPermission(uri, takeFlags);

            String path = getUri().toString();
//            Log.e("choosen", path);
            methodChannel.invokeMethod("resolveWithSDCardUri", path);
        }
    }

    //    this will always returns the choosen path, will be using this
//    in the on onActivityResult method
    public Uri getUri() {
        List<UriPermission> persistedUriPermissions = getContentResolver().getPersistedUriPermissions();
        if (persistedUriPermissions.size() > 0) {
            UriPermission uriPermission = persistedUriPermissions.get(0);
            return uriPermission.getUri();
        }
        return null;
    }

}
