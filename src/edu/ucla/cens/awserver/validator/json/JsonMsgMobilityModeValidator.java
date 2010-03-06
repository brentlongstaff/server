package edu.ucla.cens.awserver.validator.json;

import java.util.List;

import org.json.JSONObject;

import edu.ucla.cens.awserver.request.AwRequest;
import edu.ucla.cens.awserver.util.JsonUtils;
import edu.ucla.cens.awserver.validator.AwRequestAnnotator;

/**
 * Validates the mode element from an AW JSON mode_only or mode_features message.
 * 
 * @author selsky
 */
public class JsonMsgMobilityModeValidator extends AbstractAnnotatingJsonObjectValidator {
//	private static Logger _logger = Logger.getLogger(JsonMsgMobilityModeValidator.class);
	private String _key = "mode";
	private List<String> _allowedValues;
	private boolean _doFeaturesValidation;
		
	/**
     * @throws IllegalArgumentException if the provded list for allowed values is null or empty
	 */
	public JsonMsgMobilityModeValidator(AwRequestAnnotator awRequestAnnotator, List<String> allowedValues, boolean doFeaturesValidation) {
		super(awRequestAnnotator);
		if(null == allowedValues || allowedValues.size() == 0) {
			throw new IllegalArgumentException("a non-null non-empty array of allowed values is required");
		}
		_allowedValues = allowedValues;
		_doFeaturesValidation = doFeaturesValidation;
	}
	
	/**
	 * Validates the mode. If set up to doFeaturesValidation on construction, will attempt to retrieve the mode from the features
	 * object instead of the "root" object. Assumes the features object exists.  
	 * 
	 * @return true if the value returned from the AwRequest for the key "mode" exists and is a valid mode
	 * @return false otherwise
	 */
	public boolean validate(AwRequest awRequest, JSONObject jsonObject) {		 
		String mode = null; 
		
		if(! _doFeaturesValidation) {
			
			mode = JsonUtils.getStringFromJsonObject(jsonObject, _key);
			
		} else { 
			
			JSONObject object = JsonUtils.getJsonObjectFromJsonObject(jsonObject, "features");
			mode = JsonUtils.getStringFromJsonObject(object, _key);
			
		}	
		
		if(null == mode) {
			getAnnotator().annotate(awRequest, "mode in message is null");
			return false;
		}
		
		if(! _allowedValues.contains(mode)) {
			getAnnotator().annotate(awRequest, "invalid mode: " + mode);
			return false;
		}
		
		return true;
	}
}
