package com.truchisoft.jsonmanager.utils;

import android.content.ClipData;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.truchisoft.jsonmanager.JsonManagerApp;
import com.truchisoft.jsonmanager.data.FileData;
import com.truchisoft.jsonmanager.data.FileType;
import com.truchisoft.jsonmanager.data.StaticData;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Date;
import java.util.List;

/**
 * Created by Maximiliano.Schmidt on 05/10/2015.
 */
public class FileUtils {
    public static void AddFileToPrefs(File f, Uri uri) {
        FileData fData = new FileData();
        fData.rawUri = uri.toString();
        fData.FileName = f.getAbsolutePath();
        fData.CreationDate = new Date();
        fData.FileType = FileType.Local;
        if (!FileUtils.FileExists(uri))
            StaticData.getFiles().add(fData);
        PrefManager.setFileData(JsonManagerApp.getContext(), StaticData.getFiles());
    }

    public static void AddPathToPrefs(String path, Uri uri) {
        FileData fData = new FileData();
        fData.FileName = path;
        fData.rawUri = uri.toString();
        fData.CreationDate = new Date();
        fData.FileType = FileType.Local;
        if (!FileUtils.FileExists(uri))
            StaticData.getFiles().add(fData);
        PrefManager.setFileData(JsonManagerApp.getContext(), StaticData.getFiles());
    }

    public static void WriteToFile(Uri uri, byte[] data) {
        FileOutputStream outputStream = null;

        try {
            Context ctx = JsonManagerApp.getContext();
            ctx.grantUriPermission(ctx.getPackageName(), uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            ctx.getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            outputStream = new FileOutputStream(ctx.getContentResolver().openFileDescriptor(uri, "rwt").getFileDescriptor());
            outputStream.write(data);
            outputStream.close();
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    public static String ReadFromResource(Uri uri) {
        String ret = "";
        InputStream inputStream = null;
        try {
            Context ctx = JsonManagerApp.getContext();
            ctx.grantUriPermission(ctx.getPackageName(), uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            ctx.getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            inputStream = ctx.getContentResolver().openInputStream(uri);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            int i;
            try {
                i = inputStream.read();
                while (i != -1) {
                    byteArrayOutputStream.write(i);
                    i = inputStream.read();
                }
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            ret = byteArrayOutputStream.toString();
        } catch (FileNotFoundException e) {
            Log.e("ReadFromFile", "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e("ReadFromFile", "Can not read file: " + e.toString());
        } catch (SecurityException e) {

        }

        return ret;
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

    public static String getRealPathFromURI(Context context, Uri uri) {
        if (DocumentsContract.isDocumentUri(context, uri)) {
            if (isExternalStorageDocument(uri)) {
                String docId = DocumentsContract.getDocumentId(uri);
                String[] split = docId.split(":");
                String type = split[0];
                if ("primary".equals(type)) {
                    if (split.length > 1) {
                        return Environment.getExternalStorageDirectory().toString() + "/" + split[1];
                    } else {
                        return Environment.getExternalStorageDirectory().toString() + "/";
                    }
                } else {
                    return "storage" + "/" + docId.replace(":", "/");
                }
            } else if (isDownloadsDocument(uri)) {
                String fileName = getFilePath(context, uri);
                if (fileName != null) {
                    return Environment.getExternalStorageDirectory().toString() + "/Download/" + fileName;
                }
                String id = DocumentsContract.getDocumentId(uri);
                if (id.startsWith("raw:")) {
                    id = id.replaceFirst("raw:", "");
                    File file = new File(id);
                    if (file.exists()) {
                        return id;
                    }
                }
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
                return getDataColumn(context, contentUri, null, null);
            } else if (isMediaDocument(uri)) {
                String docId = DocumentsContract.getDocumentId(uri);
                String[] split = docId.split(":");
                String type = split[0];
                Uri contentUri = null;
                switch (type) {
                    case "image":
                        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                        break;
                    case "video":
                        contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                        break;
                    case "audio":
                        contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                        break;
                }
                String selection = "_id=?";
                String[] selectionArgs = new String[]{split[1]};
                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        } else if ("content".equals(uri.getScheme())) {
            if (isGooglePhotosUri(uri)) {
                return uri.getLastPathSegment();
            } else {
                return getDataColumn(context, uri, null, null);
            }
        } else if ("file".equals(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {
        Cursor cursor = null;
        String column = "_data";
        String[] projection = new String[]{column};
        try {
            if (uri == null) {
                return null;
            }
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    public static String getFilePath(Context context, Uri uri) {
        Cursor cursor = null;
        String[] projection = {
                MediaStore.MediaColumns.DISPLAY_NAME
        };
        try {
            if (uri == null) return null;
            cursor = context.getContentResolver().query(uri, projection, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }
}

