/*
 * Copyright © 2021 J. Steven Perry
 * All rights reserved.
 */

package com.jstevenperry.sportsnet.mlb.config;

import com.jstevenperry.sportsnet.framework.util.AbstractNetworkUtils;
import org.springframework.stereotype.Component;

@Component("mlbNetworkUtils")
public class MlbNetworkUtils extends AbstractNetworkUtils {

    public static final String NETWORK_TYPE = "mlb";

}
