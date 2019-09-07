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
package au.org.theark.core.dao;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;

import au.org.theark.core.model.pheno.entity.PhenoDataSetFieldDisplay;
import org.apache.commons.lang.time.DateUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import au.org.theark.core.Constants;
import au.org.theark.core.model.lims.entity.BioCollection;
import au.org.theark.core.model.lims.entity.Biospecimen;
import au.org.theark.core.model.pheno.entity.PhenoDataSetCollection;
import au.org.theark.core.model.report.entity.BiocollectionFieldSearch;
import au.org.theark.core.model.report.entity.BiospecimenField;
import au.org.theark.core.model.report.entity.ConsentStatusField;
import au.org.theark.core.model.report.entity.DemographicField;
import au.org.theark.core.model.report.entity.FieldCategory;
import au.org.theark.core.model.report.entity.Search;
import au.org.theark.core.model.study.entity.CustomFieldDisplay;
import au.org.theark.core.model.study.entity.LinkSubjectStudy;
import au.org.theark.core.util.CsvWriter;
import au.org.theark.core.vo.DataExtractionVO;
import au.org.theark.core.vo.ExtractionVO;
import au.org.theark.core.vo.LinkedExtractionVO;

/**
 * @author cellis
 * @param <T>
 * 
 */
@Repository("dataExtractionDao")
public class DataExtractionDao<T> extends HibernateSessionDao implements IDataExtractionDao {
	private static Logger	log	= LoggerFactory.getLogger(DataExtractionDao.class);

	/**
	 * Simple export to CSV as first cut
	 */
	public File createSubjectDemographicCSV(Search search, DataExtractionVO devo, List<DemographicField> allSubjectFields, List<CustomFieldDisplay> cfds, FieldCategory fieldCategory) {
		final String tempDir = System.getProperty("java.io.tmpdir");
		String filename = new String("SUBJECTDEMOGRAPHICS.csv");
		final java.io.File file = new File(tempDir, filename);
		if (filename == null || filename.isEmpty()) {
			filename = "exportcsv.csv";
		}

		HashMap<String, ExtractionVO> hashOfSubjectsWithData = devo.getDemographicData();
		HashMap<String, ExtractionVO> hashOfSubjectCustomData = devo.getSubjectCustomData();

		OutputStream outputStream;
		try {
			outputStream = new FileOutputStream(file);
			CsvWriter csv = new CsvWriter(outputStream);

			// Header
			csv.write("SUBJECTUID");
			
			Set<String> demofields = new HashSet<String>();
			for (String subjectUID : hashOfSubjectsWithData.keySet()) {
				HashMap<String, String> keyValues = hashOfSubjectsWithData.get(subjectUID).getKeyValues();
				demofields.addAll((keyValues.keySet()));
			}
			for(String demoField : demofields) {
				csv.write(demoField);
			}
			for (CustomFieldDisplay cfd : cfds) {
				csv.write(cfd.getCustomField().getName());
			}
			csv.endLine();

			for (String subjectUID : hashOfSubjectsWithData.keySet()) {
				csv.write(subjectUID);

				for (String demoField : demofields) {
					HashMap<String, String> keyValues = hashOfSubjectsWithData.get(subjectUID).getKeyValues();
					csv.write(keyValues.get(demoField));
					
				}

				/**
				 * for (String subjectUID : hashOfSubjectsWithData.keySet()) { HashMap<String, String> keyValues =
				 * hashOfSubjectsWithData.get(subjectUID).getKeyValues(); log.info(subjectUID + " has " + keyValues.size() + "demo fields"); //
				 * remove(subjectUID).getKeyValues().size() + "demo fields"); for (String key : keyValues.keySet()) { log.info("     key=" + key +
				 * "\t   value=" + keyValues.get(key)); } }
				 */
				ExtractionVO evo = hashOfSubjectCustomData.get(subjectUID);
				if (evo != null) {
					HashMap<String, String> keyValues = evo.getKeyValues();
					for (CustomFieldDisplay cfd : cfds) {

						String valueResult = keyValues.get(cfd.getCustomField().getName());
						if (cfd.getCustomField().getFieldType().getName().equalsIgnoreCase(Constants.FIELD_TYPE_DATE) && valueResult != null) {
							try {
								DateFormat dateFormat = new SimpleDateFormat(au.org.theark.core.Constants.DD_MM_YYYY);
								String[] dateFormats = { au.org.theark.core.Constants.DD_MM_YYYY, au.org.theark.core.Constants.yyyy_MM_dd_hh_mm_ss_S };
								Date date = DateUtils.parseDate(valueResult, dateFormats);
								csv.write(dateFormat.format(date));
							}
							catch (ParseException e) {
								csv.write(valueResult);
							}
						}
						else {
							csv.write(valueResult);
						}
					}
				}
				else {
					// Write out a line with no values (no data existed for subject in question
					for (CustomFieldDisplay cfd : cfds) {
						csv.write("");
					}
				}

				csv.endLine();
			}
			csv.close();
		}
		catch (FileNotFoundException e) {
			log.error(e.getMessage());
		}

		return file;
	}

	/**
	 * 
	 * Simple export to CSV as Biocollection Data
	 * 
	 */
	public File createBiocollectionCSV(Search search, DataExtractionVO devo, List<CustomFieldDisplay> cfds, FieldCategory fieldCategory) {
		final String tempDir = System.getProperty("java.io.tmpdir");
		String filename = new String("BIOCOLLECTION.csv");
		final java.io.File file = new File(tempDir, filename);
		if (filename == null || filename.isEmpty()) {
			filename = "exportBiocollectioncsv.csv";
		}
		HashMap<String, ExtractionVO> hashOfBiocollectionsWithData = devo.getBiocollectionData();
		HashMap<String, ExtractionVO> hashOfBiocollectionCustomData = devo.getBiocollectionCustomData();

		Set<String> bioCollectionUids = new HashSet<String>();
		bioCollectionUids.addAll(hashOfBiocollectionsWithData.keySet());
		bioCollectionUids.addAll(hashOfBiocollectionCustomData.keySet());

		OutputStream outputStream;
		try {
			outputStream = new FileOutputStream(file);
			CsvWriter csv = new CsvWriter(outputStream);

			// Header
			csv.write("SUBJECTID");
			csv.write("BIOCOLLECTIONUID");
			
			for (BiocollectionFieldSearch bcfs : search.getBiocollectionFieldsToReturn()) {
				csv.write(bcfs.getBiocollectionField().getPublicFieldName());
			}
			for (CustomFieldDisplay cfd : cfds) {
				csv.write(cfd.getCustomField().getName());
			}

			csv.endLine();

			for (String biocollectionUID : bioCollectionUids) {
				ExtractionVO hash = hashOfBiocollectionsWithData.get(biocollectionUID);
				csv.write(hash.getSubjectUid());
				csv.write(biocollectionUID);
				
				for (BiocollectionFieldSearch bcfs : search.getBiocollectionFieldsToReturn()) {
					
					if(hash!=null){
						HashMap<String, String> keyValues = hash.getKeyValues();
						//csv.write(keyValues.get(bcfs.getBiocollectionField().getPublicFieldName()));
						
						String valueResult = keyValues.get(bcfs.getBiocollectionField().getPublicFieldName());
						if (bcfs.getBiocollectionField().getFieldType().getName().equalsIgnoreCase(Constants.FIELD_TYPE_DATE) && valueResult != null) {
							try {
								DateFormat dateFormat = new SimpleDateFormat(au.org.theark.core.Constants.DD_MM_YYYY);
								String[] dateFormats = { au.org.theark.core.Constants.DD_MM_YYYY, au.org.theark.core.Constants.yyyy_MM_dd_hh_mm_ss_S };
								Date date = DateUtils.parseDate(valueResult, dateFormats);
								csv.write(dateFormat.format(date));
							}
							catch (ParseException e) {
								csv.write(valueResult);
							}
						}
						else {
							csv.write(valueResult);
						}
						
					}
				}
				/*
				 * for(CustomFieldDisplay cfd : cfds) { HashMap<String, String> keyValues =
				 * hashOfBiocollectionCustomData.get(biocollectionUID).getKeyValues(); String valueResult = keyValues.get(cfd.getCustomField().getName());
				 * if (cfd.getCustomField().getFieldType().getName().equalsIgnoreCase(Constants.FIELD_TYPE_DATE) && valueResult != null) { DateFormat
				 * dateFormat = new SimpleDateFormat(au.org.theark.core.Constants.DD_MM_YYYY); dateFormat.setLenient(false); try { String date =
				 * dateFormat.format(dateFormat.parse(valueResult)); csv.write(date); } catch (ParseException e) { csv.write(valueResult); }
				 * csv.write(valueResult); } else { csv.write(valueResult); } } csv.endLine();
				 */

				ExtractionVO evo = hashOfBiocollectionCustomData.get(biocollectionUID);
				if (evo != null) {
					HashMap<String, String> keyValues = evo.getKeyValues();
					for (CustomFieldDisplay cfd : cfds) {

						String valueResult = keyValues.get(cfd.getCustomField().getName());
						if (cfd.getCustomField().getFieldType().getName().equalsIgnoreCase(Constants.FIELD_TYPE_DATE) && valueResult != null) {
							try {
								DateFormat dateFormat = new SimpleDateFormat(au.org.theark.core.Constants.DD_MM_YYYY);
								String[] dateFormats = { au.org.theark.core.Constants.DD_MM_YYYY, au.org.theark.core.Constants.yyyy_MM_dd_hh_mm_ss_S };
								Date date = DateUtils.parseDate(valueResult, dateFormats);
								csv.write(dateFormat.format(date));
							}
							catch (ParseException e) {
								csv.write(valueResult);
							}
						}
						else {
							csv.write(valueResult);
						}
					}
				}
				else {
					// Write out a line with no values (no data existed for subject in question
					for (CustomFieldDisplay cfd : cfds) {
						csv.write("");
					}
				}
				csv.endLine();
			}
			csv.close();
		}
		catch (FileNotFoundException e) {
			log.error(e.getMessage());
		}

		return file;
	}

