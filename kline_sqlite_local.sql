PRAGMA foreign_keys = OFF;

--
-- Table structure for table `sync_tombstones`
--

DROP TABLE IF EXISTS `sync_tombstones`;
CREATE TABLE `sync_tombstones` (
  `id` INTEGER PRIMARY KEY AUTOINCREMENT,
  `table_name` TEXT NOT NULL,
  `record_id` TEXT NOT NULL,
  `client_deleted_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

--
-- Table structure for table `customers`
--

DROP TABLE IF EXISTS `customers`;
CREATE TABLE `customers` (
  `id` TEXT NOT NULL,
  `name` TEXT NOT NULL,
  `phone` TEXT DEFAULT NULL,
  `created_at` DATETIME NOT NULL,
  `sync_status` TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE (`name`, `phone`)
);

--
-- Dumping data for table `customers`
--

INSERT INTO `customers` VALUES ('014652e6-81c9-11f1-a4a0-4ad00a5d2118','pika','076890890','2026-07-17 15:49:33',0),('03b6dec0-81c8-11f1-a4a0-4ad00a5d2118','mw','0398908902','2026-07-17 15:42:28',0),('1a5eb35e-82c8-11f1-a4a0-4ad00a5d2118','pika','','2026-07-18 22:15:37',0),('3b1cc884-7fd4-11f1-b092-4ad00a5d2118','pika','076367','2026-07-15 04:04:52',0),('3d4812da-7fc0-11f1-b092-4ad00a5d2118','pika','0788908900','2026-07-15 01:41:46',0),('5809eb6a-82c8-11f1-a4a0-4ad00a5d2118','mw','','2026-07-18 22:17:20',0),('59463d16-81ca-11f1-a4a0-4ad00a5d2118','pika','0782002001','2026-07-17 15:59:10',0),('6f7bc674-82c8-11f1-a4a0-4ad00a5d2118','pika','076879','2026-07-18 22:18:00',0),('7857c90a-7fb7-11f1-b092-4ad00a5d2118','pika','0767898900','2026-07-15 00:38:59',0),('79de717a-81c4-11f1-a4a0-4ad00a5d2118','testc','0788908901','2026-07-17 15:17:08',0),('9593fd4a-82c8-11f1-a4a0-4ad00a5d2118','testc','','2026-07-18 22:19:03',0),('9f75014a-7fb9-11f1-b092-4ad00a5d2118','Minoka','0769726890','2026-07-15 00:54:24',0),('a0d9d274-82c8-11f1-a4a0-4ad00a5d2118','testc','322','2026-07-18 22:19:22',0);

--
-- Table structure for table `expenses`
--

DROP TABLE IF EXISTS `expenses`;
CREATE TABLE `expenses` (
  `id` TEXT NOT NULL,
  `expense_date` DATE NOT NULL,
  `description` TEXT NOT NULL,
  `category` TEXT DEFAULT NULL,
  `amount` DECIMAL(12,2) NOT NULL,
  `created_at` DATETIME NOT NULL,
  `sync_status` TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
);

--
-- Table structure for table `products`
--

DROP TABLE IF EXISTS `products`;
CREATE TABLE `products` (
  `id` TEXT NOT NULL,
  `product_code` TEXT DEFAULT NULL,
  `name` TEXT NOT NULL,
  `category` TEXT DEFAULT NULL,
  `buy_price` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  `sell_price` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  `stock` INTEGER NOT NULL DEFAULT 0,
  `updated_at` DATETIME NOT NULL,
  `sync_status` TINYINT(1) NOT NULL DEFAULT 0,
  `minimum_stock_alert` INTEGER DEFAULT 5,
  `image_path` TEXT DEFAULT NULL,
  `brand` TEXT DEFAULT NULL,
  `description` TEXT DEFAULT NULL,
  `vehicle_type` TEXT DEFAULT NULL,
  `material` TEXT DEFAULT NULL,
  `supplier_name` TEXT DEFAULT NULL,
  `created_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE (`name`),
  UNIQUE (`product_code`)
);

--
-- Dumping data for table `products`
--

INSERT INTO `products` VALUES ('100f338b-3119-46ef-b468-8e1eb40f3c04','prd23','test2','Batteries',100.00,200.00,10,'2026-07-18 22:25:27',0,5,NULL,'','','','','','2026-07-18 22:25:27'),('31561dbe-c583-427b-9c6f-18cc648ce9a7','PD3453','Tyre2','Spare Parts',1000.00,1500.00,176,'2026-07-18 22:25:46',0,30,NULL,'bajaj','test','test','rubber','test','2026-07-18 22:04:05');

