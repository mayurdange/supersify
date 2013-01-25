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

import gnu.crypto.cipher.Blowfish;
import gnu.crypto.cipher.IBlockCipher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Util {

	public static boolean checkMacAddress(SuperSify ss) {
		if (ss.macAddr == null) {
			ss.print("Attempting to obtain macaddress", SuperSify.INFO);
			try {
				InetAddress inet = getInetAddr(ss.pvtIP);
				//get mac address for private IP
				NetworkInterface iface = NetworkInterface.getByInetAddress(getInetAddr(ss.pvtIP));
				byte[] mac = iface.getHardwareAddress();				
				ss.macAddr = Util.toHexString(mac, "-").toUpperCase();
			} catch (NoSuchMethodError e) {
				ss.print("Mac address detection requires Java 6", SuperSify.INFO);					
			} catch (Exception e) {
				ss.print("Could not obtain mac address for " + ss.pvtIP + ": " + e.getMessage(), SuperSify.INFO);
			}
		}
		if (ss.macAddr != null) {
			ss.print("Mac address: " + ss.macAddr, SuperSify.INFO);			
			return true;
		} else {
			ss.print("Please specify macadddress using \"-m XX-XX-XX-XX-XX-XX\"", SuperSify.INFO);
			return false;
		}
	}
	
	public static String encrypt(String input, String key) {
		byte[] iBytes = input.getBytes();
		//pad if required to make size a multiple of eight
		int len = iBytes.length;
		if (len % 8 != 0) {
			len += 8 - (len % 8);
			byte[] nBytes = new byte[len];
			System.arraycopy(iBytes, 0, nBytes, 0, iBytes.length);
			iBytes = nBytes;
		}
		
		Blowfish fish = new Blowfish();
		HashMap attrib = new HashMap();
		attrib.put(IBlockCipher.KEY_MATERIAL, key.getBytes());
		try {
			fish.init(attrib);
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		}
		
		StringBuffer result = new StringBuffer();
		for (int i = 0; i < len; i+=8) {			
			byte[] out = new byte[8];
			fish.encryptBlock(iBytes, i, out, 0);			
			result.append(toHexString(out, null));
		}
		
		return result.toString();
	}
	
	public static String genTimeStamp(String serverTime) {
		String[] parts = serverTime.split(" |-|:");
		int dateInt = Integer.parseInt(parts[0] + parts[1] + parts[2]);
		int timeInt = Integer.parseInt(parts[3] + parts[4] + parts[5]);
		int delta = dateInt - timeInt;
		StringBuffer buf = new StringBuffer();
		while (delta != 0) {
			buf.insert(0, 48 + delta%10);
			delta /= 10;
		}
		return buf.toString();
	}
	
	
	private static InetAddress getInetAddr(String ip) {
		String[] parts = ip.split("\\.");
		byte[] addr = new byte[4];
		for (int i=0; i < 4; i++) {
			addr[i] = (byte) Integer.parseInt(parts[i]);
		}
		try {
			return InetAddress.getByAddress(addr);
		} catch (UnknownHostException e) {
			return null;
		}
	}
	
	public static int atoi(String ip) {
		String[] parts = ip.split("\\.");
		String binary = "";
		for (int i = 0; i < parts.length; i++) {
//			Integer part = Integer.parseInt(parts[i]);
			Integer part = new Integer(parts[i]);
			String c = "00000000" + Integer.toBinaryString(part.intValue());
			binary += c.substring(c.length() - 8);
		}
		return Integer.parseInt(binary, 2);
	}
	
	public static String getMD5(String input) {
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			//very unlikely
			e.printStackTrace();
		}
		md.update(input.getBytes());
		return toHexString(md.digest(), null);
	}

	public static String toHexString(byte[] dig, String join) {
		StringBuffer hash = new StringBuffer();
		for (int i = 0; i < dig.length; i++) {
			int a = dig[i] & 0xFF; //make unsigned
			String hex = Integer.toHexString(a);
			if (join != null && i > 0) hash.append(join);
			if (a <= 0xF) hash.append("0"); //ensure 2 digits
			hash.append(hex);
		}
		return hash.toString();
	}
	

	//XML utilities	
	public static Integer getResponseCode(Document doc) {
		NodeList codeList = doc.getElementsByTagName("ResponseCode");
		Node code = codeList.item(0);
		if (code != null) {
			try {
				return new Integer(code.getTextContent().trim());
			} catch (NumberFormatException e) {
				//do nothing				
			}
		}
		return null;
	}
	
	public static String getReplyMessage(Document doc) {
		NodeList replyList = doc.getElementsByTagName("ReplyMessage");
		Node replyElement = replyList.item(0);
		if (replyElement != null) {
			return replyElement.getTextContent();
		}		
		return null;
	}
	
	public static String getText(Document doc, String name) {
		return getText(doc, name, null);
	}
	
	public static String getText(Document doc, String name, String attribute) {
		Element element = (Element) doc.getElementsByTagName(name).item(0);
		if (element != null) {
			if (attribute == null) {
				return element.getTextContent();
			} else {
				return element.getAttribute(attribute);
			}
		}
		return null;
	}
	

	
	
	public static String postToServer(String loc, String data, SuperSify ss) throws IOException {
		URLConnection conn = (URLConnection) new URL(loc).openConnection();
		conn.setDoOutput(true);
//		conn.setRequestMethod("POST");
		conn.setRequestProperty("User-Agent", "BBClient");
		
		ss.print(data, SuperSify.SENDING);
		OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
		wr.write(data);
		wr.flush();
		
		BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		String line;
		StringBuffer buffer = new StringBuffer();
		while ((line = rd.readLine()) != null) {
			buffer.append(line);
		}
		wr.close();
		rd.close();
		String recvd = buffer.toString();
		ss.print(recvd, SuperSify.RECVD);
		return recvd;
	}
	
}
