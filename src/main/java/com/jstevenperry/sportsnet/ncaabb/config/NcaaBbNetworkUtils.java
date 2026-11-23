package com.jstevenperry.sportsnet.ncaabb.config;

import com.jstevenperry.sportsnet.framework.util.AbstractNetworkUtils;
import org.springframework.stereotype.Component;

@Component("ncaaBbNetworkUtils")
public class NcaaBbNetworkUtils extends AbstractNetworkUtils {

    public static final String NETWORK_TYPE = "NCAABB";
}
