package reservationStation;

import java.util.LinkedList;
import java.util.Queue;

import basicUnits.ClockDependentUnit;
import basicUnits.instruction.Instruction;

public class ReservationStation extends ClockDependentUnit
{
	private final int maxInstructionsInQueue;
	private final Queue<Instruction> instructionQueue;
	private final Queue<Instruction> inPipeline;

	public ReservationStation(int maxInstructionsInQueue)
	{
		super(1);
		this.maxInstructionsInQueue = maxInstructionsInQueue;
		this.instructionQueue = new LinkedList<Instruction>();
		this.inPipeline = new LinkedList<Instruction>();
	}
	
	public boolean addInstruction(Instruction i)
	{
		if(this.instructionQueue.size()+this.inPipeline.size() >= this.maxInstructionsInQueue)
		{
			return false;
		}
		this.instructionQueue.add(i);
		return true;
	}
	
	public Instruction getInstruction()
	{
		if(!super.doOneOperation())
		{
			return null;
		}

		Instruction ready = this.instructionQueue.poll();
		if(ready == null)
		{
			return null;
		}
		this.inPipeline.add(ready);
		return ready;
	}
	
	public Instruction removeInstruction()
	{
		return this.inPipeline.poll();
	}
	
//	public void newClockCycle()
//	{
//		super.newClockCycle();
//	}
//	
//	public boolean doOneOperation()
//	{
//		if(!super.doOneOperation())
//		{
//			return false;
//		}
//		return true;
//	}
}
