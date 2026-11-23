/*
 * Copyright © 2021 J. Steven Perry
 * All rights reserved.
 */

package com.jstevenperry.sportsnet.framework.persistence;

import com.jstevenperry.sportsnet.framework.model.SeasonData;

import java.util.Map;

public interface FileProcessor<T extends SeasonData> {

    String getRequiredFilenameEnding();

    int getNumberOfFields();

    default int getNumberOfHeaderLines() {
        return 2;
    }

    T parseAndProcessFileLine(final String[] line, final Map<String, T> teamSeasonDataMap);
}