	/**
	 * 
	 * Simple export to CSV as Biospecimen Data
	 * 
	 */
	public File createBiospecimenCSV(Search search, DataExtractionVO devo, List<BiospecimenField> bsfs, List<CustomFieldDisplay> cfds, FieldCategory fieldCategory) {
		final String tempDir = System.getProperty("java.io.tmpdir");
		String filename = new String("BIOSPECIMEN.csv");
		final java.io.File file = new File(tempDir, filename);
		if (filename == null || filename.isEmpty()) {
			filename = "exportBiospecimencsv.csv";
		}
		OutputStream outputStream;
		HashMap<String, ExtractionVO> hashOfBiospecimensWithData = devo.getBiospecimenData();
		HashMap<String, ExtractionVO> hashOfBiospecimenCustomData = devo.getBiospecimenCustomData();

		Set<String> biospecimens = new HashSet<String>();
		biospecimens.addAll(hashOfBiospecimensWithData.keySet());
		biospecimens.addAll(hashOfBiospecimenCustomData.keySet());

		try {
			outputStream = new FileOutputStream(file);
			CsvWriter csv = new CsvWriter(outputStream);

			// Header
			csv.write("SUBJECTUID");
			csv.write("BIOSPECIMENUID");
			for (BiospecimenField bsf : bsfs) {
				if (!bsf.getPublicFieldName().equalsIgnoreCase("biospecimenUid"))
					csv.write(bsf.getPublicFieldName());
			}
			for (CustomFieldDisplay cfd : cfds) {
				csv.write(cfd.getCustomField().getName());
			}
			csv.endLine();

			for (String biospecimenUID : biospecimens) {
				ExtractionVO evo = hashOfBiospecimensWithData.get(biospecimenUID);
				if(evo != null && evo.getSubjectUid() != null){
					csv.write(evo.getSubjectUid());
					csv.write(biospecimenUID);
					
					for (BiospecimenField bsf : bsfs) {
						
						if(evo != null){
							HashMap<String, String> keyValues = evo.getKeyValues();
							if (!bsf.getPublicFieldName().equalsIgnoreCase("biospecimenUid")){
								String valueResult = keyValues.get(bsf.getPublicFieldName());
								if (bsf.getFieldType().getName().equalsIgnoreCase(Constants.FIELD_TYPE_DATE) && valueResult != null) {
									try {
										DateFormat dateFormat = new SimpleDateFormat(au.org.theark.core.Constants.DD_MM_YYYY);
										String[] dateFormats = { au.org.theark.core.Constants.DD_MM_YYYY, au.org.theark.core.Constants.yyyy_MM_dd_hh_mm_ss_S };
										Date date = DateUtils.parseDate(valueResult, dateFormats);
										csv.write(dateFormat.format(date));
									}
									catch (ParseException e) {
										csv.write(valueResult);
									}
								}
								else {
									csv.write(valueResult);
								}
							}
							
							
						}
					}
				}

				evo = new ExtractionVO();
				evo = hashOfBiospecimenCustomData.get(biospecimenUID);
				if (evo != null) {
					HashMap<String, String> keyValues = evo.getKeyValues();
					for (CustomFieldDisplay cfd : cfds) {

						String valueResult = keyValues.get(cfd.getCustomField().getName());
						if (cfd.getCustomField().getFieldType().getName().equalsIgnoreCase(Constants.FIELD_TYPE_DATE) && valueResult != null) {
							try {
								DateFormat dateFormat = new SimpleDateFormat(au.org.theark.core.Constants.DD_MM_YYYY);
								String[] dateFormats = { au.org.theark.core.Constants.DD_MM_YYYY, au.org.theark.core.Constants.yyyy_MM_dd_hh_mm_ss_S };
								Date date = DateUtils.parseDate(valueResult, dateFormats);
								csv.write(dateFormat.format(date));
							}
							catch (ParseException e) {
								csv.write(valueResult);
							}
						}
						else {
							csv.write(valueResult);
						}
					}
				}
				else {
					// Write out a line with no values (no data existed for subject in question
					for (CustomFieldDisplay cfd : cfds) {
						csv.write("");
					}
				}
				csv.endLine();
			}
			csv.close();
		}
		catch (FileNotFoundException e) {
			log.error(e.getMessage());
		}

		return file;
	}

	public File createBiospecimenDataCustomCSV(Search search, DataExtractionVO devo, List<CustomFieldDisplay> cfds, FieldCategory fieldCategory) {
		HashMap<String, ExtractionVO> hashOfBiospecimenCustomData = devo.getBiospecimenCustomData();
		log.info(" writing out biospecimenCustomData " + hashOfBiospecimenCustomData.size() + " entries for category '" + fieldCategory + "'");

		final String tempDir = System.getProperty("java.io.tmpdir");
		String filename = new String("BIOSPECIMENCUSTOMDATA.csv");
		final java.io.File file = new File(tempDir, filename);
		if (filename == null || filename.isEmpty()) {
			filename = "exportBiospecimenCustomcsv.csv";
		}
		OutputStream outputStream;
		try {
			outputStream = new FileOutputStream(file);
			CsvWriter csv = new CsvWriter(outputStream);

			csv.write("SUBJECTUID");

			// Header

			for (String key : hashOfBiospecimenCustomData.keySet()) {
				HashMap<String, String> keyValues = hashOfBiospecimenCustomData.get(key).getKeyValues();
				for (String key2 : keyValues.keySet()) {
					csv.write(key2);
				}
				break;
			}
			csv.endLine();

			for (String subjectUID : hashOfBiospecimenCustomData.keySet()) {
				HashMap<String, String> keyValues = hashOfBiospecimenCustomData.get(subjectUID).getKeyValues();
				for (String key : keyValues.keySet()) {
					csv.write(keyValues.get(keyValues.get(key)));
				}
				csv.endLine();
			}
			csv.close();
		}
		catch (FileNotFoundException e) {
			log.error(e.getMessage());
		}

		return file;
	}
	
