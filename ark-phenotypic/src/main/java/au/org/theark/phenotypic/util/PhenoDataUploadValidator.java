/*******************************************************************************
 * Copyright (c) 2011  University of Western Australia. All rights reserved.
 * 
 * This file is part of The Ark.
 * 
 * The Ark is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 * 
 * The Ark is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package au.org.theark.phenotypic.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import jxl.Workbook;
import jxl.read.biff.BiffException;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.theark.core.Constants;
import au.org.theark.core.exception.ArkBaseException;
import au.org.theark.core.exception.ArkSystemException;
import au.org.theark.core.exception.FileFormatException;
import au.org.theark.core.model.pheno.entity.PhenoDataSetCollection;
import au.org.theark.core.model.pheno.entity.PhenoDataSetData;
import au.org.theark.core.model.pheno.entity.PhenoDataSetField;
import au.org.theark.core.model.pheno.entity.PhenoDataSetFieldDisplay;
import au.org.theark.core.model.pheno.entity.PhenoDataSetGroup;
import au.org.theark.core.model.study.entity.ArkFunction;
import au.org.theark.core.model.study.entity.Study;
import au.org.theark.core.service.IArkCommonService;
import au.org.theark.core.util.XLStoCSV;
import au.org.theark.core.vo.UploadVO;
import au.org.theark.core.web.component.worksheet.ArkGridCell;
import au.org.theark.phenotypic.service.IPhenotypicService;

import com.csvreader.CsvReader;

/**
 * CustomField UploadValidator provides support for validating subject matrix-formatted files.
 * 
 * @author travis
 */
public class PhenoDataUploadValidator {
	private static final long		serialVersionUID			= -1933045886948087734L;
	private static Logger			log							= LoggerFactory.getLogger(PhenoDataUploadValidator.class);

	@SuppressWarnings("unchecked")
	private IArkCommonService		iArkCommonService;
	private IPhenotypicService		iPhenotypicService;
	private Long						studyId;
	private Study						study;
	java.util.Collection<String>	fileValidationMessages	= new java.util.ArrayList<String>();
	java.util.Collection<String>	dataValidationMessages	= new java.util.ArrayList<String>();
	private HashSet<Integer>		existantSubjectUIDRows;
	private HashSet<Integer>		nonExistantUIDs;
	private HashSet<ArkGridCell>	errorCells;
	private HashSet<ArkGridCell>	insertCells;
	private HashSet<Integer>	warningRows;
	private SimpleDateFormat		simpleDateFormat			= new SimpleDateFormat(au.org.theark.core.Constants.DD_MM_YYYY);
	private char						delimiterCharacter		= au.org.theark.core.Constants.DEFAULT_DELIMITER_CHARACTER;
	private String						fileFormat					= au.org.theark.core.Constants.DEFAULT_FILE_FORMAT;
	private int							row							= 1;

	public PhenoDataUploadValidator() {
		super();
		Subject currentUser = SecurityUtils.getSubject();
		studyId = (Long) currentUser.getSession().getAttribute(au.org.theark.core.Constants.STUDY_CONTEXT_ID);
		this.study = iArkCommonService.getStudy(studyId);
		this.existantSubjectUIDRows = new HashSet<Integer>();
		this.nonExistantUIDs = new HashSet<Integer>();
		this.errorCells = new HashSet<ArkGridCell>();
		this.warningRows = new HashSet<Integer>();
		this.insertCells = new HashSet<ArkGridCell>();
		simpleDateFormat.setLenient(false);
	}

	public PhenoDataUploadValidator(Study study) {
		super();
		this.study = study;
		this.existantSubjectUIDRows = new HashSet<Integer>();
		this.nonExistantUIDs = new HashSet<Integer>();
		this.errorCells = new HashSet<ArkGridCell>();
		this.warningRows = new HashSet<Integer>();
		this.insertCells = new HashSet<ArkGridCell>();
		simpleDateFormat.setLenient(false);
	}

	@SuppressWarnings("unchecked")
	public PhenoDataUploadValidator(IArkCommonService iArkCommonService, IPhenotypicService iPhenotypicService) {
		super();
		this.iArkCommonService = iArkCommonService;
		this.iPhenotypicService = iPhenotypicService;
		Subject currentUser = SecurityUtils.getSubject();
		studyId = (Long) currentUser.getSession().getAttribute(au.org.theark.core.Constants.STUDY_CONTEXT_ID);
		this.study = iArkCommonService.getStudy(studyId);
		this.existantSubjectUIDRows = new HashSet<Integer>();
		this.nonExistantUIDs = new HashSet<Integer>();
		this.errorCells = new HashSet<ArkGridCell>();
		this.warningRows = new HashSet<Integer>();
		this.insertCells = new HashSet<ArkGridCell>();
		simpleDateFormat.setLenient(false);
	}

	public java.util.Collection<String> getFileValidationMessages() {
		return fileValidationMessages;
	}

	public void setFileValidationMessages(java.util.Collection<String> fileValidationMessages) {
		this.fileValidationMessages = fileValidationMessages;
	}

	public java.util.Collection<String> getDataValidationMessages() {
		return dataValidationMessages;
	}

