package com.jstevenperry.sportsnet.resource.config;

import com.jstevenperry.sportsnet.framework.config.AbstractSportsnetProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan("com.jstevenperry.sportsnet")
public class SportsnetProperties extends AbstractSportsnetProperties {

    @Value("${sportsnet.resource-service.base-directory}")
    private String baseDirectory;

    @Value("${sportsnet.resource-service.network-directory-name}")
    private String networkDirectoryName;

    @Value("${sportsnet.resource-service.simulation-directory-name}")
    private String simulationDirectoryName;

    @Value("${sportsnet.resource-service.network-index-directory-name}")
    private String networkIndexDirectoryName;

    public String getBaseDirectory() {
        return baseDirectory;
    }

    public String getNetworkDirectoryName() {
        return networkDirectoryName;
    }

    public String getSimulationDirectoryName() {
        return simulationDirectoryName;
    }

    public String getNetworkIndexDirectoryName() {
        return networkIndexDirectoryName;
    }
}
