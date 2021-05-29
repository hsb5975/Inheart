package com.bluetag.inheart.SDKSample;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bluetag.inheart.MainActivity;
import com.bluetag.inheart.R;

import java.util.ArrayList;

public class ExpandableListAdapter extends BaseExpandableListAdapter {
    private Context context;
    private ArrayList<ItemGroup> item_groups;
    private BLEManager ble_manager;

    public ExpandableListAdapter(Context context, ArrayList<ItemGroup> item_groups) {
        this.context = context;
        this.item_groups = item_groups;
        this.ble_manager = ((MainActivity)context).getBLEManager();
    }

    @Override
    public View getGroupView(int groupPos, boolean isExpanded, View view, ViewGroup viewGroup) {
        ItemGroup itemGroup = getGroup(groupPos);

        if(view == null) {
            LayoutInflater inflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.group, viewGroup, false);
        }

        TextView tv_title = view.findViewById(R.id.title);
        tv_title.setText(itemGroup.getTitle());

        return view;
    }

    @Override
    public View getChildView(int groupPos, int childPos, boolean isExpanded, View view, ViewGroup viewGroup) {
        final Item item = getChild(groupPos, childPos);

        if(view == null) {
            LayoutInflater inflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.child, viewGroup, false);
        }

        // item click event listener
        if(item.getBehavior().equals("")) {
            view.setOnClickListener(null);
        } else {
            view.setOnClickListener(new ItemOnClickListener(context, item, ble_manager));
        }

        // title
        TextView tv_title = view.findViewById(R.id.title);
        tv_title.setText(item.getTitle());

        // title: Bluetooth icon
        ImageView iv_bt_logo = view.findViewById(R.id.iv_bt_logo);
        if(item.getBehavior().startsWith(context.getString(R.string.PREFIX_BLESTD))) {
            iv_bt_logo.setVisibility(View.VISIBLE);
        } else {
            iv_bt_logo.setVisibility(View.GONE);
        }

        return view;
    }


    @Override
    public int getGroupCount() {
        return item_groups.size();
    }

    @Override
    public int getChildrenCount(int groupPos) {
        return item_groups.get(groupPos).getItems().size();
    }

    @Override
    public ItemGroup getGroup(int groupPos) {
        return item_groups.get(groupPos);
    }

    @Override
    public Item getChild(int groupPos, int childPos) {
        return item_groups.get(groupPos).getItems().get(childPos);
    }

    @Override
    public long getGroupId(int i) {
        return i;
    }

    @Override
    public long getChildId(int i, int i1) {
        return i1;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isChildSelectable(int i, int i1) {
        return true;
    }
}