	public void setDataValidationMessages(java.util.Collection<String> dataValidationMessages) {
		this.dataValidationMessages = dataValidationMessages;
	}

	public Long getStudyId() {
		return studyId;
	}

	public void setStudyId(Long studyId) {
		this.studyId = studyId;
	}

	public Study getStudy() {
		return study;
	}

	public void setStudy(Study study) {
		this.study = study;
	}

	public HashSet<Integer> getInsertRows() {
		return existantSubjectUIDRows;
	}

	public void setInsertRows(HashSet<Integer> insertRows) {
		this.existantSubjectUIDRows = insertRows;
	}

	public HashSet<Integer> getUpdateRows() {
		return nonExistantUIDs;
	}

	public void setUpdateRows(HashSet<Integer> updateRows) {
		this.nonExistantUIDs = updateRows;
	}

	public HashSet<ArkGridCell> getErrorCells() {
		return errorCells;
	}

	public void setErrorCells(HashSet<ArkGridCell> errorCells) {
		this.errorCells = errorCells;
	}

	public HashSet<Integer> getWarningRows() {
		return warningRows;
	}

	public void setWarningRows(HashSet<Integer> warningRows) {
		this.warningRows = warningRows;
	}

	public HashSet<ArkGridCell> getInsertCells() {
		return insertCells;
	}

	public void setInsertCells(HashSet<ArkGridCell> insertCells) {
		this.insertCells = insertCells;
	}

	/**
	 * Validates the file in the default "matrix" file format assumed: SUBJECTUID,FIELD1,FIELD2,FIELDN... Where N is any number of columns
	 * 
	 * @param uploadVo
	 *           is the UploadVO of the file
	 * @return a collection of validation messages
	 */
	public Collection<String> validateCustomFieldFileFormat(UploadVO uploadVo, PhenoDataSetCollection phenoCollection, PhenoDataSetGroup pfg)throws FileFormatException,ArkBaseException {
		java.util.Collection<String> validationMessages = null;
		try {
			InputStream inputStream = uploadVo.getFileUpload().getInputStream();
			String filename = uploadVo.getFileUpload().getClientFileName();
			fileFormat = filename.substring(filename.lastIndexOf('.') + 1).toUpperCase();
			delimiterCharacter = uploadVo.getUpload().getDelimiterType().getDelimiterCharacter();
			validationMessages = validateCustomFieldFileFormat(inputStream, fileFormat, delimiterCharacter, phenoCollection, pfg, uploadVo.getUpdateChkBox());
		}
		catch (IOException e) {
			log.error(e.getMessage());
		}
		return validationMessages;
	}

	/**
	 * Validates the file in the default "matrix" file format assumed: SUBJECTUID,FIELD1,FIELD2,FIELDN... Where N is any number of columns
	 * 
	 * @param inputStream
	 *           is the input stream of the file
	 * @param fileFormat
	 *           is the file format (eg txt)
	 * @param delimChar
	 *           is the delimiter character of the file (eg comma)
	 * @param updateExisting 
	 * @return a collection of validation messages
	 */
	public Collection<String> validateCustomFieldFileFormat(InputStream inputStream, String fileFormat, char delimChar, PhenoDataSetCollection phenoCollection, PhenoDataSetGroup pfg, Boolean updateExisting) throws FileFormatException,ArkBaseException{
		java.util.Collection<String> validationMessages = null;

		
			// If Excel, convert to CSV for validation
			if (fileFormat.equalsIgnoreCase("XLS")) {
				Workbook w;
				try {
					w = Workbook.getWorkbook(inputStream);
					delimiterCharacter = ',';
					XLStoCSV xlsToCsv = new XLStoCSV(delimiterCharacter);
					inputStream = xlsToCsv.convertXlsToCsv(w);
					inputStream.reset();
				}
				catch (BiffException e) {
					log.error(e.getMessage());
				}
				catch (IOException e) {
					log.error(e.getMessage());
				}
			}
			validationMessages = validateCustomFieldMatrixFileFormat(inputStream, inputStream.toString().length(), fileFormat, delimChar, phenoCollection, pfg, updateExisting);
		
		return validationMessages;
	}

	/**
	 * Validates the file in the default "matrix" file data assumed: SUBJECTUID,FIELD1,FIELD2,FIELDN... Where N is any number of columns
	 * 
	 * @param uploadVo
	 *           is the UploadVO of the file
	 * @return a collection of validation messages
	 */
	public Collection<String> validateCustomFieldFileData(UploadVO uploadVo, List<String> uidsToUpdateReference, PhenoDataSetGroup phenoDataSetGroup)throws FileFormatException,ArkBaseException {
		java.util.Collection<String> validationMessages = new ArrayList<String>();
		try {
			InputStream inputStream = uploadVo.getFileUpload().getInputStream();
			String filename = uploadVo.getFileUpload().getClientFileName();
			fileFormat = filename.substring(filename.lastIndexOf('.') + 1).toUpperCase();
			delimiterCharacter = uploadVo.getUpload().getDelimiterType().getDelimiterCharacter();

			// If Excel, convert to CSV for validation
			if (fileFormat.equalsIgnoreCase("XLS")) {
				Workbook w;
				try {
					w = Workbook.getWorkbook(inputStream);
					delimiterCharacter = ',';
					XLStoCSV xlsToCsv = new XLStoCSV(delimiterCharacter);
					inputStream = xlsToCsv.convertXlsToCsv(w);
					inputStream.reset();
				}
				catch (BiffException e) {
					log.error(e.getMessage());
				}
			}

			validationMessages = validateSubjectFileData(inputStream, fileFormat, delimiterCharacter, uidsToUpdateReference, phenoDataSetGroup, uploadVo.getUpdateChkBox());
		}
		catch (IOException e) {
			log.error(e.getMessage());
			validationMessages.add(e.getMessage());
			return validationMessages;
		}
		return validationMessages;
	}

