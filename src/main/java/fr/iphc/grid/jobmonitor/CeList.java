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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Locale;
import java.util.Properties;
import java.util.Random;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.ogf.saga.error.BadParameterException;
import org.ogf.saga.job.JobDescription;
import org.ogf.saga.job.JobFactory;
import org.ogf.saga.session.Session;
import org.ogf.saga.session.SessionFactory;
import org.ogf.saga.url.URL;
import org.ogf.saga.url.URLFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import fr.in2p3.jsaga.command.AbstractCommand;
import fr.iphc.grid.CeDefinition;
import fr.iphc.grid.MySQLAccess;

public class CeList extends AbstractCommand {
	private static final String OPT_HELP = "h", LONGOPT_HELP = "help";
	// required arguments
	private static final String OPT_RESOURCE = "r", LONGOPT_RESOURCE = "resource";
	// optional arguments
	private static final String OPT_FILE = "f", LONGOPT_FILE = "file";
	private static final String OPT_DESCRIPTION = "d", LONGOPT_DESCRIPTION = "description";
	private static final String OPT_JOBID = "i", LONGOPT_JOBID = "jobid";
	private static final String OPT_BATCH = "b", LONGOPT_BATCH = "batch";
	private static final String OPT_OUTDIR = "o", LONGOPT_OUTDIR = "out_dir";
	private static final String OPT_TIMEOUT = "t", LONGOPT_TIMEOUT = "timeout";
	private static final String OPT_CEPATH = "p", LONGOPT_CEPATH = "cepath";

	protected CeList() {
		super("jsaga-job-celist", null, null, new GnuParser());
	}

	static public Boolean initDirectory(File directory) {
		if (directory == null)
			return false;
		if (!directory.exists())
			return directory.mkdir();
		if (!directory.isDirectory())
			return false;

		String[] list = directory.list();

		// Some JVMs return null for File.list() when the
		// directory is empty.
		if (list != null) {
			for (int i = 0; i < list.length; i++) {
				File entry = new File(directory, list[i]);
				if (entry.isDirectory()) {
					if (!initDirectory(entry))
						return false;
				} else {
					if (!entry.delete())
						return false;
				}
			}
		}
		return true;
	};