--
-- Table structure for table `product_images`
--

DROP TABLE IF EXISTS `product_images`;
CREATE TABLE `product_images` (
  `id` TEXT NOT NULL,
  `product_id` TEXT NOT NULL,
  `image_path` TEXT NOT NULL,
  `created_at` DATETIME NOT NULL,
  `sync_status` TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
);

CREATE INDEX IF NOT EXISTS `idx_product_id` ON `product_images` (`product_id`);

--
-- Dumping data for table `product_images`
--

INSERT INTO `product_images` VALUES ('40e68776-7f8b-11f1-b092-4ad00a5d2118','730b2b94-9d65-4968-8a42-0231bc7c9d86','product_images/1784037126531_23e4bda2-8cd6-4393-be4a-270c2cc75546.png','2026-07-14 19:22:29',0),('4c99f2e4-7f89-11f1-b092-4ad00a5d2118','14816369-3ae8-4905-a54f-1961ea8d5f05','product_images/1784036223445_9fe85e58-815f-4b65-b4e0-9a6ab738c8bf.jpeg','2026-07-14 19:08:29',0),('4c9a1148-7f89-11f1-b092-4ad00a5d2118','14816369-3ae8-4905-a54f-1961ea8d5f05','product_images/1784036285389_14816369-3ae8-4905-a54f-1961ea8d5f05.jpg','2026-07-14 19:08:29',0),('4c9a1fe4-7f89-11f1-b092-4ad00a5d2118','14816369-3ae8-4905-a54f-1961ea8d5f05','product_images/1784036292463_14816369-3ae8-4905-a54f-1961ea8d5f05.jpg','2026-07-14 19:08:29',0),('4c9a34e8-7f89-11f1-b092-4ad00a5d2118','14816369-3ae8-4905-a54f-1961ea8d5f05','product_images/1784036300521_14816369-3ae8-4905-a54f-1961ea8d5f05.jpeg','2026-07-14 19:08:29',0),('4c9a4c62-7f89-11f1-b092-4ad00a5d2118','14816369-3ae8-4905-a54f-1961ea8d5f05','product_images/1784036307453_14816369-3ae8-4905-a54f-1961ea8d5f05.jpeg','2026-07-14 19:08:29',0),('7a642f58-82c9-11f1-a4a0-4ad00a5d2118','100f338b-3119-46ef-b468-8e1eb40f3c04','/Users/minoka/Desktop/aurion/K-Line-TyreHouse-Frontend/product_images/1784393707441_c3b355b4-0753-439c-b91f-5e6d0d9c344d.jpeg','2026-07-18 22:25:27',0),('85c8eab4-82c9-11f1-a4a0-4ad00a5d2118','31561dbe-c583-427b-9c6f-18cc648ce9a7','/Users/minoka/Desktop/aurion/K-Line-TyreHouse-Frontend/product_images/1784393745049_31561dbe-c583-427b-9c6f-18cc648ce9a7.jpg','2026-07-18 22:25:46',0);

--
-- Table structure for table `invoices`
--

DROP TABLE IF EXISTS `invoices`;
CREATE TABLE `invoices` (
  `id` TEXT NOT NULL,
  `invoice_id` TEXT DEFAULT NULL,
  `customer` TEXT DEFAULT NULL,
  `invoice_date` DATE DEFAULT NULL,
  `type` TEXT DEFAULT NULL,
  `status` TEXT DEFAULT NULL,
  `subtotal` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  `tax` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  `grand_total` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  `created_at` DATETIME NOT NULL,
  `updated_at` DATETIME DEFAULT NULL,
  `sync_status` TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE (`invoice_id`)
);

--
-- Dumping data for table `invoices`
--

INSERT INTO `invoices` VALUES ('1a5a4ac6-82c8-11f1-a4a0-4ad00a5d2118','INV-12436','pika','2026-07-18','Sale','completed',3120.00,0.00,2820.00,'2026-07-18 22:15:37','2026-07-18 22:18:03',0),('5805d804-82c8-11f1-a4a0-4ad00a5d2118','INV-88623','mw','2026-07-18','Sale','quotation',5299.00,0.00,5267.00,'2026-07-18 22:17:20',NULL,0),('9a9092e0-82c8-11f1-a4a0-4ad00a5d2118','INV1784393352337','testc','2026-07-18','Credit Sale','completed',30000.00,0.00,30000.00,'2026-07-18 22:19:12',NULL,0);

