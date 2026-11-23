/*
 * Copyright © 2021 J. Steven Perry
 * All rights reserved.
 */

package com.jstevenperry.sportsnet.framework.dto;

import com.jstevenperry.sportsnet.framework.model.ResourceType;

public class ResourceFileWrapperDto {

    private ResourceType resourceType;
    private String fileContentsAsString;
    private String[] fileContentsAsStringArray;
    private String[][] fileContentsAsDoubleStringArray;

    public ResourceType getResourceType() {
        return resourceType;
    }

    public ResourceFileWrapperDto setResourceType(final ResourceType resourceType) {
        this.resourceType = resourceType;
        return this;
    }

    public String getFileContentsAsString() {
        return fileContentsAsString;
    }

    public ResourceFileWrapperDto setFileContentsAsString(final String fileContentsAsString) {
        this.fileContentsAsString = fileContentsAsString;
        return this;
    }

    public String[] getFileContentsAsStringArray() {
        return fileContentsAsStringArray;
    }

    public ResourceFileWrapperDto setFileContentsAsStringArray(final String[] fileContentsAsStringArray) {
        this.fileContentsAsStringArray = fileContentsAsStringArray;
        return this;
    }

    public String[][] getFileContentsAsDoubleStringArray() {
        return fileContentsAsDoubleStringArray;
    }

    public ResourceFileWrapperDto setFileContentsAsDoubleStringArray(final String[][] fileContentsAsDoubleStringArray) {
        this.fileContentsAsDoubleStringArray = fileContentsAsDoubleStringArray;
        return this;
    }
}
