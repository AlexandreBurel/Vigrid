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

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.Executors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.ogf.saga.error.NoSuccessException;
import org.ogf.saga.job.JobDescription;
import org.ogf.saga.session.SessionFactory;

import fr.in2p3.jsaga.command.AbstractCommand;
import fr.iphc.grid.Global;

public class JobManager extends AbstractCommand {
	private static final String OPT_HELP = "h", LONGOPT_HELP = "help";
	// required arguments
	private static final String OPT_JOB = "f", LONGOPT_JOB = "job";
	private static final String OPT_FILEJOB = "l", LONGOPT_FILEJOB = "list";
	// optional arguments
	private static final String OPT_QUEUE = "q", LONGOPT_QUEUE = "queue";
	private static final String OPT_DESCRIPTION = "d", LONGOPT_DESCRIPTION = "description";
	private static final String OPT_JOBID = "i", LONGOPT_JOBID = "jobid";
	private static final String OPT_END = "e", LONGOPT_END = "end";
	private static final String OPT_RUN = "r", LONGOPT_RUN = "run";
	private static final String OPT_WAIT = "w", LONGOPT_WAIT = "wait";
	private static final String OPT_BAD = "b", LONGOPT_BAD = "bad";
	private static final String OPT_CWD = "c", LONGOPT_CWD = "cwd";
	private static final String OPT_LOGDISPLAY = "v", LONGOPT_LOGDISPLAY = "logd";
	private static final String OPT_OPTIMIZETIMEOUTRUN = "t", LONGOPT_OPTIMIZETIMEOUTRUN = "opto";
	private static final String OPT_SETUP = "s", LONGOPT_SETUP = "setup";

	protected JobManager() {
		super("jsaga-jobmanager", null, null, new GnuParser());
	}