	public File listToCSV(List<String> list) {
		final String tempDir = System.getProperty("java.io.tmpdir");
		String filename = new String("tmp.csv");
		final java.io.File file = new File(tempDir, filename);

		OutputStream outputStream;
		try {
			outputStream = new FileOutputStream(file);
			CsvWriter csv = new CsvWriter(outputStream);
			
			for (String value : list){
				csv.write(value);
				csv.endLine();
			}
		}
		catch(FileNotFoundException fne){
			
		}
		return file;
	}
	

	public File createGenoCSV(Search search, DataExtractionVO devo, FieldCategory fieldCategory, Long maxProcessesPerPipeline, Map<Long, Long> maxInputList, Map<Long, Long> maxOutputList){
		final String tempDir = System.getProperty("java.io.tmpdir");
		String filename = new String("GENO.csv");
		final java.io.File file = new File(tempDir, filename);
		
		List<LinkedExtractionVO> genoData = devo.getGenoData();

		OutputStream outputStream;
		try {
			outputStream = new FileOutputStream(file);
			CsvWriter csv = new CsvWriter(outputStream);

			// Header
			csv.write("SUBJECTUID");
			//csv.write("RECORD_DATE_TIME");??

			csv.write(Constants.GENO_FIELDS_PIPELINE_ID);
			csv.write(Constants.GENO_FIELDS_PIPELINE_NAME);
			csv.write(Constants.GENO_FIELDS_PIPELINE_DECSRIPTION);
			for (int processIndex=1; processIndex<=maxProcessesPerPipeline ; processIndex++) { //one-based humanized
				//process
				csv.write(Constants.GENO_FIELDS_PROCESS_ID + "_forProcess_" + processIndex);
				csv.write(Constants.GENO_FIELDS_PROCESS_NAME + "_forProcess_" + processIndex);
				csv.write(Constants.GENO_FIELDS_PROCESS_DESCRIPTION + "_forProcess_" + processIndex);
				csv.write(Constants.GENO_FIELDS_PROCESS_START_TIME + "_forProcess_" + processIndex);
				csv.write(Constants.GENO_FIELDS_PROCESS_END_TIME + "_forProcess_" + processIndex);
				
				//commmand
				csv.write(Constants.GENO_FIELDS_PROCESS_COMMAND_SERVER_URL + "_" + processIndex);
				csv.write(Constants.GENO_FIELDS_PROCESS_COMMAND_NAME + "_" + processIndex);
				csv.write(Constants.GENO_FIELDS_PROCESS_COMMAND_LOCATION + "_" + processIndex);
//				csv.write(Constants.GENO_FIELDS_PROCESS_COMMAND_INPUT_FILE_FORMAT)//				csv.write(Constants.GENO_FIELDS_PROCESS_COMMAND_OUTPUT_FILE_FORMAT);
					
				//input
				//for each of the inputs..........!!!!  EACH - there COULD be more than one
				long maxInputsForThisProcess = 0L;
				if(maxInputList!=null && maxInputList.get(new Long(processIndex))!=null){
					maxInputsForThisProcess = maxInputList.get(new Long(processIndex)).longValue();
				}

				long maxOutputsForThisProcess = 0L;
				if(maxOutputList!=null && maxOutputList.get(new Long(processIndex))!=null){
					maxOutputsForThisProcess = maxOutputList.get(new Long(processIndex)).longValue();
				}

				/**why ther heck isnt this workinG?
				for(long inputIndex=1L ; inputIndex<=maxInputsForThisProcess ; inputIndex++){
					//csv.write(Constants.GENO_FIELDS_PROCESS_INPUT_SERVER);
					csv.write(Constants.GENO_FIELDS_PROCESS_INPUT_SERVER + (processIndex>1?("_forProcess_"+processIndex):"") + "_" + inputIndex );
					csv.write(Constants.GENO_FIELDS_PROCESS_INPUT_LOCATION + (processIndex>1?("_forProcess_"+processIndex):"") + "_" + inputIndex );
					csv.write(Constants.GENO_FIELDS_PROCESS_INPUT_FILE_HASH + (processIndex>1?("_forProcess_"+processIndex):"") + "_" + inputIndex );
					csv.write(Constants.GENO_FIELDS_PROCESS_INPUT_FILE_TYPE + (processIndex>1?("_forProcess_"+processIndex):"") + "_" + inputIndex );
					csv.write(Constants.GENO_FIELDS_PROCESS_INPUT_KEPT + (processIndex>1?("_forProcess_"+processIndex):"") + "_" + inputIndex );
				}*/
				long inputIndex=1L ; 
				while(inputIndex<=maxInputsForThisProcess ){
					//csv.write(Constants.GENO_FIELDS_PROCESS_INPUT_SERVER);
					csv.write(Constants.GENO_FIELDS_PROCESS_INPUT_SERVER 	+  "_" + inputIndex + "_forProcess_" + processIndex);
					csv.write(Constants.GENO_FIELDS_PROCESS_INPUT_LOCATION 	+ "_" + inputIndex + "_forProcess_" + processIndex );
					csv.write(Constants.GENO_FIELDS_PROCESS_INPUT_FILE_HASH + "_" + inputIndex + "_forProcess_" + processIndex );
					csv.write(Constants.GENO_FIELDS_PROCESS_INPUT_FILE_TYPE + "_" + inputIndex + "_forProcess_" + processIndex );
					csv.write(Constants.GENO_FIELDS_PROCESS_INPUT_KEPT  	+ "_" + inputIndex + "_forProcess_" + processIndex );
					inputIndex++;	
				}
				

				long outputIndex=1L ; 
				while(outputIndex<=maxOutputsForThisProcess ){
					//csv.write(Constants.GENO_FIELDS_PROCESS_OUTPUT_SERVER);
					csv.write(Constants.GENO_FIELDS_PROCESS_OUTPUT_SERVER 	 + "_" + outputIndex  + "_forProcess_" + processIndex);
					csv.write(Constants.GENO_FIELDS_PROCESS_OUTPUT_LOCATION  + "_" + outputIndex  + "_forProcess_" + processIndex);
					csv.write(Constants.GENO_FIELDS_PROCESS_OUTPUT_FILE_HASH + "_" + outputIndex + "_forProcess_" + processIndex );
					csv.write(Constants.GENO_FIELDS_PROCESS_OUTPUT_FILE_TYPE + "_" + outputIndex + "_forProcess_" + processIndex );
					csv.write(Constants.GENO_FIELDS_PROCESS_OUTPUT_KEPT  	 + "_" + outputIndex + "_forProcess_" + processIndex );
					outputIndex++;	
				}
				
				
		/*		long maxInputsForThisProcess = 0L;
				if(maxInputList!=null && maxInputList.get(new Long(processIndex))!=null){
					maxInputsForThisProcess = maxInputList.get(new Long(processIndex));
				}
				for(long inputIndex=1 ; inputIndex<maxInputsForThisProcess ; inputIndex++){
					//csv.write(Constants.GENO_FIELDS_PROCESS_INPUT_SERVER);
					csv.write(Constants.GENO_FIELDS_PROCESS_INPUT_SERVER + (processIndex>1?("_"+processIndex):"") + "_" + inputIndex );
					csv.write(Constants.GENO_FIELDS_PROCESS_INPUT_LOCATION + (processIndex>1?("_"+processIndex):"") + "_" + inputIndex );
					csv.write(Constants.GENO_FIELDS_PROCESS_INPUT_FILE_HASH + (processIndex>1?("_"+processIndex):"") + "_" + inputIndex );
					csv.write(Constants.GENO_FIELDS_PROCESS_INPUT_FILE_TYPE + (processIndex>1?("_"+processIndex):"") + "_" + inputIndex );
					csv.write(Constants.GENO_FIELDS_PROCESS_INPUT_KEPT + (processIndex>1?("_"+processIndex):"") + "_" + inputIndex );
				}*/
/*
				//input
				//for each of the inputs..........!!!!  EACH - there COULD be more than one
				csv.write(Constants.GENO_FIELDS_PROCESS_INPUT_SERVER + "_" + processIndex);
				csv.write(Constants.GENO_FIELDS_PROCESS_INPUT_LOCATION + "_" + processIndex);
				csv.write(Constants.GENO_FIELDS_PROCESS_INPUT_FILE_HASH + "_" + processIndex);
				csv.write(Constants.GENO_FIELDS_PROCESS_INPUT_FILE_TYPE + "_" + processIndex);
				csv.write(Constants.GENO_FIELDS_PROCESS_INPUT_KEPT + "_" + processIndex);
				
				//output
				//for each of the outputs..........!!!!  EACH - there COULD be more than one
				csv.write(Constants.GENO_FIELDS_PROCESS_OUTPUT_SERVER + "_forProcess_" + processIndex);
				csv.write(Constants.GENO_FIELDS_PROCESS_OUTPUT_LOCATION + "_forProcess_" + processIndex);
				csv.write(Constants.GENO_FIELDS_PROCESS_OUTPUT_FILE_HASH + "_forProcess_" + processIndex);
				csv.write(Constants.GENO_FIELDS_PROCESS_OUTPUT_FILE_TYPE + "_forProcess_" + processIndex);
				csv.write(Constants.GENO_FIELDS_PROCESS_OUTPUT_KEPT + "_forProcess_" + processIndex);
*/
			}
			csv.endLine();

			//now for the actual data
			for (LinkedExtractionVO evo : genoData) {
				//csv.write(evo.getSubjectUid());//ExtractionVO evo = genoData.get(phenoCollectionId);
				
				if (evo != null) {
					csv.write(evo.getSubjectUid());
					//csv.write(evo.getRecordDate());
					LinkedHashMap<String, String> keyValues = evo.getKeyValues();	//TODO:  Something should be done to check that values always match order of columns
/*order is based on the code that constructs this in study dao 
  				Pipeline pl = lssp.getPipeline();
				map.put(Constants.GENO_FIELDS_PIPELINE_ID, pl.getId().toString());
				map.put(Constants.GENO_FIELDS_PIPELINE_NAME, pl.getName());
				map.put(Constants.GENO_FIELDS_PIPELINE_DECSRIPTION, pl.getDescription());
				
				int index = 0;
				for(Process p : pl.getPipelineProcesses()){
					index++;
					
					//TODO : obvbiously need to pre=append the pipeline info/count too
					map.put((Constants.GENO_FIELDS_PROCESS_ID + (index>1?("_"+index):"")), p.getId().toString());
					map.put((Constants.GENO_FIELDS_PROCESS_NAME + (index>1?("_"+index):"")), p.getName());
					map.put((Constants.GENO_FIELDS_PROCESS_DESCRIPTION + (index>1?("_"+index):"")), p.getDescription());
					map.put((Constants.GENO_FIELDS_PROCESS_START_TIME + (index>1?("_"+index):"")), p.getStartTime()!=null?p.getStartTime().toString():"");
					map.put((Constants.GENO_FIELDS_PROCESS_END_TIME + (index>1?("_"+index):"")), p.getEndTime()!=null?p.getEndTime().toString():"");
					Command command = p.getCommand();
					map.put((Constants.GENO_FIELDS_PROCESS_COMMAND_NAME + (index>1?("_"+index):"")), (command==null?"":command.getName()));
					map.put((Constants.GENO_FIELDS_PROCESS_COMMAND_LOCATION + (index>1?("_"+index):"")), (command==null?"":command.getLocation()));
					map.put((Constants.GENO_FIELDS_PROCESS_COMMAND_SERVER_URL + (index>1?("_"+index):"")), (command==null?"":command.getServerUrl()));
*/
					for(String key : keyValues.keySet()){
						log.info(key + keyValues.get(key));
					}
					csv.write(keyValues.get(Constants.GENO_FIELDS_PIPELINE_ID));

					csv.write(keyValues.get(Constants.GENO_FIELDS_PIPELINE_NAME));
					csv.write(keyValues.get(Constants.GENO_FIELDS_PIPELINE_DECSRIPTION));
					for (int processIndex=1; processIndex<=maxProcessesPerPipeline ; processIndex++) { //one-based humanized
						if(processIndex==1){
							//process
							csv.write((keyValues.get(Constants.GENO_FIELDS_PROCESS_ID) == null)?"":keyValues.get(Constants.GENO_FIELDS_PROCESS_ID));
							csv.write((keyValues.get(Constants.GENO_FIELDS_PROCESS_NAME) == null)?"":keyValues.get(Constants.GENO_FIELDS_PROCESS_NAME));
							csv.write((keyValues.get(Constants.GENO_FIELDS_PROCESS_DESCRIPTION) == null)?"":keyValues.get(Constants.GENO_FIELDS_PROCESS_DESCRIPTION));
							csv.write((keyValues.get(Constants.GENO_FIELDS_PROCESS_START_TIME) == null)?"":keyValues.get(Constants.GENO_FIELDS_PROCESS_START_TIME));
							csv.write((keyValues.get(Constants.GENO_FIELDS_PROCESS_END_TIME)== null)?"":keyValues.get(Constants.GENO_FIELDS_PROCESS_END_TIME));
							// == null)?"":
							//commmand
							csv.write((keyValues.get(Constants.GENO_FIELDS_PROCESS_COMMAND_SERVER_URL)== null)?"":keyValues.get(Constants.GENO_FIELDS_PROCESS_COMMAND_SERVER_URL));
							csv.write((keyValues.get(Constants.GENO_FIELDS_PROCESS_COMMAND_NAME)== null)?"":keyValues.get(Constants.GENO_FIELDS_PROCESS_COMMAND_NAME));
							csv.write((keyValues.get(Constants.GENO_FIELDS_PROCESS_COMMAND_LOCATION)== null)?"":keyValues.get(Constants.GENO_FIELDS_PROCESS_COMMAND_LOCATION));
			//				csv.write(Constants.GENO_FIELDS_PROCESS_COMMAND_INPUT_FILE_FORMAT)//				csv.write(Constants.GENO_FIELDS_PROCESS_COMMAND_OUTPUT_FILE_FORMAT);
							
							long maxInputsForThisProcess = 0L;
							if(maxInputList!=null && maxInputList.get(new Long(processIndex))!=null){
								maxInputsForThisProcess = maxInputList.get(new Long(processIndex));
							}
							long inputIndex=1L;
							while(inputIndex<=maxInputsForThisProcess){
								//input
								//for each of the inputs..........!!!!  EACH - there COULD be more than one
								csv.write((keyValues.get(Constants.GENO_FIELDS_PROCESS_INPUT_SERVER + "_" + processIndex + "_" + inputIndex )== null)?"":keyValues.get(Constants.GENO_FIELDS_PROCESS_INPUT_SERVER + "_" + processIndex + "_" + inputIndex ));
								csv.write((keyValues.get(Constants.GENO_FIELDS_PROCESS_INPUT_LOCATION + "_" + processIndex + "_" + inputIndex )== null)?"":keyValues.get(Constants.GENO_FIELDS_PROCESS_INPUT_LOCATION + "_" + processIndex + "_" + inputIndex ));
								csv.write((keyValues.get(Constants.GENO_FIELDS_PROCESS_INPUT_FILE_HASH + "_" + processIndex + "_" + inputIndex )== null)?"":keyValues.get(Constants.GENO_FIELDS_PROCESS_INPUT_FILE_HASH + "_" + processIndex + "_" + inputIndex ));
								csv.write((keyValues.get(Constants.GENO_FIELDS_PROCESS_INPUT_FILE_TYPE + "_" + processIndex + "_" + inputIndex )== null)?"":keyValues.get(Constants.GENO_FIELDS_PROCESS_INPUT_FILE_TYPE + "_" + processIndex + "_" + inputIndex ));
								csv.write((keyValues.get(Constants.GENO_FIELDS_PROCESS_INPUT_KEPT + "_" + processIndex + "_" + inputIndex )== null)?"":keyValues.get(Constants.GENO_FIELDS_PROCESS_INPUT_KEPT + "_" + processIndex + "_" + inputIndex ));
								inputIndex++;
							}
							
							long maxOutputsForThisProcess = 0L;
							if(maxOutputList!=null && maxOutputList.get(new Long(processIndex))!=null){
								maxOutputsForThisProcess = maxOutputList.get(new Long(processIndex));
							}
							for(long outputIndex=1L ; outputIndex<=maxOutputsForThisProcess ; outputIndex++){
								//output
								//for each of the outputs..........!!!!  EACH - there COULD be more than one
								csv.write((keyValues.get(Constants.GENO_FIELDS_PROCESS_OUTPUT_SERVER + "_" + processIndex + "_" + outputIndex )== null)?"":keyValues.get(Constants.GENO_FIELDS_PROCESS_OUTPUT_SERVER + "_" + processIndex + "_" + outputIndex ));
								csv.write((keyValues.get(Constants.GENO_FIELDS_PROCESS_OUTPUT_LOCATION + "_" + processIndex + "_" + outputIndex )== null)?"":keyValues.get(Constants.GENO_FIELDS_PROCESS_OUTPUT_LOCATION + "_" + processIndex + "_" + outputIndex ));
								csv.write((keyValues.get(Constants.GENO_FIELDS_PROCESS_OUTPUT_FILE_HASH + "_" + processIndex + "_" + outputIndex )== null)?"":keyValues.get(Constants.GENO_FIELDS_PROCESS_OUTPUT_FILE_HASH + "_" + processIndex + "_" + outputIndex ));
								csv.write((keyValues.get(Constants.GENO_FIELDS_PROCESS_OUTPUT_FILE_TYPE + "_" + processIndex + "_" + outputIndex )== null)?"":keyValues.get(Constants.GENO_FIELDS_PROCESS_OUTPUT_FILE_TYPE + "_" + processIndex + "_" + outputIndex ));
								csv.write((keyValues.get(Constants.GENO_FIELDS_PROCESS_OUTPUT_KEPT + "_" + processIndex + "_" + outputIndex )== null)?"":keyValues.get(Constants.GENO_FIELDS_PROCESS_OUTPUT_KEPT + "_" + processIndex + "_" + outputIndex ));
							}

							log.info("\n\nprocess=" + processIndex + 
									"    \n max in=" + maxInputsForThisProcess + "   \n  max outs=" + maxOutputsForThisProcess
									+ "\nequation=" + (inputIndex<maxInputsForThisProcess)
									);
							
							/*
							//output
							//for each of the outputs..........!!!!  EACH - there COULD be more than one
							csv.write((keyValues.get(Constants.GENO_FIELDS_PROCESS_OUTPUT_SERVER)== null)?"":keyValues.get(Constants.GENO_FIELDS_PROCESS_OUTPUT_SERVER));
							csv.write((keyValues.get(Constants.GENO_FIELDS_PROCESS_OUTPUT_LOCATION)== null)?"":keyValues.get(Constants.GENO_FIELDS_PROCESS_OUTPUT_LOCATION));
							csv.write((keyValues.get(Constants.GENO_FIELDS_PROCESS_OUTPUT_FILE_HASH)== null)?"":keyValues.get(Constants.GENO_FIELDS_PROCESS_OUTPUT_FILE_HASH));
							csv.write((keyValues.get(Constants.GENO_FIELDS_PROCESS_OUTPUT_FILE_TYPE)== null)?"":keyValues.get(Constants.GENO_FIELDS_PROCESS_OUTPUT_FILE_TYPE));
							csv.write((keyValues.get(Constants.GENO_FIELDS_PROCESS_OUTPUT_KEPT)== null)?"":keyValues.get(Constants.GENO_FIELDS_PROCESS_OUTPUT_KEPT));*/
						}
						else{
							//process
							csv.write((keyValues.get(Constants.GENO_FIELDS_PROCESS_ID + "_" + processIndex)== null)?"":keyValues.get(Constants.GENO_FIELDS_PROCESS_ID + "_" + processIndex));
							csv.write((keyValues.get(Constants.GENO_FIELDS_PROCESS_NAME + "_" + processIndex)== null)?"":keyValues.get(Constants.GENO_FIELDS_PROCESS_NAME + "_" + processIndex));
							csv.write((keyValues.get(Constants.GENO_FIELDS_PROCESS_DESCRIPTION + "_" + processIndex)== null)?"":keyValues.get(Constants.GENO_FIELDS_PROCESS_DESCRIPTION + "_" + processIndex));
							csv.write((keyValues.get(Constants.GENO_FIELDS_PROCESS_START_TIME + "_" + processIndex)== null)?"":keyValues.get(Constants.GENO_FIELDS_PROCESS_START_TIME + "_" + processIndex));
							csv.write((keyValues.get(Constants.GENO_FIELDS_PROCESS_END_TIME + "_" + processIndex)== null)?"":keyValues.get(Constants.GENO_FIELDS_PROCESS_END_TIME + "_" + processIndex));
							
							//commmand
							csv.write((keyValues.get(Constants.GENO_FIELDS_PROCESS_COMMAND_SERVER_URL + "_" + processIndex)== null)?"":keyValues.get(Constants.GENO_FIELDS_PROCESS_COMMAND_SERVER_URL + "_" + processIndex));
							csv.write((keyValues.get(Constants.GENO_FIELDS_PROCESS_COMMAND_NAME + "_" + processIndex)== null)?"":keyValues.get(Constants.GENO_FIELDS_PROCESS_COMMAND_NAME + "_" + processIndex));
							csv.write((keyValues.get(Constants.GENO_FIELDS_PROCESS_COMMAND_LOCATION + "_" + processIndex)== null)?"":keyValues.get(Constants.GENO_FIELDS_PROCESS_COMMAND_LOCATION + "_" + processIndex));
			//				csv.write((Constants.GENO_FIELDS_PROCESS_COMMAND_INPUT_FILE_FORMAT)//				csv.write(Constants.GENO_FIELDS_PROCESS_COMMAND_OUTPUT_FILE_FORMAT));

							long maxInputsForThisProcess = 0L;
							if(maxInputList!=null && maxInputList.get(new Long(processIndex))!=null){
								maxInputsForThisProcess = maxInputList.get(new Long(processIndex));
							}

							long inputIndex=1L ;
							while(inputIndex<=maxInputsForThisProcess){
								log.info("pr=" + processIndex + " in=" + inputIndex);
								csv.write((keyValues.get(Constants.GENO_FIELDS_PROCESS_INPUT_SERVER + "_" + processIndex + "_" + inputIndex )== null)?"":keyValues.get(Constants.GENO_FIELDS_PROCESS_INPUT_SERVER + "_" + processIndex + "_" + inputIndex ));
								csv.write((keyValues.get(Constants.GENO_FIELDS_PROCESS_INPUT_LOCATION + "_" + processIndex + "_" + inputIndex )== null)?"":keyValues.get(Constants.GENO_FIELDS_PROCESS_INPUT_LOCATION + "_" + processIndex + "_" + inputIndex ));
								csv.write((keyValues.get(Constants.GENO_FIELDS_PROCESS_INPUT_FILE_HASH + "_" + processIndex + "_" + inputIndex )== null)?"":keyValues.get(Constants.GENO_FIELDS_PROCESS_INPUT_FILE_HASH + "_" + processIndex + "_" + inputIndex ));
								csv.write((keyValues.get(Constants.GENO_FIELDS_PROCESS_INPUT_FILE_TYPE + "_" + processIndex + "_" + inputIndex )== null)?"":keyValues.get(Constants.GENO_FIELDS_PROCESS_INPUT_FILE_TYPE + "_" + processIndex + "_" + inputIndex ));
								csv.write((keyValues.get(Constants.GENO_FIELDS_PROCESS_INPUT_KEPT + "_" + processIndex + "_" + inputIndex )== null)?"":keyValues.get(Constants.GENO_FIELDS_PROCESS_INPUT_KEPT + "_" + processIndex + "_" + inputIndex ));
								inputIndex++;
							}


							long maxOutputsForThisProcess = 0L;
							if(maxOutputList!=null && maxOutputList.get(new Long(processIndex))!=null){
								maxOutputsForThisProcess = maxOutputList.get(new Long(processIndex));
							}

							log.info("\n\nprocess=" + processIndex + 
									"    \n max in=" + maxInputsForThisProcess + "   \n  max outs=" + maxOutputsForThisProcess
									+ "\nequation=" + (inputIndex<maxInputsForThisProcess)
									);
							
							for(long outputIndex=1 ; outputIndex<=maxOutputsForThisProcess ; outputIndex++){
								//output
								//for each of the outputs..........!!!!  EACH - there COULD be more than one
								csv.write((keyValues.get(Constants.GENO_FIELDS_PROCESS_OUTPUT_SERVER + "_" + processIndex + "_" + outputIndex )== null)?"":keyValues.get(Constants.GENO_FIELDS_PROCESS_OUTPUT_SERVER + "_" + processIndex + "_" + outputIndex ));
								csv.write((keyValues.get(Constants.GENO_FIELDS_PROCESS_OUTPUT_LOCATION + "_" + processIndex + "_" + outputIndex )== null)?"":keyValues.get(Constants.GENO_FIELDS_PROCESS_OUTPUT_LOCATION + "_" + processIndex + "_" + outputIndex ));
								csv.write((keyValues.get(Constants.GENO_FIELDS_PROCESS_OUTPUT_FILE_HASH + "_" + processIndex + "_" + outputIndex )== null)?"":keyValues.get(Constants.GENO_FIELDS_PROCESS_OUTPUT_FILE_HASH + "_" + processIndex + "_" + outputIndex ));
								csv.write((keyValues.get(Constants.GENO_FIELDS_PROCESS_OUTPUT_FILE_TYPE + "_" + processIndex + "_" + outputIndex )== null)?"":keyValues.get(Constants.GENO_FIELDS_PROCESS_OUTPUT_FILE_TYPE + "_" + processIndex + "_" + outputIndex ));
								csv.write((keyValues.get(Constants.GENO_FIELDS_PROCESS_OUTPUT_KEPT + "_" + processIndex + "_" + outputIndex )== null)?"":keyValues.get(Constants.GENO_FIELDS_PROCESS_OUTPUT_KEPT + "_" + processIndex + "_" + outputIndex ));
							}

							/*output
							//for each of the outputs..........!!!!  EACH - there COULD be more than one
							csv.write((keyValues.get(Constants.GENO_FIELDS_PROCESS_OUTPUT_SERVER + "_" + processIndex)== null)?"":keyValues.get(Constants.GENO_FIELDS_PROCESS_OUTPUT_SERVER + "_" + processIndex));
							csv.write((keyValues.get(Constants.GENO_FIELDS_PROCESS_OUTPUT_LOCATION + "_" + processIndex)== null)?"":keyValues.get(Constants.GENO_FIELDS_PROCESS_OUTPUT_LOCATION + "_" + processIndex));
							csv.write((keyValues.get(Constants.GENO_FIELDS_PROCESS_OUTPUT_FILE_HASH + "_" + processIndex)== null)?"":keyValues.get(Constants.GENO_FIELDS_PROCESS_OUTPUT_FILE_HASH + "_" + processIndex));
							csv.write((keyValues.get(Constants.GENO_FIELDS_PROCESS_OUTPUT_FILE_TYPE + "_" + processIndex)== null)?"":keyValues.get(Constants.GENO_FIELDS_PROCESS_OUTPUT_FILE_TYPE + "_" + processIndex));
							csv.write((keyValues.get(Constants.GENO_FIELDS_PROCESS_OUTPUT_KEPT + "_" + processIndex)== null)?"":keyValues.get(Constants.GENO_FIELDS_PROCESS_OUTPUT_KEPT + "_" + processIndex));
							 */
						}
					}
					
					//TODO:  replace this with hardcoded use of the field names or use the function if 
					//		available to getColumnOf(GENO_FIELD_PIPELINE_ID) and put it in there at appropriate line
					
					/*
					 * TODO ASAP  : Change this to iterating in order so naming remains the same
					 *
					 * THIS CODE WORKS BUT WE MAY WANT TO MAKE SURE THAT WE HAVE THE CORRECT ORDER
					 *			
					for (String key : keyValues.keySet()) {

						String valueResult = keyValues.get(key);
						if (valueResult != null) {
							if(key.startsWith(Constants.GENO_FIELDS_PROCESS_END_TIME) || key.startsWith(Constants.GENO_FIELDS_PROCESS_START_TIME)){
								try {
									DateFormat dateFormat = new SimpleDateFormat(au.org.theark.core.Constants.DD_MM_YYYY);
									String[] dateFormats = { au.org.theark.core.Constants.DD_MM_YYYY, au.org.theark.core.Constants.yyyy_MM_dd_hh_mm_ss_S };
									Date date = DateUtils.parseDate(valueResult, dateFormats);
									csv.write(dateFormat.format(date));
								}
								catch (ParseException e) {
									csv.write(valueResult);
								}
							}
							else{
								csv.write(valueResult);
							}
						}
						else {
							csv.write("");
						}
					} */	
				}
				else{
					//not sure if we need this
//					// Write out a line with no values (no data existed for subject in question
//					for (CustomFieldDisplay cfd : cfds) {
//						csv.write("");
//					}
				}

				csv.endLine();
			}
			csv.close();
		}
		catch (FileNotFoundException e) {
			log.error(e.getMessage());
		}

		return file;
	}
	