--
-- Table structure for table `invoice_line_items`
--

DROP TABLE IF EXISTS `invoice_line_items`;
CREATE TABLE `invoice_line_items` (
  `id` TEXT NOT NULL,
  `invoice_id` TEXT DEFAULT NULL,
  `invoice_ref` TEXT NOT NULL,
  `product_id` TEXT DEFAULT NULL,
  `description` TEXT DEFAULT NULL,
  `type` TEXT DEFAULT NULL,
  `qty` INTEGER DEFAULT NULL,
  `unit_price` DECIMAL(12,2) DEFAULT NULL,
  `total` DECIMAL(12,2) DEFAULT NULL,
  `created_at` DATETIME NOT NULL,
  `sync_status` TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  FOREIGN KEY (`invoice_ref`) REFERENCES `invoices` (`id`) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS `idx_invoice_line_items_invoice_ref` ON `invoice_line_items` (`invoice_ref`);

--
-- Dumping data for table `invoice_line_items`
--

INSERT INTO `invoice_line_items` VALUES ('58069dac-82c8-11f1-a4a0-4ad00a5d2118','INV-88623','5805d804-82c8-11f1-a4a0-4ad00a5d2118','31561dbe-c583-427b-9c6f-18cc648ce9a7','PD3453 - Tyre2','Sale',2,1500.00,3000.00,'2026-07-18 22:17:20',0),('5806ca20-82c8-11f1-a4a0-4ad00a5d2118','INV-88623','5805d804-82c8-11f1-a4a0-4ad00a5d2118',NULL,'Labour','Service',1,2000.00,2000.00,'2026-07-18 22:17:20',0),('5806f252-82c8-11f1-a4a0-4ad00a5d2118','INV-88623','5805d804-82c8-11f1-a4a0-4ad00a5d2118',NULL,'Additional parts','Service',1,299.00,299.00,'2026-07-18 22:17:20',0),('713dac3e-82c8-11f1-a4a0-4ad00a5d2118','INV-12436','1a5a4ac6-82c8-11f1-a4a0-4ad00a5d2118','31561dbe-c583-427b-9c6f-18cc648ce9a7','PD3453 - Tyre2','Sale',2,1500.00,3000.00,'2026-07-18 22:18:03',0),('713dc5f2-82c8-11f1-a4a0-4ad00a5d2118','INV-12436','1a5a4ac6-82c8-11f1-a4a0-4ad00a5d2118',NULL,'Labour','Service',1,20.00,20.00,'2026-07-18 22:18:03',0),('713df5ae-82c8-11f1-a4a0-4ad00a5d2118','INV-12436','1a5a4ac6-82c8-11f1-a4a0-4ad00a5d2118',NULL,'Additional parts','Service',1,100.00,100.00,'2026-07-18 22:18:03',0),('9a912962-82c8-11f1-a4a0-4ad00a5d2118','INV1784393352337','9a9092e0-82c8-11f1-a4a0-4ad00a5d2118','31561dbe-c583-427b-9c6f-18cc648ce9a7','PD3453 - Tyre2','Sale',20,1500.00,30000.00,'2026-07-18 22:19:12',0);

--
-- Table structure for table `credit_sales`
--

DROP TABLE IF EXISTS `credit_sales`;
CREATE TABLE `credit_sales` (
  `id` TEXT NOT NULL,
  `credit_id` TEXT DEFAULT NULL,
  `customer` TEXT DEFAULT NULL,
  `customer_name` TEXT DEFAULT NULL,
  `sale_date` DATE DEFAULT NULL,
  `due_date` DATE DEFAULT NULL,
  `subtotal` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  `paid_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  `amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  `status` TEXT DEFAULT NULL,
  `created_at` DATETIME NOT NULL,
  `updated_at` DATETIME DEFAULT NULL,
  `sync_status` TINYINT(1) NOT NULL DEFAULT 0,
  `labour` DECIMAL(12,2) DEFAULT 0.00,
  `parts_cost` DECIMAL(12,2) DEFAULT 0.00,
  `discount` DECIMAL(12,2) DEFAULT 0.00,
  `labour_cost` DECIMAL(12,2) DEFAULT 0.00,
  `extra_parts` DECIMAL(12,2) DEFAULT 0.00,
  `discount_amount` DECIMAL(12,2) DEFAULT 0.00,
  PRIMARY KEY (`id`)
);

--
-- Dumping data for table `credit_sales`
--

INSERT INTO `credit_sales` VALUES ('a0d7024c-82c8-11f1-a4a0-4ad00a5d2118','4200',NULL,'testc','2026-07-18','2026-08-17',30020.00,30020.00,0.00,'PAID','2026-07-18 22:19:22',NULL,0,1000.00,20.00,1000.00,0.00,0.00,0.00);

--
-- Table structure for table `credit_sale_parts`
--

DROP TABLE IF EXISTS `credit_sale_parts`;
CREATE TABLE `credit_sale_parts` (
  `id` TEXT NOT NULL,
  `credit_sale_id` TEXT NOT NULL,
  `product_id` TEXT DEFAULT NULL,
  `description` TEXT DEFAULT NULL,
  `quantity` INTEGER DEFAULT NULL,
  `unit_price` DECIMAL(12,2) DEFAULT NULL,
  `total` DECIMAL(12,2) DEFAULT NULL,
  `created_at` DATETIME NOT NULL,
  `sync_status` TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  FOREIGN KEY (`credit_sale_id`) REFERENCES `credit_sales` (`id`) ON DELETE CASCADE,
  FOREIGN KEY (`product_id`) REFERENCES `products` (`id`)
);

CREATE INDEX IF NOT EXISTS `idx_credit_sale_parts_product_id` ON `credit_sale_parts` (`product_id`);
CREATE INDEX IF NOT EXISTS `idx_credit_sale_id` ON `credit_sale_parts` (`credit_sale_id`);

--
-- Dumping data for table `credit_sale_parts`
--

INSERT INTO `credit_sale_parts` VALUES ('a0d8a7b4-82c8-11f1-a4a0-4ad00a5d2118','a0d7024c-82c8-11f1-a4a0-4ad00a5d2118','31561dbe-c583-427b-9c6f-18cc648ce9a7','PD3453 - Tyre2',20,1500.00,30000.00,'2026-07-18 22:19:22',0);

--
-- Table structure for table `quick_service_presets`
--

DROP TABLE IF EXISTS `quick_service_presets`;
CREATE TABLE `quick_service_presets` (
  `id` TEXT NOT NULL,
  `service` TEXT NOT NULL,
  `price` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  `active` TINYINT(1) NOT NULL DEFAULT 1,
  `created_at` DATETIME NOT NULL,
  `icon` TEXT DEFAULT 'fas-bolt',
  `sync_status` TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
);

--
-- Dumping data for table `quick_service_presets`
--

INSERT INTO `quick_service_presets` VALUES 
('07667f7c-9971-4d67-be58-5d6caa54d2a1','Interior Vacuum',300.00,1,'2026-07-18 17:16:55','fas-car',0),
('09dec9d2-20c2-4017-b9c8-018d3d0aa25a','Injector Cleaning',500.00,1,'2026-07-18 17:09:28','fas-car',0),
('0d604094-7a59-49bd-a470-9cef380c1182','Car Wash',1000.00,1,'2026-07-18 17:10:22','fas-water',0),
('1e08ac9c-b6fd-4aa2-8be1-368c27b606f6','Wheel Alignment + Balancing',1000.00,1,'2026-07-18 17:28:00','fas-wrench',0),
('335b3071-5f63-40ef-9bce-fec5f184f384','Brake Service',300.00,1,'2026-07-18 17:26:21','fas-wrench',0),
('396e6dcd-26b5-4261-8f7d-453771b862b3','Oil Change',200.00,1,'2026-07-18 17:28:14','fas-oil-can',0),
('59463184-c062-47d6-bb39-ef5c40a1f72c','Air Filter Car',500.00,1,'2026-07-18 16:57:58','fas-wind',0),
('5ba754af-bd44-42aa-84e2-18a3945ccbed','Air Filter Bike',100.00,1,'2026-07-18 16:58:06','fas-wind',0),
('614bb1c7-5064-4673-9374-b004236829e2','Car Inspection (General)',1500.00,1,'2026-07-18 17:00:42','fas-car',0),
('c81038d8-7140-485b-ba57-0144be55291b','Bike Inspection (General)',500.00,1,'2026-07-18 17:01:04','fas-car',0),
('d6413b5c-3eda-41d0-a722-29721e893864','Battery Check',200.00,1,'2026-07-18 17:02:50','fas-bolt',0),
('f9ebd97f-9e64-4415-aa80-b383536e2b02','Air Filter  Van',500.00,1,'2026-07-18 16:58:59','fas-wind',0);

--
-- Table structure for table `quick_services`
--

DROP TABLE IF EXISTS `quick_services`;
CREATE TABLE `quick_services` (
  `id` TEXT NOT NULL,
  `service` TEXT DEFAULT NULL,
  `price` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  `service_date` DATE DEFAULT NULL,
  `sync_status` TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
);

--
-- Dumping data for table `quick_services`
--

INSERT INTO `quick_services` VALUES ('002101ea-82a0-11f1-a4a0-4ad00a5d2118','Battery Check',200.00,'2026-07-18',0),('00a77c2a-82a0-11f1-a4a0-4ad00a5d2118','Air Filter  Van',500.00,'2026-07-18',0),('0123baba-82a0-11f1-a4a0-4ad00a5d2118','Air Filter Bike',100.00,'2026-07-18',0),('01899f2e-82a0-11f1-a4a0-4ad00a5d2118','Air Filter Car',500.00,'2026-07-18',0),('02367cb2-82a0-11f1-a4a0-4ad00a5d2118','Wheel Alignment + Balancing',1000.00,'2026-07-18',0),('028ceef8-82a0-11f1-a4a0-4ad00a5d2118','Oil Change',200.00,'2026-07-18',0),('02ff8c2e-82a0-11f1-a4a0-4ad00a5d2118','Interior Vacuum',300.00,'2026-07-18',0),('03865dbc-82a0-11f1-a4a0-4ad00a5d2118','Interior Vacuum',300.00,'2026-07-18',0),('087e8060-82a0-11f1-a4a0-4ad00a5d2118','Brake Service',300.00,'2026-07-18',0),('08d21914-82a0-11f1-a4a0-4ad00a5d2118','Bike Inspection (General)',500.00,'2026-07-18',0),('09512772-82a0-11f1-a4a0-4ad00a5d2118','Battery Check',200.00,'2026-07-18',0),('745cfaca-82a3-11f1-a4a0-4ad00a5d2118','Injector Cleaning',500.00,'2026-07-18',0),('78dac55a-82a3-11f1-a4a0-4ad00a5d2118','Air Filter  Van',500.00,'2026-07-18',0),('79ce0d1e-82a3-11f1-a4a0-4ad00a5d2118','Air Filter  Van',500.00,'2026-07-18',0),('869f5444-82a3-11f1-a4a0-4ad00a5d2118','Brake Service',300.00,'2026-07-18',0),('8a2836bc-82a3-11f1-a4a0-4ad00a5d2118','Air Filter Bike',100.00,'2026-07-18',0),('8b163cc2-82a3-11f1-a4a0-4ad00a5d2118','Air Filter Car',500.00,'2026-07-18',0),('8bad2510-82a3-11f1-a4a0-4ad00a5d2118','Air Filter Bike',100.00,'2026-07-18',0),('8e07e1ec-82a3-11f1-a4a0-4ad00a5d2118','Air Filter  Van',500.00,'2026-07-18',0),('9438eae8-82a3-11f1-a4a0-4ad00a5d2118','Car Wash',1000.00,'2026-07-18',0),('97481100-82a3-11f1-a4a0-4ad00a5d2118','Air Filter Car',500.00,'2026-07-18',0),('f0064f96-82a3-11f1-a4a0-4ad00a5d2118','Car Wash',1000.00,'2026-07-18',0),('f2dc245c-82a3-11f1-a4a0-4ad00a5d2118','Battery Check',200.00,'2026-07-18',0),('f2f2831e-82a3-11f1-a4a0-4ad00a5d2118','Battery Check',200.00,'2026-07-18',0),('f3097844-82a3-11f1-a4a0-4ad00a5d2118','Battery Check',200.00,'2026-07-18',0),('f32084a8-82a3-11f1-a4a0-4ad00a5d2118','Battery Check',200.00,'2026-07-18',0),('f33732d4-82a3-11f1-a4a0-4ad00a5d2118','Battery Check',200.00,'2026-07-18',0),('f35113ac-82a3-11f1-a4a0-4ad00a5d2118','Battery Check',200.00,'2026-07-18',0),('f36f1ece-82a3-11f1-a4a0-4ad00a5d2118','Battery Check',200.00,'2026-07-18',0),('fe129de6-829f-11f1-a4a0-4ad00a5d2118','Wheel Alignment + Balancing',1000.00,'2026-07-18',0),('ff3952be-829f-11f1-a4a0-4ad00a5d2118','Bike Inspection (General)',500.00,'2026-07-18',0);

--
-- Table structure for table `services`
--

DROP TABLE IF EXISTS `services`;
CREATE TABLE `services` (
  `id` TEXT NOT NULL,
  `name` TEXT DEFAULT NULL,
  `price` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  `service_date` DATE DEFAULT NULL,
  `remark` TEXT DEFAULT NULL,
  `sync_status` TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
);

--
-- Table structure for table `tyre_exports`
--

DROP TABLE IF EXISTS `tyre_exports`;
CREATE TABLE `tyre_exports` (
  `id` TEXT NOT NULL,
  `export_id` TEXT DEFAULT NULL,
  `operation` TEXT DEFAULT NULL,
  `serial_number` TEXT DEFAULT NULL,
  `company` TEXT DEFAULT NULL,
  `tyres` INTEGER NOT NULL DEFAULT 0,
  `cust_price` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  `comp_price` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  `service_fee` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  `paid_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  `total_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  `balance_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  `payment_status` TEXT DEFAULT NULL,
  `status` TEXT DEFAULT NULL,
  `export_date` DATE DEFAULT NULL,
  `notes` TEXT DEFAULT NULL,
  `created_by` TEXT DEFAULT NULL,
  `updated_by` TEXT DEFAULT NULL,
  `created_at` DATETIME DEFAULT NULL,
  `updated_at` DATETIME DEFAULT NULL,
  `sync_status` TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE (`export_id`)
);

--
-- Dumping data for table `tyre_exports`
--

INSERT INTO `tyre_exports` VALUES ('5cb66a7c-6ba1-4b97-a5d1-c7f6ef9aeb95','EXP-480B8822','create','001','test',2,1000.00,300.00,2000.00,4000.00,4000.00,0.00,'PAID','PAID','2026-07-18',NULL,NULL,NULL,'2026-07-18 22:14:52','2026-07-18 22:15:00',0);

--
-- Table structure for table `workers`
--

DROP TABLE IF EXISTS `workers`;
CREATE TABLE `workers` (
  `id` TEXT NOT NULL,
  `name` TEXT NOT NULL,
  `phone` TEXT DEFAULT NULL,
  `role` TEXT DEFAULT NULL,
  `rate` TEXT DEFAULT NULL,
  `created_at` DATETIME NOT NULL,
  `sync_status` TINYINT(1) NOT NULL DEFAULT 0,
  `salary_type` TEXT DEFAULT NULL,
  PRIMARY KEY (`id`)
);

--
-- Dumping data for table `workers`
--

INSERT INTO `workers` VALUES ('5e5cac54-82c4-11f1-a4a0-4ad00a5d2118','Minoka','0769726890','Mechanic','1000','2026-07-18 21:48:53',0,NULL),('6d2c43a2-82c4-11f1-a4a0-4ad00a5d2118','Amila','078 8908908','Mechanic','2000','2026-07-18 21:49:18',0,NULL);

--
-- Table structure for table `salary_advances`
--

DROP TABLE IF EXISTS `salary_advances`;
CREATE TABLE `salary_advances` (
  `id` TEXT NOT NULL,
  `worker` TEXT DEFAULT NULL,
  `amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  `advance_date` DATE DEFAULT NULL,
  `sync_status` TINYINT(1) NOT NULL DEFAULT 0,
  `worker_id` TEXT DEFAULT NULL,
  `note` TEXT DEFAULT NULL,
  `created_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`)
);

