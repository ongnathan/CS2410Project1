import basicUnits.instruction.Instruction;
import basicUnits.ClockDependentUnit;
import java.util.*;
public class instructionDecode extends ClockDependentUnit
{
	int ni; //queue length 
	//Can have nd instructions ready to be issued
	Queue <Instruction> queue; //for storing instructions before issuing them
	int nw; // instructions issued per cycle (external to this class possibly)
	public int instructionNum;
	public instructionDecode(int numDecoded, int queueLength) 
	{
		
		super(numDecoded);
		instructionNum = 0;
		queue = new ArrayDeque<Instruction>(ni);
		ni = queueLength;
	}
	public double util()
	{
		int counter = 0;
		for(Instruction i : queue)
		{
			if(i!= null)
				counter++;
		}
		return (double)counter/(double)ni;
	}
	public void clear()
	{
		queue.clear();
	}
	public Instruction issue()
	{
		queue.peek().instructionNum = instructionNum;
		instructionNum++;
		return queue.poll();
	}
	public Instruction peek()
	{
		return queue.peek();
	}
	public boolean add(Instruction i)
	{
		if(!(queue.size()==ni) && super.doOneOperation())
		{
			queue.add(i);
			return true;
		}
		else
		{
			return false;
		}
			
	}
	public boolean isEmpty()
	{
		return queue.isEmpty();
	}
	public String toString()
	{
		String s = "The decoder queue contains " + queue;
		return s;
	}
}