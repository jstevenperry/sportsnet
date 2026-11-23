/*
 * Copyright 2015-2021, James Steven Perry. All rights reserved.
 */

package com.jstevenperry.sportsnet.mlb.model;

import com.jstevenperry.sportsnet.framework.model.SeasonData;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("UnusedReturnValue")
public class MlbSeasonData extends SeasonData {
    public static List<Double> asDoubleList(final MlbSeasonData seasonData, final Double resultClass, final Boolean isTraining) {
        List<Double> ret = new ArrayList<>();

        ret.add(asDouble(seasonData.getNumBat()));
        ret.add(asDouble(seasonData.getBatAge()));
        ret.add(asDouble(seasonData.getrPerG()));
        ret.add(asDouble(seasonData.getGamesBat()));
        ret.add(asDouble(seasonData.getPlateAppear()));
        ret.add(asDouble(seasonData.getAtBat()));
        ret.add(asDouble(seasonData.getRuns()));
        ret.add(asDouble(seasonData.getHits()));
        ret.add(asDouble(seasonData.getDoubles()));
        ret.add(asDouble(seasonData.getTriples()));
        ret.add(asDouble(seasonData.getHr()));
        ret.add(asDouble(seasonData.getRbis()));
        ret.add(asDouble(seasonData.getSb()));
        ret.add(asDouble(seasonData.getCaughtStealing()));
        ret.add(asDouble(seasonData.getBb()));
        ret.add(asDouble(seasonData.getSo()));
        ret.add(asDouble(seasonData.getBa()));
        ret.add(asDouble(seasonData.getObp()));
        ret.add(asDouble(seasonData.getSlg()));
        ret.add(asDouble(seasonData.getOps()));
        ret.add(asDouble(seasonData.getOps_plus()));
        ret.add(asDouble(seasonData.getTb()));
        ret.add(asDouble(seasonData.getGdp()));
        ret.add(asDouble(seasonData.getHbp()));
        ret.add(asDouble(seasonData.getSh()));
        ret.add(asDouble(seasonData.getSf()));
        ret.add(asDouble(seasonData.getIbb()));
        ret.add(asDouble(seasonData.getLob()));

        ret.add(asDouble(seasonData.getNumPitchers()));
        ret.add(asDouble(seasonData.getpAge()));
        ret.add(asDouble(seasonData.getRallPerG()));
        ret.add(asDouble(seasonData.getWin()));
        ret.add(asDouble(seasonData.getLoss()));
        ret.add(asDouble(seasonData.getWlpct()));
        ret.add(asDouble(seasonData.getEra()));
        ret.add(asDouble(seasonData.getGamesPitch()));
        ret.add(asDouble(seasonData.getGs()));
        ret.add(asDouble(seasonData.getGf()));
        ret.add(asDouble(seasonData.getCg()));
        ret.add(asDouble(seasonData.gettSho()));
        ret.add(asDouble(seasonData.getcSho()));
        ret.add(asDouble(seasonData.getSv()));
        ret.add(asDouble(seasonData.getIp()));
        ret.add(asDouble(seasonData.getHa()));
        ret.add(asDouble(seasonData.getRa()));
        ret.add(asDouble(seasonData.getEr()));
        ret.add(asDouble(seasonData.getHra()));
        ret.add(asDouble(seasonData.getWalks()));
        ret.add(asDouble(seasonData.getIntWalks()));
        ret.add(asDouble(seasonData.getSoP()));
        ret.add(asDouble(seasonData.getHbpP()));
        ret.add(asDouble(seasonData.getBk()));
        ret.add(asDouble(seasonData.getWp()));
        ret.add(asDouble(seasonData.getBf()));
        ret.add(asDouble(seasonData.getEra_plus()));
        ret.add(asDouble(seasonData.getFip()));
        ret.add(asDouble(seasonData.getWhip()));
        ret.add(asDouble(seasonData.getH9()));
        ret.add(asDouble(seasonData.getHr9()));
        ret.add(asDouble(seasonData.getBb9()));
        ret.add(asDouble(seasonData.getSo9()));
        ret.add(asDouble(seasonData.getSoPerW()));
        ret.add(asDouble(seasonData.getLobP()));

        ret.add(asDouble(seasonData.getNumFld()));
        ret.add(asDouble(seasonData.getRapg()));
        ret.add(asDouble(seasonData.getDefEff()));
        ret.add(asDouble(seasonData.getInn()));
        ret.add(asDouble(seasonData.getCh()));
        ret.add(asDouble(seasonData.getPo()));
        ret.add(asDouble(seasonData.getAssist()));
        ret.add(asDouble(seasonData.getErrors()));
        ret.add(asDouble(seasonData.getDp()));
        ret.add(asDouble(seasonData.getFldPct()));
        ret.add(asDouble(seasonData.getrTot()));
        ret.add(asDouble(seasonData.getrTotYr()));
        ret.add(asDouble(seasonData.getRdrs()));
        ret.add(asDouble(seasonData.getRdrsYr()));
        ret.add(asDouble(seasonData.getRgood()));

        if (isTraining) {
            ret.add(resultClass);
        }
        return ret;
    }

