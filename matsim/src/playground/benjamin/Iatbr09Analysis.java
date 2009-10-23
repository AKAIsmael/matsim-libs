/* *********************************************************************** *
 * project: org.matsim.*
 * Trb09Analysis
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.benjamin;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;

import playground.benjamin.BkPaths;
import playground.dgrether.analysis.charts.DgAvgDeltaScoreIncomeGroupChart;
import playground.dgrether.analysis.charts.DgAvgDeltaScoreIncomeModeChoiceChart;
import playground.dgrether.analysis.charts.DgDeltaScoreIncomeChart;
import playground.dgrether.analysis.charts.DgDeltaScoreIncomeModeChoiceChart;
import playground.dgrether.analysis.charts.DgMixedDeltaScoreIncomeModeChoiceChart;
import playground.dgrether.analysis.charts.DgModeChoiceIncomeChart;
import playground.dgrether.analysis.population.DgAnalysisPopulation;
import playground.dgrether.analysis.population.DgHouseholdsAnalysisReader;
import playground.dgrether.analysis.population.DgPopulationAnalysisReader;
import playground.dgrether.utils.charts.DgChartWriter;


public class Iatbr09Analysis {

	private static final Logger log = Logger.getLogger(Iatbr09Analysis.class);
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
			String runNumber1 = "855";
			String runNumber2 = "873";
//			String runNumber1 = "860";
//			String runNumber2 = "862";
		
			String runid1 = "run" + runNumber1;
			String runid2 = "run" + runNumber2;
			
			String runiddot1 = runid1 + ".";
//			String runiddot2 = runid2 + ".";
			
			String netfile = BkPaths.RUNBASE + runid1 + "/" + runiddot1 + "output_network.xml.gz";
			String plans1file = BkPaths.RUNBASE + runid1 + "/" + runiddot1 + "output_plans.xml.gz";
			String plans2file = BkPaths.RUNBASE + runid2 + "/" + runNumber2 + ".output_plans.xml.gz";
			String housholdsfile = BkPaths.RUNBASE + "dgrether/einkommenSchweiz/households_all_zrh30km_transitincl_10pct.xml.gz";
//			String housholdsfile = BkPaths.RUNBASE+ "bkick/oneRouteTwoModeIncomeTest/households.xml";

			String deltaScoreChartFile = BkPaths.RUNBASE + runid2 + "/deltaScoreSingleColorIncomeChart"+runNumber1+"vs"+runNumber2+".png";
			String deltaScoreColorChartFile = BkPaths.RUNBASE + runid2 + "/deltaScoreColorIncomeChart"+runNumber1+"vs"+runNumber2+".png";
//			String deltaScoreModeSwitchOnlyChartFile = BkPaths.RUNBASE + runid2 + "/deltaScoreIncomeChart"+runNumber1+"vs"+runNumber2+"ModeSwitcherOnly.png";
			String modeChoiceIncomeChartFile1 = BkPaths.RUNBASE + runid1 + "/"+runNumber1+"modeChoiceIncomeChart.png";
			String modeChoiceIncomeChartFile2 = BkPaths.RUNBASE + runid2 + "/"+runNumber2+"modeChoiceIncomeChart.png";
			
			String avgDeltaScoreIncomeGroupChartFile = BkPaths.RUNBASE + runid2 + "/avgDeltaScoreIncomeGroupChart"+runNumber1+"vs"+runNumber2+".png";
			String avgDeltaScoreIncomeGroupChartModeSwitchFile = BkPaths.RUNBASE + runid2 + "/avgDeltaScoreIncomeGroupModeSwitchChart"+runNumber1+"vs"+runNumber2+".png";
			
			String mixedDeltaScoreIncomeChartFile = BkPaths.RUNBASE + runid2 + "/mixedDeltaScoreIncomeChart"+runNumber1+"vs"+runNumber2+".png";
			
			ScenarioImpl sc = new ScenarioImpl();

			
			DgPopulationAnalysisReader pc = new DgPopulationAnalysisReader(sc);
			DgAnalysisPopulation ana = pc.doPopulationAnalysis(netfile, plans1file, plans2file);
			
			DgHouseholdsAnalysisReader hhr = new DgHouseholdsAnalysisReader(ana);
			hhr.readHousholds(housholdsfile);
			
			DgDeltaScoreIncomeChart incomeChart;
			incomeChart = new DgDeltaScoreIncomeChart(ana);
			incomeChart.writeFile(deltaScoreChartFile);
			
//			incomeChart.setWriteModeSwitcherOnly(true);
//			incomeChart.writeFile(deltaScoreModeSwitchOnlyChartFile);
//		
			DgDeltaScoreIncomeModeChoiceChart modeChoiceIncomeChart;
			modeChoiceIncomeChart = new DgDeltaScoreIncomeModeChoiceChart(ana);
			DgChartWriter.writerChartToFile(deltaScoreColorChartFile, modeChoiceIncomeChart.createChart());
			
			DgModeChoiceIncomeChart modechoiceIncomeChartRun1 = new DgModeChoiceIncomeChart(ana, DgAnalysisPopulation.RUNID1);
			DgChartWriter.writerChartToFile(modeChoiceIncomeChartFile1, modechoiceIncomeChartRun1.createChart());
			DgModeChoiceIncomeChart modechoiceIncomeChartRun2 = new DgModeChoiceIncomeChart(ana, DgAnalysisPopulation.RUNID2);
			DgChartWriter.writerChartToFile(modeChoiceIncomeChartFile2, modechoiceIncomeChartRun2.createChart());
			
//			DgChartFrame frame = new DgChartFrame("test", modechoiceIncomeChartRun2.createChart());
//
			new DgAvgDeltaScoreIncomeGroupChart(ana).writeFile(avgDeltaScoreIncomeGroupChartFile);
			DgAvgDeltaScoreIncomeModeChoiceChart avgDScoreIncomeChartData = new DgAvgDeltaScoreIncomeModeChoiceChart(ana);
//			DgChartFrame frame = new DgChartFrame("test", avgDScoreIncomeChartData.createChart());
			DgChartWriter.writerChartToFile(avgDeltaScoreIncomeGroupChartModeSwitchFile, avgDScoreIncomeChartData.createChart());
			
			DgMixedDeltaScoreIncomeModeChoiceChart mixedDsIncomeChart = new DgMixedDeltaScoreIncomeModeChoiceChart();
			mixedDsIncomeChart.addIncomeModeChoiceDataSet(modeChoiceIncomeChart.createDeltaScoreIncomeModeChoiceDataset());
			mixedDsIncomeChart.addAvgDeltaScoreIncomeDs(avgDScoreIncomeChartData.getDataset());
//			DgChartFrame frame = new DgChartFrame("test", mixedDsIncomeChart.createChart());
			DgChartWriter.writerChartToFile(mixedDeltaScoreIncomeChartFile, mixedDsIncomeChart.createChart());
			
			log.debug("ya esta ;-)");
			
	}
	

}
