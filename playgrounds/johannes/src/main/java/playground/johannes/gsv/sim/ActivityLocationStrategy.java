/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.sim;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.replanning.GenericPlanStrategy;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.router.TripRouter;

import playground.johannes.gsv.misc.QuadTree;
import playground.johannes.sna.util.ProgressLogger;

/**
 * @author johannes
 * 
 */
public class ActivityLocationStrategy implements GenericPlanStrategy<Plan, Person> {

	// private static final Logger logger =
	// Logger.getLogger(ActivityLocationStrategy.class);

	private Map<String, QuadTree<ActivityFacility>> quadTrees;

	private Random random;

	private String blacklist;

	private double mutationError = 0.1;

	private ActivityFacilities facilities;

	private ReplanningContext replanContext;

	private final ExecutorService executor;

	private List<Future<?>> futures;

	private final Queue<TripRouter> routers = new ConcurrentLinkedQueue<TripRouter>();

	private final Map<Person, double[]> targetDistances = new ConcurrentHashMap<>();

	public ActivityLocationStrategy(ActivityFacilities facilities, Random random, int numThreads, String blacklist) {
		this.random = random;
		this.facilities = facilities;
		this.blacklist = blacklist;
		executor = Executors.newFixedThreadPool(numThreads);

	}

	@Override
	public void run(HasPlansAndId<Plan, Person> person) {
		Task task = new Task((Person) person);
		Future<?> future = executor.submit(task);
		futures.add(future);
	}

	@Override
	public void init(ReplanningContext replanningContext) {
		this.replanContext = replanningContext;
		futures = new LinkedList<Future<?>>();

		if (quadTrees == null) {
			double minx = Double.MAX_VALUE;
			double miny = Double.MAX_VALUE;
			double maxx = 0;
			double maxy = 0;
			for (ActivityFacility facility : facilities.getFacilities().values()) {
				Coord coord = facility.getCoord();
				minx = Math.min(minx, coord.getX());
				miny = Math.min(miny, coord.getY());
				maxx = Math.max(maxx, coord.getX());
				maxy = Math.max(maxy, coord.getY());
			}

			quadTrees = new HashMap<String, QuadTree<ActivityFacility>>();
			for (ActivityFacility facility : facilities.getFacilities().values()) {
				for (ActivityOption option : facility.getActivityOptions().values()) {
					QuadTree<ActivityFacility> quadTree = quadTrees.get(option.getType());
					if (quadTree == null) {
						quadTree = new QuadTree<ActivityFacility>(minx, miny, maxx, maxy);
						quadTrees.put(option.getType(), quadTree);
					}
					Coord coord = facility.getCoord();
					quadTree.put(coord.getX(), coord.getY(), facility);
				}
			}
		}

	}

