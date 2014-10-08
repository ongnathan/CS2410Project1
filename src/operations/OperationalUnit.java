package operations;

import basicUnits.ClockDependentInterface;
import basicUnits.ClockDependentUnit;
import basicUnits.instruction.Instruction;

public abstract class OperationalUnit extends ClockDependentUnit implements ClockDependentInterface
{
	public final int latency;
	public final boolean isPipelineable;
	private final Instruction[] pipeline;
	private boolean outputIsReady;
	private boolean outputIsTaken;
	private Number output;
	
	public OperationalUnit(int latency, boolean isPipelineable)
	{
		super(Integer.MAX_VALUE);
		this.latency = latency;
		this.isPipelineable = isPipelineable;
		this.pipeline = new Instruction[this.latency];
	}
	
	public boolean isOutputReady()
	{
		return this.outputIsReady;
	}
	
	public Number getOutput()
	{
		if(!this.outputIsReady)
		{
			return null;
		}
		this.outputIsTaken = true;
		return output;
	}
	
	@Override
	public void newClockCycle()
	{
		super.newClockCycle();
		this.outputIsReady = false;
		this.outputIsTaken = false;
		this.output = null;
	}
	
	/**
	 * 
	 */
	@Override
	public boolean doOneOperation()
	{
		if(super.doOneOperation())
		{
			this.output = this.operate();
			return true;
		}
		return false;
	}
	
	public abstract Number operate();
}
