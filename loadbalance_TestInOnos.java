/*
 * Copyright 2020-present Open Networking Foundation
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
package nthu.wmnet.loadbalance;

import static org.onosproject.net.config.NetworkConfigEvent.Type.CONFIG_ADDED;
import static org.onosproject.net.config.NetworkConfigEvent.Type.CONFIG_UPDATED;
import static org.onosproject.net.config.basics.SubjectFactories.APP_SUBJECT_FACTORY;

import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import com.google.common.collect.ImmutableSet;
import org.onosproject.cfg.ComponentConfigService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.ReferenceCardinality;

import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onlab.packet.*;
import org.onlab.packet.Ethernet;
import org.onlab.packet.ICMP;
import org.onlab.packet.IPv4;
import org.onlab.packet.MacAddress;

import org.onosproject.net.packet.*;


import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;

import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.PortNumber;
import org.onosproject.net.host.HostService;

import org.onosproject.net.topology.TopologyService;
import org.onosproject.net.topology.TopologyListener;

import org.onosproject.net.topology.TopologyEvent;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.event.Event;

import org.onosproject.net.topology.PathService; 

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.event.Event;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.PortNumber;
import org.onosproject.net.host.HostService;
import org.onosproject.net.topology.TopologyService;

import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.ForwardingObjective;

import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.edge.EdgePortService;
import org.onosproject.net.topology.PathService; 
import java.util.Dictionary;
import java.util.Properties;
import java.util.*; 
import java.net.URI;
import com.google.common.collect.Maps;
import java.util.Map;
import org.onlab.packet.IpPrefix;
import java.util.HashMap;
import static org.onlab.util.Tools.get;

/**
 * Skeletal ONOS application component.
 */
@Component(immediate = true,
           service = {SomeInterface.class},
           property = {
               "someProperty=Some Default String Value",
           })
public class AppComponent implements SomeInterface {

    private final Logger log = LoggerFactory.getLogger(getClass());

    /** Some configurable property. */
    private String someProperty;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService cfgService;
    ////////////////////////////////////////////////////////////
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;
    
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PacketService packetService;
    
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected HostService hostService;
    
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected TopologyService topologyService;
    
    private PacketProcessor processor = new ReactivePacketProcessor();
    
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowObjectiveService flowObjectiveService;
    
    private ApplicationId appId;
    protected Map<DeviceId, Map<MacAddress, PortNumber>> macTables = Maps.newConcurrentMap();
    
    ///// What need to be processed after Activate /////

