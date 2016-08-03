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

//import java.rmi.RemoteException;
//import java.util.ArrayList;
//import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ogf.saga.job.Job;
import org.ogf.saga.job.JobFactory;
import org.ogf.saga.job.JobService;
import org.ogf.saga.monitoring.Metric;
import org.ogf.saga.session.Session;
import org.ogf.saga.session.SessionFactory;
import org.ogf.saga.url.URL;
import org.ogf.saga.url.URLFactory;

public class statusCe {
	// private static final String REGISTERED = "REGISTERED"; //the job has been
	// registered (but not started yet)
	// private static final String PENDING = "PENDING"; //the job has been
	// started, but it has still to be submitted to BLAH
	// private static final String IDLE = "IDLE"; //the job is idle in the Local
	// Resource Management System (LRMS)
	// private static final String RUNNING = "RUNNING"; //the job wrapper which
	// "encompasses" the user job is running in the LRMS.
	// private static final String REALLY_RUNNING = "REALLY-RUNNING"; //the
	// actual user job (the one specified as Executable in the job JDL) is
	// running in the LRMS
	// private static final String HELD = "HELD"; //the job is held (suspended)
	// in the LRMS
	// private static final String CANCELLED = "CANCELLED"; //the job has been
	// cancelled
	// private static final String DONE_OK = "DONE-OK"; //the job has
	// successfully been executed
	// private static final String DONE_FAILED = "DONE-FAILED";//the job has
	// been executed, but some errors occurred
	// private static final String ABORTED = "ABORTED"; //errors occurred during
	// the ``management'' of the job, e.g. the submission to the LRMS
	// abstraction layer software (BLAH) failed.
	//// private static final String UNKNOWN = "UNKNOWN"; //the job is an
	// unknown statu

	public String getStatusCe(String nativeJobId) throws Exception {
		String value = "Unknown1:Unknown1";
		URL serviceURL;
		Pattern pattern = Pattern.compile("\\[(.*)\\]-\\[(.*)\\]");
		Matcher matcher = pattern.matcher(nativeJobId);
		if (matcher.find()) {
			serviceURL = URLFactory.createURL(matcher.group(1));
			nativeJobId = matcher.group(2);
		} else {
			return ("Job ID does not match regular expression: " + pattern.pattern());
		}
		Session session = SessionFactory.createSession(true);
		JobService service = JobFactory.createJobService(session, serviceURL);
		Job job = service.getJob(nativeJobId);
		Metric metric = job.getMetric(Job.JOB_STATEDETAIL);
		// Integer cookie = metric.addCallback(new Callback() {
		// public boolean cb(Monitorable mt, Metric metric, Context ctx)
		// throws NotImplementedException, AuthorizationFailedException {
		// try {
		// String value = metric.getAttribute(Metric.VALUE);
		// } catch (Exception e) {
		// e.printStackTrace();
		// }
		// // callback must stay registered
		// return true;
		// } // end public boolean cb
		// }); // end metric.addCallback
		do {
			value = metric.getAttribute(Metric.VALUE);
		} while (!value.startsWith("JSAGA:"));
		// ->END

		// metric.removeCallback(cookie);
		// String name=value.split(":")[1];
		// if (REGISTERED.equals(name)) {
		// status="RUNNING_SUBMITTED";
		// } else if (PENDING.equals(name) || IDLE.equals(name) ||
		// RUNNING.equals(name)) {
		// status="RUNNING_QUEUED";
		// }
		// else if (REALLY_RUNNING.equals(name)) {
		// status="RUNNING_ACTIVE";
		// } else if (HELD.equals(name)) {
		// status="SUSPENDED_ACTIVE";
		// } else if (CANCELLED.equals(name)) {
		// status="CANCELED";
		// } else if (DONE_OK.equals(name)) {
		// status="DONE";
		// } else if (DONE_FAILED.equals(name)) {
		// status="FAILED_ERROR";
		// } else if (ABORTED.equals(name)) {
		// status="FAILED_ABORTED";
		// } else {
		//
		// }
		// value="JSAGA:RUNNING_SUBMITTED";

		// System.out.println("PGJDL STATUS:"+nativeJobId+" "+value);

		// ((JobServiceImpl)service).disconnect();

		// metric.removeCallback(cookie);
		// session=null;
		// job=null;
		// service=null;
		// metric=null;
		// value=null;
		// jdl=null;
		// cookie=null;
		// serviceURL=null;
		// nativeJobId=null;

		// System.out.println("STATUS_CE "+ jdl.getJobId()+" "+": "+value);
		// } catch (Exception e) {System.err.println("Exception:
		// "+job.getNodeCe()+" "+status
		// +"\n"+e.getMessage());
		// job.setStatus("JSAGA:FAILED_ERROR");
		// }
		return (value);
	};
}