    // Batting
    int numBat;
    double batAge;
    double rPerG;
    int gamesBat;
    int plateAppear;
    int atBat;
    int runs;
    int hits;
    int doubles;
    int triples;
    int hr;
    int rbis;
    int sb;
    int caughtStealing;
    int bb;
    int so;
    double ba;
    double obp;
    double slg;
    double ops;
    double ops_plus;
    int tb;
    int gdp;
    int hbp;
    int sh;
    int sf;
    int ibb;
    int lob;

    // Pitching
    int numPitchers;
    double pAge;
    double rallPerG;
    int win;
    int loss;
    double wlpct;
    double era;
    int gamesPitch;
    int gs;
    int gf;
    int cg;
    int tSho;
    int cSho;
    int sv;
    double ip;
    int ha;
    int ra;
    int er;
    int hra;
    int walks;
    int intWalks;
    int soP;
    int hbpP;
    int bk;
    int wp;
    int bf;
    int era_plus;
    double fip;
    double whip;
    double h9;
    double hr9;
    double bb9;
    double so9;
    double soPerW;
    int lobP;

    // Fielding
    int numFld;
    double rapg;
    double defEff;
    double inn;
    int ch;
    int po;
    int assist;
    int errors;
    int dp;
    double fldPct;
    double rTot;
    double rTotYr;
    double rdrs;
    double rdrsYr;
    double rgood;

    @Override
    public int getNumberOfFields() {
        return MlbSeasonData.asDoubleList(this, 0.0, false).size();
    }

    @Override
    public String toString() {
        return "MlbSeasonData{" +
                "numBat=" + numBat +
                ", batAge=" + batAge +
                ", rPerG=" + rPerG +
                ", gamesBat=" + gamesBat +
                ", plateAppear=" + plateAppear +
                ", atBat=" + atBat +
                ", runs=" + runs +
                ", hits=" + hits +
                ", doubles=" + doubles +
                ", triples=" + triples +
                ", hr=" + hr +
                ", rbis=" + rbis +
                ", sb=" + sb +
                ", caughtStealing=" + caughtStealing +
                ", bb=" + bb +
                ", so=" + so +
                ", ba=" + ba +
                ", obp=" + obp +
                ", slg=" + slg +
                ", ops=" + ops +
                ", ops_plus=" + ops_plus +
                ", tb=" + tb +
                ", gdp=" + gdp +
                ", hbp=" + hbp +
                ", sh=" + sh +
                ", sf=" + sf +
                ", ibb=" + ibb +
                ", lob=" + lob +
                ", numPitchers=" + numPitchers +
                ", pAge=" + pAge +
                ", rallPerG=" + rallPerG +
                ", win=" + win +
                ", loss=" + loss +
                ", wlpct=" + wlpct +
                ", era=" + era +
                ", gamesPitch=" + gamesPitch +
                ", gs=" + gs +
                ", gf=" + gf +
                ", cg=" + cg +
                ", tSho=" + tSho +
                ", cSho=" + cSho +
                ", sv=" + sv +
                ", ip=" + ip +
                ", ha=" + ha +
                ", ra=" + ra +
                ", er=" + er +
                ", hra=" + hra +
                ", walks=" + walks +
                ", intWalks=" + intWalks +
                ", soP=" + soP +
                ", hbpP=" + hbpP +
                ", bk=" + bk +
                ", wp=" + wp +
                ", bf=" + bf +
                ", era_plus=" + era_plus +
                ", fip=" + fip +
                ", whip=" + whip +
                ", h9=" + h9 +
                ", hr9=" + hr9 +
                ", bb9=" + bb9 +
                ", so9=" + so9 +
                ", soPerW=" + soPerW +
                ", lobP=" + lobP +
                ", numFld=" + numFld +
                ", rapg=" + rapg +
                ", defEff=" + defEff +
                ", inn=" + inn +
                ", ch=" + ch +
                ", po=" + po +
                ", assist=" + assist +
                ", errors=" + errors +
                ", dp=" + dp +
                ", fldPct=" + fldPct +
                ", rTot=" + rTot +
                ", rTotYr=" + rTotYr +
                ", rdrs=" + rdrs +
                ", rdrsYr=" + rdrsYr +
                ", rgood=" + rgood +
                '}';
    }

