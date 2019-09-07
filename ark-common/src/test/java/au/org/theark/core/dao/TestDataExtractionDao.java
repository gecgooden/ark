package au.org.theark.core.dao;

import au.org.theark.core.vo.DataExtractionVO;
import au.org.theark.core.vo.ExtractionVO;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

public class TestDataExtractionDao {

    private DateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");

    private ExtractionVO buildExtractionVO(String subjectUID, Date recordDate, String collectionName, String[] fields, String[] values) {
        ExtractionVO extractionVO = new ExtractionVO();
        extractionVO.setSubjectUid(subjectUID);
        extractionVO.setRecordDate(recordDate);
        extractionVO.setCollectionName(collectionName);
        if (fields.length != values.length) Assert.fail("Invalid test data provided");

        HashMap<String, String> keyValues = new HashMap<>();
        for(int i = 0; i < fields.length; i++) {
            keyValues.put(fields[i], values[i]);
        }
        extractionVO.setKeyValues(keyValues);

        return extractionVO;
    }

    @Test
    public void testPhenoCSVOutputStream() throws ParseException {
        HashMap<String, ExtractionVO> demographicData = new HashMap<>();
        demographicData.put("TEST-123", buildExtractionVO(null, null, null, new String[]{}, new String[]{}));

        Date recordDate = simpleDateFormat.parse("2019-09-07");
        HashMap<String, ExtractionVO> phenotypicData = new HashMap<>();
        phenotypicData.put("123", buildExtractionVO("TEST-123", recordDate, "Test Collection", new String[]{"DOB"}, new String[]{"19/11/1992"}));

        DataExtractionDao dataExtractionDao = new DataExtractionDao();
        DataExtractionVO dataExtractionVO = new DataExtractionVO();
        dataExtractionVO.setPhenoCustomData(phenotypicData);
        dataExtractionVO.setDemographicData(demographicData);

        ByteArrayOutputStream testOutputStream = new ByteArrayOutputStream();
        dataExtractionDao.createPhenoCSVOutputStream(dataExtractionVO, testOutputStream);

        String expectedOutput =
                "\"Subject UID\",\"P1_Record Date\",\"P1_Collection Name\",\"P1_DOB\"\r\n" +
                "\"TEST-123\",\"09/07/2019\",\"Test Collection\",\"19/11/1992\"";
        Assert.assertEquals(expectedOutput.trim(), new String(testOutputStream.toByteArray()).trim());
    }

    @Test
    public void testPhenoCSVOutputStreamNoPhenoData() throws ParseException {
        HashMap<String, ExtractionVO> demographicData = new HashMap<>();
        demographicData.put("TEST-123", buildExtractionVO(null, null, null, new String[]{}, new String[]{}));
        demographicData.put("TEST-456", buildExtractionVO(null, null, null, new String[]{}, new String[]{}));

        Date recordDate = simpleDateFormat.parse("2019-09-07");
        HashMap<String, ExtractionVO> phenotypicData = new HashMap<>();
        phenotypicData.put("123", buildExtractionVO("TEST-123", recordDate, "Test Collection", new String[]{"DOB"}, new String[]{"19/11/1992"}));

        DataExtractionDao dataExtractionDao = new DataExtractionDao();
        DataExtractionVO dataExtractionVO = new DataExtractionVO();
        dataExtractionVO.setPhenoCustomData(phenotypicData);
        dataExtractionVO.setDemographicData(demographicData);

        ByteArrayOutputStream testOutputStream = new ByteArrayOutputStream();
        dataExtractionDao.createPhenoCSVOutputStream(dataExtractionVO, testOutputStream);

        String expectedOutput =
                "\"Subject UID\",\"P1_Record Date\",\"P1_Collection Name\",\"P1_DOB\"\r\n" +
                "\"TEST-123\",\"09/07/2019\",\"Test Collection\",\"19/11/1992\"";
        Assert.assertEquals(expectedOutput.trim(), new String(testOutputStream.toByteArray()).trim());
    }

