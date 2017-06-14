package com.tool4us.task;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.LinkedBlockingQueue;

import com.tool4us.logging.Logs;



/**
 * 작업을 수행하기 위한 스레드 풀을 가지고 있고 수행해야 할 작업이 들어 오면 작업 스레드가 자동으로 호출되어
 * 작업을 실행하는 작업 관리 클래스.
 * 
 * @author TurboK
 */
public class TaskQueue
{
	/// 수행해야 할 작업 관리 큐. 작업 스레드가 바라보고 있는 큐임.
	private final BlockingQueue<ITask> _taskQueue = new LinkedBlockingQueue<ITask>();
	
	/// 작업 수행 컨트롤링으로 인하여 따로 대기 목록에 관리하는 큐
	private final BlockingQueue<ITask> _waitingJobs = new LinkedBlockingQueue<ITask>();
	
	/// 교체해야 할 작업을 관리
	private final Map<String, ITask>   _replacedJobs = new ConcurrentSkipListMap<String, ITask>();
	
	/// 수행 중인 작업 개수
	private Integer			   _workingJobNum = new Integer(0);

	/// 작업 스레드 관리 멤버
	private List<Thread>       _runService = null;
	
	private boolean            _running = false;
	
	private ITaskMonitor	   _monitor = null;
	
	private Object			   _waitAll = new Object();
	
	/// 마지막에 Waiting Queue(_waitingJobs)에 들어 간 아이템.
	/// 작업 구분자가 여러 개 들어 가는 것을 방지하기 위하여 사용함.
	private ITask              _lastWaitingJob = null;
	
	/// 잠깐 대기 모드
	private boolean            _holdOn = false;
	
	private List<ITask>        _holdingJobs = null;
	
	
	/**
	 * TaskQueue 작업 흐름을 조절하기 위한 클래스
	 * @author TurboK
	 *
	 */
	public class ControlTask extends ITask
	{
	    private int    _code = 0;
	    
	    public ControlTask(int code) { _code = code; }
	    
	    public int getCode() { return _code; }

        @Override
        public boolean isPossibleToRun() { return true; }

        @Override
        public String toString() { return "ControlTask : " + _code; }

        @Override
        public void run() throws Exception {}
	}

	
	public TaskQueue(ITaskMonitor taskController)
	{
		_monitor = taskController;
	}

	// 모든 작업이 모두 끝날 때까지 대기하는 메소드
	public void waitAllDone()
	{
	    // 이미 모든 작업이 끝난 상태이므로 그냥 리턴함
		if( _waitingJobs.isEmpty() && _taskQueue.isEmpty() && _workingJobNum <= 0 )
			return;
		
		try
		{
			synchronized(_waitAll)
			{
			    while( !(_waitingJobs.isEmpty() && _taskQueue.isEmpty() && _workingJobNum <= 0) )
			        _waitAll.wait();
			}
		}
		catch( InterruptedException e )
		{
		    Logs.trace(e);
		}
		catch( Exception e )
        {
		    Logs.trace(e);
        }
	}
	
	/**
	 * 앞으로 추가될 작업을 잠시 대기시키고자 할 때 holdJob(true)를 호출함.
	 * 대기 중인 작업을 실행시키려면 holdJob(false)를  호출
	 * @param hold     앞으로 추가될 작업을 바로 실행하지 말고 잠시 실행 대기시킬 지 여부
	 */
	public void holdJob(boolean hold)
	{
	    _holdOn = hold;
	    
	    if( !hold && _holdingJobs != null )
	    {
	        for(ITask task : _holdingJobs)
	            this.pushTask(task);

	        _holdingJobs.clear();
	    }
	}
	
	private void addNewWorker(String queueName)
	{
	    Thread worker = new Thread()
	    {
	        @Override
            public void run()
            {
                while( _running && !Thread.currentThread().isInterrupted() )
                {
                    ITask taskItem = null;
                    
                    try
                    {
                        taskItem = take();

                        // 종료 시그널
                        if( taskItem == null ) break;

                        // System.err.println("RUN : " + taskItem.toString() + "  COUNT: " + _taskQueue.size());
                        
                        OnBeginTask(taskItem);
                        
                        if( _monitor != null )
                            _monitor.OnStartTask(taskItem);
                        
                        taskItem.run();
                        
                        // System.err.println("END : " + taskItem.toString() + "  COUNT: " + _taskQueue.size());

                        if( _monitor != null )   
                            _monitor.OnEndTask(taskItem);
                    }
                    catch( InterruptedException ex )
                    {   
                        break; // 이 Exception은 Thread 강제 종료시 날 수 있으므로 루프를 중지해야 함
                    }
                    catch( OutOfMemoryError ex )
                    {
                        if( _monitor != null )
                            _monitor.OnErrorRaised(taskItem, ex);
                        else
                            Logs.trace(ex);
                    }
                    catch( Throwable ex )
                    {
                        if( _monitor != null )
                            _monitor.OnErrorRaised(taskItem, ex);
                        else
                            Logs.trace(ex);
                    }
                    finally
                    {
                        OnDoneTask(taskItem);
                    }
                }
                
                System.out.println("Working Thread [" + this.getName() + "] in TaskQueue Finished.");
            }
	    };
	    
	    if( queueName != null )
	        worker.setName(queueName + "-" + _runService.size());
	    else
	        worker.setName("TaskQueue-" + _runService.size());
	    
        _runService.add(worker);

        worker.start();
	}
	