	public Collection<String> validateSubjectFileData(InputStream inputStream, String fileFormat, char delimChar, List<String> uidsToUpdateReference, PhenoDataSetGroup phenoDataSetGroup, Boolean updateExisting)throws FileFormatException,ArkBaseException {
		java.util.Collection<String> validationMessages = null;

		try {
			//TODO performance of valdation now approx 60-90K records per minute, file creation after validation doubles that
			//I think this is acceptable for now to keep in user interface.  Can make some slight improvements though, and if it bloats with more fields could be part of batch too
			validationMessages = validateMatrixCustomFileData(inputStream, inputStream.toString().length(), fileFormat, delimChar, Long.MAX_VALUE, uidsToUpdateReference, phenoDataSetGroup, updateExisting);
		}
		catch (FileFormatException ffe) {
			log.error(au.org.theark.phenotypic.web.Constants.FILE_FORMAT_EXCEPTION + ffe);
			throw new FileFormatException();
		}
		catch (ArkBaseException abe) {
			log.error(au.org.theark.phenotypic.web.Constants.ARK_BASE_EXCEPTION + abe);
			throw new ArkBaseException();
		}
		return validationMessages;
	}

	/**
	 * Validates the file in the custom field list.
	 * 
	 * Requires.  SubjectUID specified in row one.  And all Fields must be valid for its type
	 * 
	 * Where N is any number of columns
	 * 
	 * @param fileInputStream
	 *           is the input stream of a file
	 * @param inLength
	 *           is the length of a file
	 * @param updateExisting 
	 * @throws FileFormatException
	 *            file format Exception
	 * @throws ArkBaseException
	 *            general ARK Exception
	 * @return a collection of file format validation messages
	 */
	public java.util.Collection<String> validateCustomFieldMatrixFileFormat(InputStream fileInputStream, long inLength, String inFileFormat, char inDelimChr, PhenoDataSetCollection phenoCollection, PhenoDataSetGroup phenoDataSetGroup, Boolean updateExisting) throws FileFormatException, ArkBaseException {
		delimiterCharacter = inDelimChr;
		
		fileFormat = inFileFormat;
		row = 0;
		InputStreamReader inputStreamReader = null;
		CsvReader csvReader = null;
		try {
			inputStreamReader = new InputStreamReader(fileInputStream);
			if (inLength <= 0) {
				throw new FileFormatException("The input size was not greater than 0.  Actual length reported: " + inLength);
			}

			csvReader = new CsvReader(inputStreamReader, delimiterCharacter);
			csvReader.readHeaders();
			String[] headerColumnArray = csvReader.getHeaders();
			boolean headerError = false;
			boolean hasSubjectUIDHeader = false;
			boolean hasDateHeader = false;																
			ArkFunction customFieldArkFunction = iArkCommonService.getArkFunctionByName(Constants.FUNCTION_KEY_VALUE_DATA_DICTIONARY);
			List<String> badHeaders = new ArrayList<String>();
			for(String header : headerColumnArray){																						
				if(header.equalsIgnoreCase("SUBJECTUID")) {
					hasSubjectUIDHeader = true;
				}																						
				else if(header.equalsIgnoreCase("RECORD_DATE_TIME")) {
					hasDateHeader = true;
				}
				else if(!header.isEmpty()){
					//TODO just make it get all of them and look through in memory rather than 10-50-300-500 selects?
					if(iPhenotypicService.getPhenoDataSetFieldByNameStudyPFG(header, study, customFieldArkFunction, phenoDataSetGroup) == null){
						badHeaders.add(header);
						headerError = true;
					}
				}
			}
			if (headerError || !hasSubjectUIDHeader || !hasDateHeader) {
				// Invalid file format
				StringBuffer stringBuffer = new StringBuffer();
				stringBuffer.append("ERROR:  This file is not valid because; \n");//The specified delimiter type was: " + delimiterCharacter + ".\n\n");This file is not valid because; \n");
				fileValidationMessages.add(stringBuffer.toString());
				if(!hasSubjectUIDHeader){
					fileValidationMessages.add("The column name \"SUBJECTUID\" must exist as the header of the first column.\n");
				}
				if(!hasDateHeader){
					fileValidationMessages.add("The column name \"RECORD_DATE_TIME\" must exist as the header of the second column.\n");
				}
				for (String badHeader : badHeaders) {
					fileValidationMessages.add("The column name " + badHeader + " does not match with an existing custom field assigned to the data set: " + phenoDataSetGroup.getName() + "\n");
				}
				log.warn("failed header validation");
			}
			else{
				log.warn("passed header validation");
			}
			row = 1;
		}
		catch (IOException ioe) {
			log.error("processMatrixSubjectFile IOException stacktrace:", ioe);
			throw new ArkSystemException("Unexpected I/O exception whilst reading the subject data file");
		}
		catch (Exception ex) {
			log.error("processMatrixSubjectFile Exception stacktrace:", ex);
			throw new ArkSystemException("Unexpected exception occurred when trying to process subject data file");
		}
		finally {
			if (csvReader != null) {
				try {
					csvReader.close();
				}
				catch (Exception ex) {
					log.error("Cleanup operation failed: csvRdr.close()", ex);
				}
			}
			if (inputStreamReader != null) {
				try {
					//TODO ASAP : re-evaluate below
					inputStreamReader.close();
				}
				catch (Exception ex) {
					log.error("Cleanup operation failed: isr.close()", ex);
				}
			}
		}

		return fileValidationMessages;
	}

