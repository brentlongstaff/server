<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="edu.ucla.cens.awserver.domain.User" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%
    response.setHeader( "Pragma", "no-cache" );
    response.setHeader( "Cache-Control", "no-cache" );
    response.setDateHeader( "Expires", 0 );
%>

<html>
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
	
    <!-- Force IE8 into IE7 mode for VML compatibility -->
    <meta http-equiv="X-UA-Compatible" content="IE=EmulateIE7" />

    <title>Data Visualizations</title>
    
    
    
    <link href="/css/zp-compressed.css" type="text/css" media="screen, print" rel="stylesheet" />
    <link href="/css/zp-print.css" type="text/css" media="print" rel="stylesheet" />
    <link href="/favicon.ico" rel="shortcut icon" type="image/x-icon">

    
    

	<!-- CSS includes for various formatting -->
	<!-- Custom CSS for the "dashboard" setup -->
    <link type="text/css" href="/css/dashboard.css" rel="stylesheet" />  
    <!-- Formats the tabs for jquery.tools -->
	<link type="text/css" href="/css/tabs.css" rel="stylesheet" />
	<!-- Custom css specifically for the jqueryui -->
	<link type="text/css" href="/css/jquery-ui-1.7.2.custom.css" rel="stylesheet" />
	<link type="text/css" href="/css/jquery-validity.css" rel="stylesheet" /> 
    <link rel="stylesheet" type="text/css" href="/css/dateinput.css"/>
    <link type="text/css" href="/css/gwt/standard.css" rel="stylesheet" /> 
    
    <!-- A large number of javascript includes, will reduce -->
	<!-- Main jQuery library -->
    <script type="text/javascript" src="/js/thirdparty/jquery/jquery-1.4.2.min.js"></script>  
    <!-- jQuery UI toolkit for tabs -->
    <script type="text/javascript" src="/js/thirdparty/jquery/jquery.tools.min.js"></script>   
    <!-- jQuery UI for Datepicker -->
    <script type="text/javascript" src="/js/thirdparty/jquery/jquery-ui-1.7.2.custom.min.js"></script>
    <!-- jQuery TinySort plugin, to sort DIVs by their attributes -->
    <script type="text/javascript" src="/js/thirdparty/jquery/jquery.tinysort.min.js"></script>
    <!-- log4java like logging -->
    <script type="text/javascript" src="/js/lib/misc/log4javascript.js"></script>
    <!-- Protovis graphing library -->
    <script type="text/javascript" src="/js/thirdparty/Protovis/protovis-d3.2.js"></script>
    <!-- Useful additions to Javascript objects -->
    <script type="text/javascript" src="/js/lib/misc/array_lib.js"></script>
    <script type="text/javascript" src="/js/lib/misc/date-functions.js"></script>
    <script type="text/javascript" src="/js/thirdparty/misc/date.format.js"></script>
    <!-- Various validators to validate user input and server responses -->
    <script type="text/javascript" src="/js/thirdparty/jquery/jquery.validity.min.js"></script>
    <script type="text/javascript" src="/js/lib/validator/DateValidator.js"></script>
    <!-- Contains the query response visualization types -->
    <script type="text/javascript" src="/js/response_list.js"></script>
    <!-- Generates the different graph types using Protovis -->
    <script type="text/javascript" src="/js/lib/DashBoard/DashBoard.js"></script>
    <script type="text/javascript" src="/js/lib/DataSource/DataSource.js"></script>
    <script type="text/javascript" src="/js/lib/DataSource/AwData.js"></script>
    <script type="text/javascript" src="/js/lib/DataSource/AwDataCreator.js"></script>
    <script type="text/javascript" src="/js/lib/Graph/ProtoGraph.js"></script>
    <script type="text/javascript" src="/js/lib/DashBoard/StatDisplay.js"></script>
    <script type="text/javascript" src="/js/lib/DashBoard/View.js"></script>
    
    <!-- Compiled GWT javascript -->
    <script type="text/javascript" src="/js/gwt/andwellnessvisualizations.nocache.js"></script>
    
	
    <!--[if IE]>
	<link href="/css/zp-ie.css" type="text/css" media="screen" rel="stylesheet" />
	<![endif]-->
   
    
   
    <!-- Main page javascript.  Instantiates the dashboard and datasource.  Does
         basic form validation. -->
    <script type="text/javascript">
	
    // Holds the currently requested start date and number of days
    //var startDate = new Date();
    //var numDays = 0;

    // Holds the current page's DashBoard setup
    var dashBoard = null;
	
	// Grab the logged in user name from the jsp session
	var userName = "<c:out value="${sessionScope.user.userName}"></c:out>";
	var isResearcher = "<c:out value="${sessionScope.user.isResearcherOrAdmin}"></c:out>";
	
    // Main logger
    var log = log4javascript.getLogger();
	
    // Called when document is done loading
    $(function() {
        // Setup logging
        var popUpAppender = new log4javascript.PopUpAppender();
        popUpAppender.setThreshold(log4javascript.Level.DEBUG);
        var popUpLayout = new log4javascript.PatternLayout("%d{HH:mm:ss} %-5p - %m%n");
        popUpAppender.setLayout(popUpLayout);
        log.addAppender(popUpAppender);

        // Uncomment the line below to disable logging
        log4javascript.setEnabled(false);

        // Setup the datepickers for the date input box
        // old datepicker
        //$("#startDate").datepicker({dateFormat: 'yy-mm-dd'});
 
 		
 		var today = new Date().incrementDay(-13);
        $(":date").dateinput({
        	format: 'yyyy-mm-dd',	// the format displayed for the user
        	selectors: true,        // whether month/year dropdowns are shown
        })
        // Initially set to 13 days ago
        .data("dateinput").setValue(today);
        
        // Override the default submit function for the form
        $("#grabDateForm").submit(sendJsonRequest);
		
        
        // Setup the dash board with the campaign configuration JSON
        dashBoard = new DashBoard();
        dashBoard.setUserName(userName);
        dashBoard.initialize();
		
        // Initialize the page by grabbing config information from server
        sendJsonRequestInit();
    });

    /*
     * Uses the validity library to validate the date form inputs on this page.
     * Call this from the submit override before sending a request to the server.
     */
    function validateDateFormInputs() {
        // Start validation
        $.validity.start();

        // Validate the startDate field
        $("#startDate")
            .require("A starting date is required")
            .assert(DateValidator.validate, "Date is invalid.");

        // All of the validator methods have been called
        // End the validation session
        var result = $.validity.end();
        
        // Return whether it's okay to proceed with the request
        return result.valid;
    }

    /*
     * Ask for enough info to initialize the webpage
     */
    function sendJsonRequestInit() {
    	// Switch on the loading graphic
    	// currently broken, comes back on before the data is actually loaded into the graphs.
        //dashBoard.loading(true);

		// This will initialize the main user upload page
		DataSourceJson.requestData(DataSourceJson.DATA_HOURS_SINCE_LAST_SURVEY);
    }

    /*
     * Grab the form inputs, validate, and send a request to the server for data.
     */
    function sendJsonRequest() {
      // Validate inputs
        if (!validateDateFormInputs()) {
            if (log.isWarnEnabled()) {
                log.warn("Validation failed!");
            }
            //return false;	    	 
        }

        // Switch on the loading graphic
        //dashBoard.loading(true);
		
        // Grab the URL from the form
        var startDateString = $("#startDate").val();
        var numDaysString = $("#numDays").val();
		
        // Parse out the forms
        // Hack these into the dashBoard for now
        dashBoard.startDate = Date.parseDate(startDateString, "Y-m-d");
        dashBoard.numDays = parseInt(numDaysString);

        // Setup params
        var endDateString = dashBoard.startDate.incrementDay(dashBoard.numDays).dateFormat("Y-m-d");
        var params = {
        	    's': startDateString,
        	    'e': endDateString
        }; 

        if (log.isInfoEnabled()) {
            log.info("Grabbing data from " + startDateString + " to " + endDateString);
        }
        
		// Grab hours since last survey information
	 	DataSourceJson.requestData(DataSourceJson.DATA_HOURS_SINCE_LAST_SURVEY);

		// Grab percentage good location updates
		DataSourceJson.requestData(DataSourceJson.DATA_LOCATION_UPDATES);
		
		// Grab hours since last location update
		DataSourceJson.requestData(DataSourceJson.DATA_HOURS_SINCE_LAST_UPDATE);

		// Grab number of completed surveys per day from server
		DataSourceJson.requestData(DataSourceJson.DATA_SURVEYS_PER_DAY, params);

        // Grab EMA data from the server 
        DataSourceJson.requestData(DataSourceJson.DATA_EMA, params);
        
		// Grab number of mobilities from the survey per day
		DataSourceJson.requestData(DataSourceJson.DATA_MOBILITY_MODE_PER_DAY, params);
		  
        // Return false to cancel the usual submit functionality
        return false;
    }

	
    </script>
	
  </head>
  <body>
  <!-- Wrap the entire page in a custom div, maybe can use body instead -->
  <div id="wrapper" class="f">
  
  <!-- Dashboard banner -->
  <div id="banner">

  </div>
  
  <div id="controls">
 	Choose a time period:
    <form method="post" action="/app/q/ema" id="grabDateForm">
      <label for="startDate" class="label">Start Date:</label>
      <input id="startDate" type="date" />
      <label for="numDays" class="label">Length:</label>
      <select id="numDays">
	    <option value="7">1 week</option>
	    <option selected="selected" value="14">2 weeks</option>
	    <option value="21">3 weeks</option>
	    <option value="28">4 weeks</option>
      </select>
      <button type="submit" id="submit">Go</button>                
    </form>
    
    <div id="gwt_test"></div>
  </div>
  
  <!-- Main body of the dashboard -->
  <div id="main">

  </div>
  
  <!-- Dashboard footer -->
  <div id="footer">
    Question? Comment? Problem? Email us at andwellness-info@cens.ucla.edu.
  </div>
  
  </div>
  </body>
</html>