	public void startQueue(int workThreadNum)
	{
	    startQueue(workThreadNum, null);
	}
	
	public void startQueue(int workThreadNum, String queueName)
	{
	    _running = true;
	    
	    if( workThreadNum <= 0 )
	        workThreadNum = 1;

	    _runService = new ArrayList<Thread>();
        
        for(int i = 0; i < workThreadNum; ++i)
        {
            addNewWorker(queueName);
        }
	}

	public void endQueue()
	{
		_running = false;
		
		_taskQueue.clear();
		
		if( _runService != null )
		{
			try
			{
			    for(Thread worker : _runService)
			        worker.interrupt();
			}
			catch( Exception e )
			{
				//
			}
		}
		
		_runService = null;
		_workingJobNum = 0;
		
		synchronized(_waitAll) { _waitAll.notifyAll(); }
	}

	/**
	 * 수행할 작업 추가.
	 * @param task
	 */
	public void pushTask(ITask task)
	{
		if( task != null )
			task.setTaskMonitor(_monitor);
		
		if( !_waitingJobs.isEmpty() )
		{
		    _lastWaitingJob = task;
	        _waitingJobs.add(task);
		}
		else
		{
		    _replacedJobs.remove(task.toString());

		    if( _holdOn )
		    {
		        if( _holdingJobs == null )
		            _holdingJobs = new ArrayList<ITask>();
		        
		         _holdingJobs.add(task);
		    }
		    else
		        _taskQueue.add(task);
		}
	}
	
	/**
	 * 교체용 작업으로 넣음.
	 */
	public void pushAsReplacement(ITask task)
	{
	    _replacedJobs.put(task.toString(), task);
	}
	
	public ITask take() throws InterruptedException
	{
	    ITask task = _taskQueue.take();

	    ITask replacedTask = _replacedJobs.remove(task.toString());
	    
	    if( replacedTask != null )
	        task = replacedTask;

	    return task;
	}
	
	/** 현재 입력된 작업의 개수 반환 */
	public int getTaskCount()
    {
        return _taskQueue.size() + _waitingJobs.size();
    }
	
	/**
	 * 현재 들어 가 있는 작업이 모두 끝난 후 작업이 실행되도록 하는 Waiting Task 추가
	 */
	public void pushWaitTask()
	{
	    if( !(_lastWaitingJob instanceof ControlTask)
	            && (!_taskQueue.isEmpty() || _workingJobNum > 0) )
	    {
	        ITask jobSep = new ControlTask(1);
	        _waitingJobs.add( jobSep );
	        _lastWaitingJob = jobSep;
	    }
	}
	
	private synchronized void OnBeginTask(ITask task)
	{
	    ++_workingJobNum;
	}
	
    private synchronized void OnDoneTask(ITask task)
    {
        if( _workingJobNum > 0 )
            --_workingJobNum;

	    if( !_taskQueue.isEmpty() || _workingJobNum > 0 )
	        return;
	    
        if( _waitingJobs.isEmpty() )
        {
            synchronized(_waitAll)
            {
                _waitAll.notifyAll();
            }
        }
        else
        {
            // 처음 하나는 ControlTask 이므로 버림
            ITask taskItem = _waitingJobs.poll();
            
            while( null != (taskItem = _waitingJobs.peek()) )
            {
                if( taskItem instanceof ControlTask )
                    break;

                _taskQueue.add(taskItem);

                _waitingJobs.poll();
            }

            if( _taskQueue.isEmpty() && _workingJobNum <= 0 )
                synchronized(_waitAll) { _waitAll.notifyAll(); }
        }
    }

    public void clearAllJob()
    {
        _taskQueue.clear();
        _waitingJobs.clear();
        _replacedJobs.clear();
    }
    
}
