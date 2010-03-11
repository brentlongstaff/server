package edu.ucla.cens.awserver.validator;

import edu.ucla.cens.awserver.request.AwRequest;
import edu.ucla.cens.awserver.util.StringUtils;

/**
 * Validates the userName from the User within the AwRequest.
 * 
 * @author selsky
 */
public class AwRequestUserNameValidator extends AbstractAnnotatingRegexpValidator {
	
	public AwRequestUserNameValidator(String regexp, AwRequestAnnotator awRequestAnnotator) {
		super(regexp, awRequestAnnotator);
	}
	
	/**
	 * @throws ValidatorException if the userName property of the User within the AwRequest is null, the empty string, all 
	 * whitespace, or if it contains numbers or special characters aside from ".".  
	 */
	public boolean validate(AwRequest awRequest) {
		
		if(null == awRequest.getUser()) { // logical error!
			
			throw new ValidatorException("User object not found in AwRequest");
		}
		
		if(StringUtils.isEmptyOrWhitespaceOnly(awRequest.getUser().getUserName())) {
			
			getAnnotator().annotate(awRequest, "empty user name found");
			return false;
		
		}
		
		String userName = awRequest.getUser().getUserName();
		
		if(! _regexpPattern.matcher(userName).matches()) {
		
			getAnnotator().annotate(awRequest, "incorrect character found in user name: " + userName);
			return false;
		}
		
		return true;
	}
}
