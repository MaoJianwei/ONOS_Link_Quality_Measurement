package com.maojianwei.link.quality.measurement.cli;

import com.maojianwei.link.quality.measurement.intf.MaoLinkQualityService;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;

@Service
@Command(scope = "onos",
        name = "link-latencies",
        description = "Show all latencies of all links.",
        detailedDescription = "Show all latencies of all links.")
public class MaoLinkQuality extends AbstractShellCommand {

    @Override
    protected void doExecute() {
        MaoLinkQualityService service = getService(MaoLinkQualityService.class);
        print("====== Link Latencies ======");
        service.getAllLinkLatencies().forEach((k, v) -> print("%s --- %dms", k.toString(), v));
    }
}
