package org.lal.app;
import java.io.Serializable;
import java.lang.CloneNotSupportedException;
public class Header implements Serializable, Cloneable{
    private int srcIp;
 
    private int dstIp;

    private short srcPort;

    private short dstPort;

    private byte protocol;
    
    Header(int sip, int dip, short sport, short dport, byte pro){

	this.srcIp = sip;
	this.dstIp = dip;
	this.srcPort = sport;
	this.dstPort = dport;
	this.protocol = pro;
    }

    Header(){
    }

    public Object clone(){
	Header header = null;
	try{
	    header = (Header)super.clone();
	    header.srcIp = this.srcIp;
	}catch(CloneNotSupportedException e){
	    e.printStackTrace();
	}
	return header;
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


    public int getSrcIp( ) {
	return this.srcIp;
    }

    public int getDstIp( ) {
	return this.dstIp;
    }

    public short getSrcPort( ) {
	return this.srcPort;
    }

    public short getDstPort( ) {
	return this.dstPort;
    }

    public byte getProtocol( ) {
	return this.protocol;
    }

    public boolean sameHeader(Header header){
	return this.srcIp == header.getSrcIp() &&
	       this.dstIp == header.getDstIp() &&
	       this.srcPort == header.getSrcPort() &&
	       this.dstPort == header.getDstPort() &&
	       this.protocol == header.getProtocol();
  
  }

    public void dump() {
	System.out.println("srcIp : " + srcIp
		+ "; srcPort : " + srcPort
		+ "; dstIp : " + dstIp 
		+ "; dstPort : " + dstPort
		+ "; protocol : " + protocol);
    }

 }