	/**
	 * Validates the file in the default "matrix" file format assumed: SUBJECTUID,FIELD1,FIELD2,FIELDN...
	 * 
	 * TODO:  remove globals unless their is a legit reason
	 * 
	 * Where N is any number of columns
	 * 
	 * @param fileInputStream
	 *           is the input stream of a file
	 * @param inLength
	 *           is the length of a file
	 * @param rowsToValidate
	 *           validate the number of rows specified (or as many as exist, if that number is greater).
	 * @throws FileFormatException
	 *            file format Exception
	 * @throws ArkBaseException
	 *            general ARK Exception
	 * @return a collection of data validation messages
	 */
	public java.util.Collection<String> validateMatrixCustomFileData(InputStream fileInputStream, long inLength, String inFileFormat, char inDelimChr, long rowsToValidate, 
			List<String> uidsToUpdateReference, PhenoDataSetGroup phenoDataSetGroup, Boolean updateExisting) throws FileFormatException, ArkSystemException {
		delimiterCharacter = inDelimChr;
		fileFormat = inFileFormat;
		row = 1;
		InputStreamReader inputStreamReader = null;
		CsvReader csvReader = null;
		
		try {
			inputStreamReader = new InputStreamReader(fileInputStream);
			String[] stringLineArray;
			if (inLength <= 0) {
				throw new FileFormatException("The input files' size was not greater than 0.  Actual length reported: " + inLength);
			}

			csvReader = new CsvReader(inputStreamReader, delimiterCharacter);
			csvReader.readHeaders();
			List<String> subjectUIDsAlreadyExisting = iArkCommonService.getAllSubjectUIDs(study);	//TODO evaluate data in future to know if should get all id's in the csv, rather than getting all id's in study to compre

			List<String> fieldNameCollection = Arrays.asList(csvReader.getHeaders());
			ArkFunction phenoDataDictionaryFunction = iArkCommonService.getArkFunctionByName(Constants.FUNCTION_KEY_VALUE_DATA_DICTIONARY);
																							//remove if not subjectuid, enforce fetch of customField to save another query each
			List<PhenoDataSetFieldDisplay> cfdsThatWeNeed = iPhenotypicService.getPhenoFieldDisplaysIn(fieldNameCollection, study, phenoDataDictionaryFunction, phenoDataSetGroup);
			
			/* 
			 * other validation for pheno?
			 * - do u own it?
			 * all the validation that is on subject custom fields too
			 * 
			 */
			
			while (csvReader.readRecord()) {
				stringLineArray = csvReader.getValues();//i might still need this or might not now that i am evaluating by name ... TODO evaluate
				String subjectUID = stringLineArray[0];	// First/0th column should be the SubjectUID
				if(!subjectUIDsAlreadyExisting.contains(subjectUID)){
					nonExistantUIDs.add(row);//TODO test and compare array.
					for(PhenoDataSetFieldDisplay pfd : cfdsThatWeNeed){
						errorCells.add(new ArkGridCell(csvReader.getIndex(pfd.getPhenoDataSetField().getName()), row));
					}
					errorCells.add(new ArkGridCell(0, row));
				}
				else{
					Date recordDate = (stringLineArray[1].isEmpty() ? new Date() : simpleDateFormat.parse(stringLineArray[1]));
					List<PhenoDataSetCollection> phenos = iPhenotypicService.getSubjectMatchingPhenoCollections(iArkCommonService.getSubjectByUID(subjectUID, study), phenoDataSetGroup, recordDate);
					if(phenos.size() >= 2  && updateExisting) {
						warningRows.add(row);
						dataValidationMessages.add("WARNING:  Subject " + subjectUID + " on row " + row + " has too many Pheno Collections to automatically update."
								+ " If you continue, no changes will be made to this subject.");
					} else if(uidsToUpdateReference.contains(subjectUID)){
						for(PhenoDataSetFieldDisplay pfd : cfdsThatWeNeed){
							errorCells.add(new ArkGridCell(csvReader.getIndex(pfd.getPhenoDataSetField().getName()), row));
						}
						errorCells.add(new ArkGridCell(0, row));
						dataValidationMessages.add("ERROR:  Subject " + subjectUID + " on row " + row + " is listed multiple times in this file.  " +
								"Please remove this row and retry.");
					}
					else{
						uidsToUpdateReference.add(subjectUID);
						PhenoDataSetField phenoDataSetField = null;		
						for(PhenoDataSetFieldDisplay pfd : cfdsThatWeNeed){
							phenoDataSetField	 = pfd.getPhenoDataSetField();
							String theDataAsString = csvReader.get(pfd.getPhenoDataSetField().getName());
							if(theDataAsString!=null && !theDataAsString.isEmpty()){
								//TODO : also check if the value == "missingvaluePatternThingy" , then dont validate
								if(phenoDataSetField.getMissingValue()!=null && phenoDataSetField.getMissingValue().toString().equalsIgnoreCase(theDataAsString)){
									//then move on and don't validate it...it goes straight in
								}
								else
								{
									//log.info("customField = " + customField==null?"null":customField.getName());
									if(!validateFieldData(phenoDataSetField, theDataAsString, subjectUID, dataValidationMessages, phenoDataSetField.getAllowMultiselect())){
										errorCells.add(new ArkGridCell(csvReader.getIndex(pfd.getPhenoDataSetField().getName()), row));
									}								
								}
							}
						}
						if(phenos.size() == 1 && updateExisting) {
							PhenoDataSetCollection existingCollection = phenos.get(0);
							for(PhenoDataSetFieldDisplay pfd : cfdsThatWeNeed) { //TODO: Optimize to not need multiple loops.
								for(PhenoDataSetData phenoData : existingCollection.getPhenoDataSetData()) {
									if(phenoData.getPhenoDataSetFieldDisplay().getId() == pfd.getId()) {
										int index = csvReader.getIndex(pfd.getPhenoDataSetField().getName());
										switch (phenoData.getPhenoDataSetFieldDisplay().getPhenoDataSetField().getFieldType().getName()) {
											case Constants.FIELD_TYPE_CHARACTER:
												if(!phenoData.getTextDataValue().equals(stringLineArray[index])) {
													insertCells.add(new ArkGridCell(index, row));
												}
												break;
											case Constants.FIELD_TYPE_NUMBER:
												if(!phenoData.getNumberDataValue().equals(new Double(stringLineArray[index]))) {
													insertCells.add(new ArkGridCell(index, row));
												}
												break;
											case Constants.FIELD_TYPE_DATE:
												if(!(simpleDateFormat.format(phenoData.getDateDataValue()).equals(stringLineArray[index]))) {
													insertCells.add(new ArkGridCell(index, row));
												}
												break;
											default:
												break;
											}
									}
								}
							}						
						} else {
							existantSubjectUIDRows.add(row);
						}
					}
				}
				row++;
			}
		}
		catch (IOException ioe) {
			log.error("processMatrixSubjectFile IOException stacktrace:", ioe);
			throw new ArkSystemException("Unexpected I/O exception whilst reading the subject data file");
		}
		catch (Exception ex) {
			log.error("processMatrixSubjectFile Exception stacktrace:", ex);
			throw new ArkSystemException("Unexpected exception occurred when trying to process subject data file");
		}
		finally {
			if (csvReader != null) {
				try {
					csvReader.close();
				}
				catch (Exception ex) {
					log.error("Cleanup operation failed: csvRdr.close()", ex);
				}
			}
			if (inputStreamReader != null) {
				try {
					inputStreamReader.close();
				}
				catch (Exception ex) {
					log.error("Cleanup operation failed: isr.close()", ex);
				}
			}
		}

		//TODO:  test hashset this i.intvalue or left hashset value??
		for (Iterator<Integer> iterator = nonExistantUIDs.iterator(); iterator.hasNext();) {
			Integer i = (Integer) iterator.next();
			dataValidationMessages.add("ERROR:  Subject on row " + i.intValue() + " does not exist in the database.  Please remove this row and retry or run upload/create this subject first.");
		}
		return dataValidationMessages;
	}

