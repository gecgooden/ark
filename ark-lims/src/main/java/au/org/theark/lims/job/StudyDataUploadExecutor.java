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
package au.org.theark.lims.job;

import static org.quartz.DateBuilder.nextGivenSecondDate;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

import java.io.InputStream;
import java.util.Date;
import java.util.List;

import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerFactory;
import org.quartz.SimpleTrigger;
import org.quartz.impl.StdSchedulerFactory;

import au.org.theark.core.Constants;
import au.org.theark.core.service.IArkCommonService;
import au.org.theark.lims.service.ILimsService;

public class StudyDataUploadExecutor {
	
	private IArkCommonService<Void>	iArkCommonService			= null;
	private ILimsService				iLimsService				= null;
	private Long							uploadId;
	private Long							studyId;
	private String							fileFormat;
	private InputStream					inputStream;
	private char							delimiter		= Constants.IMPORT_DELIM_CHAR_COMMA;
	private long							size;

	private String							report;
	private List<String>					uidsToUpload;
	
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
	public StudyDataUploadExecutor(IArkCommonService iArkCommonService,
											ILimsService iLimsService,
											InputStream inputStream,
											Long uploadId,
											Long studyId,
											String fileFormat,
											char delimiter,
											long size, String report, List<String> uidsToUpload) {
		this.iArkCommonService = iArkCommonService;
		this.iLimsService = iLimsService;
		this.inputStream = inputStream;
		this.uploadId = uploadId;
		this.studyId = studyId;
		this.fileFormat = fileFormat;
		this.delimiter = delimiter;
		this.size = size;
		this.report = report;
		this.uidsToUpload = uidsToUpload;
	}

	public void run() throws Exception {
		SchedulerFactory sf = new StdSchedulerFactory();
		Scheduler sched = sf.getScheduler();
		JobDetail studyUploadJob = newJob(StudyDataUploadJob.class).withIdentity("StudyDataUploadJob", "group1").build();
		// pass initialization parameters into the job
		studyUploadJob.getJobDataMap().put(StudyDataUploadJob.IARKCOMMONSERVICE, iArkCommonService);
		studyUploadJob.getJobDataMap().put(StudyDataUploadJob.ILimsService, iLimsService);
		studyUploadJob.getJobDataMap().put(StudyDataUploadJob.UPLOADID, uploadId);
		studyUploadJob.getJobDataMap().put(StudyDataUploadJob.STUDY_ID, studyId);
		studyUploadJob.getJobDataMap().put(StudyDataUploadJob.REPORT, report);
		studyUploadJob.getJobDataMap().put(StudyDataUploadJob.FILE_FORMAT, fileFormat);
		studyUploadJob.getJobDataMap().put(StudyDataUploadJob.INPUT_STREAM, inputStream);
		studyUploadJob.getJobDataMap().put(StudyDataUploadJob.DELIMITER, delimiter);
		studyUploadJob.getJobDataMap().put(StudyDataUploadJob.SIZE, size);
		studyUploadJob.getJobDataMap().put(StudyDataUploadJob.LIST_OF_UIDS_TO_UPDATE, uidsToUpload);
		Date startTime = nextGivenSecondDate(null, 1);
		SimpleTrigger trigger1 = newTrigger().withIdentity("StudyDataUploadJobTrigger", "group1").startAt(startTime).withSchedule(simpleSchedule()).build();
		sched.scheduleJob(studyUploadJob, trigger1);
		//		log.warn(studyUploadJob.getKey() + " will run at: " + scheduleTime1 + " and repeat: " + trigger1.getRepeatCount() + " times, every " + trigger1.getRepeatInterval() / 1000 + " seconds");
		// All of the jobs have been added to the scheduler, but none of the jobs will run until the scheduler has been started
		sched.start();
	}
	
}
