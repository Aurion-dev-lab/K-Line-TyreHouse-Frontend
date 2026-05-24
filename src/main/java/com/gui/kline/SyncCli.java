package com.gui.kline;

import com.gui.kline.data.DatabaseManager;
import com.gui.kline.data.SyncQueueRepository;

public class SyncCli {
    public static void main(String[] args) {
        DatabaseManager.init();
        SyncQueueRepository repository = new SyncQueueRepository();
        int pending = repository.countPending();
        System.out.println("Pending records: " + pending);
        System.out.println("DB URL: " + DatabaseManager.getJdbcUrl());
    }
}
