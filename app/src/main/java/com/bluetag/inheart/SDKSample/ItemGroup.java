package com.bluetag.inheart.SDKSample;

import java.util.ArrayList;

public class ItemGroup {
    private String title;
    private ArrayList<Item> items;

    public ItemGroup(String title, ArrayList<Item> items) {
        this.title = title;
        this.items = items;
    }

    String getTitle() {
        return title;
    }

    ArrayList<Item> getItems() {
        return items;
    }
}
