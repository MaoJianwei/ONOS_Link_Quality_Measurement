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

public final class OsgiPropertyConstants {
    private OsgiPropertyConstants() {}

    public static final String PROBE_INTERVAL = "probeInterval";
    public static final int PROBE_INTERVAL_DEFAULT = 3000; // ms

    public static final String CALCULATE_INTERVAL = "calculateInterval";
    public static final int CALCULATE_INTERVAL_DEFAULT = 3000; // ms

    public static final String LATENCY_AVERAGE_SIZE = "latencyAverageSize";
    public static final int LATENCY_AVERAGE_SIZE_DEFAULT = 5;
}
