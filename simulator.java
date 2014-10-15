import java.util.*;
import java.io.*;
import basicUnits.ClockDependentUnit;
import basicUnits.instruction.Instruction;
import basicUnits.register.Register;
import operations.*;
import reservationStation.ReservationStation;

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
	public static HashMap<String,ArrayList<Instruction>> needResult; 
	//Maps Register Name to which instruction needs it
	
	public static HashMap<Instruction,Integer> renameMap;
	//Maps instruction to which renaming register has it 
	//Then when an instruction finally commits we can take the instruction out of the ROB
	//and simply free up its renaming register, and commit the value that was there to memory
	public static HashMap<Instruction,Integer> memLocations;
	//Maps location of instruction accessing memory, with the location that it wants to access
	//All of the operationalUnits!!
	public static OperationalUnit Iunit;
	public static OperationalUnit Bunit;
	public static OperationalUnit Dunit;
	public static OperationalUnit Lunit;
	public static OperationalUnit Munit;
	public static OperationalUnit Funit;
	
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
		
	}
	public static boolean everythingIsEmpty()
	{
		if(fetch.isEmpty() && decoder.isEmpty()) //need to add the rest of the objects just the reorder buffer
		{
			for (int i = 0; i < 6; i++)
			{
				if(!stations[i].isEmpty())
					return false;
			}
			return true;
		}
		return false;
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
			
			if(Bunit.isOutputReady() && ! used[0])
			{
				used[0] = true;
				curr = Bunit.getOutputInstruction();
			}
			if(Iunit.isOutputReady() && iFound != -1 && !used[1])
			{
				used[1] = true;
				curr = Iunit.getOutputInstruction();
				int k = Iunit.getOutput().intValue();
				int resNumber = stations[0].getReservationNumber(curr);
				stations[0].removeInstruction();
				String x = "!I" + resNumber;
				if(needResult.get(curr.destination.getName()) != null)
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
				}
				regLocations.remove(curr.destination.getName());
				Ir[iFound] = true;
				Irenaming[iFound] = k;
				regLocations.put(curr.destination.getName(),"RR" + iFound);
				//Need to put instruction in ROB
				renameMap.put(curr,iFound);
			}
			else if(Munit.isOutputReady() && fFound != -1 && !used[2])
			{
				used[2] = true;
				curr = Munit.getOutputInstruction();
				double k = Munit.getOutput().doubleValue();
				int resNumber = stations[1].getReservationNumber(curr);
				stations[1].removeInstruction();
				String x = "*M" + resNumber;
				if(needResult.get(curr.destination.getName()) != null)
				{
					ArrayList<Instruction> result = needResult.get(curr.destination.getName());
					for(Instruction j :result)
					{
						if(j.operandOne.getName().equals(curr.destination.getName()))
							j.setOpOne(Double.valueOf(k));
						else if(j.operandTwo.getName().equals(curr.destination.getName()))
							j.setOpTwo(Double.valueOf(k));
					}
					needResult.remove(curr.destination.getName());
				}
				regLocations.remove(curr.destination.getName());
				Fr[fFound] = true;
				Frenaming[fFound] = k;
				regLocations.put(curr.destination.getName(),"RF" + fFound);
				//Need to put instruction in ROB
				renameMap.put(curr,fFound);
			}
			else if(Lunit.isOutputReady() && iFound != -1 && !used[3])
			{
				used[3] = true;
				curr = Lunit.getOutputInstruction();
				System.out.println(Lunit.getOutput()+"\n\n");
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
				regLocations.remove(curr.destination.getName());
				regLocations.put(curr.destination.getName(),"memory");
				//Need to put instruction in ROB
			}
			else if(Funit.isOutputReady() && fFound == -1 && ! used[4])
			{
				used[4] = true;
				curr = Funit.getOutputInstruction();
				double k = Funit.getOutput().doubleValue();
				int resNumber = stations[3].getReservationNumber(curr);
				stations[3].removeInstruction();
				String x = "*F" + resNumber;
				if(needResult.get(curr.destination.getName()) != null)
				{
					ArrayList<Instruction> result = needResult.get(curr.destination.getName());
					for(Instruction j :result)
					{
						if(j.operandOne.getName().equals(curr.destination.getName()))
							j.setOpOne(Double.valueOf(k));
						else if(j.operandTwo.getName().equals(curr.destination.getName()))
							j.setOpTwo(Double.valueOf(k));
					}
					needResult.remove(curr.destination.getName());
				}
				regLocations.remove(curr.destination.getName());
				Fr[fFound] = true;
				Frenaming[fFound] = k;
				regLocations.put(curr.destination.getName(),"RF" + fFound);
				//Need to put instruction in ROB
				renameMap.put(curr,fFound);				
			}
			else if(Dunit.isOutputReady() && fFound == -1 && !used[5])
			{
				used[5] = true;
				curr = Dunit.getOutputInstruction();
				double k = Dunit.getOutput().doubleValue();
				int resNumber = stations[4].getReservationNumber(curr);
				stations[4].removeInstruction();
				String x = "*D" + resNumber;
				if(needResult.get(curr.destination.getName()) != null)
				{
					ArrayList<Instruction> result = needResult.get(curr.destination.getName());
					for(Instruction j :result)
					{
						if(j.operandOne.getName().equals(curr.destination.getName()))
							j.setOpOne(Double.valueOf(k));
						else if(j.operandTwo.getName().equals(curr.destination.getName()))
							j.setOpTwo(Double.valueOf(k));
					}
					needResult.remove(curr.destination.getName());
				}
				regLocations.remove(curr.destination.getName());
				Fr[fFound] = true;
				Frenaming[fFound] = k;
				regLocations.put(curr.destination.getName(),"RF" + fFound);
				//Need to put instruction in ROB
				renameMap.put(curr,fFound);
			}
			
			
		}
	}
	public static void main(String [] args) throws FileNotFoundException
	{
		getCommands(args);
		//Construct All Objects
		//******************
		needResult = new HashMap<String,ArrayList<Instruction>>();
		intRegisters = new int [32];
		floatRegisters = new double [32];
		regLocations = new HashMap<String,String>();
		renameMap = new HashMap<Instruction,Integer>();
		memLocations = new HashMap<Instruction,Integer>();
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
		while(counter < 20)
		{
			resetClocks(); //Start new clock cycles  
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
					if(decoder.peek().instructionType.isINT1() && onlyOne[0] == false)
					{
						if(stations[0].addInstruction(decoder.peek()))
						{
							whichPlace = 0;
							onlyOne[0] = true;
							curr = decoder.issue();
							 //Doing this too early,
							//If we have ADD R3, R3, R1 Then R3 will be waited for by this instruction (doesn'tm ake sense)
							//Read the operand values if they are available. If not, know the location of where they are
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
						if(regLocations.get(curr.operandOne.getName()) == null)
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
							if(loc.equals("memory"))
							{
							}
							else if(loc.startsWith("RR") || loc.startsWith("RF")) //Renaming Register Get value from it
							{
								if(loc.charAt(i)=='R')
								{
									curr.setOpOne(Integer.valueOf(getReValI(loc)));
								}
								else
								{
									curr.setOpOne(Double.valueOf(getReValF(loc)));
								}
								
							}
							else //value is in a reservation station, so tell the hashmap that you need it
							{
								ArrayList<Instruction> now = needResult.get(curr.operandOne.getName());
								if(now!=null)
								{
									now.add(curr);
									needResult.put(curr.operandOne.getName(),now);
								}
								else
								{
									now = new ArrayList<Instruction>();
									now.add(curr);
									needResult.put(curr.operandOne.getName(),now);
								}
							}
						}
						if(curr.operandTwo!= null)
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
								if(loc.equals("memory"))
								{
								}
								else if(loc.startsWith("RR") || loc.startsWith("RF")) //Renaming Register Get value from it
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
								else //TODO Hard part, value is in a reservation station
								{
									ArrayList<Instruction> now = needResult.get(curr.operandTwo.getName());
									if(now!=null)
									{
										now.add(curr);
										needResult.put(curr.operandTwo.getName(),now);
									}
									else
									{
										now = new ArrayList<Instruction>();
										now.add(curr);
										needResult.put(curr.operandTwo.getName(),now);
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
							regLocations.put(curr.destination.getName(),"*M" + stations[1].getReservationNumber(curr));
						else if(whichPlace == 2)
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
			System.out.println("-----------------Register Locations-----------------\n\n");
			System.out.println(regLocations);
			System.out.println("---------------------Need Result-------------------\n\n");
			System.out.println(needResult);
			System.out.println("---------------------------Rename Map--------------------\n\n");
			System.out.println(renameMap);
		}
		
		
		
		
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
