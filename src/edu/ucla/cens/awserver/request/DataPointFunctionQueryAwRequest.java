package edu.ucla.cens.awserver.request;

import edu.ucla.cens.awserver.domain.DataPointFunctionQueryMetadata;

/**
 * State for data point function query API.
 * 
 * @author selsky
 */
public class DataPointFunctionQueryAwRequest extends ResultListAwRequest {
	private String _startDate;
	private String _endDate;
	private String _userNameRequestParam;
	private String _client;
	private String _campaignName;
	private String _campaignVersion;
	private String _functionName;
	private DataPointFunctionQueryMetadata _metadata;
	
	public String getStartDate() {
		return _startDate;
	}
	
	public void setStartDate(String startDate) {
		_startDate = startDate;
	}
	
	public String getEndDate() {
		return _endDate;
	}
	
	public void setEndDate(String endDate) {
		_endDate = endDate;
	}
	
	public String getUserNameRequestParam() {
		return _userNameRequestParam;
	}

	public void setUserNameRequestParam(String userNameRequestParam) {
		_userNameRequestParam = userNameRequestParam;
	}

	public String getClient() {
		return _client;
	}

	public void setClient(String client) {
		_client = client;
	}

	public String getCampaignName() {
		return _campaignName;
	}

	public void setCampaignName(String campaignName) {
		_campaignName = campaignName;
	}

	public String getCampaignVersion() {
		return _campaignVersion;
	}

	public void setCampaignVersion(String campaignVersion) {
		_campaignVersion = campaignVersion;
	}
	
	public String getFunctionName() {
		return _functionName;
	}

	public void setFunctionName(String functionName) {
		_functionName = functionName;
	}
	
	public DataPointFunctionQueryMetadata getMetadata() {
		return _metadata;
	}
	
	public void setMetadata(DataPointFunctionQueryMetadata metadata) {
		_metadata = metadata;
	}
}
