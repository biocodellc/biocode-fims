/* Currently, users and roles are handled in the BCID system */

CREATE TABLE `manager` (
   `manager_id` int(11) NOT NULL AUTO_INCREMENT,
   `shortname` VARCHAR(50) NOT NULL,
   `abstract` text,
   `xml_validation_url` VARCHAR(2083),
   `db_location_url` VARCHAR(2083),
   `sample_key` VARCHAR(2083),
   `event_key` VARCHAR(2083),
   `information_artificat_key` VARCHAR(2083)
)

