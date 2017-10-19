package com.quamove.jsonmanager.data;

import java.io.File;
import java.util.List;

/**
 * Created by Maximiliano.Schmidt on 05/10/2015.
 */
public final class StaticData {
    private static File _CurrentFile;
    private static List<FileData> _files = null;

    public static File getCurrentFile() {
        return _CurrentFile;
    }

    public static void setCurrentFile(File CurrentFile) {
        _CurrentFile = CurrentFile;
    }

    public static List<FileData> getFiles() {
        return _files;
    }

    public static void setFiles(List<FileData> files) {
        _files = files;
    }
}