    @Activate
    protected void activate() {
        cfgService.registerProperties(getClass());

        // about packetservice:
        // http://api.onosproject.org/2.3.0/apidocs/org/onosproject/net/packet/PacketService.html?msclkid=bf0b93eca9dd11ec96e999d6e0a7f869
        appId = coreService.registerApplication("nthu.wmnet.loadbalance"); // ??????app
        packetService.addProcessor(processor, PacketProcessor.director(2)); // ??????eventhandler
        
        // packetService.requestPackets(DefaultTrafficSelector.builder()
        // .matchEthType(Ethernet.TYPE_IPV4).build(), PacketPriority.REACTIVE, appId);  
        // packetService.requestPackets(DefaultTrafficSelector.builder()
        // .matchEthType(Ethernet.TYPE_ARP).build(), PacketPriority.REACTIVE, appId);

        
        log.info("load balancing app start");

        // ???app????????????install??????flow rules?????????destination ip = fake ip?????????packet in?????????????????????packetService?????????


        //Ip4Address fakeIP = IpAddress.valueOf("172.27.0.114/32").getIp4Address();

        ///// ??????treatment: ?????????????????????packet in???controller /////
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder()
        .setOutput(PortNumber.CONTROLLER);


        ///// ??????selector /////

        // selector 1: ????????????ipv4??????
        TrafficSelector.Builder selectorBuilder_getIPv4 = DefaultTrafficSelector.builder();
        selectorBuilder_getIPv4.matchEthType(Ethernet.TYPE_IPV4)
                        .matchIPDst(IpPrefix.valueOf("10.0.0.6/32"));

        
        // ??????flowrule??????
        ForwardingObjective forwardingObjective_IPPacket = DefaultForwardingObjective.builder() 
        .withSelector(selectorBuilder_getIPv4.build())  // ??????????????????selector
        .withTreatment(treatment.build())
        .withPriority(30)  // ?????????priority???rule????????????????????????
        .withFlag(ForwardingObjective.Flag.VERSATILE)
        .fromApp(appId)
        .makePermanent() //timeout
        .add();

        log.info("Setting Flow rule of get IP packet with Fake IP: Done!");
        
        // ?????????????????????device?????????flow rules
        // ??????????????????sw1??????controller?????????sw1????????????flow rule????????????????????????sw1???destination ip=fake ip???????????????????????????port?????????
        flowObjectiveService.forward(DeviceId.deviceId("of:0000000000000001"), forwardingObjective_IPPacket);
        log.info("Flow rule: getting IP packet with Fake IP installed!");

        // selector 2: ????????????arp?????? (???????????????????????????arp????????????)
        packetService.requestPackets(DefaultTrafficSelector.builder()
        .matchEthType(Ethernet.TYPE_ARP).build(), PacketPriority.REACTIVE, appId);

        // TrafficSelector.Builder selectorBuilder_getARP = DefaultTrafficSelector.builder();
        // selectorBuilder_getARP.matchEthType(Ethernet.TYPE_ARP)
        //                     .matchIPDst(IpPrefix.valueOf("10.0.0.6/32"));
        

        // ForwardingObjective forwardingObjective_ARPPacket = DefaultForwardingObjective.builder()
        // .withSelector(selectorBuilder_getARP.build())  // ??????????????????selector
        // .withTreatment(treatment.build())
        // .withPriority(30)  // ?????????priority???rule????????????????????????
        // .withFlag(ForwardingObjective.Flag.VERSATILE)
        // .fromApp(appId)
        // .makePermanent() //timeout
        // .add();
        
        // log.info("Setting Flow rule of get ARP packet with Fake IP: Done!");
        
        // flowObjectiveService.forward(DeviceId.deviceId("of:0000000000000001"), forwardingObjective_ARPPacket);
         log.info("Flow rule: getting ARP packet installed!");
        
    }
  

    ///// Containing all Process here /////

