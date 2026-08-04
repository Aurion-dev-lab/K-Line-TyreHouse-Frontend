-- ============================================================================
-- SQLite Data Script: Quick Services & Quick Service Presets
-- Description: Table DDL and INSERT data matching K-Line TyreHouse database schema.
-- ============================================================================

BEGIN TRANSACTION;

-- ----------------------------------------------------------------------------
-- Table Structure: quick_service_presets
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `quick_service_presets`;
CREATE TABLE IF NOT EXISTS `quick_service_presets` (
  `id` TEXT PRIMARY KEY,
  `service` TEXT NOT NULL,
  `price` REAL NOT NULL DEFAULT 0.00,
  `active` INTEGER NOT NULL DEFAULT 1,
  `created_at` DATETIME NOT NULL,
  `icon` TEXT DEFAULT 'fas-bolt',
  `sync_status` INTEGER NOT NULL DEFAULT 0
);

-- ----------------------------------------------------------------------------
-- Dumping Data: quick_service_presets (12 Active Presets)
-- ----------------------------------------------------------------------------
INSERT INTO `quick_service_presets` (`id`, `service`, `price`, `active`, `created_at`, `icon`, `sync_status`) VALUES 
('QSP-001', 'Air Filter Car', 500.00, 1, '2026-07-18 16:57:58', 'fas-wind', 0),
('QSP-002', 'Air Filter Bike', 100.00, 1, '2026-07-18 16:58:06', 'fas-wind', 0),
('QSP-003', 'Air Filter Van', 500.00, 1, '2026-07-18 16:58:59', 'fas-wind', 0),
('QSP-004', 'Car Inspection (General)', 1500.00, 1, '2026-07-18 17:00:42', 'fas-car', 0),
('QSP-005', 'Bike Inspection (General)', 500.00, 1, '2026-07-18 17:01:04', 'fas-car', 0),
('QSP-006', 'Battery Check', 200.00, 1, '2026-07-18 17:02:50', 'fas-bolt', 0),
('QSP-007', 'Injector Cleaning', 500.00, 1, '2026-07-18 17:09:28', 'fas-car', 0),
('QSP-008', 'Car Wash', 1000.00, 1, '2026-07-18 17:10:22', 'fas-water', 0),
('QSP-009', 'Interior Vacuum', 300.00, 1, '2026-07-18 17:16:55', 'fas-car', 0),
('QSP-010', 'Brake Service', 300.00, 1, '2026-07-18 17:26:21', 'fas-wrench', 0),
('QSP-011', 'Wheel Alignment + Balancing', 1000.00, 1, '2026-07-18 17:28:00', 'fas-wrench', 0),
('QSP-012', 'Oil Change', 200.00, 1, '2026-07-18 17:28:14', 'fas-oil-can', 0);

-- ----------------------------------------------------------------------------
-- Table Structure: quick_services
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `quick_services`;
CREATE TABLE IF NOT EXISTS `quick_services` (
  `id` TEXT PRIMARY KEY,
  `service` TEXT DEFAULT NULL,
  `price` REAL NOT NULL DEFAULT 0.00,
  `service_date` DATE DEFAULT NULL,
  `sync_status` INTEGER NOT NULL DEFAULT 0
);

-- ----------------------------------------------------------------------------
-- Dumping Data: quick_services (31 Records)
-- ----------------------------------------------------------------------------
INSERT INTO `quick_services` (`id`, `service`, `price`, `service_date`, `sync_status`) VALUES 
('QS-20260718-001', 'Wheel Alignment + Balancing', 1000.00, '2026-07-18', 0),
('QS-20260718-002', 'Bike Inspection (General)', 500.00, '2026-07-18', 0),
('QS-20260718-003', 'Battery Check', 200.00, '2026-07-18', 0),
('QS-20260718-004', 'Air Filter Van', 500.00, '2026-07-18', 0),
('QS-20260718-005', 'Air Filter Bike', 100.00, '2026-07-18', 0),
('QS-20260718-006', 'Air Filter Car', 500.00, '2026-07-18', 0),
('QS-20260718-007', 'Wheel Alignment + Balancing', 1000.00, '2026-07-18', 0),
('QS-20260718-008', 'Oil Change', 200.00, '2026-07-18', 0),
('QS-20260718-009', 'Interior Vacuum', 300.00, '2026-07-18', 0),
('QS-20260718-010', 'Interior Vacuum', 300.00, '2026-07-18', 0),
('QS-20260718-011', 'Brake Service', 300.00, '2026-07-18', 0),
('QS-20260718-012', 'Bike Inspection (General)', 500.00, '2026-07-18', 0),
('QS-20260718-013', 'Battery Check', 200.00, '2026-07-18', 0),
('QS-20260718-014', 'Injector Cleaning', 500.00, '2026-07-18', 0),
('QS-20260718-015', 'Air Filter Van', 500.00, '2026-07-18', 0),
('QS-20260718-016', 'Air Filter Van', 500.00, '2026-07-18', 0),
('QS-20260718-017', 'Brake Service', 300.00, '2026-07-18', 0),
('QS-20260718-018', 'Air Filter Bike', 100.00, '2026-07-18', 0),
('QS-20260718-019', 'Air Filter Car', 500.00, '2026-07-18', 0),
('QS-20260718-020', 'Air Filter Bike', 100.00, '2026-07-18', 0),
('QS-20260718-021', 'Air Filter Van', 500.00, '2026-07-18', 0),
('QS-20260718-022', 'Car Wash', 1000.00, '2026-07-18', 0),
('QS-20260718-023', 'Air Filter Car', 500.00, '2026-07-18', 0),
('QS-20260718-024', 'Car Wash', 1000.00, '2026-07-18', 0),
('QS-20260718-025', 'Battery Check', 200.00, '2026-07-18', 0),
('QS-20260718-026', 'Battery Check', 200.00, '2026-07-18', 0),
('QS-20260718-027', 'Battery Check', 200.00, '2026-07-18', 0),
('QS-20260718-028', 'Battery Check', 200.00, '2026-07-18', 0),
('QS-20260718-029', 'Battery Check', 200.00, '2026-07-18', 0),
('QS-20260718-030', 'Battery Check', 200.00, '2026-07-18', 0),
('QS-20260718-031', 'Battery Check', 200.00, '2026-07-18', 0);

COMMIT;
