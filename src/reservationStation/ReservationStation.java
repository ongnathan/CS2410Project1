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
	private final Instruction[] mapping;

	public ReservationStation(int maxInstructionsInQueue)
	{
		super(1);
		this.maxInstructionsInQueue = maxInstructionsInQueue;
		this.instructionQueue = new LinkedList<Instruction>();
		this.inPipeline = new LinkedList<Instruction>();
		this.mapping = new Instruction[this.maxInstructionsInQueue];
	}
	
	public boolean addInstruction(Instruction i)
	{
		if(this.instructionQueue.size()+this.inPipeline.size() >= this.maxInstructionsInQueue)
		{
			return false;
		}
		this.instructionQueue.add(i);
		for(int j = 0; j < this.maxInstructionsInQueue; j++)
		{
			if(this.mapping[j] == null)
			{
				this.mapping[j] = i;
				break;
			}
		}
		return true;
	}
	
	public int getReservationNumber(Instruction i)
	{
		for(int j = 0; j < this.maxInstructionsInQueue; j++)
		{
			if(this.mapping[j] == i)
			{
				return j;
			}
		}
		return -1;
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
		Instruction i = this.inPipeline.poll();
		for(int j = 0; j < this.maxInstructionsInQueue; j++)
		{
			if(this.mapping[j] == i)
			{
				this.mapping[j] = null;
			}
		}
		return i;
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