	private String getPhenoCollectionName(String collectionID) {
		Criteria c = getSession().createCriteria(PhenoDataSetCollection.class, "p");
		c.add(Restrictions.eq("p.id", Long.parseLong(collectionID)));

		PhenoDataSetCollection pc = (PhenoDataSetCollection) c.uniqueResult();
		return pc.getQuestionnaire().getName();
	}
	
	public File createPhenotypicCSV(Search search, DataExtractionVO devo, List<PhenoDataSetFieldDisplay> cfds, FieldCategory fieldCategory) {
		final String tempDir = System.getProperty("java.io.tmpdir");
		String filename = "PHENOTYPIC.csv";
		final java.io.File file = new File(tempDir, filename);

		try {
			OutputStream outputStream = new FileOutputStream(file);
			createPhenoCSVOutputStream(devo, outputStream);
		} catch (FileNotFoundException e) {
			log.error("File not found", e);
		}
		return file;
	}

	void createPhenoCSVOutputStream(DataExtractionVO dataExtractionVO, OutputStream outputStream) {
		Map<String, List<String>> phenoCollectionMapping = new HashMap<>();
		Map<String, Integer> phenoCollectionTypeCount = new HashMap<>();
		for(Entry<String, ExtractionVO> entry : dataExtractionVO.getPhenoCustomData().entrySet()) {
			String subjectUID = entry.getValue().getSubjectUid();
			if(phenoCollectionMapping.containsKey(subjectUID)) {
				phenoCollectionMapping.get(subjectUID).add(entry.getKey());
			} else {
				List<String> phenoCollectionIDs = new ArrayList<>();
				phenoCollectionIDs.add(entry.getKey());
				phenoCollectionMapping.put(subjectUID, phenoCollectionIDs);
			}

			String collectionName = entry.getValue().getCollectionName();
			phenoCollectionTypeCount.put(collectionName, phenoCollectionTypeCount.getOrDefault(collectionName, 0) + 1);
		}

		Set<String> phenoCollectionHeadersSet = new HashSet<>();
		int maxPhenoCollections = 0;
		for(List<String> pc : phenoCollectionMapping.values()) {
			if(pc.size() > maxPhenoCollections) {
				maxPhenoCollections = pc.size();
			}
		}

		for (ExtractionVO evo : dataExtractionVO.getPhenoCustomData().values()) {
			phenoCollectionHeadersSet.addAll(evo.getKeyValues().keySet());
		}

		List<String> phenoCollectionHeaders = new ArrayList<>(phenoCollectionHeadersSet);
		Collections.sort(phenoCollectionHeaders);

		phenoCollectionHeaders.add(0, "Record Date");
		phenoCollectionHeaders.add(1, "Collection Name");

		CsvWriter csv = new CsvWriter(outputStream);
		csv.write("Subject UID");

		for(int i = 1; i <= maxPhenoCollections; i++) {
			for(String header : phenoCollectionHeaders) {
				csv.write("P" + i + "_" + header);
			}
		}

		csv.endLine();

		for(Entry<String, List<String>> entry : phenoCollectionMapping.entrySet()) {
			String subjectUID = entry.getKey();
			if (!phenoCollectionMapping.containsKey(subjectUID)) {
				continue;
			}

			csv.write(subjectUID);
			DateFormat df = new SimpleDateFormat("MM/dd/yyyy");
			for(String phenoCollectionID : phenoCollectionMapping.get(subjectUID)) {
				ExtractionVO phenoCollectionData = dataExtractionVO.getPhenoCustomData().get(phenoCollectionID);
				for(String header : phenoCollectionHeaders) {
					if(header.equals("Record Date")) {
						csv.write(df.format(phenoCollectionData.getRecordDate()));
					} else if(header.equals("Collection Name")) {
						csv.write(phenoCollectionData.getCollectionName());
					} else csv.write(phenoCollectionData.getKeyValues().getOrDefault(header, ""));
				}
			}
			if(phenoCollectionMapping.get(subjectUID).size() < maxPhenoCollections) {
				for(int i = 0; i < (maxPhenoCollections - phenoCollectionMapping.get(subjectUID).size()); i++) {
					for(int j = 0; j < phenoCollectionHeaders.size(); j++) {
						csv.write("");
					}
				}
			}
			csv.endLine();
		}
		csv.close();
	}
	
