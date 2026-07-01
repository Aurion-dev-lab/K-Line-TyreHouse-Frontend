package com.gui.kline.sync.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gui.kline.sync.model.TyreExportRecord;

public interface TyreExportRecordRepository extends JpaRepository<TyreExportRecord, String> {
	TyreExportRecord findByExportId(String exportId);
}

