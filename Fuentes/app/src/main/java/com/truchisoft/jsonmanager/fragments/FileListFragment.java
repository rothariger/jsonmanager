package com.truchisoft.jsonmanager.fragments;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

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
        final Context ctx = this.getActivity();
        final FileListAdapter fla = new FileListAdapter(ctx, StaticData.getFiles());

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

//        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                String x = view.toString();
//            }
//        });
        return view;
    }
}