	public File createConsentStatusCSV(Search search, DataExtractionVO devo, List<ConsentStatusField> consentStatusFields, FieldCategory fieldCategory) {
		final String tempDir = System.getProperty("java.io.tmpdir");
		String filename = new String("STUDYCOMPONENTCONSENTSTATUS.csv");
		final java.io.File file = new File(tempDir, filename);
		if (filename == null || filename.isEmpty()) {
			filename = "exportcsv.csv";
		}
		log.info(consentStatusFields.toString());
		HashMap<String, ExtractionVO> hashOfConsentStatusData = devo.getConsentStatusData();

		OutputStream outputStream;
		try {
			outputStream = new FileOutputStream(file);
			CsvWriter csv = new CsvWriter(outputStream);
			log.info("hash: " + devo.getConsentStatusData());
			for(Entry<String, ExtractionVO> hash : devo.getConsentStatusData().entrySet()) {
				log.info("Entry: " + hash.getKey() + " " + hash.getValue().getSubjectUid() + " " + hash.getValue().getKeyValues());
			}
			
			// Header
			csv.write("SUBJECTUID");
			
			Set<String> fields = new HashSet<String>();
			for (String subjectUID : hashOfConsentStatusData.keySet()) {
				HashMap<String, String> keyValues = hashOfConsentStatusData.get(subjectUID).getKeyValues();
				fields.addAll((keyValues.keySet()));
			}
			for(String f : fields) {
				csv.write(f);
			}
			csv.endLine();
			log.info("keyset: " + hashOfConsentStatusData.keySet().toString());
			for (String subjectUID : hashOfConsentStatusData.keySet()) {
				csv.write(subjectUID);

				for (String f : fields) {
					HashMap<String, String> keyValues = hashOfConsentStatusData.get(subjectUID).getKeyValues();
					csv.write(keyValues.get(f));
					log.info(keyValues.get(f));
					
				}

				/**
				 * for (String subjectUID : hashOfSubjectsWithData.keySet()) { HashMap<String, String> keyValues =
				 * hashOfSubjectsWithData.get(subjectUID).getKeyValues(); log.info(subjectUID + " has " + keyValues.size() + "demo fields"); //
				 * remove(subjectUID).getKeyValues().size() + "demo fields"); for (String key : keyValues.keySet()) { log.info("     key=" + key +
				 * "\t   value=" + keyValues.get(key)); } }
				 */
				ExtractionVO evo = hashOfConsentStatusData.get(subjectUID);
				if (evo != null) {
					HashMap<String, String> keyValues = evo.getKeyValues();
					for (ConsentStatusField csf : consentStatusFields) {

						String valueResult = keyValues.get(csf.getPublicFieldName());
						if (valueResult != null) {
							csv.write(valueResult);
						}
						else {
							csv.write(valueResult);
						}
					}
				}
				else {
					// Write out a line with no values (no data existed for subject in question
					for (ConsentStatusField csf : consentStatusFields) {
						csv.write("");
					}
				}

				csv.endLine();
			}
			csv.close();
		}
		catch (FileNotFoundException e) {
			log.error(e.getMessage());
		}

		return file;
	}	

