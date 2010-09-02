package edu.ucla.cens.awserver.jee.servlet;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import edu.ucla.cens.awserver.controller.Controller;
import edu.ucla.cens.awserver.jee.servlet.glue.AwRequestCreator;
import edu.ucla.cens.awserver.request.AwRequest;
import edu.ucla.cens.awserver.util.StringUtils;

/**
 * Servlet for processing sensor data uploads.
 * 
 * @author selsky
 */
@SuppressWarnings("serial") 
public class MobilityUploadServlet extends AbstractAwHttpServlet {
	private static Logger _logger = Logger.getLogger(MobilityUploadServlet.class);
	private Controller _controller;
	private AwRequestCreator _awRequestCreator;
	private List<String> _parameterList;
	
	/**
	 * Default no-arg constructor.
	 */
	public MobilityUploadServlet() {
		_parameterList = new ArrayList<String>(Arrays.asList(new String[]{"u","phv","d","p","c"})); // user, phone version, data 
		                                                                                            // (JSON), password, campaign id
	}
		
	/**
	 * JavaEE-to-Spring glue code. When the web application starts up, the init method on all servlets is invoked by the Servlet 
	 * container (if load-on-startup for the Servlet > 0). In this method, names of Spring "beans" are pulled out of the 
	 * ServletConfig and the names are used to retrieve the beans out of the ApplicationContext. The basic design rule followed
	 * is that only Servlet.init methods contain Spring Framework glue code.
	 */
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		String servletName = config.getServletName();
		
		String awRequestCreatorName = config.getInitParameter("awRequestCreatorName");
		String controllerName = config.getInitParameter("controllerName");
		
		if(StringUtils.isEmptyOrWhitespaceOnly(awRequestCreatorName)) {
			throw new ServletException("Invalid web.xml. Missing awRequestCreatorName init param. Servlet " + servletName +
					" cannot be initialized and put into service.");
		}
		if(StringUtils.isEmptyOrWhitespaceOnly(controllerName)) {
			throw new ServletException("Invalid web.xml. Missing controllerName init param. Servlet " + servletName +
					" cannot be initialized and put into service.");
		}
		
		// OK, now get the beans out of the Spring ApplicationContext
		// If the beans do not exist within the Spring configuration, Spring will throw a RuntimeException and initialization
		// of this Servlet will fail. (check catalina.out in addition to aw.log)
		ServletContext servletContext = config.getServletContext();
		ApplicationContext applicationContext = WebApplicationContextUtils.getWebApplicationContext(servletContext);
		
		_awRequestCreator = (AwRequestCreator) applicationContext.getBean(awRequestCreatorName);
		_controller = (Controller) applicationContext.getBean(controllerName);
	}
	
	/**
	 * Dispatches to a Controller to perform sensor data upload. If the upload fails, an error message is persisted to the response.
	 * If the request is successful, allow Tomcat to simply return HTTP 200.
	 */
	protected void processRequest(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException { 
		
		// Top-level security validation
		if(! prevalidate(request)) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND); // if some entity is doing strange stuff, just respond with a 404
			                                                      // in order not to give away too much about how the app works
			return;
		}
		
		// Map data from the inbound request to our internal format
		AwRequest awRequest = _awRequestCreator.createFrom(request);
		
		Writer writer = new BufferedWriter(new OutputStreamWriter(getOutputStream(request, response)));
	    
		try {
			// Execute feature-specific logic
			_controller.execute(awRequest);
		    
			response.setContentType("application/json");
			
			if(awRequest.isFailedRequest()) { 
				
				writer.write(awRequest.getFailedRequestErrorMessage());
				
			} else {
				
				writer.write("{\"result\":\"success\"}");
			}
		}
		
		catch(Exception e) { 
			
			_logger.error("an error occurred on sensor data upload", e);
			// the exception is not wrapped inside a ServletException in order to avoid sending the Tomcat HTTP 500 error page 
			// back to the client
			
			// instead of the exception being wrapped and re-thrown, send the error code for severe errors
			writer.write("{\"errors\":[{\"code\":\"0103\",\"text\":\"server error\"}]}");
			
		}
		
		finally {
			
			if(null != writer) {
				writer.flush();
				writer.close();
			}
			
			request.getSession().invalidate(); // sensor data uploads only have state for the duration of a request
		}
	}
	
	/**
	 * Dispatches to processRequest().
	 */
	@Override protected final void doGet(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException {

		_logger.warn("GET disallowed on phone authentication.");
		response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);

	}

	/**
	 * Dispatches to processRequest().
	 */
	@Override protected final void doPost(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException {
        
		_logger.info("in doPost");
		processRequest(request, response);
	
	}
	
	/**
	 * Pre-validate to avoid situations where someone is sending purposefully malicious data 
	 */
	private boolean prevalidate(HttpServletRequest request) {
		Map<?,?> parameterMap = request.getParameterMap(); // String, String[]
		
		// Check for missing or extra parameters
		
		if(parameterMap.size() != _parameterList.size()) {
			_logger.warn("an incorrect number of parameters was found on phone mobility upload: " + parameterMap.size());
			return false;
		}
		
		// Check for duplicate parameters
		
		Iterator<?> iterator = parameterMap.keySet().iterator();
		
		while(iterator.hasNext()) {
			String key = (String) iterator.next();
			String[] valuesForKey = (String[]) parameterMap.get(key);
			
			if(valuesForKey.length != 1) {
				_logger.warn("an incorrect number of values (" + valuesForKey.length + ") was found for parameter " + key);
				return false;
			}
		}
		
		// Check for parameters with unknown names
		
		iterator = parameterMap.keySet().iterator(); // there is no way to reset the iterator so just obtain a new one
		
		while(iterator.hasNext()) {
			String name = (String) iterator.next();
			if(! _parameterList.contains(name)) {
			
				_logger.warn("an incorrect parameter name was found: " + name);
				return false;
			}
		}
		
		String u = (String) request.getParameter("u");
		String c = (String) request.getParameter("c");
		String p = (String) request.getParameter("p");
		String phv = (String) request.getParameter("phv");
		
		// Check for abnormal lengths (buffer overflow attack)
		// 50 is an arbitrary number for length, but for these parameters it would be very strange
		
		if(greaterThanLength("user", "u", u, 50)
		   || greaterThanLength("campaign", "c", c, 50)
		   || greaterThanLength("phone version", "phv", phv, 50)
		   || greaterThanLength("password", "p", p, 180) // handle up to 60 %-encoded characters
		) {
			return false;
		}
		
		// The JSON data is not checked because its length is so variable and potentially huge (some messages are 700000+ characters
		// when URL-encoded). It will be heavily validated once inside the main application validation layer.
		
		// The default setting for Tomcat is to disallow requests that are greater than 2MB
		
		return true;
	}
}
