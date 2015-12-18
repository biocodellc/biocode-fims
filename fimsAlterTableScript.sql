// First drop all foreign key constraints
// List all foreign key constraints for the specified db with the following cmd, make sure to change table_schema to correct database name

mysql --batch --skip-column-names -e "SELECT concat('alter table ',table_schema,'.',table_name,' DROP FOREIGN KEY ',constraint_name,';') FROM information_schema.table_constraints WHERE constraint_type='FOREIGN KEY' AND table_schema='biscicol';"

// drop the following tables
DROP TABLE identifiers;
DROP TABLE authorities;

// run the alter statements that are produced

ALTER TABLE users CHANGE user_id userId INT(11);
ALTER TABLE users CHANGE set_password hasSetPassword boolean;
ALTER TABLE users CHANGE pass_reset_token passwordResetToken char(20);
ALTER TABLE users CHANGE pass_reset_expiration passwordResetExpiration datetime;
ALTER TABLE users DROP COLUMN fullname;
ALTER TABLE users DROP COLUMN IDLimit;

RENAME TABLE datasets TO bcids;
ALTER TABLE bcids CHANGE datasets_id bcidId INT(11);
ALTER TABLE bcids CHANGE internalID internalId char(36);
ALTER TABLE bcids CHANGE users_id userId INT(11);
ALTER TABLE bcids CHANGE webaddress webAddress text;

ALTER TABLE expeditions CHANGE expedition_id expeditionId INT(11);
ALTER TABLE expeditions CHANGE project_id projectId INT(11);
ALTER TABLE expeditions CHANGE internalID internalId char(36);
ALTER TABLE expeditions CHANGE expedition_code expeditionCode varchar(58);
ALTER TABLE expeditions CHANGE expedition_title expeditionTitle varchar(128);
ALTER TABLE expeditions CHANGE users_id userId INT(11);

RENAME TABLE expeditionsBCIDs TO expeditionBcids;
ALTER TABLE expeditionBcids CHANGE expeditionsBCIDs_id expeditionBcidId INT(11);
ALTER TABLE expeditionBcids CHANGE expedition_id expeditionId INT(11);
ALTER TABLE expeditionBcids CHANGE datasets_id bcidId INT(11);

ALTER TABLE projects CHANGE project_id projectId INT(11);
ALTER TABLE projects CHANGE project_code projectCode varchar(6);
ALTER TABLE projects CHANGE project_title projectTitle varchar(128);
ALTER TABLE projects CHANGE bioValidator_validation_xml validationXml text;
ALTER TABLE projects CHANGE users_id userId INT(11);

RENAME TABLE usersProjects TO userProjects;
ALTER TABLE userProjects CHANGE usersProjects_id userProjectId INT(11);
ALTER TABLE userProjects CHANGE project_id projectId INT(11);
ALTER TABLE userProjects CHANGE users_id userId INT(11);

ALTER TABLE oAuthClients CHANGE oauthClients_id oAuthClientId INT(11);
ALTER TABLE oAuthClients CHANGE client_id clientId char(20);
ALTER TABLE oAuthClients CHANGE client_secret clientSecret char(75);

ALTER TABLE oAuthNonces CHANGE oauthNonces_id oAuthNonceId INT(11);
ALTER TABLE oAuthNonces CHANGE client_id clientId char(20);
ALTER TABLE oAuthNonces CHANGE user_id userId INT(11);
ALTER TABLE oAuthNonces CHANGE redirect_uri redirectUri varchar(256);

ALTER TABLE oAuthTokens CHANGE oauthTokens_id oAuthTokenId INT(11);
ALTER TABLE oAuthTokens CHANGE client_id clientId char(20);
ALTER TABLE oAuthTokens CHANGE user_id userId INT(11);
ALTER TABLE oAuthTokens CHANGE refresh_token refreshToken char(20);

ALTER TABLE templateConfigs CHANGE templateConfigs_id templateConfigId INT(11);
ALTER TABLE templateConfigs CHANGE users_id userId INT(11);
ALTER TABLE templateConfigs CHANGE project_id projectId INT(11);
ALTER TABLE templateConfigs CHANGE config_name configName varchar(100);

// add back in foreign key constraints

ALTER TABLE bcids ADD CONSTRAINT `FK_bcids_userId` FOREIGN KEY (`userId`) REFERENCES users(`userId`);
ALTER TABLE expeditions ADD CONSTRAINT `FK_expeditions_userId` FOREIGN KEY (`userId`) REFERENCES users(`userId`);
ALTER TABLE projects ADD CONSTRAINT `FK_projects_userId` FOREIGN KEY (`userId`) REFERENCES users(`userId`);
ALTER TABLE userProjects ADD CONSTRAINT `FK_userProjects_userId` FOREIGN KEY (`userId`) REFERENCES users(`userId`);
ALTER TABLE oAuthNonces ADD CONSTRAINT `FK_oAuthNonces_userId` FOREIGN KEY (`userId`) REFERENCES `users` (`userId`);
ALTER TABLE oAuthTokens ADD CONSTRAINT `FK_oAuthTokens_userId` FOREIGN KEY (`userId`) REFERENCES `users` (`userId`);
ALTER TABLE templateConfigs ADD CONSTRAINT `FK_templateConfigs_userId`  FOREIGN KEY (`userId`) REFERENCES `users` (`userId`);

ALTER TABLE expeditionBcids ADD CONSTRAINT `FK_expeditionBcids_expeditionId` FOREIGN KEY (`expeditionId`) REFERENCES expeditions(`expeditionId`);

ALTER TABLE expeditionBcids ADD CONSTRAINT `FK_expeditionBcids_bcidId` FOREIGN KEY (`bcidId`) REFERENCES bcids(`bcidId`);

ALTER TABLE expeditions ADD CONSTRAINT `FK_expeditions_projectId`  FOREIGN KEY (`projectId`) REFERENCES `projects` (`projectId`);
ALTER TABLE userProjects ADD CONSTRAINT `FK_userProjects_projectId` FOREIGN KEY (`projectId`) REFERENCES projects(`projectId`);
ALTER TABLE templateConfigs ADD CONSTRAINT `FK_templateConfigs_projectId` FOREIGN KEY(`projectId`) REFERENCES `projects` (`projectId`);

ALTER TABLE oAuthNonces ADD CONSTRAINT `FK_oAuthNonces_clientId` FOREIGN KEY (`clientId`) REFERENCES `oAuthClients` (`clientId`);
ALTER TABLE oAuthTokens ADD CONSTRAINT `FK_oAuthTokens_clientId` FOREIGN KEY (`clientId`) REFERENCES `oAuthClients` (`clientId`);

// select * from information_schema.key_column_usage where referenced_table_schema='biscicol'; should return 14 rows at this point


// folowing is for nmnh only?

ALTER TABLE ldapNonces CHANGE ldapNonces_id ldapNonceId INT(11);
