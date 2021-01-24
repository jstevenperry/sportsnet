package com.jstevenperry.sportsnet.nba.persistence;

import com.jstevenperry.sportsnet.framework.persistence.AbstractTournamentParticipantFlatFileDao;
import com.jstevenperry.sportsnet.nba.model.NbaSeasonData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("nbaTournamentParticipantDao")
public class NbaTournamentParticipantFlatFileDao extends AbstractTournamentParticipantFlatFileDao<NbaSeasonData> {

    private static final Logger log = LoggerFactory.getLogger(NbaTournamentParticipantFlatFileDao.class);

    @Override
    protected String[][] getDataFileNames() {
        return new String[][]{
                {"2010", "/nba/2010-11-Playoff-Participant.csv"},
                {"2011", "/nba/2011-12-Playoff-Participant.csv"},
                {"2012", "/nba/2012-13-Playoff-Participant.csv"},
                {"2013", "/nba/2013-14-Playoff-Participant.csv"},
                {"2014", "/nba/2014-15-Playoff-Participant.csv"},
                {"2015", "/nba/2015-16-Playoff-Participant.csv"},
                {"2016", "/nba/2016-17-Playoff-Participant.csv"},
                {"2017", "/nba/2017-18-Playoff-Participant.csv"},
                {"2018", "/nba/2018-19-Playoff-Participant.csv"},
                {"2019", "/nba/2019-20-Playoff-Participant.csv"},
        };
    }
}
