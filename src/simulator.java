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
	public static int InstructionsCommitted;
	//NEEd to write code for all of these!
	public static double robUtil;
	public static double fetchUtil;
	public static double decodeUtil;
	public static double [] stationsUtil;
	public static double renameUtilI;
	public static double renameUtilF;
	public static double exToRenameUtil;
	public static double renameToRegUtil;
	public static double cacheUtil;
	public static int totalIssued;
	public static int exToRenameThisCycle;
	public static int renameToRegThisCycle;
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
	//NEW FOR THE PREDICTION OF BRANCHES**********************************************************************************
	public static HashMap<Instruction,Integer> currPrediction; //Maps a branch instruction to its current prediction******
	public static HashMap<Integer,Integer> branchPreds; //Maps the address of the branch to its previous destination******
	//********************************************************************************************************************
	public static HashMap<String,Integer> labels; //Holds integer location of each branch label
	
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
	public static boolean amStalling;
	//If I'm not predicting branches then I need to set this to true when a branch is
	//encountered (stall until branch is committed!)

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
	public static boolean everythingIsEmpty()
	{
		if(fetch.isEmpty() && decoder.isEmpty() && ROB.numInst() == 0) //Just need to add ROB and we're done!
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
	public static void clearAll(int curr)//Clear everything if incorrect prediction! and pass in the next instruction location
	{
		int count = 0; //figure out how mnay instructions you incorrectly issued and decrease that from instruction num
		if(predictBranch)
		{
			
			for(int i = 0 ;i < stations.length;i++)
			{
				count = count + stations[i].numInstructions();
				stations[i].clear();
			}
			count = count + ROB.numInst() - 1;
			decoder.instructionNum = decoder.instructionNum-count;
		}
		else
		{
			for(int i = 0 ;i < stations.length;i++)
				stations[i].clear();
		}
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
		needResult.clear();
		memLocations.clear();
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
	public static void commitInstructions() throws Exception
	{
		renameToRegThisCycle = 0;
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
							InstructionsCommitted++;
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
							if(x.equals("R0"))
								d = 0;
							if(x.startsWith("R")) //Integer value / register
							{
								String regNum = x.substring(1,x.length());
								int regNumber = Integer.parseInt(regNum);
								intRegisters[regNumber] = (int) d;
								ArrayList<Instruction> results = needResult.get(i);
								if(results!=null)
								{
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
								}
								needResult.remove(i);
							}
							else
							{
								String regNum = x.substring(1,x.length());
								int regNumber = Integer.parseInt(regNum);
								floatRegisters[regNumber] = d;
								ArrayList<Instruction> results = needResult.get(i);
								if(results!=null)
								{
									for(Instruction inst: results)
									{
										if(inst.operandOne.getName().equals(x))
											inst.setOpOne(Double.valueOf(d));
										if(inst.operandTwo !=null && inst.operandTwo.getName().equals(x))
											inst.setOpTwo(Double.valueOf(d));
									}
								}
								needResult.remove(i);             
							}
							ROB.remove();
							InstructionsCommitted++;
						}
					}
					else
					{
						cache.add(curr);
					}
					
				}
				else if(curr.instructionType.isFloatingPointOperation()) //Just write the data from the renaming register corresponding to the destination and free up the renaming reg
				{
					renameToRegThisCycle++;
					int renameIndex = renameMap.get(curr);
					renameMap.remove(curr);
					double value = Frenaming[renameIndex];
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
					int regIndex = Integer.parseInt(x.substring(1,x.length()));
					floatRegisters[regIndex] = value;
					ROB.remove();
					InstructionsCommitted++;
				}
				else if(curr.instructionType.isMULT())
				{
					renameToRegThisCycle++;
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
					int regIndex = Integer.parseInt(x.substring(1,x.length()));
					intRegisters[regIndex] = value;
					ROB.remove();
					InstructionsCommitted++;
				}
				else if(curr.instructionType.isINT1()) //Just write the data from the renaming integer register to the destination
				{
					renameToRegThisCycle++;
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
					int regIndex = Integer.parseInt(x.substring(1,x.length()));
					intRegisters[regIndex] = value;
					ROB.remove();
					InstructionsCommitted++;
				}
				else if(curr.instructionType.isBranch()) //TO DO Need to handle a few cases here based on branch prediction and result of branch
				{
					renameToRegThisCycle++;
					if(!predictBranch)
					{
						int bresult = branchResult.get(curr);
						
						if(bresult == 1) // we are branching!
						{
							int location = labels.get(curr.branchLabel);
							clearAll(location);
							amStalling = false;
						}
						else//just keep on chugging!
						{
							amStalling = false;
						}
						InstructionsCommitted++;
					}
					else
					{
						InstructionsCommitted++;
						int bresult = branchResult.get(curr); //actual result
						Integer prediction = currPrediction.get(curr); //current prediction
						if(bresult != prediction)
						{
							if(bresult == 1) //didn't branch but we should have...
							{
								int location = labels.get(curr.branchLabel);
								branchPreds.put(fetch.getAddress(curr),location);
								clearAll(location);
								currPrediction.clear();
							}
							else //branched but we shouldn't have
							{
								int location = fetch.getAddress(curr) + 1;
								branchPreds.put(fetch.getAddress(curr),location);
								clearAll(location);
								currPrediction.clear();
								
							}

						}
						else
						{
							currPrediction.remove(curr);
						}
					}
					ROB.remove();
				
				}
			}
		}
	}
	public static void computeResults() throws Exception
	{ 
		boolean [] used = new boolean [6];
		exToRenameThisCycle = 0;
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
			if(Bunit.isOutputReady() && ! used[0])
			{
				exToRenameThisCycle++;
				used[0] = true;
				curr = Bunit.getOutputInstruction();
				int k = Bunit.getOutput().intValue();
				branchResult.put(curr,k);
				curr.readyToFinish = true;
				//ROB.add(curr);
				stations[5].removeInstruction();
				
				
			}
			if(Iunit.isOutputReady() && iFound != -1 && !used[1])
			{
				exToRenameThisCycle++;
				used[1] = true;
				curr = Iunit.getOutputInstruction();
				int k = Iunit.getOutput().intValue();
				if(curr.destination.getName().equals("R0"))
					k = 0;
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
				Ir[iFound] = true;
				Irenaming[iFound] = k;
				renameMap.put(curr,iFound);
				curr.readyToFinish = true;
				//ROB.add(curr);
			}
			else if(Munit.isOutputReady() && iFound != -1 && !used[2])
			{
				exToRenameThisCycle++;
				used[2] = true;
				curr = Munit.getOutputInstruction();
				int k = Munit.getOutput().intValue();
				if(curr.destination.getName().equals("R0"))
					k = 0;
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
				Ir[iFound] = true;
				Irenaming[iFound] = k;
				renameMap.put(curr,iFound);
				curr.readyToFinish = true;
				//ROB.add(curr);
			}
			else if(Lunit.isOutputReady() && !used[3])
			{
				exToRenameThisCycle++;
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
				curr.readyToFinish = true;
				//ROB.add(curr);
			}
			else if(Funit.isOutputReady() && fFound != -1 && ! used[4])
			{
				exToRenameThisCycle++;
				used[4] = true;
				curr = Funit.getOutputInstruction();
				double k = Funit.getOutput().doubleValue();
				if(curr.destination.getName().equals("R0"))
					k = 0;
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
				Fr[fFound] = true;
				Frenaming[fFound] = k;
				curr.readyToFinish = true;
				//ROB.add(curr);
				renameMap.put(curr,fFound);				
			}
			else if(Dunit.isOutputReady() && fFound != -1 && !used[5])
			{
				exToRenameThisCycle++;
				used[5] = true;
				curr = Dunit.getOutputInstruction();
				double k = Dunit.getOutput().doubleValue();
				if(curr.destination.getName().equals("R0"))
					k = 0;
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
				Fr[fFound] = true;
				Frenaming[fFound] = k;
				curr.readyToFinish = true;
				//ROB.add(curr);
				renameMap.put(curr,fFound);
			}
			
			
		}
	}
	public static void computeStatistics()
	{
		robUtil = robUtil + ROB.util();
		fetchUtil = fetchUtil + fetch.util();
		decodeUtil = decodeUtil+decoder.util();
		for(int i = 0; i < 6; i++)
			stationsUtil[i] = stationsUtil[i] + stations[i].util();
		int iCount = 0;
		int fCount = 0;
		for (int i = 0; i < intRenaming; i++)
		{
			if(Ir[i] == true)
				iCount++;
			if(Fr[i] == true)
				fCount++;
		}
		renameUtilI = renameUtilI + ((double)iCount/(double)intRenaming);
		renameUtilF = renameUtilF + ((double)fCount/(double)floatRenaming);
		exToRenameUtil = exToRenameUtil + ((double)exToRenameThisCycle/(double) execToWrite);
		renameToRegUtil = renameToRegUtil + ((double) renameToRegThisCycle / (double) renameToActual);
		if(cache.isBusy())
			cacheUtil++;
		totalIssued++;
	}
	public static void printStatistics()
	{
		System.out.println("--------------Here are the computed statistics for this simulation---------------\n\n\n");
		System.out.println("Reorder Buffer utilization: " + robUtil/totalIssued);
		System.out.println("Fetch Unit utilization: " + fetchUtil/totalIssued);
		System.out.println("Decoder Unit utilization: " + decodeUtil/totalIssued);
		//I,M,L,F,D,B
		System.out.println("Integer Res Station Utilization: " + stationsUtil[0]/totalIssued);
		System.out.println("Multiplication Res Station Utilization: " + stationsUtil[1]/totalIssued);
		System.out.println("Load/Store Res Station Utilization: " + stationsUtil[2]/totalIssued);
		System.out.println("Floating Point Res Station Utilization: " + stationsUtil[3]/totalIssued);
		System.out.println("Division Unit Res Station Utilization: " + stationsUtil[4]/totalIssued);
		System.out.println("Branch Unit Res Station Utilization: " + stationsUtil[5]/totalIssued);
		System.out.println("Integer Renaming Register Utilization: " + renameUtilI/totalIssued);
		System.out.println("Floating Point Renaming Register Utilization: " + renameUtilF/totalIssued);
		System.out.println("Execution to Renaming Bus Utilization: " + exToRenameUtil/totalIssued);
		System.out.println("Renaming to Registers Bus Utilization: " + renameToRegUtil/totalIssued);
		System.out.println("Average Data Cache Utilization: " + cacheUtil/totalIssued);
		System.out.println("Total Clock Cycles Used: " + totalIssued);
		System.out.println("Total Instructions Committed: " + InstructionsCommitted);

	}
	public static void main(String [] args) throws Exception
	{
		getCommands(args);
		//Construct All Objects
		//******************
		robUtil = 0;
		fetchUtil = 0;
		decodeUtil = 0;
		stationsUtil  = new double[6];
		 renameUtilI = 0;
		 renameUtilF = 0;
		 exToRenameUtil = 0;
		 renameToRegUtil = 0;
		cacheUtil = 0;
		 totalIssued = 0;
		InstructionsCommitted = 0;
		ROB = new ReorderBuffer(nr);
		cache = new dataCache();
		needResult = new HashMap<Instruction,ArrayList<Instruction>>();
		intRegisters = new int [32];
		floatRegisters = new double [32];
		renameMap = new HashMap<Instruction,Integer>();
		memLocations = new HashMap<Instruction,Integer>();
		branchResult = new HashMap<Instruction,Integer>();
		branchPreds = new HashMap<Integer,Integer>();
		currPrediction = new HashMap<Instruction,Integer>();
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
		amStalling = false;
		
		
		//******************
		
		
		int counter = 0;
		//Begin Main Simulation Loop
		while(true)
		{
			resetClocks(); //Start new clock cycles
			commitInstructions();
			computeResults();
			updateExecution();	//EXECUTE 
			//Need to update all instructions that may now have their operands ready after
			//Execution phase is done for other instructions
			//need to check for branching and set pointer if necessary
			//IF someone is going to write to memory location that you're using, wait for the result to be available and know the location 
			//Can't know the location until after effective address calculation
			//Otherwise, just access memory, but need to wait for data cache to be available
			if(!amStalling)
			{
				boolean [] onlyOne = new boolean[6]; // can only issue one instruction per reservationStation per cycle
				for(int i = 0; i < nw; i++) //send the instruction to the appropriate reservationStation
				{
					if(decoder.peek()!=null && ROB.isSpace())
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
								if(predictBranch == false)
								{
									i = nw + 1;
									amStalling = true;
								}
								//If there's no entry, then we predict not taken
								//If there is an entry and it was a taken entry, then moveee baby move
								else //Okay, so we need to look at the previous destination of the current branch and use it as the prediction
								{
									int currentInstruc = fetch.getAddress(curr);
									if(branchPreds.get(Integer.valueOf(currentInstruc)) != null)
									{
										int dest = branchPreds.get(Integer.valueOf(currentInstruc)); //predict taken to dest!
										if(dest != fetch.getAddress(curr) + 1) // if it isn't the next instruction
										{
											currPrediction.put(curr,1); //also clear fetch and decode units
											fetch.clear(dest);//set fetch's pointer to the destination
											decoder.clear();
										}
										else //predict not taken
										{
											currPrediction.put(curr,0);
										}
									}
									else // predict not taken
									{
										currPrediction.put(curr,0);
									}
								}
								if(curr.destination != null && curr.operandOne != null) //BEQ BNEQ
									curr.operandTwo = curr.destination;
								if(curr.operandOne == null && curr.operandTwo == null)
								{
									curr.operandOne = curr.destination;
								}
							}
						}
						if(curr!=null && curr.operandOne!= null) 
						{
							int counterOne = ROB.numInst()-1;
							boolean foundItOne = false;
							while(counterOne >= 0 && !foundItOne)
							{
								if(ROB.getIndex(counterOne).destination.getName().equals(curr.operandOne.getName()) && !ROB.getIndex(counterOne).instructionType.isStore() && !ROB.getIndex(counterOne).instructionType.isBranch())
									foundItOne = true;
								counterOne--;
							}
							counterOne++;
							if(!foundItOne) //Simply Read value from the register
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
							else //get the instruction and check if the value is ready
							{
								Instruction ii = ROB.getIndex(counterOne);
								if(ii.readyToFinish) //if it's a load get the value from memlocations,
								{
									int renameIndex = renameMap.get(ii);
									if(curr.operandOne.getName().startsWith("R"))
									{
										int value = Irenaming[renameIndex];
										curr.setOpOne(value);
									}
									else
									{
										double value = Frenaming[renameIndex];
										curr.setOpOne(value);
									}
									
								}
								else
								{
								
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
							}
							if(curr.operandTwo!= null) //Make sure it isn't a store operation
							{
								int counter2 = ROB.numInst()-1;
								boolean foundItTwo = false;
								while(counter2 >= 0 && !foundItTwo)
								{
									if(ROB.getIndex(counter2).destination.getName().equals(curr.operandTwo.getName())&& !ROB.getIndex(counter2).instructionType.isStore() && !ROB.getIndex(counter2).instructionType.isBranch())
										foundItTwo = true;
									counter2--;
								}
								counter2++;
								
								if(!foundItTwo)
								{
								
									if(curr.operandTwo.getName().startsWith("R"))
									{
										curr.setOpTwo(Integer.valueOf(getValI(curr.operandTwo.getName()))); //Here is the error!
									}
									else
									{
										curr.setOpTwo(Double.valueOf(getValF(curr.operandTwo.getName()))); //Also error...
									}
								}
								else //Store the location
								{
									Instruction ii = ROB.getIndex(counter2);
									if(renameMap.get(ii)!=null && ii.readyToFinish)
									{
										int renameIndex = renameMap.get(ii);
										if(curr.operandTwo.getName().startsWith("R"))
										{
											int value = Irenaming[renameIndex];
											curr.setOpTwo(value);
										}
										else
										{
											double value = Frenaming[renameIndex];
											curr.setOpTwo(value);
										}
									
									}
									else
									{
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
								}
							}
							else
							{
								curr.isReadyTwo = true;
							}
						}
						if(curr!=null)
							ROB.add(curr);
					}
				}
				if(!amStalling)
				{
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
				}
			}
			computeStatistics();
			
			//Next step, send instructions from reservation stations to execution units
			//Then send instructions to ROB for Write Back
			//Keep track of all relevant statistics for this entire process
			counter++;
			/*System.out.println(counter+"\n\n\n\n");
			System.out.println("----------Fetch Unit-----------\n\n");
				System.out.println(fetch);
			System.out.println("--------------Decode Unit--------------\n\n");
				System.out.println(decoder);
			System.out.println("--------------------Reservation Stations-------------\n\n\n");
				for(int i = 0; i < 6; i++)
					System.out.println("Reservation Station " + i + " contains " + stations[i]);
			System.out.println("\n\n---------------------Need Result-------------------\n\n");
			System.out.println(needResult);
			System.out.println("\n\n---------------------------Rename Map--------------------\n\n");
			System.out.println(renameMap);
			System.out.println("\n\n---------------------------Reorder Buffer------------------\n\n");
			System.out.println(ROB);
			//System.out.println("\n\n ------------Branch Predictor -------------\n\n");
			//System.out.println(branchPreds);*/
			if(everythingIsEmpty()) //Check if all instructions have been processed
				break;
		}
		if(registerDump)
		{
			System.out.println("-------------Here are the registers!--------------\n\n\n");
			System.out.println("Integer Registers: " + Arrays.toString(intRegisters));
			System.out.println("Floating Point Registers: " + Arrays.toString(floatRegisters));
		}
		if(memoryDump)
		{
			System.out.println("\n\n----------------Here is the memory location requested from (" + dumpStart + "," + dumpEnd + ") --------------------------\n\n");
			for(Integer i : memory.keySet())
			{
				if(i.intValue() > dumpStart && i.intValue() < dumpEnd)
				{
					System.out.println(i + " : " + memory.get(i));
				}
			}
		}
		if(branchDump)
		{
			System.out.println("\n\n---------------Here is the branch prediction information----------------\n\n");
			for(Integer a : branchPreds.keySet())
			{
				System.out.println("Instruction address: " + a + " is predicted to branch to " + branchPreds.get(a));
			}
		}
		printStatistics();
		
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
			
			else if (args[i].toUpperCase().equals("BRANCH"))
				predictBranch = false;
			else if(args[i].equals("--dump_mem"))
			{
				memoryDump = true;
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
			else if(args[i].toUpperCase().equals("LOAD"))
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