    @Test
    public void testPhenoCSVOutputStreamMultipleCollections() throws ParseException {
        HashMap<String, ExtractionVO> demographicData = new HashMap<>();
        demographicData.put("TEST-123", buildExtractionVO(null, null, null, new String[]{}, new String[]{}));

        Date recordDate = simpleDateFormat.parse("2019-09-07");
        HashMap<String, ExtractionVO> phenotypicData = new HashMap<>();
        phenotypicData.put("123", buildExtractionVO("TEST-123", recordDate, "Test Collection", new String[]{"HEIGHT"}, new String[]{"1.7"}));
        phenotypicData.put("456", buildExtractionVO("TEST-123", recordDate, "Test Collection", new String[]{"HEIGHT"}, new String[]{"1.8"}));

        DataExtractionDao dataExtractionDao = new DataExtractionDao();
        DataExtractionVO dataExtractionVO = new DataExtractionVO();
        dataExtractionVO.setPhenoCustomData(phenotypicData);
        dataExtractionVO.setDemographicData(demographicData);

        ByteArrayOutputStream testOutputStream = new ByteArrayOutputStream();
        dataExtractionDao.createPhenoCSVOutputStream(dataExtractionVO, testOutputStream);

        String expectedOutput =
                "\"Subject UID\",\"P1_Record Date\",\"P1_Collection Name\",\"P1_HEIGHT\",\"P2_Record Date\",\"P2_Collection Name\",\"P2_HEIGHT\"\r\n" +
                "\"TEST-123\",\"09/07/2019\",\"Test Collection\",\"1.7\",\"09/07/2019\",\"Test Collection\",\"1.8\"";
        Assert.assertEquals(expectedOutput.trim(), new String(testOutputStream.toByteArray()).trim());
    }

    @Test
    public void testPhenoCSVOutputStreamMultipleCollectionsMissing() throws ParseException {
        HashMap<String, ExtractionVO> demographicData = new HashMap<>();
        demographicData.put("TEST-123", buildExtractionVO(null, null, null, new String[]{}, new String[]{}));
        demographicData.put("TEST-456", buildExtractionVO(null, null, null, new String[]{}, new String[]{}));

        Date recordDate = simpleDateFormat.parse("2019-09-07");
        HashMap<String, ExtractionVO> phenotypicData = new HashMap<>();
        phenotypicData.put("1", buildExtractionVO("TEST-123", recordDate, "Test Collection", new String[]{"HEIGHT"}, new String[]{"1.7"}));
        phenotypicData.put("2", buildExtractionVO("TEST-123", recordDate, "Another Test Collection", new String[]{"WEIGHT"}, new String[]{"90"}));
        phenotypicData.put("3", buildExtractionVO("TEST-456", recordDate, "Test Collection", new String[]{"HEIGHT"}, new String[]{"1.8"}));

        DataExtractionDao dataExtractionDao = new DataExtractionDao();
        DataExtractionVO dataExtractionVO = new DataExtractionVO();
        dataExtractionVO.setPhenoCustomData(phenotypicData);
        dataExtractionVO.setDemographicData(demographicData);

        ByteArrayOutputStream testOutputStream = new ByteArrayOutputStream();
        dataExtractionDao.createPhenoCSVOutputStream(dataExtractionVO, testOutputStream);

        String expectedOutput =
                "\"Subject UID\",\"P1_Record Date\",\"P1_Collection Name\",\"P1_HEIGHT\",\"P1_WEIGHT\",\"P2_Record Date\",\"P2_Collection Name\",\"P2_HEIGHT\",\"P2_WEIGHT\"\r\n" +
                "\"TEST-123\",\"09/07/2019\",\"Test Collection\",\"1.7\",\"\",\"09/07/2019\",\"Another Test Collection\",\"\",\"90\"\r\n" +
                "\"TEST-456\",\"09/07/2019\",\"Test Collection\",\"1.8\",\"\",\"\",\"\",\"\",\"\"";
        Assert.assertEquals(expectedOutput.trim(), new String(testOutputStream.toByteArray()).trim());
    }
}
