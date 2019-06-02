package com.lzp.capture.test;

import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.lzp.capture.CaptureUtils;
import com.lzp.capture.R;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    ListView mListView;
    MyAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mListView = findViewById(R.id.listview);
        mAdapter = new MyAdapter();
        mListView.setAdapter(mAdapter);

        findViewById(R.id.capture).setOnClickListener(v -> test());
    }

    private void test() {
        Point point = new Point();
        getWindowManager().getDefaultDisplay().getSize(point);

        File file = new File(getExternalFilesDir("capture"), "111.jpg");

        CaptureUtils.capture(mListView, point.x, point.y, null, file, 80);

        Toast.makeText(this, "success", Toast.LENGTH_SHORT).show();
    }
}

