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
        appId = coreService.registerApplication("nthu.wmnet.loadbalance"); // 註冊app
        packetService.requestPackets(DefaultTrafficSelector.builder()
        .matchEthType(Ethernet.TYPE_IPV4).build(), PacketPriority.REACTIVE, appId);  
        packetService.requestPackets(DefaultTrafficSelector.builder()
        .matchEthType(Ethernet.TYPE_ARP).build(), PacketPriority.REACTIVE, appId);
      

        log.info("Load Balancing App Started");

        // 在app啟動時先install一個flow rules讓所有destination ip = fake ip的封包packet in，這邊可以透過packetService來完成
        

        Ip4Address fakeIP = IpAddress.valueOf("172.27.0.114").getIp4Address();

        ///// 建立treatment: 將過濾過的封包packet in到controller /////
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder()
        .setOutput(PortNumber.CONTROLLER);


        ///// 建立selector /////

        // selector 1: 設定取得ipv4封包
        TrafficSelector.Builder selectorBuilder_getIPv4 = DefaultTrafficSelector.builder();
        selectorBuilder_getIPv4.matchIPDst(IpPrefix.valueOf("172.27.0.114"))
                        .matchEthType(Ethernet.TYPE_IPV4);

        
        // 宣告flowrule物件
        ForwardingObjective forwardingObjective_IPpacket = DefaultForwardingObjective.builder() 
        .withSelector(selectorBuilder_getIPv4.build())  // 前面設定好的selector
        .withTreatment(treatment.build())
        .withPriority(50000)  // 設定高priority讓rule之後能夠優先執行
        .withFlag(ForwardingObjective.Flag.VERSATILE)
        .fromApp(appId)
        .makeTemporary(20) //timeout
        .add();

        log.info("Setting of IPv4 packet done.");

        // selector 2: 設定取得arp封包
        TrafficSelector.Builder selectorBuilder_getARP = DefaultTrafficSelector.builder();
        selectorBuilder_getARP.matchIPDst(IpPrefix.valueOf("172.27.0.114"))
                        .matchEthType(Ethernet.TYPE_ARP);
             
        ForwardingObjective forwardingObjective_ARPpacket = DefaultForwardingObjective.builder()
        .withSelector(selectorBuilder_getARP.build())  // 前面設定好的selector
        .withTreatment(treatment.build())
        .withPriority(50000)  // 設定高priority讓rule之後能夠優先執行
        .withFlag(ForwardingObjective.Flag.VERSATILE)
        .fromApp(appId)
        .makeTemporary(20) //timeout
        .add();
        
        log.info("Setting of ARP packet done.");

        // 在該封包進來的device上安裝flow rules
        // 假設此封包從sw1進到controller，則在sw1讓安裝該flow rule，讓之後所有經過sw1且destination ip=fake ip的封包都從設定好的port傳出去
        flowObjectiveService.forward(DeviceId.deviceId("of:0000000000000001"), forwardingObjective_IPpacket);
        packetService.addProcessor(processor, PacketProcessor.director(2)); // 註冊eventhandler
        log.info("Load Balancing App Started");

    }
  

    ///// Containing all Process here /////

    private class ReactivePacketProcessor implements PacketProcessor {
        // 當有封包進來時，都會呼叫process函式，因此這部分就包含了要對封包進行的動作設定
        @Override
        public void process(PacketContext context) {

            // Stop processing if the packet has been handled, since we
            // can't do any more to it.
            if (context.isHandled()) {
                return;
            }


            // extract the original Ethernet frame from the packet information
            // 接收到的封包需要一層一層解析，從inboundpacket到Ethernet
            InboundPacket pkt = context.inPacket();
            Ethernet ethPkt = pkt.parsed();
        

            if (ethPkt == null) {
                return;
            }


            // 現在會進到controller中的封包就只有兩種，dst ip = fake ip的ipv4封包或者dst ip = fake ip的arp封包

            // step 1. 檢查是否為arp封包, if dst ip = fake ip, then install flow rules, and send ARP reply with fake MAC address
            // 這邊進行arp reply只是因為這是一個必要的回覆過程，否則arp的要求會一直懸在網路環境中
            if (ethPkt.getEtherType() == Ethernet.TYPE_ARP) {

                // 分析arp request packet
                ARP ARPrequest = (ARP) ethPkt.getPayload(); // 解開ethernet packet取得arp payload
                IpAddress dstIP = IpAddress.valueOf(IpAddress.Version.valueOf("INET"), ARPrequest.getTargetProtocolAddress());
                
                // 確認dstip是送往fake ip且為arp request
                if (dstIP.equals(IpAddress.valueOf("172.27.0.114")) && ARPrequest.getOpCode() == ARP.OP_REQUEST) {

                    // set ARP reply packet, 這邊大部份的資訊可以直接從request封包中取得，重點是要設定假的Mac address
                    // 要注意request封包的來源位置對於reply來說是dst address
                    ARP ARPreply = (ARP) ARPrequest.clone();
                    ARPreply.setOpCode(ARP.OP_REPLY);
                    ARPreply.setTargetProtocolAddress(ARPrequest.getSenderProtocolAddress());
                    ARPreply.setTargetHardwareAddress(ARPrequest.getSenderHardwareAddress());
                    ARPreply.setSenderProtocolAddress(ARPrequest.getTargetProtocolAddress());
                    ARPreply.setSenderHardwareAddress(MacAddress.valueOf("1B:AA:4D:E2:F4:B5").toBytes());

                    // 將設定好的arp packet打包成ethernet
                    Ethernet ethReply = new Ethernet();
                    ethReply.setSourceMACAddress(MacAddress.valueOf("1B:AA:4D:E2:F4:B5").toBytes());
                    ethReply.setDestinationMACAddress(ethPkt.getSourceMAC());
                    ethReply.setEtherType(Ethernet.TYPE_ARP);
                    ethReply.setPayload(ARPreply);

                    // 設定action: 準備輸出，以及輸出的port
                    TrafficTreatment.Builder treatmentbuilder = DefaultTrafficTreatment.builder();
                    treatmentbuilder.setOutput(pkt.receivedFrom().port());

                    // OutboundPacket是能夠送出的最終封包型態
                    // 建立OutboundPacket物件，把需要的東西都設定好
                    OutboundPacket outpacket = new DefaultOutboundPacket(pkt.receivedFrom().deviceId(), treatmentbuilder.build(), ByteBuffer.wrap(ethReply.serialize()));
                    
                    // 把arp reply封包送出controller
                    packetService.emit(outpacket);

                }   
            }
            
            log.info("finished arp process");
            if (ethPkt.getEtherType() == Ethernet.TYPE_IPV4) {
                IPv4 ipacket = (IPv4) ethPkt.getPayload();
                if(ipacket.getDestinationAddress() == Ip4Address.valueOf("172.27.0.114").toInt()){
                    log.info("ipv4 with fakeip packet in");
                }
        
            }

            return;

        }
    }


    private void installRule(PacketContext context, HostId srcId, HostId dstId,PortNumber outport){
    
      Ethernet inPkt = context.inPacket().parsed();         // 分析接收到的封包

      // 宣告traffic selector
      TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();

      Host dst = hostService.getHost(dstId);  //dst address
      Host src = hostService.getHost(srcId);  //src address

      
      //如果沒有src和dst則return
      if(src == null || dst == null){
            return;
      }else{
          

          // 過濾具有對應sourceMAC和DestinationMAC 
          selectorBuilder.matchEthSrc(inPkt.getSourceMAC())
                      .matchEthDst(inPkt.getDestinationMAC());
          
          // 宣告要執行的動作
          TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                      .setOutput(outport)
                      .build();


          
          // 根據Lab要求定義flow rule的match field, priority, app id, life time, flag
          ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                      .withSelector(selectorBuilder.build())
                      .withTreatment(treatment)
                      .withPriority(20)
                      .withFlag(ForwardingObjective.Flag.VERSATILE)
                      .fromApp(appId)
                      .makeTemporary(20) //timeout
                      .add();
          
          // install the forwarding rules onto the specific device
          flowObjectiveService.forward(context.inPacket().receivedFrom().deviceId(), forwardingObjective);

          packetOut(context, outport);
      }
    }


    private void flood(PacketContext context) {
        // check if broadcast is allowed for traffic received on the specified connection point.
        // 檢查目前的topology，接收到的封包，如果允許broadcast則flood
        if (topologyService.isBroadcastPoint(topologyService.currentTopology(),
                                             context.inPacket().receivedFrom())) {
            packetOut(context, PortNumber.FLOOD);
            log.info("packet flooded!");
        } else {
            context.block();
            log.info("packet blocked!");
        }
    }


    // 傳送封包到指定的Port
    private void packetOut(PacketContext context, PortNumber portNumber) {
        context.treatmentBuilder().setOutput(portNumber);
        context.send();
    }

    // 初始化mac table
    private void initMacTable(ConnectPoint cp) {
            macTables.putIfAbsent(cp.deviceId(), Maps.newConcurrentMap());

    }
    
    ///// 這邊以下基本上可以直接套用模組的 /////

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
