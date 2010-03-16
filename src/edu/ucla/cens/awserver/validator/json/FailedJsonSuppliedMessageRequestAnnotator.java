package edu.ucla.cens.awserver.validator.json;

import org.json.JSONException;
import org.json.JSONObject;

import edu.ucla.cens.awserver.request.AwRequest;
import edu.ucla.cens.awserver.request.SensorUploadAwRequest;
import edu.ucla.cens.awserver.validator.AwRequestAnnotator;

/**
 * An annotator to be used when a JSON-based request fails and the message is custom to each invocation of the annotate() method.
 * @see FailedJsonRequestAnnotator
 * 
 * @author selsky
 */
public class FailedJsonSuppliedMessageRequestAnnotator implements AwRequestAnnotator {
	
	/**
     * Sets failed request properties on the AwRequest. The provided message is used as the "base" JSON error message.
     * 
     * @see FailedJsonRequestAnnotator.annotate for the JSON output message syntax
	 */
	public void annotate(AwRequest awRequest, String message) {
		awRequest.setFailedRequest(true);
		JSONObject jsonObject = null;
		
		try {
			
			jsonObject = new JSONObject(message);
			// now add the original request URL and the original JSON input message to the error output
			
			if(awRequest instanceof SensorUploadAwRequest) { // TODO - this is a bit of a hack and needs to be fixed
				jsonObject.put("request_url", awRequest.getRequestUrl());
				jsonObject.put("request_json", awRequest.getJsonDataAsString());
			}
		
		} catch(JSONException jsone) {  
		
			throw new IllegalStateException(jsone);
		}
		
		awRequest.setFailedRequestErrorMessage(jsonObject.toString());
	}
}
