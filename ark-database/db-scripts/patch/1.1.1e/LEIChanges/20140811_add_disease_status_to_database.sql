CREATE DATABASE IF NOT EXISTS `geno` DEFAULT CHARACTER SET latin1 COLLATE latin1_swedish_ci;
USE `geno`;

CREATE TABLE IF NOT EXISTS `beam` (
  `ID` int(11) NOT NULL AUTO_INCREMENT,
  `NAME` varchar(255) NOT NULL,
  `STUDY_ID` int(11) NOT NULL,
  `ENCODED_VALUES` text NOT NULL,
  `DEFAULT_VALUE` text,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `unique_beam_for_study` (`NAME`,`STUDY_ID`,`ENCODED_VALUES`(767)),
  KEY `STUDY_ID` (`STUDY_ID`)
) ENGINE=InnoDB  DEFAULT CHARSET=latin1;

CREATE TABLE IF NOT EXISTS `data` (
  `ID` int(11) NOT NULL AUTO_INCREMENT,
  `PERSON_ID` int(11) NOT NULL,
  `STUDY_ID` int(11) NOT NULL,
  `ROW_ID` int(11) NOT NULL,
  `BEAM_ID` int(11) NOT NULL,
  `VALUE` varchar(255) NOT NULL,
  PRIMARY KEY (`ID`),
  KEY `PERSON_FK` (`PERSON_ID`),
  KEY `ROW_FK` (`ROW_ID`),
  KEY `BEAM_FK` (`BEAM_ID`),
  KEY `STUDY_FK` (`STUDY_ID`)
) ENGINE=InnoDB  DEFAULT CHARSET=latin1;

CREATE TABLE IF NOT EXISTS `row` (
  `ID` int(11) NOT NULL AUTO_INCREMENT,
  `STUDY_ID` int(11) NOT NULL,
  `NAME` varchar(255) NOT NULL,
  PRIMARY KEY (`ID`),
  KEY `STUDY_ID` (`STUDY_ID`)
) ENGINE=InnoDB  DEFAULT CHARSET=latin1;


ALTER TABLE `beam`
  ADD CONSTRAINT `STUDY_ID_FK` FOREIGN KEY (`STUDY_ID`) REFERENCES `study`.`study` (`ID`);

ALTER TABLE `geno`.`data` 
ADD CONSTRAINT `data_ibfk_1`
  FOREIGN KEY (`PERSON_ID`)
  REFERENCES `study`.`person` (`ID`)
  ON DELETE NO ACTION
  ON UPDATE NO ACTION,
ADD CONSTRAINT `data_ibfk_2`
  FOREIGN KEY (`STUDY_ID`)
  REFERENCES `study`.`study` (`ID`)
  ON DELETE NO ACTION
  ON UPDATE NO ACTION,
ADD CONSTRAINT `data_ibfk_3`
  FOREIGN KEY (`BEAM_ID`)
  REFERENCES `geno`.`beam` (`ID`)
  ON DELETE NO ACTION
  ON UPDATE NO ACTION,
ADD CONSTRAINT `data_ibfk_4`
  FOREIGN KEY (`ROW_ID`)
  REFERENCES `geno`.`row` (`ID`)
  ON DELETE NO ACTION
  ON UPDATE NO ACTION;

/*
ALTER TABLE `data`
  ADD CONSTRAINT `data_ibfk_1` FOREIGN KEY (`PERSON_ID`) REFERENCES `study`.`person` (`ID`),
  ADD CONSTRAINT `data_ibfk_2` FOREIGN KEY (`STUDY_ID`) REFERENCES `study`.`study` (`ID`),
  ADD CONSTRAINT `data_ibfk_3` FOREIGN KEY (`BEAM_ID`) REFERENCES `beam` (`ID`),
  ADD CONSTRAINT `data_ibfk_4` FOREIGN KEY (`ROW_ID`) REFERENCES `row` (`ID`),
*/

ALTER TABLE `row`
  ADD CONSTRAINT `STUDY_ID_ROW_FK` FOREIGN KEY (`STUDY_ID`) REFERENCES `study`.`study` (`ID`);

use `study`;

INSERT INTO `ark_function` (`NAME`, `DESCRIPTION`, `ARK_FUNCTION_TYPE_ID`, `RESOURCE_KEY`) VALUES
('GENO_TABLE', NULL, 1, 'tab.module.geno.table');

SET @FUNCTION_ID = (SELECT ID FROM `ark_function` WHERE `NAME` = 'GENO_TABLE');

SET @MODULE_ID = (SELECT ID FROM `ark_module` WHERE `NAME` = 'Subject');

INSERT INTO `ark_module_function` (`ARK_MODULE_ID`, `ARK_FUNCTION_ID`) VALUES (@MODULE_ID, @FUNCTION_ID);

INSERT INTO `ark_role` (`NAME`) VALUES ('Geno Administrator'), ('Geno Data Manager');

SET @ADMIN_ROLE_ID = (SELECT ID FROM `ark_role` WHERE `NAME` = 'Geno Administrator');
SET @DATA_MAN_ROLE_ID = (SELECT ID FROM `ark_role` WHERE `NAME` = 'Geno Data Manager');
SET @READOLNY_ROLE_ID = (SELECT ID FROM `ark_role` WHERE `NAME` = 'Geno Read-Only User');

INSERT INTO `ark_role_policy_template` (`ARK_ROLE_ID`, `ARK_MODULE_ID`, `ARK_FUNCTION_ID`, `ARK_PERMISSION_ID`) VALUES
(@READOLNY_ROLE_ID, @MODULE_ID, @FUNCTION_ID, 2),
(@ADMIN_ROLE_ID, @MODULE_ID, @FUNCTION_ID, 1),
(@ADMIN_ROLE_ID, @MODULE_ID, @FUNCTION_ID, 2),
(@ADMIN_ROLE_ID, @MODULE_ID, @FUNCTION_ID, 3),
(@ADMIN_ROLE_ID, @MODULE_ID, @FUNCTION_ID, 4),
(@DATA_MAN_ROLE_ID, @MODULE_ID, @FUNCTION_ID, 2),
(@DATA_MAN_ROLE_ID, @MODULE_ID, @FUNCTION_ID, 3),
(1, @MODULE_ID, @FUNCTION_ID, 1),
(1, @MODULE_ID, @FUNCTION_ID, 2),
(1, @MODULE_ID, @FUNCTION_ID, 3),
(1, @MODULE_ID, @FUNCTION_ID, 4);
