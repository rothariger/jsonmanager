package com.quamove.jsonmanager.utils;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.quamove.jsonmanager.data.FileData;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Maximiliano.Schmidt on 05/10/2015.
 */
public final class PrefManager {

    public static List<FileData> getFileData(Context ctx) {
        String values = ctx.getSharedPreferences("JsonManager", Context.MODE_PRIVATE).getString("FileList", "");
        List<FileData> _files = new ArrayList<>();
        if (!values.isEmpty()) {
            Gson gson = new GsonBuilder().create();
            Type listType = new TypeToken<ArrayList<FileData>>() {
            }.getType();
            _files = new Gson().fromJson(values, listType);
        }
        return _files;
    }

    public static void setFileData(Context ctx, List<FileData> files) {
        Type listType = new TypeToken<ArrayList<FileData>>() {
        }.getType();
        Gson gson = new GsonBuilder().create();
        String json = gson.toJson(files, listType);
        ctx.getSharedPreferences("JsonManager", Context.MODE_PRIVATE).edit().putString("FileList", json).commit();
    }
}
