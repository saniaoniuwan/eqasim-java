package org.eqasim.scenario.location_assignment;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eqasim.location_assignment.matsim.solver.MATSimAssignmentSolver;
import org.eqasim.location_assignment.matsim.solver.MATSimSolverResult;
import org.eqasim.location_assignment.matsim.utils.LocationAssignmentPlanAdapter;
import org.eqasim.misc.ParallelProgress;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;

public class LocationAssignment {
	private final MATSimAssignmentSolver solver;
	private final int numberOfThreads;
	private final int batchSize;

	public LocationAssignment(MATSimAssignmentSolver solver, int numberOfThreads, int batchSize) {
		this.solver = solver;
		this.numberOfThreads = numberOfThreads;
		this.batchSize = batchSize;
	}

	public void run(Population population) throws InterruptedException {
		Iterator<? extends Person> personIterator = population.getPersons().values().iterator();

		List<Thread> threads = new LinkedList<>();
		ThreadGroup threadGroup = new ThreadGroup("LocationAssignment");

		ParallelProgress progress = new ParallelProgress("Location assignment ...", population.getPersons().size());

		for (int i = 0; i < numberOfThreads; i++) {
			Thread thread = new Thread(threadGroup, new Worker(personIterator, progress));
			thread.setDaemon(true);
			thread.start();
			threads.add(thread);
		}
		
		progress.start();

		for (Thread thread : threads) {
			thread.join();
		}

		progress.close();
	}

	class Worker implements Runnable {
		private final Iterator<? extends Person> personIterator;
		private final ParallelProgress progress;

		public Worker(Iterator<? extends Person> personIterator, ParallelProgress progress) {
			this.personIterator = personIterator;
			this.progress = progress;
		}

		@Override
		public void run() {
			List<Person> localTasks = new LinkedList<>();
			LocationAssignmentPlanAdapter adapter = new LocationAssignmentPlanAdapter();

			do {
				localTasks.clear();

				synchronized (personIterator) {
					while (personIterator.hasNext() && localTasks.size() < batchSize) {
						localTasks.add(personIterator.next());
					}
				}

				for (Person person : localTasks) {
					for (Plan plan : person.getPlans()) {
						Collection<MATSimSolverResult> result = solver.solvePlan(plan);
						result.forEach(adapter::accept);
					}
				}

				progress.update(localTasks.size());
			} while (localTasks.size() > 0);
		}
	}
}