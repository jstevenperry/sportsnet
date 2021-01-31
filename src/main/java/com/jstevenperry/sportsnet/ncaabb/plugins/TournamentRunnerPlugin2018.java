package com.jstevenperry.sportsnet.ncaabb.plugins;

import com.jstevenperry.sportsnet.framework.model.TeamCoordinate;
import com.jstevenperry.sportsnet.framework.persistence.SeasonDataDao;
import com.jstevenperry.sportsnet.ncaabb.model.NcaaBbSeasonData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TournamentRunnerPlugin2018 extends AbstractNcaaBbTournamentRunnerPlugin {

    public static final String NCAABB_TOURNAMENT_TEMPLATE_NCAABB_2018_TXT = "/ncaabb/tournament-template-ncaabb-2018.txt";

    public static final String NCAABB_TOURNAMENT_RESULTS_TEMPLATE_2018 = "/ncaabb/tournament-results-template-2018.csv";

    public TournamentRunnerPlugin2018(final SeasonDataDao<NcaaBbSeasonData> seasonDataDao) {
        super(seasonDataDao);
        setTeamCoordinateSeasonDataMap(buildTeamCoordinateMap());
    }

    @Override
    public Integer getTournamentYear() {
        return 2019;
    }

    @Override
    public String getTournamentTemplateFileName() {
        return NCAABB_TOURNAMENT_TEMPLATE_NCAABB_2018_TXT;
    }

    @Override
    protected String getTournamentResultsTemplateFileName() {
        return NCAABB_TOURNAMENT_RESULTS_TEMPLATE_2018;
    }

    /**
     * Builds the internal map of TeamCoordinate -> SeasonData
     */
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
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 0, 0, "North Dakota St");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 0, 1, "NC Central");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 0, 8, "Arizona St");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 0, 9, "St Johns NY");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 0, 0, "Prairie View");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 0, 1, "Fairleigh Dickinson");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 0, 8, "Belmont");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 0, 9, "Temple");
    }

    // For 2018 Region 0 is the East
    private void createRoundOneMappingsRegion0(final Map<TeamCoordinate, NcaaBbSeasonData> coordinateSeasonDataMap) {
//    [0,1,0],[0,1,1]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 0, "Duke");
        // [0,1,1] is First Four Winner, cannot initialize this TeamCoordinate (it must be computed)
//    [0,1,2],[0,1,3]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 2, "VCU");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 3, "UCF");
//    [0,1,4],[0,1,5]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 4, "Mississippi St");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 5, "Liberty");
//    [0,1,6],[0,1,7]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 6, "Virginia Tech");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 7, "Saint Louis");
//    [0,1,8],[0,1,9]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 8, "Maryland");
        // [0,1,9] is First Four Winner, cannot initialize this TeamCoordinate (it must be computed)
//    [0,1,10],[0,1,11]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 10, "LSU");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 11, "Yale");
//    [0,1,12],[0,1,13]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 12, "Louisville");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 13, "Minnesota");
//    [0,1,14],[0,1,15]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 14, "Michigan St");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 15, "Bradley");
    }

    // For 2018 Region 1 is the West
    private void createRoundOneMappingsRegion1(final Map<TeamCoordinate, NcaaBbSeasonData> coordinateSeasonDataMap) {
//    [1,1,0],[1,1,1]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 0, "Gonzaga");
        // [1,1,1] is First Four Winner, cannot initialize this TeamCoordinate (it must be computed)
//    [1,1,2],[1,1,3]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 2, "Syracuse");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 3, "Baylor");
//    [1,1,4],[1,1,5]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 4, "Marquette");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 5, "Murray St");
//    [1,1,6],[1,1,7]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 6, "Florida St");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 7, "Vermont");
//    [1,1,8],[1,1,9]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 8, "Buffalo");
        // [1,1,9] is First Four Winner, cannot initialize this TeamCoordinate (it must be computed)
//    [1,1,10],[1,1,11]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 10, "Texas Tech");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 11, "Northern Ky");
//    [1,1,12],[1,1,13]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 12, "Nevada");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 13, "Florida");
//    [1,1,14],[1,1,15]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 14, "Michigan");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 15, "Montana");
    }

    // For 2018 Region 2 is the South
    private void createRoundOneMappingsRegion2(final Map<TeamCoordinate, NcaaBbSeasonData> coordinateSeasonDataMap) {
//    [2,1,0],[2,1,1]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 0, "Virginia");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 1, "Gardner-Webb");
//    [2,1,2],[2,1,3]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 2, "Ole Miss");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 3, "Oklahoma");
//    [2,1,4],[2,1,5]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 4, "Wisconsin");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 5, "Oregon");
//    [2,1,6],[2,1,7]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 6, "Kansas St");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 7, "UC Irvine");
//    [2,1,8],[2,1,9]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 8, "Villanova");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 9, "Saint Marys CA");
//    [2,1,10],[2,1,11]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 10, "Purdue");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 11, "Old Dominion");
//    [2,1,12],[2,1,13]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 12, "Cincinnati");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 13, "Iowa");
//    [2,1,14],[2,1,15]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 14, "Tennessee");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 15, "Colgate");
    }

    // For 2019 Region 3 is the Midwest
    private void createRoundOneMappingsRegion3(final Map<TeamCoordinate, NcaaBbSeasonData> coordinateSeasonDataMap) {
//    [3,1,0],[3,1,1]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 0, "North Carolina");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 1, "Iona");
//    [3,1,2],[3,1,3]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 2, "Utah St");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 3, "Washington");
//    [3,1,4],[3,1,5]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 4, "Auburn");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 5, "New Mexico St");
//    [3,1,6],[3,1,7]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 6, "Kansas");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 7, "Northeastern");
//    [3,1,8],[3,1,9]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 8, "Iowa St");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 9, "Ohio St");
//    [3,1,10],[3,1,11]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 10, "Houston");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 11, "Georgia St");
//    [3,1,12],[3,1,13]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 12, "Wofford");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 13, "Seton Hall");
//    [3,1,14],[3,1,15]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 14, "Kentucky");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 15, "Abilene Christian");
    }

    @Override
    public List<Double> seasonDataAsDoubleList(final NcaaBbSeasonData seasonData, final Double resultClass, final boolean isTraining) {
        return NcaaBbSeasonData.asDoubleList(seasonData, resultClass, isTraining);
    }
}