	/**
	 * Returns true of the field data value is a valid format, either NUMBER, CHARACTER or DATE as specified in the data dictionary
	 * 
	 * @param customField
	 * @return boolean
	 */
	public static boolean isValidFieldData(PhenoDataSetField phenoDataSetField, String value, String subjectUID, java.util.Collection<String> errorMessages) {
		boolean isValidFieldData = true;
		//TODO ASAP is null coming in acceptable? or do we just just check before call... if value null return false?
		
		// Number field type
		if (phenoDataSetField.getFieldType().getName().equalsIgnoreCase(Constants.FIELD_TYPE_NUMBER)) {
			try {
				Double.parseDouble(value);
			}
			catch (NumberFormatException nfe) {
				errorMessages.add(fieldDataNotDefinedType(phenoDataSetField, value, subjectUID));
				log.error("Field data number format exception " + nfe.getMessage());
				isValidFieldData = false;
			}
			catch (NullPointerException npe) {
				log.error("Field data null pointer exception " + npe.getMessage());
				isValidFieldData = false;
			}
		}

		// Character field type
		if (phenoDataSetField.getFieldType().getName().equalsIgnoreCase(Constants.FIELD_TYPE_CHARACTER)) {
			//TODO previously had simple null check by way of making an exception here.... what do we need to validation?	
		}

		// Date field type
		if (phenoDataSetField.getFieldType().getName().equalsIgnoreCase(Constants.FIELD_TYPE_DATE)) {
			try {//TODO : think about defining date format with the field, particularly after i18n and if datetime needed 
				DateFormat dateFormat = new SimpleDateFormat(au.org.theark.core.Constants.DD_MM_YYYY);
				dateFormat.setLenient(false);
				dateFormat.parse(value);
			}
			catch (ParseException pe) {
				errorMessages.add(fieldDataNotValidDate(phenoDataSetField, value, subjectUID));
				log.error("Field data date parse exception " + pe.getMessage());
				isValidFieldData = false;
			}
			catch (NullPointerException npe) {
				log.error("Field data null pointer exception " + npe.getMessage());
				isValidFieldData = false;
			}
		}

		return isValidFieldData;
	}

