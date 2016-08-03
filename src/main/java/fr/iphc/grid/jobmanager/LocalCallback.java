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

import org.ogf.saga.context.Context;
import org.ogf.saga.error.AuthorizationFailedException;
import org.ogf.saga.error.NotImplementedException;
import org.ogf.saga.monitoring.Callback;
import org.ogf.saga.monitoring.Metric;
import org.ogf.saga.monitoring.Monitorable;

import fr.iphc.grid.Global;

public class LocalCallback implements Callback {
	public Callback callback = null;

	@Override
	public boolean cb(Monitorable arg0, Metric metric, Context arg2)
			throws NotImplementedException, AuthorizationFailedException {
		try {
			String value = metric.getAttribute(Metric.VALUE);
			if (value.startsWith("JSAGA")) {
				for (Jdl job : Global.ListJob) {
					// if ((job.getJob() != null) &&
					// (job.getMetric().equals(metric))) {) {
					job.setStatus(value);
					// System.out.println("METRIC Current state: "+value+"
					// ID:"+job.getId());
					// Runtime runtime = Runtime.getRuntime();
					// System.out.println("METRIC Current state: "+new Date()+"
					// "+job.getStatus()+" ID:"+job.getId()+"
					// "+runtime.freeMemory());
					// if (value.startsWith("JSAGA:DONE")) {
					// System.out.println("METRIC Current state:
					// "+job.getStatus()+" ID:"+job.getId());
					// job.setStatus("LOAD");
					// Global.getOutputexecutor.execute(new
					// GetOutputThread(job));
					// }
					break;
				}
			}
			// }
		} catch (NotImplementedException e) {
			throw e;
		} catch (AuthorizationFailedException e) {
			throw e;
		} catch (Exception e) {
			System.err.println("LocalCallback\n" + e.getMessage());
		}
		// e.printStackTrace();}
		return true;
	}

}
