/**
* SQL for Fims mysql tables
*/

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `users`;

CREATE TABLE `users` (
  `userId` int(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `username` varchar(50) not null primary key,
  `password` varchar(110) not null,
  `enabled` boolean not null DEFAULT '1',
  `email` char(64) DEFAULT NULL,
  `firstName` varchar(60) DEFAULT NULL,
  `lastName` varchar(60) DEFAULT NULL,
  `institution` char(128) DEFAULT NULL,
  `hasSetPassword` boolean not null DEFAULT '0',
  `admin` boolean not null,
  `passwordResetToken` char(20) DEFAULT NULL COMMENT 'Unique token used to reset a users password',
  `passwordResetExpiration` datetime DEFAULT NULL COMMENT 'time when the reset token expires',
   KEY (`userId`)
) ENGINE=Innodb DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `bcids`;

CREATE TABLE `bcids` (
  `bcidId` int(11) NOT NULL AUTO_INCREMENT,
  `ezidMade` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'indicates if EZID has been made',
  `ezidRequest` tinyint(1) NOT NULL DEFAULT '1' COMMENT 'indicates if we want system to request EZID, all bcids by default get an EZID request',
  `suffixPassthrough` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'indicates if we want to use suffixPassthrough for this identifier',
  `finalCopy` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'indicates if this is the final copy of a particular dataset for nmnh projects',
  `internalId` char(36) COLLATE utf8_bin NOT NULL DEFAULT '' COMMENT 'The internal ID for this dataset',
  `identifier` varchar(255) NOT NULL DEFAULT '' COMMENT 'ark:/1234/ab1',
  `userId` int(10) UNSIGNED NOT NULL COMMENT 'who created this data',
  `doi` char(36) COMMENT 'DOI linked to this dataset identifier',
  `title` text COMMENT 'title for this dataset',
  `webAddress` text COLLATE utf8_bin COMMENT 'the target URL for this dataset',
  `graph` text  COMMENT 'A reference to a graph, used by the biocode-fims expedition for storing graph references for a particular dataset',
  `resourceType` text NOT NULL COMMENT 'default resource type for this dataset, stored as a URI',
  `ts` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'timestamp of insertion',
  PRIMARY KEY `bcids_bcidId` (`bcidId`),
  KEY `bcids_userId` (`userId`),
  CONSTRAINT `FK_bcids_userId`  FOREIGN KEY (`userId`) REFERENCES `users` (`userId`)
) ENGINE=Innodb DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `expeditions`;

CREATE TABLE `expeditions` (
  `expeditionId` int(11) NOT NULL AUTO_INCREMENT COMMENT 'The unique, internal key for this expedition',
  `projectId` int(11),
  `internalId` char(36) COLLATE utf8_bin NOT NULL DEFAULT '' COMMENT 'The internal ID for this expedition',
  `expeditionCode` varchar(20) NOT NULL DEFAULT '' COMMENT 'The short name for this expedition',
  `expeditionTitle` varchar(128) NOT NULL DEFAULT '' COMMENT 'Title for this expedition, will be used to populate group title',
  `abstract` text COMMENT 'The abstract for this particular expedition',
  `userId` int(10) DEFAULT NULL COMMENT 'who created this data',
  `ts` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'timestamp of insertion',
  `public` tinyint(1) NOT NULL DEFAULT '1' COMMENT 'Whether or not this is a public expedition',
  UNIQUE KEY `expeditions_expeditionId_idx` (`expeditionId`),
  UNIQUE KEY `expeditions_expeditionCode_projectIdx` (`expeditionCode`,`projectId`),
  KEY `expeditions_projectId_idx` (`projectId`)
  CONSTRAINT `FK_expeditions_userId`  FOREIGN KEY (`userId`) REFERENCES `users` (`userId`),
  CONSTRAINT `FK_expeditions_projectId`  FOREIGN KEY (`projectId`) REFERENCES `projects` (`projectId`),
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `expeditionBcids`;

CREATE TABLE `expeditionBcids` (
  `expeditionBcidId` int(11) NOT NULL AUTO_INCREMENT COMMENT 'The unique, internal key for this element',
  `expeditionId` int(11) NOT NULL COMMENT 'The expeditionId',
  `bcidId` int NOT NULL COMMENT 'The bcidId',
  UNIQUE KEY `expeditionBcids_expeditionBcidId` (`expeditionBcidId`),
  KEY `expeditionBcids_expeditionId` (`expeditionId`),
  KEY `bcidId` (`bcidId`),
  CONSTRAINT `FK_expeditionBcids_bcidId` FOREIGN KEY(`bcidId`) REFERENCES `bcids` (`bcidId`),
  CONSTRAINT `FK_expeditionBcids_expeditionId` FOREIGN KEY(`expeditionId`) REFERENCES `expeditions` (`expeditionId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;


DROP TABLE IF EXISTS `projects`;

CREATE TABLE `projects` (
  `projectId` int(11) NOT NULL AUTO_INCREMENT COMMENT 'The unique, internal key for this projects',
  `projectCode` varchar(6) NOT NULL DEFAULT '' COMMENT 'The short name for this project',
  `projectTitle` varchar(128) NOT NULL DEFAULT '' COMMENT 'Title for this project',
  `validationXml` text COMMENT 'The bioValidator XML Validation Specification, published under the id/schemas webservice',
  `ts` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'timestamp of insertion',
  `userId` int(11) UNSIGNED NOT NULL COMMENT 'The userId of the project admin',
  `public` tinyint(1) NOT NULL DEFAULT '1' COMMENT 'Whether or not this is a public project?',
  UNIQUE KEY `projects_projectId_idx` (`projectId`),
  KEY `projects_userId_idx` (`userId`),
  CONSTRAINT `FK_projects_userId`  FOREIGN KEY (`userId`) REFERENCES `users` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


DROP TABLE IF EXISTS `userProjects`;

CREATE TABLE `userProjects` (
  `userProjectId` int(11) NOT NULL AUTO_INCREMENT COMMENT 'The unique internal key',
  `projectId` int(11) NOT NULL COMMENT 'The project Id',
  `userId` int(11) UNSIGNED NOT NULL COMMENT 'The users id',
  UNIQUE KEY `usersProjects_userProjectId` (`userProjectId`),
  UNIQUE KEY `usersProjects_userId_projectId_idx` (`userId`, `projectId`),
  KEY `usersProjects_projectId` (`projectId`),
  KEY `usersProjects_userId` (`userId`),
  CONSTRAINT `FK_usersProjects_userId`  FOREIGN KEY (`userId`) REFERENCES `users` (`userId`),
  CONSTRAINT `FK_usersProjects_projectId` FOREIGN KEY(`projectId`) REFERENCES `projects` (`projectId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `oAuthClients`;

CREATE TABLE `oAuthClients` (
  `oAuthClientId` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `clientId` char(20) NOT NULL DEFAULT '' COMMENT 'the public unique client id',
  `clientSecret` char(75) NOT NULL DEFAULT '' COMMENT 'the private shared secret',
  `callback` text NOT NULL COMMENT 'The callback url of the client app',
  PRIMARY KEY (`oAuthClientId`),
  UNIQUE KEY `oAuthClients_clientIdx` (`clientId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `oAuthNonces`;

CREATE TABLE `oAuthNonces` (
  `oAuthNonceId` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `clientId` char(20) NOT NULL DEFAULT '' COMMENT 'The clientId of the oAuth client app',
  `code` char(20) NOT NULL DEFAULT '' COMMENT 'The generated code the client app can exchange for an access token',
  `ts` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'timestamp of the nonce creation',
  `userId` int(11) unsigned NOT NULL COMMENT 'The userId this Nonce represents',
  `redirectUri` varchar(256) NOT NULL DEFAULT '' COMMENT 'The redirectUri associated with this code',
  PRIMARY KEY (`oAuthNonceId`),
  UNIQUE KEY `oAuthNonces_code_clientIdx` (`clientId`,`code`),
  KEY `FK_oAuthNonces_userId` (`userId`),
  CONSTRAINT `FK_oAuthNonces_clientId` FOREIGN KEY (`clientId`) REFERENCES `oAuthClients` (`clientId`),
  CONSTRAINT `FK_oAuthNonces_userId` FOREIGN KEY (`userId`) REFERENCES `users` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `oAuthTokens`;

CREATE TABLE `oAuthTokens` (
  `oAuthTokenId` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `clientId` char(20) NOT NULL DEFAULT '' COMMENT 'The clientId the token belongs to',
  `token` char(20) NOT NULL DEFAULT '' COMMENT 'The generated token used by the client app',
  `ts` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'The ts when the token was issued',
  `userId` int(11) unsigned NOT NULL COMMENT 'The userId that this token represents',
  `refreshToken` char(20) NOT NULL DEFAULT '' COMMENT 'The generated token used to gain a new access_token',
  UNIQUE KEY `oAuthTokens_oAuthTokenIdx` (`oAuthTokenId`),
  UNIQUE KEY `oAuthTokens_oAuthTokenx` (`token`),
  KEY `oAuthTokens_clientId` (`clientId`),
  KEY `oAuthTokens_userId` (`userId`),
  CONSTRAINT `FK_oAuthTokens_userId` FOREIGN KEY (`userId`) REFERENCES `users` (`userId`),
  CONSTRAINT `FK_oAuthTokens_clientId` FOREIGN KEY (`clientId`) REFERENCES `oAuthClients` (`clientId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `ldapNonces`;

CREATE TABLE `ldapNonces` (
  `ldapNonceId` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL DEFAULT '' COMMENT 'The username of the login attempt',
  `ts` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'timestamp of the ldap user lockout or when the users first login attempt was',
  `attempts` int(11) NOT NULL DEFAULT '1' COMMENT 'the number of ldap login attempts',
  PRIMARY KEY (`ldapNonceId`),
  UNIQUE KEY `ldapNonces_usernamex` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `templateConfigs`;

CREATE TABLE `templateConfigs` (
  `templateConfigId` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `userId` int(11) UNSIGNED NOT NULL COMMENT 'The users id',
  `projectId` int(11) NOT NULL COMMENT 'The project Id',
  `configName` varchar(100) NOT NULL COMMENT 'The name of the config',
  `public` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'Whether or not this is a public template config?',
  `config` MEDIUMTEXT NOT NULL COMMENT 'The array of uris to be checked when generating a template',
  UNIQUE KEY `templateConfigs_configName_projectId` (`configName`, `projectId`),
  KEY `templateConfigs_projectId` (`projectId`),
  KEY `templateConfigs_userId` (`userId`),
  CONSTRAINT `FK_templateConfigs_userId`  FOREIGN KEY (`userId`) REFERENCES `users` (`userId`),
  CONSTRAINT `FK_templateConfigs_projectId` FOREIGN KEY(`projectId`) REFERENCES `projects` (`projectId`),
  PRIMARY KEY (`templateConfigId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
