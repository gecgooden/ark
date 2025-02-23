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
package au.org.theark.core.model.study.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import au.org.theark.core.Constants;

@Entity
@Table(name = "correspondence_mode_direction_outcome", schema = Constants.STUDY_SCHEMA)
public class CorrespondenceModeDirectionOutcome implements Serializable {

	private static final long serialVersionUID = 1L;
	private Long id;
	private CorrespondenceModeType correspondenceModeType;
	private CorrespondenceDirectionType correspondenceDirectionType;
	private CorrespondenceOutcomeType correspondenceOutcomeType; 

	@Id
	@SequenceGenerator(name = "correspondence_mode_direction_outcome_generator", sequenceName = "CORRESPONDENCE_MODE_DIRECTION_OUTCOME_SEQUENCE")
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "correspondence_mode_direction_outcome_generator")
	@Column(name = "ID", unique = true, nullable = false, precision = 22, scale = 0)
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MODE_TYPE_ID")
	public CorrespondenceModeType getCorrespondenceModeType() {
		return correspondenceModeType;
	}
	public void setCorrespondenceModeType(
			CorrespondenceModeType correspondenceModeType) {
		this.correspondenceModeType = correspondenceModeType;
	}
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "DIRECTION_TYPE_ID")
	public CorrespondenceDirectionType getCorrespondenceDirectionType() {
		return correspondenceDirectionType;
	}
	public void setCorrespondenceDirectionType(
			CorrespondenceDirectionType correspondenceDirectionType) {
		this.correspondenceDirectionType = correspondenceDirectionType;
	}
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "OUTCOME_TYPE_ID")
	public CorrespondenceOutcomeType getCorrespondenceOutcomeType() {
		return correspondenceOutcomeType;
	}
	public void setCorrespondenceOutcomeType(
			CorrespondenceOutcomeType correspondenceOutcomeType) {
		this.correspondenceOutcomeType = correspondenceOutcomeType;
	}
	

	
}
