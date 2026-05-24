package com.gui.kline.sync.repo;

import com.gui.kline.sync.model.ProductRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRecordRepository extends JpaRepository<ProductRecord, String> {
}

