package com.jstevenperry.sportsnet.framework.persistence;

import com.jstevenperry.sportsnet.framework.model.AbstractSeasonData;

import java.util.Map;

public interface FileProcessor<T extends AbstractSeasonData> {

    String getRequiredFilenameEnding();

    int getNumberOfFields();

    default int getNumberOfHeaderLines() {
        return 2;
    }

    T parseAndProcessFileLine(final String[] line, final Map<String, T> teamSeasonDataMap);
}
