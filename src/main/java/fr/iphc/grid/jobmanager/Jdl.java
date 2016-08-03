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

import java.util.Date;

import org.ogf.saga.job.JobDescription;
import org.ogf.saga.url.URL;

import fr.iphc.grid.Global;

public class Jdl {
	Integer id;
	String name;
	JobDescription desc;
	String status;
	Date start;
	Integer fail;
	Integer timeoutWait;
	Integer timeoutRun;
	URL nodeCe;
	String jobId;
	RunThread runthread;



	public Jdl (Integer id) {
		this.id=id;
		this.status="INIT";
		this.nodeCe=null;
		this.timeoutWait=Global.TIMEOUTWAIT;
		this.timeoutRun=Global.TIMEOUTRUN;
		this.fail=0;
		this.runthread=null;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Date getStart() {
		return start;
	}

	public void setStart(Date start) {
		this.start = start;
	}

	public Integer getTimeoutWait() {
		return timeoutWait;
	}

	public void setTimeoutWait(Integer timeoutWait) {
		this.timeoutWait = timeoutWait;
	}

	public Integer getTimeoutRun() {
		return timeoutRun;
	}

	public void setTimeoutRun(Integer timeoutRun) {
		this.timeoutRun = timeoutRun;
	}

	public URL getNodeCe() {
		return nodeCe;
	}

	public void setNodeCe(URL nodeCe) {
		this.nodeCe = nodeCe;
	}

	public String getJobId() {
		return jobId;
	}

	public void setJobId(String jobId) {
		this.jobId = jobId;
	}

	public JobDescription getDesc() {
		return desc;
	}

	public void setDesc(JobDescription desc) {
		this.desc = desc;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Integer getFail() {
		return fail;
	}

	public void setFail(Integer fail) {
		this.fail = fail;
	}

	public RunThread getRunthread() {
		return runthread;
	}

	public void setRunthread(RunThread runthread) {
		this.runthread = runthread;
	}


}