	/**
	 * Returns true if field data value is within the defined range as specified in the data dictionary
	 * 
	 * @param customField
	 * @return boolean
	 */
	public static boolean isInValidRange(PhenoDataSetField phenoDataSetField, String valueToValidate, String subjectUID, java.util.Collection<String> errorMessages) {
		boolean isInValidRange = true;
		//Field field = fieldData.getField();
		String minValue = phenoDataSetField.getMinValue();
		String maxValue = phenoDataSetField.getMaxValue();
		//log.warn("about to validate customField " + customField.getName() + " against value = " + valueToValidate);
		if(valueToValidate!=null && phenoDataSetField.getMissingValue()!=null && valueToValidate.trim().equalsIgnoreCase(phenoDataSetField.getMissingValue().trim())) {
			return isInValidRange;//TODO investigate 
		}

		if ((minValue == null || minValue.isEmpty()) && (maxValue == null || maxValue.isEmpty())) {
			return isInValidRange;
		}

		if (phenoDataSetField.getFieldType().getName().equalsIgnoreCase(Constants.FIELD_TYPE_NUMBER)) {
			try {
				Double doubleMinValue = Double.parseDouble(phenoDataSetField.getMinValue());
				Double doubleMaxValue = Double.parseDouble(phenoDataSetField.getMaxValue());
				Double doubleFieldValue = Double.parseDouble(valueToValidate);

				if ((doubleFieldValue > doubleMaxValue) || (doubleFieldValue < doubleMinValue)) {
					if ((doubleFieldValue > doubleMaxValue)) {
						errorMessages.add("ERROR:  Subject " + subjectUID + " has a value: " + valueToValidate + " which is greater than the maximum allowed value of " + doubleMaxValue);
					}
					if ((doubleFieldValue < doubleMinValue)) {
						errorMessages.add("ERROR:  Subject " + subjectUID + " has a value: " + valueToValidate + " which  is less than the minimum allowed value of " + doubleMinValue);
					}
					isInValidRange = false;
				}
			}
			catch (NumberFormatException nfe) {
				log.error("Field data number format exception " + nfe.getMessage());
				isInValidRange = false;
			}
			catch (NullPointerException npe) {
				log.error("Field data null pointer exception " + npe.getMessage());
				isInValidRange = false;
			}
		}
		else if (phenoDataSetField.getFieldType().getName().equalsIgnoreCase(Constants.FIELD_TYPE_DATE)) {
			if (phenoDataSetField.getMinValue() != null && phenoDataSetField.getMaxValue() != null) {
				try {
					DateFormat dateFormat = new SimpleDateFormat(au.org.theark.core.Constants.DD_MM_YYYY);
					dateFormat.setLenient(false);

					Date dateMinValue = dateFormat.parse(phenoDataSetField.getMinValue());
					Date dateMaxValue = dateFormat.parse(phenoDataSetField.getMaxValue());
					Date dateFieldValue = dateFormat.parse(valueToValidate);

					if (dateMaxValue.before(dateFieldValue) || dateMinValue.after(dateFieldValue)) {
						if (dateMaxValue.before(dateFieldValue)) {
							errorMessages.add(valueToValidate + " is greater than the maximum allowed value of " + dateMaxValue);
						}
						if (dateMinValue.after(dateFieldValue)) {
							errorMessages.add(valueToValidate + " is less than the minimum allowed value of " + dateMinValue);
						}
						isInValidRange = false;
					}
				}
				catch (ParseException pe) {
					isInValidRange = false;
				}
			}
		}
		//log.warn("about to return " + isInValidRange);
		return isInValidRange;
	}

