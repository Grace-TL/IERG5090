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
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.ElementId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.topology.TopologyService;

import java.util.Map;
import java.util.List;

public class LALPacketProcessor implements PacketProcessor {
    protected Map<DeviceId, Map<MacAddress, PortNumber>> macTables = Maps.newConcurrentMap();

    private final Logger log = LoggerFactory.getLogger(getClass());

    protected HostService hostService;

    protected TopologyService topologyService;

    private LALMeasurement lal;

    public void registerSketch(LALMeasurement sketch) {
	this.lal = sketch;
    }

    @Override
    public void process(PacketContext context) {

	InboundPacket pkt = context.inPacket();
	ElementId switchId = pkt.receivedFrom().elementId();
	Ethernet ethPkt = pkt.parsed();
	
	if (ethPkt == null) {
	    System.out.println("ethPkt == null");
	    return;
	}

	// print out the packet type
//	dumpPacketType(ethPkt);

//	if (isControlPacket(ethPkt)) {
//	    System.out.println("Controll packet");
//	    return;
//	}

	MacAddress srcMacAddr = ethPkt.getSourceMAC();
	MacAddress destMacAddr = ethPkt.getDestinationMAC();

	HostId id = HostId.hostId(destMacAddr);

	if (id.mac().isLinkLocal()) {
	    System.out.println("linklocal packet");
	    return;
	}	

	Host dst = hostService.getHost(id);
	if (dst == null) {
//	    System.out.println("Don't know how to route this packet");
	    flood(context);
	    return;
	}
	System.out.println("We can deal with this packet");
	
	if (ethPkt.getEtherType() == Ethernet.TYPE_IPV4) {
	    IPv4 ipv4packet = (IPv4) ethPkt.getPayload();
	    int srcaddr = ipv4packet.getSourceAddress();	
	    int destaddr = ipv4packet.getDestinationAddress();
	    this.lal.dealipv4(ipv4packet, switchId, srcMacAddr, destMacAddr, srcaddr, destaddr); 
	}
	
    }   
    
    private void flood(PacketContext context) {
        if (topologyService.isBroadcastPoint(topologyService.currentTopology(),
                                             context.inPacket().receivedFrom())) {
            packetOut(context, PortNumber.FLOOD);
        } else {
            context.block();
        }
    }

    private void packetOut(PacketContext context, PortNumber portNumber) {
        context.treatmentBuilder().setOutput(portNumber);
        context.send();
    }

    public boolean isControlPacket(Ethernet eth) {
	short type = eth.getEtherType();
	return type == Ethernet.TYPE_LLDP || type == Ethernet.TYPE_BSN;
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

}
