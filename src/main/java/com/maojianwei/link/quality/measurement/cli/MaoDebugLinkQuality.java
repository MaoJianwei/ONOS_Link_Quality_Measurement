package com.maojianwei.link.quality.measurement.cli;

import com.maojianwei.link.quality.measurement.intf.MaoLinkQualityService;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;

@Service
@Command(scope = "onos",
        name = "link-latencies-debug",
        description = "Debug: Show all latencies of all links.",
        detailedDescription = "Debug: Show all latencies of all links.")
public class MaoDebugLinkQuality extends AbstractShellCommand {

    @Override
    protected void doExecute() {
        MaoLinkQualityService service = getService(MaoLinkQualityService.class);
        print("====== Link Latencies ======");
        service.getAllLinkLatencies().forEach((k, v) -> print("%s --- %d", k.toString(), v));
        print("====== Link Latencies (init) ======");
        service.getAllInitLatencies().forEach((k, v) -> print("%s --- %d", k.toString(), v));
        print("====== Link Latencies (control) ======");
        service.getAllControlLatencies().forEach((k, v) -> print("%s --- %d", k.toString(), v));
        print("====== Link Latencies (record) ======");
        service.getDebugLinkLatancies().forEach((k, v) -> print("%s --- %s", k.toString(), v));
    }
}
