/*
 * SuperSify Version 0.8
 * Copyright (C) 2004-2007 Brian Fernandes
 * Website: http://thegoan.com/supersify
 * Email: infernalproteus@gmail.com 

 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/
package com.thegoan.supersify;

import android.os.Environment;
import android.os.StrictMode;
import android.widget.TextView;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Properties;
import java.util.Timer;


public class SuperSify {
		
	static final int INFO = 0;
	static final int RECVD = 1;
	static final int SENDING = 2;
	
	private static final String gConstKey = "3654egf^@q|$ds!as87&#%35ef@|d!s7#";
	private static final String gConstHex = "29355d121c211de0717c127166713ebb";
	
	private static final String SERVER_HTTP = "http://202.144.65.70";
	private static final String LOGOUT_URL = SERVER_HTTP + ":8090/bbandclient_v30/logout.php";
	private static final String LOGIN_URL = SERVER_HTTP + ":8090";
	private static final String CLIENT_VERSION = "3.22";
	private static final String SID_FILE = "supersify.sid";
	private static final String FC_STRING = "srcip=192.168.1.1&version=" + CLIENT_VERSION + "&os=xp";

	private static String loginURL;
	private static String logoutURL;
	private static String sessionID;
	String pvtIP;
	private static String serverTime;

	private DocumentBuilder documentBuilder;

	private HashMap params = new HashMap();
	private int vLevel;
	private boolean logout;
	String username;
	private String password;
	String macAddr;
	private String samIP;
	private Boolean hbeat;
	private Integer hbeatInterval;
	private boolean suppressKey;
	private boolean keepOpen;	
	private Heartbeat hbThread;
	private Integer kaInterval;
	private Integer kaRInterval;
	private Timer kaTimer;
    TextView t;
	public SuperSify(String[] args,TextView tt) throws ParserConfigurationException {

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();

        StrictMode.setThreadPolicy(policy);

        t=tt;
		//print("SuperSify version 0.9b\nhttp://thegoan.com/supersify\ninfernalproteus@gmail.com\n", INFO);
		parseParameters(args);
		documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
	}

	public boolean start() {
		//login or logout?
		if (!logout) {
			hbThread = new Heartbeat(this);
			return login();
		} else {
			return logout();
		}
	}
	
	private boolean waitForInput() {
		InputStreamReader inputStreamReader = new InputStreamReader ( System.in );
	    BufferedReader stdin = new BufferedReader ( inputStreamReader );
	    while (true) {
	    	try {
				String line = stdin.readLine();
				if ("quit".equals(line)) {
					return true;
				}
				if ("logout".equals(line)) {
					return logout();
				}
			} catch (IOException e) {
				print (e.getMessage(), INFO);
				break;
			}
		}
	    //hbThread.stopBeat();	    
	    try {
			stdin.close();
		} catch (IOException e) {
			//do nothing
		}
		return true;
	}
	
	private void printUsage() {
	    String loginString = "Login: {0} -u username -p password [-m XX-XX-XX-XX-XX-XX] [-v level] [-k]\nLogout: {0} -l";
	    		
	    String os = System.getProperty("os.name");
	    String cmdString;
	    if (os == null || os.indexOf("Windows") != -1) {
	        cmdString = "ss";
	    } else {
	        cmdString = "./ss.sh";
	    }
	    print (MessageFormat.format(loginString, new Object[] {cmdString}), INFO);
	    //print ("OR", INFO);
	    //print (MessageFormat.format(loginString, new Object[] {"java -jar supersify.jar"}), INFO);
	}
	
	
	public boolean coreLogin() {		
		try {
			// make first contact
			print("Connecting to Sify server...", INFO);
			String initXML = Util.postToServer(LOGIN_URL, FC_STRING, this);
			parseInitXML(initXML);
			print("SAM IP: " + samIP, INFO);
			// make sure mac address is available
			if (!Util.checkMacAddress(this)) {
				throw new RuntimeException("No mac address");
			}

			saveSessionDetails();

			// proceed with login
			print("Logging in...", INFO);
			String loginXML = Util.postToServer(loginURL, getLoginText(), this);

			Document doc = documentBuilder.parse(new InputSource(
					new StringReader(loginXML)));
			if (parseLoginXML(doc)) {
				hbThread.parseHeartBeat(doc);
				// heartbeat necessary?

				print(MessageFormat.format("Heartbeat {0} by user",
						new Object[] { hbeat == null ? "unspecified" : (hbeat
								.booleanValue() ? "enabled" : "disabled") }),
						INFO);
				print(MessageFormat.format("Heartbeat {0} by Sify",
						new Object[] { hbThread.isHBEnabled() ? "enabled"
								: "disabled" }), INFO);
				return true;
			}

		} catch (IOException e) {
			print("Could not connect to Sify server\n" + e.getMessage(), INFO);
		} catch (SAXException e) {
			print("Unable to parse login XML\n" + e.getMessage(), INFO);
		} catch (Exception e) {
			print("Error logging in\n" + e.getMessage(), INFO);
		}
		return false;
	}
	
	private boolean login() {
		if (username == null) {
		    printUsage();
			return false;
		}
		
		if (password == null) {
		    printUsage();
			return false;
		}
		
		print("Username: " + username, INFO);		
		try {
			boolean success = coreLogin();
			if (kaInterval != null) {
				kaTimer = new Timer();
				int delay = kaInterval.intValue() * 1000;
				kaTimer.scheduleAtFixedRate(
						new KeepAlive(this, kaRInterval), delay, delay);
				return waitForInput();
			}
			return success;
		} catch (RuntimeException e) {
			//no mac address...quit now
			return false;
		}
	}
	
	private String getLoginText() {
		int pvtATOI = Util.atoi(pvtIP);
		
		//create password encryption key
		String temp = gConstKey + pvtATOI + sessionID;
		String passBFKey = Util.getMD5(temp);
		
		//encrypt password
		String encPwd = Util.encrypt(password, passBFKey);
		
		//create login string
		String loginString = username + "|" + encPwd + "|" + pvtIP + "|" + macAddr + "|" + CLIENT_VERSION + "|" + sessionID + "|" + gConstHex;
		
		//create login encryption key
		temp = gConstKey + Util.genTimeStamp(serverTime) + pvtATOI + sessionID;
		String loginBFKey = Util.getMD5(temp);		
		
		//create authentication string
		String cons = Util.encrypt(loginString, loginBFKey);
		
		return "cons=" + cons + "&macaddress=" + macAddr;
	}
	
	private boolean parseLoginXML(Document doc) {
		String rMsg = Util.getReplyMessage(doc);
		if (rMsg != null) {
			print(rMsg, INFO);
		}
		Integer rCode = Util.getResponseCode(doc);
		if (rCode == null || rCode.intValue() < 0) {
			print("Error logging in", INFO);
			return false;
		}		
		
		//print account info
		print("Last login: " + Util.getText(doc, "LastLogin"), INFO);
		print("Product code: " + Util.getText(doc, "ProdCode"), INFO);
		print("Expiry date: " + Util.getText(doc, "Expiry"), INFO);
		print("Balance: " + Util.getText(doc, "Balance"), INFO);
		print("Quota: " + Util.getText(doc, "Quota"), INFO);
		return true;
	}
	
	private void parseInitXML(String xml) throws SAXException, IOException {
		Document doc = documentBuilder.parse(new InputSource(new StringReader(xml)));
		loginURL = Util.getText(doc, "LoginURL");
		sessionID = Util.getText(doc, "sessionID");
		pvtIP = Util.getText(doc, "pvtIP");
		serverTime = Util.getText(doc, "ServerTime");		
		hbThread.parseHeartBeat(doc);
		logoutURL = Util.getText(doc, "Logout", "url");		
		samIP = Util.getText(doc, "samIP");
	}
	
	
	private boolean logout() {		
		print("Logging out...", INFO);
		try {
			String logoutText;
			if (loadSessionDetails()) {
				logoutText = "username=" + username + "&srcip=" + pvtIP
						+ "&macaddress=" + macAddr + "&version="
						+ CLIENT_VERSION + "&sessionid=" + sessionID;
			} else {
				print("Could not load session details, attempting to logout anyway", INFO);
				logoutURL = LOGOUT_URL;
				logoutText = "";
			}
			String output = Util.postToServer(logoutURL, logoutText, this);
			Document doc = documentBuilder.parse(new InputSource(new StringReader(output)));
			Integer res = Util.getResponseCode(doc);
			String message = Util.getReplyMessage(doc);
			if (message != null) {
				print(message, INFO);
			}			
			if (res == null || res.intValue() < 0) {
				print("Error logging out", INFO);
				return false;
			}	
			return true;
		} catch (IOException e) {
			System.err.println("IOException while contacting Sify server\n" + e.getMessage());
		} catch (SAXException e) {
			System.err.println("Unable to parse logout XML\n" + e.getMessage());
		}
		return false;
	}
	
	private void saveSessionDetails() {
		Properties props = new Properties();
		props.put("username", username);
		props.put("pvtIP", pvtIP);
		props.put("macaddress", macAddr);
		props.put("sessionID", sessionID);
		props.put("logoutURL", logoutURL);
		try {
			FileOutputStream os = new FileOutputStream(Environment.getExternalStorageDirectory() + File.separator +SID_FILE);
			props.store(os, "SuperSify session details");
			os.close();
		} catch (Exception e) {
			print("Cannot save session details: " + e.getMessage(), INFO);
		} 
	}
	
	private boolean loadSessionDetails() {
		Properties props = new Properties();
		try {
			FileInputStream is = new FileInputStream(Environment.getExternalStorageDirectory() + File.separator +SID_FILE);
			props.load(is);
			is.close();
			print("Session details loaded", INFO);
		} catch (Exception e) {
			print("Could not load details from " + SID_FILE + ": " + e.getMessage(), INFO);
			return false;
		}
		username = props.getProperty("username");
		pvtIP = props.getProperty("pvtIP");
		macAddr = props.getProperty("macaddress");
		sessionID = props.getProperty("sessionID");
		logoutURL = props.getProperty("logoutURL");		
		return true;
	}
	
	public void print(String what, int level) {
		if (level <= vLevel) {
			switch(level) {				
				case RECVD:
					what = "Received<<\n" + what;
					break;
				case SENDING:
					what = "Sending>>\n" + what;
					break;
			}
            t.append(what+"\n");
			System.out.println(what);
		}
	}
	
	private void parseParameters(String[] args) {
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			try {
				if (arg.charAt(0) != '-') continue;
				if (i + 1 < args.length && args[i+1].charAt(0) != '-') {
					params.put("" + arg.charAt(1), args[++i]);
				} else {
					params.put("" + arg.charAt(1), null);
				}
			} catch (IndexOutOfBoundsException e) {
				//do nothing, just continue				
			}
		}
		
		logout = getBoolean("l");
		username = getParameter("u");
		password = getParameter("p");
		macAddr = getParameter("m");
		suppressKey = getBoolean("k");
		keepOpen = getBoolean("c");		
		
		if (getBoolean("h")) {
			hbeat = new Boolean("enable".equalsIgnoreCase(getParameter("h")));
		}
		if (getBoolean("b")) {
			try {
				hbeatInterval = new Integer(getParameter("b"));
			} catch (NumberFormatException e) {
				print("Could not parse heartbeat interval", INFO);
			}
		}
		
		if (getBoolean("a")) {
			String temp = getParameter("a");
			String[] parts = temp.split("/");
			try {
				kaInterval = new Integer(parts[0].trim());
			} catch (NumberFormatException e) {
				print("Could not parse keep alive interval", INFO);
			}
			if (parts.length > 1 && parts[1].trim().length() != 0) {
				try {
					kaRInterval = new Integer(parts[1].trim());
				} catch (NumberFormatException e) {
					print("Could not parse retry interval", INFO);
				}				
			}
		}
			
		
		String temp = getParameter("v");
		if(temp != null) {
			try {
				vLevel = Integer.parseInt(temp);
			} catch (NumberFormatException e) {
				print("Could not parse verbosity level, assuming 0", INFO);
			}
		}
	}	
	
	public boolean suppressKey() {
		return suppressKey;
	}
	
	private boolean getBoolean(String key) {
		return params.containsKey(key);
	}
	
	private String getParameter(String key) {
		return (String) params.get(key);
	}
	
	
}
