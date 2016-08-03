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

import org.apache.commons.cli.*;
import org.ogf.saga.job.JobFactory;
import org.ogf.saga.job.JobService;
import org.ogf.saga.session.Session;
import org.ogf.saga.session.SessionFactory;
import org.ogf.saga.url.URL;
import org.ogf.saga.url.URLFactory;

import fr.in2p3.jsaga.command.AbstractCommand;

import java.util.List;

public class JobList extends AbstractCommand {
	private static final String OPT_HELP = "h", LONGOPT_HELP = "help";

	protected JobList() {
		super("jsaga-job-list", new String[] { "resource" }, new String[] { OPT_HELP, LONGOPT_HELP });
	}

	public static void main(String[] args) throws Exception {
		JobList command = new JobList();
		CommandLine line = command.parse(args);
		if (line.hasOption(OPT_HELP)) {
			command.printHelpAndExit(null);
		} else {
			// get arguments
			URL serviceURL = URLFactory.createURL(command.m_nonOptionValues[0]);

			// get status
			Session session = SessionFactory.createSession(true);
			JobService service = JobFactory.createJobService(session, serviceURL);

			// dump list
			List<String> list = service.list();
			for (String jobid : list) {
				System.out.println(jobid);
			}
		}
	}

	protected Options createOptions() {
		Options opt = new Options();

		// command
		opt.addOption(
				OptionBuilder.withDescription("Display this help and exit").withLongOpt(LONGOPT_HELP).create(OPT_HELP));

		// returns
		return opt;
	}
}
