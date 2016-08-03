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
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ogf.saga.error.BadParameterException;
import org.ogf.saga.error.NoSuccessException;
import org.ogf.saga.job.Job;
import org.ogf.saga.job.JobFactory;
import org.ogf.saga.job.JobService;
import org.ogf.saga.session.Session;
import org.ogf.saga.session.SessionFactory;
// import javax.mail.Message;
//import javax.mail.MessagingException;
//import javax.mail.Session;
//import javax.mail.Transport;
//import javax.mail.internet.InternetAddress;
//import javax.mail.internet.MimeMessage;
import org.ogf.saga.url.URL;
import org.ogf.saga.url.URLFactory;

//import fr.in2p3.jsaga.impl.job.instance.JobImpl;
import fr.iphc.grid.Global;


public class JobThread extends Thread {
	private ArrayList<Url> m_ListUrl;
	private ArrayList<Jdl> m_ListJob;
	private boolean stopThread = false;
	private Integer max_loop=0;


	
	JobThread (ArrayList<Jdl> ListJob,ArrayList<Url> ListUrl) {	
		m_ListUrl= ListUrl;
		m_ListJob= ListJob;
	}
	static void jobCancel(String Ce) {
		Pattern pattern = Pattern.compile("\\[(.*)\\]-\\[(.*)\\]");
		URL serviceURL=null;
    String nativeJobId=null;
    Matcher matcher = pattern.matcher(Ce);
    if (matcher.find()) {
    	try {
    		serviceURL = URLFactory.createURL(matcher.group(1));
    		nativeJobId = matcher.group(2);
    	} catch (Exception e) {e.printStackTrace();} 
     } else {
    	  System.err.println ("Job ID does not match regular expression: "+pattern.pattern());
    	  return;
     }               
//              // get job
      Session session;
      JobService service=null;
			try {
				session = SessionFactory.createSession(true);						
				service = JobFactory.createJobService(session, serviceURL);
				Job job = service.getJob(nativeJobId);
				job.cancel();
			} catch (Exception e) {
				System.err.println("URL:"+serviceURL+" CreamID:"+nativeJobId+"\nMsg: "+e.getMessage());
				return;
			}
		return;
	}
	
	private static String jobStatus(Jdl job) {
		String answer=null;
		String cmd[]={"/usr/local/iphc/grid/bin/iphc-job-status.sh",job.getJobId()};
// 		System.out.println("PG CA SE PASSE:"+job.getJobId());
		try {
			Process pr = Runtime.getRuntime().exec(cmd);
			Integer statusReturn=pr.waitFor();
			if (statusReturn == 0) {
				BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream()));
				String ligne=null;
				StringBuilder s = new StringBuilder();
				while((ligne=input.readLine()) != null) {
					s.append(ligne);
				}
//				System.out.println("ALL_JOBSTATUS: "+s.toString());
				return(s.toString());
			} else {
				BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream()));
				String ligne=null;
				StringBuilder s = new StringBuilder();
				while((ligne=input.readLine()) != null) {
					s.append(ligne);
				}
				throw new BadParameterException("Job status failed:"+s.toString());
			}
		} catch (Exception e1) {e1.printStackTrace();}
		return (answer);
	}
	
