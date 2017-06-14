package com.tool4us.task;



/**
 * TaskQueue에서 사용할 작업을 정의한 클래스
 * 
 * 작업을 하기 위하여 필요한 인수들
 * 작업의 선후 관계, 실행, 중단, 발생시켜야 하는 이벤트: 작업 진행과정, 완료여부 반환
 * 
 * @author TurboK
 * 
 */
public abstract class ITask
{
	private ITaskMonitor		_taskMonitor = null;
	
	
	/**
	 * 작업을 일괄 관리하고 있는 컨크롤러 설정.
	 * 이 작업 객체가 추가될 TaskQueue에서 할당하며, 임의 할당 금지.
	 * 
	 * @param taskController
	 */
	public void setTaskMonitor(ITaskMonitor taskMonitor)
	{
		_taskMonitor = taskMonitor;
	}
	
	/**
	 * task 인스턴스를 변경하지 않으면서, 멤버변수만 변경시키고자 할때, 이 함수를 override하고, 적절한 지점에서 호출하도록 한다.
	 * 사실, abstract 로 하는게 맞으나, 기존에 ITask 상속받은게 너무 많아 수정사항이 많고, 특이 케이스에서만 사용하기에 abstract 하지 않음.
	 * @param newTask
	 */
	public void modify(ITask newTask)
	{
		// NOTE:
		// modify할게 있으면 각 클래스가 override 해야 한다. 보통, ITask를 상속받은 클래스의 멤버변수값을 변경하는 코드를 넣으면 된다.		
		// oldTask = newTask; 와 같이 대입하면 id()값이 달라지기에, id()값이 달라지지 않으면서 일부 내용만 변경하고 싶을때 사용한다.
		
		// default 는 아무것도 안함.
	}
	/**
	 * 지정된 작업 컨트롤러 반환
	 * @return
	 */
	public ITaskMonitor getTaskMonitor()
	{
		return _taskMonitor;
	}
	
	/**
	 * 계속 실행해도 되는 지 여부 반환.
	 * @return
	 */
	public boolean isContinuing()
	{
		return _taskMonitor == null || _taskMonitor.isContinuing(this);
	}
	
	/**
	 * hashCode 기반의 문자열
	 * @return
	 */
	public String id()
	{
	    return "@" + super.hashCode();
	}
	
	/**
	 * 실행가능 여부 반환
	 * @return
	 */
	abstract public boolean isPossibleToRun();
	
	/**
	 * 작업을 식별할 수 있는 문자열 반환.
	 * 다른 작업들과 Unique하게 구분되도록 구현해야 함.
	 * GUIDE: "작업 종류 + 수행 시 필요한 인수"의 조합으로 작성
	 */
	@Override
	abstract public String toString();
	
	/**
	 * 작업 시작
	 * @throws Exception
	 */
	abstract public void run() throws Exception;
	
	/**
	 * toString()이 id용으로 사용되기에, 그 대체로서, 가독성을 위한 함수임. id용으로는 사용하면 안됨.
	 * @return
	 */
	public String getDesc()
	{
		return toString();	
	}
}

