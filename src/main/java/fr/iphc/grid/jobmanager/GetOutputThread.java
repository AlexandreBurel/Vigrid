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

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Date;

import org.ogf.saga.error.BadParameterException;

//import fr.in2p3.jsaga.impl.job.instance.JobImpl;
//import fr.in2p3.jsaga.impl.job.service.JobServiceImpl;

public class GetOutputThread extends Thread {
	private Jdl m_job;

	GetOutputThread(Jdl job) {
		this.m_job = job;
	}

	/// This method is called when the thread runs
	public void run() {
		try {
			// Call job external
			String cmd[] = { "/usr/local/iphc/grid/bin/iphc-job-getoutput.sh", m_job.getJobId() };
			Process pr = Runtime.getRuntime().exec(cmd);
			Integer statusReturn = pr.waitFor();
			// Integer statusReturn=0;
			if (statusReturn == 255) {
				System.out.println("PG1:" + statusReturn);
				BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream()));
				String ligne = null;
				StringBuilder s = new StringBuilder();
				while ((ligne = input.readLine()) != null) {
					s.append(ligne);
				}
				// System.err.println("foo: " + s.toString());
				throw new BadParameterException("postStagingAndCleanup failed:" + s.toString());
			}
			// ((JobImpl) job).postStagingAndCleanup();
			m_job.setStatus("END");
			m_job.setTimeoutRun((int) (new Date().getTime() - m_job.getStart().getTime()));
			m_job.setJobId(null);
		} catch (Exception e) {
			// System.err.println("Thread GetOutputThread Job"+m_job.getId()+"
			// "+m_job.getName()+"\n Msg: "+
			// e.getMessage().replace("_O_GRID_FR_C_FR_O_CNRS_OU_IPHC_CN_Patrick_Guterl_biomed_Role_NULL_Capability_NULL",
			// "..."));
			System.err.println(
					"Thread GetOutputThread Job" + m_job.getId() + " " + m_job.getName() + "\n Msg: " + e.getMessage());
			// e.printStackTrace();
			m_job.setStatus("FAILED");
			m_job.setJobId(null);
			try {
				String[] InOut = m_job.getDesc().getVectorAttribute("FileTransfer");
				for (String inout : InOut) {
					if (inout != null) {
						String[] file = inout.split("<");
						if (file.length == 2) {
							try {
								// Construct a File object for the file to be
								// deleted.
								File target = new File(file[0]);

								int random = (int) (Math.random() * 100);
								File bad = new File(file[0] + "-" + "BAD" + random);
								if (target.exists()) {
									target.renameTo(bad);
									target.delete();
								}
							} catch (SecurityException e2) {
								System.err.println("Unable to delete " + file[0] + e2.getMessage());
							}
						}
					}
				}
			} catch (Exception e1) {
				System.err.println("EXP INOUT\n" + e1.getMessage());
			}
			m_job.setStatus("FAILED");
			m_job.setJobId(null);
		} finally {
			m_job = null;
			System.gc();
		}
	}

}
