USE pheno;
ALTER TABLE `pheno`.`delimiter_type` CHANGE COLUMN `DELIMITER_CHARACTER` `DELIMITER_CHARACTER` VARCHAR(1) NOT NULL DEFAULT ',';
UPDATE `pheno`.`delimiter_type` SET `NAME`='AT SYMBOL', `DESCRIPTION`='At character', `DELIMITER_CHARACTER`='\@' WHERE `ID`='5';