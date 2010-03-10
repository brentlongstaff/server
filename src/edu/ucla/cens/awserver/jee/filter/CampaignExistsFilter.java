package edu.ucla.cens.awserver.jee.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import edu.ucla.cens.awserver.controller.Controller;
import edu.ucla.cens.awserver.controller.ControllerException;
import edu.ucla.cens.awserver.request.AwRequest;
import edu.ucla.cens.awserver.request.ResultListAwRequest;
import edu.ucla.cens.awserver.util.StringUtils;

/**
 * Filter for determining if a user or device is attempting to access a subdomain that maps onto a campaign.
 *
 * @author selsky
 */
public class CampaignExistsFilter implements Filter {
	private static Logger _logger = Logger.getLogger(CampaignExistsFilter.class);
	private Controller _controller;
	
	
	/**
	 * Default no-arg constructor.
	 */
	public CampaignExistsFilter() {
		
	}
	
	/**
	 * Destroys instance variables.
	 */
	public void destroy() {
		_controller = null;
	}
	
	/**
	 * Looks for a Controller name (Spring bean id) in the FilterConfig and attempts to retrieve the Controller out of the Spring
	 * ApplicationContext.
	 * 
	 * @throws ServletException if an init-param named controllerName cannot be found in the FilterConfig
	 */
	public void init(FilterConfig config) throws ServletException {
		String filterName = config.getFilterName();
		String controllerName = config.getInitParameter("controllerName");
		
		if(StringUtils.isEmptyOrWhitespaceOnly(controllerName)) {
			throw new ServletException("Invalid web.xml. Missing controllerName init param. Filter " + filterName +
					" cannot be initialized and put into service.");
		}
		
		ServletContext servletContext = config.getServletContext();
		ApplicationContext applicationContext = WebApplicationContextUtils.getWebApplicationContext(servletContext);
		_controller = (Controller) applicationContext.getBean(controllerName);
	}
	
	/**
	 * Checks that a user is hitting a valid request subdomain.
	 */
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
		throws ServletException, IOException {
		
		// ---- TODO this belongs in an AwRequestCreator
		AwRequest awRequest = new ResultListAwRequest();
		
		String url = ((HttpServletRequest) request).getRequestURL().toString();
		String uri = ((HttpServletRequest) request).getRequestURI();
		String subdomain = StringUtils.retrieveSubdomainFromUrlString(url);
		awRequest.setSubdomain(subdomain);
		awRequest.setRequestUrl(url + "?" +  ((HttpServletRequest) request).getQueryString());
		// ------- end TODO
		
		try {
		
			_controller.execute(awRequest);
			
			if(! awRequest.isFailedRequest()) {
				
				chain.doFilter(request, response);
				
			} else { // a subdomain that is not bound to a campaign was found
				
				if(uri.startsWith("/app/sensor")) { // a phone or device is attempting access
					
					ServletOutputStream outputStream = response.getOutputStream();
					outputStream.print(awRequest.getFailedRequestErrorMessage());
					outputStream.flush();
					
				} else { // assume it's a browser. Tomcat will return a custom 404 page if configured to do so.
					
					((HttpServletResponse) response).sendError(HttpServletResponse.SC_NOT_FOUND);
				}
			}	
		}
		catch(ControllerException ce) {
			_logger.error("", ce); // make sure the stack trace gets into our app log
			throw new ServletException(ce); // re-throw and allow Tomcat to redirect to the configured error page. the stack trace will also end up
			                                // in catalina.out
		}
	}
}
