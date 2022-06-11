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
package nctu.winlab.bridge;

import com.google.common.collect.ImmutableSet;
import org.onosproject.cfg.ComponentConfigService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.Properties;

import static org.onlab.util.Tools.get;

import com.google.common.collect.Maps;
import org.onosproject.net.DeviceId;
import org.onlab.packet.MacAddress;
import org.onosproject.net.ConnectPoint;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.InboundPacket;
import org.onlab.packet.Ethernet;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.host.HostService;
import org.onosproject.net.topology.TopologyService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import java.util.Map;

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
    
    /////////////////////////////////////////////////////////////
    @Activate
    protected void activate() {
        cfgService.registerProperties(getClass());

        // about packetservice: http://api.onosproject.org/2.3.0/apidocs/org/onosproject/net/packet/PacketService.html?msclkid=bf0b93eca9dd11ec96e999d6e0a7f869 
        appId = coreService.registerApplication("nctu.winlab.bridge");          // 註冊app

        packetService.addProcessor(processor, PacketProcessor.director(2));     // 註冊eventhandler
        
        
        // 向dataplane取得所需的封包 
        // packetService.requestPackets(TrafficSelector, PacketPriority, ApplicationId)
        // PacketPriority.REACTIVE: low priority for reactive application

        packetService.requestPackets(DefaultTrafficSelector.builder()
            .matchEthType(Ethernet.TYPE_IPV4).build(), PacketPriority.REACTIVE, appId);  
        packetService.requestPackets(DefaultTrafficSelector.builder()
            .matchEthType(Ethernet.TYPE_ARP).build(), PacketPriority.REACTIVE, appId);
        
    }
    //////////////////////////////////////////////////////////////////////////////
    private class ReactivePacketProcessor implements PacketProcessor {
    

    // 當有封包進來時，都會呼叫process函式，因此這部分就包含了要對封包進行的動作設定
    @Override
      public void process(PacketContext context) {
          // Stop processing if the packet has been handled, since we
          // can't do any more to it.
          if (context.isHandled()) {
              return;
          }
          
          // setting mac table
          initMacTable(context.inPacket().receivedFrom());
          
          // extract the original Ethernet frame from the packet information
          InboundPacket pkt = context.inPacket();
          Ethernet ethPkt = pkt.parsed();
          
          
          if (ethPkt == null) {
              return;
          }
          

          // 取得封包資訊
          Map<MacAddress, PortNumber> macportTable = macTables.get(pkt.receivedFrom().deviceId());  // get device id's <macaddress,portnumber> table
          MacAddress srcMac = ethPkt.getSourceMAC();   // get src mac address
          MacAddress dstMac = ethPkt.getDestinationMAC();  // get dst mac address
          
          // 接著需要紀錄srcMac，如果原本的mac table中的src是空的，則將資訊儲存
          if(macportTable.get(srcMac)==null){
            log.info("Add MAC address ==> switch: " + pkt.receivedFrom().deviceId() + ",MAC: "+ srcMac + ", port: " + pkt.receivedFrom().port());
            macportTable.put(srcMac, pkt.receivedFrom().port());
            
           }
            
           
           // 如果dstMac存在，將封包傳送到對應的port，並將flow rule紀錄(install)下來
           if(macportTable.get(dstMac)!=null){
               log.info("MAC "+ dstMac +" is matched on " + pkt.receivedFrom().deviceId() + " Install flow rule!");   
               HostId srcId = HostId.hostId(srcMac);
               HostId dstId = HostId.hostId(dstMac);
               installRule(context, srcId, dstId, macportTable.get(dstMac));  //install flow rules
               return;
           } else {

               // table miss :在mac table中找不到對應dst的卡號
               log.info("MAC "+ dstMac +" is missed on " + pkt.receivedFrom().deviceId() + " Flood packet!");
               flood(context);
        
               return;
           }
            

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
