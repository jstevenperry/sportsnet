/*
 * Copyright 2015-2021, James Steven Perry. All rights reserved.
 */
package com.jstevenperry.sportsnet;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration class. Saves having to use XML files to configure Spring.
 *
 * @author J Steven Perry
 */
@Configuration
@ComponentScan(basePackages = {"com.jstevenperry.sportsnet"})
public class ApplicationConfig {
    // Nothing to do
}
