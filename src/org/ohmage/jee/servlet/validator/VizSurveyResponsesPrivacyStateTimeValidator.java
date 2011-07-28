package org.ohmage.jee.servlet.validator;

import javax.servlet.http.HttpServletRequest;

/**
 * Basic validation that the required parameters exist and are reasonable for a
 * survey response privacy state over time visualization request.
 * 
 * @author John Jenkins
 */
public class VizSurveyResponsesPrivacyStateTimeValidator extends VisualizationValidator {
	/**
	 * Default constructor.
	 */
	public VizSurveyResponsesPrivacyStateTimeValidator() {
		super();
	}

	/**
	 * Validates that all required parameters exist.
	 * 
	 * @throws MissingAuthTokenException Thrown if the authentication / session
	 * 									 token is not in the header.
	 */
	@Override
	public boolean validate(HttpServletRequest httpRequest) throws MissingAuthTokenException {
		return super.validate(httpRequest);
	}
}