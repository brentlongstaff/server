package edu.ucla.cens.awserver.cache;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import edu.ucla.cens.awserver.dao.ParameterLessDao;
import edu.ucla.cens.awserver.domain.CampaignNameVersion;
import edu.ucla.cens.awserver.domain.Configuration;

/**
 * The default implementation of a ConfigurationCacheService. Contains Configurations accessible by campaign name-version pairs.
 * 
 * @author selsky
 */
public class ConfigurationCacheService extends AbstractCacheService {
	private static Logger _logger = Logger.getLogger(ConfigurationCacheService.class);
	private Map<CampaignNameVersion, Configuration> _configurationMap;
	
	/**
	 * The provided DAO will be used to load the cache.
	 */
	public ConfigurationCacheService(ParameterLessDao dao) {
		super(dao);
		init();
	}
	
	/**
	 * Loads all campaign configurations into a cache for in-memory querying.
	 */
	private void init() {
		@SuppressWarnings("unchecked")
		List<Configuration> configurations = (List<Configuration>) _dao.execute(); 
		
		if(configurations.size() < 1) {
			throw new IllegalStateException("cannot startup with zero configurations");
		}
		
		_logger.info("loaded " + configurations.size() + " campaign configurations");
		
//		if(_logger.isDebugEnabled()) {
//			for(Configuration c : configurations) {
//				_logger.debug(c);
//			}
//		}
		
		_configurationMap = new HashMap<CampaignNameVersion, Configuration>();
		
		for(Configuration c : configurations) {
			CampaignNameVersion cnv = new CampaignNameVersion(c.getCampaignName(), c.getCampaignVersion());
			_configurationMap.put(cnv, c);
		}
	}

	/**
	 * Returns a Configuration given a CampaignNameVersion key.
	 * 
	 * TODO return a copy or an immutable configuration
	 */
	@Override
	public Object lookup(Object key) {
		return _configurationMap.get(key);
	}
	
	/**
	 * Returns whether this cache contains a Configuration identified by the provided key.
	 */
	@Override
	public boolean containsKey(Object key) {
		return _configurationMap.containsKey(key);
	}
	
	/**
	 * Returns a map of Configurations for the provided list of campaign names.
	 * 
	 * TODO the output list needs to be made Immutable as do the Configurations inside of the list
	 * TODO this should be named something like findAllForList for a more generic API to push up to the Cache interface
	 */
	public SortedMap<CampaignNameVersion, Configuration> lookupByCampaigns(List<?> campaignNames) {
		Set<CampaignNameVersion> keys = _configurationMap.keySet();
		SortedMap<CampaignNameVersion, Configuration> configurations = new TreeMap<CampaignNameVersion, Configuration>();
		
		for(CampaignNameVersion key : keys) {
			if(campaignNames.contains(key.getCampaignName())) {
				configurations.put(key, _configurationMap.get(key)); 
			}
		}
		
		return configurations;
	}
}
