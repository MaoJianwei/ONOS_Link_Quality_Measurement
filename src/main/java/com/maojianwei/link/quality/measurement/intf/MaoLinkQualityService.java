package com.maojianwei.link.quality.measurement.intf;

import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;

import java.util.List;
import java.util.Map;

/**
 * Created by Mao Link Quality Measurement on 2020.10.18.
 */
public interface MaoLinkQualityService {

    int getLinkLatency(Link link);
    Map<Link, Integer> getAllLinkLatencies();


    // ========== debug usages ==========
    Map<Link, Integer> getAllInitLatencies();
    Map<DeviceId, Integer> getAllControlLatencies();

    Map<Link, List<Integer>> getDebugLinkLatancies();
}