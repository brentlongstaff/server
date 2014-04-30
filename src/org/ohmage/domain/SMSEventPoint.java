package org.ohmage.domain;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.ohmage.exception.DomainException;


public class SMSEventPoint implements Comparable<SMSEventPoint> {
	
	
	public enum Direction {
		IN, OUT
	}
	private final DateTime time;
	
	private final String contact;
	private final Direction direction; 
	
	/**
	 * Creates a new point with the given date-time and mode.
	 * 
	 * @param date The date this Mobility point was created.
	 * 
	 * @param mode The mode of this Mobility point.
	 * 
	 * @throws DomainException The date and/or mode were null.
	 */
	public SMSEventPoint(
			final DateTime time,
			final String contact,
			final Direction direction) 
			throws DomainException {
		
		if (time == null) {
			throw new DomainException("The start is null.");
		}
		if (contact == null) {
			throw new DomainException("The contact is null.");
		}
		if (direction == null) {
			throw new DomainException("The direction is null.");
		}
		
		this.time = time;
		this.contact = contact;
		this.direction = direction;
	}
	
	/**
	 * Returns the start time as DateTime.
	 * 
	 * @return The start time as DateTime.
	 */
	public DateTime getTime() {
		return time;
	}
	
	public String getContact() {
		return contact;
	}

	public Direction getDirection() {
		return direction;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(SMSEventPoint other) {
		long difference = time.getMillis() - other.time.getMillis();
		
		return (difference < 0) ? -1 : (difference > 0) ? 1 : 0;
	}
}
