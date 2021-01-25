/*
 * Copyright 2017 Makoto Consulting Group, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jstevenperry.sportsnet.framework.persistence;

import com.jstevenperry.sportsnet.framework.model.AbstractSeasonData;
import com.jstevenperry.sportsnet.ncaabb.model.NcaaBbSeasonData;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Data Access Object interface for SeasonData model data.
 *
 * @author J Steven Perry
 */
@Component
public interface SeasonDataDao<T extends AbstractSeasonData> {

    /**
     * Fetch all {@link NcaaBbSeasonData} objects by the specified <code>year</code>
     * and return a <code>List<SeasonData></code>. The <code>List</code>
     * will be empty if no {@link NcaaBbSeasonData} objects could be found for
     * the specified year (which probably means you forgot to load the
     * data into the DB, shame, shame, shame).
     *
     * @param year The year for which {@link NcaaBbSeasonData} objects are to be
     *             fetched.
     * @return <code>List<SeasonData></code> - the <code>List</code> of
     * {@link NcaaBbSeasonData} objects if they could be located, or an empty list
     * if not.
     */
    List<T> fetchAllByYear(Integer year);

    /**
     * Fetch the {@link NcaaBbSeasonData} object associated with the specified
     * <code>year</code> and <code>teamName</code>. This combination is
     * guaranteed to be unique in the system.
     *
     * @param year     The year for which the {@link NcaaBbSeasonData} object is to be
     *                 fetched.
     * @param teamName The team name for which the {@link NcaaBbSeasonData} object
     *                 is to be fetched.
     * @return {@link NcaaBbSeasonData} - The {@link NcaaBbSeasonData} object associated
     * with the unique combination of <code>year</code> and <code>teamName</code>
     * or <code>null</code> if no such object could be found.
     */
    T fetchByYearAndTeamName(Integer year, String teamName);

}
