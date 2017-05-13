package org.lal.app;
import java.io.Serializable;

public class Header implements Serializable{
    public static int srcIp;
 
    public static int dstIp;

    public static short srcPort;

    public static short dstPort;

    public static byte protocol;
    
    Header(int sip, int dip, short sport, short dport, byte pro){

	this.srcIp = sip;
	this.dstIp = dip;
	this.srcPort = sport;
	this.dstPort = dport;
	this.protocol = pro;
    }

 }
