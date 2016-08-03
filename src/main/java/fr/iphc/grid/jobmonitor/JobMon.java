package fr.iphc.grid.jobmonitor;

import java.io.FileInputStream;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.ogf.saga.context.Context;
import org.ogf.saga.error.AuthorizationFailedException;
import org.ogf.saga.error.BadParameterException;
import org.ogf.saga.error.NotImplementedException;
import org.ogf.saga.error.SagaException;
import org.ogf.saga.job.Job;
import org.ogf.saga.job.JobDescription;
import org.ogf.saga.job.JobFactory;
import org.ogf.saga.job.JobService;
import org.ogf.saga.monitoring.Callback;
import org.ogf.saga.monitoring.Metric;
import org.ogf.saga.monitoring.Monitorable;
import org.ogf.saga.session.Session;
import org.ogf.saga.session.SessionFactory;
import org.ogf.saga.task.State;
import org.ogf.saga.url.URL;
import org.ogf.saga.url.URLFactory;

import fr.in2p3.jsaga.command.AbstractCommand;

/* ***************************************************
* *** Centre de Calcul de l'IN2P3 - Lyon (France) ***
* ***             http://cc.in2p3.fr/             ***
* ***************************************************
* File:   JobRun
* Author: Sylvain Reynaud (sreynaud@in2p3.fr)
* Date:   4 avr. 2007
* ***************************************************
* Description:                                      */
/**
 * -r local://localhost -Executable job.sh -FileTransfer
 * input>input,output<<output
 */
public class JobMon extends AbstractCommand {
	private static final String OPT_HELP = "h", LONGOPT_HELP = "help";
	// required arguments
	private static final String OPT_RESOURCE = "r", LONGOPT_RESOURCE = "resource";
	// optional arguments
	private static final String OPT_FILE = "f", LONGOPT_FILE = "file";
	private static final String OPT_DESCRIPTION = "d", LONGOPT_DESCRIPTION = "description";
	private static final String OPT_JOBID = "i", LONGOPT_JOBID = "jobid";
	private static final String OPT_BATCH = "b", LONGOPT_BATCH = "batch";

	protected JobMon() {
		super("jsaga-job-run", null, null, new GnuParser());
	}

