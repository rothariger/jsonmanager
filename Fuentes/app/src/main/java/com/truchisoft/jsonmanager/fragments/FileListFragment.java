package com.truchisoft.jsonmanager.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.truchisoft.jsonmanager.R;
import com.truchisoft.jsonmanager.adapters.FileListAdapter;
import com.truchisoft.jsonmanager.data.StaticData;

/**
 * A placeholder fragment containing a simple view.
 */
public class FileListFragment extends Fragment {

    public FileListFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final FileListAdapter fla = new FileListAdapter(this.getActivity(), StaticData.getFiles());
        View view = inflater.inflate(R.layout.fragment_filelist, container, false);
        ListView lv = (ListView) view.findViewById(R.id.lvFileList);
        final SwipeRefreshLayout srlFileList = (SwipeRefreshLayout) view.findViewById(R.id.srlFileList);
        srlFileList.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                fla.setFiles(StaticData.getFiles());
                srlFileList.setRefreshing(false);
            }
        });
        lv.setAdapter(fla);

        return view;
    }
}
