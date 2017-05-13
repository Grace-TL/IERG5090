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

    Header(){
    }

    public void setSrcIp(int sourceip) {
	this.srcIp = sourceip;
    }

    public void setDstIp(int destip) {
	this.dstIp = destip;
    }

    public void setSrcPort(short sourceport) {
	this.srcPort = sourceport;
    }

    public void setDstPort(short destport) {
	this.dstPort = destport;
    }

    public void setProtocol(byte prot) {
	this.protocol = prot;
    }

    public void dump() {
	System.out.println("srcIp : " + srcIp
		+ "; srcPort : " + srcPort
		+ "; dstIp : " + dstIp 
		+ "; dstPort : " + dstPort
		+ "; protocol : " + protocol);
    }

 }
