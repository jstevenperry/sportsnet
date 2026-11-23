/*
 * Copyright © 2021 J. Steven Perry
 * All rights reserved.
 */
package com.jstevenperry.sportsnet.ncaabb;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Spring configuration class. Saves having to use XML files to configure Spring.
 *
 * @author J Steven Perry
 */
@EnableAutoConfiguration
@ComponentScan(basePackages = {
        "com.jstevenperry.sportsnet.framework",
        "com.jstevenperry.sportsnet.ncaabb",
        "com.jstevenperry.sportsnet.resource"
})
public class ApplicationConfig {
    // Nothing to do
}