    private class ReactivePacketProcessor implements PacketProcessor {
        // ????????????????????????????????????process?????????????????????????????????????????????????????????????????????
        @Override
        public void process(PacketContext context) {

            // Stop processing if the packet has been handled, since we
            // can't do any more to it.
            if (context.isHandled()) {
                return;
            }


            // extract the original Ethernet frame from the packet information
            // ????????????????????????????????????????????????inboundpacket???Ethernet
            InboundPacket pkt = context.inPacket();
            Ethernet ethPkt = pkt.parsed();

        

            if (ethPkt == null) {
                return;
            }

            
            // ???????????????controller??????????????????????????????dst ip = fake ip???ipv4????????????dst ip = fake ip???arp??????

            // step 1. ???????????????arp??????, if dst ip = fake ip, then install flow rules, and send ARP reply with fake MAC address
            // ????????????arp reply??????????????????????????????????????????????????????arp???????????????????????????????????????
            if (ethPkt.getEtherType() == Ethernet.TYPE_ARP) {

                log.info("get ARP packet!");

                // ??????arp request packet
                ARP ARPrequest = (ARP) ethPkt.getPayload(); // ??????ethernet packet??????arp payload
                IpAddress dstIP = IpAddress.valueOf(IpAddress.Version.valueOf("INET"), ARPrequest.getTargetProtocolAddress());
                
                // ??????dstip?????????fake ip??????arp request
                if (dstIP.equals(IpAddress.valueOf("10.0.0.6")) && ARPrequest.getOpCode() == ARP.OP_REQUEST) {
                    log.info("get ARP request packet.");

                    // set ARP reply packet, ???????????????????????????????????????request??????????????????????????????????????????Mac address
                    // ?????????request???????????????????????????reply?????????dst address
                    ARP ARPreply = (ARP) ARPrequest.clone();
                    ARPreply.setOpCode(ARP.OP_REPLY);
                    ARPreply.setTargetProtocolAddress(ARPrequest.getSenderProtocolAddress());
                    ARPreply.setTargetHardwareAddress(ARPrequest.getSenderHardwareAddress());
                    ARPreply.setSenderProtocolAddress(ARPrequest.getTargetProtocolAddress());
                    ARPreply.setSenderHardwareAddress(MacAddress.valueOf("1B:AA:4D:E2:F4:B5").toBytes());

                    // ???????????????arp packet?????????ethernet
                    Ethernet ethReply = new Ethernet();
                    ethReply.setSourceMACAddress(MacAddress.valueOf("1B:AA:4D:E2:F4:B5").toBytes());
                    ethReply.setDestinationMACAddress(ethPkt.getSourceMAC());
                    ethReply.setEtherType(Ethernet.TYPE_ARP);
                    ethReply.setPayload(ARPreply);


                    // ??????action: ??????????????????????????????port
                    TrafficTreatment.Builder treatmentbuilder = DefaultTrafficTreatment.builder();
                    treatmentbuilder.setOutput(pkt.receivedFrom().port());

                    // OutboundPacket????????????????????????????????????
                    // ??????OutboundPacket???????????????????????????????????????
                    OutboundPacket outpacket = new DefaultOutboundPacket(pkt.receivedFrom().deviceId(), treatmentbuilder.build(), ByteBuffer.wrap(ethReply.serialize()));
                    
                    // ???arp reply????????????controller
                    packetService.emit(outpacket);

                }
                
                
                log.info("finished arp process");
            } 
            else if (ethPkt.getEtherType() == Ethernet.TYPE_IPV4) {

                // ??????ipv4??????
                IPv4 ipacket = (IPv4) ethPkt.getPayload();

                // ??????????????????fake ip???ipv4?????????install flow rules
                if(ipacket.getDestinationAddress() == Ip4Address.valueOf("10.0.0.6").toInt()){
                    log.info("ipv4 with fakeip packet in");

                    PortNumber InPort = pkt.receivedFrom().port();
                    String inport_number_string = InPort.toString();
                    int inport_number = Integer.parseInt(InPort.toString());

                    MacAddress srcMac = ethPkt.getSourceMAC();
                    HostId srcID = HostId.hostId(srcMac);

                    log.info("packet in from port = " + inport_number_string);
                    log.info("source host ID = " + srcID.toString());

                    if (inport_number%2 == 1){
                        // ?????????h1

                        log.info("assigned to h1.");

                        // ??????????????????port
                        PortNumber port_h1 = PortNumber.fromString("1");
                        HostId dstID = HostId.hostId(MacAddress.valueOf("6A:3D:6F:B1:71:92"));   //????????????mininet??????????????????
                        IpAddress newdstIP = IpAddress.valueOf("10.0.0.1");
                        MacAddress newdesMAC = MacAddress.valueOf("6A:3D:6F:B1:71:92");
                        installRule(context, srcID, dstID, InPort, port_h1, newdstIP, newdesMAC);

                    }
                    else{
                        // ?????????h2

                        log.info("assigned to h2.");

                        PortNumber port_h2 = PortNumber.fromString("2");
                        HostId dstID = HostId.hostId(MacAddress.valueOf("DE:04:39:0B:8A:03"));
                        IpAddress newdstIP = IpAddress.valueOf("10.0.0.2");
                        MacAddress newdesMAC = MacAddress.valueOf("DE:04:39:0B:8A:03");
                        installRule(context, srcID, dstID, InPort, port_h2, newdstIP, newdesMAC);
                    }

                }
                log.info("finished ipv4 process");
            }
            

            return;

        }
    }


