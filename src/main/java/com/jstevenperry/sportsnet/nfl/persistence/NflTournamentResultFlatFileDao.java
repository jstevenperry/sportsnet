package com.jstevenperry.sportsnet.nfl.persistence;

import com.jstevenperry.sportsnet.framework.model.TournamentResult;
import com.jstevenperry.sportsnet.framework.persistence.AbstractTournamentResultFlatFileDao;
import com.jstevenperry.sportsnet.nfl.model.NflSeasonData;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component("nflTournamentResultFlatFileDao")
public class NflTournamentResultFlatFileDao extends AbstractTournamentResultFlatFileDao<NflSeasonData> {

    @Override
    protected int getNumberOfFields() {
        return 9;
    }

    @Override
    protected String[][] getDataFileNames() {
        return new String[][]{
                {"2006", "/nfl/2006/2006-2007-tournament_result.csv"},
                {"2007", "/nfl/2007/2007-2008-tournament_result.csv"},
                {"2008", "/nfl/2008/2008-2009-tournament_result.csv"},
                {"2009", "/nfl/2009/2009-2010-tournament_result.csv"},
                {"2010", "/nfl/2010/2010-2011-tournament_result.csv"},
                {"2011", "/nfl/2011/2011-2012-tournament_result.csv"},
                {"2012", "/nfl/2012/2012-2013-tournament_result.csv"},
                {"2013", "/nfl/2013/2013-2014-tournament_result.csv"},
                {"2014", "/nfl/2014/2014-2015-tournament_result.csv"},
                {"2015", "/nfl/2015/2015-2016-tournament_result.csv"},
                {"2016", "/nfl/2016/2016-2017-tournament_result.csv"},
                {"2017", "/nfl/2017/2017-2018-tournament_result.csv"},
                {"2018", "/nfl/2018/2018-2019-tournament_result.csv"},
                {"2019", "/nfl/2019/2019-2020-tournament_result.csv"},
        };
    }

    @Override
    protected TournamentResult parseLine(final Integer year, final String[] line) {
        TournamentResult ret = new TournamentResult();
        ret.setYear(year);
        ret.setWinningTeamName(StringUtils.remove(StringUtils.trim(line[3]), "*."));
        ret.setWinningScore(Integer.parseInt(line[7]));
        ret.setLosingTeamName(StringUtils.remove(StringUtils.remove(StringUtils.trim(line[5]), '*'), '.'));
        ret.setLosingScore(Integer.parseInt(line[8]));
        return ret;
    }

}
