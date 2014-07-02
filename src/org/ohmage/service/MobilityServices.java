/*******************************************************************************
 * Copyright 2012 The Regents of the University of California
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.joda.time.DateTime;
import org.ohmage.annotator.Annotator.ErrorCode;
import org.ohmage.domain.MobilityAggregatePoint;
import org.ohmage.domain.MobilityPoint;
import org.ohmage.domain.MobilityPoint.LocationStatus;
import org.ohmage.domain.MobilityPoint.Mode;
import org.ohmage.domain.MobilityPoint.SensorData;
import org.ohmage.exception.DataAccessException;
import org.ohmage.exception.DomainException;
import org.ohmage.exception.ServiceException;
import org.ohmage.query.IUserMobilityQueries;
import org.ohmage.query.IUserQueries;

import edu.ucla.cens.mobilityclassifier.Classification;
import edu.ucla.cens.mobilityclassifier.Location;
import edu.ucla.cens.mobilityclassifier.MobilityClassifier;
import edu.ucla.cens.mobilityclassifier.Sample;
import edu.ucla.cens.mobilityclassifier.WifiScan;

/**
 * This class is responsible for all services pertaining to Mobility points.
 * 
 * @author John Jenkins
 */
public final class MobilityServices {
	/**
	 * This is the maximum number of milliseconds before a Mobility point that
	 * we need to get the WiFi data for the classifier.
	 */
	private static final long MAX_MILLIS_OF_PREVIOUS_WIFI_DATA = 
			1000 * 60 * 10;
	
	private static MobilityServices instance;
	private IUserQueries userQueries;
	private IUserMobilityQueries userMobilityQueries;
	
	/**
	 * Default constructor. Privately instantiated via dependency injection
	 * (reflection).
	 * 
	 * @throws IllegalStateException if an instance of this class already
	 * exists
	 * 
	 * @throws IllegalArgumentException if iUserMobilityQueries is null
	 */
	private MobilityServices(
			final IUserQueries iUserQueries,
			final IUserMobilityQueries iUserMobilityQueries) {
		
		if(instance != null) {
			throw new IllegalStateException("An instance of this class already exists.");
		}
		
		if(iUserQueries == null) {
			throw new IllegalArgumentException("An instance of IUserQueries is required.");
		}
		
		if(iUserMobilityQueries == null) {
			throw new IllegalArgumentException("An instance of IUserMobilityQueries is required.");
		}
		
		userQueries = iUserQueries;
		userMobilityQueries = iUserMobilityQueries;
		instance = this;
	}
	
	/**
	 * @return  Returns the singleton instance of this class.
	 */
	public static MobilityServices instance() {
		return instance;
	}
	
	/**
	 * Adds the Mobility point to the database.
	 * 
	 * @param mobilityPoints A list of Mobility points to be added to the 
	 * 						 database.
	 * 
	 * @throws ServiceException Thrown if there is an error.
	 */
	public void createMobilityPoint(final String username, 
			final String client, final List<MobilityPoint> mobilityPoints) 
			throws ServiceException {
		
		if(username == null) {
			throw new ServiceException("The username cannot be null.");
		}
		else if(client == null) {
			throw new ServiceException("The client cannot be null.");
		}
		
		try {
			for(MobilityPoint mobilityPoint : mobilityPoints) {
				userMobilityQueries.createMobilityPoint(username, client, mobilityPoint);
			}
		}
		catch(DataAccessException e) {
			throw new ServiceException(e);
		}
	}
	
