ALTER TABLE `spark`.`analysis` 
ADD COLUMN `PARAMETERS` VARCHAR(255) NULL AFTER `COMPUTATION_ID`,
ADD COLUMN `RESULT` VARCHAR(100) NULL AFTER `PARAMETERS`;

