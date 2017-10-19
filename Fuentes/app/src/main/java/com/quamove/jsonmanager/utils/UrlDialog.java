package com.quamove.jsonmanager.utils;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;

import com.quamove.jsonmanager.R;

/**
 * Created by maximiliano.schmidt on 30/10/2015.
 */
public class UrlDialog extends AlertDialog
        implements DialogInterface.OnClickListener {
    private EditText etValue;
    private IDialogResult _IDialogResult;
    private UrlDialog self;

    public interface IDialogResult {
        void OnResult(String Value);
    }

    public UrlDialog(Context context, IDialogResult iDialogResult) {
        super(context);
        _IDialogResult = iDialogResult;
    }

    public UrlDialog(Context context) {
        super(context);
    }

    {
        self = this;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        View v = getLayoutInflater().inflate(R.layout.urldialog, null);

        setInverseBackgroundForced(true);
        setTitle("Enter the URL");
        setView(v);
        setCancelable(true);

        etValue = (EditText) v.findViewById(R.id.urldialog_value);

        this.setButton(BUTTON_POSITIVE, getContext().getText(android.R.string.ok), this);
        this.setButton(BUTTON_NEGATIVE, getContext().getText(android.R.string.cancel), this);

        super.onCreate(savedInstanceState);

        this.getButton(BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                self.onClick(self, BUTTON_POSITIVE);
            }
        });
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case BUTTON_NEGATIVE:
                this.dismiss();
                break;
            case BUTTON_POSITIVE:
                if (!Patterns.WEB_URL.matcher(etValue.getText().toString()).matches()) {
                    etValue.setError("Wrong url");
                    break;
                }
                DownloadString();
                this.dismiss();
                break;
        }
    }

    private void DownloadString() {
        final ProgressDialog progress = ProgressDialog.show(getContext(), "Json Manager",
                "Please Wait...", true);
        final String Url = etValue.getText().toString();
        AsyncTask<Void, Void, String> at = new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String urlResult = Downloader.DownloadString(Url);
                return urlResult.toString();
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);

                if (_IDialogResult != null)
                    _IDialogResult.OnResult(s);
                progress.hide();

            }
        };
        at.execute();
    }
}
