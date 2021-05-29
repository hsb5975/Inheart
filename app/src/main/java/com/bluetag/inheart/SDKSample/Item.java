package com.bluetag.inheart.SDKSample;

public class Item {
    private String title;
    private String behavior;

    public Item(String title, String behavior) {
        this.title = title;
        this.behavior = behavior;
    }

    String getTitle() {
        return title;
    }

    String getBehavior() { return this.behavior; }
}
