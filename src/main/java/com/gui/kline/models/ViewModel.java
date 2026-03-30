package com.gui.kline.models;

import com.gui.kline.view.ViewFactory;

public enum ViewModel {
    INSTANCE;

    private final ViewFactory viewsFactory;

    ViewModel() {
        this.viewsFactory = new ViewFactory();
    }

    public ViewFactory getViewsFactory() {
        return viewsFactory;
    }
}