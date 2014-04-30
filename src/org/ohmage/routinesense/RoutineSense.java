package org.ohmage.routinesense;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import org.joda.time.DateTime;



public class RoutineSense {
	/*HashMap<String, ArrayList<String>>*/ public static String getDayEvents(String username, DateTime date)
	{
		ProcessBuilder builder = new ProcessBuilder("mono", "Loris.exe", username, date.toString());
		builder.redirectErrorStream(true);
		try {
			Process process = builder.start();
			StringBuffer sb = new StringBuffer();
			InputStream in = process.getInputStream();
			process.waitFor();
			for (int i = 0; i < in.available(); i++)
			{
				sb.append(in.read() + "");
			}
			String str = sb.toString(); 
			return str;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
		
	}
}
