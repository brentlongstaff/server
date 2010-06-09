package edu.ucla.cens.awserver.cache;


/**
 * Provide data lookup functionality against a cache.
 * 
 * @author selsky
 */
public interface CacheService {
	
	public Object lookup(Object key);
	
}
