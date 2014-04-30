package org.ohmage.domain;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.ohmage.exception.DomainException;


public class AppEventPoint implements Comparable<AppEventPoint> {
	
	private final DateTime start;
	private final Duration duration;
	private final String label;
	
	/**
	 * Creates a new point with the given date-time and mode.
	 * 
	 * @param date The date this Mobility point was created.
	 * 
	 * @param mode The mode of this Mobility point.
	 * 
	 * @throws DomainException The date and/or mode were null.
	 */
	public AppEventPoint(
			final DateTime start, 
			final Duration duration,
			final String label) 
			throws DomainException {
		
		if(start == null) {
			throw new DomainException("The start is null.");
		}
		if(duration == null) {
			throw new DomainException("The duration is null.");
		}
		if (label == null) {
			throw new DomainException("The mode is null.");
		}
		
		this.start = start;
		this.duration = duration;
		this.label = label;
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
	
	public String getLabel() {
		return label;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(AppEventPoint other) {
		long difference = start.getMillis() - other.start.getMillis();
		
		return (difference < 0) ? -1 : (difference > 0) ? 1 : 0;
	}
}