	/**
	 * Returns true if the field data value is within the discrete range as defined in the data dictionary
	 * @param isMultiSelect 
	 * 
	 * @param customfield
	 * @return boolean
	 */
	public static boolean isInEncodedValues(PhenoDataSetField phenoDataSetField, String value, String subjectUID, java.util.Collection<String> errorMessages, boolean isMultiSelect) {
		boolean allInEncodedValues = true;
		if(phenoDataSetField.getMissingValue()!=null && value!=null && value.trim().equalsIgnoreCase(phenoDataSetField.getMissingValue().trim())) {
			return true;
		}

		// Validate if encoded values is definedisInEncodedValues, and not a DATE fieldType
		if (phenoDataSetField.getEncodedValues() != null 
				&& !phenoDataSetField.getEncodedValues().isEmpty() 
				&& !phenoDataSetField.getFieldType().getName().equalsIgnoreCase(Constants.FIELD_TYPE_DATE)) {

			List<String> allMyValues = new ArrayList<String>();
			
			try {
				StringTokenizer tokenSpacestringTokenizer = new StringTokenizer(value, Constants.ENCODED_VALUES_FROM_TELEFORMS_TOKEN_SPACE);
				if(isMultiSelect){					
					// Iterate through all discrete defined values and compare to field data value
					while (tokenSpacestringTokenizer.hasMoreTokens()) {
						String encodedValueToken = tokenSpacestringTokenizer.nextToken();
	
						log.info("envoded:" + encodedValueToken);
						allMyValues.add(encodedValueToken);
					}

				}
				else if (!isMultiSelect 
							&& tokenSpacestringTokenizer.countTokens()>1){
					errorMessages.add(fieldDataDoesNotAllowMultiSelectedEncodedValue(phenoDataSetField, value, subjectUID));
					return false;
				}
				else{
					allMyValues.add(value);
				}
				
				for(String currentValue : allMyValues){
					boolean inEncodedValues = false;
					
					StringTokenizer stringTokenizer = new StringTokenizer(phenoDataSetField.getEncodedValues(), Constants.ENCODED_VALUES_TOKEN);
	
					// Iterate through all discrete defined values and compare to field data value
					while (stringTokenizer.hasMoreTokens()) {
						String encodedValueToken = stringTokenizer.nextToken();
						StringTokenizer encodedValueSeparator = new StringTokenizer(encodedValueToken, Constants.ENCODED_VALUES_SEPARATOR);
						String encodedValue = encodedValueSeparator.nextToken().trim();
	
						if (encodedValue.equalsIgnoreCase(currentValue)) {
							inEncodedValues = true;
							break;
						}
					}

					if (!inEncodedValues) {
						errorMessages.add(fieldDataNotInEncodedValues(phenoDataSetField, value, subjectUID, isMultiSelect));
						allInEncodedValues = false;
					}
	
				}
			}
			catch (NullPointerException npe) {
				log.error("Field data null format exception " + npe.getMessage());
				errorMessages.add("Unexpected data issue while validating an encoded value.  Please contact your System Administrator");
				return false;
			}

		}
		return allInEncodedValues;
	}

	/**
	 * Returns true of the field data value is a valididated
	 * 
	 * @param customField
	 * @param errorMessages
	 * @param isMultiSelect 
	 * @return boolean
	 */
	public static boolean validateFieldData(PhenoDataSetField phenoDataSetField, String value, String subjectUID, java.util.Collection<String> errorMessages, boolean isMultiSelect) {
		boolean isValid = true;
		boolean isValidFieldData = true;
		boolean isValidEncodedValues = true;
		boolean isValidRange = true;

		isValidFieldData = isValidFieldData(phenoDataSetField, value, subjectUID, errorMessages);
		//log.info("isValidFieldData " + isValidFieldData );
		isValidEncodedValues = isInEncodedValues(phenoDataSetField,value, subjectUID, errorMessages, isMultiSelect);
		//log.info("isValidEncodedValues " + isValidEncodedValues );
		isValidRange = isInValidRange(phenoDataSetField, value, subjectUID, errorMessages);
		//log.info("isInValidRange " + isValidRange );
		isValid = (isValidFieldData && isValidEncodedValues && isValidRange);
		//log.info("isvalidoverall " + isValid );
		return (isValid);
	}

	/**
	 * Returns field not of the defined type error message
	 * 
	 * @param field
	 * @param fieldData
	 * @return String
	 */
	public static String fieldDataSubjectUidNotFound(String subjectUid) {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append("Subject UID: ");
		stringBuffer.append(subjectUid);
		stringBuffer.append(" ");
		stringBuffer.append("was not found in the database. Please check and try again.");
		return (stringBuffer.toString());
	}

	/**
	 * Returns field greater than defined max value error message
	 * TODO: Remove after change to new tables
	 * @param field
	 * @param fieldData
	 * @return String
	 */
	public static String fieldDataGreaterThanMaxValue(PhenoDataSetField field, String value, String subjectUID) {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append("Subject UID: ");
		stringBuffer.append(subjectUID);
		stringBuffer.append(": ");
		stringBuffer.append("The field ");
		stringBuffer.append(field.getName().toString());
		stringBuffer.append(" value ");
		stringBuffer.append(value);
		stringBuffer.append(" is greater than the defined maximum value: ");
		stringBuffer.append(field.getMaxValue());
		return (stringBuffer.toString());
	}