	static public ArrayList<URL> AvailableLdapCe() throws Exception {
		ArrayList<URL> CeList = new ArrayList<URL>();
		Hashtable<String, String> env = new Hashtable<String, String>();
		env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		env.put(Context.PROVIDER_URL, "ldap://cclcgtopbdii01.in2p3.fr:2170");
		env.put("java.naming.ldap.attributes.binary", "objectSID");
		try {
			// Create initial context
			DirContext ctx = new InitialDirContext(env);
			SearchControls contraints = new SearchControls();
			contraints.setSearchScope(SearchControls.SUBTREE_SCOPE);
			String[] attributIDs = { "GlueCEUniqueID" };
			contraints.setReturningAttributes(attributIDs);
			String BASE_SEARCH = "Mds-Vo-name=local,o=grid";
			String filter = "(&(objectClass=GlueCE)(GlueCEImplementationName=CREAM)(GlueCEAccessControlBaseRule=VO:biomed))";
			NamingEnumeration<SearchResult> answer = ctx.search(BASE_SEARCH, filter, contraints);
//			int index = 0;
			Random rand = new Random();
			while (answer.hasMore()) {
//				index++;
				SearchResult result = answer.next();
//				Attributes attrs = result.getAttributes();
//				NamingEnumeration f = attrs.getAll();
//				Attribute attr = (Attribute) f.next();
				String line = "cream://" + result.getAttributes().get("GlueCEUniqueID").get() + "?delegationId="
						+ rand.nextLong();
				URL serviceURL = URLFactory.createURL(line);
				CeList.add(serviceURL);
			}
			// Close the context when we're done
			ctx.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		;
		return CeList;
	}

	// "/home/dsa/grid/ce_queue.txt"
	static public ArrayList<URL> AvailableCe(String CePathFile) throws Exception {
		ArrayList<URL> CeList = new ArrayList<URL>();
		// Read Data CE later acces sql database
		BufferedReader br = new BufferedReader(new FileReader(CePathFile));
		String line = null;
		try {
			while ((line = br.readLine()) != null) {
				// Process the data, here we just print it out
				URL serviceURL = URLFactory.createURL(line);
				CeList.add(serviceURL);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
		} finally {
			br.close(); // Close the BufferedReader
		}
		return CeList;
	}

	public static void BilanCe(String OutDir, ArrayList<URL> serviceURL, String TableSql)
			throws ParserConfigurationException, SAXException, IOException, DOMException, ParseException {
		File directory = new File(OutDir);
		ArrayList<CeDefinition> bilan = new ArrayList<CeDefinition>();
		Iterator<URL> i = serviceURL.iterator();

//		XPathExpression expr = null;
		Document doc = null;
		CeDefinition ce = null;
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		while (i.hasNext()) {
			URL url = i.next();
			String file = url.getHost() + "_" + url.getPath().replaceAll("/cream-", "");
			File entry = new File(directory, file + ".out");
			if (entry.exists()) {
				doc = builder.parse(entry);
				ce = new CeDefinition();
				doc.getDocumentElement().normalize();
				NodeList nList = doc.getElementsByTagName("CeHost");
				SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.US);
				for (int temp = 0; temp < nList.getLength(); temp++) {
					Node nNode = nList.item(temp);
					if (nNode.getNodeType() == Node.ELEMENT_NODE) {
						Element eElement = (Element) nNode;
						ce.setDate(dateFormat.parse(eElement.getElementsByTagName("Start").item(0).getTextContent()));
						// System.out.println("Start : " +
						// eElement.getElementsByTagName("Start").item(0).getTextContent());
						ce.setCe(eElement.getElementsByTagName("Ce").item(0).getTextContent());
						ce.setPath(eElement.getElementsByTagName("Path").item(0).getTextContent());
						ce.setJobid(eElement.getElementsByTagName("Jobid").item(0).getTextContent());
						ce.setState(eElement.getElementsByTagName("Status").item(0).getTextContent());
						ce.setTemps(Integer.parseInt(eElement.getElementsByTagName("Time").item(0).getTextContent()));
						ce.setMsg(eElement.getElementsByTagName("Desc").item(0).getTextContent());
					}
				}
				if (ce != null)
					bilan.add(ce);
			}
			entry = new File(directory, file + ".err");
			if (entry.exists()) {
				doc = builder.parse(entry);
				ce = new CeDefinition();
				doc.getDocumentElement().normalize();
				NodeList nList = doc.getElementsByTagName("CeHost");
				SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.US);
				for (int temp = 0; temp < nList.getLength(); temp++) {
					Node nNode = nList.item(temp);
					if (nNode.getNodeType() == Node.ELEMENT_NODE) {
						Element eElement = (Element) nNode;
						ce.setDate(dateFormat.parse(eElement.getElementsByTagName("Start").item(0).getTextContent()));
						ce.setCe(eElement.getElementsByTagName("Ce").item(0).getTextContent());
						ce.setPath(eElement.getElementsByTagName("Path").item(0).getTextContent());
						ce.setState(eElement.getElementsByTagName("Status").item(0).getTextContent());
						ce.setTemps(Integer.parseInt(eElement.getElementsByTagName("Time").item(0).getTextContent()));
						ce.setMsg(eElement.getElementsByTagName("Desc").item(0).getTextContent());
					}
				}
				if (ce != null)
					bilan.add(ce);
			}
			entry = new File(directory, file + ".lock");
			ce = new CeDefinition();
			if (entry.exists()) {
				doc = builder.parse(entry);
				ce = new CeDefinition();
				doc.getDocumentElement().normalize();
				NodeList nList = doc.getElementsByTagName("CeHost");
				SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.US);
				for (int temp = 0; temp < nList.getLength(); temp++) {
					Node nNode = nList.item(temp);
					if (nNode.getNodeType() == Node.ELEMENT_NODE) {
						Element eElement = (Element) nNode;
						ce.setDate(dateFormat.parse(eElement.getElementsByTagName("Start").item(0).getTextContent()));
						ce.setCe(eElement.getElementsByTagName("Ce").item(0).getTextContent());
						ce.setPath(eElement.getElementsByTagName("Path").item(0).getTextContent());
						ce.setState(eElement.getElementsByTagName("Status").item(0).getTextContent());
						ce.setTemps(Integer.parseInt(eElement.getElementsByTagName("Time").item(0).getTextContent()));
						ce.setMsg(eElement.getElementsByTagName("Desc").item(0).getTextContent());
					}
				}
				if (ce != null)
					bilan.add(ce);
			}
		}
		MySQLAccess sql = new MySQLAccess();
		sql.connectDataBase();
		Iterator<CeDefinition> k = bilan.iterator();
		while (k.hasNext()) {
			ce = k.next();
			// System.out.println(ce.getCe() + "\t" + ce.getPath() + "\t" +
			// ce.getDate()
			// + "\t" + ce.getState() + "\t" + ce.getTemps() + "\t" +
			// ce.getMsg()
			// + "\t" + ce.getImplementation());
			sql.writeDataBase(ce, TableSql);
		}
		sql.closeDataBase();
	}

	public static void main(String[] args) throws Exception {

		SessionFactory.createSession(true);
		CeList command = new CeList();
		CommandLine line = command.parse(args);
		Integer timeout = 0;
		String TableSql = "monce";
//		MySQLAccess sql = new MySQLAccess();
		if (line.getOptionValue(OPT_TIMEOUT) == null) {
			timeout = 15;
		} else {
			timeout = Integer.parseInt(line.getOptionValue(OPT_TIMEOUT));
		}
		timeout = timeout * 60; // convertir en secondes
		Date start = new Date();
		String OutDir = line.getOptionValue(OPT_OUTDIR);

		if (OutDir == null) {
			OutDir = "/tmp/thread";
		}
		ArrayList<URL> CeList = null;
		if (line.getOptionValue(OPT_CEPATH) == null) {
			CeList = AvailableLdapCe();
//			for (URL k : CeList) {
//				// System.out.println(k);
//			}
		} else {
			CeList = AvailableCe(line.getOptionValue(OPT_CEPATH));
		}
		Boolean ret = initDirectory(new File(OutDir));
		if (!ret) {
			System.out.println("ERROR: " + OutDir + "STOP");
			System.exit(-1);
		}

		// check if we can connect to the grid
		// try{
		// SessionFactory.createSession(true);
		// }catch(NoSuccessException e){
		// System.err.println("Could not connect to the grid at all
		// ("+e.getMessage()+")");
		// System.err.println("Aborting");
		// System.exit(0);
		//
		// }

		SubmitterThread[] st = new SubmitterThread[CeList.size()];

		Iterator<URL> i = CeList.iterator();
		int index = 0;
		while (i.hasNext()) {
			URL serviceURL = i.next();
			// Ne pas importer dans thread because options.
			Properties prop = new Properties();
			prop.setProperty("Executable", "/bin/hostname");//
			// prop.setProperty("Executable", "touch /dev/null");
			JobDescription desc = createJobDescription(prop);
			desc.setAttribute(JobDescription.INTERACTIVE, "true");
			desc.setAttribute(JobDescription.EXECUTABLE, "/bin/hostname");
			// proxy="/home/dsa/.globus/biomed.txt";
			// Context ctx = ContextFactory.createContext();
			// ctx.setAttribute(Context.TYPE, "VOMS");
			// ctx.setAttribute(Context.USERVO, "biomed");
			// ctx.setAttribute(Context.USERPROXY,proxy);
			// Session session = SessionFactory.createSession(false);
			// session.addContext(ctx);
			Session session = SessionFactory.createSession(true);
			st[index] = new SubmitterThread(serviceURL, session, desc, OutDir, timeout, start);
			st[index].setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
				@Override
				public void uncaughtException(Thread t, Throwable e) {
					// System.out.println("Error! An exception occured in "
					// + t.getName() + ". Cause: " + e.getMessage());
				}
			});
			st[index].start();
			// Thread.sleep(15*1000);
			// test si fichier exist
			// System.out.println("Alive
			// "+OutDir+"/"+serviceURL.getHost()+"_"+serviceURL.getPath().replaceAll("/cream-","")+".out");
			// while ((!((new
			// File(OutDir+"/"+serviceURL.getHost()+"_"+serviceURL.getPath().replaceAll("/cream-","")+".out").exists())
			// ||
			// (new
			// File(OutDir+"/"+serviceURL.getHost()+"_"+serviceURL.getPath().replaceAll("/cream-","")+".err").exists()))))
			// {
			// Thread.sleep(500);
			// }
			// System.out.println("Alive "+serviceURL.getHost()+"-"+ index+"FILE
			// EXIST");
			index++;

		}
		;

		// System.out.println("BEGIN LOOP: Max " + index);
		long date_start = System.currentTimeMillis();
		// System.out.println("BEGIN START: " + date_start);
		Integer time_out = (timeout + 180) * 1000; // unité ms value in minute
													// +120
													// =delta par rapport thread
		Boolean Alive = true;
