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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Stack;

//import org.apache.log4j.FileAppender;
//import org.apache.log4j.Level;
//import org.apache.log4j.Logger;
//import org.apache.log4j.PatternLayout;

import fr.iphc.grid.Global;

public class LoggingThread extends Thread {
	private float SEUILDISPLAYLOG = 0;
	private ArrayList<Jdl> m_ListJob;
	private ArrayList<Url> m_ListUrl;
	private boolean stopThread = false;
	private BufferedWriter log = null;
	Date start = null;

	public LoggingThread(ArrayList<Jdl> ListJob, ArrayList<Url> ListUrl, float SeuilDisplayLog) {
		m_ListJob = ListJob;
		m_ListUrl = ListUrl;
		SEUILDISPLAYLOG = SeuilDisplayLog;
	}

	public void run() {
		Integer nb_job = m_ListJob.size();
		Integer CeMax = m_ListUrl.size();
		Float seuil = SEUILDISPLAYLOG;
		start = new Date();
		try {
			log = new BufferedWriter(new FileWriter(new File(Global.Cwd + "/grid.log")));
			log.write("Threshold log display: ====== " + seuil + " ======");
			log.newLine();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		while (!this.stopThread) {
			try {
				Integer nb_init = 0, nb_run = 0, nb_end = 0, nb_load = 0, nb_jsaga = 0, TO_w = 0, TO_r = 0;
				Stack<Jdl> st_run = new Stack<Jdl>();
				Stack<Jdl> st_jsaga = new Stack<Jdl>();
				Stack<Jdl> st_st = new Stack<Jdl>();
				for (Jdl job : m_ListJob) {
					if (!job.getStatus().equals("END")) {
						// log.write
						// (job.getId()+"-"+"-"+job.getStart()+"-"+job.getStatus()+"-"+job.getNodeCe()+"\n"
						// +"JOBID:"+job.getJobId()+"\n----------------\n");
					}
					TO_w = job.getTimeoutWait();
					TO_r = job.getTimeoutRun();
					st_st.push(job);
					if (job.getStatus().equals("INIT")) {
						nb_init++;
					}
					if (job.getStatus().startsWith("JSAGA:")) {
						if (job.getStatus().equals("JSAGA:RUNNING_ACTIVE")) {
							nb_run++;
							st_run.push(job);
						} else {
							nb_jsaga++;
							st_jsaga.push(job);
						}
					}
					if (job.getStatus().equals("LOAD")) {
						nb_load++;
					}
					if (job.getStatus().equals("END")) {
						nb_end++;
					}
				}
				if (m_ListUrl.size() > CeMax) {
					CeMax = m_ListUrl.size();
				}
				;

				log.write("ST1: ====== " + new Date() + " ======");
				log.newLine();
				log.write("ST2: INIT: " + nb_init + "/" + nb_job + "  QUEUE: " + nb_jsaga + "/" + nb_job + "  RUN: "
						+ nb_run + "/" + nb_job + "  LOAD: " + nb_load + "  END: " + nb_end + "/" + nb_job + "  TO_W: "
						+ TO_w + "  TO_R: " + TO_r + "\n");
				log.write("ST3: %\t" + String.format("%.2f", ((float) nb_end / (float) nb_job) * 100));
				log.newLine();
				log.write("ST4: Nb CE: " + m_ListUrl.size() + "/" + CeMax + "\n");
				log.newLine();

				if ((float) nb_end / (float) nb_job >= seuil) {
					while (!st_st.isEmpty()) {
						Jdl job = st_st.pop();
						if (!job.getStatus().equals("END")) {
							log.write(job.getId() + "-" + "-" + job.getStart() + "-" + job.getStatus() + "-"
									+ job.getNodeCe() + "\n" + "JOBID:" + job.getJobId() + " TR:" + job.getTimeoutRun()
									+ "\n----------------\n");
						}
					}
					log.write(">> JSAGA STATUS\n");
					while (!st_jsaga.isEmpty()) {
						Jdl job = st_jsaga.pop();
						log.write(job.getId() + "-" + job.getName() + "-" + "-" + job.getStart() + "-" + job.getStatus()
								+ "-" + job.getNodeCe() + "\n");
					}
					log.write(">> JSAGA RUNNING\n");
					while (!st_run.isEmpty()) {
						Jdl job = st_run.pop();
						log.write(job.getId() + "-" + job.getName() + "-" + "-" + job.getStart() + "-" + job.getNodeCe()
								+ "\n" + job.getJobId() + "\n----------------\n");
					}
				}
				log.write("<<<<<<<<<<<<<<<<<<<<<<\n");
				log.flush();
				long startTime = System.currentTimeMillis();
				long endTime = startTime + (30 * 1000);
				while (System.currentTimeMillis() < endTime) {
				}

			} catch (Exception e) {
				System.out.println("Exception LoggerThread:\n" + e.getMessage());
			}
		} // end while

	}

	public synchronized void halt() {
		this.stopThread = true;
		try {
			log.write("=== END ==== Start " + start + " End " + new Date() + "\n");
			log.flush();
			log.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
