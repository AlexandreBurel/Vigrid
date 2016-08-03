-- MySQL dump 10.13  Distrib 5.1.73, for redhat-linux-gnu (x86_64)
--
-- Host: localhost    Database: mongrid
-- ------------------------------------------------------
-- Server version	5.1.73-log

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `monce`
--

DROP TABLE IF EXISTS `monce`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `monce` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `host` varchar(255) NOT NULL,
  `path` varchar(255) NOT NULL,
  `implementation` varchar(255) NOT NULL DEFAULT 'cream',
  `date` datetime NOT NULL,
  `state` varchar(255) NOT NULL,
  `temps` int(11) NOT NULL DEFAULT '0',
  `msg` text,
  `jobid` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `state` (`state`),
  KEY `date` (`date`)
) ENGINE=MyISAM AUTO_INCREMENT=1264903 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping routines for database 'mongrid'
--
/*!50003 DROP PROCEDURE IF EXISTS `LastBestCe` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8 */ ;
/*!50003 SET character_set_results = utf8 */ ;
/*!50003 SET collation_connection  = utf8_general_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = '' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50020 DEFINER=`biouser`@`%`*/ /*!50003 PROCEDURE `LastBestCe`()
BEGIN
select * from monce c where c.date=(select date from monce 
where monce.id =(select max(id) from monce)) and state="OK" order by temps;

END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP PROCEDURE IF EXISTS `RangeBestCe` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8 */ ;
/*!50003 SET character_set_results = utf8 */ ;
/*!50003 SET collation_connection  = utf8_general_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = '' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50020 DEFINER=`biouser`@`%`*/ /*!50003 PROCEDURE `RangeBestCe`(IN rang INT)
BEGIN
DECLARE done INT;
DECLARE p_path,p_host VARCHAR(255);

select host ,path, count(host)as Nb_ok,sum(temps)  as temps,
(select count(DISTINCT date)from monce m where date > DATE_SUB(NOW(),INTERVAL rang  DAY) 
and m.host=c.host and m.path=c.path)as Nb_req,(select max(DISTINCT date)from monce m where date > DATE_SUB(NOW(),INTERVAL rang DAY) and m.host=c.host and m.path=c.path) as Last_req
from monce c 
where date > DATE_SUB(NOW(),INTERVAL rang DAY) and state="OK" 
group by CONCAT(host,path) order by Nb_ok desc,temps asc;

END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP PROCEDURE IF EXISTS `RangeBestCeWithLastOK` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8 */ ;
/*!50003 SET character_set_results = utf8 */ ;
/*!50003 SET collation_connection  = utf8_general_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = '' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50020 DEFINER=`biouser`@`%`*/ /*!50003 PROCEDURE `RangeBestCeWithLastOK`(IN rang  int,IN thr_temps int)
BEGIN
DROP  TEMPORARY TABLE  IF EXISTS ce ;
CREATE TEMPORARY Table ce AS
 select host,path from monce c where c.date=(select date from monce 
 where monce.id =(select max(id) from monce)) AND state="OK" order by temps;
select host ,path, count(host)as Nb_ok,sum(temps)  as temps,
(select count(DISTINCT date)from monce m 
where date > DATE_SUB(NOW(),INTERVAL rang  DAY) and m.host=c.host and m.path=c.path)as Nb_req ,(select now()) as Last_req from monce c 
JOIN ce USING(host,path) where date > DATE_SUB(NOW(),INTERVAL rang DAY) and state="OK" group by CONCAT(host,path) order by Nb_ok desc,temps asc;

END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP PROCEDURE IF EXISTS `RangeBestCeWithLastOKandTime` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8 */ ;
/*!50003 SET character_set_results = utf8 */ ;
/*!50003 SET collation_connection  = utf8_general_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = '' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50020 DEFINER=`biouser`@`localhost`*/ /*!50003 PROCEDURE `RangeBestCeWithLastOKandTime`(IN rang  int,IN thr_temps int)
BEGIN
DROP  TEMPORARY TABLE  IF EXISTS ce ;
CREATE TEMPORARY Table ce AS
 select host,path from monce c where c.date=(select date from monce 
 where monce.id =(select max(id) from monce)) AND state="OK" AND temps <= thr_temps order by temps;
select host ,path, count(host)as Nb_ok,sum(temps)  as temps,
(select count(DISTINCT date)from monce m 
where date > DATE_SUB(NOW(),INTERVAL rang  DAY) and m.host=c.host and m.path=c.path)as Nb_req ,(select now()) as Last_req from monce c 
JOIN ce USING(host,path) where date > DATE_SUB(NOW(),INTERVAL rang DAY) and state="OK" group by CONCAT(host,path) order by Nb_ok desc,temps asc;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2016-08-03  8:47:20
