package com.gui.kline.sync.repo;

import com.gui.kline.sync.model.SyncedItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SyncedItemRepository extends JpaRepository<SyncedItem, String> {
}

