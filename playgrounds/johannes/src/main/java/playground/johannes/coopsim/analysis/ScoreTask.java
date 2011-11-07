/* *********************************************************************** *
 * project: org.matsim.*
 * ScoreTask.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.coopsim.analysis;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import playground.johannes.coopsim.eval.ActivityDurationEvaluator;
import playground.johannes.coopsim.eval.ActivityEvaluator;
import playground.johannes.coopsim.eval.JointActivityEvaluator;
import playground.johannes.coopsim.eval.LegEvaluator;
import playground.johannes.coopsim.pysical.Trajectory;

/**
 * @author illenberger
 * 
 */
public class ScoreTask extends TrajectoryAnalyzerTask {

	@Override
	public void analyze(Set<Trajectory> trajectories, Map<String, DescriptiveStatistics> results) {
		DescriptiveStatistics allScores = new DescriptiveStatistics();
		for(Trajectory t : trajectories)
			allScores.addValue(t.getPerson().getSelectedPlan().getScore());
		results.put("score", allScores);
		
		DescriptiveStatistics actScores = ActivityEvaluator.stopLogging();
		results.put("score_act", actScores);

		DescriptiveStatistics legScores = LegEvaluator.stopLogging();
		results.put("score_leg", legScores);
		
		DescriptiveStatistics jointScore = JointActivityEvaluator.stopLogging();
		results.put("score_join", jointScore);
		
		DescriptiveStatistics durScore = ActivityDurationEvaluator.stopLogging();
		results.put("score_dur", durScore);
		
		try {
			writeHistograms(allScores, "score", 50, 50);
			writeHistograms(actScores, "score_act", 50, 50);
			writeHistograms(legScores, "score_leg", 50, 50);
			writeHistograms(jointScore, "score_join", 50, 50);
			writeHistograms(durScore, "score_dur", 50, 50);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
