///*
// * Copyright 2016-present Open Networking Laboratory
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//package com.maojianwei.link.quality.measurement.rest;
//
//import com.fasterxml.jackson.databind.node.ArrayNode;
//import com.fasterxml.jackson.databind.node.ObjectNode;
//import org.onosproject.net.device.DeviceService;
//import org.onosproject.net.link.LinkService;
//import org.onosproject.net.statistic.PortStatisticsService;
//import org.onosproject.rest.AbstractWebResource;
//
//import javax.ws.rs.GET;
//import javax.ws.rs.Path;
//import javax.ws.rs.Produces;
//import javax.ws.rs.core.MediaType;
//import javax.ws.rs.core.Response;
//
///**
// * Mao Test Label.
// *
// * ONOS REST API Docs
// * http://127.0.0.1:8181/onos/v1/docs/
// */
//@Path("Mao_Link_Quality_Measurement")
//public class MaoRestLatency extends AbstractWebResource {
//
//    //    private MaoRoutingService maoRoutingService = get(MaoRoutingService.class);
//    private LinkService linkService = get(LinkService.class);
//    private PortStatisticsService portStatisticsService = get(PortStatisticsService.class);
//    private DeviceService deviceService = get(DeviceService.class);
//
//    /**
//     * Hello world.
//     * Mao.
//     *
//     * REST API:
//     * http://127.0.0.1:8181/onos/mao/hello
//     *
//     * @return Beijing
//     */
//    @GET
//    @Path("hello")
//    @Produces(MediaType.APPLICATION_JSON)
//    public Response hello() {
//        ObjectNode root = mapper().createObjectNode();
//        root.put("Hello", 1080)
//                .put("Mao", 7181);
//
//        ArrayNode array = root.putArray("RadioStation");
//        array.add("192.168.1.1").add("127.0.0.1").add("10.3.8.211");
//
//        return ok(root.toString()).build();
//    }
//}