	@Override
	public void finish() {
		ProgressLogger.init(futures.size(), 2, 10);
		for (Future<?> future : futures) {
			try {
				future.get();
				ProgressLogger.step();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}

		ProgressLogger.termiante();

	}

	private final synchronized TripRouter pollTripRoute() {
		// System.out.println("enter");
		TripRouter router;
		if (routers.isEmpty()) {
			router = replanContext.getTripRouter();
		} else {
			router = routers.poll();
		}
		// System.out.println("exit");
		return router;
	}

	private final void releaseTripRouter(TripRouter router) {
		// System.out.println("Releasing trip router...");
		routers.add(router);
	}

	@Override
	protected void finalize() {
		executor.shutdown();
	}

	private class Task implements Runnable {

		private final Person person;

		public Task(Person person) {
			this.person = person;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run() {
			/*
			 * need to ensure that there is only one plan
			 */
			Plan plan = person.getSelectedPlan();
			/*
			 * select activity to change
			 */
			int n = (int) Math.floor(plan.getPlanElements().size() / 2.0);
			int idx = random.nextInt(n) * 2;
			ActivityImpl act = (ActivityImpl) plan.getPlanElements().get(idx);
			/*
			 * check if not blacklisted activity
			 */
			if (!act.getType().equalsIgnoreCase(blacklist)) {
				/*
				 * use the target distance of the from-trip as reference, except
				 * when it is the first activity. This needs to be done before
				 * the plan is copied, otherwise initializing target distance
				 * will fail
				 */
				int legIdx = 0;
				if (idx > 0) {
					legIdx = idx / 2 - 1;
				}
				double d = getTargetDistances(person)[legIdx];
				/*
				 * create copy of plan an set new references
				 */
				plan = person.createCopyOfSelectedPlanAndMakeSelected();
				act = (ActivityImpl) plan.getPlanElements().get(idx);

				// ActivityFacility ref = null;
				// if (idx == 0) {
				// ActivityImpl next = (ActivityImpl)
				// plan.getPlanElements().get(idx + 2);
				// ref = facilities.getFacilities().get(next.getFacilityId());
				// } else {
				// ActivityImpl prev = (ActivityImpl)
				// plan.getPlanElements().get(idx - 2);
				// ref = facilities.getFacilities().get(prev.getFacilityId());
				// }
				//
				ActivityFacility current = facilities.getFacilities().get(act.getFacilityId());
				//
				// double dx = current.getCoord().getX() -
				// ref.getCoord().getX();
				// double dy = current.getCoord().getY() -
				// ref.getCoord().getY();
				// double d = Math.sqrt(dx * dx + dy * dy);

				/*
				 * get quadtree for activity type
				 */
				QuadTree<ActivityFacility> quadtree = quadTrees.get(act.getType());
				/*
				 * get all facility candidates in target distance +/- mutation
				 * range
				 */
				Coord coord = current.getCoord();
				double min = Math.max(0, d * (1 - mutationError));
				double max = d * (1 + mutationError);
				List<ActivityFacility> candidates = (List<ActivityFacility>) quadtree.get(coord.getX(), coord.getY(), min, max);

				if (!candidates.isEmpty()) {
					/*
					 * draw random facility from candidates
					 */
					ActivityFacility newFac = candidates.get(random.nextInt(candidates.size()));

					act.setFacilityId(newFac.getId());
					act.setLinkId(newFac.getLinkId());
					/*
					 * route outward trip
					 */
					TripRouter router = pollTripRoute();
					if (idx > 1) {
						Activity prev = (Activity) plan.getPlanElements().get(idx - 2);
						ActivityFacility source = facilities.getFacilities().get(prev.getFacilityId());
						ActivityFacility target = newFac;

						Leg toLeg = (Leg) plan.getPlanElements().get(idx - 1);
						List<? extends PlanElement> stages = router.calcRoute(toLeg.getMode(), source, target, prev.getEndTime(), person);
						if (stages.size() > 1) {
							throw new UnsupportedOperationException();
						}
						plan.getPlanElements().set(idx - 1, stages.get(0));
					}
					/*
					 * route return trip
					 */
					if (idx < plan.getPlanElements().size() - 1) {
						Activity next = (Activity) plan.getPlanElements().get(idx + 2);
						ActivityFacility target = facilities.getFacilities().get(next.getFacilityId());
						ActivityFacility source = newFac;

						Leg fromLeg = (Leg) plan.getPlanElements().get(idx + 1);
						List<? extends PlanElement> stages = router.calcRoute(fromLeg.getMode(), source, target, act.getEndTime(), person);
						if (stages.size() > 1) {
							throw new UnsupportedOperationException();
						}
						plan.getPlanElements().set(idx + 1, stages.get(0));
					}
					releaseTripRouter(router);
				}
			}
		}

		private double[] getTargetDistances(Person person) {
			double[] distances = targetDistances.get(person);

			if (distances == null) {
				if (person.getPlans().size() > 1) {
					throw new RuntimeException("Only one plan is allowed!");
				}

				Plan plan = person.getSelectedPlan();
				distances = new double[(plan.getPlanElements().size() - 1) / 2];
				for (int i = 1; i < plan.getPlanElements().size(); i+=2) {
//					Activity prev = (Activity) plan.getPlanElements().get(i - 1);
//					Activity next = (Activity) plan.getPlanElements().get(i + 1);
//
//					ActivityFacility origin = facilities.getFacilities().get(prev.getFacilityId());
//					ActivityFacility target = facilities.getFacilities().get(next.getFacilityId());
//
//					double dx = origin.getCoord().getX() - target.getCoord().getX();
//					double dy = origin.getCoord().getY() - target.getCoord().getY();
//
//					double d = Math.sqrt(dx * dx + dy * dy);

					Leg leg = (Leg) plan.getPlanElements().get(i);
					double d = leg.getTravelTime();
					distances[(i - 1)/2] = d;
				}

				targetDistances.put(person, distances);
			}

			return distances;
		}

	}
}
