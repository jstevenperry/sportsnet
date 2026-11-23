package com.jstevenperry.sportsnet.ncaabb.config;

import com.jstevenperry.sportsnet.framework.util.AbstractNetworkProperties;
import com.jstevenperry.sportsnet.framework.util.AbstractNetworkUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NcaaNetworkProperties extends AbstractNetworkProperties {

    private final SportsnetProperties sportsnetProperties;

    public NcaaNetworkProperties(final AbstractNetworkUtils networkUtils,
                                 final SportsnetProperties sportsnetProperties) {
        super(networkUtils, sportsnetProperties);
        this.sportsnetProperties = sportsnetProperties;
    }

    @Override
    public String getSport() {
        return StringUtils.remove(sportsnetProperties.getSportsnetNcaabbUri(), '/');
    }

}
