package com.maojianwei.link.quality.measurement.cli;

import com.maojianwei.link.quality.measurement.intf.MaoLinkQualityService;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;

@Service
@Command(scope = "onos",
        name = "link-latencies-init",
        description = "Show all init latencies of all links.",
        detailedDescription = "Show all init latencies of all links.")
public class MaoAllInitLinkQuality extends AbstractShellCommand {

    @Override
    protected void doExecute() {
        MaoLinkQualityService service = getService(MaoLinkQualityService.class);
        print("====== Link Latencies (init) ======");
        service.getAllInitLatencies().forEach((k, v) -> print("%s --- %d", k.toString(), v));
    }
}
