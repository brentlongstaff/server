/*******************************************************************************
 * Copyright 2011 The Regents of the University of California
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.ohmage.service;

import java.util.List;

import org.apache.log4j.Logger;
import org.ohmage.request.AwRequest;
import org.ohmage.request.InputKeys;
import org.ohmage.validator.AwRequestAnnotator;


/**
 * Checks that the user has all of a set of roles in each campaign in a 
 * campaign ID list.
 * 
 * @author John Jenkins
 */
public class UserHasRolesInAllCampaignsInListService extends AbstractAnnotatingService {
	private static Logger _logger = Logger.getLogger(UserHasRolesInAllCampaignsInListService.class);
	
	private List<String> _roles;
	private boolean _required;
	
	/**
	 * Builds this service.
	 * 
	 * @param annotator The annotator to use if the user doesn't have one of
	 * 					the roles in each of the campaigns in the list.
	 * 
	 * @param roles The campaign roles to check that the user has for each of 	
	 * 				the campaigns in the list.
	 * 
	 * @param required Whether or not this check is required.
	 */
	public UserHasRolesInAllCampaignsInListService(AwRequestAnnotator annotator, List<String> roles, boolean required) {
		super(annotator);
		
		if((roles == null) || (roles.size() == 0)) {
			throw new IllegalArgumentException("The list of roles cannot be null or empty.");
		}
		
		_roles = roles;
		_required = required;
	}
	
	/**
	 * Checks that the requesting user has all the roles in the list of roles
	 * for each campaign in the list of campaigns.
	 */
	@Override
	public void execute(AwRequest awRequest) {
		String campaignIdList;
		try {
			campaignIdList = (String) awRequest.getToProcessValue(InputKeys.CAMPAIGN_URN_LIST);
		}
		catch(IllegalArgumentException e) {
			if(_required) {
				throw new IllegalArgumentException("The required key '" + InputKeys.CAMPAIGN_URN_LIST + "' is missing.");
			}
			else {
				return;
			}
		}
		
		if("".equals(campaignIdList.trim())) {
			return;
		}
		
		_logger.info("Validating the that user has the appropriate roles for each campaign in the campaign list.");
		
		String[] campaignIdArray = campaignIdList.split(InputKeys.LIST_ITEM_SEPARATOR);
		for(int i = 0; i < campaignIdArray.length; i++) {
			for(String role : _roles) {
				if(! awRequest.getUser().hasRoleInCampaign(campaignIdArray[i], role)) {
					getAnnotator().annotate(awRequest, "The user doesn't have the permission '" + role + "' in campaign: " + campaignIdArray[i]);
					awRequest.setFailedRequest(true);
					return;
				}
			}
		}
	}
}
