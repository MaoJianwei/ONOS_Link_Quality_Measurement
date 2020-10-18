COMPILE_DEPS = CORE_DEPS + CLI + [
    "//core/common:onos-core-common",
]

osgi_jar_with_tests(
    karaf_command_packages = ["com.maojianwei.link.quality.measurement.cli"],
    deps = COMPILE_DEPS,
)

REQUIRE_APPS = [
]

onos_app(
    app_name = "com.maojianwei.link.quality.measurement",
    category = "Monitoring",
    description = "Mao Link Quality Measurement",
    required_apps = REQUIRE_APPS,
    title = "Mao Link Quality Measurement",
    url = "https://www.maojianwei.com",
)
