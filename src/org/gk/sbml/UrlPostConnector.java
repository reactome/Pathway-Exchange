/* Copyright (c) 2009 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;


/**
 * Connect to a URL and send form-like information to it via a POST request.
 * You can use this to make a connection to a servlet.
 * 
 * @author David Croft
 *
 */

public class UrlPostConnector {
	private static final int BUFF_SIZE = 1024;
	private static final byte[] buffer = new byte[BUFF_SIZE];
	private static final String boundary = "---------------------------7d226f700d0";
	private String urlString;
	private List<FormParameter> formParameters = new ArrayList<FormParameter>();

	public UrlPostConnector(String urlString) {
		this.urlString = urlString;
	}

	public void addParameter(String name, String value) {
		FormParameter formParameter = new FormParameter(name, value);
		formParameters.add(formParameter);
	}
	
	public void addFileParameter(String name, String value) {
		FileFormParameter formParameter = new FileFormParameter(name, value);
		formParameters.add(formParameter);
	}
	
	private void writeParam(String name, String value, DataOutputStream out) {
		try {
			out.writeBytes("content-disposition: form-data; name=\"" + name + "\"\r\n\r\n");
			out.writeBytes(value);
		}
		catch (Exception e) {
			System.out.println(e.toString());
		}
	}

	private void writeFile(String name, String filePath, DataOutputStream out) {
		try {
			out.writeBytes("content-disposition: form-data; name=\"" + name + "\"; filename=\"" + filePath + "\"\r\n");
			out.writeBytes("content-type: application/octet-stream" + "\r\n\r\n");
			FileInputStream fis = new FileInputStream(filePath);
			while (true) {
				synchronized (buffer) {
					int amountRead = fis.read(buffer);
					if (amountRead == -1) {
						break;
					}
					out.write(buffer, 0, amountRead); 
				}
			}
			fis.close();
		}
		catch (Exception e) {
			System.out.println(e.toString());
		}
	}

	/**
	 * @param args the command line arguments
	 */
	public String connect() {
		System.err.println("UrlPostConnector.connect: entered");

		String response = "";
		try {
			URL url = new URL(urlString);            
			URLConnection connection =url.openConnection();
			connection.setDoOutput(true);
			connection.setDoInput(true);
			connection.setUseCaches(false);
			connection.setRequestProperty("Content-type","multipart/form-data; boundary=" + boundary);
			connection.setRequestProperty("Cache-Control", "no-cache");

			DataOutputStream out = new DataOutputStream(connection.getOutputStream());
			out.writeBytes("--" + boundary + "\r\n");
			int formParameterCount = formParameters.size();
			for (int i=0; i<formParameterCount; i++) {
				FormParameter formParameter = formParameters.get(i);
				if (formParameter.getType().equals("file"))
					writeFile(formParameter.getName(), formParameter.getValue(), out);
				else
					writeParam(formParameter.getName(), formParameter.getValue(), out);
				if (i < formParameterCount - 1)
					out.writeBytes("\r\n" + "--" + boundary + "\r\n");
			}
			out.writeBytes("\r\n" + "--" + boundary + "--\r\n");
			out.flush();
			out.close();

			HttpURLConnection httpConn = (HttpURLConnection)connection;
			int responseCode = httpConn.getResponseCode();
			
			System.err.println("UrlPostConnector.connect: HTTP response code: " + responseCode);

			InputStream inputStream = null;
			if (responseCode < 400)
			    inputStream = httpConn.getInputStream();
			else {
				InputStream errorStream = httpConn.getErrorStream();
			    String errorString;
			    if (errorStream == null)
			    	errorString = "input stream is null, response code: " + responseCode;
			    else
			    	errorString = readFromInputStream(errorStream);
				System.err.println("UrlPostConnector.connect: WARNING - problem connecting to URL, error message: " + errorString);
			    return response;
			}

			response = readFromInputStream(inputStream);
		} catch (Exception e) {  
			System.err.println("UrlPostConnector.connect: WARNING - problem getting data from URL");
			e.printStackTrace(System.err);
		}
		
		System.err.println("UrlPostConnector.connect: done");

		return response;
	}
	
	private String readFromInputStream(InputStream inputStream) {
		String response = "";
		
		if (inputStream == null) {
			System.err.println("UrlPostConnector.readFromInputStream: inputStream == null!!");
			return response;
		}
		
        try {
			BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
			String line;
			while ((line = in.readLine()) != null)
				response += line + "\n";
			in.close();
		} catch (Exception e) {
			System.err.println("UrlPostConnector.readFromInputStream: WARNING - problem getting data from URL");
			e.printStackTrace(System.err);
		}

        return response;
	}
	
	private class FormParameter {
		protected String type = "regular";
		private String name;
		private String value;

		public FormParameter(String name, String value) {
			this.name = name;
			this.value = value;
		}
		
		public String getType() {
			return type;
		}

		public String getName() {
			return name;
		}

		public String getValue() {
			return value;
		}
	}
	
	private class FileFormParameter extends FormParameter {
		public FileFormParameter(String name, String value) {
			super(name, value);
			type = "file";
		}
	}
}
