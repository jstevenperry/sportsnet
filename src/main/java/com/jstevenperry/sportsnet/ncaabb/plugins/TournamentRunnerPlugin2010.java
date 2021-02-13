package com.jstevenperry.sportsnet.ncaabb.plugins;

import com.jstevenperry.sportsnet.framework.model.TeamCoordinate;
import com.jstevenperry.sportsnet.framework.persistence.SeasonDataDao;
import com.jstevenperry.sportsnet.ncaabb.model.NcaaBbSeasonData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TournamentRunnerPlugin2010 extends AbstractNcaaBbTournamentRunnerPlugin {

    public TournamentRunnerPlugin2010(final SeasonDataDao<NcaaBbSeasonData> seasonDataDao) {
        super(seasonDataDao);
        setTeamCoordinateSeasonDataMap(buildTeamCoordinateMap());
    }

    @Override
    public Integer getTournamentYear() {
        return 2010;
    }

    @Override
    public String getTournamentYearForDisplay() {
        return "2010-11";
    }

    @Override
    public String getTournamentTemplateFileName() {
        return "/ncaabb/2010/tournament-template-ncaabb.txt";
    }

    @Override
    protected String getTournamentResultsTemplateFileName() {
        return "/ncaabb/2010/tournament-results-template.csv";
    }

    @Override
    protected Map<TeamCoordinate, NcaaBbSeasonData> buildTeamCoordinateMap() {
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
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 0, 0, "UTSA");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 0, 1, "Alabama St");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 0, 4, "UAB");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 0, 5, "Clemson");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 0, 8, "Southern California");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 0, 9, "VCU");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 0, 0, "UNC Asheville");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 0, 1, "Little Rock");
    }

    private void createRoundOneMappingsRegion0(final Map<TeamCoordinate, NcaaBbSeasonData> coordinateSeasonDataMap) {
//    [0,1,0],[0,1,1]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 0, "Ohio St");
        // [0,1,1] is First Four Winner, cannot initialize this TeamCoordinate (it must be computed)
//    [0,1,2],[0,1,3]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 2, "George Mason");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 3, "Villanova");
//    [0,1,4],[0,1,5]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 4, "West Virginia");
        // [0,1,5] is First Four Winner, cannot initialize this TeamCoordinate (it must be computed)
//    [0,1,6],[0,1,7]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 6, "Kentucky");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 7, "Princeton");
//    [0,1,8],[0,1,9]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 8, "Xavier");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 9, "Marquette");
//    [0,1,10],[0,1,11]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 10, "Syracuse");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 11, "Indiana St");
//    [0,1,12],[0,1,13]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 12, "Washington");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 13, "Georgia");
//    [0,1,14],[0,1,15]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 14, "North Carolina");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 15, "LIU Brooklyn");
    }

    private void createRoundOneMappingsRegion1(final Map<TeamCoordinate, NcaaBbSeasonData> coordinateSeasonDataMap) {
//    [1,1,0],[1,1,1]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 0, "Duke");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 1, "Hampton");
//    [1,1,2],[1,1,3]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 2, "Michigan");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 3, "Tennessee");
//    [1,1,4],[1,1,5]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 4, "Arizona");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 5, "Memphis");
//    [1,1,6],[1,1,7]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 6, "Texas");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 7, "Oakland");
//    [1,1,8],[1,1,9]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 8, "Cincinnati");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 9, "Missouri");
//    [1,1,10],[1,1,11]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 10, "UConn");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 11, "Bucknell");
//    [1,1,12],[1,1,13]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 12, "Temple");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 13, "Penn St");
//    [1,1,14],[1,1,15]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 14, "San Diego St");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 15, "Northern Colo");
    }

    private void createRoundOneMappingsRegion2(final Map<TeamCoordinate, NcaaBbSeasonData> coordinateSeasonDataMap) {
//    [2,1,0],[2,1,1]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 0, "Kansas");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 1, "Boston U");
//    [2,1,2],[2,1,3]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 2, "UNLV");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 3, "Illinois");
//    [2,1,4],[2,1,5]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 4, "Vanderbilt");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 5, "Richmond");
//    [2,1,6],[2,1,7]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 6, "Louisville");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 7, "Morehead St");
//    [2,1,8],[2,1,9]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 8, "Georgetown");
        // [2,1,9] is First Four Winner, cannot initialize this TeamCoordinate (it must be computed)
//    [2,1,10],[2,1,11]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 10, "Purdue");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 11, "Saint Peters");
//    [2,1,12],[2,1,13]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 12, "Texas AM");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 13, "Florida St");
//    [2,1,14],[2,1,15]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 14, "Notre Dame");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 15, "Akron");
    }

    private void createRoundOneMappingsRegion3(final Map<TeamCoordinate, NcaaBbSeasonData> coordinateSeasonDataMap) {
//    [3,1,0],[3,1,1]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 0, "Pittsburgh");
        // [3,1,1] is First Four Winner, cannot initialize this TeamCoordinate (it must be computed)
//    [3,1,2],[3,1,3]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 2, "Butler");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 3, "Old Dominion");
//    [3,1,4],[3,1,5]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 4, "Kansas St");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 5, "Utah St");
//    [3,1,6],[3,1,7]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 6, "Wisconsin");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 7, "Belmont");
//    [3,1,8],[3,1,9]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 8, "St Johns NY");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 9, "Gonzaga");
//    [3,1,10],[3,1,11]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 10, "BYU");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 11, "Wofford");
//    [3,1,12],[3,1,13]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 12, "UCLA");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 13, "Michigan St");
//    [3,1,14],[3,1,15]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 14, "Florida");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 15, "UC Santa Barbara");
    }

    @Override
    public List<Double> seasonDataAsDoubleList(final NcaaBbSeasonData seasonData, final Double resultClass, final boolean isTraining) {
        return NcaaBbSeasonData.asDoubleList(seasonData, resultClass, isTraining);
    }
}
