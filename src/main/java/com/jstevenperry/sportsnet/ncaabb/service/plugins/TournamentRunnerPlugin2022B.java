package com.jstevenperry.sportsnet.ncaabb.service.plugins;

import com.jstevenperry.sportsnet.framework.model.TeamCoordinate;
import com.jstevenperry.sportsnet.framework.persistence.SeasonDataDao;
import com.jstevenperry.sportsnet.ncaabb.model.NcaaBbSeasonData;

import java.util.HashMap;
import java.util.Map;

public class TournamentRunnerPlugin2022B extends NcaaBbTournamentRunnerPlugin {
    public TournamentRunnerPlugin2022B(SeasonDataDao<NcaaBbSeasonData> seasonDataDao) {
        super(seasonDataDao);
    }

    @Override
    public Integer getTournamentYear() {
        return 2022;
    }

    @Override
    public String getTournamentYearForDisplay() {
        return "2022-23 (Bracket)";
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

    private void createFirstFourMappings(Map<TeamCoordinate, NcaaBbSeasonData> coordinateSeasonDataMap) {
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 0, 0, "Texas A&M-Corpus Christi");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 0, 1, "Southeast Missouri State");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 0, 0, "Texas Southern");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 0, 1, "Fairleigh Dickinson");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 0, 8, "Arizona State");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 0, 9, "Nevada");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 0, 8, "Mississippi State");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 0, 9, "Pittsburgh");
    }

    private void createRoundOneMappingsRegion0(Map<TeamCoordinate, NcaaBbSeasonData> coordinateSeasonDataMap) {
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 0, "Alabama");
        // vs Winner of First four
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 2, "Maryland");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 3, "West Virginia");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 4, "San Diego State");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 5, "College of Charleston");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 6, "Virginia");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 7, "Furman");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 8, "Creighton");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 9, "NC State");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 10, "Baylor");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 11, "UC Santa Barbara");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 12, "Missouri");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 13, "Utah State");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 14, "Arizona");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 15, "Princeton");
    }

    private void createRoundOneMappingsRegion1(Map<TeamCoordinate, NcaaBbSeasonData> coordinateSeasonDataMap) {
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 0, "Purdue");
        // vs Winner of First four
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 2, "Memphis");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 3, "Florida Atlantic");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 4, "Duke");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 5, "Oral Roberts");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 6, "Tennessee");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 7, "Louisiana");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 8, "Kentucky");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 9, "Providence");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 10, "Kansas State");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 11, "Montana State");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 12, "Michigan State");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 13, "Southern California");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 14, "Marquette");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 15, "Vermont");
    }

    private void createRoundOneMappingsRegion2(Map<TeamCoordinate, NcaaBbSeasonData> coordinateSeasonDataMap) {
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 0, "Houston");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 1, "Northern Kentucky");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 2, "Iowa");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 3, "Auburn");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 4, "Miami (FL)");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 5, "Drake");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 6, "Indiana");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 7, "Kent State");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 8, "Iowa State");
        // vs Winner of First four                                           
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 10, "Xavier");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 11, "Kennesaw State");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 12, "Texas A&M");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 13, "Penn State");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 14, "Texas");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 15, "Colgate");
    }

    private void createRoundOneMappingsRegion3(Map<TeamCoordinate, NcaaBbSeasonData> coordinateSeasonDataMap) {
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 0, "Kansas");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 1, "Howard");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 2, "Arkansas");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 3, "Illinois");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 4, "Saint Mary's (CA)");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 5, "Virginia Commonwealth");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 6, "Connecticut");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 7, "Iona");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 8, "Texas Christian");
        // vs Winner of First four
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 10, "Gonzaga");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 11, "Grand Canyon");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 12, "Northwestern");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 13, "Boise State");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 14, "UCLA");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 15, "UNC Asheville");
    }

}
