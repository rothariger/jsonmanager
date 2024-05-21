package com.truchisoft.jsonmanager.adapters;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.truchisoft.jsonmanager.R;
import com.truchisoft.jsonmanager.data.FileData;
import com.truchisoft.jsonmanager.fragments.EditorFragment;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Created by Maximiliano.Schmidt on 07/10/2015.
 */
public class FileListAdapter extends BaseAdapter {
    private List<FileData> _files;
    private Context _context;

    public FileListAdapter(Context context, List<FileData> files) {
        _context = context;
        _files = files;
    }

    public void setFiles(List<FileData> files) {
        _files = files;
        this.notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        if (_files == null)
            return 0;
        return _files.size();
    }

    @Override
    public Object getItem(int position) {
        return _files.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView tvFileName;
        TextView tvFileCreationDate;
        TextView tvFileType;
        TextView tvFilePath;

        if (convertView == null) {
            convertView = LayoutInflater.from(_context)
                    .inflate(R.layout.filelistitem, parent, false);
            tvFileName = (TextView) convertView.findViewById(R.id.tvFileName);
            tvFileCreationDate = (TextView) convertView.findViewById(R.id.tvFileCreationDate);
            tvFileType = (TextView) convertView.findViewById(R.id.tvFileType);
            tvFilePath = (TextView) convertView.findViewById(R.id.tvFilePath);
            convertView.setTag(R.id.tvFileName, tvFileName);
            convertView.setTag(R.id.tvFileCreationDate, tvFileCreationDate);
            convertView.setTag(R.id.tvFileType, tvFileType);
            convertView.setTag(R.id.tvFilePath, tvFilePath);
        } else {
            tvFileName = (TextView) convertView.getTag(R.id.tvFileName);
            tvFileCreationDate = (TextView) convertView.getTag(R.id.tvFileCreationDate);
            tvFileType = (TextView) convertView.getTag(R.id.tvFileType);
            tvFilePath = (TextView) convertView.getTag(R.id.tvFilePath);
        }

        FileData fd = (FileData) getItem(position);
        final File f = new File(fd.FileName);
        tvFileName.setText(f.getName());
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MMM/yyyy HH:mm:ss");
        tvFileCreationDate.setText(sdf.format(fd.CreationDate));
        tvFileType.setText(fd.FileType.toString());
        tvFilePath.setText(f.getParent());

        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (f.exists()) {
                    FragmentTransaction transaction = ((Activity) _context).getFragmentManager().beginTransaction();
                    transaction.replace(R.id.fragment, EditorFragment.newInstance(f.getAbsolutePath()));
                    transaction.addToBackStack(null);
                    transaction.commit();
                }
            }
        });
        return convertView;
    }
}
