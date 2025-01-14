USE `pheno`;
ALTER TABLE `pheno_dataset_collection` 
DROP FOREIGN KEY `FK_PHENO_COLL_REVIEWED_ID`;
ALTER TABLE `pheno_dataset_collection` 
CHANGE COLUMN `REVIEWED_BY_ID` `REVIEWED_BY_ID` INT(11) NULL ;
ALTER TABLE `pheno_dataset_collection` 
ADD CONSTRAINT `FK_PHENO_COLL_REVIEWED_ID`
  FOREIGN KEY (`REVIEWED_BY_ID`)
  REFERENCES `study`.`ark_user` (`ID`)
  ON DELETE NO ACTION
  ON UPDATE NO ACTION;