    public int getNumBat() {
        return numBat;
    }

    public MlbSeasonData setNumBat(final int numBat) {
        this.numBat = numBat;
        return this;
    }

    public double getBatAge() {
        return batAge;
    }

    public MlbSeasonData setBatAge(final double batAge) {
        this.batAge = batAge;
        return this;
    }

    public double getrPerG() {
        return rPerG;
    }

    public MlbSeasonData setrPerG(final double rPerG) {
        this.rPerG = rPerG;
        return this;
    }

    public int getGamesBat() {
        return gamesBat;
    }

    public MlbSeasonData setGamesBat(final int gamesBat) {
        this.gamesBat = gamesBat;
        return this;
    }

    public int getPlateAppear() {
        return plateAppear;
    }

    public MlbSeasonData setPlateAppear(final int plateAppear) {
        this.plateAppear = plateAppear;
        return this;
    }

    public int getAtBat() {
        return atBat;
    }

    public MlbSeasonData setAtBat(final int atBat) {
        this.atBat = atBat;
        return this;
    }

    public int getRuns() {
        return runs;
    }

    public MlbSeasonData setRuns(final int runs) {
        this.runs = runs;
        return this;
    }

    public int getHits() {
        return hits;
    }

    public MlbSeasonData setHits(final int hits) {
        this.hits = hits;
        return this;
    }

    public int getDoubles() {
        return doubles;
    }

    public MlbSeasonData setDoubles(final int doubles) {
        this.doubles = doubles;
        return this;
    }

    public int getTriples() {
        return triples;
    }

    public MlbSeasonData setTriples(final int triples) {
        this.triples = triples;
        return this;
    }

    public int getHr() {
        return hr;
    }

    public MlbSeasonData setHr(final int hr) {
        this.hr = hr;
        return this;
    }

    public int getRbis() {
        return rbis;
    }

    public MlbSeasonData setRbis(final int rbis) {
        this.rbis = rbis;
        return this;
    }

    public int getSb() {
        return sb;
    }

    public MlbSeasonData setSb(final int sb) {
        this.sb = sb;
        return this;
    }

    public int getCaughtStealing() {
        return caughtStealing;
    }

    public MlbSeasonData setCaughtStealing(final int caughtStealing) {
        this.caughtStealing = caughtStealing;
        return this;
    }

    public int getBb() {
        return bb;
    }

    public MlbSeasonData setBb(final int bb) {
        this.bb = bb;
        return this;
    }

    public int getSo() {
        return so;
    }

    public MlbSeasonData setSo(final int so) {
        this.so = so;
        return this;
    }

    public double getBa() {
        return ba;
    }

    public MlbSeasonData setBa(final double ba) {
        this.ba = ba;
        return this;
    }

    public double getObp() {
        return obp;
    }

    public MlbSeasonData setObp(final double obp) {
        this.obp = obp;
        return this;
    }

    public double getSlg() {
        return slg;
    }

    public MlbSeasonData setSlg(final double slg) {
        this.slg = slg;
        return this;
    }

    public double getOps() {
        return ops;
    }

    public MlbSeasonData setOps(final double ops) {
        this.ops = ops;
        return this;
    }

    public double getOps_plus() {
        return ops_plus;
    }

    public MlbSeasonData setOps_plus(final double ops_plus) {
        this.ops_plus = ops_plus;
        return this;
    }

    public int getTb() {
        return tb;
    }

    public MlbSeasonData setTb(final int tb) {
        this.tb = tb;
        return this;
    }

    public int getGdp() {
        return gdp;
    }

