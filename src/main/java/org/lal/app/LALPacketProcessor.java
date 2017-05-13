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

import com.google.common.collect.Maps;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.MacAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.onosproject.event.Event;
import org.onosproject.net.ElementId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.topology.TopologyService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.ForwardingObjective.Builder;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;

import java.util.Map;
import java.util.List;

public class LALPacketProcessor implements PacketProcessor {

    protected Map<DeviceId, Map<MacAddress, PortNumber>> macTables = Maps.newConcurrentMap();

    private ApplicationId appId;

    protected FlowRuleService flowRuleService;
    private final Logger log = LoggerFactory.getLogger(getClass());

    protected FlowObjectiveService flowObjectiveService;
    protected HostService hostService;

    protected TopologyService topologyService;

    private LALMeasurement lal;

    private int count=0;

    public void registerSketch(LALMeasurement sketch) {
	this.lal = sketch;
    }

    @Override
    public void process(PacketContext context) {
	
	// unpack context
	InboundPacket pkt = context.inPacket();
	
	// unpack inpacket
	Ethernet ethPkt = pkt.parsed();
	ConnectPoint cp = pkt.receivedFrom();

	// filter out some packet
	if (context.isHandled()) return;
	if (ethPkt == null) return;
	if (isControlPacket(ethPkt)) return;
	if (isIpv6Multicast(ethPkt)) return;
	HostId id = HostId.hostId(ethPkt.getDestinationMAC());
	if (id.mac().isLinkLocal()) return;
	if (ethPkt.getEtherType() == Ethernet.TYPE_IPV4 && id.mac().isMulticast()) return;
	if (ethPkt.getDestinationMAC().isBroadcast()) return;

	System.out.println("received from : " + cp.toString()
		+ "; srcMAC : " + ethPkt.getSourceMAC()
		+ "; dstMAC : " + ethPkt.getDestinationMAC());

	count += 1;
	System.out.println("process " + count);
	// if from switch not registered, we need to create macmap for that switch
	initMacTable(context.inPacket().receivedFrom());

	// add the srcMac and port to the macTable of the incoming switch
	updateMacEntry(cp.deviceId(), ethPkt.getSourceMAC(), cp.port());
	dumpMacTable();

	// check mactable whether we know how to route this packet
	PortNumber dstport = checkRoute(cp.deviceId(), ethPkt.getDestinationMAC());

	// route
	if (dstport != PortNumber.FLOOD) {
	    System.out.println("installRule in " + cp.toString()
			+ "; from srcMAC " + ethPkt.getSourceMAC()
			+ "; to dstMAC " + ethPkt.getDestinationMAC()
			+ "; via port " + dstport);
	    installRule(context, dstport);
	}  
	packetOut(context, dstport);
 	
	
    }

    private void installRule(PacketContext context, PortNumber portNumber) {
	Ethernet inPkt = context.inPacket().parsed();

	TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
	selectorBuilder.matchInPort(context.inPacket().receivedFrom().port())
		.matchEthSrc(inPkt.getSourceMAC())
		.matchEthDst(inPkt.getDestinationMAC());
	
	// treatment 1
	TrafficTreatment.Builder tbuilder = DefaultTrafficTreatment.builder()
		.add(Instructions.createOutput(PortNumber.CONTROLLER))
		.add(Instructions.createOutput(portNumber));
	TrafficTreatment treatment = tbuilder.build();

//	System.out.println("1111111111111111111");

	// treatment 2
//	TrafficTreatment treatment = DefaultTrafficTreatment.builder()
//		.setOutput(portNumber)
//		.build();

//	System.out.println("22222222222222222222");
	ForwardingObjective.Builder fbuilder = DefaultForwardingObjective.builder()
		.withSelector(selectorBuilder.build())
		.withTreatment(treatment)
		.withPriority(10)
		.withFlag(ForwardingObjective.Flag.VERSATILE)
		.fromApp(appId)
		.makeTemporary(20);
//	System.out.println("33333333333333333333");
	ForwardingObjective fObjective = fbuilder.add();
//	System.out.println("444444444444444444444");
	flowObjectiveService.forward(context.inPacket().receivedFrom().deviceId(),
		fObjective);
//	System.out.println("555555555555555555555");
    }

    private void packetOut(PacketContext context, PortNumber portNumber) {
        context.treatmentBuilder().setOutput(portNumber);
        context.send();
    }

    public boolean isControlPacket(Ethernet eth) {
	short type = eth.getEtherType();
	return type == Ethernet.TYPE_LLDP || type == Ethernet.TYPE_BSN;
    }

    private boolean isIpv6Multicast(Ethernet eth) {
	return eth.getEtherType() == Ethernet.TYPE_IPV6 && eth.isMulticast();
    } 
    
    public void dumpPacketType(Ethernet ethPkt) {
	if (ethPkt.getEtherType() == Ethernet.TYPE_ARP) {
	    System.out.println("ARP Packet");
	} else if (ethPkt.getEtherType() == Ethernet.TYPE_RARP) {
	    System.out.println("RARP Packet");
	} else if (ethPkt.getEtherType() == Ethernet.TYPE_IPV4) {
	    System.out.println("IPv4 Packet");
	} else if (ethPkt.getEtherType() == Ethernet.TYPE_IPV6) {
	    System.out.println("IPv6 Packet");
	} else if (ethPkt.getEtherType() == Ethernet.TYPE_LLDP) {
	    System.out.println("LLDP Packet");
	} else if (ethPkt.getEtherType() == Ethernet.TYPE_VLAN) {
	    System.out.println("VLAN Packet");
	} else if (ethPkt.getEtherType() == Ethernet.TYPE_BSN) {
	    System.out.println("BSN Packet");
	} else {
	    System.out.println("Unknown Packet");
	}
    }

    public void registerHostservice(HostService service) {
	hostService = service;
    }

    public void registerTopologyservice(TopologyService top) {
	topologyService = top;
    }

    public void registerAppId(ApplicationId id) {
	appId = id;
    }

    public void registerFlowObjectiveService(FlowObjectiveService flowservice) {
	flowObjectiveService = flowservice;
    }
 
    private void initMacTable(ConnectPoint cp) {
        macTables.putIfAbsent(cp.deviceId(), Maps.newConcurrentMap());
    }

    private void dumpMacTable() {
	System.out.println("--------MacTable--------");
	for(Map.Entry<DeviceId, Map<MacAddress, PortNumber>> entry : macTables.entrySet()) {
	    String tmpstr = new String("");
	    DeviceId did = entry.getKey();
	    tmpstr = tmpstr + did.toString() + " : \n";
	
	    Map<MacAddress, PortNumber> tmpmap = entry.getValue();
	    for(Map.Entry<MacAddress, PortNumber> subentry : tmpmap.entrySet()) {
		tmpstr = tmpstr + "	" + subentry.getKey().toString() + "/" + subentry.getValue() + ";\n";
	    }   
	    System.out.println(tmpstr);
	}
    }
  
    private void updateMacEntry(DeviceId switchid, MacAddress srcmac, PortNumber port) {
	Map<MacAddress, PortNumber> mactable = macTables.get(switchid);
	mactable.putIfAbsent(srcmac, port);
    }

    private PortNumber checkRoute(DeviceId switchid, MacAddress dstmac) {
	Map<MacAddress, PortNumber> mactable = macTables.get(switchid);
	if(mactable.containsKey(dstmac)) {
	  return mactable.get(dstmac);
	} else {
	  return PortNumber.FLOOD;
	}
    }
			
}
