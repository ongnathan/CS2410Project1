import java.util.*;
import java.io.*;
import basicUnits.ClockDependentUnit;
import basicUnits.instruction.Instruction;
import basicUnits.register.Register;
import operations.*;
import reservationStation.ReservationStation;
//TODO 
//NEED SPECIAL CASES FOR BRANCH 
//FOR BRANCH DO DIFFERENT THINGS BASED ON PREDICTION, IF WE'RE PREDICTING THEN WHENEVER SET CURR INSTRUCTION TO PREDICTED LOCATION BUT REMEMBER THE LOCATION THAT WE SHOULD HAVE GONE TO 
//IF INCORRECT
//AT ROB TIME WE FIND OUT IF WE WERE INCORRECT OR NOT
//BRANCHES:
/*Predict as taken-> stall prior to branch, then wait for address calculation
predict not taken-> continue running as normal
if wrong-> kill everything in every object prior to Branch
if right-> keep on chugging
if no prediction-> stall everything before it until the branch has been committed and then set pointer to proper instruction and then continue

*/
public class simulator
{
	public static int nf; //# of fetched instructions per cycle
	public static int nd; //# of instructions decoded per cycle
	public static int ni; //length of decoded queue (before issuing)
	public static int nq;//length of instruction queue (after fetch)
	public static int nw;//# of instructions issued per cycle
	public static int nr; //# reorder buffers
	public static boolean predictBranch; //True means we predict branches
	public static boolean memoryDump; //Option to dump contents of memory
	public static boolean registerDump; //Option to dump contents of registers
	public static boolean branchDump; //Option to dump contents of branch predictor
	public static int dumpStart; //Beginning location of memory dump if option is selected
	public static int dumpEnd; //Ending location of memory dump if option is selected
	public static int intRenaming; //number of Integer renaming registers
	public static int floatRenaming; //number of Floating Point renaming registers
	public static int execToWrite; //holds the busses between execution station and reservation station/renaming/write buffer
	public static int renameToActual; //holds the number of busses to write from renaming registers to actual registers
	
	public static int [] numStations; //Holds the number of each reservation station type
	public static instructionFetch fetch;
	public static instructionDecode decoder;
	
	public static int [] intRegisters; //32 Integer Registers
	public static double [] floatRegisters; //32 Float Registers
	public static int [] Irenaming; //Integer renaming registers
	public static boolean [] Ir;
	public static boolean [] Fr;
	public static double [] Frenaming; //Float Renaming Registers
	public static HashMap<Integer,Double> memory; //Data Memory
	public static String inputFile; //File name with instructions
	public static ReservationStation [] stations; //Holds each Reservation Station
	public static HashMap<String,Integer> labels; //Holds integer location of each branch label
	
	public static HashMap<String,String> regLocations; //When an Instruction goes to RS, Save where the destination reg is going to be
	
	/*
	Register name maps to:
	1. Renaming Register in the form "RR1, RR2, etc. OR RF1, RF2, etc. *-> renaming register
	2. Reservation Station in the form "!I1, !I2, *M1, *M2, etc.  ! -> reservation station
	*/
	//public static HashMap<String,ArrayList<Instruction>> needResult; 
	//Changing ^ to v is in progress!!!!
	public static HashMap<Instruction,ArrayList<Instruction>> needResult; 
	//Maps Instruction that has result to which instruction need it
	
	public static HashMap<Instruction,Integer> renameMap;
	//Maps instruction to which renaming register has it 
	//Then when an instruction finally commits we can take the instruction out of the ROB
	//and simply free up its renaming register, and commit the value that was there to memory
	public static HashMap<Instruction,Integer> memLocations;
	//Maps location of instruction accessing memory, with the location that it wants to access
	//All of the operationalUnits!!
	public static HashMap<Instruction,Integer> branchResult;
	public static OperationalUnit Iunit;
	public static OperationalUnit Bunit;
	public static OperationalUnit Dunit;
	public static OperationalUnit Lunit;
	public static OperationalUnit Munit;
	public static OperationalUnit Funit;
	
	public static ReorderBuffer ROB;
	public static dataCache cache;
	/*NEED TO UPDATE ALL OF THESE METHODS WITH THE REST OF THE OBJECTS
	*/
	

