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
        service.getAllControlLatencies().forEach((k, v) -> print("%s --- %d", k.toString(), v));
    }
}
