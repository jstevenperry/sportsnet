/*
 * Copyright © 2021 J. Steven Perry
 * All rights reserved.
 */

package com.jstevenperry.sportsnet.mlb.config;

import com.jstevenperry.sportsnet.framework.config.AbstractSportsnetProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SportsnetProperties extends AbstractSportsnetProperties {

    // MLB properties
    @Value("${sportsnet.mlb.uri}")
    private String sportsnetMlbUri;

    public String getSportsnetMlbUri() {
        return sportsnetMlbUri;
    }
}