	/**
	 * Runs the classifier against all of the Mobility points in the list.
	 * 
	 * @param mobilityPoints The Mobility points that are to be classified by
	 * 						 the server.
	 * 
	 * @throws ServiceException Thrown if there is an error with the 
	 * 							classification service.
	 */
	public void classifyData(
			final String uploadersUsername,
			final List<MobilityPoint> mobilityPoints) 
			throws ServiceException {
		
		// If the list is empty, just exit.
		if(mobilityPoints == null) {
			return;
		}
		
		// Create a new classifier.
		MobilityClassifier classifier = new MobilityClassifier();
				
		// Create place holders for the previous data.
		Classification previousClassification = null;
		List<WifiScan> previousWifiScans = new LinkedList<WifiScan>();
		ArrayList<Location> previousLocations = new ArrayList<Location>();
		// This is a bit more involved now that we are doing everything through
		// the observers.
		/*
		if(mobilityPoints.size() > 0) {
			try {
				MobilityPoint firstPoint = mobilityPoints.get(0);
				DateTime startDate = new DateTime();

				startDate = 
						new DateTime(
							firstPoint.getTime() -
							MAX_MILLIS_OF_PREVIOUS_WIFI_DATA);

				List<MobilityPoint> previousPoints = 
						userMobilityQueries.getMobilityInformation(
							uploadersUsername, 
							startDate, 
							new DateTime(firstPoint.getTime()), 
							null, 
							null, 
							null);

				for(MobilityPoint previousPoint : previousPoints) {
					try {
						previousWifiScans.add(previousPoint.getWifiScan());
					}
					catch(DomainException e) {
						// This point does not have a WifiScan, so we will skip
						// it.
					}
				}
			}
			catch(DataAccessException e) {
				throw new ServiceException(e);
			}
		}
		*/

		// For each of the Mobility points,
		for(MobilityPoint mobilityPoint : mobilityPoints) {
			// If the data point is of type error, don't attempt to classify 
			// it.
			if(mobilityPoint.getMode().equals(Mode.ERROR)) {
				continue;
			}
			
			// If the SubType is sensor data,
			if(MobilityPoint.SubType.SENSOR_DATA.equals(mobilityPoint.getSubType())) {
				SensorData currSensorData = mobilityPoint.getSensorData();
				
				// Get the Samples from this new point.
				List<Sample> samples;
				try {
					samples = mobilityPoint.getSamples();
				}
				catch(DomainException e) {
					throw new ServiceException(
							"There was a problem retrieving the samples.",
							e);
				}
				
				// Get the new WifiScan from this new point.
				WifiScan wifiScan;
				if(mobilityPoint.getSensorData().getWifiData() == null) {
					wifiScan = null;
				}
				else {
					try {
						wifiScan = mobilityPoint.getWifiScan();
					} 
					catch(DomainException e) {
						throw new ServiceException(
								"The Mobility point does not contain WiFi data.",
								e);
					}
				}
				
				Location currLoc;
				if(mobilityPoint.getLocation() == null) {
					currLoc = null;
				}
				else {
					try {
						currLoc = new Location(mobilityPoint.getLocation().getLatitude(), mobilityPoint.getLocation().getLatitude(),
								mobilityPoint.getTime());
					} 
					catch(Exception e) {
						throw new ServiceException(
								"The Mobility point does not contain Location data.",
								e);
					}
				}
				
				// Prune out the old WifiScans that are more than 10 minutes 
				// old.
				long minPreviousTime = 
						mobilityPoint.getTime() - 
							MAX_MILLIS_OF_PREVIOUS_WIFI_DATA;
				Iterator<WifiScan> previousWifiScansIter = 
						previousWifiScans.iterator();
				while(previousWifiScansIter.hasNext()) {
					if(previousWifiScansIter.next().getTime() < minPreviousTime) {
						previousWifiScansIter.remove();
					}
					else {
						// Given the fact that the list is ordered, we can now
						// be assured that all of the remaining WiFi scans are
						// invalid.
						break;
					}
				}
				
				Iterator<Location> previousLocationsIter = 
						previousLocations.iterator();
				while(previousLocationsIter.hasNext()) {
					if(previousLocationsIter.next().getTime() < minPreviousTime) {
						previousLocationsIter.remove();
					}
					else {
						// Given the fact that the list is ordered, we can now
						// be assured that all of the remaining WiFi scans are
						// invalid.
						break;
					}
				}

				// Classify the data.
				Classification classification =
						classifier.classify(
								samples,
								currSensorData.getSpeed(),
								wifiScan, 
								previousWifiScans, currLoc, previousLocations,
								previousClassification);
				// new things: Location currLoc, ArrayList<Location> histLocs, Classification lastClassification
				// Update the place holders for the previous data.
				if(wifiScan != null) {
					previousWifiScans.add(wifiScan);
				}
				if (currLoc != null) {
					previousLocations.add(currLoc);
				}
				
				previousClassification = classification;
				
				// If the classification generated some results, pull them out
				// and store them in the Mobility point.
				if(classification.hasFeatures()) {
					try {
						mobilityPoint.setClassifierData(
								classification.getFft(), 
								classification.getVariance(),
								classification.getAverage(), 
								MobilityPoint.Mode.valueOf(classification.getMode().toUpperCase()));
					}
					catch(DomainException e) {
						throw new ServiceException(
								"There was a problem reading the classification's information.", 
								e);
					}
				}
				// If the features don't exist, then create the classifier data
				// with only the mode.
				else {
					try {
						mobilityPoint.setClassifierModeOnly(MobilityPoint.Mode.valueOf(classification.getMode().toUpperCase()));
					}
					catch(DomainException e) {
						throw new ServiceException(
								"There was a problem reading the classification's mode.", 
								e);
					}
				}
			}
		}
	}
	
