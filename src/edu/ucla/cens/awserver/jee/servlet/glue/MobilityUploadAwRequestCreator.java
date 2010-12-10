package edu.ucla.cens.awserver.jee.servlet.glue;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

import edu.ucla.cens.awserver.domain.UserImpl;
import edu.ucla.cens.awserver.request.AwRequest;
import edu.ucla.cens.awserver.request.UploadAwRequest;

/**
 * Transformer for creating an AwRequest for the upload feature.
 * 
 * @author selsky
 */
public class MobilityUploadAwRequestCreator implements AwRequestCreator {
	private static Logger _logger = Logger.getLogger(MobilityUploadAwRequestCreator.class);
	
	/**
	 * Default no-arg constructor. Simply creates an instance of this class.
	 */
	public MobilityUploadAwRequestCreator() {
		
	}
	
	/**
	 *  Pulls the u (userName), p (password), ci (client), and d (json data) parameters out of the HttpServletRequest
	 *  and places them in a new AwRequest. Assumes the parameters in the HttpServletRequest exist.
	 */
	public AwRequest createFrom(HttpServletRequest request) {
		@SuppressWarnings("unchecked")
		Map<String, String[]> parameterMap = (Map<String, String[]>) request.getAttribute("validatedParameterMap");
		
		String sessionId = request.getSession(false).getId(); // for connecting app logs to upload logs
		
		String userName = parameterMap.get("u")[0];
		String password = parameterMap.get("p")[0]; 
		String ci = parameterMap.get("ci")[0];
		String jsonData = parameterMap.get("d")[0];
		
		UserImpl user = new UserImpl();
		user.setUserName(userName);
		user.setPassword(password);
		
		AwRequest awRequest = new UploadAwRequest();

		awRequest.setStartTime(System.currentTimeMillis());
		awRequest.setSessionId(sessionId);
		awRequest.setUser(user);
		awRequest.setClient(ci);
		awRequest.setJsonDataAsString(jsonData);
		
		String requestUrl = request.getRequestURL().toString();
		if(null != request.getQueryString()) {
			requestUrl += "?" + request.getQueryString(); 
		}
		
		awRequest.setRequestUrl(requestUrl); // placed in the request for use in logging messages
		
		NDC.push("ci=" + ci); // push the client string into the Log4J NDC for the currently executing thread - this means that it 
                              // will be in every log message for the thread
		
		return awRequest;
	}
}