    private void installRule(PacketContext context, HostId srcId, HostId dstId,PortNumber inport, PortNumber outport, IpAddress dstIP, MacAddress dstMAC){
    

        log.info("flow rules process started!");

        Ethernet inPkt = context.inPacket().parsed();         // ????????????????????????

        Host dst = hostService.getHost(dstId);  //dst address
        Host src = hostService.getHost(srcId);  //src address
      
        
      // ??????traffic selector
      TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
      
      //????????????src???dst???return
      if(src == null || dst == null){
            return;
      }else{
          

        // ??????????????????sourceMAC??????
        selectorBuilder.matchInPort(inport)
                    .matchEthType(Ethernet.TYPE_IPV4)
                    .matchIPDst(IpPrefix.valueOf("10.0.0.6/32"));
        

        // ????????????????????????

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .setEthDst(dstMAC)
                    .setIpDst(dstIP)
                    .setOutput(outport)
                    .build();


        // ??????Lab????????????flow rule???match field, priority, app id, life time, flag
        ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                    .withSelector(selectorBuilder.build())
                    .withTreatment(treatment)
                    .withPriority(50000)
                    .withFlag(ForwardingObjective.Flag.VERSATILE)
                    .fromApp(appId)
                    .makePermanent() //timeout
                    .add();
        

        log.info("flow rules setting completed!"); 
        // install the forwarding rules onto the specific device
        flowObjectiveService.forward(context.inPacket().receivedFrom().deviceId(), forwardingObjective);


        // ???????????????flow rules
        TrafficSelector.Builder selectorBuilder_reverse = DefaultTrafficSelector.builder();
        selectorBuilder_reverse.matchInPort(outport)
                        //.matchEthType(Ethernet.TYPE_IPV4);
                        .matchEthDst(inPkt.getSourceMAC());
                    //.matchIPDst(IpPrefix.valueOf("10.0.0.6/32"));

        TrafficTreatment treatment_reverse = DefaultTrafficTreatment.builder()
                    //.setIpSrc(IpAddress.valueOf("10.0.0.6"))
                    .setOutput(inport)
                    .build();

        ForwardingObjective forwardingObjective_reverse = DefaultForwardingObjective.builder()
                    .withSelector(selectorBuilder_reverse.build())
                    .withTreatment(treatment_reverse)
                    .withPriority(50000)
                    .withFlag(ForwardingObjective.Flag.VERSATILE)
                    .fromApp(appId)
                    .makePermanent() //timeout
                    .add();

        flowObjectiveService.forward(context.inPacket().receivedFrom().deviceId(), forwardingObjective_reverse);

        log.info("reverse flow rules setting completed!");

        packetOut(context, outport);

      }
    }


    private void flood(PacketContext context) {
        // check if broadcast is allowed for traffic received on the specified connection point.
        // ???????????????topology????????????????????????????????????broadcast???flood
        if (topologyService.isBroadcastPoint(topologyService.currentTopology(),
                                             context.inPacket().receivedFrom())) {
            packetOut(context, PortNumber.FLOOD);
            log.info("packet flooded!");
        } else {
            context.block();
            log.info("packet blocked!");
        }
    }


    // ????????????????????????Port
    private void packetOut(PacketContext context, PortNumber portNumber) {
        context.treatmentBuilder().setOutput(portNumber);
        context.send();
    }

    // ?????????mac table
    private void initMacTable(ConnectPoint cp) {
            macTables.putIfAbsent(cp.deviceId(), Maps.newConcurrentMap());

    }
    
    ///// ???????????????????????????????????????????????? /////

    @Deactivate
    protected void deactivate() {
        cfgService.unregisterProperties(getClass(), false);
        packetService.removeProcessor(processor);
        processor = null;
    }

    @Modified
    public void modified(ComponentContext context) {
        Dictionary<?, ?> properties = context != null ? context.getProperties() : new Properties();
        if (context != null) {
            someProperty = get(properties, "someProperty");
        }
        log.info("Reconfigured");
    }

    @Override
    public void someMethod() {
        log.info("Invoked");
    }

}
