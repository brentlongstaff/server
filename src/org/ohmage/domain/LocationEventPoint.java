package org.ohmage.domain;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import org.ohmage.exception.DomainException;

/**
 * This class represents a single Mobility point, but only contains the 
 * information specifically needed when aggregating Mobility information.
 *
 * @author John Jenkins
 */
public class LocationEventPoint implements Comparable<LocationEventPoint> {
	private final DateTime start;
	private final Duration duration;
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
	public LocationEventPoint(
			final DateTime start, 
			final Duration duration,
			final Double latitude,
			final Double longitude,
			final Long locationID) 
			throws DomainException {
		
		if(start == null) {
			throw new DomainException("The start is null.");
		}
		if(duration == null) {
			throw new DomainException("The duration is null.");
		}
		if (latitude == null) {
			throw new DomainException("The latitude is null.");
		}
		if (longitude == null) {
			throw new DomainException("The longitude is null.");
		}
		if (locationID == null) {
			throw new DomainException("The locationID is null.");
		}
		
		this.start = start;
		this.duration = duration;
		this.latitude = latitude;
		this.longitude = longitude;
		this.locationID = locationID;
	}
	
	/**
	 * Returns the start time as DateTime.
	 * 
	 * @return The start time as DateTime.
	 */
	public DateTime getStart() {
		return start;
	}
	
	/**
	 * Returns the duration.
	 * 
	 * @return The duration of this location event.
	 */
	public Duration getDuration() {
		return duration;
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
	public int compareTo(LocationEventPoint other) {
		long difference = start.getMillis() - other.start.getMillis();
		
		return (difference < 0) ? -1 : (difference > 0) ? 1 : 0;
	}
}
