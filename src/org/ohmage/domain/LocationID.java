package org.ohmage.domain;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import org.ohmage.exception.DomainException;

/**
 * This class represents a location identity
 *
 * @author John Jenkins
 */
public class LocationID implements Comparable<LocationID> {
	
	private final Double latitude;
	private final Double longitude;
	private final Long locationID;
	
	/**
	 * Creates a new point with the given date-time and mode.
	 * 
	 * @param date The date this Mobility point was created.
	 * 
	 * @param mode The mode of this Mobility point.
	 * 
	 * @throws DomainException The date and/or mode were null.
	 */
	public LocationID(
			final Double latitude,
			final Double longitude,
			final Long locationID) 
			throws DomainException {
		
		if (latitude == null) {
			throw new DomainException("The latitude is null.");
		}
		if (longitude == null) {
			throw new DomainException("The longitude is null.");
		}
		if (locationID == null) {
			throw new DomainException("The locationID is null.");
		}
		
		this.latitude = latitude;
		this.longitude = longitude;
		this.locationID = locationID;
	}
	
	
	
	/**
	 * Returns the latitude.
	 *  
	 */
	public Double getLatitude() {
		return latitude;
	}

	public Double getLongitude() {
		return longitude;
	}

	public Long getLocationID() {
		return locationID;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(LocationID other) {
		long difference = locationID - other.locationID;
		
		return (difference < 0) ? -1 : (difference > 0) ? 1 : 0;
	}
}
