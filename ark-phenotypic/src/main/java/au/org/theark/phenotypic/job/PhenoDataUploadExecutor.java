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
package au.org.theark.phenotypic.job;

import static org.quartz.DateBuilder.nextGivenSecondDate;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

import java.io.InputStream;
import java.util.Date;
import java.util.List;

import org.apache.shiro.SecurityUtils;
import org.jfree.util.Log;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerFactory;
import org.quartz.SimpleTrigger;
import org.quartz.impl.StdSchedulerFactory;

import au.org.theark.core.Constants;
import au.org.theark.core.model.pheno.entity.PhenoDataSetCollection;
import au.org.theark.core.model.pheno.entity.PhenoDataSetGroup;
import au.org.theark.core.service.IArkCommonService;
import au.org.theark.phenotypic.service.IPhenotypicService;

public class PhenoDataUploadExecutor {
	//private static final Logger	log	= LoggerFactory.getLogger(CustomDataUploadExecutor.class);
	
	private IArkCommonService<Void>		iArkCommonService			= null;
	private IPhenotypicService			iPhenoService				= null;
	private Long						uploadId;
	private Long						studyId;
	private String						fileFormat;
	private InputStream					inputStream;
	private char						delimiter		= Constants.IMPORT_DELIM_CHAR_COMMA;
	private long						size;
	private String						report;
	private List<String>				uidsToUpload;
	private PhenoDataSetCollection		phenoCollection;
	private PhenoDataSetGroup			phenoDataSetGroup;
	private boolean						overwriteExisting;
	
	/**
	 * StudyDataUploadExecutor constructor
	 * @param iArkCommonService
	 * @param iPhenoService
	 * @param uploadId
	 * @param currentUser
	 * @param study
	 * @param phenoCollection
	 * @param file
	 * @param delimiter
	 * @param report 
	 * @param uidsToUpload 
	 */
	public PhenoDataUploadExecutor(IArkCommonService iArkCommonService,
											IPhenotypicService iPhenoService,
											InputStream inputStream,
											Long uploadId,
											Long studyId,
											String fileFormat,
											char delimiter,
											long size, String report, List<String> uidsToUpload,
											PhenoDataSetCollection phenoCollection,
											PhenoDataSetGroup phenoDataSetGroup,
											boolean overwriteExisting) {
		this.iArkCommonService = iArkCommonService;
		this.iPhenoService = iPhenoService;
		this.inputStream = inputStream;
		this.uploadId = uploadId;
		this.studyId = studyId;
		this.fileFormat = fileFormat;
		this.delimiter = delimiter;
		this.size = size;
		this.report = report;
		this.uidsToUpload = uidsToUpload;
		this.phenoCollection = phenoCollection;
		this.phenoDataSetGroup = phenoDataSetGroup;
		this.overwriteExisting = overwriteExisting;
	}

	public void run() throws Exception {

		SchedulerFactory sf = new StdSchedulerFactory();
		Scheduler sched = sf.getScheduler();
		Log.warn("executor " + uidsToUpload.size());
		JobDetail phenoDataUploadJob = newJob(PhenoDataUploadJob.class).withIdentity("PhenoDataUploadJob", "group2").build();
		// pass initialization parameters into the job
		phenoDataUploadJob.getJobDataMap().put(PhenoDataUploadJob.IARKCOMMONSERVICE, iArkCommonService);
		phenoDataUploadJob.getJobDataMap().put(PhenoDataUploadJob.IPHENOSERVICE, iPhenoService);
		phenoDataUploadJob.getJobDataMap().put(PhenoDataUploadJob.UPLOADID, uploadId);
		phenoDataUploadJob.getJobDataMap().put(PhenoDataUploadJob.STUDY_ID, studyId);
		phenoDataUploadJob.getJobDataMap().put(PhenoDataUploadJob.REPORT, report);
		phenoDataUploadJob.getJobDataMap().put(PhenoDataUploadJob.FILE_FORMAT, fileFormat);
		phenoDataUploadJob.getJobDataMap().put(PhenoDataUploadJob.INPUT_STREAM, inputStream);
		phenoDataUploadJob.getJobDataMap().put(PhenoDataUploadJob.DELIMITER, delimiter);
		phenoDataUploadJob.getJobDataMap().put(PhenoDataUploadJob.SIZE, size);
		phenoDataUploadJob.getJobDataMap().put(PhenoDataUploadJob.LIST_OF_UIDS_TO_UPDATE, uidsToUpload);
		phenoDataUploadJob.getJobDataMap().put(PhenoDataUploadJob.PHENO_COLLECTION, phenoCollection);
		phenoDataUploadJob.getJobDataMap().put(PhenoDataUploadJob.PHENO_FIELD_GROUP, phenoDataSetGroup);
		phenoDataUploadJob.getJobDataMap().put(PhenoDataUploadJob.OVERWRITE_EXISTING, overwriteExisting);
		phenoDataUploadJob.getJobDataMap().put(PhenoDataUploadJob.USERNAME, SecurityUtils.getSubject().getPrincipal().toString());
		Date startTime = nextGivenSecondDate(null, 1);
		SimpleTrigger trigger1 = newTrigger().withIdentity("PhenoDataUploadJobTrigger", "group1").startAt(startTime).withSchedule(simpleSchedule()).build();
		sched.scheduleJob(phenoDataUploadJob, trigger1);
		//		log.warn(studyUploadJob.getKey() + " will run at: " + scheduleTime1 + " and repeat: " + trigger1.getRepeatCount() + " times, every " + trigger1.getRepeatInterval() / 1000 + " seconds");
		// All of the jobs have been added to the scheduler, but none of the jobs will run until the scheduler has been started
		sched.start();
		//sched.triggerJob(phenoDataUploadJob.getKey());
	}
	
}
