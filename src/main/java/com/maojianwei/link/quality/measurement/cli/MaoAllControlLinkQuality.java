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
package com.maojianwei.link.quality.measurement.cli;

import com.maojianwei.link.quality.measurement.intf.MaoLinkQualityService;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;

@Service
@Command(scope = "onos",
        name = "link-latencies-control",
        description = "Show all latencies of all control links.",
        detailedDescription = "Show all latencies of all control links.")
public class MaoAllControlLinkQuality extends AbstractShellCommand {

    @Override
    protected void doExecute() {
        MaoLinkQualityService service = getService(MaoLinkQualityService.class);
        print("====== Link Latencies (control) ======");
        service.getAllControlLatencies().forEach((k, v) -> print("%s --- %dms", k.toString(), v));
    }
}
