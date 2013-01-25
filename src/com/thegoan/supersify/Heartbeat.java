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

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Heartbeat extends Thread {
	
	//heartbeat details according to Sify
	private  String hbIP;
	private  String hbPort;
	private  int hbFreq;
	private  boolean hbEnable;
	private SuperSify ss;
	private int status;
	
	public Heartbeat(SuperSify ss) {
		this.ss = ss;
		status = 1;
	}
	public  void parseHeartBeat(Document doc) {
		Element element = (Element) doc.getElementsByTagName("HBS").item(0);
		if (element != null) {		
			String temp = element.getAttribute("serverIP");
			if (temp.length() > 0) hbIP = temp;
			temp = element.getAttribute("port");
			if (temp.length() > 0) hbPort = temp;
			temp = element.getAttribute("Enable");
			if (temp.length() > 0) hbEnable = "Y".equalsIgnoreCase(temp);
			temp = element.getAttribute("Freq");
			if (temp.length() > 0) hbFreq = Integer.parseInt(temp) * 60;
		}		
	}
	
	public boolean isHBEnabled() {
		return hbEnable;
	}
	
	public int getHBInterval() {
		return hbFreq == 0 ? 300 : hbFreq;				
	}
	
	public void setHBInterval(int interval) {
		hbFreq = interval;
	}
	
	public void stopBeat() {
		status = 0;
		if (isAlive()) interrupt();
	}
	
	public void run() {
	   
	}
	
}
