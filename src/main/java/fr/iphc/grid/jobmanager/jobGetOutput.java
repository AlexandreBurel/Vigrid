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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ogf.saga.job.Job;
import org.ogf.saga.job.JobFactory;
import org.ogf.saga.job.JobService;
import org.ogf.saga.session.Session;
import org.ogf.saga.session.SessionFactory;
import org.ogf.saga.url.URL;
import org.ogf.saga.url.URLFactory;

import fr.in2p3.jsaga.impl.job.instance.JobImpl;

public class jobGetOutput {

	public static void main(String[] args) {

		String idCe = args[0];
		Pattern pattern = Pattern.compile("\\[(.*)\\]-\\[(.*)\\]");
		URL serviceURL = null;
		String nativeJobId = null;
		Matcher matcher = pattern.matcher(idCe);
		if (matcher.find()) {
			try {
				serviceURL = URLFactory.createURL(matcher.group(1));
				nativeJobId = matcher.group(2);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				System.exit(-1);
				e.printStackTrace();
			}
		} else {
			System.err.println("Job ID does not match regular expression: " + pattern.pattern());
			System.exit(-1);
		}
		// // get job
		Session session;
		JobService service = null;
		try {
			session = SessionFactory.createSession(true);
			service = JobFactory.createJobService(session, serviceURL);
			Job job = service.getJob(nativeJobId);
			// execute post-staging and cleanup
			((JobImpl) job).postStagingAndCleanup();
		} catch (Exception e) {
			System.err.println("URL:" + serviceURL + " CreamID:" + nativeJobId + "\nMsg: " + e.getMessage());
			System.exit(-1);
		}
		// finally {
		// try {
		// ((JobServiceImpl)service).disconnect();
		// } catch (Exception e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// }
		System.exit(0);
	}
}