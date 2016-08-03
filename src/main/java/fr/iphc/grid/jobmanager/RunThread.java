/*
Copyright CNRS - IPHC - LSMBO 2016
Authors: 
- Alexandre Burel: alexandre.burel@unistra.fr
- Patrick Guterl: Patrick.Guterl@unistra.fr

This software is a computer program whose purpose is to submit jobs on a 
computing grid using the JSAGA API. It is composed of two modules, a JobMonitor 
meant to run as a service to check periodically the state of each nodes on the 
grid, and a JobManager that will send the jobs on the best nodes at the moment 
of the submission.

This software is governed by the CeCILL license under French law and
abiding by the rules of distribution of free software.  You can  use, 
modify and/ or redistribute the software under the terms of the CeCILL
license as circulated by CEA, CNRS and INRIA at the following URL
"http://www.cecill.info". 

As a counterpart to the access to the source code and  rights to copy,
modify and redistribute granted by the license, users are provided only
with a limited warranty  and the software's author,  the holder of the
economic rights,  and the successive licensors  have only  limited
liability. 

In this respect, the user's attention is drawn to the risks associated
with loading,  using,  modifying and/or developing or reproducing the
software by the user in light of its specific status of free software,
that may mean  that it is complicated to manipulate,  and  that  also
therefore means  that it is reserved for developers  and  experienced
professionals having in-depth computer knowledge. Users are therefore
encouraged to load and test the software's suitability as regards their
requirements in conditions enabling the security of their systems and/or 
data to be ensured and,  more generally, to use and operate it in the 
same conditions as regards security. 

The fact that you are presently reading this means that you have had
knowledge of the CeCILL license and that you accept its terms.
*/

package fr.iphc.grid.jobmanager;

import java.util.Date;

import org.ogf.saga.job.Job;
import org.ogf.saga.job.JobFactory;
import org.ogf.saga.job.JobService;
import org.ogf.saga.session.Session;

public class RunThread extends Thread {
	private final Jdl m_job;
	private final Url m_url;
	private final Session m_session;

	RunThread(Jdl job, Url url, Session session) {
		this.m_job = job;
		this.m_url = url;
		this.m_session = session;
	}

	public void run() {
		JobService service = null;
		Job job = null;
		try {
			// Random randomGenerator = new Random();
			// Thread.sleep(randomGenerator.nextInt(1000)); //em millisecondes
			// Context ctx = ContextFactory.createContext();
			// ctx.setAttribute(Context.TYPE, "VOMS");
			// ctx.setAttribute(Context.USERVO, "biomed");
			// ctx.setAttribute(Context.USERPROXY,Global.proxy);
			// Session session = SessionFactory.createSession(false);
			// session.addContext(ctx);
			// Session session = SessionFactory.createSession(true);

			service = JobFactory.createJobService(m_session, m_url.getUrl());
			job = service.createJob(m_job.getDesc());
			job.run();

			// System.out.println("OUT Job:"+m_url.getUrl());
			if (!Thread.currentThread().isInterrupted()) {
				m_job.setStart(new Date());
				m_job.setNodeCe(m_url.getUrl());
				m_job.setStatus("BEGIN");
				m_job.setJobId(job.getAttribute(Job.JOBID));
			}

		} catch (Exception e) {
			m_job.setStart(new Date());
			m_job.setNodeCe(m_url.getUrl());
			m_url.setFailed(m_url.getFailed() + 1);
			if (m_url.getFailed() < 3) {
				m_job.setStatus("INIT");
				m_job.setJobId(null);
			} else {
				m_job.setStatus("INIT");
				m_job.setJobId(null);
			}
			System.err.println("\nERROR Thread JobRun: " + m_url.getUrl() + "\nMesg:\n" + e.getMessage());
		} // catch ( Exception e) {}
		finally {
			service = null;
			job = null;
			System.gc();
		}
	};

}
