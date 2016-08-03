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

package fr.iphc.grid.jobmonitor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
//import java.net.SocketTimeoutException;
import java.util.Date;
//import java.util.Properties;

//import org.ogf.saga.context.*;
import org.ogf.saga.error.SagaException;
//import org.ogf.saga.error.TimeoutException;
import org.ogf.saga.job.Job;
import org.ogf.saga.job.JobDescription;
import org.ogf.saga.job.JobFactory;
import org.ogf.saga.job.JobService;
import org.ogf.saga.session.Session;
//import org.ogf.saga.session.SessionFactory;
import org.ogf.saga.url.URL;

public class SubmitterThread extends Thread {
	private URL m_serviceURL;
	private Session m_session;
	private String m_OutDir;
	private JobDescription m_desc;
	private Integer m_timeout;
	private Date date_start;

	SubmitterThread(URL serviceURL, Session session, JobDescription desc, String OutDir, Integer timeout, Date date) {
		m_serviceURL = serviceURL;
		m_session = session;
		m_OutDir = OutDir;
		m_desc = desc;
		m_timeout = timeout;
		date_start = date;
	}

	// This method is called when the thread runs
	public void run() {
		try {
//			String proxy = null;
			// On recupere le context VOMS dans jsaga-default-context
			// for (org.ogf.saga.context.Context context:
			// ConfigurableContextFactory.getContextsOfDefaultSession()) {
			// if
			// (((String)context.getAttribute(org.ogf.saga.context.Context.USERVO)).equals("biomed"))
			// {
			// proxy=context.getAttribute(Context.USERPROXY.toString());
			// break;
			// }
			// }

			JobService service = JobFactory.createJobService(m_session, m_serviceURL);
			final Job job = service.createJob(m_desc);
			Date start = new Date();
			// traitement
			// System.out.println("Ligne affichée par le thread:
			// "+m_serviceURL.getHost()+" "+m_timeout);
			// pause
			job.run();
			// test status
			String jobId = job.getAttribute(Job.JOBID);
			FileOutputStream Output = new FileOutputStream(m_OutDir + "/" + m_serviceURL.getHost() + "_"
					+ m_serviceURL.getPath().replaceAll("/cream-", "") + ".out");
			PrintStream out = new PrintStream(Output);
			out.println("<?xml version=\"1.0\"?>");
			out.println("<Ce>");
			out.println("\t<CeHost>");
			out.println("\t\t<Start>" + date_start + "</Start>");
			out.println("\t\t<Ce>" + m_serviceURL.getHost() + "</Ce>");
			out.println("\t\t<Path>" + m_serviceURL.getPath() + "</Path>");
			out.println("\t\t<Jobid>" + jobId + "</Jobid>");
			try {
				Boolean status = job.waitFor(m_timeout);
				if (status) {
					out.println("\t\t<Status>OK</Status>");
					out.println("\t\t<Time>" + ((new Date().getTime()) - start.getTime()) / 1000 + "</Time>");
					out.println("\t\t<Desc></Desc>");
					out.println("\t</CeHost>");
					out.println("</Ce>");
				} else {
					out.println("\t\t<Status>TIMEOUT</Status>");
					out.println("\t\t<Time>" + ((new Date().getTime()) - start.getTime()) / 1000 + "</Time>");
					out.println("\t\t<Desc></Desc>");
					out.println("\t</CeHost>");
					out.println("</Ce>");
				}
			} catch (SagaException e) {
				out.println("\t\t<Status>ERROR</Status>");
				out.println("\t\t<Time>" + ((new Date().getTime()) - date_start.getTime()) / 1000 + "</Time>");
				out.println("\t\t<Desc>EXCEPTION: " + e.getMessage().replaceAll("[<>]", "") + "</Desc>");
				out.println("\t</CeHost>");
				out.println("</Ce>");
			}
			out.flush();
			out.close();
		} catch (Exception e) {
			try {
				FileOutputStream Erreur = new FileOutputStream(m_OutDir + "/" + m_serviceURL.getHost() + "_"
						+ m_serviceURL.getPath().replaceAll("/cream-", "") + ".err");
				PrintStream err = new PrintStream(Erreur);
				err.println("<?xml version=\"1.0\"?>");
				err.println("<Ce>");
				err.println("\t<CeHost>");
				err.println("\t\t<Start>" + date_start + "</Start>");
				err.println("\t\t<Ce>" + m_serviceURL.getHost() + "</Ce>");
				err.println("\t\t<Path>" + m_serviceURL.getPath() + "</Path>");
				err.println("\t\t<Status>ERROR</Status>");
				err.println("\t\t<Time>" + ((new Date().getTime()) - date_start.getTime()) / 1000 + "</Time>");
				err.println("\t\t<Desc>CREATE_JOB: " + e.getMessage() + "</Desc>");
				err.println("\t</CeHost>");
				err.println("</Ce>");
				err.flush();
				err.close();
				// e.printStackTrace();
			} catch (FileNotFoundException e1) {
			}
			;
			throw new RuntimeException(m_serviceURL.getHost() + "\t" + e.getCause());
		} // catch ( Exception e) {}
	}

	public void Requeststop() {
		try {
			FileOutputStream Erreur = new FileOutputStream(m_OutDir + "/" + m_serviceURL.getHost() + "_"
					+ m_serviceURL.getPath().replaceAll("/cream-", "") + ".lock");
//			FileOutputStream Output = new FileOutputStream(m_OutDir + "/" + m_serviceURL.getHost() + "_"
//					+ m_serviceURL.getPath().replaceAll("/cream-", "") + ".out");
			PrintStream err = new PrintStream(Erreur);
			err.println("<?xml version=\"1.0\"?>");
			err.println("<Ce>");
			err.println("\t<CeHost>");
			err.println("\t\t<Start>" + date_start + "</Start>");
			err.println("\t\t<Ce>" + m_serviceURL.getHost() + "</Ce>");
			err.println("\t\t<Path>" + m_serviceURL.getPath() + "</Path>");
			err.println("\t\t<Status>ERROR</Status>");
			err.println("\t\t<Time>0</Time>");
			err.println("\t\t<Desc>LOOP RUN</Desc>");
			err.println("\t</CeHost>");
			err.println("</Ce>");
			err.flush();
			err.close();
		} catch (FileNotFoundException e1) {
		} finally {
			File out = null;
			out = new File(m_OutDir + "/" + m_serviceURL.getHost() + "_"
					+ m_serviceURL.getPath().replaceAll("/cream-", "") + ".out");
			if (out.exists()) {
				out.delete();
			}
			;
		}
		;

	}

}
