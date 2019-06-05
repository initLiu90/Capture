package com.lzp.capture.test;

import android.graphics.Point;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lzp.capture.CaptureUtil;
import com.lzp.capture.R;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    ListView mListView;
    MyAdapter mAdapter;

    ScrollView mScrollView;

    RecyclerView mRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        mListView = findViewById(R.id.listview);
//        mAdapter = new MyAdapter();
//        mListView.setAdapter(mAdapter);

//        mScrollView = findViewById(R.id.scrollView);

        mRecyclerView = findViewById(R.id.recyclerview);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        mRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        mRecyclerView.setAdapter(new MyRecyclerViewAdapter());

        findViewById(R.id.capture).setOnClickListener(v -> test());
    }

    private void test() {
        Point point = new Point();
        getWindowManager().getDefaultDisplay().getSize(point);

        File file = new File(getExternalFilesDir("capture"), "111.jpg");

        CaptureUtil.Logo logo = new CaptureUtil.Logo(getResources().getDrawable(R.drawable.yp_label_new), 200, 200, 100, 150);

        CaptureUtil.capture(mRecyclerView, point.x, point.y, null, file, 80, logo);

        Toast.makeText(this, "success", Toast.LENGTH_SHORT).show();
    }
}

