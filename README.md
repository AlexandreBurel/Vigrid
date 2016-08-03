# Vigrid
Vigrid is a computer program whose purpose is to submit jobs on a computing grid using the JSAGA API. It is composed of two modules, a JobMonitor meant to run as a service to check periodically the state of each nodes on the grid, and a JobManager that will send the jobs on the best nodes at the moment of the submission.

**Authors**
* Alexandre Burel, CNRS - IPHC - LSMBO
* Patrick Guterl, CNRS - IPHC - LSMBO

**Details**

Vigrid has been developped for MSDA (https://msda.unistra.fr), an online plateform for proteomic tools.<br />
Searching for proteins in fasta files or using a *de novo* approach requires time and powerful computing resources. Using grid computing reduces the amount of time required by deporting the computation on the available grid nodes.<br />
Vigrid makes sure that every job sent will be treated. If a grid node (or Computing Element, CE) is not able to compute the requested task (missing dependencies, not enough space, bad environment...), Vigrid will send it on another node.

**Job monitor**
```
usage: jsaga-job-run [-Arguments <arg>] [-b | -i] [-CandidateHosts <arg>]
       [-Cleanup <arg>] [-CPUArchitecture <arg>] [-d] [-Environment <arg>]
       [-Error <arg>] -Executable <arg> | -f <path>  [-FileTransfer <arg>] [-h]
       [-Input <arg>] [-Interactive] [-JobContact <arg>] [-JobProject <arg>]
       [-JobStartTime <arg>] [-NumberOfProcesses <arg>] [-OperatingSystemType
       <arg>] [-Output <arg>] [-ProcessesPerHost <arg>] [-Queue <arg>] -r <URL>
       [-SPMDVariation <arg>] [-ThreadsPerProcess <arg>] [-TotalCPUCount <arg>]
       [-TotalCPUTime <arg>] [-TotalPhysicalMemory <arg>] [-WallTimeLimit <arg>]
       [-WorkingDirectory <arg>]

where:
 -Arguments <arg>             positional parameters for the command
 -b,--batch                   print the job identifier as soon as it is
                              submitted, and exit immediatly.
 -CandidateHosts <arg>        list of host names which are to be
                              considered by the resource manager as candidate targets
 -Cleanup <arg>               defines if output files get removed after
                              the job finishes
 -CPUArchitecture <arg>       compatible processor for job submission
 -d,--description             generate the job description in the targeted
                              grid language and exit (do not submit the job)
 -Environment <arg>           set of environment variables for the job
 -Error <arg>                 pathname of the standard error file
 -Executable <arg>            command to execute
 -f,--file <path>             read job description from file <path>
 -FileTransfer <arg>          a list of file transfer directives
 -h,--help                    Display this help and exit
 -i,--jobid                   print the job identifier as soon as it is
                              submitted, and wait for it to be finished
 -Input <arg>                 pathname of the standard input file
 -Interactive                 run the job in interactive mode
 -JobContact <arg>            set of endpoints describing where to report
 -JobProject <arg>            name of an account or project name
 -JobStartTime <arg>          time at which a job should be scheduled
 -NumberOfProcesses <arg>     number of process instances to start
 -OperatingSystemType <arg>   compatible operating system for job
                              submission
 -Output <arg>                pathname of the standard output file
 -ProcessesPerHost <arg>      number of processes to start per host
 -Queue <arg>                 name of a queue to place the job into
 -r,--resource <URL>          the URL of the job service
 -SPMDVariation <arg>         SPMD job type and startup mechanism
 -ThreadsPerProcess <arg>     expected number of threads per process
 -TotalCPUCount <arg>         total number of cpus requested for this job
 -TotalCPUTime <arg>          estimated total number of CPU seconds which
                              the job will require
 -TotalPhysicalMemory <arg>   estimated amount of memory the job requires
 -WallTimeLimit <arg>         hard limit for the total job runtime
 -WorkingDirectory <arg>      working directory for the job

```


**Job manager**
```
usage: jsaga-jobmanager [-Arguments <arg>] [-b <arg>] [-c <arg>]
       [-CandidateHosts <arg>] [-Cleanup <arg>] [-CPUArchitecture <arg>] [-d] [-e
       <arg>] [-Environment <arg>] [-Error <arg>] -f <file> | -l <list>
       [-FileTransfer <arg>] [-h] [-i] [-Input <arg>] [-Interactive] [-JobContact
       <arg>] [-JobProject <arg>] [-JobStartTime <arg>]  [-NumberOfProcesses
       <arg>] [-OperatingSystemType <arg>] [-Output <arg>] [-ProcessesPerHost
       <arg>] [-q <queue>] [-Queue <arg>] [-r <arg>] [-s <arg>] [-SPMDVariation
       <arg>] [-t] [-ThreadsPerProcess <arg>] [-TotalCPUCount <arg>]
       [-TotalCPUTime <arg>] [-TotalPhysicalMemory <arg>] [-v <arg>] [-w <arg>]
       [-WallTimeLimit <arg>] [-WorkingDirectory <arg>

where:
 -Arguments <arg>             positional parameters for the command
 -b,--bad <arg>               File List of Bad Ce elminate from the Ce
                              List[IPHC]
 -c,--cwd <arg>               Define working directory for monitoring file
                              (default cwd) [IPHC]
 -CandidateHosts <arg>        list of host names which are to be
                              considered by the resource manager as candidate targets
 -Cleanup <arg>               defines if output files get removed after
                              the job finishes
 -CPUArchitecture <arg>       compatible processor for job submission
 -d,--description             generate the job description in the targeted
                              grid language and exit (do not submit the job)
 -e,--end <arg>               Time -> END Program in minutes/hour (Default
                              10H) Value: mM hH  [IPHC]
 -Environment <arg>           set of environment variables for the job
 -Error <arg>                 pathname of the standard error file
 -f,--job <file>              read job description from file <path> [IPHC]
 -FileTransfer <arg>          a list of file transfer directives
 -h,--help                    Display this help and exit
 -i,--jobid                   print the job identifier as soon as it is
                              submitted, and wait for it to be finished
 -Input <arg>                 pathname of the standard input file
 -Interactive                 run the job in interactive mode
 -JobContact <arg>            set of endpoints describing where to report
 -JobProject <arg>            name of an account or project name
 -JobStartTime <arg>          time at which a job should be scheduled
 -l,--list <list>             read file include jdl file [IPHC]
 -NumberOfProcesses <arg>     number of process instances to start
 -OperatingSystemType <arg>   compatible operating system for job
                              submission
 -Output <arg>                pathname of the standard output file
 -ProcessesPerHost <arg>      number of processes to start per host
 -q,--queue <queue>           file with cream URL [IPHC]
 -Queue <arg>                 name of a queue to place the job into
 -r,--run <arg>               Duration Run -Thread in minutes/hour
                              (Default 1H) Value: mM hH  [IPHC]
 -s,--setup <arg>             Setup xml file Global paramters (default
                              setup) [IPHC]
 -SPMDVariation <arg>         SPMD job type and startup mechanism
 -t,--opto                    Disable Optimize Timeout Run Average
                              execution time[IPHC]
 -ThreadsPerProcess <arg>     expected number of threads per process
 -TotalCPUCount <arg>         total number of cpus requested for this job
 -TotalCPUTime <arg>          estimated total number of CPU seconds which
                              the job will require
 -TotalPhysicalMemory <arg>   estimated amount of memory the job requires
 -v,--logd <arg>              Threshold display full log in log file[IPHC]
 -w,--wait <arg>              Time queue wating in CE/queue in minutes
                              (Default 15 mn) [IPHC]
 -WallTimeLimit <arg>         hard limit for the total job runtime
 -WorkingDirectory <arg>      working directory for the job
```


