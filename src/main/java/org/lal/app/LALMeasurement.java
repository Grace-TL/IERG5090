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

import org.onosproject.net.ElementId;

/**
 * Skeletal ONOS application component.
 */
//@Component(immediate = true)
public class LALMeasurement extends Thread {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private Thread t;
    private int count;
    private boolean flag;  // flag == true, thread continue to run; flag == false, thread stop;

    LALMeasurement() {
	this.count = 0;
	this.flag = true;
    }

    public void run() {
	System.out.println("LALMeasurement start");
//	while(this.flag) {
	   // System.out.println(count);
	   // count += 1;
	   // try {
	//	Thread.sleep(5000);
	  //  } catch (InterruptedException e) {
	//	System.out.println("org.lal.app.LALMeasurement.run: InterruptedException");
	  //  }
//	}
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

    public int getCount() {
	return this.count;
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
}
