import java.util.*;
import java.io.*;
import basicUnits.instruction.Instruction;
import basicUnits.ClockDependentUnit;
import basicUnits.register.Register;
public class instructionFetch extends ClockDependentUnit
{
	private ArrayList<Instruction> loadQueue; //holds all of the instructions to start with
	private Queue<Instruction> queue2; //holds the second set of instructions
	public int currNum;
	private int queue2size; //nq size of the fetch queue
	private int numFetch;  //nf number of instructions to move to num fetch
	public HashMap<String,Integer> labels;
	
	public instructionFetch(int nf, int queue2s)
	{
		super(nf);
		currNum = 0;
		loadQueue = new ArrayList<Instruction>();
		queue2size = queue2s;
		queue2 = new ArrayDeque<Instruction>(queue2s);
		
	}
	public double util()
	{
		int counter = 0;
		for(Instruction i: queue2)
		{
			if(i!= null)
				counter++;
		}
		return (double)counter / (double) queue2size;
	}
	public int getAddress(Instruction i)
	{
		int counter = 0;
		for(Instruction j : loadQueue) //Need to alter this, to make sure it works even when we replace the new instructions.
		{
			if(j.instructionType.isBranch())
			{
				if(j.branchLabel.equals(i.branchLabel))
					return counter;
				else
					counter++;
			}
			else
				counter++;
		}
		return -1;
	}
	public String toString()
	{
		String s ="The current queue contains " + queue2;
		return s;
	}
	public boolean isEmpty()
	{
		return queue2.isEmpty();
	}
	public void clear(int curr)
	{
		ArrayList <Instruction> newQueue = new ArrayList<Instruction>();
		int counter = 0;
		for(Instruction i : loadQueue)
		{
			
			Instruction newone = null;
			
			if(i.instructionType.isINT())
			{
				newone = new Instruction(i.instructionType,new Register<Integer>(i.destination.getName()),new Register<Integer>(i.operandOne.getName()),new Register<Integer>(i.operandTwo.getName()),i.immediate);
			}
			else if(i.instructionType.isINTI())
			{
				newone = new Instruction(i.instructionType,new Register<Integer>(i.destination.getName()),new Register<Integer>(i.operandOne.getName()),null,i.immediate);
			}
			else if(i.instructionType.ordinal() == 10 || i.instructionType.ordinal() == 11)
			{
				newone = new Instruction(i.instructionType,new Register<Integer>(i.destination.getName()),new Register<Integer>(i.operandOne.getName()),null,i.immediate);
			}
			else if(i.instructionType.ordinal() == 12 || i.instructionType.ordinal() == 13)
			{
				newone = new Instruction(i.instructionType,new Register<Double>(i.destination.getName()),new Register<Integer>(i.operandOne.getName()),null,i.immediate);
			}
			else if(i.instructionType.isFPMULDIV())
			{
				newone = new Instruction(i.instructionType,new Register<Double>(i.destination.getName()),new Register<Double>(i.operandOne.getName()),new Register<Double>(i.operandTwo.getName()),i.immediate);
			}
			else if(i.instructionType.ordinal()==18 || i.instructionType.ordinal() == 19)
			{
				newone = new Instruction(i.instructionType,new Register<Integer>(i.destination.getName()),null,null,i.immediate);	
			}
			else if(i.instructionType.ordinal() == 20 || i.instructionType.ordinal()==21)
			{
				newone = new Instruction(i.instructionType,new Register<Integer>(i.destination.getName()),new Register<Integer>(i.operandOne.getName()),null,i.immediate);
			}
			newone.branchLabel = i.branchLabel;
			newQueue.add(counter,newone);
			counter++;
		}
		loadQueue.clear();
		loadQueue = newQueue;
		queue2.clear();
		currNum = curr;
	}
	public void update() //move nf instructions to queue2
	{
		while(super.canOperate() && queue2.size()!=queue2size)
		{
			if(currNum >= loadQueue.size())
				return;
			super.doOneOperation();
			queue2.add(loadQueue.get(currNum));
			currNum++;
		}
	}
	
	public Instruction get()
	{
		return queue2.peek();
	}
	
	public void remove()
	{
		queue2.poll();
	}
	
	public void setPointer(int x)
	{
		currNum = x;
	}
	
