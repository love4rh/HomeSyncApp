package com.tool4us.task;



public interface ITaskMonitor
{
	/**
	 * 작업 계속 진행여부 반환. false가 반환되면 현재 작업 중인 것을 중단하고 루틴을 반환해야 함.
	 * @return
	 */
	public boolean isContinuing(ITask task);
	
	/**
	 * 작업 중지 명령
	 */
	public void stopTask(ITask task);
	
	/**
	 * 작업이 시작될 때 발생하는 이벤트 핸들러
	 * @param task
	 */
	public void OnStartTask(ITask task);
	
	/**
	 * 작업 진행 정도를 알려 주고, 작업을 계속 진행할 지 여부를 반환하는 이벤트 핸들러
	 * @param task     진행 중인 작업. 작업이 하나인 경우 굳이 구별할 필요가 없기 때문에 null인 경우도 있음
	 * @param progress
	 * @return 작업을 계속 진행해도 되면 true, 중단해야 하면 false 반환
	 */
	public boolean OnProgress(ITask task, long progress);
	
	/**
	 * 작업이 끝났을 때 발생하는 이벤트 핸들러
	 * @param task
	 */
	public void OnEndTask(ITask task);
	
	/**
	 * 작업 수행 중 오류가 발생했을 때 발생하는 이벤트 핸들러
	 * @param task
	 * @param e
	 */
	public void OnErrorRaised(ITask task, Throwable e);
	
	/**
     * 관련된 모든 작업이 끝났을 때 이벤트를 반환하는 핸들러
     */
    public void OnEndAllTask();
}
