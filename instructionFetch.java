import java.util.*;
import java.io.*;

public class instructionFetch
{
	private Queue<Instruction> loadQueue; //holds all of the instructions to start with
	private Queue<Instruction> instructionStorage; //auxillary data structure to hold all insturctions in case a branch occurs
	private Queue<Instruction> queue2; //holds the second set of instructions
	private int queue2size; //nq size of the fetch queue
	private int numFetch;  //nf number of instructions to move to num fetch
	public HashMap<String,Integer> labels;
	
	public instructionFetch(int nf, int queue2s)
	{
		loadQueue = new ArrayDeque<Instruction>();
		instructionStorage = new ArrayDeque<Instruction>();
		queue2size = queue2s;
		queue2 = new ArrayDeque<Instruction>(queue2s);
		numFetch = nf;
	}
	
	public void update() //move nf instructions to queue2
	{
		int counter = 0;
		while(queue2.size() <= queue2size && counter < numFetch)
		{
			queue2.add(loadQueue.poll());
			counter++;
		}
	}
	
	public Instruction pop()
	{
		return queue2.poll();
	}
	
	public HashMap<Integer,Double> loadData(String filename) throws FileNotFoundException //Fill up the queue with all of the instructions and return the memory with data in a HashMap
	{
		HashMap<Integer,Double> memory = new HashMap<Integer,Double>();
		labels = new HashMap<String,Integer>();
		Scanner input = new Scanner(new File(filename));
		int counter = 0;
		while(input.hasNextLine()) //Instruction information loading, not data loading!
		{

			System.out.println(counter);
			//Need to pre edit the input, go until there's white space and isolate the part of the string after the whitespace
			//Then trim this second part of the string, and reinsert it into the whole String and then leave the rest of the code as is

			String y = input.nextLine();
			y.replaceAll("\\s+", " ");
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
			String[] lineSplit = line.split(" ");
			if(lineSplit.length < 3 && lineSplit.length > 1) //No Label for this instruction and not on data segment yet
			{
				if(lineSplit[0].equals("BNEZ") || lineSplit[0].equals("BEZ") || lineSplit[0].equals("BEQ") || lineSplit[0].equals("BNEQ"))
				{
					Instruction curr = null;
					String [] operands = lineSplit[1].split(",");
					if(operands.length == 2)
					{
						curr = new Instruction(counter,operands[0],null,operands[1],lineSplit[0],true);
					}
					else //BEQ BNEQ
					{
						 curr = new Instruction(counter,operands[0],operands[1],operands[2],lineSplit[0],true);
					}
					counter++;
					instructionStorage.add(curr);
					loadQueue.add(curr);
				}
				else
				{
					String [] operands = lineSplit[1].split(",");
					if(operands.length == 2) // only one operand and one destination
					{
						Instruction curr = new Instruction(counter,operands[1],null,operands[0],lineSplit[0],false);
						//order is counter,operand1,operand2,dest,opcode
						instructionStorage.add(curr);
						loadQueue.add(curr);
						counter++;
					}
					else
					{
						Instruction curr = new Instruction(counter,operands[2],operands[1],operands[0],lineSplit[0],false);
						instructionStorage.add(curr);
						loadQueue.add(curr);
						counter++;
					}
				}
			}
			else if(lineSplit.length == 3) //This means we have a label here 
			{
				String currLabel = lineSplit[0];
				currLabel = currLabel.substring(0,currLabel.length()-1); //remove the colon CHECK IF THIS WORKS
				labels.put(currLabel,counter);
				
				
				String [] operands = lineSplit[2].split(",");
				if(operands.length == 2) // only one operand and one destination
				{
					Instruction curr = new Instruction(counter,operands[1],null,operands[0],lineSplit[1],false);
					//order is counter,operand1,operand2,dest,opcode
					instructionStorage.add(curr);
					loadQueue.add(curr);
					counter++;
				}
				else
				{
					Instruction curr = new Instruction(counter,operands[2],operands[1],operands[0],lineSplit[1],false);
					instructionStorage.add(curr);
					loadQueue.add(curr);
					counter++;
					
				}
				
			}
			else if(lineSplit[0].equals("DATA") || lineSplit[0].equals("Data") || lineSplit[0].equals("data"))
				break;
			else
				continue;
		}
		
		while(input.hasNextLine())
		{
			String line = input.nextLine();
			String [] stuff = line.split("=");
			//stuff[0] is Mem(200) stuff[1] is value
			double thisValue = Double.parseDouble(stuff[1].trim());
			System.out.println(line);
			System.out.println(thisValue);
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
		
		System.out.println("These are the instructions!");
		System.out.println(instructionStorage);
		System.out.println("\n\n\n\n\n\n\n");
		System.out.println("Here is the data!");
		System.out.println(memory);
		System.out.println("\n\n\n\n\n" + "And here are the labels");
		System.out.println(labels);
		
		
		return memory;
	}
	
	public void moveToLoading(int number) //When a branch is taken, move the instructions following the branch to the loading queue
	{
		
	}
}
	
	
	
