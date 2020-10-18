package com.maojianwei.link.quality.measurement.impl;

import com.maojianwei.link.quality.measurement.intf.MaoLinkQualityService;
import org.onlab.packet.Data;
import org.onlab.packet.Ethernet;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.*;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.packet.*;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component(immediate = true,
           service = {MaoLinkQualityService.class,})
public class MaoLinkQualityManager implements MaoLinkQualityService {

    private static final Logger logger = LoggerFactory.getLogger(MaoLinkQualityManager.class);

    private static final String PROBE_SPLITER = ";";
    private static final short PROBE_ETHERTYPE = 0x3366;

    private static final String PROBE_SRC = "20:15:08:10:00:05";
    private static final String PROBE_DST = "FF:FF:FF:FF:FF:FF";

    private static final int PROBE_INTERVAL = 3000; // ms
    private static final int CALCULATE_INTERVAL = PROBE_INTERVAL; // ms

    private static final int LATENCY_AVERAGE_SIZE = 5;


    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LinkService linkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowRuleService flowRuleService;


    private MaoProbeLinkQualityTask probeTask;
    private MaoCalculateLinkQualityTask calculateTask;
    private ExecutorService probeWorker;
    private MaoLinkProbeReceiver linkProbeReceiver;
    private ApplicationId appId;

    private Map<Link, Integer> initLinklatencies = new ConcurrentHashMap<>();
    private Map<DeviceId, Integer> controlLinkLatencies = new ConcurrentHashMap<>();
    private Map<Link, List<Integer>> linkLatencies = new ConcurrentHashMap<>(); // hold last 5 records for averages.


    @Activate
    private void activate() {
        logger.info("Starting...");
        appId = coreService.registerApplication("com.maojianwei.link.quality.measurement");

        linkProbeReceiver = new MaoLinkProbeReceiver();
        packetService.addProcessor(linkProbeReceiver, PacketProcessor.advisor(1));
        requestPushPacket();

        probeTask = new MaoProbeLinkQualityTask();
        calculateTask = new MaoCalculateLinkQualityTask();
        probeWorker = Executors.newCachedThreadPool();
        probeWorker.submit(probeTask);
        probeWorker.submit(calculateTask);
        logger.info("Started, {}", appId.id());
    }

    @Deactivate
    private void deactivate() {
        logger.info("Stopping...");

        probeTask.requireShutdown();
        calculateTask.requireShutdown();

        probeWorker.shutdown();
        try {
            logger.info("waits thread pool to shutdown...");
            probeWorker.awaitTermination(3, TimeUnit.SECONDS);
            logger.info("thread pool shutdown ok.");
        } catch (InterruptedException e) {
            e.printStackTrace();
            logger.warn("thread pool shutdown timeout.");
        }

        cancelPushPacket();
        packetService.removeProcessor(linkProbeReceiver);

        logger.info("Stopped");
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
        return sum / LATENCY_AVERAGE_SIZE;
    }

    @Override
    public Map<Link, Integer> getAllLinkLatencies() {
        Map<Link, Integer> result = new HashMap<>();
        linkLatencies.forEach((link, list) -> {
            int sum = 0;
            for(Integer l : list) {
                sum += l;
            }
            result.put(link, sum / LATENCY_AVERAGE_SIZE);
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

                    if (records.size() >= LATENCY_AVERAGE_SIZE) {
                        records.remove(0);
                    }
                    records.add(latency < 0 ? 0 : latency);
                });
                try {
                    Thread.sleep(CALCULATE_INTERVAL);
                } catch (InterruptedException e) {
                    break;
                }
            }
            logger.info("Calculate latency task stopped.");
        }
    }

    private class MaoProbeLinkQualityTask implements Runnable {

        private boolean toRun = true;
        public void requireShutdown() {
            toRun = false;
        }

        @Override
        public void run() {
            int count = 1;
            while (toRun) {
                for(Device device : deviceService.getAvailableDevices()) {
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
                    packetService.emit(new DefaultOutboundPacket(deviceId, treatmentAll, ByteBuffer.wrap(probePkt.serialize())));


                    probeData = (deviceId.toString() + PROBE_SPLITER + System.currentTimeMillis()).getBytes();
                    probePkt.setPayload(new Data(probeData));
                    packetService.emit(new DefaultOutboundPacket(deviceId, treatmentController, ByteBuffer.wrap(probePkt.serialize())));
                }
                try {
                    Thread.sleep(PROBE_INTERVAL);
                } catch (InterruptedException e) {
                    break;
                }
            }
            logger.info("Probe latency task stopped.");
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
                String [] deviceProbe = new String(probePacket).split(PROBE_SPLITER);

                DeviceId probeSrc = DeviceId.deviceId(deviceProbe[0]);
                long before = Long.parseLong(deviceProbe[1]);

                if (context.inPacket().receivedFrom().port().equals(PortNumber.CONTROLLER)) {
                    controlLinkLatencies.put(context.inPacket().receivedFrom().deviceId(), (int)(now - before));

                } else {
                    Set<Link> links = linkService.getIngressLinks(context.inPacket().receivedFrom());
                    if (links.isEmpty()) {
                        logger.warn("link is not exist. {}", context.inPacket().receivedFrom());
                        return;
                    }

                    for (Link link : links) { // may >2 in broadcast network.
                        if (link.src().deviceId().equals(probeSrc)) {
                            initLinklatencies.put(link, (int)(now - before));
                            break;
                        }
                    }
                }
                context.block();
            }
        }
    }
}
