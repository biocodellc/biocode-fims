/* Currently, users and roles are handled in the BCID system */
DROP TABLE IF EXISTS `project`;
CREATE TABLE `project` (
    `project_id` int(11) NOT NULL AUTO_INCREMENT,
    `shortname` VARCHAR(50),
    `abstract` text,
    `xml_validation_url` VARCHAR(2083),
    `db_location_url` VARCHAR(2083),
    `agent_key` VARCHAR(2083),
    `specimen_key` VARCHAR(2083),
    `tissue_key` VARCHAR(2083),
    `event_key` VARCHAR(2083),
    `information_artificat_key` VARCHAR(2083),
     UNIQUE KEY `project_id` (`project_id`)
) ENGINE=InnoDb DEFAULT CHARSET=UTF8;
INSERT INTO project (project_id,specimen_key,event_key,tissue_key) VALUES (1,'ark:/21547/R2','ark:/21547/S2','ark:/21547/Q2');

DROP TABLE IF EXISTS `specimen`;
CREATE TABLE `specimen` (
    `specimen_id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Internal Key',
    `project_id` int(11) NOT NULL,
    `event_id` int(11) NOT NULL,
    `local_id` varchar(128) DEFAULT NULL UNIQUE,
    `dateEntered` date DEFAULT NULL,
    `dateLastModified` date DEFAULT NULL,
    `recordedByAgent` varchar(128) DEFAULT NULL,
    `modifiedByAgent` varchar(128) DEFAULT NULL,
    `individualCount` int(11) DEFAULT NULL,
    `sex` varchar(128) DEFAULT NULL,
    `lifeStage` varchar(128) DEFAULT NULL,
    `tissue_barcode` varchar(128) DEFAULT NULL,
    `plate_name` varchar(64) DEFAULT NULL,
    `well_number` varchar(16) DEFAULT NULL,
    `tissue_num` int(11) DEFAULT NULL COMMENT 'DO I NEED THIS? NOTE, maybe not using RDF',
    UNIQUE KEY `specimen_id` (`specimen_id`)
) ENGINE=InnoDb DEFAULT CHARSET=UTF8;
INSERT INTO specimen
    (specimen_id,project_id,event_id,local_id,dateEntered,dateLastModified,recordedByAgent,modifiedByAgent,sex,plate_name,well_number,tissue_num)
    VALUES (1,1,1,'JD1','2008-10-10','2008-10-12','John Deck','Chris Meyer','Male','Plate_M037','A02',1);
INSERT INTO specimen
    (specimen_id,project_id,event_id,local_id,dateEntered,dateLastModified,recordedByAgent,modifiedByAgent,lifeStage)
    VALUES (2,1,2,'MBIO56','2008-10-10','2008-10-12','John Deck','Chris Meyer','3rd instar');

DROP TABLE IF EXISTS `event`;
CREATE TABLE `event` (
    `event_id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Internal Key',
    `project_id` int(11) NOT NULL,
    `local_id` varchar(128) DEFAULT NULL UNIQUE,
    `dateEntered` date DEFAULT NULL,
    `dateLastModified` date DEFAULT NULL,
    `recordedByAgent` varchar(128) DEFAULT NULL,
    `modifiedByAgent` varchar(128) DEFAULT NULL,
    `year` int(11) DEFAULT NULL,
    `month` int(11) DEFAULT NULL,
    `day` int(11) DEFAULT NULL,
    UNIQUE KEY `event_id` (`event_id`)
) ENGINE=InnoDb DEFAULT CHARSET=UTF8;
INSERT INTO event
    (project_id,local_id,dateEntered,dateLastModified,recordedByAgent,modifiedByAgent,year,month,day)
    VALUES (1,'colevent1','2008-10-10','2008-10-12','John Deck','Chris Meyer',2008,12,1);
INSERT INTO event
    (project_id,local_id,dateEntered,dateLastModified,recordedByAgent,modifiedByAgent,year,month)
    VALUES (1,'colevent2','2008-10-10','2008-10-12','John Deck','Chris Meyer',2008,6);