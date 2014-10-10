package basicUnits;

public abstract class ClockDependentUnit
{
	public final int numOperationsPerCycle;
	private int numOperationsLeftThisCycle;
	
	public ClockDependentUnit(int numOperationsPerCycle)
	{
		this.numOperationsPerCycle = numOperationsPerCycle;
		this.numOperationsLeftThisCycle = this.numOperationsPerCycle;
	}
	
	public final boolean canOperate()
	{
		return this.numOperationsLeftThisCycle > 0;
	}
	
	public void newClockCycle()
	{
		this.numOperationsLeftThisCycle = this.numOperationsPerCycle;
	}
	
	public boolean doOneOperation()
	{
		if(this.numOperationsLeftThisCycle > 0)
		{
			this.numOperationsLeftThisCycle--;
			return true;
		}
		return false;
	}
}