	public File createMegaCSV(Search search, DataExtractionVO allTheData, List<DemographicField> allSubjectFields, List<CustomFieldDisplay> biocollectionCustomFieldDisplay, List<CustomFieldDisplay> biospecimenCustomFieldDisplay, List<PhenoDataSetFieldDisplay> phenotypicCustomFieldDisplay, List<ConsentStatusField> consentStatusFields) {
		final String tempDir = System.getProperty("java.io.tmpdir");
		String filename = new String("COMBINED.csv");
		final java.io.File file = new File(tempDir, filename);
		
		long start = System.currentTimeMillis();
		
		//Add geno later

		
		
		
		Set<String> headers = new HashSet<String>();
		HashMap<String, List<String>> biospecimenMapping = new HashMap<String, List<String>>();
		HashMap<String, List<String>> biocollectionMapping = new HashMap<String, List<String>>();
		HashMap<String, List<String>> phenoCollectionMapping = new HashMap<String, List<String>>();
		
		for(Entry<String, ExtractionVO> entry : allTheData.getDemographicData().entrySet()) {
			headers.addAll(entry.getValue().getKeyValues().keySet());
		}
		
		for(Entry<String, ExtractionVO> entry : allTheData.getBiospecimenData().entrySet()) {
			String subjectUID = entry.getValue().getSubjectUid();
			if(biospecimenMapping.containsKey(subjectUID)) {
				biospecimenMapping.get(subjectUID).add(entry.getKey());
			} else {
				List<String> biospecimenUIDs = new ArrayList<String>();
				biospecimenUIDs.add(entry.getKey());
				biospecimenMapping.put(subjectUID, biospecimenUIDs);
			}
		}
		
		for(Entry<String, ExtractionVO> entry : allTheData.getBiocollectionData().entrySet()) {
			String subjectUID = entry.getValue().getSubjectUid();
			if(biocollectionMapping.containsKey(subjectUID)) {
				biocollectionMapping.get(subjectUID).add(entry.getKey());
			} else {
				List<String> biocollectionUIDs = new ArrayList<String>();
				biocollectionUIDs.add(entry.getKey());
				biocollectionMapping.put(subjectUID, biocollectionUIDs);
			}
		}
		
		for(Entry<String, ExtractionVO> entry : allTheData.getPhenoCustomData().entrySet()) {
			String subjectUID = entry.getValue().getSubjectUid();
			if(phenoCollectionMapping.containsKey(subjectUID)) {

				phenoCollectionMapping.get(subjectUID).add(entry.getKey());
			} else {
				List<String> phenoCollectionIDs = new ArrayList<String>();
				phenoCollectionIDs.add(entry.getKey());
				phenoCollectionMapping.put(subjectUID, phenoCollectionIDs);
			}
		}
		
		for(Entry<String, ExtractionVO> entry : allTheData.getSubjectCustomData().entrySet()) {
			headers.addAll(entry.getValue().getKeyValues().keySet());
		}
		
		//Biospecimens
		Set<String> biospecimenHeadersSet = new HashSet<String>();
		int maxBiospecimens = 0;
		for(List<String> bs : biospecimenMapping.values()) {
			if(bs.size() > maxBiospecimens) {
				maxBiospecimens = bs.size();
			}
		}
		
		Iterator<ExtractionVO> iter = allTheData.getBiospecimenData().values().iterator();
		while(iter.hasNext()) {
			ExtractionVO evo = iter.next();
			biospecimenHeadersSet.addAll(evo.getKeyValues().keySet());
		}
		
		iter = allTheData.getBiospecimenCustomData().values().iterator();
		while(iter.hasNext()) {
			ExtractionVO evo = iter.next();
			biospecimenHeadersSet.addAll(evo.getKeyValues().keySet());
		}
		
		//Biocollections
		Set<String> biocollectionHeadersSet = new HashSet<String>();
		int maxBiocollections = 0; 
		for(List<String> bc : biocollectionMapping.values()) { 
			if(bc.size() > maxBiocollections) { 
				maxBiocollections = bc.size(); 
			}
		}
		
		iter = allTheData.getBiocollectionData().values().iterator();
		while(iter.hasNext()) {
			ExtractionVO evo = iter.next();
			biocollectionHeadersSet.addAll(evo.getKeyValues().keySet());
		}
		
		iter = allTheData.getBiocollectionCustomData().values().iterator();
		while(iter.hasNext()) {
			ExtractionVO evo = iter.next();
			biocollectionHeadersSet.addAll(evo.getKeyValues().keySet());
		}
		
		//Phenocollections
		Set<String> phenoCollectionHeadersSet = new HashSet<String>();
		int maxPhenoCollections = 0; 
		for(List<String> pc : phenoCollectionMapping.values()) { 
			if(pc.size() > maxPhenoCollections) { 
				maxPhenoCollections = pc.size(); 
			}
		}
		
		iter = allTheData.getPhenoCustomData().values().iterator();
		while(iter.hasNext()) {
			ExtractionVO evo = iter.next();
			phenoCollectionHeadersSet.addAll(evo.getKeyValues().keySet());
		}
		
		List<String> biospecimenHeaders = new ArrayList<String>(biospecimenHeadersSet);
		List<String> biocollectionHeaders = new ArrayList<String>(biocollectionHeadersSet);
		List<String> phenoCollectionHeaders = new ArrayList<String>(phenoCollectionHeadersSet);
		
		List<String> headersList = new ArrayList<String>(headers);
		Collections.sort(headersList);
		Collections.sort(biocollectionHeaders);
		Collections.sort(biospecimenHeaders);
		Collections.sort(phenoCollectionHeaders);
		
		biospecimenHeaders.add(0, "Biospecimen UID");
		biocollectionHeaders.add(0, "Biocollection UID");
		phenoCollectionHeaders.add(0, "Record Date");
				
		OutputStream outputStream;
		try {
			outputStream = new FileOutputStream(file);
			CsvWriter csv = new CsvWriter(outputStream);
			
			csv.write("Subject UID");
			
			for(String header : headersList) {
				csv.write(header);
			}
			
			for(int i = 1; i <= maxBiospecimens; i++) {
				for(String header : biospecimenHeaders) {
					csv.write("BS" + i + "_" + header);
				}
			}
			
			for(int i = 1; i <= maxBiocollections; i++) {
				for(String header : biocollectionHeaders) {
					csv.write("BC" + i + "_" + header);
				}
			}
			
			for(int i = 1; i <= maxPhenoCollections; i++) {
				for(String header : phenoCollectionHeaders) {
					csv.write("P" + i + "_" + header);
				}
			}
			
			csv.endLine();
			
			for(String subjectUID : allTheData.getDemographicData().keySet()) {
				List<String> row = new ArrayList<String>();
				csv.write(subjectUID);
				
				ExtractionVO subjectData = allTheData.getDemographicData().get(subjectUID);
				ExtractionVO subjectCustomData = allTheData.getSubjectCustomData().get(subjectUID);
				for(String header : headersList) {
					if(subjectData.getKeyValues().containsKey(header)) {
						csv.write(subjectData.getKeyValues().get(header));
					} else if(subjectCustomData != null && subjectCustomData.getKeyValues().containsKey(header)) {
						csv.write(subjectCustomData.getKeyValues().get(header));
					} else {
						csv.write("");
					}
				}
				if(biospecimenMapping.containsKey(subjectUID)) {
					for(String biospecimenUID : biospecimenMapping.get(subjectUID)) {
						ExtractionVO biospecimenData = allTheData.getBiospecimenData().get(biospecimenUID);
						ExtractionVO biospecimenCustomData = allTheData.getBiospecimenCustomData().get(biospecimenUID);
						for(String header : biospecimenHeaders) {
							if(header.equals("Biospecimen UID")) {
								csv.write(biospecimenUID);
							} else if(biospecimenData.getKeyValues().containsKey(header)) {
								csv.write(biospecimenData.getKeyValues().get(header));
							} else if(biospecimenCustomData != null && biospecimenCustomData.getKeyValues().containsKey(header)){
								csv.write(biospecimenCustomData.getKeyValues().get(header));
							} else {
								csv.write("");
							}
						}
					}
					//Inserting empty cells where subject has fewer biospecimens than the max
					if(biospecimenMapping.get(subjectUID).size() < maxBiospecimens) {
						for(int i = 0; i < (maxBiospecimens - biospecimenMapping.get(subjectUID).size()); i++) {
							for(String header : biospecimenHeaders) {
								csv.write("");
							}
						}
					}
				} else {
					for(int i = 0; i < maxBiospecimens; i++) {
						for(String header : biospecimenHeaders) {
							csv.write("");
						}
					}
				}
				
				if(biocollectionMapping.containsKey(subjectUID)) {
					for(String biocollectionUID : biocollectionMapping.get(subjectUID)) {
						ExtractionVO biocollectionData = allTheData.getBiocollectionData().get(biocollectionUID);
						ExtractionVO biocollectionCustomData = allTheData.getBiocollectionCustomData().get(biocollectionUID);
						for(String header : biocollectionHeaders) {
							if(header.equals("Biocollection UID")) {
								csv.write(biocollectionUID);
							} else if(biocollectionData.getKeyValues().containsKey(header)) {
								csv.write(biocollectionData.getKeyValues().get(header));
							} else if(biocollectionCustomData != null && biocollectionCustomData.getKeyValues().containsKey(header)){
								csv.write(biocollectionCustomData.getKeyValues().get(header));
							} else {
								csv.write("");
							}
						}
					}
				
					//Inserting empty cells where subject has fewer biocollections than the max
					if(biocollectionMapping.get(subjectUID).size() < maxBiocollections) {
						for(int i = 0; i < (maxBiocollections - biocollectionMapping.get(subjectUID).size()); i++) {
							for(String header : biocollectionHeaders) {
								csv.write("");
							}
						}
					}
				} else {
					for(int i = 0; i < maxBiocollections; i++) {
						for(String header : biocollectionHeaders) {
							csv.write("");
						}
					}
				}
				
				if(phenoCollectionMapping.containsKey(subjectUID)) {
					DateFormat df = new SimpleDateFormat("MM/dd/yyyy");
					for(String phenoCollectionID : phenoCollectionMapping.get(subjectUID)) {
						ExtractionVO phenoCollectionData = allTheData.getPhenoCustomData().get(phenoCollectionID);
						for(String header : phenoCollectionHeaders) {
							if(header.equals("Record Date")) {
								csv.write(df.format(phenoCollectionData.getRecordDate()));
							} else if(phenoCollectionData.getKeyValues().containsKey(header)) {
								csv.write(phenoCollectionData.getKeyValues().get(header));
							} else {
								csv.write("");
							}
						}
					}
					if(phenoCollectionMapping.get(subjectUID).size() < maxPhenoCollections) {
						for(int i = 0; i < (maxPhenoCollections - phenoCollectionMapping.get(subjectUID).size()); i++) {
							for(String header : phenoCollectionHeaders) {
								csv.write("");
							}
						}
					}
				} else {
					for(int i = 0; i < maxPhenoCollections; i++) {
						for(String header : phenoCollectionHeaders) {
							csv.write("");
						}
					}
				}
				csv.endLine();
			}
			
			csv.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		log.info("taken " + (System.currentTimeMillis() - start));
		return file;
	}
	
}
