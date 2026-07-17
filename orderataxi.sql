-- MySQL dump 10.13  Distrib 9.7.0, for macos15 (arm64)
--
-- Host: localhost    Database: orderataxi
-- ------------------------------------------------------
-- Server version	9.7.0

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
SET @MYSQLDUMP_TEMP_LOG_BIN = @@SESSION.SQL_LOG_BIN;
SET @@SESSION.SQL_LOG_BIN= 0;

--
-- GTID state at the beginning of the backup 
--

SET @@GLOBAL.GTID_PURGED=/*!80000 '+'*/ '8d569436-4f8c-11f1-a581-1e8e6f7f3381:1-167';

--
-- Table structure for table `documents`
--

DROP TABLE IF EXISTS `documents`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `documents` (
  `id` varchar(255) NOT NULL,
  `owner_id` varchar(255) NOT NULL,
  `type` varchar(255) NOT NULL,
  `file_path` text,
  `uploaded_at` varchar(255) DEFAULT NULL,
  `is_approved` int DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `documents`
--

LOCK TABLES `documents` WRITE;
/*!40000 ALTER TABLE `documents` DISABLE KEYS */;
/*!40000 ALTER TABLE `documents` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `drivers`
--

DROP TABLE IF EXISTS `drivers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `drivers` (
  `id` varchar(255) NOT NULL,
  `name` varchar(255) NOT NULL,
  `email` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `phone` varchar(255) DEFAULT NULL,
  `vehicle_model` varchar(255) DEFAULT NULL,
  `plate_number` varchar(255) DEFAULT NULL,
  `service_type` varchar(255) DEFAULT 'ECONOMY',
  `is_verified` int DEFAULT '0',
  `is_banned` int DEFAULT '0',
  `is_available` int DEFAULT '1',
  `rating_sum` double DEFAULT '5',
  `rating_count` int DEFAULT '1',
  `avg_rating` double DEFAULT '5',
  `status` varchar(255) DEFAULT 'IDLE',
  PRIMARY KEY (`id`),
  UNIQUE KEY `email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `drivers`
--

LOCK TABLES `drivers` WRITE;
/*!40000 ALTER TABLE `drivers` DISABLE KEYS */;
INSERT INTO `drivers` VALUES ('378b2c44-9be2-4994-80bd-bd3218e26ec3','Ayşe Çelik','ayse.celik@gmail.com','123','555-1007','Toyota Corolla','34 TKS 03','ECONOMY',1,0,1,29.114519329638497,6,4.852419888273083,'IDLE'),('44b08b96-b04b-4e29-8ece-93d5a1205f4e','Mustafa Koç','mustafa.koc@gmail.com','123','555-10012','Ford Focus','34 TKS 05','ECONOMY',1,0,1,72.6976767828272,17,4.2763339284016,'IDLE'),('509b81f3-8947-41f9-8916-81c227390463','Mehmet Demir','mehmet.demir@outlook.com','123','555-10059','Renault Megane','34 TKS 02','ECONOMY',1,0,1,47.959282893591144,11,4.359934808508286,'IDLE'),('62bd1b2e-a55d-40c3-b157-e6553e9b7e71','Zeynep Işık','zeynep.isik@outlook.com','123','555-10015','Mercedes Vito','34 XL 88','XL',1,0,1,61.78292150983532,17,3.6342895005785483,'IDLE'),('665ab88c-cae3-4044-870f-0f44a6ff0f9f','Ahmet Yılmaz','ahmet.yilmaz@gmail.com','123','555-10018','Fiat Egea','34 TKS 01','ECONOMY',1,0,1,34.520748177837135,7,4.931535453976734,'IDLE'),('82445f9d-c4ae-4cbd-8445-481cb91732bd','Elif Polat','elif.polat@outlook.com','123','555-10010','Honda Civic','34 TKS 08','ECONOMY',1,0,1,54.4399330231584,11,4.949084820287127,'IDLE'),('8b74ba26-c433-495f-98b3-bd873bfd7686','Kral Sürücü','driver','123','555-9999','Mercedes S-Class','34 KRAL 01','PREMIUM',1,0,1,5,1,5,'IDLE'),('bbf1376c-3795-447a-a7e2-6b570a55aeb2','Fatma Kaya','fatma.kaya@outlook.com','123','555-10071','VW Passat','34 VIP 01','PREMIUM',1,0,1,38.39017404273498,8,4.798771755341872,'IDLE'),('e7e7a557-d4b3-46f3-a22c-b7f5f4c9846e','Cemre Aydın','cemre.aydin@outlook.com','123','555-10014','Skoda Octavia','34 TKS 10','ECONOMY',1,0,1,79.69199199598532,16,4.980749499749082,'IDLE'),('f68a200c-dd0d-45bf-a50e-8b09afc4ad0b','Burak Sahin','burak.sahin@gmail.com','123','555-10082','BMW 320i','34 VIP 99','PREMIUM',1,0,1,59.45474843210164,13,4.5734421870847415,'IDLE'),('f6981fab-cfb8-41e8-b9c0-ee0a2582dae3','Can Yıldız','can.yildiz@gmail.com','123','555-10067','Hyundai i20','34 TKS 07','ECONOMY',1,0,1,36.371859356233735,9,4.041317706248193,'IDLE');
/*!40000 ALTER TABLE `drivers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `payments`
--

DROP TABLE IF EXISTS `payments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payments` (
  `id` varchar(255) NOT NULL,
  `ride_id` varchar(255) NOT NULL,
  `amount` double NOT NULL,
  `method` varchar(255) DEFAULT 'CARD',
  `is_paid` int DEFAULT '0',
  `transaction_id` varchar(255) DEFAULT '',
  `paid_at` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_payments_ride` (`ride_id`),
  CONSTRAINT `fk_payments_ride` FOREIGN KEY (`ride_id`) REFERENCES `rides` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `payments`
--

LOCK TABLES `payments` WRITE;
/*!40000 ALTER TABLE `payments` DISABLE KEYS */;
/*!40000 ALTER TABLE `payments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `rides`
--

DROP TABLE IF EXISTS `rides`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rides` (
  `id` varchar(255) NOT NULL,
  `passenger_id` varchar(255) NOT NULL,
  `driver_id` varchar(255) DEFAULT NULL,
  `start_loc` text,
  `end_loc` text,
  `vehicle_type` varchar(255) DEFAULT 'ECONOMY',
  `status` varchar(255) DEFAULT 'REQUESTED',
  `distance_km` double DEFAULT '0',
  `fare_amount` double DEFAULT '0',
  `estimated_minutes` int DEFAULT '0',
  `pickup_lat` double DEFAULT NULL,
  `pickup_lon` double DEFAULT NULL,
  `pickup_addr` text,
  `dropoff_lat` double DEFAULT NULL,
  `dropoff_lon` double DEFAULT NULL,
  `dropoff_addr` text,
  `scheduled_at` varchar(255) DEFAULT NULL,
  `rating` int DEFAULT '0',
  `feedback` text,
  `passenger_comment` text,
  `tip_amount` double DEFAULT '0',
  `start_time` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_rides_passenger` (`passenger_id`),
  KEY `fk_rides_driver` (`driver_id`),
  CONSTRAINT `fk_rides_driver` FOREIGN KEY (`driver_id`) REFERENCES `drivers` (`id`),
  CONSTRAINT `fk_rides_passenger` FOREIGN KEY (`passenger_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rides`
--

LOCK TABLES `rides` WRITE;
/*!40000 ALTER TABLE `rides` DISABLE KEYS */;
INSERT INTO `rides` VALUES ('14aa5f26-680f-4eeb-840b-3b2d48ad4702','f624558a-9c8c-42ec-ba60-c9c6876d6d69','82445f9d-c4ae-4cbd-8445-481cb91732bd','41,0319, 28,9452','41,0369, 28,9601','ECONOMY','IN_PROGRESS',4.957199999999999,210,12,41.031909531696336,28.94524097442627,'',41.036862404611746,28.960132598876953,'',NULL,5,'','',0,'2026-05-14T16:00:31.473200'),('5d80e09f-b564-4fc7-b398-aab8a6c230c3','f624558a-9c8c-42ec-ba60-c9c6876d6d69','378b2c44-9be2-4994-80bd-bd3218e26ec3','41,0319, 28,9452','41,0369, 28,9601','ECONOMY','IN_PROGRESS',4.957199999999999,234.786,12,41.031909531696336,28.94524097442627,'',41.036862404611746,28.960132598876953,'',NULL,5,'','',0,'2026-05-14T16:01:12.974902'),('fd8d652d-4e36-4794-98a9-639033daf106','f624558a-9c8c-42ec-ba60-c9c6876d6d69','665ab88c-cae3-4044-870f-0f44a6ff0f9f','41,0319, 28,9452','41,0369, 28,9601','ECONOMY','IN_PROGRESS',4.957199999999999,234.786,12,41.031909531696336,28.94524097442627,'',41.036862404611746,28.960132598876953,'',NULL,5,'','',0,'2026-05-14T16:00:51.403604');
/*!40000 ALTER TABLE `rides` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `support_tickets`
--

DROP TABLE IF EXISTS `support_tickets`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `support_tickets` (
  `id` varchar(255) NOT NULL,
  `user_id` varchar(255) NOT NULL,
  `driver_id` varchar(255) DEFAULT NULL,
  `type` varchar(255) NOT NULL,
  `description` text,
  `is_resolved` int DEFAULT '0',
  `created_at` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_support_user` (`user_id`),
  KEY `fk_support_driver` (`driver_id`),
  CONSTRAINT `fk_support_driver` FOREIGN KEY (`driver_id`) REFERENCES `drivers` (`id`),
  CONSTRAINT `fk_support_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `support_tickets`
--

LOCK TABLES `support_tickets` WRITE;
/*!40000 ALTER TABLE `support_tickets` DISABLE KEYS */;
INSERT INTO `support_tickets` VALUES ('c331ffbe-d98e-43f6-a439-1773a0587c22','f624558a-9c8c-42ec-ba60-c9c6876d6d69','665ab88c-cae3-4044-870f-0f44a6ff0f9f','FARE_REVIEW','SUPPORT TİCKET DENEME',0,'2026-05-14T16:01:06.837145');
/*!40000 ALTER TABLE `support_tickets` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` varchar(255) NOT NULL,
  `name` varchar(255) NOT NULL,
  `email` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `phone` varchar(255) DEFAULT NULL,
  `role` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES ('7a3c5a2b-b986-4af3-93b0-b96f089c2569','Test Passenger','test','123','555-0000','PASSENGER'),('f624558a-9c8c-42ec-ba60-c9c6876d6d69','Ertan','ertanyolcu1@example.com','123','+90 555 555 55 55','PASSENGER');
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;
SET @@SESSION.SQL_LOG_BIN = @MYSQLDUMP_TEMP_LOG_BIN;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-05-14 16:11:01
