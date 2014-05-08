package org.ohmage.routinesense;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

import org.joda.time.DateTime;



public class RoutineSense {
	/*HashMap<String, ArrayList<String>>*/ public static String getDayEvents(String username, DateTime date)
	{
		ProcessBuilder builder = new ProcessBuilder("mono", "/home/blongsta/RoutineSense/Loris.exe", username, date.year() + "-" + date.monthOfYear() + "-" + date.dayOfMonth());
//		ProcessBuilder builder = new ProcessBuilder("echo", "{ \"frog\" : 3 }");
		
		try {
			builder.redirectErrorStream(true);
			
			Process process = builder.start();
			InputStream is = process.getInputStream();
		    InputStreamReader isr = new InputStreamReader(is);
		    BufferedReader br = new BufferedReader(isr);
		    String line;
		    StringBuffer sb = new StringBuffer();
	        while ((line = br.readLine()) != null) 
	        {
	            sb.append(line);
		    }
			
//			InputStream in = process.getInputStream();
//			process.waitFor();
//			for (int i = 0; i < in.available(); i++)
//			{
//				sb.append(in.read() + "");
//			}
			String str = sb.toString();
			String header = "##DATASTART##";
			int start = str.indexOf(header) + header.length();
			str = str.substring(start);
			return str;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} /*catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		return null;
		
	}
}
