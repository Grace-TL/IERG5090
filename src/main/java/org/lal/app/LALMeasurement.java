/*
 * Copyright 2017-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.lal.app;

import org.onlab.packet.IPv4;
import org.onlab.packet.MacAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.InterruptedException;
import java.lang.Thread;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

import org.onosproject.net.ElementId;


/**
 * Skeletal ONOS application component.
 */
//@Component(immediate = true)
public class LALMeasurement extends Thread {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private Thread t;
    private int count;
    private long totalCount;
    private boolean flag;  // flag == true, thread continue to run; flag == false, thread stop;
    private static CountMin cm;
    private static CountMin lc;
    private static LCount lcm;
    private int disCount;
    private Map<Header, Integer> packetmap = new HashMap<Header, Integer>(); 
    private Map<Header, Integer> supermap = new HashMap<Header, Integer>();
//    private CountMin cm = new CountMin(65535, 4);
    LALMeasurement() {
	this.count = 0;
	this.flag = true;
	this.cm = new CountMin(65535,4);
	this.lc = new CountMin(5000,4);
	this.lcm = new LCount(65536, 4, 5000);
    }


    public void start() {
	if (t == null) {
	    t = new Thread(this);
	    t.start();
	}
    }

    public void terminate() {
	this.flag = false;
    }

    public String getResult() {
	String result = "Top 5 heavy flows are:" + System.lineSeparator();	
	//output heavy packet
	MyUtil myutil = new MyUtil();
	int i = 5;
	Map<Header, Integer> sortedmap = myutil.sortByValue(this.packetmap);
	for(Map.Entry<Header, Integer> entry: sortedmap.entrySet()){
	    if(i > 0){
	        result = result 
                    + myutil.iptoString(entry.getKey().getSrcIp())
		    +" - "+myutil.iptoString(entry.getKey().getDstIp())
		    +" - "+entry.getKey().getSrcPort()
		    +" - "+entry.getKey().getDstPort()
		    +" - "+entry.getKey().getProtocol()
		    +" / "+entry.getValue();	
	        result += System.lineSeparator();
	    }
	    i--;
	}
	result += "Top 5 superspreaders are:" + System.lineSeparator();	
	i = 5;	
	Map<Header, Integer> tmpmap = myutil.sortByValue(this.supermap);
	for(Map.Entry<Header, Integer> entry: tmpmap.entrySet()){
	   if(i > 0){ 
	       result = result 
                    + myutil.iptoString(entry.getKey().getSrcIp())
//		    +" - "+myutil.iptoString(entry.getKey().getDstIp())
//		    +" - "+entry.getKey().getSrcPort()
//		    +" - "+entry.getKey().getDstPort()
//		    +" - "+entry.getKey().getProtocol()
		    +" / "+entry.getValue();	
	        result += System.lineSeparator();
	    }
	    i--;
	}
	result +="Total traffic size is: " + this.totalCount
		+ System.lineSeparator();
	result += "Number of unique flow is: " + this.disCount
		+ System.lineSeparator();

	return result;
    }

    public void measure(IPv4 pkt){
		
    }

