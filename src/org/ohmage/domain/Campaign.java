package org.ohmage.domain;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ohmage.util.StringUtils;

/**
 * Storage of everything defining a campaign.
 * 
 * @author Joshua Selsky
 */
public class Campaign {
	private String urn;
	private String name;
	private String description;
	private String runningState;
	private String privacyState;
	/**
	 * TODO: Make this a Timestamp or Calendar?
	 */
	private String campaignCreationTimestamp;
	
	// This should not be included in a constructor and should only be set when
	// needed as it may be a large element.
	private String xml;
	
	private Set<String> classes;
	
	private Set<String> supervisors;
	private Set<String> authors;
	private Set<String> analysts;
	private Set<String> participants;

	public Campaign() {
		classes = new HashSet<String>();
		
		supervisors = new HashSet<String>();
		authors = new HashSet<String>();
		analysts = new HashSet<String>();
		participants = new HashSet<String>();
	}
	
	public Campaign(String urn) {
		this.urn = urn;
		
		classes = new HashSet<String>();
		
		supervisors = new HashSet<String>();
		authors = new HashSet<String>();
		analysts = new HashSet<String>();
		participants = new HashSet<String>();
	}
	
	public Campaign(String urn, String name, String description, String runningState, String privacyState, String campaignCreationTimestamp) {
		this.urn = urn;
		this.name = name;
		this.description = description;
		this.runningState = runningState;
		this.privacyState = privacyState;
		this.campaignCreationTimestamp = campaignCreationTimestamp;

		classes = new HashSet<String>();
		
		supervisors = new HashSet<String>();
		authors = new HashSet<String>();
		analysts = new HashSet<String>();
		participants = new HashSet<String>();
	}

	public Campaign(Campaign campaign) {
		this.urn = campaign.urn;
		this.name = campaign.name;
		this.description = campaign.description;
		this.runningState = campaign.runningState;
		this.privacyState = campaign.privacyState;
		this.campaignCreationTimestamp = campaign.campaignCreationTimestamp;
		
		classes = new HashSet<String>(campaign.classes);
		
		supervisors = new HashSet<String>(campaign.participants);
		authors = new HashSet<String>(campaign.participants);
		analysts = new HashSet<String>(campaign.participants);
		participants = new HashSet<String>(campaign.participants);
	}

	public String getUrn() {
		return this.urn;
	}

	public void setUrn(String urn) {
		this.urn = urn;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getRunningState() {
		return this.runningState;
	}

	public void setRunningState(String runningState) {
		this.runningState = runningState;
	}

	public String getPrivacyState() {
		return this.privacyState;
	}

	public void setPrivacyState(String privacyState) {
		this.privacyState = privacyState;
	}

	public String getCampaignCreationTimestamp() {
		return this.campaignCreationTimestamp;
	}
	
	public Calendar getCampaignCreationTimestampAsCalendar() {
		if(campaignCreationTimestamp == null) {
			return null;
		}
		
		Calendar result = Calendar.getInstance();
		result.setTime(StringUtils.decodeDate(campaignCreationTimestamp));
		return result;
	}

	public void setCampaignCreationTimestamp(String campaignCreationTimestamp) {
		this.campaignCreationTimestamp = campaignCreationTimestamp;
	}
	
	public void setXml(String xml) {
		this.xml = xml;
	}
	
	public String getXml() {
		return xml;
	}
	
	public void addClass(String classId) {
		if(classId != null) {
			classes.add(classId);
		}
	}
	
	public void addClasses(Collection<String> classIds) {
		if(classIds != null) {
			classes.addAll(classIds);
		}
	}
	
	public List<String> getClasses() {
		return new ArrayList<String>(classes);
	}
	
	public void addSupervisor(String username) {
		if(username != null) {
			supervisors.add(username);
		}
	}
	
	public void addSupervisors(List<String> usernames) {
		if(usernames != null) {
			supervisors.addAll(usernames);
		}
	}
	
	public List<String> getSupervisors() {
		return new ArrayList<String>(supervisors);
	}
	
	public void addAuthor(String username) {
		if(username != null) {
			authors.add(username);
		}
	}
	
	public void addAuthors(List<String> usernames) {
		if(usernames != null) {
			authors.addAll(usernames);
		}
	}
	
	public List<String> getAuthors() {
		return new ArrayList<String>(authors);
	}
	
	public void addAnalyst(String username) {
		if(username != null) {
			analysts.add(username);
		}
	}
	
	public void addAnalysts(List<String> usernames) {
		if(usernames != null) {
			analysts.addAll(usernames);
		}
	}
	
	public List<String> getAnalysts() {
		return new ArrayList<String>(analysts);
	}
	
	public void addParticipant(String username) {
		if(username != null) {
			participants.add(username);
		}
	}
	
	public void addParticiapnts(List<String> usernames) {
		if(usernames != null) {
			participants.addAll(usernames);
		}
	}
	
	public List<String> getParticipants() {
		return new ArrayList<String>(participants);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((campaignCreationTimestamp == null) ? 0
						: campaignCreationTimestamp.hashCode());
		result = prime * result
				+ ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result
				+ ((privacyState == null) ? 0 : privacyState.hashCode());
		result = prime * result
				+ ((runningState == null) ? 0 : runningState.hashCode());
		result = prime * result + ((urn == null) ? 0 : urn.hashCode());
		result = prime * result + ((xml == null) ? 0 : xml.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Campaign other = (Campaign) obj;
		if (campaignCreationTimestamp == null) {
			if (other.campaignCreationTimestamp != null)
				return false;
		} else if (!campaignCreationTimestamp
				.equals(other.campaignCreationTimestamp))
			return false;
		if (description == null) {
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (privacyState == null) {
			if (other.privacyState != null)
				return false;
		} else if (!privacyState.equals(other.privacyState))
			return false;
		if (runningState == null) {
			if (other.runningState != null)
				return false;
		} else if (!runningState.equals(other.runningState))
			return false;
		if (urn == null) {
			if (other.urn != null)
				return false;
		} else if (!urn.equals(other.urn))
			return false;
		if (xml == null) {
			if (other.xml != null)
				return false;
		} else if (!xml.equals(other.xml))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Campaign [urn=" + urn + ", name=" + name + ", description="
				+ description + ", runningState=" + runningState
				+ ", privacyState=" + privacyState
				+ ", campaignCreationTimestamp=" + campaignCreationTimestamp
				+ "]";
	}
}
