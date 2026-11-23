/*
 * Copyright © 2021 J. Steven Perry
 * All rights reserved.
 */

package com.jstevenperry.sportsnet.mlb.config;

import com.jstevenperry.sportsnet.framework.util.AbstractNetworkProperties;
import com.jstevenperry.sportsnet.framework.util.AbstractNetworkUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MlbNetworkProperties extends AbstractNetworkProperties {
    private final SportsnetProperties sportsnetProperties;

    public MlbNetworkProperties(final AbstractNetworkUtils networkUtils,
                                final SportsnetProperties sportsnetProperties) {
        super(networkUtils, sportsnetProperties);
        this.sportsnetProperties = sportsnetProperties;
    }

    @Override
    public String getSport() {
        return StringUtils.remove(sportsnetProperties.getSportsnetMlbUri(), '/');
    }

}