    public void dealipv4(IPv4 pkt, ElementId switchid, MacAddress srcmacaddr, MacAddress destmacaddr,
	int srcipv4addr, int destipv4addr) {
	String switchstr = switchid.toString();
	String srcmacstr = srcmacaddr.toString();
	String destmacstr = destmacaddr.toString();
	String srcipv4str = pkt.fromIPv4Address(srcipv4addr);
	String destipv4str = new String(pkt.toIPv4AddressBytes(destipv4addr));
	System.out.println("switchid = " + switchstr
			+ ", srcmac = " + srcmacstr
			+ ", destmac = " + destmacstr
			+ ", srcipv4 = " + srcipv4str
			+ ", destipv4 = " + destipv4str);
    }

//top k heavy packet
    public synchronized void topk(Header headero, int val){
	System.out.println("Detect top k heavy flows");
	Header header = (Header)headero.clone();;
	if(this.cm == null)
	    return;
	cm.update(header, val);
	lc.update(header, 1);
	int packetsize = cm.pointEst(header);
	System.out.println("TEST:val="+packetsize
	+"/total:"+cm.getCount());
	if(packetsize > 0.1*cm.getCount()){
	    boolean flag = true;
	    for(Map.Entry<Header, Integer> entry: this.packetmap.entrySet()){
		if(entry.getKey().sameHeader(header)){
		    flag = false;
		    entry.setValue(packetsize); 
		    break;    
		}
	    }
	    if(flag){
	    	this.packetmap.put(header, packetsize);
	    }
	    if(this.packetmap.size()>=5){
		for(Iterator<Map.Entry<Header, Integer>> it = this.packetmap.entrySet().iterator(); it.hasNext();){
		    Map.Entry<Header, Integer> entry = it.next();
		    if(entry.getValue()<0.1*cm.getCount())
			it.remove();
		}  
	    }
	}
	disCount = lc.linearCount();
	totalCount = cm.getCount();
	//output heavy packet
	Map<Header, Integer> sortedmap = new MyUtil().sortByValue(this.packetmap);
	for(Map.Entry<Header, Integer> entry: sortedmap.entrySet())
	    System.out.println(entry.getKey().getSrcIp()
		+"-"+entry.getKey().getDstIp()
		+"-"+entry.getKey().getSrcPort()
		+"-"+entry.getKey().getDstPort()
		+"-"+entry.getKey().getProtocol()
		+"/"+entry.getValue());	
    }

//top k superspreader
    public void superspreader(Header headero, int val){
	System.out.println("Detect top 5 superspreaders!");
	Header header = (Header) headero.clone();
	this.lcm.update(header, 1);
	Header sipheader = new Header(headero.getSrcIp(),0,(short)0,(short)0,(byte)0);
	int degree = lcm.pointEst(header);
	System.out.println("TEST:degree=" + degree
	+"/total:"+lcm.getCount());
	if(degree > 0.05*lcm.getCount()){
	    boolean flag = true;
	    for(Map.Entry<Header, Integer> entry: this.supermap.entrySet()){
		if(entry.getKey().sameHeader(sipheader)){
		    flag = false;
		    entry.setValue(degree); 
		    break;    
		}
	    }
	    if(flag){
	    	this.supermap.put(sipheader, degree);
	    }
	    if(this.supermap.size()>=5){
		for(Iterator<Map.Entry<Header, Integer>> it = this.supermap.entrySet().iterator(); it.hasNext();){
		    Map.Entry<Header, Integer> entry = it.next();
		    if(entry.getValue()<0.03*lcm.getCount())
			it.remove();
		}  
	    }
	}
	//output heavy packet
	Map<Header, Integer> sortedmap = new MyUtil().sortByValue(this.supermap);
	int i = 5;
	for(Map.Entry<Header, Integer> entry: sortedmap.entrySet()){
	    if(i > 0)
	    System.out.println(entry.getKey().getSrcIp()
	//	+"-"+entry.getKey().getDstIp()
	//	+"-"+entry.getKey().getSrcPort()
	//	+"-"+entry.getKey().getDstPort()
	//	+"-"+entry.getKey().getProtocol()
		+" -- "+entry.getValue());	
	    i--;
	}

    }

//top k dos
    public void dos(){

    }

    public void reset(){
	this.cm.clear();
	this.lc.clear();
	this.lcm.clear();
	System.out.println("this.packetmap length = " + this.packetmap.size());
	this.packetmap.clear();
	System.out.println("after reset, packetmap length = " + this.packetmap.size());
	this.supermap.clear();
    }

    public void test(Header header, int val){
	System.out.println("[Count-Min]---------------------------");
	System.out.println("Total_count is "+cm.getCount());	
	System.out.println("Width:"+cm.getWidth());
	System.out.println("Depth:"+cm.getDepth());
	System.out.println("lgn:"+cm.lgn);
	System.out.println("packet length is " + val);
	cm.update(header, val);
	int count = cm.pointEst(header);
	System.out.println("Counter is : "+count);
	int discount = cm.linearCount();
	System.out.println("Dis_Counter is :"+discount);
    }


}