	public HashMap<Integer,Double> loadData(String filename) throws FileNotFoundException //Fill up the queue with all of the instructions and return the memory with data in a HashMap
	{
		
		HashMap<Integer,Double> memory = new HashMap<Integer,Double>();
		labels = new HashMap<String,Integer>();
		Scanner input = new Scanner(new File(filename));
		int counter = 0;
		while(input.hasNextLine()) //Instruction information loading, not data loading!
		{
			//Need to pre edit the input, go until there's white space and isolate the part of the string after the whitespace
			//Then trim this second part of the string, and reinsert it into the whole String and then leave the rest of the code as is

			String y = input.nextLine();
			
			
			while(y.indexOf("\t") != -1)
			{
				int asd = y.indexOf("\t");
				String temp = y;
				String newy;
				if(asd==0)
				{
					newy = temp.substring(asd+1,temp.length());
				}
				else
				{
					newy = temp.substring(0,asd) + " " + temp.substring(asd+1,temp.length());
				}
				y = newy;
			}	
			y.replaceAll("\\s", " ").trim();
			for(int i = 0; i < y.length()-1;i++)
			{
				if(y.charAt(i) == ' ' && y.charAt(i+1)== ' ')
				{
					y = y.substring(0,i+1) + y.substring(i+2,y.length());
					i--;
				}
			}
			int whiteLocation = 0;
			for(int i = 0; i < y.length(); i++)
			{
				if(whiteLocation == 0 && y.charAt(i) == ' ' && i > 1)
				{
					if(i < y.length()-1 && (y.charAt(i+1) == 'R' || y.charAt(i+1) == 'F') && Character.isDigit(y.charAt(i+2)))
						whiteLocation = i;
					
				}
			}
			if(whiteLocation != 0)
			{
				String y1 = y.substring(0,whiteLocation);
				String y2 = y.substring(whiteLocation+1,y.length());
				y2 = y2.replaceAll("\\s+","");
				y = y1 + " " + y2;
			}
			//PRE-EDITING DONE!
			String line = y;
			String[] lineSplit = line.trim().split(" ");
			if(lineSplit.length == 0)
			{
				continue;
			}
			else if(lineSplit.length < 3 && lineSplit.length > 1) //No Label for this instruction and not on data segment yet
			{
				if(lineSplit[0].equals("BNEZ") || lineSplit[0].equals("BEZ") || lineSplit[0].equals("BEQ") || lineSplit[0].equals("BNE"))
				{
					Instruction curr = null;
					String [] operands = lineSplit[1].split(",");
					if(operands.length == 2)
					{
						curr = new Instruction(lineSplit[0],new Register<Integer>(operands[0]),null,0);
						curr.setLabel(operands[1]);
					}
					else //BEQ BNEQ
					{
						 curr = new Instruction(lineSplit[0],new Register<Integer>(operands[0]),new Register<Integer>(operands[1]),0);
						 curr.setLabel(operands[2]);
					}
					//instructionStorage.add(curr);
					loadQueue.add(curr);
				}
				else
				{
					Instruction curr = null;
					String [] operands = lineSplit[1].split(",");
					if(operands.length == 2) // only one operand and one destination
					{
						int loc = 0;
						String xx = operands[1]; //Will be 200(R3)
						for(int i = 0; i < xx.length();i++)
						{
							if(xx.charAt(i) == '(')
							{
								loc = i;
							}
						}
						String num = xx.substring(0,loc); //immediate
						String yy = xx.substring(loc+1,xx.length()-1); //Has the register
						if(yy.charAt(0) == 'R')
							curr = new Instruction(lineSplit[0],new Register<Integer>(operands[0]),new Register<Integer>(yy),Integer.parseInt(num));
						else
							curr = new Instruction(lineSplit[0],new Register<Double>(operands[0]),new Register<Integer>(yy),Integer.parseInt(num));
						//order is counter,operand1,operand2,dest,opcode
						//instructionStorage.add(curr);
						loadQueue.add(curr);
					}
					else
					{
						if(operands[0].charAt(0) == 'R') //Integer Registers
						{
							if(operands[2].charAt(0) == 'R') // no Immediate
							{
								curr = new Instruction(lineSplit[0],new Register<Integer>(operands[0]),new Register<Integer>(operands[1]),new Register<Integer>(operands[2]));
							}
							else //there is an immediate
							{
								curr = new Instruction(lineSplit[0],new Register<Integer>(operands[0]),new Register<Integer>(operands[1]),Integer.parseInt(operands[2]));
							}
							//instructionStorage.add(curr);
							loadQueue.add(curr);
						}
						else //F register, check for immediate
						{
							if(operands[2].charAt(0) == 'F') //No Floating point instructions with immediates
							{
								curr= new Instruction(lineSplit[0],new Register<Double>(operands[0]),new Register<Double>(operands[1]),new Register<Double>(operands[2]));
							}
							else
							{
								System.out.println("error");
								return null;
							}
							//instructionStorage.add(curr);
							loadQueue.add(curr);
						}
					}
				}
			}
			else if(lineSplit.length == 3) //This means we have a label here 
			{
				String currLabel = lineSplit[0];
				currLabel = currLabel.substring(0,currLabel.length()-1); //remove the colon CHECK IF THIS WORKS
				labels.put(currLabel,counter);
				if(lineSplit[1].equals("BNEZ") || lineSplit[0].equals("BEZ") || lineSplit[0].equals("BEQ") || lineSplit[0].equals("BNE"))
				{
					Instruction curr = null;
					String [] operands = lineSplit[2].split(",");
					if(operands.length == 2)
					{
						curr = new Instruction(lineSplit[1],new Register<Integer>(operands[0]),null,0);
						curr.setLabel(operands[1]);
					}
					else //BEQ BNEQ
					{
						 curr = new Instruction(lineSplit[1],new Register<Integer>(operands[0]),new Register<Integer>(operands[1]),0);
						 curr.setLabel(operands[2]);
					}
					//instructionStorage.add(curr);
					loadQueue.add(curr);
				}
				else
				{
					Instruction curr = null;
					String [] operands = lineSplit[2].split(",");
					if(operands.length == 2) // only one operand and one destination
					{
						int loc = 0;
						String xx = operands[1]; //Will be 200(R3)
						for(int i = 0; i < xx.length();i++)
						{
							if(xx.charAt(i) == '(')
							{
								loc = i;
							}
						}
						String num = xx.substring(0,loc); //immediate
						String yy = xx.substring(loc+1,xx.length()-1); //Has the register
						if(yy.charAt(0) == 'R')
							curr = new Instruction(lineSplit[1],new Register<Integer>(operands[0]),new Register<Integer>(yy),Integer.parseInt(num));
						else
							curr = new Instruction(lineSplit[1],new Register<Double>(operands[0]),new Register<Integer>(yy),Integer.parseInt(num));
						//order is counter,operand1,operand2,dest,opcode
						//instructionStorage.add(curr);
						loadQueue.add(curr);
					}
					else
					{
						if(operands[0].charAt(0) == 'R') //Integer Registers
						{
							if(operands[2].charAt(0) == 'R') // no Immediate
							{
								curr = new Instruction(lineSplit[1],new Register<Integer>(operands[0]),new Register<Integer>(operands[1]),new Register<Integer>(operands[2]));
							}
							else //there is an immediate
							{
								curr = new Instruction(lineSplit[1],new Register<Integer>(operands[0]),new Register<Integer>(operands[1]),Integer.parseInt(operands[2]));
							}
							//instructionStorage.add(curr);
							loadQueue.add(curr);
						}
						else //F register, check for immediate
						{
							if(operands[2].charAt(0) == 'F') //No Floating point instructions with immediates
							{
								curr= new Instruction(lineSplit[1],new Register<Double>(operands[0]),new Register<Double>(operands[1]),new Register<Double>(operands[2]));
							}
							else
							{
								System.out.println("error");
								return null;
							}
							//instructionStorage.add(curr);
							loadQueue.add(curr);
						}
					}
				}
			}
			else if(lineSplit[0].equals("DATA") || lineSplit[0].equals("Data") || lineSplit[0].equals("data"))
				break;
			else
				continue;
			counter++;
		}
		
		while(input.hasNextLine())
		{
			String line = input.nextLine();
			String [] stuff = line.split("=");
			if(stuff.length <2)
				continue;
			//stuff[0] is Mem(200) stuff[1] is value
			double thisValue = Double.parseDouble(stuff[1].trim());
			String x = stuff[0];
			int loc1 = 0;
			int loc2 = 0;
			for(int i = 0 ; i < x.length(); i++)
			{
				if(x.charAt(i) == '(')
					loc1 = i;
				else if(x.charAt(i) == ')')
					loc2 = i;
			}
			x = x.substring(loc1+1,loc2);
			int secondValue = Integer.parseInt(x);
			memory.put(secondValue,thisValue);
			
		}
		
		//Now, we need to load the data in!
		return memory;
	}
}
	
	
	