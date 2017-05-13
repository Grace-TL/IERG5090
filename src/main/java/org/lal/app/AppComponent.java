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

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.Ethernet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.InterruptedException;
import java.lang.Thread;
import java.util.List;

import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.event.Event;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.topology.TopologyEvent;
import org.onosproject.net.topology.TopologyListener;
import org.onosproject.net.topology.TopologyService;
import org.osgi.service.component.ComponentContext;

/**
 * Skeletal ONOS application component.
 */
@Component(immediate = true)
@Service(value = AppComponent.class)
public class AppComponent {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowObjectiveService flowObjectiveService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyService topologyService;

    private final TopologyListener topologyListener = new InternalTopologyListener();

    private ApplicationId appId;

    private LALPacketProcessor processor = new LALPacketProcessor();

    private int count = 0;
    private LALMeasurement lal;

    @Activate
    protected void activate(ComponentContext context) {
        lal = new LALMeasurement();
        lal.start();
	processor.registerSketch(lal);
	processor.registerHostservice(hostService);

	appId = coreService.registerApplication("org.lal.app");
	processor.registerAppId(appId);
	packetService.addProcessor(processor, PacketProcessor.director(2));
	topologyService.addListener(topologyListener);
	processor.registerTopologyservice(topologyService);
	processor.registerFlowObjectiveService(flowObjectiveService);
	requestIntercepts();

	log.info("Started", appId.id());
    }

    @Deactivate
    protected void deactivate() {
	withdrawIntercepts();
	packetService.removeProcessor(processor);
	topologyService.removeListener(topologyListener);
	processor = null;

        lal.terminate();

	log.info("Stopped");
    }
    
    public String getStatistic() {
	return Integer.toString(lal.getCount());
    }

    private void requestIntercepts() {
	// we deal with Ethernet packet and ARP packet
  	TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
	selector.matchEthType(Ethernet.TYPE_IPV4);
	packetService.requestPackets(selector.build(), PacketPriority.REACTIVE, appId);
//	selector.matchEthType(Ethernet.TYPE_ARP);
//	packetService.requestPackets(selector.build(), PacketPriority.REACTIVE, appId);
    }

    private void withdrawIntercepts() {
	TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
	selector.matchEthType(Ethernet.TYPE_IPV4);
	packetService.cancelPackets(selector.build(), PacketPriority.REACTIVE, appId);
	selector.matchEthType(Ethernet.TYPE_ARP);
	packetService.cancelPackets(selector.build(), PacketPriority.REACTIVE, appId);
    }

    private class InternalTopologyListener implements TopologyListener {
        @Override
        public void event(TopologyEvent event) {
            List<Event> reasons = event.reasons();
            if (reasons != null) {
                reasons.forEach(re -> {
                    if (re instanceof LinkEvent) {
                        LinkEvent le = (LinkEvent) re;
                        if (le.type() == LinkEvent.Type.LINK_REMOVED) {
                        }
                    }
                });
            }
        }
    } 	
}
