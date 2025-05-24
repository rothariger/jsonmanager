package com.truchisoft.jsonmanager.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;

import com.truchisoft.jsonmanager.JsonManagerApp;
import com.truchisoft.jsonmanager.data.FileData;
import com.truchisoft.jsonmanager.data.FileType;
import com.truchisoft.jsonmanager.data.StaticData;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;

/**
 * Created by Maximiliano.Schmidt on 05/10/2015.
 */
public class FileUtils {

    public static String getDisplayNameFromUri(Context context, Uri uri) {
        if (uri == null) {
            return "untitled.json"; // Or null, depending on desired error handling
        }
        String displayName = null;
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            Cursor cursor = null;
            try {
                cursor = context.getContentResolver().query(uri, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        displayName = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                // Log error, could be SecurityException or others
                Log.e("FileUtils", "Error querying display name for content URI: " + uri, e);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        if (displayName == null) {
            displayName = uri.getLastPathSegment();
        }
        // Basic cleanup for typical file name issues from lastPathSegment
        if (displayName != null) {
            int slashIndex = displayName.lastIndexOf('/');
            if (slashIndex != -1) {
                displayName = displayName.substring(slashIndex + 1);
            }
        }
        if (displayName == null || displayName.isEmpty()) {
            displayName = "untitled.json"; // Default if everything else fails
        }
        return displayName;
    }

    public static void AddUriToPrefs(Context context, String displayName, Uri uri) {
        if (uri == null) return;
        FileData fData = new FileData();
        fData.rawUri = uri.toString();
        fData.FileName = displayName; // Store the display name
        fData.CreationDate = new Date();
        fData.FileType = FileType.Local; // Or determine more accurately if possible

        // Ensure context is not null for PrefManager
        Context appContext = (context != null) ? context.getApplicationContext() : JsonManagerApp.getContext();

        if (!FileUtils.FileExists(uri)) { // FileExists checks StaticData
            StaticData.getFiles().add(fData);
        } else {
            // Optional: Update existing FileData if found, e.g., timestamp or display name
            for (FileData existingFd : StaticData.getFiles()) {
                if (existingFd.rawUri.equals(uri.toString())) {
                    existingFd.FileName = displayName; // Update display name
                    existingFd.CreationDate = new Date(); // Update date
                    break;
                }
            }
        }
        PrefManager.setFileData(appContext, StaticData.getFiles());
    }

    public static boolean WriteToFile(Context context, Uri uri, byte[] data) {
        if (uri == null || context == null) return false;
        OutputStream outputStream = null;
        try {
            // No need to call grantUriPermission here if permissions are handled by the caller (Activity/Fragment)
            // The caller should ensure it has write permission, possibly through ACTION_CREATE_DOCUMENT or persisted permissions.
            // context.getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION); // Caller should manage this
            outputStream = context.getContentResolver().openOutputStream(uri);
            if (outputStream == null) {
                 Log.e("FileUtils", "Failed to open output stream for URI: " + uri);
                 return false;
            }
            outputStream.write(data);
            return true; // Indicate success
        } catch (FileNotFoundException e) {
            Log.e("FileUtils", "File not found for URI (Write): " + uri, e);
            return false;
        } catch (IOException e) {
            Log.e("FileUtils", "IOException during write for URI: " + uri, e);
            return false;
        } catch (SecurityException e) {
            Log.e("FileUtils", "SecurityException during write for URI: " + uri, e);
            return false;
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    Log.e("FileUtils", "Error closing output stream for URI: " + uri, e);
                }
            }
        }
    }

    public static String ReadFromResource(Context context, Uri uri) {
        if (uri == null || context == null) return ""; // Or null
        InputStream inputStream = null;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            // Similar to WriteToFile, caller should manage persistable permissions.
            // context.getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                Log.e("FileUtils", "Failed to open input stream for URI: " + uri);
                return ""; // Or null
            }
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
            }
        } catch (FileNotFoundException e) {
            Log.e("FileUtils", "File not found for URI (Read): " + uri, e);
            return ""; // Or null
        } catch (IOException e) {
            Log.e("FileUtils", "IOException during read for URI: " + uri, e);
            return ""; // Or null
        } catch (SecurityException e) {
            Log.e("FileUtils", "SecurityException during read for URI: " + uri, e);
            return ""; // Or null
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.e("FileUtils", "Error closing input stream for URI: " + uri, e);
                }
            }
        }
        return byteArrayOutputStream.toString();
    }

    public static boolean FileExists(Uri uri) {
        List<FileData> lst = StaticData.getFiles();
        boolean vReturn = false;
        for (FileData fd : lst) {
            if (fd.rawUri.equals(uri.toString()))
                vReturn = true;
        }
        return vReturn;
    }
}