	/**
	 * Retrieves the information about all of the Mobility points that satisfy
	 * the parameters. The username is required as that is how Mobility points
	 * are referenced; however, all other parameters are optional and limit the
	 * results based on their value.<br />
	 * <br />
	 * For example, if only a username is given, the result is all of the
	 * Mobility points for that user. If the username and start date are given,
	 * then all of the Mobility points made by that user after that date are
	 * returned. If the username, start date, and end date are all given, the
	 * result is the list of Mobility points made by that user on or after the
	 * start date and on or before the end date.
	 * 
	 * @param username The username of the user whose points are being queried.
	 * 				   Required.
	 * 
	 * @param startDate A date to which all returned points must be on or 
	 * 					after. Optional.
	 * 
	 * @param endDate A date to which all returned points must be on or before.
	 * 				  Optional.
	 * 
	 * @param privacyState A privacy state to limit the results to only those
	 * 					   with this privacy state. Optional.
	 * 
	 * @param locationStatus A location status to limit the results to only 
	 * 						 those with this location status. Optional.
	 * 
	 * @param mode A mode to limit the results to only those with this mode.
	 * 			   Optional.
	 * 
	 * @return A list of MobilityInformation objects where each object 
	 * 		   represents a single Mobility point that satisfies the  
	 * 		   parameters.
	 * 
	 * @throws ServiceException Thrown if there is an error.
	 */
	public List<MobilityPoint> retrieveMobilityData(
			final String username,  
			final DateTime startDate, final DateTime endDate, 
			final MobilityPoint.PrivacyState privacyState,
			final LocationStatus locationStatus, final Mode mode) 
			throws ServiceException {
		
		try {
			return userMobilityQueries.getMobilityInformation(
					username, 
					startDate, 
					endDate, 
					privacyState, 
					locationStatus, 
					mode);
		}
		catch(DataAccessException e) {
			throw new ServiceException(e);
		}
	}
	
	/**
	 * Retrieves the Mobility aggregate information for a user within a range.
	 * 
	 * @param username The user name of the user. Required
	 * 
	 * @param startDate Limits the results to only those on or after this date.
	 * 					Optional.
	 * 
	 * @param endDate Limits the results to only those on or before this date.
	 * 				  Optional.
	 * 
	 * @return A list of Mobility aggregate information.
	 * 
	 * @throws ServiceException There was an error.
	 */
	public List<MobilityAggregatePoint> retrieveMobilityAggregateData(
			final String username,
			final DateTime startDate,
			final DateTime endDate)
			throws ServiceException {
		
		try {
			return userMobilityQueries.getMobilityAggregateInformation(
					username,
					startDate,
					endDate);
		}
		catch(DataAccessException e) {
			throw new ServiceException(e);
		}
	}
	
	/**
	 * Retrieves all of the dates on which the user has created a Mobility 
	 * point within the date range.
	 * 
	 * @param startDate The earliest date to check if the user has any Mobility
	 * 					points.
	 * 
	 * @param endDate The latest date to check if the user has any Mobility
	 * 				  points.
	 * 
	 * @param username The user's username.
	 * 
	 * @return A collection of all of the dates.
	 * 
	 * @throws ServiceException Thrown if there is an error.
	 */
	public Collection<DateTime> getDates(
			final DateTime startDate,
			final DateTime endDate,
			final String username) 
			throws ServiceException {
		
		try {
			return userMobilityQueries.getDates(startDate, endDate, username);
		}
		catch(DataAccessException e) {
			throw new ServiceException(e);
		}
	}
	
	/**
	 * Verifies that a Mobility point can be updated by the requesting user.
	 * 
	 * @param username The requesting user's username.
	 * 
	 * @param mobilityId The Mobility point's unique identifier.
	 * 
	 * @throws ServiceException The user does not have sufficient permission to
	 * 							update the Mobility point.
	 */
	public void verifyUserCanUpdatePoint(
			final String username,
			final UUID mobilityId)
			throws ServiceException {
		
		try {
			// If the user is an admin, we are OK.
			if(userQueries.userIsAdmin(username)) {
				return;
			}
			
			// If the user owns the point, we are OK.
			String owner = userMobilityQueries.getUserForId(mobilityId);
			if((owner != null) && (owner.equals(username))) {
				return;
			}
			
			throw new ServiceException(
					ErrorCode.MOBILITY_INSUFFICIENT_PERMISSIONS,
					"You do not have permissions to modify this Mobility point.");
		}
		catch(DataAccessException e) {
			throw new ServiceException(e);
		}
	}
	
	/**
	 * Updates a Mobility point.
	 * 
	 * @param mobilityId The Mobility point's unique identifier. Required.
	 * 
	 * @param privacyState The new privacy state or null if no change is 
	 * 					   required.
	 * 
	 * @throws ServiceException There was an error.
	 */
	public void updateMobilityPoint(
			final UUID mobilityId,
			final MobilityPoint.PrivacyState privacyState)
			throws ServiceException {
		
		try {
			userMobilityQueries.updateMobilityPoint(mobilityId, privacyState);
		}
		catch(DataAccessException e) {
			throw new ServiceException(e);
		}
	}
}