	public static void main(String[] args) throws Exception {
		JobMon command = new JobMon();
		CommandLine line = command.parse(args);
		if (line.hasOption(OPT_HELP)) {
			command.printHelpAndExit(null);
		} else {
			// get arguments
			URL serviceURL = URLFactory.createURL(line.getOptionValue(OPT_RESOURCE));
			String file = line.getOptionValue(OPT_FILE);

			// create the job description
			Properties prop = new Properties();
			if (file != null) {
				prop.load(new FileInputStream(file));
			}
			for (Iterator it = line.iterator(); it.hasNext();) {
				Option opt = (Option) it.next();
				if (opt.getValue() != null) {
					prop.setProperty(opt.getOpt(), opt.getValue());
				} else {
					prop.setProperty(opt.getOpt(), Boolean.toString(true));
				}
			}
			JobDescription desc = createJobDescription(prop);
			boolean isStreamRedirected = prop.containsKey(JobDescription.INPUT)
					|| prop.containsKey(JobDescription.OUTPUT) || prop.containsKey(JobDescription.ERROR);
			if (!line.hasOption(OPT_BATCH) && !isStreamRedirected) {
				desc.setAttribute(JobDescription.INTERACTIVE, "true");
			}

			// create the job
			try {
				Session session = SessionFactory.createSession(true);
				JobService service = JobFactory.createJobService(session, serviceURL);
				final Job job = service.createJob(desc);

				if (line.hasOption(OPT_DESCRIPTION)) {
					// dump job description
					String nativeDesc = job.getAttribute("NativeJobDescription");
					System.out.println(nativeDesc);
				} else {
					// submit
					System.out.println("First " + new Date());
					try {
						job.run();
					} catch (SagaException sa) {
						System.out.println("RUN ERROR");
					}
					;
					System.out.println(new Date() + " " + job.getState());
					// print job identifier
					if (line.hasOption(OPT_JOBID) || line.hasOption(OPT_BATCH)) {
						String jobId = job.getAttribute(Job.JOBID);
						System.out.println(jobId);
					}

					// monitor
					if (!line.hasOption(OPT_BATCH)) {
						// add shutdown hook
						Thread hook = new Thread() {
							public void run() {
								// cancel the job
								try {
									System.out.println("Canceling job: " + job.getAttribute(Job.JOBID));
									job.cancel();
								} catch (SagaException e) {
									e.printStackTrace();
								}
								// give it a change to display final job state
								try {
									sleep(1000);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}
						};
						Runtime.getRuntime().addShutdownHook(hook);
						State state1 = job.getState();
						System.out.println("Current state1: " + state1);
						Metric metric = job.getMetric(Job.JOB_STATEDETAIL);
						metric.addCallback(new Callback() {
							public boolean cb(Monitorable mt, Metric metric, Context ctx)
									throws NotImplementedException, AuthorizationFailedException {
								try {
									String value = metric.getAttribute(Metric.VALUE);
									System.out.println("Current state: " + value);
									String descr = metric.getAttribute(Metric.TYPE);
									System.out.println("Param: " + descr);
								} catch (NotImplementedException e) {
									throw e;
								} catch (AuthorizationFailedException e) {
									throw e;
								} catch (Exception e) {
									e.printStackTrace();
								}
								return true;
							}
						});

						// wait
						try {
							Boolean status = job.waitFor(240);
							if (status) {
								Integer code = 0;
								System.out.println(new Date());
								System.out.println("CODE:" + code);
								System.out.println("DESC: OK");
							} else {
								Integer code = 1;
								System.out.println(new Date());
								System.out.println("CODE:" + code);
								System.out.println("DESC: TIMEOUT");
							}
						} catch (SagaException e) {
							System.out.println(new Date());
							Integer code = 2;
							System.out.println("CODE:" + code);
							System.out.println("DESC:" + e.getMessage());
						}
					}
					// End Modif
					// job.waitFor();
					// display final state
					//// State state = job.getState();
					// if (State.CANCELED.compareTo(state) == 0) {
					// System.out.println("Job canceled.");
					// } else {
					// Runtime.getRuntime().removeShutdownHook(hook);
					// if (State.DONE.compareTo(state) == 0) {
					// try {
					// if
					//// ("true".equalsIgnoreCase(desc.getAttribute(JobDescription.INTERACTIVE)))
					//// {
					// copyStream(job.getStdout(), System.out);
					// } else {
					// System.out.println("Job done.");
					// }
					// } catch(SagaException e) {
					// System.out.println("Job done.");
					// }
					// } else if (State.FAILED.compareTo(state) == 0) {
					// try {
					// String exitCode = job.getAttribute(Job.EXITCODE);
					// System.out.println("Job failed with exit code:
					//// "+exitCode);
					// } catch(SagaException e) {
					// System.out.println("Job failed.");
					// job.rethrow();
					// }
					// } else {
					// throw new Exception("Unexpected state: "+ state);
					// }
					// }
				}
			} catch (Exception e) {
				System.out.println("ERROR : " + e.getMessage());
			}
			;
			System.out.println("Bye");
			System.exit(0);
		}
	}

	protected Options createOptions() {
		Options opt = new Options();

		// command
		opt.addOption(
				OptionBuilder.withDescription("Display this help and exit").withLongOpt(LONGOPT_HELP).create(OPT_HELP));

		// required arguments
		opt.addOption(OptionBuilder.withDescription("the URL of the job service").isRequired(true).hasArg()
				.withArgName("URL").withLongOpt(LONGOPT_RESOURCE).create(OPT_RESOURCE));

		// optional arguments
		opt.addOption(
				OptionBuilder
						.withDescription("generate the job description in the targeted grid language "
								+ "and exit (do not submit the job)")
						.withLongOpt(LONGOPT_DESCRIPTION).create(OPT_DESCRIPTION));

		// optional group
		OptionGroup optGroup = new OptionGroup();
		optGroup.addOption(OptionBuilder
				.withDescription(
						"print the job identifier as soon as it is submitted, " + "and wait for it to be finished")
				.withLongOpt(LONGOPT_JOBID).create(OPT_JOBID));
		optGroup.addOption(
				OptionBuilder
						.withDescription(
								"print the job identifier as soon as it is submitted, " + "and exit immediatly.")
						.withLongOpt(LONGOPT_BATCH).create(OPT_BATCH));
		optGroup.setRequired(false);
		opt.addOptionGroup(optGroup);

		// required group
		OptionGroup reqGroup = new OptionGroup();
		reqGroup.addOption(OptionBuilder.withDescription("read job description from file <path>").hasArg()
				.withArgName("path").withLongOpt(LONGOPT_FILE).create(OPT_FILE));
		reqGroup.addOption(o("command to execute").hasArg().create(JobDescription.EXECUTABLE));
		reqGroup.setRequired(true);
		opt.addOptionGroup(reqGroup);

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

	private static JobDescription createJobDescription(Properties prop) throws Exception {
		JobDescription desc = JobFactory.createJobDescription();
		setRequired(desc, prop, JobDescription.EXECUTABLE);
		setOptMulti(desc, prop, JobDescription.ARGUMENTS);
		setOptional(desc, prop, JobDescription.SPMDVARIATION);
		setOptional(desc, prop, JobDescription.TOTALCPUCOUNT);
		setOptional(desc, prop, JobDescription.NUMBEROFPROCESSES);
		setOptional(desc, prop, JobDescription.PROCESSESPERHOST);
		setOptional(desc, prop, JobDescription.THREADSPERPROCESS);
		setOptMulti(desc, prop, JobDescription.ENVIRONMENT);
		setOptional(desc, prop, JobDescription.WORKINGDIRECTORY);
		setOptional(desc, prop, JobDescription.INTERACTIVE);
		setOptional(desc, prop, JobDescription.INPUT);
		setOptional(desc, prop, JobDescription.OUTPUT);
		setOptional(desc, prop, JobDescription.ERROR);
		setOptMulti(desc, prop, JobDescription.FILETRANSFER);
		setOptional(desc, prop, JobDescription.CLEANUP);
		setOptional(desc, prop, JobDescription.JOBSTARTTIME);
		setOptional(desc, prop, JobDescription.WALLTIMELIMIT);
		setOptional(desc, prop, JobDescription.TOTALCPUTIME);
		setOptional(desc, prop, JobDescription.TOTALPHYSICALMEMORY);
		setOptional(desc, prop, JobDescription.CPUARCHITECTURE);
		setOptional(desc, prop, JobDescription.OPERATINGSYSTEMTYPE);
		setOptMulti(desc, prop, JobDescription.CANDIDATEHOSTS);
		setOptional(desc, prop, JobDescription.QUEUE);
		setOptional(desc, prop, JobDescription.JOBPROJECT);
		setOptMulti(desc, prop, JobDescription.JOBCONTACT);
		return desc;
	}

	private static void setRequired(JobDescription desc, Properties prop, String name) throws Exception {
		String value = prop.getProperty(name);
		if (value != null) {
			desc.setAttribute(name, value);
		} else {
			throw new BadParameterException("Missing required attribute: " + name);
		}
	}

	private static void setOptional(JobDescription desc, Properties prop, String name) throws Exception {
		String value = prop.getProperty(name);
		if (value != null) {
			desc.setAttribute(name, value);
		}
	}

	private static void setOptMulti(JobDescription desc, Properties prop, String name) throws Exception {
		String values = prop.getProperty(name);
		if (values != null) {
			desc.setVectorAttribute(name, values.split(","));
		}
	}

//	private static void copyStream(InputStream in, OutputStream out) throws IOException {
//		byte[] buffer = new byte[1024];
//		for (int len; (len = in.read(buffer)) > 0;) {
//			out.write(buffer, 0, len);
//		}
//	}
}
