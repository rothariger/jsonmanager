package com.quamove.fileselector;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class FileSelectorActivity extends Activity {

    /**
     * Sample filters array
     */
    final String[] mFileFilter = {"*.*", ".jpeg", ".txt", ".png"};
    OnHandleFileListener mLoadFileListener = new OnHandleFileListener() {
        @Override
        public void handleFile(final String filePath) {
            Toast.makeText(FileSelectorActivity.this, "Load: " + filePath, Toast.LENGTH_SHORT).show();
        }
    };
    OnHandleFileListener mSaveFileListener = new OnHandleFileListener() {
        @Override
        public void handleFile(final String filePath) {
            Toast.makeText(FileSelectorActivity.this, "Save: " + filePath, Toast.LENGTH_SHORT).show();
        }
    };
    private Button mLoadButton;
    private Button mSaveButton;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mLoadButton = (Button) findViewById(R.id.button_load);
        mSaveButton = (Button) findViewById(R.id.button_save);

        mLoadButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                new FileSelector(FileSelectorActivity.this, FileOperation.LOAD, mLoadFileListener, mFileFilter).show();
            }
        });

        mSaveButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                new FileSelector(FileSelectorActivity.this, FileOperation.SAVE, mSaveFileListener, mFileFilter).show();
            }
        });
    }

}