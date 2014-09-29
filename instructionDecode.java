public class instructionDecode
{
	int nd; //num instructions decoded every cycle
	int ni; //queue length
	Queue <Instruction> queue //for storing instructions before issuing them
	int nw // instructions issued per cycle (external to this class possibly)
	
	public instructionDecode(int numDecoded, int queueLength)
	{
		queue = new ArrayDeque<Instruction>(ni);
		nd = numDecoded;
		ni = queueLength;
	}
	
	public Instruction issue()
	{
		return queue.poll();
	}
	
	public boolean add(Instruction i)
	{
		if(!queue.size()==ni)
		{
			queue.add(i);
			return true;
		}
		else
		{
			return false;
		}
			
	}
}
