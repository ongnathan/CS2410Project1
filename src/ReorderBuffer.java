
import java.util.ArrayList;
import basicUnits.instruction.Instruction;
public class ReorderBuffer 
{
	
	private ArrayList<Instruction> queue;
	private int size;
	private int numItems;
	private static int currInstructionNum;
	public ReorderBuffer(int capacity)
	{
		queue = new ArrayList<Instruction>(capacity);
		size = capacity;
		numItems = 0;
		currInstructionNum = 0;
	}
	public double util()
	{
		return (double)numItems/(double)size;
	}
	public int numInst()
	{
		return numItems;
	}
	public Instruction getByInstructionNum(int num)
	{
		for(Instruction i : queue)
		{
			if(i.instructionNum == num)
			{
				return i;
			}
		}
		return null;
	}
	public boolean add(Instruction j)
	{
		if(!isSpace())
			return false;
		for(int i = 0; i < numItems; i++)
		{
			if(j.instructionNum < queue.get(i).instructionNum)
			{
				queue.add(i,j);
				numItems++;
				return true;
			}
		}
		queue.add(j);
		numItems++;
		return true;
	}
	public boolean isSpace()
	{
		return numItems < size;
	}
	public Instruction get()
	{
		if(numItems == 0)
			return null;
		else
		{
			if(queue.get(0).instructionNum > currInstructionNum) //This might cause problems when we branch, but we'll figure it out
			{
				return null;
			}
		}
		return queue.get(0);
	}
	public boolean remove()
	{
	
		if (numItems == 0)
			return false;
		currInstructionNum++;
		queue.remove(0);
		numItems--;
		return true;
	}
	public void clear()
	{
		currInstructionNum++;
		queue.clear();
		numItems = 0;
	}
	public String toString()
	{
		return queue.toString();
	}

}   
	