	static void sleep(long sleep) { // in ms
		long startTime = System.currentTimeMillis();
		long endTime = startTime + sleep;
		while (System.currentTimeMillis() < endTime) {
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		JobManager command = new JobManager();
		CommandLine line = command.parse(args);
		ArrayList<File> JdlList = new ArrayList<File>();
		Global.getOutputexecutor = Executors.newFixedThreadPool(10);
		Initialize init = new Initialize();
		String SetupFile = "setup_vigrid.xml";

		if (line.hasOption(OPT_SETUP)) {
			SetupFile = line.getOptionValue(OPT_SETUP);
		}
		if ((new File(SetupFile).isFile())) {
			init.GlobalSetup(SetupFile);
		}
		// Init Job
		if (line.hasOption(OPT_JOB)) {
			File file = new File(line.getOptionValue(OPT_JOB));
			if ((file.isFile())) {
				JdlList.add(file);
			} else {
				System.err.println("The file " + file + " doesn't exist");
				System.exit(-1);
			}
		} else {
			File file = new File(line.getOptionValue(OPT_FILEJOB));
			if ((file.isFile())) {
				JdlList = init.InitJdl(file);
			} else {
				System.err.println("The file " + file + " doesn't exist");
				System.exit(-1);
			}
		}
		if (line.hasOption(OPT_WAIT)) {
			Global.TIMEOUTWAIT = Integer.parseInt(line.getOptionValue(OPT_WAIT));
		}
		if (line.hasOption(OPT_RUN)) {
			Global.TIMEOUTRUN = Integer.parseInt(line.getOptionValue(OPT_RUN));
		}
		if (line.hasOption(OPT_END)) {
			Global.TIMEOUTEND = Integer.parseInt(line.getOptionValue(OPT_END));
		}
		if (line.hasOption(OPT_LOGDISPLAY)) {
			Global.SEUILDISPLAYLOG = Float.parseFloat(line.getOptionValue(OPT_LOGDISPLAY));
		}
		init.InitJob(JdlList);
		// Init Url Ce
		if (line.hasOption(OPT_QUEUE)) {
			Global.file = new File(line.getOptionValue(OPT_QUEUE));
		}
		if (line.hasOption(OPT_BAD)) {
			Global.BadCe = new File(line.getOptionValue(OPT_BAD));
		}
		if (line.hasOption(OPT_OPTIMIZETIMEOUTRUN)) {
			Global.OPTTIMEOUTRUN = false;
		}
		if (line.hasOption(OPT_CWD)) {
			File theDir = new File(line.getOptionValue(OPT_CWD));
			if (!theDir.exists()) {
				if (!theDir.mkdirs()) {
					System.err.println("Working directory create failed: " + line.getOptionValue(OPT_CWD));
					System.exit(-1);
				}
			}
			Global.Cwd = line.getOptionValue(OPT_CWD);
		} else {
			Global.Cwd = System.getProperty("user.dir");
		}
		if (!(new File(Global.Cwd)).canWrite()) {
			System.err.println(" Write permission denied : " + Global.Cwd);
			System.exit(-1);
		}
		System.out.println("Current working directory : " + Global.Cwd);
		Date start = new Date();
		init.PrintGlobalSetup();
		init.InitUrl(Global.file);
		init.InitSosCe();
		init.rmLoadFailed(Global.Cwd + "/loadFailed.txt");
		System.out.println("CE: " + Global.ListUrl.size() + " Nb JOB: " + Global.ListJob.size() + " " + new Date());
		if (Global.ListJob.size() < 6) { // pour obtenir rapport de 0.8
			Global.OPTTIMEOUTRUN = false;
		}
		// check if we can connect to the grid
		try {
			SessionFactory.createSession(true);
		} catch (NoSuccessException e) {
			System.err.println("Could not connect to the grid at all (" + e.getMessage() + ")");
			System.err.println("Aborting");
			System.exit(0);

		}
		// Launch Tread Job
		JobThread st = new JobThread(Global.ListJob, Global.ListUrl);
		st.start();
		LoggingThread logst = new LoggingThread(Global.ListJob, Global.ListUrl, Global.SEUILDISPLAYLOG);
		logst.start();
		// create Thread Hook intercept kill +CNTL+C
		Thread hook = new Thread() {
			public void run() {
				try {
					for (Jdl job : Global.ListJob) {
						if (job.getJobId() != null) {
							JobThread.jobCancel(job.getJobId());
						}
					}
				} catch (Exception e) {
					System.err.println("Thread Hook:\n" + e.getMessage());
				}
				// give it a change to display final job state
				try {
					sleep(5000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		};
		Runtime.getRuntime().addShutdownHook(hook);

//		Integer timer = 180 * 60 * 1000;
		Date now = new Date();

//		Boolean Fin = false;
		while ((!Global.END) && ((now.getTime() - start.getTime()) < Global.TIMEOUTEND * 60 * 1000)) { // TOEND
																										// en
																										// minutes
			now = new Date();
			// int mb = 1024*1024;
			// Getting the runtime reference from system
			// Runtime runtime = Runtime.getRuntime();
			// System.out.println("##### Heap utilization statistics [MB]
			// #####");
			// Print used memory
			// System.out.println("Used Memory:"
			// + (runtime.totalMemory() - runtime.freeMemory()) / mb);

			// Print free memory
			// System.out.println("Free Memory:"
			// + runtime.freeMemory() / mb);

			// Print total available memory
			// System.out.println("Total Memory:" + runtime.totalMemory() / mb);

			// Print Maximum available memory
			// System.out.println("Max Memory:" + runtime.maxMemory() / mb);
			// // System.out.println("NB: "+nb_end);
			// if ((float)(runtime.totalMemory() -
			// runtime.freeMemory())/(float)runtime.maxMemory() > (float)0.3){
			// System.out.println ("GC: "+(float)(runtime.totalMemory() -
			// runtime.freeMemory())/runtime.maxMemory());
			// System.gc();
			// };
			sleep(15 * 1000); // in ms

			// System.gc();
			// Fin=true;
			// for (Jdl job : Global.ListJob) {
			// if (job.getJob() != null) {
			// System.out.println("JOB: "+job.getId()+"\t"+job.getStatus());
			// if (job.getStatus().compareTo("END")==0){
			// ((JobImpl) job.getJob()).postStagingAndCleanup();
			// System.out.println("END JOB: "+job.getId());
			// job.setStatus("END");
			// }
			// if (job.getStatus().compareTo("END")!=0){
			// Fin=false;
			// }
			// System.out.println("JOB: "+job.getId()+"\t"+job.getStatus() +
			// "\t"+job.getFail()+"\t"+job.getNodeCe());
			// }
			// }
			// while ((Global.END==0) && ((new
			// Date().getTime()-start.getTime())<timer)){
		}
		// Boolean end_load=false;
		// while (!end_load){
		// end_load=true;
		// for(Jdl job:Global.ListJob){
		// if (job.getStatus().equals("LOAD")){
		// end_load=false;
		// }
		// }
		// }
		System.out.println("END JOB: " + now);
		st.halt();
		logst.halt();
		Iterator<Url> k = Global.ListUrl.iterator();
		while (k.hasNext()) {
			Url url = k.next();
			System.out.println("URL: " + url.getUrl());
		}
		Iterator<Jdl> m = Global.ListJob.iterator();
		while (m.hasNext()) {
			Jdl job = m.next();
			System.out.println(
					"JOB: " + job.getId() + "\t" + job.getFail() + "\t" + job.getStatus() + "\t" + job.getNodeCe());
		}
		System.out.println(start + " " + new Date());
		System.exit(0);
	}

	@SuppressWarnings("static-access")
	protected Options createOptions() {
		Options opt = new Options();

		// command
		opt.addOption(
				OptionBuilder.withDescription("Display this help and exit").withLongOpt(LONGOPT_HELP).create(OPT_HELP));

		// optional arguments
		opt.addOption(OptionBuilder.withDescription("file with cream URL [IPHC]").hasArg().withLongOpt(LONGOPT_QUEUE)
				.withArgName("queue").create(OPT_QUEUE));

		opt.addOption(
				OptionBuilder
						.withDescription("generate the job description in the targeted grid language "
								+ "and exit (do not submit the job)")
						.withLongOpt(LONGOPT_DESCRIPTION).create(OPT_DESCRIPTION));

		opt.addOption(OptionBuilder.withDescription("File List of Bad Ce elminate from the Ce List[IPHC]").hasArg()
				.withLongOpt(LONGOPT_BAD).create(OPT_BAD));

		opt.addOption(
				OptionBuilder.withDescription("Time -> END Program in minutes/hour (Default 10H) Value: mM hH  [IPHC]")
						.hasArg().withLongOpt(LONGOPT_END).create(OPT_END));

		opt.addOption(
				OptionBuilder.withDescription("Duration Run -Thread in minutes/hour (Default 1H) Value: mM hH  [IPHC]")
						.hasArg().withLongOpt(LONGOPT_RUN).create(OPT_RUN));

		opt.addOption(OptionBuilder.withDescription("Time queue wating in CE/queue in minutes (Default 15 mn) [IPHC]")
				.hasArg().withLongOpt(LONGOPT_WAIT).create(OPT_WAIT));

		opt.addOption(OptionBuilder.withDescription("Threshold display full log in log file[IPHC]").hasArg()
				.withLongOpt(LONGOPT_LOGDISPLAY).create(OPT_LOGDISPLAY));

		opt.addOption(OptionBuilder.withDescription("Define working directory for monitoring file (default cwd) [IPHC]")
				.hasArg().withLongOpt(LONGOPT_CWD).create(OPT_CWD));

		opt.addOption(OptionBuilder.withDescription("Setup xml file Global paramters (default setup) [IPHC]").hasArg()
				.withLongOpt(LONGOPT_SETUP).create(OPT_SETUP));

		opt.addOption(OptionBuilder.withDescription("Disable Optimize Timeout Run Average execution time[IPHC]")
				.withLongOpt(LONGOPT_OPTIMIZETIMEOUTRUN).create(OPT_OPTIMIZETIMEOUTRUN));

		// required arguments group jdl file | list of jdl
		OptionGroup reqGroup = new OptionGroup();
		reqGroup.addOption(OptionBuilder.withDescription("read job description from file <path> [IPHC]").hasArg()
				.withLongOpt(LONGOPT_JOB).withArgName("file").create(OPT_JOB));
		reqGroup.addOption(OptionBuilder.withDescription("read file include jdl file [IPHC]").hasArg()
				.withLongOpt(LONGOPT_FILEJOB).withArgName("list").create(OPT_FILEJOB));
		reqGroup.setRequired(true);
		opt.addOptionGroup(reqGroup);

		// optional group
		OptionGroup optGroup = new OptionGroup();
		optGroup.addOption(OptionBuilder
				.withDescription(
						"print the job identifier as soon as it is submitted, " + "and wait for it to be finished")
				.withLongOpt(LONGOPT_JOBID).create(OPT_JOBID));

		optGroup.setRequired(false);
		opt.addOptionGroup(optGroup);

		// job description
		opt.addOption(o("positional parameters for the command").hasArgs().create(JobDescription.ARGUMENTS));
		opt.addOption(o("SPMD job type and startup mechanism").hasArg().create(JobDescription.SPMDVARIATION));
		opt.addOption(o("total number of cpus requested for this job").hasArg().create(JobDescription.TOTALCPUCOUNT));
		opt.addOption(o("number of process instances to start").hasArg().create(JobDescription.NUMBEROFPROCESSES));
		opt.addOption(o("number of processes to start per host").hasArg().create(JobDescription.PROCESSESPERHOST));
		opt.addOption(o("expected number of threads per process").hasArg().create(JobDescription.THREADSPERPROCESS));
		opt.addOption(o("set of environment variables for the job").hasArgs().withValueSeparator()
				.create(JobDescription.ENVIRONMENT));
		opt.addOption(o("working directory for the job").hasArg().create(JobDescription.WORKINGDIRECTORY));
		opt.addOption(o("run the job in interactive mode").create(JobDescription.INTERACTIVE));
		opt.addOption(o("pathname of the standard input file").hasArg().create(JobDescription.INPUT));
		opt.addOption(o("pathname of the standard output file").hasArg().create(JobDescription.OUTPUT));
		opt.addOption(o("pathname of the standard error file").hasArg().create(JobDescription.ERROR));
		opt.addOption(o("a list of file transfer directives").hasArgs().create(JobDescription.FILETRANSFER));
		opt.addOption(o("defines if output files get removed after the job finishes").hasArg()
				.create(JobDescription.CLEANUP));
		opt.addOption(o("time at which a job should be scheduled").hasArg().create(JobDescription.JOBSTARTTIME));
		opt.addOption(o("hard limit for the total job runtime").hasArg().create(JobDescription.WALLTIMELIMIT));
		opt.addOption(o("estimated total number of CPU seconds which the job will require").hasArg()
				.create(JobDescription.TOTALCPUTIME));
		opt.addOption(
				o("estimated amount of memory the job requires").hasArg().create(JobDescription.TOTALPHYSICALMEMORY));
		opt.addOption(o("compatible processor for job submission").hasArg().create(JobDescription.CPUARCHITECTURE));
		opt.addOption(o("compatible operating system for job submission").hasArg()
				.create(JobDescription.OPERATINGSYSTEMTYPE));
		opt.addOption(o("list of host names which are to be considered by the resource manager as candidate targets")
				.hasArgs().create(JobDescription.CANDIDATEHOSTS));
		opt.addOption(o("name of a queue to place the job into").hasArg().create(JobDescription.QUEUE));
		opt.addOption(o("name of an account or project name").hasArg().create(JobDescription.JOBPROJECT));
		opt.addOption(o("set of endpoints describing where to report").hasArgs().create(JobDescription.JOBCONTACT));

		// returns
		return opt;
	}

	private static OptionBuilder o(String description) {
		return OptionBuilder.withDescription(description);
	}

}