--
-- Dumping data for table `salary_advances`
--

INSERT INTO `salary_advances` VALUES ('9ece9a76-3103-4a2e-b973-af80725d4cb2','suneetha',200.00,'2026-07-14',0,'29d1d3f0-7f8c-11f1-b092-4ad00a5d2118','','2026-07-14 19:43:14'),('a54d167e-730a-4d5c-901b-389064ee5a8e','Amila',3000.00,'2026-07-18',0,'6d2c43a2-82c4-11f1-a4a0-4ad00a5d2118','test','2026-07-18 21:49:48');

--
-- Table structure for table `salary_payments`
--

DROP TABLE IF EXISTS `salary_payments`;
CREATE TABLE `salary_payments` (
  `id` TEXT NOT NULL,
  `worker_id` TEXT NOT NULL,
  `worker` TEXT NOT NULL,
  `period_from` DATE NOT NULL,
  `period_to` DATE NOT NULL,
  `amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  `paid_at` DATETIME NOT NULL,
  `sync_status` TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
);

--
-- Dumping data for table `salary_payments`
--

INSERT INTO `salary_payments` VALUES ('7b476e05-9d5a-413c-827f-f11a4aff43ab','5e5cac54-82c4-11f1-a4a0-4ad00a5d2118','Minoka','2026-07-01','2026-07-31',1800.00,'2026-07-18 22:28:19',0),('8ac0c0f2-7291-4d12-8f47-2a5c7b5aafa4','6d2c43a2-82c4-11f1-a4a0-4ad00a5d2118','Amila','2026-07-01','2026-07-31',2000.00,'2026-07-18 22:28:21',0),('c98e8e19-c4eb-4a50-ae98-d0126f59ec26','5e5cac54-82c4-11f1-a4a0-4ad00a5d2118','Minoka','2026-07-01','2026-07-31',1200.00,'2026-07-18 22:28:17',0);

--
-- Table structure for table `worker_attendance`
--

DROP TABLE IF EXISTS `worker_attendance`;
CREATE TABLE `worker_attendance` (
  `id` TEXT NOT NULL,
  `worker_id` TEXT NOT NULL,
  `attendance_date` DATE NOT NULL,
  `status` TEXT NOT NULL,
  `created_at` DATETIME NOT NULL,
  `updated_at` DATETIME NOT NULL,
  `sync_status` TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE (`worker_id`, `attendance_date`)
);

--
-- Dumping data for table `worker_attendance`
--

INSERT INTO `worker_attendance` VALUES ('6e17b940-82c4-11f1-a4a0-4ad00a5d2118','5e5cac54-82c4-11f1-a4a0-4ad00a5d2118','2026-07-18','PRESENT','2026-07-18 21:49:19','2026-07-18 21:49:19',0),('6efdaea0-82c4-11f1-a4a0-4ad00a5d2118','6d2c43a2-82c4-11f1-a4a0-4ad00a5d2118','2026-07-18','HALF_DAY','2026-07-18 21:49:21','2026-07-18 21:49:21',0),('9705af6a-82c4-11f1-a4a0-4ad00a5d2118','6d2c43a2-82c4-11f1-a4a0-4ad00a5d2118','2026-07-17','PRESENT','2026-07-18 21:50:28','2026-07-18 21:50:28',0),('97b54c4a-82c4-11f1-a4a0-4ad00a5d2118','5e5cac54-82c4-11f1-a4a0-4ad00a5d2118','2026-07-17','PRESENT','2026-07-18 21:50:29','2026-07-18 21:50:29',0),('9a8f2238-82c4-11f1-a4a0-4ad00a5d2118','6d2c43a2-82c4-11f1-a4a0-4ad00a5d2118','2026-07-16','PRESENT','2026-07-18 21:50:34','2026-07-18 21:50:34',0),('9aed1c6c-82c4-11f1-a4a0-4ad00a5d2118','5e5cac54-82c4-11f1-a4a0-4ad00a5d2118','2026-07-16','PRESENT','2026-07-18 21:50:34','2026-07-18 21:50:34',0);

--
-- Table structure for table `worker_credits`
--

DROP TABLE IF EXISTS `worker_credits`;
CREATE TABLE `worker_credits` (
  `id` TEXT NOT NULL,
  `worker` TEXT DEFAULT NULL,
  `amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  `credit_type` TEXT DEFAULT NULL,
  `credit_date` DATE DEFAULT NULL,
  `sync_status` TINYINT(1) NOT NULL DEFAULT 0,
  `worker_id` TEXT DEFAULT NULL,
  `note` TEXT DEFAULT NULL,
  `created_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`)
);

PRAGMA foreign_keys = ON;
