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

    protected HostService hostService;

    protected TopologyService topologyService;

    private LALMeasurement lal;

    private int count=0;

    public void registerSketch(LALMeasurement sketch) {
	this.lal = sketch;
    }

    @Override
    public void process(PacketContext context) {
        Short type = context.inPacket().parsed().getEtherType();
        if (type != Ethernet.TYPE_IPV4) {
            return;
        }


	log.info(context.toString());
	initMacTable(context.inPacket().receivedFrom());
	System.out.println("Now begin to process packets!");
	actLikeSwitch(context);
/*
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
*/	
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
 
/**
         * Example method. Floods packet out of all switch ports.
         *
         * @param pc the PacketContext object passed through from activate() method
         */
        public void actLikeHub(PacketContext pc) {
            pc.treatmentBuilder().setOutput(PortNumber.FLOOD);
            pc.send();
        }

        /**
         * Ensures packet is of required type. Obtain the PortNumber associated with the inPackets DeviceId.
         * If this port has previously been learned (in initMacTable method) build a flow using the packet's
         * out port, treatment, destination, and other properties.  Send the flow to the learned out port.
         * Otherwise, flood packet to all ports if out port is not learned.
         *
         * @param pc the PacketContext object passed through from activate() method
         */
        public void actLikeSwitch(PacketContext pc) {
	    System.out.println("In act like Switch");
            /*
             * Ensures the type of packet being processed is only of type IPV4 (not LLDP or BDDP).  If it is not, return
             * and do nothing with the packet. actLikeSwitch can only process IPV4 packets.
             */
            Short type = pc.inPacket().parsed().getEtherType();
            if (type != Ethernet.TYPE_IPV4) {
                return;
            }

	    /*
             * Learn the destination, source, and output port of the packet using a ConnectPoint and the
             * associated macTable.  If there is a known port associated with the packet's destination MAC Address,
             * the output port will not be null.
             */
            ConnectPoint cp = pc.inPacket().receivedFrom();
            Map<MacAddress, PortNumber> macTable = macTables.get(cp.deviceId());
            MacAddress srcMac = pc.inPacket().parsed().getSourceMAC();
            MacAddress dstMac = pc.inPacket().parsed().getDestinationMAC();
            macTable.put(srcMac, cp.port());
            PortNumber outPort = macTable.get(dstMac);

            /*
             * If port is known, set pc's out port to the packet's learned output port and construct a
             * FlowRule using a source, destination, treatment and other properties. Send the FlowRule
             * to the designated output port.
             */
            if (outPort != null) {
		count++;
	    	System.out.println("Switch_count="+count);
		System.out.println("Install rule:dstMac "+dstMac+" outPort "+outPort+" deviceId "+cp.deviceId());
	        IPv4 ipv4packet = (IPv4) pc.inPacket().parsed().getPayload();
		int srcaddr = ipv4packet.getSourceAddress();	
	        int destaddr = ipv4packet.getDestinationAddress();
		System.out.println("IPv4:src "+srcaddr+"destaddr "+destaddr);
                pc.treatmentBuilder().setOutput(outPort);

		FlowRule fr = DefaultFlowRule.builder()
                        .withSelector(DefaultTrafficSelector.builder().matchEthDst(dstMac).build())
                        .withTreatment(DefaultTrafficTreatment.builder().setOutput(outPort).build())
                        .forDevice(cp.deviceId()).withPriority(PacketPriority.REACTIVE.priorityValue())
                        .makeTemporary(60)
                        .fromApp(appId).build();
                flowRuleService.applyFlowRules(fr);
                pc.send();
            } else {
            /*
             * else, the output port has not been learned yet.  Flood the packet to all ports using
             * the actLikeHub method
             */
		System.out.println("Flooding the packet!");
                actLikeHub(pc);
            }
        }

    /**
         * puts the ConnectPoint's device Id into the map macTables if it has not previously been added.
         * @param cp ConnectPoint containing the required DeviceId for the map
         */
        private void initMacTable(ConnectPoint cp) {
            macTables.putIfAbsent(cp.deviceId(), Maps.newConcurrentMap());

        }

}
