/*
 * Copyright 2015-2021, James Steven Perry. All rights reserved.
 */

package com.jstevenperry.sportsnet.ncaabb.service.plugins;

import com.jstevenperry.sportsnet.framework.model.TeamCoordinate;
import com.jstevenperry.sportsnet.framework.persistence.SeasonDataDao;
import com.jstevenperry.sportsnet.ncaabb.model.NcaaBbSeasonData;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.HashMap;
import java.util.Map;

public class TournamentRunnerPlugin2020 extends NcaaBbTournamentRunnerPlugin {

    public TournamentRunnerPlugin2020(@Qualifier("ncaaBbSeasonDataDao") final SeasonDataDao<NcaaBbSeasonData> seasonDataDao) {
        super(seasonDataDao);
    }

    @Override
    public Integer getTournamentYear() {
        return 2020;
    }

    @Override
    public String getTournamentYearForDisplay() {
        return "2020-21";
    }

    @Override
    public String getTournamentTemplateFileName() {
        return "tournament-template-ncaabb.txt";
    }

    @Override
    public String getTournamentResultsTemplateFileName() {
        return "tournament-results-template.csv";
    }

    @Override
    public Map<TeamCoordinate, NcaaBbSeasonData> buildTeamCoordinateSeasonDataMap() {
        // Map a team to its initial team coordinates
        Map<TeamCoordinate, NcaaBbSeasonData> coordinateSeasonDataMap = new HashMap<>();
        // First Four
        createFirstFourMappings(coordinateSeasonDataMap);
        // Round of 64 (Round 1)
        createRoundOneMappingsRegion0(coordinateSeasonDataMap);
        createRoundOneMappingsRegion1(coordinateSeasonDataMap);
        createRoundOneMappingsRegion2(coordinateSeasonDataMap);
        createRoundOneMappingsRegion3(coordinateSeasonDataMap);
        return coordinateSeasonDataMap;
    }

    private void createFirstFourMappings(final Map<TeamCoordinate, NcaaBbSeasonData> coordinateSeasonDataMap) {
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 0, 0, "Norfolk St");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 0, 1, "Appalachian State");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 0, 8, "Wichita St");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 0, 9, "Drake");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 0, 0, "Mt St Marys");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 0, 1, "Texas Southern");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 0, 8, "Michigan St");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 0, 9, "UCLA");
    }

    // For 2018-19 Region 0 (Top Left)
    private void createRoundOneMappingsRegion0(final Map<TeamCoordinate, NcaaBbSeasonData> coordinateSeasonDataMap) {
//    [0,1,0],[0,1,1]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 0, "Gonzaga");
        // [0,1,1] is First Four Winner, cannot initialize this TeamCoordinate (it must be computed)
//    [0,1,2],[0,1,3]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 2, "Oklahoma");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 3, "Missouri");
//    [0,1,4],[0,1,5]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 4, "Creighton");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 5, "UC Santa Barbara");
//    [0,1,6],[0,1,7]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 6, "Virginia");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 7, "Ohio");
//    [0,1,8],[0,1,9]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 8, "Southern California");
        // [0,1,9] is First Four Winner, cannot initialize this TeamCoordinate (it must be computed)
//    [0,1,10],[0,1,11]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 10, "Kansas");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 11, "Eastern Wash");
//    [0,1,12],[0,1,13]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 12, "Oregon");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 13, "VCU");
//    [0,1,14],[0,1,15]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 14, "Iowa");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 15, "Grand Canyon");
    }

    // For 2018-19 Region 1 (Bottom Left)
    private void createRoundOneMappingsRegion1(final Map<TeamCoordinate, NcaaBbSeasonData> coordinateSeasonDataMap) {
//    [1,1,0],[1,1,1]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 0, "Michigan");
        // [1,1,1] is First Four Winner, cannot initialize this TeamCoordinate (it must be computed)
//    [1,1,2],[1,1,3]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 2, "LSU");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 3, "St Bonaventure");
//    [1,1,4],[1,1,5]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 4, "Colorado");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 5, "Georgetown");
//    [1,1,6],[1,1,7]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 6, "Florida St");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 7, "UNC Greensboro");
//    [1,1,8],[1,1,9]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 8, "BYU");
        // [1,1,9] is First Four Winner, cannot initialize this TeamCoordinate (it must be computed)
//    [1,1,10],[1,1,11]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 10, "Texas");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 11, "Abilene Christian");
//    [1,1,12],[1,1,13]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 12, "UConn");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 13, "Maryland");
//    [1,1,14],[1,1,15]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 14, "Alabama");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 15, "Iona");
    }

    // For 2018-19 Region 2 (Top Right)
    private void createRoundOneMappingsRegion2(final Map<TeamCoordinate, NcaaBbSeasonData> coordinateSeasonDataMap) {
//    [2,1,0],[2,1,1]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 0, "Baylor");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 1, "Hartford");
//    [2,1,2],[2,1,3]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 2, "North Carolina");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 3, "Wisconsin");
//    [2,1,4],[2,1,5]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 4, "Villanova");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 5, "Winthrop");
//    [2,1,6],[2,1,7]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 6, "Purdue");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 7, "North Texas");
//    [2,1,8],[2,1,9]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 8, "Texas Tech");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 9, "Utah St");
//    [2,1,10],[2,1,11]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 10, "Arkansas");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 11, "Colgate");
//    [2,1,12],[2,1,13]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 12, "Florida");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 13, "Virginia Tech");
//    [2,1,14],[2,1,15]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 14, "Ohio St");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 15, "Oral Roberts");
    }

    // For 2018-19 Region 3 (Bottom Right)
    private void createRoundOneMappingsRegion3(final Map<TeamCoordinate, NcaaBbSeasonData> coordinateSeasonDataMap) {
//    [3,1,0],[3,1,1]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 0, "Illinois");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 1, "Drexel");
//    [3,1,2],[3,1,3]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 2, "Loyola Chicago");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 3, "Georgia Tech");
//    [3,1,4],[3,1,5]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 4, "Tennessee");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 5, "Oregon St");
//    [3,1,6],[3,1,7]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 6, "Oklahoma St");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 7, "Liberty");
//    [3,1,8],[3,1,9]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 8, "San Diego St");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 9, "Syracuse");
//    [3,1,10],[3,1,11]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 10, "West Virginia");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 11, "Morehead St");
//    [3,1,12],[3,1,13]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 12, "Clemson");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 13, "Rutgers");
//    [3,1,14],[3,1,15]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 14, "Houston");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 15, "Cleveland St");
    }
}
