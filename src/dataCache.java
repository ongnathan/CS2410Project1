import basicUnits.instruction.Instruction;
import basicUnits.ClockDependentUnit;
import java.util.*;

public class dataCache extends ClockDependentUnit
{

	private static int accessNum; //which # Instruction that has accessed data (if mod 10 is 0 then cache miss)
	private int cycleNum; //if cycleNum == 0 then instruction is ready to be removed
	private Instruction current;
	
	public dataCache()
	{
		super(1);
		accessNum = 0;
		cycleNum = 0;
	}
	public void clear()
	{
		cycleNum = 0;
		current = null;
	}
	public Instruction get()
	{
		return current;
	}
	public boolean add(Instruction i)
	{
		if(isBusy())
			return false;
		else
		{
			current = i;
			accessNum++;
			if(accessNum % 10 == 0)
				cycleNum = 20;
			else
				cycleNum = 1;
			return true;
		}
		
	}

	public Instruction remove()
	{
		if(!isBusy() || (cycleNum != 0))
			return null;
		else
		{
			Instruction i = current;
			current = null;
			cycleNum = 0;
			return i;
			
		}
	}
	public boolean isBusy()
	{
		return (current != null || cycleNum != 0);
	}
	
	public void newClockCycle()
	{
		if(cycleNum != 0)
			cycleNum--;
			
	}
	
}