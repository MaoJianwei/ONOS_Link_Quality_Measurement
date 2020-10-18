# ONOS_Link_Quality_Measurement

An active method to measure link quality: latency(delay) / jitter / packet loss ratio. Performed by ONOS controller.

If you like this project, please click **Star** at the top-right corner, thanks for your support :)

Discuss on ONOS mailing list:
[Is there any app/module/manager that can detect the quality of network links?](https://groups.google.com/a/onosproject.org/g/onos-discuss/c/bMmLeMW7UyQ/m/M3gBxywwBQAJ) 

.

## Update Note

**2020.10.18** Support link delay measurement, based on ONOS **2.5.0-SNAPSHOT** version, backed by Apache Karaf **4.2.9** & JDK **11**.

.

## Demo
![demo-topo](https://raw.githubusercontent.com/MaoJianwei/ONOS_Link_Quality_Measurement/master/docs/demo-topo.jpg)
![link-latency-show](https://raw.githubusercontent.com/MaoJianwei/ONOS_Link_Quality_Measurement/master/docs/link-latency-show.jpg)
```
sudo tc qdisc add dev s1-eth2 root netem delay 100ms
sudo tc qdisc add dev s2-eth3 root netem delay 300ms
```
.

## Latest Instruction to Compile

1. Embed me with ONOS codebase
2. Modify the **$ONOS_ROOT/tools/build/bazel/modules.bzl** file, refer to **modules.bzl__available_example** file
3. Build whole ONOS by bazel.
   (You can use my utility script for ONOS: https://github.com/MaoJianwei/SDN_Scripts/blob/master/ONOS/autoONOS_Bazel.sh)


**Out-of-date:**
ONOS does not support to be built and imported to IDE(Intellij IDEA) by Maven(pom.xml) anymore, we should use Bazel(BUILD).

.

## Backward Compatibility

You can find all milestone versions at [Release](https://github.com/MaoJianwei/ONOS_Link_Quality_Measurement/releases) page.

.

## Community Support

Long term support(LTS) from 2020.10.18, by:

:) [Jianwei Mao @ BUPT FNLab](https://www.maojianwei.com/) - ONOS China Ambassador - MaoJianwei2012@126.com / MaoJianwei2020@gmail.com