	public static void resetClocks()
	{
		fetch.newClockCycle();
		decoder.newClockCycle();
		for(int i = 0; i < 6; i++)
			stations[i].newClockCycle();
		Iunit.newClockCycle();
		Bunit.newClockCycle();
		Dunit.newClockCycle();
		Lunit.newClockCycle();
		Munit.newClockCycle();
		Funit.newClockCycle();
		cache.newClockCycle();
	}
	public static void clear() //This method should be called if a branch was incorrectly predicted to wipe out all objects except the fetch unit and then start over!
	{
	//TOOOOOOOODOOOOOOOOOOOOOOO
	}
	public static boolean everythingIsEmpty()
	{
		if(fetch.isEmpty() && decoder.isEmpty()) //Just need to add ROB and we're done!
		{
			for (int i = 0; i < 6; i++)
			{
				if(!stations[i].isEmpty())
					return false;
			}
			for (int i = 0; i < intRenaming; i++)
			{
				if(Ir[i] == true)
					return false;
			}
			for (int i =0; i < floatRenaming; i++)
			{
				if(Fr[i] == true)
					return false;
			}
			return true;
		}
		return false;
	}
	public static void clearAll(int curr) //Clear everything if incorrect prediction! and pass in the next instruction location
	{
		for(int i = 0 ;i < stations.length;i++)
			stations[i].clear();
		ROB.clear();
		cache.clear();
		fetch.clear(curr);
		decoder.clear();
		Iunit.clear();
		Bunit.clear();
		Dunit.clear();
		Lunit.clear();
		Munit.clear();
		Funit.clear();
		regLocations.clear();
		needResult.clear();
		mapLocations.clear();
		renameMap.clear();
		for(int i = 0; i < Irenaming.length;i++)
		{
			Irenaming[i] = 0;
			Ir[i] = false;
		}
		for(int i = 0; i <Frenaming.length;i++)
		{
			Frenaming[i] = 0.0;
			Fr[i] = false;
		}
		
	}
	public static void updateExecution()
	{
		Iunit.prepInstructionForPipeline();
		Bunit.prepInstructionForPipeline();
		Dunit.prepInstructionForPipeline();
		Lunit.prepInstructionForPipeline();
		Munit.prepInstructionForPipeline();
		Funit.prepInstructionForPipeline();
	}
	public static void commitInstructions()
	{
		for(int j = 0; j < renameToActual; j++)
		{
			Instruction curr = ROB.get();
			if(curr!=null)
			{
				if(curr.instructionType.ordinal() == 11 || curr.instructionType.ordinal() == 13) //Store instruction put the instruction into the cache and don't remove it until it's ready
				{
					if(curr == cache.get())
					{
						Instruction i = cache.remove();
						if(i != null) //Then the instruction was ready so we can store the data into memory!
						{
							int x = memLocations.get(i); //Effective Address
							memLocations.remove(i);
							memory.put(Integer.valueOf(x),curr.operandTwo.getValue().doubleValue());
							ROB.remove();
						}
					}
					else
					{
						cache.add(curr);
					}
				}
				else if(curr.instructionType.isLOAD())//Load instruction same thing as store with the cache
				{ 
					if(curr == cache.get())
					{
						Instruction i = cache.remove();
						if(i!=null)
						{
							int y = memLocations.get(i);
							memLocations.remove(i);
							double d = memory.get(Integer.valueOf(y));//result of the load
							String x = i.destination.getName(); //Should return R1, F3 etc.
							if(x.startsWith("R")) //Integer value / register
							{
								String regNum = x.substring(1,x.length());
								int regNumber = Integer.parseInt(regNum);
								intRegisters[regNumber] = (int) d;
								ArrayList<Instruction> results = needResult.get(i);
								for(Instruction inst: results) //Give the instruction the result it needs
								{
									if(inst.operandOne.getName().equals(x))
									{
										int dd = (int) d;
										inst.setOpOne(Integer.valueOf(dd));
									}
									if(inst.operandTwo != null && inst.operandTwo.getName().equals(x))
									{
										int dd = (int) d;
										inst.setOpTwo(Integer.valueOf(dd));
									}
								}
								needResult.remove(i);
							}
							else
							{
								String regNum = x.substring(1,x.length());
								int regNumber = Integer.parseInt(regNum);
								floatRegisters[regNumber] = d;
								ArrayList<Instruction> results = needResult.get(i);
								for(Instruction inst: results)
								{
									if(inst.operandOne.getName().equals(x))
										inst.setOpOne(Double.valueOf(d));
									if(inst.operandTwo !=null && inst.operandTwo.getName().equals(x))
										inst.setOpTwo(Double.valueOf(d));
								}
								needResult.remove(i);
								regLocations.remove(x);
							}
							ROB.remove();
						}
					}
					else
					{
						cache.add(curr);
					}
					
				}
				else if(curr.instructionType.isFloatingPointOperation()) //Just write the data from the renaming register corresponding to the destination and free up the renaming reg
				{
					int renameIndex = renameMap.get(curr);
					renameMap.remove(curr);
					double value = Frenaming[renameIndex];
					System.out.println(curr + "\n\n" + value + "\n\n");
					Fr[renameIndex] = false; //free up the renaming register
					Frenaming[renameIndex] = 0.0;
					String x = curr.destination.getName();
					ArrayList<Instruction> result = needResult.get(curr);
					if(result!=null)
					{
						for(Instruction inst: result)
						{
							if(inst.operandOne.getName().equals(x))
								inst.setOpOne(Double.valueOf(value));
							if(inst.operandTwo!=null && inst.operandTwo.getName().equals(x))
								inst.setOpTwo(Double.valueOf(value));
						}
					}
					needResult.remove(curr);
					regLocations.remove(x);
					int regIndex = Integer.parseInt(x.substring(1,x.length()));
					floatRegisters[regIndex] = value;
					ROB.remove();
				}
				else if(curr.instructionType.isINT1()) //Just write the data from the renaming integer register to the destination
				{
					int renameIndex = renameMap.get(curr);
					renameMap.remove(curr);
					int value = Irenaming[renameIndex];
					Ir[renameIndex] = false; //free up the renaming register
					Irenaming[renameIndex] = 0;
					String x = curr.destination.getName();
					ArrayList<Instruction> result = needResult.get(curr);
					if(result != null)
					{
						for(Instruction inst: result)
						{
							if(inst.operandOne.getName().equals(x))
								inst.setOpOne(Integer.valueOf(value));
							if(inst.operandTwo!=null && inst.operandTwo.getName().equals(x))
								inst.setOpTwo(Integer.valueOf(value));
						}
					}
					needResult.remove(curr);
					regLocations.remove(x);
					int regIndex = Integer.parseInt(x.substring(1,x.length()));
					intRegisters[regIndex] = value;
					ROB.remove();				
				}
				else if(curr.instructionType.isBranch()) //TO DO Need to handle a few cases here based on branch prediction and result of branch
				{
				}
			}
		}
	}
	public static void computeResults()
	{ 
		boolean [] used = new boolean [6];
	//We can only move execToWrite instructions to the ROB in one cycle
		for(int i = 0; i < execToWrite; i++)
		{
			int iFound = -1; //holds whether an integer renaming register is available
			int fFound = -1;//holds whether a float renaming register is available
			Instruction curr = null;
			for(int j = 0; j < intRenaming; j++)
				{
					if(Ir[j] == false)
					{
						iFound = j;
					}
					if(Fr[j] == false)
						fFound = j;
				}
			if(Bunit.isOutputReady() && ! used[0] && ROB.isSpace())
			{
				used[0] = true;
				curr = Bunit.getOutputInstruction();
				int k = Bunit.getOutput().intValue();
				branchResult.put(curr,k);
				ROB.add(curr);
				
				
			}
			if(Iunit.isOutputReady() && iFound != -1 && !used[1] && ROB.isSpace())
			{
				used[1] = true;
				curr = Iunit.getOutputInstruction();
				int k = Iunit.getOutput().intValue();
				int resNumber = stations[0].getReservationNumber(curr);
				stations[0].removeInstruction();
				String x = "!I" + resNumber;
				if(needResult.get(curr) != null)
				{
					ArrayList<Instruction> result = needResult.get(curr);
					for(Instruction j :result)
					{
						if(j.operandOne.getName().equals(curr.destination.getName()))
							j.setOpOne(Integer.valueOf(k));
						else if(j.operandTwo.getName().equals(curr.destination.getName()))
							j.setOpTwo(Integer.valueOf(k));
					}
					needResult.remove(curr);
				}
				regLocations.remove(curr.destination.getName());
				Ir[iFound] = true;
				Irenaming[iFound] = k;
				regLocations.put(curr.destination.getName(),"RR" + iFound);
				renameMap.put(curr,iFound);
				ROB.add(curr);
			}
			else if(Munit.isOutputReady() && iFound != -1 && !used[2] && ROB.isSpace())
			{
				used[2] = true;
				curr = Munit.getOutputInstruction();
				int k = Munit.getOutput().intValue();
				int resNumber = stations[1].getReservationNumber(curr);
				stations[1].removeInstruction();
				String x = "*M" + resNumber;
				if(needResult.get(curr) != null)
				{
					ArrayList<Instruction> result = needResult.get(curr);
					for(Instruction j :result)
					{
						if(j.operandOne.getName().equals(curr.destination.getName()))
							j.setOpOne(Integer.valueOf(k));
						else if(j.operandTwo.getName().equals(curr.destination.getName()))
							j.setOpTwo(Integer.valueOf(k));
					}
					needResult.remove(curr);
				}
				regLocations.remove(curr.destination.getName());
				Ir[fFound] = true;
				Irenaming[iFound] = k;
				regLocations.put(curr.destination.getName(),"RR" + iFound);
				renameMap.put(curr,iFound);
				ROB.add(curr);
			}
			else if(Lunit.isOutputReady() && iFound != -1 && !used[3] && ROB.isSpace())
			{
				used[3] = true;
				curr = Lunit.getOutputInstruction();
				int k = Lunit.getOutput().intValue();
				int resNumber = stations[2].getReservationNumber(curr);
				stations[2].removeInstruction();
				memLocations.put(curr,k); //save the computed address calculation
				String x = "!L" + resNumber;
				/*if(needResult.get(curr.destination.getName()) != null)
				{
					ArrayList<Instruction> result = needResult.get(curr.destination.getName());
					for(Instruction j :result)
					{
						if(j.operandOne.getName().equals(curr.destination.getName()))
							j.setOpOne(Integer.valueOf(k));
						else if(j.operandTwo.getName().equals(curr.destination.getName()))
							j.setOpTwo(Integer.valueOf(k));
					}
					needResult.remove(curr.destination.getName());
				}*/
				if(curr.instructionType.ordinal() != 11 && curr.instructionType.ordinal()!= 13) // Load Instruction
				{
					regLocations.remove(curr.destination.getName());
					regLocations.put(curr.destination.getName(),"memory" + curr.instructionNum);
				}
				ROB.add(curr);
			}
			else if(Funit.isOutputReady() && fFound != -1 && ! used[4]&& ROB.isSpace())
			{
				used[4] = true;
				curr = Funit.getOutputInstruction();
				System.out.println("here");
				double k = Funit.getOutput().doubleValue();
				int resNumber = stations[3].getReservationNumber(curr);
				stations[3].removeInstruction();
				String x = "*F" + resNumber;
				if(needResult.get(curr) != null)
				{
					ArrayList<Instruction> result = needResult.get(curr);
					for(Instruction j :result)
					{
						if(j.operandOne.getName().equals(curr.destination.getName()))
							j.setOpOne(Double.valueOf(k));
						else if(j.operandTwo.getName().equals(curr.destination.getName()))
							j.setOpTwo(Double.valueOf(k));
					}
					needResult.remove(curr);
				}
				regLocations.remove(curr.destination.getName());
				Fr[fFound] = true;
				Frenaming[fFound] = k;
				regLocations.put(curr.destination.getName(),"RF" + fFound);
				System.out.println(curr);
				ROB.add(curr);
				renameMap.put(curr,fFound);				
			}
			else if(Dunit.isOutputReady() && fFound == -1 && !used[5] && ROB.isSpace())
			{
				used[5] = true;
				curr = Dunit.getOutputInstruction();
				double k = Dunit.getOutput().doubleValue();
				int resNumber = stations[4].getReservationNumber(curr);
				stations[4].removeInstruction();
				String x = "*D" + resNumber;
				if(needResult.get(curr) != null)
				{
					ArrayList<Instruction> result = needResult.get(curr);
					for(Instruction j :result)
					{
						if(j.operandOne.getName().equals(curr.destination.getName()))
							j.setOpOne(Double.valueOf(k));
						else if(j.operandTwo.getName().equals(curr.destination.getName()))
							j.setOpTwo(Double.valueOf(k));
					}
					needResult.remove(curr);
				}
				regLocations.remove(curr.destination.getName());
				Fr[fFound] = true;
				Frenaming[fFound] = k;
				regLocations.put(curr.destination.getName(),"RF" + fFound);
				ROB.add(curr);
				renameMap.put(curr,fFound);
			}
			
			
		}
	}
	public static void main(String [] args) throws FileNotFoundException
	{
		getCommands(args);
		//Construct All Objects
		//******************
		ROB = new ReorderBuffer(nr);
		cache = new dataCache();
		needResult = new HashMap<Instruction,ArrayList<Instruction>>();
		intRegisters = new int [32];
		floatRegisters = new double [32];
		regLocations = new HashMap<String,String>();
		renameMap = new HashMap<Instruction,Integer>();
		memLocations = new HashMap<Instruction,Integer>();
		branchResult = new HashMap<Instruction,Integer>();
		Irenaming = new int [intRenaming];
		Ir = new boolean[intRenaming];
		Fr = new boolean[floatRenaming];
		Frenaming = new double[floatRenaming];
		fetch = new instructionFetch(nf,nq); //Unit that fetches instructions
		decoder = new instructionDecode(nd,ni); //Unit that decodes instructions
		stations = new ReservationStation[6]; // Array for all of the reservation stations
		//INT1,MULT,LOAD/STORE,FPU,FPDIV,BU
		for (int i = 0; i < 6; i++)
		{
			stations[i] = new ReservationStation(numStations[i]);
		}
		Iunit = new IntegerUnit(stations[0]);
		Munit = new MultiplicationUnit(stations[1]);
		Lunit = new LoadStoreUnit(stations[2]);
		Funit = new FloatingPointUnit(stations[3]);
		Dunit = new DivisionUnit(stations[4]);
		Bunit = new BranchUnit(stations[5]);
		memory = fetch.loadData(inputFile);
		labels = fetch.labels;
		
		
		
		//******************
		
		
		int counter = 0;
		//Begin Main Simulation Loop
		while(counter < 28)
		{
			resetClocks(); //Start new clock cycles  
			commitInstructions();
			computeResults();
			updateExecution();	//EXECUTE 
			
			//Need to update all instructions that may now have their operands ready after
			//Execution phase is done for other instructions
			//need to check for branching and set pointer if necessary
			//STILL NEED TO HANDLE MEMORY ACCESS 
			//IF someone is going to write to memory location that you're using, wait for the result to be available and know the location 
			//Can't know the location until after effective address calculation
			//Otherwise, just access memory, but need to wait for data cache to be available
			boolean [] onlyOne = new boolean[6]; // can only issue one instruction per reservationStation per cycle
			for(int i = 0; i < nw; i++) //send the instruction to the appropriate reservationStation
			{
				if(decoder.peek()!=null)
				{
					int whichPlace = -1;
					Instruction curr = null;
					boolean isStore = false;
					if(decoder.peek().instructionType.isINT1() && onlyOne[0] == false)
					{
						if(stations[0].addInstruction(decoder.peek()))
						{
							whichPlace = 0;
							onlyOne[0] = true;
							curr = decoder.issue();
						}
					}
					else if(decoder.peek().instructionType.isMULT() && onlyOne[1] == false)
					{
						if(stations[1].addInstruction(decoder.peek()))
						{
							whichPlace = 1;
							curr = decoder.issue();
							onlyOne[1] = true;
						}
					}
					else if(decoder.peek().instructionType.isLOAD()&&onlyOne[2] == false)
					{
						if(stations[2].addInstruction(decoder.peek()))
						{
							whichPlace = 2;
							curr = decoder.issue();
							if(curr.instructionType.ordinal() == 11 || curr.instructionType.ordinal() == 13) // If it is a store, move dest to op 2 for conveninece
							{
								isStore = true;
								curr.operandTwo = curr.destination;
							}
							onlyOne[2] = true;
						}
					}
					else if(decoder.peek().instructionType.isFPU() && onlyOne[3] == false)
					{
						if(stations[3].addInstruction(decoder.peek()))
						{
							whichPlace = 3;
							curr = decoder.issue();
							onlyOne[3] = true;
						}
					}
					else if(decoder.peek().instructionType.isFPDIV() && onlyOne[4] == false)
					{
						if(stations[4].addInstruction(decoder.peek()))
						{
							whichPlace = 4;
							curr = decoder.issue();
							onlyOne[4] = true;
						}
					}
					else if(decoder.peek().instructionType.isBranch()&&onlyOne[5] == false)
					{
						if(stations[5].addInstruction(decoder.peek()))
						{
							whichPlace = 5;
							curr = decoder.issue();
							onlyOne[5] = true;
						}
					}
					if(curr!=null && curr.operandOne!= null) 
					{
						if(regLocations.get(curr.operandOne.getName()) == null) //Simply Read value from the register
						{
							if(curr.operandOne.getName().startsWith("R")) //Integer register
							{
								curr.setOpOne(Integer.valueOf(getValI(curr.operandOne.getName())));
							}
							else
							{
								curr.setOpOne(Double.valueOf(getValF(curr.operandOne.getName()))); //Float register
							}
						}
						else
						{
							String loc = regLocations.get(curr.operandOne.getName());
							if(loc.startsWith("memory")) //loop through ROB and find the one with this Instruction Num
							{
								Instruction ii = ROB.getByInstructionNum(Integer.parseInt(loc.substring(6,loc.length()))); //The number after memory denotes the instructionNum
								ArrayList<Instruction> b = needResult.get(ii);
								if(b == null)
								{
									b = new ArrayList<Instruction>();
									b.add(curr);
									needResult.put(ii,b);
								}
								else
								{
									b.add(curr);
									needResult.put(ii,b);
								}
							}
							else if(loc.startsWith("RR") || loc.startsWith("RF")) //Renaming Register Get value from it
							{
								if(loc.charAt(1)=='R')
								{
									curr.setOpOne(Integer.valueOf(getReValI(loc)));
								}
								else
								{
									curr.setOpOne(Double.valueOf(getReValF(loc)));
								}
								
							} 	//!I,!M,*L,!L,*F,*D
							else //value is in a reservation station, so tell the hashmap that you need it
							{
								Instruction x;
								if(loc.charAt(1)=='I')
								{
									x = stations[0].getByMapping(Integer.parseInt(loc.substring(2,loc.length())));
								}
								else if(loc.charAt(1)=='M')
								{
									x = stations[1].getByMapping(Integer.parseInt(loc.substring(2,loc.length())));
								}
								else if(loc.charAt(1) == 'L')
								{
									x = stations[2].getByMapping(Integer.parseInt(loc.substring(2,loc.length())));
								}
								else if(loc.charAt(1) == 'F')
								{
									x = stations[3].getByMapping(Integer.parseInt(loc.substring(2,loc.length())));
								}
								else if(loc.charAt(1) =='D')
								{
									x = stations[4].getByMapping(Integer.parseInt(loc.substring(2,loc.length())));
								}
								else
								{
									x = stations[5].getByMapping(Integer.parseInt(loc.substring(2,loc.length())));
								}
								if(needResult.get(x)== null)
								{
									ArrayList<Instruction>asdf = new ArrayList<Instruction>();
									asdf.add(curr);
									needResult.put(x,asdf);
								}
								else
								{
									ArrayList<Instruction> asdf = needResult.get(x);
									asdf.add(curr);
									needResult.put(x,asdf);
								}
							
							}
						}
						if(curr.operandTwo!= null) //Make sure it isn't a store operation
						{
							if(regLocations.get(curr.operandTwo.getName())==null)
							{
							
								if(curr.operandTwo.getName().startsWith("R"))
								{
									
									curr.setOpOne(Integer.valueOf(getValI(curr.operandTwo.getName()))); //Here is the error!
								}
								else
								{
									curr.setOpTwo(Double.valueOf(getValF(curr.operandTwo.getName()))); //Also error...
									
								}
							}
							else //Store the location
							{
								String loc = regLocations.get(curr.operandTwo.getName());
								if(loc.startsWith("memory"))
								{
									Instruction ii = ROB.getByInstructionNum(Integer.parseInt(loc.substring(6,loc.length()))); //The number after memory denotes the instructionNum
									ArrayList<Instruction> b = needResult.get(ii);
									if(b == null)
									{
										b = new ArrayList<Instruction>();
										b.add(curr);
										needResult.put(ii,b);
									}
									else
									{
										b.add(curr);
										needResult.put(ii,b);
									}								
								}
								if(loc.startsWith("RR") || loc.startsWith("RF")) //Renaming Register Get value from it
								{
									if(loc.charAt(1)=='R')
									{
										curr.setOpTwo(Integer.valueOf(getReValI(loc)));
									}
									else
									{
										curr.setOpTwo(Double.valueOf(getReValF(loc)));
									}
								}
								else //TODO Hard part, value is in a reservation station, add myself to the list of values that need the result, and tell Hashmap who has the result
								{
									Instruction x;
									if(loc.charAt(1)=='I')
									{
										x = stations[0].getByMapping(Integer.parseInt(loc.substring(2,loc.length())));
									}
									else if(loc.charAt(1)=='M')
									{
										x = stations[1].getByMapping(Integer.parseInt(loc.substring(2,loc.length())));
									}
									else if(loc.charAt(1) == 'L')
									{
										x = stations[2].getByMapping(Integer.parseInt(loc.substring(2,loc.length())));
									}
									else if(loc.charAt(1) == 'F')
									{
										x = stations[3].getByMapping(Integer.parseInt(loc.substring(2,loc.length())));
									}
									else if(loc.charAt(1) =='D')
									{
										x = stations[4].getByMapping(Integer.parseInt(loc.substring(2,loc.length())));
									}
									else
									{
										x = stations[5].getByMapping(Integer.parseInt(loc.substring(2,loc.length())));
									}
									if(needResult.get(x)== null)
									{
										ArrayList<Instruction>asdf = new ArrayList<Instruction>();
										asdf.add(curr);
										needResult.put(x,asdf);
									}
									else
									{
										ArrayList<Instruction> asdf = needResult.get(x);
										asdf.add(curr);
										needResult.put(x,asdf);
									}
						
								}
							}
						}
						else
						{
							curr.isReadyTwo = true;
						}
					}
					if(curr!=null)
					{
						if(whichPlace == 0)
							regLocations.put(curr.destination.getName(),"!I" + stations[0].getReservationNumber(curr));
						else if(whichPlace == 1)
							regLocations.put(curr.destination.getName(),"!M" + stations[1].getReservationNumber(curr));
						else if(whichPlace == 2 && !isStore)
						{
							if(curr.destination.getName().startsWith("F"))
								regLocations.put(curr.destination.getName(),"*L"+stations[2].getReservationNumber(curr));
							else
								regLocations.put(curr.destination.getName(),"!L"+stations[2].getReservationNumber(curr));
						}
						else if(whichPlace == 3)
							regLocations.put(curr.destination.getName(),"*F"+stations[3].getReservationNumber(curr));				
						else if(whichPlace == 4)
							regLocations.put(curr.destination.getName(),"*D"+stations[4].getReservationNumber(curr));
						
					}
				}
			}
			for (int i = 0; i < nd; i++)
			{
				if(fetch.isEmpty())
					break;
				else
				{
					if(decoder.add(fetch.get())) //decode instructions
						fetch.remove();
				}
			}
			fetch.update(); //fetch instructions
			if(everythingIsEmpty()) //Check if all instructions have been processed
				break;
			
			
			//Next step, send instructions from reservation stations to execution units
			//Then send instructions to ROB for Write Back
			//Keep track of all relevant statistics for this entire process
			counter++;
			System.out.println(counter+"\n\n\n\n");
			System.out.println("----------Fetch Unit-----------\n\n");
				System.out.println(fetch);
			System.out.println("--------------Decode Unit--------------\n\n");
				System.out.println(decoder);
			System.out.println("--------------------Reservation Stations-------------\n\n\n");
				for(int i = 0; i < 6; i++)
					System.out.println("Reservation Station " + i + " contains " + stations[i]);
			System.out.println("\n\n-----------------Register Locations-----------------\n\n");
			System.out.println(regLocations);
			System.out.println("\n\n---------------------Need Result-------------------\n\n");
			System.out.println(needResult);
			System.out.println("\n\n---------------------------Rename Map--------------------\n\n");
			System.out.println(renameMap);
			System.out.println("\n\n---------------------------Reorder Buffer------------------\n\n");
			System.out.println(ROB);
		}
		
		System.out.println("-------------Here are the registers! (Int and then Float)--------------\n\n\n");
		System.out.println(Arrays.toString(intRegisters));
		System.out.println(Arrays.toString(floatRegisters));
		System.out.println(memory);
		
		
	}
	
	
	public static void getCommands(String [] args) // Gets all of the parameter arguments from user!
	{
	 //Default values!!!!
		int [] nums = {4,2,4,4,2,2};
		numStations = nums; //holds the number of each type of reservations station
		predictBranch = true;
		memoryDump = false;
		registerDump = false;
		branchDump = false;
		nf = 4;
		nq = 8;
		nd = 4;
		nw = 4;
		ni = 8;
		nr = 16;
		intRenaming = 8;
		floatRenaming = 8;
		execToWrite = 4;
		renameToActual = 4;
		inputFile = args[0];
	//User Input can change these default values!
		for(int i = 0; i < args.length;i++)
		{
			if(args[i].toUpperCase().contains("ND"))
			{
				nd = Integer.parseInt(args[i+1]);
			}
			else if(args[i].toUpperCase().contains("NF"))
			{
				nf = Integer.parseInt(args[i+1]);
			}
			else if(args[i].toUpperCase().contains("NI"))
				ni = Integer.parseInt(args[i+1]);
			else if(args[i].toUpperCase().contains("NQ"))
				nq = Integer.parseInt(args[i+1]);
			else if(args[i].toUpperCase().contains("NW"))
				nw = Integer.parseInt(args[i+1]);
			
			else if (args[i].toUpperCase().contains("BRANCH"))
				predictBranch = false;
			else if(args[i].equals("--dump_mem"))
			{
				String line = args[i+1];
				String [] x = line.split(",");
				dumpStart = Integer.parseInt(x[0]);
				dumpEnd = Integer.parseInt(x[1]);
			}
			else if(args[i].equals("--dump_reg"))
				registerDump = true;
			else if(args[i].equals("--dump_branch"))
				branchDump = true;
			
			else if(args[i].toUpperCase().equals("INT1"))
				numStations[0] = Integer.parseInt(args[i+1]);
			else if(args[i].toUpperCase().equals("MULT"))
				numStations[1] = Integer.parseInt(args[i+1]);
			else if(args[i].toUpperCase().equals("Load"))
				numStations[2] = Integer.parseInt(args[i+1]);
			else if(args[i].toUpperCase().equals("FPU"))
				numStations[3] = Integer.parseInt(args[i+1]);
			else if(args[i].toUpperCase().equals("FPDIV"))
				numStations[4] = Integer.parseInt(args[i+1]);
			else if(args[i].toUpperCase().equals("BU"))
				numStations[5] = Integer.parseInt(args[i+1]);
			
			else if(args[i].toUpperCase().equals("NR"))
				nr = Integer.parseInt(args[i+1]);
			
			else if(args[i].toUpperCase().equals("INTREG"))
				intRenaming = Integer.parseInt(args[i+1]);
			else if(args[i].toUpperCase().equals("FLOATREG"))
				floatRenaming = Integer.parseInt(args[i+1]);
			else if(args[i].toUpperCase().equals("EXECBUS"))
				execToWrite = Integer.parseInt(args[i+1]);
			else if(args[i].toUpperCase().equals("RENAMEBUS"))
				renameToActual = Integer.parseInt(args[i+1]);
			
				
		}
	}
	public static int getValI(String name)
	{
			String x = name.substring(1,name.length());
			int y = Integer.parseInt(x);
			return intRegisters[y];
		
	}
	public static double getValF(String name)
	{
			String x = name.substring(1,name.length());
			int y = Integer.parseInt(x);
			return floatRegisters[y];
	}
	public static double getReValF(String name)
	{
		String num = name.substring(2,name.length());
			return(Frenaming[Integer.parseInt(num)]);

	}
	public static int getReValI(String name)
	{
		String num = name.substring(2,name.length());
			return(Irenaming[Integer.parseInt(num)]);
		
	}
}
