/*
-- Query: SELECT 
	`NAME`,
	`TOP_LEVEL_GROUPING_ID`,
	@NEWSTUDYKEY,
	`STATUS`,
	`STARTTIME`,
	`FINISHTIME`,
	`INCLUDE_GENO` 
	FROM `reporting`.`search`
	WHERE 	study_id = @COPYFROMSTUDYKEY
	AND 	name in (@SEARCHNAME1, @SEARCHNAME2, @SEARCHNAME3)
LIMIT 0, 14500

-- Date: 2014-07-02 10:52
*/

-- SET @STUDYKEY = 17;
SET @NEWSTUDYKEY = 18;
SET @COPYFROMSTUDYKEY = 17;
SET @SEARCHNAME1 = 'Biospecimen Detailed Report';
SET @SEARCHNAME2 = 'Locations Info Only';
SET @SEARCHNAME3 = 'Biospecimen Custom Fields';

INSERT INTO reporting.search (`NAME`,`TOP_LEVEL_GROUPING_ID`,STUDY_ID,`STATUS`,`STARTTIME`,`FINISHTIME`,`INCLUDE_GENO`) VALUES ('Biospecimen Custom Fields',NULL,18,'FINISHED','2014-06-17 14:12:15','2014-06-17 14:12:49',0);
INSERT INTO reporting.search (`NAME`,`TOP_LEVEL_GROUPING_ID`,STUDY_ID,`STATUS`,`STARTTIME`,`FINISHTIME`,`INCLUDE_GENO`) VALUES ('Locations Info Only',NULL,18,'FINISHED','2014-06-20 11:59:02','2014-06-20 11:59:36',0);
INSERT INTO reporting.search (`NAME`,`TOP_LEVEL_GROUPING_ID`,STUDY_ID,`STATUS`,`STARTTIME`,`FINISHTIME`,`INCLUDE_GENO`) VALUES ('Biospecimen Detailed Report',NULL,18,'FINISHED','2014-06-20 13:37:58','2014-06-20 13:38:25',0);