    public MlbSeasonData setGdp(final int gdp) {
        this.gdp = gdp;
        return this;
    }

    public int getHbp() {
        return hbp;
    }

    public MlbSeasonData setHbp(final int hbp) {
        this.hbp = hbp;
        return this;
    }

    public int getSh() {
        return sh;
    }

    public MlbSeasonData setSh(final int sh) {
        this.sh = sh;
        return this;
    }

    public int getSf() {
        return sf;
    }

    public MlbSeasonData setSf(final int sf) {
        this.sf = sf;
        return this;
    }

    public int getIbb() {
        return ibb;
    }

    public MlbSeasonData setIbb(final int ibb) {
        this.ibb = ibb;
        return this;
    }

    public int getLob() {
        return lob;
    }

    public MlbSeasonData setLob(final int lob) {
        this.lob = lob;
        return this;
    }

    public int getNumPitchers() {
        return numPitchers;
    }

    public MlbSeasonData setNumPitchers(final int numPitchers) {
        this.numPitchers = numPitchers;
        return this;
    }

    public double getpAge() {
        return pAge;
    }

    public MlbSeasonData setpAge(final double pAge) {
        this.pAge = pAge;
        return this;
    }

    public double getRallPerG() {
        return rallPerG;
    }

    public MlbSeasonData setRallPerG(final double rallPerG) {
        this.rallPerG = rallPerG;
        return this;
    }

    public int getWin() {
        return win;
    }

    public MlbSeasonData setWin(final int win) {
        this.win = win;
        return this;
    }

    public int getLoss() {
        return loss;
    }

    public MlbSeasonData setLoss(final int loss) {
        this.loss = loss;
        return this;
    }

    public double getWlpct() {
        return wlpct;
    }

    public MlbSeasonData setWlpct(final double wlpct) {
        this.wlpct = wlpct;
        return this;
    }

    public double getEra() {
        return era;
    }

    public MlbSeasonData setEra(final double era) {
        this.era = era;
        return this;
    }

    public int getGamesPitch() {
        return gamesPitch;
    }

    public MlbSeasonData setGamesPitch(final int gamesPitch) {
        this.gamesPitch = gamesPitch;
        return this;
    }

    public int getGs() {
        return gs;
    }

    public MlbSeasonData setGs(final int gs) {
        this.gs = gs;
        return this;
    }

    public int getGf() {
        return gf;
    }

    public MlbSeasonData setGf(final int gf) {
        this.gf = gf;
        return this;
    }

    public int getCg() {
        return cg;
    }

    public MlbSeasonData setCg(final int cg) {
        this.cg = cg;
        return this;
    }

    public int gettSho() {
        return tSho;
    }

    public MlbSeasonData settSho(final int tSho) {
        this.tSho = tSho;
        return this;
    }

    public int getcSho() {
        return cSho;
    }

    public MlbSeasonData setcSho(final int cSho) {
        this.cSho = cSho;
        return this;
    }

    public int getSv() {
        return sv;
    }

    public MlbSeasonData setSv(final int sv) {
        this.sv = sv;
        return this;
    }

    public double getIp() {
        return ip;
    }

    public MlbSeasonData setIp(final double ip) {
        this.ip = ip;
        return this;
    }

    public int getHa() {
        return ha;
    }

    public MlbSeasonData setHa(final int ha) {
        this.ha = ha;
        return this;
    }

    public int getRa() {
        return ra;
    }

    public MlbSeasonData setRa(final int ra) {
        this.ra = ra;
        return this;
    }

    public int getEr() {
        return er;
    }

    public MlbSeasonData setEr(final int er) {
        this.er = er;
        return this;
    }

    public int getHra() {
        return hra;
    }

    public MlbSeasonData setHra(final int hra) {
        this.hra = hra;
        return this;
    }

    public int getWalks() {
        return walks;
    }

    public MlbSeasonData setWalks(final int walks) {
        this.walks = walks;
        return this;
    }

    public int getIntWalks() {
        return intWalks;
    }

    public MlbSeasonData setIntWalks(final int intWalks) {
        this.intWalks = intWalks;
        return this;
    }

    public int getSoP() {
        return soP;
    }

    public MlbSeasonData setSoP(final int soP) {
        this.soP = soP;
        return this;
    }

    public int getHbpP() {
        return hbpP;
    }

