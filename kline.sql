INSERT INTO products (id,product_code,name,category,buy_price,sell_price,stock,minimum_stock_alert,brand,description,vehicle_type,material,supplier_name,image_paths,created_at,updated_at,sync_status) VALUES
	 ('PRD-PK-AXE6HKMA','PRD-2371','Bike Tyre','Tyres',4000.0,5500.0,44,5,'','','','','','["C:\\Users\\Lenovo\\K-Line-Hub\\product_images\\1786270917981_IMG-FED9IWOO.jpg"]','2026-08-09T15:52:12.165641300','2026-08-09T10:26:25',0),
	 ('PRD-PK-G07C4I36','PRD-76413','Wrench 12 inch','Accessories',400.0,600.0,35,5,'','','','','','["C:\\Users\\Lenovo\\K-Line-Hub\\product_images\\1786270964872_IMG-VW0Z8SSZ.jpg"]','2026-08-09T15:52:55.258900600','2026-08-09T10:26:25',0);

INSERT INTO credit_customers (id,name,phone,email,address,created_at,updated_at,sync_status) VALUES
	 ('CST-NC411QWQ','Miller Enterprises','0771658034','miller@gmail.com','123, main road, kegalle','2026-08-09T10:25:48','2026-08-09T10:25:48',0);

INSERT INTO workers (id,name,phone,"role",rate,salary_type,created_at,sync_status) VALUES
	 ('WRK-98YCGH','Jonathan','0713972223','Mechanic','1500',NULL,'2026-08-09T10:29:10',0);

INSERT INTO expenses (id,expense_date,description,category,amount,created_at,sync_status) VALUES
	 ('EXP1786271306383','2026-08-09','Engine Oil','Supplies',10000.0,'2026-08-09 10:28:26',0),
	 ('EXP1786271323858','2026-08-09','Tyre Export','Transport',5000.0,'2026-08-09 10:28:43',0);

INSERT INTO quick_service_presets (id,service,price,active,icon,created_at,sync_status) VALUES
	 ('PRE-V8QLLYU2','Air Fill',100.0,1,'fas-wrench','2026-08-09 10:30:14',0),
	 ('PRE-BPPC8MI8','Oil Change',500.0,1,'fas-oil-can','2026-08-09 10:30:28',0);

INSERT INTO invoices (id,invoice_id,customer,phone,description,vehicle_number,invoice_date,"type",status,subtotal,grand_total,line_items,created_at,updated_at,sync_status) VALUES
	 ('INV-PK-9R1ES99E','INV-6ZKB6KB7','Kumara','0771658034','','','2026-08-09','Sale','completed',22000.0,22000.0,'[{"productId":"PRD-PK-AXE6HKMA","qty":4,"unitPrice":5500.0,"total":22000.0,"description":"PRD-2371 - Bike Tyre"}]','2026-08-09T10:23:25','2026-08-09T10:23:36',0),
	 ('INV-PK-FH9UPZLZ','INV-DGJKFPHG','Asanka','0773972232','Engine Oil and Tyre Change','WP-7328','2026-08-09','Service','completed',14500.0,14000.0,'[{"productId":"PRD-PK-AXE6HKMA","qty":2,"unitPrice":5500.0,"total":11000.0,"description":"PRD-2371 - Bike Tyre"},{"productId":"Labour","qty":1,"unitPrice":2000.0,"total":2000.0,"description":"Labour"},{"productId":"Additional parts","qty":1,"unitPrice":1500.0,"total":1500.0,"description":"Additional parts"}]','2026-08-09T10:24:54','2026-08-09T10:25:05',0);

INSERT INTO credit_sales (id,credit_id,customer_id,sale_date,due_date,sub_total,grand_total,settlement,status,parts,created_at,updated_at,sync_status) VALUES
	 ('CS-PK-VN08O73M','CS-D5C5ULCP','CST-NC411QWQ','2026-08-09','2026-09-08',58000.0,58000.0,28000.0,'PARTIAL','[{"total":55000.0,"description":"PRD-2371 - Bike Tyre","category":"Tyres","quantity":10,"unitPrice":5500.0,"productId":"PRD-PK-AXE6HKMA"},{"total":3000.0,"description":"PRD-76413 - Wrench 12 inch","category":"Accessories","quantity":5,"unitPrice":600.0,"productId":"PRD-PK-G07C4I36"}]','2026-08-09T10:26:25',NULL,0);

INSERT INTO credit_payments (id,credit_id,customer_id,payment_date,amount,payment_method,notes,created_at,sync_status) VALUES
	 ('PAY-CS-IM5Z2DP6','CS-D5C5ULCP','CST-NC411QWQ','2026-08-09',28000.0,'Settlement','Credit sale settlement payment','2026-08-09 10:27:20',0);

INSERT INTO services (id,invoice_id,name,price,service_date,remark,sync_status) VALUES
	 ('SRV-FTFROLQC','INV-DGJKFPHG','Invoiced Service',14000.0,'2026-08-09','Engine Oil and Tyre Change',0);

INSERT INTO tyre_exports (id,export_id,serial_number,company,tyre_size,tyre_make,tyres,cust_price,comp_price,service_fee,sub_total,grand_total,initial_payment,settlement,status,export_date,remark,created_at,updated_at,sync_status) VALUES
	 ('EXP-PK-8HVU0FN4','EXP-8BZYUD4F','SP-43148','Denso','13 * 15','CV',20,5500.0,4500.0,5000.0,110000.0,115000.0,50000.0,50000.0,'PARTIAL','2026-08-09','exported via port','2026-08-09T10:28:05','2026-08-09T10:28:05',0);

INSERT INTO tyre_export_payments (id,export_id,company,payment_date,amount,payment_method,notes,created_at,sync_status) VALUES
	 ('PAY-EX-B3C8ZGOI','EXP-8BZYUD4F','Denso','2026-08-09',50000.0,'Cash','Initial payment balance','2026-08-09 10:28:05',0);

INSERT INTO worker_attendance (id,worker_id,attendance_date,status,created_at,updated_at,sync_status) VALUES
	 ('ATT-G6PK27WI','WRK-98YCGH','2026-08-09','PRESENT','2026-08-09T10:29:12','2026-08-09T10:29:12',0),
	 ('ATT-TF67IA8L','WRK-98YCGH','2026-08-08','HALF_DAY','2026-08-09T10:29:17','2026-08-09T10:29:17',0),
	 ('ATT-BY9WKI15','WRK-98YCGH','2026-08-06','PRESENT','2026-08-09T10:29:22','2026-08-09T10:29:22',0),
	 ('ATT-UECANXAD','WRK-98YCGH','2026-08-04','PRESENT','2026-08-09T10:29:26','2026-08-09T10:29:26',0),
	 ('ATT-SG0B5XLY','WRK-98YCGH','2026-08-02','HALF_DAY','2026-08-09T10:29:29','2026-08-09T10:29:29',0);

INSERT INTO salary_payments (id,worker_id,worker,period_from,period_to,amount,paid_at,sync_status) VALUES
	 ('PAY-UE6O5M8F','WRK-98YCGH','Jonathan','2026-08-01','2026-08-31',6000.0,'2026-08-09T10:29:38',0);

INSERT INTO quick_services (id,service,price,service_date,sync_status) VALUES
	 ('QSV-GSJ5I9JN','Air Fill',100.0,'2026-08-09',0),
	 ('QSV-W3Y6JFIB','Air Fill',100.0,'2026-08-09',0),
	 ('QSV-C8W1US64','Oil Change',500.0,'2026-08-09',0),
	 ('QSV-0WXOKQF2','Oil Change',500.0,'2026-08-09',0);
