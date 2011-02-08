package edu.ucla.cens.awserver.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import edu.ucla.cens.awserver.dao.Dao;
import edu.ucla.cens.awserver.dao.DataAccessException;
import edu.ucla.cens.awserver.domain.MobilityQueryResult;
import edu.ucla.cens.awserver.request.AwRequest;

/**
 * @author selsky
 */
public class ChunkedMobilityQueryService extends AbstractDaoService {
	private static Logger _logger = Logger.getLogger(ChunkedMobilityQueryService.class);
	
	public ChunkedMobilityQueryService(Dao dao) {
		super(dao);
	}
	
	/**
	 * Dispatches to a DAO to query for mobility data and then collates the mobility modes into 10-record chunks. TODO add a
	 * configurable chunk size? or allow a chunk size to be passed in as a HTTP param?
	 */
	@Override
	public void execute(AwRequest awRequest) {
		try {
			getDao().execute(awRequest);
			@SuppressWarnings("unchecked")
			List<MobilityQueryResult> results = (List<MobilityQueryResult>) awRequest.getResultList();
			List<MobilityQueryResult> chunkedResults = new ArrayList<MobilityQueryResult>();
			MobilityQueryResult currentChunkedResult = new MobilityQueryResult();
			int count = 0, still = 0, walk = 0, run = 0, bike = 0, drive = 0;
			
			for(int i = 0; i < results.size(); i++) {
				MobilityQueryResult result = results.get(i);
				count++;
				
				currentChunkedResult.setLocation(result.getLocation());
				currentChunkedResult.setLocationStatus(result.getLocationStatus());
				currentChunkedResult.setTimestamp(result.getTimestamp());
				currentChunkedResult.setTimezone(result.getTimezone());
				
				if("still".equals(result.getValue())) {
					still++;
				} else if("walk".equals(result.getValue())) {
					walk++;
				} else if("run".equals(result.getValue())) {
					run++;
				} else if("bike".equals(result.getValue())) {
					bike++;
				} else if("drive".equals(result.getValue())) {
					drive++;
				} else {
					_logger.warn("unknown mobility mode found: " + result.getValue());
				}
				
				if(10 == count || (i == (results.size() - 1))) {
					JSONObject o = new JSONObject();
					try {
						o.put("still", still);
						o.put("walk", walk);
						o.put("run", run);
						o.put("bike", bike);
						o.put("drive", drive);
					} catch (JSONException jsone) { // can never happen! 
						_logger.warn(jsone);
					}
					
					currentChunkedResult.setValue(o);
					chunkedResults.add(currentChunkedResult);
					count = 0; still = 0; walk = 0; run = 0; bike = 0; drive = 0;
				}
				
				results.clear();
				awRequest.setResultList(chunkedResults);
			}
			
		} catch (DataAccessException dae) {
			throw new ServiceException(dae);
		}
	}
}
