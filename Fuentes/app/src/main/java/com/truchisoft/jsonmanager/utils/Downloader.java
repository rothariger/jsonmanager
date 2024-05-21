package com.truchisoft.jsonmanager.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by Maximiliano.Schmidt on 01/11/2015.
 */
public class Downloader {
    private static InputStream getStream(String sUrl) throws IOException {
        URL url;
        try {
            url = new URL(sUrl);
        } catch (MalformedURLException e) {
            url = new URL("http://" + sUrl);
        }

        URLConnection urlConnection = url.openConnection();
        urlConnection.setConnectTimeout(1000);
        return urlConnection.getInputStream();
    }

    public static String DownloadString(String url) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(getStream(url)));
            StringBuilder sb = new StringBuilder();

            String line = null;
            try {
                while ((line = reader.readLine()) != null) {
                    sb.append(line + "\n");
                }
            } catch (Exception e) {
                return "";
            } finally {
                reader.close();
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
