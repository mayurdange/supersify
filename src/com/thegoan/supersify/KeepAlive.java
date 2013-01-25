package com.thegoan.supersify;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.TimerTask;

public class KeepAlive extends TimerTask {

	private static final int CONNECT_TIMEOUT = 5000;
	private static final String ALIVE_URL = "google.com";
	
	private SuperSify ss;
	private Integer kaRInteval;
	
	public KeepAlive(SuperSify ss, Integer kaRInterval) {
		this.ss = ss;
		this.kaRInteval = kaRInterval;
	}
	
	public void run() {		
		if (isAlive()) return;
		ss.print("\nConnection dead, attempting reconnect", SuperSify.INFO);
		while (true) {
			if (ss.coreLogin() || kaRInteval == null) {
				break;
			}
			try {
				Thread.sleep(kaRInteval.intValue() * 1000);
			} catch (InterruptedException e) {
				//do nothing
			}
		}		
	}	
	
	public boolean isAlive() {
		InetAddress addr;
		try {
			addr = InetAddress.getByName(ALIVE_URL);
		} catch (UnknownHostException e) {
			return false;
		}
		SocketAddress sockaddr = new InetSocketAddress(addr, 80);
        Socket sock = new Socket();
        try {
			sock.connect(sockaddr, CONNECT_TIMEOUT);
		} catch (IOException e) {
			return false;
		} finally {
			try {
				sock.close();
			} catch (IOException e) {
				//do nothing
			}			
		}
		return true;
	}

}
