package com.lzp.capture.test;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.BaseAdapter;

import com.lzp.capture.R;

import java.util.ArrayList;
import java.util.List;

public class MyAdapter extends BaseAdapter {
    List<String> mData = new ArrayList<>();

    {
        mData.add("#FFF68F");
        mData.add("#FF4500");
        mData.add("#FFC125");
        mData.add("#FF4500");
        mData.add("#FFB5C5");
        mData.add("#FF4500");
        mData.add("#EEB422");
        mData.add("#FF4500");
        mData.add("#BFEFFF");
//        mData.add("#FF4500");
//        mData.add("#B3EE3A");
//        mData.add("#FF4500");
//        mData.add("#A52A2A");
//        mData.add("#FF4500");
//        mData.add("#551A8B");
//        mData.add("#FF4500");
//        mData.add("#32CD32");
//        mData.add("#FF4500");
//        mData.add("#00F5FF");
//        mData.add("#FF4500");
//        mData.add("#C4C4C4");
//        mData.add("#FF4500");
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public Object getItem(int position) {
        return mData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_layout, parent, false);
        }
        convertView.setBackgroundColor(Color.parseColor(mData.get(position)));
        return convertView;
    }
}