//This method is called when the thread runs
		public void run() {
			
			GetOutputThread[] get_th=new GetOutputThread[10];
			GetOutputThread st=null;
			Integer index=0;
//			Boolean end=false;
			Boolean fin=false;
			Date now=new Date();
//			statusCe stce=new statusCe();
			while (! fin) {
//				end=true;	
				ArrayList<RunThread> array_thread=new ArrayList<RunThread>();
				Integer size=m_ListUrl.size();
//				ExecutorService executor=Executors.newFixedThreadPool(Global.NBTHREAD);
//			Test if array Url not null
				if (size == 0){
					System.err.println("List Url empty Exit");
					Initialize init=new Initialize();
					try {
						init.InitUrl(Global.file);
						size=m_ListUrl.size();
					} catch (Exception e) {System.err.println("Thread JobThread init.InitUrl\n"+e.getMessage());}
				}
//				System.out.println("BEGIN FOR JDL");	
				for (Jdl job:m_ListJob) {
					if (job.getStatus().equals("INIT")) {
						Url url=null;		
						if ((job.getFail() >0) && (job.getFail() <= Global.SosCe.size())) {							
							url=Global.SosCe.get(job.getFail()-1);							
						} else {
							url=m_ListUrl.get(index%size); //associe url à un job
							index++;
						}
// 					System.out.println("TEST LOOP "+ job.getId()+" "+url.getUrl()+" idx: "+index);
						//associe job et CE
						Session session=null;
						try {
							session = SessionFactory.createSession(true);
						} catch (NoSuccessException e) { System.err.println("SessionFactory failed\n"+e.getMessage());}
						RunThread run=new RunThread(job,url,session);
						run.setName(url.getUrl().toString());
						array_thread.add(run);
						run.start();
//						end=false;
//							if ((index % Global.MAXTHREAD) == 0) {
//							System.out.println("SIZE: "+array_thread.size()+" "+Global.MAXTHREAD);
						while (array_thread.size() >= Global.MAXTHREAD)  {
							Stack<RunThread> idx=new Stack<RunThread>();
							for (RunThread run_st:array_thread){
// 								System.out.println("THREAD RUN STATE:"+run_st.getState()+" Alive:"+run_st.isAlive()+array_thread.size());
								if (!run_st.isAlive()){
									idx.push(run_st);
								}
							}
							while (!idx.isEmpty()) {
								array_thread.remove(idx.pop());
							}
						}
					} // if Phase init				
				}	 //for (Jdl job:m_ListJob) 
				Date start_loop= new Date();
				while ((array_thread.size() >0))  {
// 					System.out.println("HALT_OUT: "+array_thread.size()+" "+Global.MAXTHREAD);
					Stack<RunThread> idx=new Stack<RunThread>();
					for (RunThread run_st:array_thread){								
						if (!run_st.isAlive()){
							idx.push(run_st);
							continue;
						} 
						if ((new Date().getTime()-start_loop.getTime()> (600*1000)) 
  						&& (run_st.getState().toString().compareTo("RUNNABLE") == 0)){  // 10 mn en ms
							run_st.interrupt();
							idx.push(run_st);
							System.out.println("HANG_DISABLE: "+run_st.getName()+" - "+run_st.getState());
						}
						if (new Date().getTime()-start_loop.getTime()> (30*1000)) {  // 30s mn en ms
  					long delta=new Date().getTime()-start_loop.getTime()/1000;//
  						System.out.println("HANG: "+run_st.getName()+" - "+run_st.getState()+"-"+run_st.isInterrupted()
  							+" "+delta+" sec");
						}
					}

					while (!idx.isEmpty()) {
						array_thread.remove(idx.pop());
					}
				} //END while ((array_thread.size() >0))
				Integer nb_end=0;
				Integer nb_load=0;
				Integer nb_end_aver=0;
				Integer nb_job_aver=0;
				Float exec_time=(float) 0.0;
				now=new Date();
//	Calculate timeout value= average execution time jobs if end_job/nb_job> threshold		
				if (Global.OPTTIMEOUTRUN) {
					for (Jdl job:Global.ListJob){
						nb_job_aver++;
						if (job.getStatus().equals("END")){
							nb_end_aver++;
							exec_time=exec_time+(job.getTimeoutRun());
						}
					}
					if ((float)nb_end_aver/(float)nb_job_aver >= 0.8) {
						float average=((exec_time/(float)nb_end_aver)/1000);
						average=average+(average/2);
						for (Jdl job:Global.ListJob){
							if (!(job.getStatus().equals("END"))){
								job.setTimeoutRun((int) average);
							}
						}
						Global.OPTTIMEOUTRUN=false;
					}
				}
				for (Jdl job:Global.ListJob) {	
					if ((job.getJobId() != null) && ((job.getStatus().equals("BEGIN")) ||
								( job.getStatus().startsWith("JSAGA:")) )) {
						String status = jobStatus(job);
//						System.out.println("PG227JobTHREAD:"+status);
						if ((status != null) && (status.startsWith("JSAGA:"))) {

							job.setStatus(status);
						} else {
							job.setStatus("FAILED");
						}
					}
									
					if ((job.getStatus().equals("BEGIN"))&& (now.getTime()-job.getStart().getTime()>(job.getTimeoutWait()*1000))){
						jobCancel(job.getJobId()); 
 						reinit(job, m_ListUrl);
					}
					if (job.getStatus().equals("FAILED")){						
 						reinit(job, m_ListUrl);	
					}
					if (((job.getStatus().equals("JSAGA:RUNNING_SUBMITTED")) ||
							(job.getStatus().equals("JSAGA:RUNNING_QUEUED")))		&&
							(now.getTime()-job.getStart().getTime()>(job.getTimeoutWait()*1000))){
						System.err.println("TIMEOUT WAITING: "+job.getId()+" "+job.getName()+" "+job.getJobId());
 						jobCancel(job.getJobId());
 						reinit(job, m_ListUrl);						
					}
					if ((job.getStatus().equals("JSAGA:RUNNING_ACTIVE"))&& (now.getTime()-job.getStart().getTime()>(job.getTimeoutRun()*1000))){
						jobCancel(job.getJobId()); 
						System.err.println("TIMEOUT RUNNING: "+job.getId());
 						reinit(job, m_ListUrl);
					}
					if (((job.getStatus().equals("JSAGA:CANCELED")) ||
							(job.getStatus().equals("JSAGA:CANCEL_REQUESTED")) ||
							(job.getStatus().equals("JSAGA:FAILED_ERROR")) ||
							(job.getStatus().equals("JSAGA:FAILED_ABORTED")))){
						reinit(job, m_ListUrl);						
					}
					if (job.getStatus().equals("JSAGA:DONE")){
				// memorisé thread download pb du à la boucle etat intermédiaire
						for(Integer i=0; i<get_th.length; i++){							
							st=get_th[i];
							if ((st ==null || !st.isAlive()) && (!job.getStatus().equals("LOAD"))){
								st=null;
								job.setStatus("LOAD");//
//							System.out.println("LOADTH"+job.getId()+"\n");				
								st=new GetOutputThread(job);
								st.start();
								while (!st.isAlive()){
								}
								get_th[i]=st;
								break;
							} //fin If
						} //fin for
				}				
				if (job.getStatus().equals("LOAD")) {
					nb_load++;
				}				
				if (job.getStatus().equals("END")) {
					nb_end++;
				}
			} // End for
			Boolean no_fail=true;	
			Url badUrl=null;
			while (no_fail) {
				no_fail=false;
				for (Url url:m_ListUrl){
					if (url.getFailed() >= 5){
						badUrl=url;
						break;		
					}
				}
				if (badUrl != null){
					Global.ListUrl.remove(badUrl);
					badUrl= null;
					no_fail=true;
				}
			}


// Traitement si un thread load reste en attente infini
//			if (((nb_end+nb_load) == m_ListJob.size()) && (nb_load > 0)) {
// 					try {
//						Thread.sleep(Global.WAITLOAD);
//						Global.WAITLOAD=Global.WAITLOAD*2;
//					} catch (InterruptedException e) {e.printStackTrace();}
// 			};
 			if (Global.WAITLOAD > 300*1000){ //5 mn attente load hang
// 				System.out.println("PGSENDMAIL:"+nb_load+" WAIT"+Global.WAITLOAD+" FULL"+nb_end);
//				Properties props = new Properties();
//				props.setProperty("mail.from","iphc_jobmanager@unistra.fr");
//				Session session = Session.getDefaultInstance( props, null );
//				MimeMessage message = new MimeMessage( session );
//				try {
//					message.addRecipient(Message.RecipientType.TO, new InternetAddress("guterlp@unistra.fr;a.burel@unistra.fr"));
//					message.setSubject( "IPHC MANAGER LOAD HANG" );
//					message.setText("HANG DOWNLOAD FILE");
//					Transport.send( message );	
//					for (Jdl job:m_ListJob) {
//						if (!(job.getStatus().equals("END"))) {
//							System.err.println("PGLOADPRINT:"+job.getName()+" CWD:"+Global.Cwd);
//							PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(Global.Cwd+"/loadFailed.txt", true)));
//					    out.println(Global.Cwd+"/"+job.getName());
//					    out.close();
//						}
//					}
//					System.err.println("ERROR LOAD HANG");
//					System.exit(-2);
//				} catch (Exception e) {e.printStackTrace();}
 			}
			if (nb_end == m_ListJob.size())	{ 
				Global.END=true;
			}	
			fin=this.stopThread;
			long startTime = System.currentTimeMillis();
			long endTime = startTime+(60*1000);          
			while(System.currentTimeMillis()<endTime){ }
		} //While true
	}
			public synchronized void halt() {
				this.stopThread = true;
 				System.out.println("PGMAX LOOP:"+max_loop);
		}
		
//		@SuppressWarnings("unchecked")
		private  void reinit(Jdl job,ArrayList<Url> ListUrl)  {
// 			System.err.println("REINTI "+ job.getId()+" "+job.getName()+" St: "+job.getStatus()+"Date:"+ new Date());	
			Url url=null;
 			for (Url ce: ListUrl) {
 				if (job.getNodeCe().equals(ce.getUrl())){
 					url=ce;
 					break;
 				}	
 			}			
			if (url != null) {
//				m_ListUrl.remove(url);
			}
			
			url=null;
 			for (Url ce: Global.SosCe) {
 				if (job.getNodeCe().equals(ce.getUrl())){
 					url=ce;
					break;
 				}	
 			}			
			
 			if (url != null) {
				Global.SosCe.remove(url);
			}
			
			job.setStatus("INIT");
 			job.setStart(null);
 			job.setNodeCe(null);
 			job.setJobId(null);
				job.setFail(job.getFail()+1);
 			if (!Global.OPTTIMEOUTRUN){
 				job.setTimeoutRun((int)(job.getTimeoutRun()*1.5));
 			}
 			ListUrl=null;
		}


}
