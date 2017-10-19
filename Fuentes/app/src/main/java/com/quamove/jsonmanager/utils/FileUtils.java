package com.quamove.jsonmanager.utils;

import android.util.Log;

import com.quamove.fileselector.OnHandleFileListener;
import com.quamove.jsonmanager.JsonManagerApp;
import com.quamove.jsonmanager.data.FileData;
import com.quamove.jsonmanager.data.FileType;
import com.quamove.jsonmanager.data.StaticData;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.List;

/**
 * Created by Maximiliano.Schmidt on 05/10/2015.
 */
public class FileUtils {
    public static String getExtension(String fileName) {
        final String emptyExtension = "";
        if (fileName == null) {
            return emptyExtension;
        }
        int index = fileName.lastIndexOf(".");
        if (index == -1) {
            return emptyExtension;
        }
        return fileName.substring(index + 1);
    }

    public static final String[] FileFilter = {"*.*", ".json", ".txt"};
    public static OnHandleFileListener SaveFileListener = new OnHandleFileListener() {
        @Override
        public void handleFile(final String filePath) {
            File f = CreateFile(filePath);
            AddFileToPrefs(f);
        }
    };

    public static File CreateFile(String filePath) {
        String fullFilename = filePath;
        if (FileUtils.getExtension(filePath) == "") {
            fullFilename += ".json";
        }
        File f = new File(fullFilename);
        if (!f.exists()) {
            try {
                f.createNewFile();
                StaticData.setCurrentFile(f);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return f;
    }

    public static void AddFileToPrefs(File f) {
        FileData fData = new FileData();
        fData.FileName = f.getAbsolutePath();
        fData.CreationDate = new Date();
        fData.FileType = FileType.Local;
        StaticData.getFiles().add(fData);
        PrefManager.setFileData(JsonManagerApp.getContext(), StaticData.getFiles());
    }

    public static void WriteToFile(File f, byte[] data) {
        try {
            FileOutputStream outputStreamWriter = new FileOutputStream(f, false);
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    public static String ReadFromFile(String filename) {
        String ret = "";

        try {
            FileInputStream inputStream = new FileInputStream(filename);

            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ((receiveString = bufferedReader.readLine()) != null) {
                    stringBuilder.append(receiveString);
                }

                inputStream.close();
                ret = stringBuilder.toString();
            }
        } catch (FileNotFoundException e) {
            Log.e("ReadFromFile", "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e("ReadFromFile", "Can not read file: " + e.toString());
        }

        return ret;
    }

    public static boolean FileExists(File f) {
        List<FileData> lst = StaticData.getFiles();
        boolean vReturn = false;
        for (FileData fd : lst) {
            if (fd.FileName.equals(f.getAbsolutePath()))
                vReturn = true;
        }
        return vReturn;
    }
}
