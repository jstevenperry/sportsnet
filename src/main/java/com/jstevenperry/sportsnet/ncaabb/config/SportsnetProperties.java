package com.jstevenperry.sportsnet.ncaabb.config;

import com.jstevenperry.sportsnet.framework.config.AbstractSportsnetProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SportsnetProperties extends AbstractSportsnetProperties {
    // NCAABB Properties
    @Value("${sportsnet.ncaabb.uri}")
    private String sportsnetNcaabbUri;

    public String getSportsnetNcaabbUri() {
        return sportsnetNcaabbUri;
    }
}
