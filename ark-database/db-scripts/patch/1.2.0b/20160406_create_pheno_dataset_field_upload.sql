use `pheno`;
DROP TABLE IF EXISTS `pheno`.`pheno_field_upload`;
CREATE TABLE `pheno`.`pheno_field_upload` (
  `ID` int(11) NOT NULL AUTO_INCREMENT,
  `PHENO_FIELD_ID` int(11) NOT NULL,
  `UPLOAD_ID` int(11) NOT NULL,
  PRIMARY KEY (`ID`),
  KEY `FK_PHENO_FIELD_idx` (`PHENO_FIELD_ID`),
  KEY `FK_UPLOAD_ID_idx` (`UPLOAD_ID`),
  CONSTRAINT `FK_PHENO_FIELD_UPLOAD_PHENO_FIELD_ID` FOREIGN KEY (`PHENO_FIELD_ID`) REFERENCES `pheno_dataset_field` (`ID`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `FK_PHENO_FIELD_STUDY_SCHEMA_UPLOAD_ID` FOREIGN KEY (`UPLOAD_ID`) REFERENCES `study`.`upload` (`ID`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=latin1;