//		int nb = 0;
		long now = System.currentTimeMillis();
		do {
			now = System.currentTimeMillis();
			Alive = false;
//			nb = 0;
			for (int j = 0; j < index; j++) {
				if (st[j].isAlive()) {
					// System.out.println("Alive "+j);
					Alive = true;
//					nb++;
				}
			}
			// System.out.println(nb);
			Thread.sleep(10000);
		} while ((Alive) && ((now - date_start) < time_out));

		for (int j = 0; j < index; j++) {
			if (st[j].isAlive()) {
				st[j].Requeststop();
			}
		}
		BilanCe(OutDir, CeList, TableSql);
		jobManagerLdap jm = new jobManagerLdap();
		jm.updateLdapCe();
		System.out.println("END " + new Date());
		// faire un traitement...
		System.exit(0);
	}

	// ----------------- OPTIONS
	// ------------------------------------------------------------
	@SuppressWarnings("static-access")
	protected Options createOptions() {
		Options opt = new Options();
		/*
		 * opt.addOption(OptionBuilder.create()); OptionBuilder.withDescription(
		 * "Display this help and exit");
		 * OptionBuilder.withLongOpt(LONGOPT_HELP);
		 * opt.addOption(OptionBuilder.create(OPT_HELP));
		 * 
		 * 
		 * OptionBuilder.create(); OptionBuilder.withDescription(
		 * "Output file directory"); OptionBuilder.hasArg();
		 * OptionBuilder.withArgName("OutDir");
		 * OptionBuilder.withLongOpt(LONGOPT_OUTDIR);
		 * opt.addOption(OptionBuilder.create(OPT_OUTDIR));
		 * 
		 * OptionBuilder.create(); OptionBuilder.withDescription(
		 * "Waiting TimeOut in minutes"); OptionBuilder.hasArg();
		 * OptionBuilder.withArgName("timeout");
		 * OptionBuilder.withLongOpt(LONGOPT_TIMEOUT);
		 * opt.addOption(OptionBuilder.create(OPT_TIMEOUT));
		 * 
		 * 
		 * OptionGroup reqGroup = new OptionGroup(); OptionBuilder.create();
		 * OptionBuilder.withDescription("read job description from file <path>"
		 * ); OptionBuilder.hasArg(); OptionBuilder.withArgName("path");
		 * OptionBuilder.withLongOpt(LONGOPT_FILE);
		 * reqGroup.addOption(OptionBuilder.create(OPT_FILE));
		 * OptionBuilder.create(); OptionBuilder.withDescription(
		 * "command to execute"); OptionBuilder.hasArg();
		 * reqGroup.addOption(OptionBuilder.create(JobDescription.EXECUTABLE));
		 * reqGroup.setRequired(false); opt.addOptionGroup(reqGroup);
		 */

		// command
		opt.addOption(
				OptionBuilder.withDescription("Display this help and exit").withLongOpt(LONGOPT_HELP).create(OPT_HELP));

		// required arguments
		opt.addOption(OptionBuilder.withDescription("the URL of the job service").hasArg().withArgName("URL")
				.withLongOpt(LONGOPT_RESOURCE).create(OPT_RESOURCE));

		opt.addOption(
				OptionBuilder
						.withDescription("generate the job description in the targeted grid language "
								+ "and exit (do not submit the job)")
						.withLongOpt(LONGOPT_DESCRIPTION).create(OPT_DESCRIPTION));

		opt.addOption(OptionBuilder.withDescription("Output file directory").hasArg().withArgName("OutDir")
				.withLongOpt(LONGOPT_OUTDIR).create(OPT_OUTDIR));

		opt.addOption(OptionBuilder.withDescription("Waiting TimeOut in minutes").hasArg().withArgName("timeout")
				.withLongOpt(LONGOPT_TIMEOUT).create(OPT_TIMEOUT));

		opt.addOption(OptionBuilder.withDescription("File with CE/Path Format: CREAM://host:8443/path").hasArg()
				.withArgName("ce_path").withLongOpt(LONGOPT_CEPATH).create(OPT_CEPATH));

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

		OptionGroup reqGroup = new OptionGroup();
		reqGroup.addOption(OptionBuilder.withDescription("read job description from file <path>").hasArg()
				.withArgName("path").withLongOpt(LONGOPT_FILE).create(OPT_FILE));
		reqGroup.addOption(o("command to execute").hasArg().create(JobDescription.EXECUTABLE));
		// reqGroup.setRequired(true);
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

}