    public MlbSeasonData setHbpP(final int hbpP) {
        this.hbpP = hbpP;
        return this;
    }

    public int getBk() {
        return bk;
    }

    public MlbSeasonData setBk(final int bk) {
        this.bk = bk;
        return this;
    }

    public int getWp() {
        return wp;
    }

    public MlbSeasonData setWp(final int wp) {
        this.wp = wp;
        return this;
    }

    public int getBf() {
        return bf;
    }

    public MlbSeasonData setBf(final int bf) {
        this.bf = bf;
        return this;
    }

    public int getEra_plus() {
        return era_plus;
    }

    public MlbSeasonData setEra_plus(final int era_plus) {
        this.era_plus = era_plus;
        return this;
    }

    public double getFip() {
        return fip;
    }

    public MlbSeasonData setFip(final double fip) {
        this.fip = fip;
        return this;
    }

    public double getWhip() {
        return whip;
    }

    public MlbSeasonData setWhip(final double whip) {
        this.whip = whip;
        return this;
    }

    public double getH9() {
        return h9;
    }

    public MlbSeasonData setH9(final double h9) {
        this.h9 = h9;
        return this;
    }

    public double getHr9() {
        return hr9;
    }

    public MlbSeasonData setHr9(final double hr9) {
        this.hr9 = hr9;
        return this;
    }

    public double getBb9() {
        return bb9;
    }

    public MlbSeasonData setBb9(final double bb9) {
        this.bb9 = bb9;
        return this;
    }

    public double getSo9() {
        return so9;
    }

    public MlbSeasonData setSo9(final double so9) {
        this.so9 = so9;
        return this;
    }

    public double getSoPerW() {
        return soPerW;
    }

    public MlbSeasonData setSoPerW(final double soPerW) {
        this.soPerW = soPerW;
        return this;
    }

    public int getLobP() {
        return lobP;
    }

    public MlbSeasonData setLobP(final int lobP) {
        this.lobP = lobP;
        return this;
    }

    public int getNumFld() {
        return numFld;
    }

    public MlbSeasonData setNumFld(final int numFld) {
        this.numFld = numFld;
        return this;
    }

    public double getRapg() {
        return rapg;
    }

    public MlbSeasonData setRapg(final double rapg) {
        this.rapg = rapg;
        return this;
    }

    public double getDefEff() {
        return defEff;
    }

    public MlbSeasonData setDefEff(final double defEff) {
        this.defEff = defEff;
        return this;
    }

    public double getInn() {
        return inn;
    }

    public MlbSeasonData setInn(final double inn) {
        this.inn = inn;
        return this;
    }

    public int getCh() {
        return ch;
    }

    public MlbSeasonData setCh(final int ch) {
        this.ch = ch;
        return this;
    }

    public int getPo() {
        return po;
    }

    public MlbSeasonData setPo(final int po) {
        this.po = po;
        return this;
    }

    public int getAssist() {
        return assist;
    }

    public MlbSeasonData setAssist(final int assist) {
        this.assist = assist;
        return this;
    }

    public int getErrors() {
        return errors;
    }

    public MlbSeasonData setErrors(final int errors) {
        this.errors = errors;
        return this;
    }

    public int getDp() {
        return dp;
    }

    public MlbSeasonData setDp(final int dp) {
        this.dp = dp;
        return this;
    }

    public double getFldPct() {
        return fldPct;
    }

    public MlbSeasonData setFldPct(final double fldPct) {
        this.fldPct = fldPct;
        return this;
    }

    public double getrTot() {
        return rTot;
    }

    public MlbSeasonData setrTot(final double rTot) {
        this.rTot = rTot;
        return this;
    }

    public double getrTotYr() {
        return rTotYr;
    }

    public MlbSeasonData setrTotYr(final double rTotYr) {
        this.rTotYr = rTotYr;
        return this;
    }

    public double getRdrs() {
        return rdrs;
    }

    public MlbSeasonData setRdrs(final double rdrs) {
        this.rdrs = rdrs;
        return this;
    }

    public double getRdrsYr() {
        return rdrsYr;
    }

    public MlbSeasonData setRdrsYr(final double rdrsYr) {
        this.rdrsYr = rdrsYr;
        return this;
    }

    public double getRgood() {
        return rgood;
    }

    public MlbSeasonData setRgood(final double rgood) {
        this.rgood = rgood;
        return this;
    }

}
