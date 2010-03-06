package edu.ucla.cens.awserver.validator.json;

import org.json.JSONObject;

import edu.ucla.cens.awserver.dao.Dao;
import edu.ucla.cens.awserver.dao.DataAccessException;
import edu.ucla.cens.awserver.request.AwRequest;
import edu.ucla.cens.awserver.util.JsonUtils;
import edu.ucla.cens.awserver.validator.AwRequestAnnotator;
import edu.ucla.cens.awserver.validator.ValidatorException;

/**
 * Validator for the version id from a prompt message.
 * 
 * @author selsky
 */
public class JsonMsgPromptVersionIdValidator extends AbstractDaoAnnotatingJsonObjectValidator {
	private String _key = "version_id";
		
	public JsonMsgPromptVersionIdValidator(AwRequestAnnotator awRequestAnnotator, Dao dao) {
		super(awRequestAnnotator, dao);
	}
	
	/**
	 * @return true if the value returned from the JSONObject for the key "version_id" exists and is a valid version id for the 
	 * campaign found in the AwRequest.
	 * @return false otherwise
	 */
	public boolean validate(AwRequest awRequest, JSONObject jsonObject) {		 
		String versionId = JsonUtils.getStringFromJsonObject(jsonObject, _key);
		
		if(null == versionId) {
			getAnnotator().annotate(awRequest, "version_id in message is null or invalid");
			return false;
		}

		awRequest.setVersionId(versionId);
		
		try {
		
			getDao().execute(awRequest);
			
		} catch(DataAccessException daoe) { // unrecoverable error, just rethrow
			
			throw new ValidatorException(daoe);
		}
		
		return true;
	}
}
