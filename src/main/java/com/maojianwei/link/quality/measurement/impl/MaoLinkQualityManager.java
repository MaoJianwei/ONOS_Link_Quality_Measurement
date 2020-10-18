/*
 * Copyright 2020-present Open Networking Foundation.
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
package com.maojianwei.link.quality.measurement.impl;

import com.maojianwei.link.quality.measurement.intf.MaoLinkQualityService;
import org.onlab.packet.Data;
import org.onlab.packet.Ethernet;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.maojianwei.link.quality.measurement.impl.OsgiPropertyConstants.*;

@Component(
        immediate = true,
        service = {MaoLinkQualityService.class, },
        property = {
                PROBE_INTERVAL + ":Integer=" + PROBE_INTERVAL_DEFAULT,
                CALCULATE_INTERVAL + ":Integer=" + CALCULATE_INTERVAL_DEFAULT,
                LATENCY_AVERAGE_SIZE + ":Integer=" + LATENCY_AVERAGE_SIZE_DEFAULT,
        }
)
public class MaoLinkQualityManager implements MaoLinkQualityService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String PROBE_SPLITER = ";";
    private static final short PROBE_ETHERTYPE = 0x3366;

    private static final String PROBE_SRC = "20:15:08:10:00:05";
    private static final String PROBE_DST = "FF:FF:FF:FF:FF:FF";


    /** Interval for sending probe. */
    private int probeInterval = PROBE_INTERVAL_DEFAULT;

    /** Interval for calculating latency. */
    private int calculateInterval = CALCULATE_INTERVAL_DEFAULT; // ms

    /** Number of buffered latency records. */
    private int latencyAverageSize = LATENCY_AVERAGE_SIZE_DEFAULT;


    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LinkService linkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowRuleService flowRuleService;


    private MaoProbeLinkQualityTask probeTask;
    private MaoCalculateLinkQualityTask calculateTask;
    private ExecutorService probeWorker;
    private MaoLinkProbeReceiver linkProbeReceiver;
    private ApplicationId appId;

    // hold last 5 records for averages.
    private final Map<Link, List<Integer>> linkLatencies = new ConcurrentHashMap<>();
    private final Map<Link, Integer> initLinklatencies = new ConcurrentHashMap<>();
    private final Map<DeviceId, Integer> controlLinkLatencies = new ConcurrentHashMap<>();

    @Activate
    private void activate(ComponentContext context) {
        log.info("Starting...");
        cfgService.registerProperties(getClass());
        loadConfiguration(context);
        appId = coreService.registerApplication("com.maojianwei.link.quality.measurement");

        linkProbeReceiver = new MaoLinkProbeReceiver();
        packetService.addProcessor(linkProbeReceiver, PacketProcessor.advisor(1));
        requestPushPacket();

        probeTask = new MaoProbeLinkQualityTask();
        calculateTask = new MaoCalculateLinkQualityTask();
        probeWorker = Executors.newCachedThreadPool();
        probeWorker.submit(probeTask);
        probeWorker.submit(calculateTask);
        log.info("Started, {}", appId.id());
    }

    @Deactivate
    private void deactivate() {
        log.info("Stopping...");

        probeTask.requireShutdown();
        calculateTask.requireShutdown();

        probeWorker.shutdown();
        try {
            log.info("waits thread pool to shutdown...");
            probeWorker.awaitTermination(3, TimeUnit.SECONDS);
            log.info("thread pool shutdown ok.");
        } catch (InterruptedException e) {
            e.printStackTrace();
            log.warn("thread pool shutdown timeout.");
        }

        cancelPushPacket();
        packetService.removeProcessor(linkProbeReceiver);
        cfgService.unregisterProperties(getClass(), false);
        log.info("Stopped");
    }

    @Modified
    private void modify(ComponentContext context) {
        loadConfiguration(context);
    }

    private void loadConfiguration(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();

        probeInterval = Tools.getIntegerProperty(properties, PROBE_INTERVAL, PROBE_INTERVAL_DEFAULT);
        log.info("Configured. Probe Interval is configured to {} ms", probeInterval);

        calculateInterval = Tools.getIntegerProperty(properties, CALCULATE_INTERVAL, CALCULATE_INTERVAL_DEFAULT);
        log.info("Configured. Calculate Interval is configured to {} ms", calculateInterval);

        latencyAverageSize = Tools.getIntegerProperty(properties, LATENCY_AVERAGE_SIZE, LATENCY_AVERAGE_SIZE_DEFAULT);
        log.info("Configured. Latency Average Size is configured to {}", latencyAverageSize);
    }


    private void requestPushPacket() {
        TrafficSelector selector = DefaultTrafficSelector.builder().matchEthType(PROBE_ETHERTYPE).build();
        packetService.requestPackets(selector, PacketPriority.HIGH, appId);
    }

    private void cancelPushPacket() {
        TrafficSelector selector = DefaultTrafficSelector.builder().matchEthType(PROBE_ETHERTYPE).build();
        packetService.cancelPackets(selector, PacketPriority.HIGH, appId);
    }


    @Override
    public int getLinkLatency(Link link) {
        int sum = 0;
        List<Integer> latencies = linkLatencies.getOrDefault(link, Collections.EMPTY_LIST);
        for (Integer l : latencies) {
            sum += l;
        }
        return sum / latencyAverageSize;
    }

    @Override
    public Map<Link, Integer> getAllLinkLatencies() {
        Map<Link, Integer> result = new HashMap<>();
        linkLatencies.forEach((link, list) -> {
            int sum = 0;
            for (Integer l : list) {
                sum += l;
            }
            result.put(link, sum / latencyAverageSize);
        });
        return Collections.unmodifiableMap(result);
    }

    @Override
    public Map<Link, Integer> getAllInitLatencies() {
        return Collections.unmodifiableMap(initLinklatencies);
    }

    @Override
    public Map<DeviceId, Integer> getAllControlLatencies() {
        return Collections.unmodifiableMap(controlLinkLatencies);
    }

    @Override
    public Map<Link, List<Integer>> getDebugLinkLatancies() {
        return Collections.unmodifiableMap(linkLatencies);
    }


    private class MaoCalculateLinkQualityTask implements Runnable {

        private boolean toRun = true;

        public void requireShutdown() {
            toRun = false;
        }

        @Override
        public void run() {
            while (toRun) {
                initLinklatencies.forEach((link, latency) -> {
                    latency -= controlLinkLatencies.getOrDefault(link.src().deviceId(), 0) / 2;
                    latency -= controlLinkLatencies.getOrDefault(link.dst().deviceId(), 0) / 2;

                    List<Integer> records;
                    if (!linkLatencies.containsKey(link)) {
                        records = new ArrayList<>();
                        linkLatencies.put(link, records);
                    } else {
                        records = linkLatencies.get(link);
                    }

                    if (records.size() >= latencyAverageSize) {
                        records.remove(0);
                    }
                    records.add(latency < 0 ? 0 : latency);
                });
                try {
                    Thread.sleep(calculateInterval);
                } catch (InterruptedException e) {
                    break;
                }
            }
            log.info("Calculate latency task stopped.");
        }
    }

    private class MaoProbeLinkQualityTask implements Runnable {

        private boolean toRun = true;

        public void requireShutdown() {
            toRun = false;
        }

        @Override
        public void run() {
            while (toRun) {
                for (Device device : deviceService.getAvailableDevices()) {
                    DeviceId deviceId = device.id();

//                    pushFlowrule(deviceId);

                    TrafficTreatment treatmentAll = DefaultTrafficTreatment.builder()
                            .setOutput(PortNumber.ALL).build();
                    TrafficTreatment treatmentController = DefaultTrafficTreatment.builder()
                            .setOutput(PortNumber.CONTROLLER).build();

                    Ethernet probePkt = new Ethernet();
                    probePkt.setDestinationMACAddress(PROBE_DST);
                    probePkt.setSourceMACAddress(PROBE_SRC);
                    probePkt.setEtherType(PROBE_ETHERTYPE);

                    byte[] probeData = (deviceId.toString() + PROBE_SPLITER + System.currentTimeMillis()).getBytes();
                    probePkt.setPayload(new Data(probeData));
                    packetService.emit(new DefaultOutboundPacket(deviceId, treatmentAll,
                            ByteBuffer.wrap(probePkt.serialize())));


                    probeData = (deviceId.toString() + PROBE_SPLITER + System.currentTimeMillis()).getBytes();
                    probePkt.setPayload(new Data(probeData));
                    packetService.emit(new DefaultOutboundPacket(deviceId, treatmentController,
                            ByteBuffer.wrap(probePkt.serialize())));
                }
                try {
                    Thread.sleep(probeInterval);
                } catch (InterruptedException e) {
                    break;
                }
            }
            log.info("Probe latency task stopped.");
        }

        // Mao: useless now
        private void pushFlowrule(DeviceId deviceId) {

            TrafficSelector selector = DefaultTrafficSelector.builder().matchEthType(PROBE_ETHERTYPE).build();

            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .setOutput(PortNumber.ALL)
                    .build();

            FlowRule flowRule = DefaultFlowRule.builder()
                    .withSelector(selector)
                    .withTreatment(treatment)
                    .forDevice(deviceId)
                    .forTable(0)
                    .fromApp(appId)
                    .withPriority(60000)
                    .makePermanent()
                    .build();
            flowRuleService.applyFlowRules(flowRule);
        }
    }

    private class MaoLinkProbeReceiver implements PacketProcessor {

        @Override
        public void process(PacketContext context) {
            long now = System.currentTimeMillis();

            if (context.isHandled()) {
                return;
            }

            Ethernet pkt = context.inPacket().parsed();
            if (pkt.getEtherType() == PROBE_ETHERTYPE) {
                byte[] probePacket = pkt.getPayload().serialize();
                String[] deviceProbe = new String(probePacket).split(PROBE_SPLITER);

                DeviceId probeSrc = DeviceId.deviceId(deviceProbe[0]);
                long before = Long.parseLong(deviceProbe[1]);

                if (context.inPacket().receivedFrom().port().equals(PortNumber.CONTROLLER)) {
                    controlLinkLatencies.put(context.inPacket().receivedFrom().deviceId(), (int) (now - before));

                } else {
                    Set<Link> links = linkService.getIngressLinks(context.inPacket().receivedFrom());
                    if (links.isEmpty()) {
                        log.warn("link is not exist. {}", context.inPacket().receivedFrom());
                        return;
                    }

                    for (Link link : links) { // may >2 in broadcast network.
                        if (link.src().deviceId().equals(probeSrc)) {
                            initLinklatencies.put(link, (int) (now - before));
                            break;
                        }
                    }
                }
                context.block();
            }
        }
    }
}