	/**
	 * Returns field less than defined min value error message
	 * TODO: Remove after change to new tables
	 * @param field
	 * @param fieldData
	 * @return String
	 */
	public static String fieldDataLessThanMinValue(PhenoDataSetField field, String value, String subjectUID) {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append("Subject UID: ");
		stringBuffer.append(subjectUID);
		stringBuffer.append(": ");
		stringBuffer.append("The field ");
		stringBuffer.append(field.getName().toString());
		stringBuffer.append(" value ");
		stringBuffer.append(value);
		stringBuffer.append(" is less than the defined minimum value: ");
		stringBuffer.append(field.getMinValue());
		return (stringBuffer.toString());
	}

	/**
	 * Returns field not within the defined encoded values error message
	 * TODO: Remove after change to new tables
	 * @param field
	 * @param isMultiSelect 
	 * @param fieldData
	 * @return String
	 */
	public static String fieldDataNotInEncodedValues(PhenoDataSetField field, String value, String subjectUID, boolean isMultiSelect) {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append("Subject UID: ");
		stringBuffer.append(subjectUID);
		stringBuffer.append(": ");
		stringBuffer.append("The field ");
		stringBuffer.append(field.getName().toString());
		stringBuffer.append(" value ");
		stringBuffer.append(value);
		stringBuffer.append(isMultiSelect?" has value(s) not in the expected encoded values: ":" is not in the expected encoded values: ");
		stringBuffer.append(field.getEncodedValues().replace('\n', ' '));
		return (stringBuffer.toString());
	}
	
	/**
	 * Returns field doesn't allow multiselect values
	 * @param field
	 * @param value
	 * @param subjectUID
	 * @return String
	 */
	public static String fieldDataDoesNotAllowMultiSelectedEncodedValue(PhenoDataSetField field, String value, String subjectUID) {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append("Subject UID: ");
		stringBuffer.append(subjectUID);
		stringBuffer.append(": ");
		stringBuffer.append("The field ");
		stringBuffer.append(field.getName().toString());
		stringBuffer.append(" trying to load multiple selections into a field which doesn't allow multiselect values. Select one value from ");
		stringBuffer.append(value);
		return (stringBuffer.toString());
	}

	/**
	 * Returns field not a valid date format error message
	 * TODO: Remove after change to new tables
	 * @param field
	 * @param fieldData
	 * @return String
	 */
	public static String fieldDataNotValidDate(PhenoDataSetField field, String value, String subjectUID) {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append("Error: ");
		stringBuffer.append("Subject UID: ");
		stringBuffer.append(subjectUID);
		stringBuffer.append(": ");
		stringBuffer.append("The field ");
		stringBuffer.append(field.getName().toString());
		stringBuffer.append(" value ");
		stringBuffer.append(value);
		stringBuffer.append(" is not in the valid date format of: ");
		stringBuffer.append(Constants.DD_MM_YYYY.toLowerCase());
		return (stringBuffer.toString());
	}

	/**
	 * Returns field not of the defined type error message
	 * 
	 * @param field
	 * @param fieldData
	 * @return String
	 **/
	public static String fieldDataNotDefinedType(PhenoDataSetField field, String value, String subjectUID) {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append("Subject UID: ");
		stringBuffer.append(subjectUID);
		stringBuffer.append(": ");
		stringBuffer.append("The field ");
		stringBuffer.append(field.getName().toString());
		stringBuffer.append(" value ");
		stringBuffer.append(value);
		stringBuffer.append(" is not the defined field type: ");
		stringBuffer.append(field.getFieldType().getName());
		return (stringBuffer.toString());
	}
	
	/**
	 * Returns dateCollected not a valid date format error message
	 * 
	 * @param subjectUid
	 * @param dateCollectedStr
	 * @return String
	 */
	public static String dateCollectedNotValidDate(String subjectUid, String dateCollectedStr) {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append("Error: ");
		stringBuffer.append("Subject UID: ");
		stringBuffer.append(subjectUid);
		stringBuffer.append(": ");
		stringBuffer.append(" with the date collected: ");
		stringBuffer.append(dateCollectedStr);
		stringBuffer.append(" is not in the valid date format of: ");
		stringBuffer.append(Constants.DD_MM_YYYY.toLowerCase());
		return (stringBuffer.toString());
	}


	public static String fieldNotFound(String fieldName) {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append("Error: ");
		stringBuffer.append("The field ");
		stringBuffer.append(fieldName);
		stringBuffer.append(" was not found in the database. Please check the name and try again, or add the field to the Data Dictionary.");
		return (stringBuffer.toString());
	}

	/**
	 * Returns status not valid error message
	 * 
	 * @param subjectUid
	 * @param statusStr
	 * @return String
	 */
	public static String statusNotValid(String subjectUid, String statusStr) {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append("Error: ");
		stringBuffer.append("Subject UID: ");
		stringBuffer.append(subjectUid);
		stringBuffer.append(": ");
		stringBuffer.append(" with the status: ");
		stringBuffer.append(statusStr);
		stringBuffer.append(" is not amongst the valid status options.");
		return (stringBuffer.toString());
	}
}
