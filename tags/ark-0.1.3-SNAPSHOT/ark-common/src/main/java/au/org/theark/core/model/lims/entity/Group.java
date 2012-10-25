package au.org.theark.core.model.lims.entity;

// Generated 15/06/2011 1:22:58 PM by Hibernate Tools 3.3.0.GA

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import au.org.theark.core.model.Constants;

/**
 * Group generated by hbm2java
 */
@Entity(name = "au.org.theark.lims.model.entity.Group")
@Table(name = "group", schema = Constants.LIMS_TABLE_SCHEMA)
public class Group implements java.io.Serializable
{

	private Long		id;
	private String		timestamp;
	private Integer	deleted;
	private int			groupId;
	private String		name;
	private String		description;
	private int			activityId;

	public Group()
	{
	}

	public Group(Long id, int groupId, String name, int activityId)
	{
		this.id = id;
		this.groupId = groupId;
		this.name = name;
		this.activityId = activityId;
	}

	public Group(Long id, Integer deleted, int groupId, String name, String description, int activityId)
	{
		this.id = id;
		this.deleted = deleted;
		this.groupId = groupId;
		this.name = name;
		this.description = description;
		this.activityId = activityId;
	}

	@Id
	@SequenceGenerator(name = "group_generator", sequenceName = "GROUP_SEQUENCE")
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "group_generator")
	public Long getId()
	{
		return this.id;
	}

	public void setId(Long id)
	{
		this.id = id;
	}

	
	@Column(name = "TIMESTAMP", length = 55)
	public String getTimestamp()
	{
		return this.timestamp;
	}

	public void setTimestamp(String timestamp)
	{
		this.timestamp = timestamp;
	}

	@Column(name = "DELETED")
	public Integer getDeleted()
	{
		return this.deleted;
	}

	public void setDeleted(Integer deleted)
	{
		this.deleted = deleted;
	}

	@Column(name = "GROUP_ID", nullable = false)
	public int getGroupId()
	{
		return this.groupId;
	}

	public void setGroupId(int groupId)
	{
		this.groupId = groupId;
	}

	@Column(name = "NAME", nullable = false, length = 100)
	public String getName()
	{
		return this.name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	@Column(name = "DESCRIPTION", length = 65535)
	public String getDescription()
	{
		return this.description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	@Column(name = "ACTIVITY_ID", nullable = false)
	public int getActivityId()
	{
		return this.activityId;
	}

	public void setActivityId(int activityId)
	{
		this.activityId = activityId;
	}

}