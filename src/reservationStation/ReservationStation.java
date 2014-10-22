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
	public Instruction getByMapping(int i)
	{
		return this.mapping[i];
	}
	
	public Instruction peekAtNextInstruction()
	{
		return this.instructionQueue.peek();
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
	public double util()
	{
		return (double) numInstructions()/(double)this.maxInstructionsInQueue;
	}
	public int numInstructions()
	{
		int counter = 0;
		for(Instruction i: mapping)
		{
			if(i!=null)
				counter++;
		}
		return counter;
	}
	public boolean isEmpty()
	{
		return this.instructionQueue.isEmpty() && this.inPipeline.isEmpty();
	}
	
	public String toString()
	{
		String s = "The reservation station queue contains " + instructionQueue + "\n The pipeline queue contains " + inPipeline;
		return s;
	}
	public void clear()
	{
		instructionQueue.clear();
		inPipeline.clear();
		for(int i = 0; i < mapping.length;i++)
		mapping[i] = null;
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
