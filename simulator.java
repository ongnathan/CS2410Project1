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
	public static double [] Frenaming; //Float Renaming Registers
	public static HashMap<Integer,Double> memory; //Data Memory
	public static String inputFile; //File name with instructions
	public static ReservationStation [] stations; //Holds each Reservation Station
	public static HashMap<String,Integer> labels; //Holds integer location of each branch label
	
	public static HashMap<String,String> regLocations; 
	/*
	Register name maps to:
	1. Renaming Register in the form "RR1, RR2, etc. OR RF1, RF2, etc. *-> renaming register
	2. Reservation Station in the form "!I1, !I2, *M1, *M2, etc.  ! -> reservation station
	*/
	
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
		if(fetch.isEmpty() && decoder.isEmpty()) //need to add the rest of the objects
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
	
	public static void main(String [] args) throws FileNotFoundException
	{
		getCommands(args);
		//Construct All Objects
		//******************
		intRegisters = new int [32];
		floatRegisters = new double [32];
		regLocations = new HashMap<String,String>();
		Irenaming = new int [intRenaming];
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
		while(counter < 6)
		{
			resetClocks(); //Start new clock cycles  
			//need to check for branching and set pointer if necessary
			
			for(int i = 0; i < 6; i++) //EXECUTE AND HOLD OFF IF THE OPERANDS AREN'T READY!!!
			{
				
			}
			//STILL NEED TO HANDLE MEMORY ACCESS 
			//IF someone is going to write to memory location that you're using, wait for the result to be available and know the location 
			//Can't know the location until after effective address calculation
			//Otherwise, just access memory, but need to wait for data cache to be available
			boolean [] onlyOne = new boolean[6]; // can only issue one instruction per reservationStation per cycle
			for(int i = 0; i < nw; i++) //send the instruction to the appropriate reservationStation
			{
				if(decoder.peek()!=null)
				{
					Instruction curr = null;
					if(decoder.peek().instructionType.isINT1() && onlyOne[0] == false)
					{
						if(stations[0].addInstruction(decoder.peek()))
						{
							onlyOne[0] = true;
							curr = decoder.issue();
							regLocations.put(curr.destination.getName(),"!I" + stations[0].getReservationNumber(curr));
							//Read the operand values if they are available. If not, know the location of where they are
						}
					}
					else if(decoder.peek().instructionType.isMULT() && onlyOne[1] == false)
					{
						if(stations[1].addInstruction(decoder.peek()))
						{
							curr = decoder.issue();
							onlyOne[1] = true;
							regLocations.put(curr.destination.getName(),"*M" + stations[1].getReservationNumber(curr));
						}
					}
					else if(decoder.peek().instructionType.isLOAD()&&onlyOne[2] == false)
					{
						if(stations[2].addInstruction(decoder.peek()))
						{
							curr = decoder.issue();
							onlyOne[2] = true;
							if(curr.destination.getName().startsWith("F"))
								regLocations.put(curr.destination.getName(),"*L"+stations[2].getReservationNumber(curr));
							else
								regLocations.put(curr.destination.getName(),"!L"+stations[2].getReservationNumber(curr));
						}
					}
					else if(decoder.peek().instructionType.isFPU() && onlyOne[3] == false)
					{
						if(stations[3].addInstruction(decoder.peek()))
						{
							curr = decoder.issue();
							onlyOne[3] = true;
							regLocations.put(curr.destination.getName(),"*F"+stations[3].getReservationNumber(curr));
						}
					}
					else if(decoder.peek().instructionType.isFPDIV() && onlyOne[4] == false)
					{
						if(stations[4].addInstruction(decoder.peek()))
						{
							curr = decoder.issue();
							onlyOne[4] = true;
							regLocations.put(curr.destination.getName(),"*D"+stations[4].getReservationNumber(curr));
						}
					}
					else if(decoder.peek().instructionType.isBranch()&&onlyOne[5] == false)
					{
						if(stations[5].addInstruction(decoder.peek()))
						{
							curr = decoder.issue();
							onlyOne[5] = true;
						}
					}
					if(curr!=null) //TODO FIX THE ISSUE WITH THE REGISTERS!!!
					{
						if(regLocations.get(curr.operandOne.getName()) == null)
						{
							if(curr.operandOne.getName().startsWith("!"))
							{
								curr.operandOne.setValue(getValI(curr.operandOne.getName()));
							}
							else
							{
								curr.operandOne.setValue(getValF(curr.operandOne.getName()));
							}
						}
						else
						{
							String loc = regLocations.get(curr.operandOne.getName());
							if(loc.startsWith("RR") || loc.startsWith("RF")) //Renaming Register Get value from it
							{
								if(loc.charAt(i)=='R')
								{
									curr.operandOne.setValue(getReValI(loc));
								}
								else
								{
									curr.operandOne.setValue(getReValF(loc));
								}
								
							}
							else //TODO Hard part, value is in a reservation station
							{
								
							}
						}
						if(curr.operandTwo!= null)
						{
							if(regLocations.get(curr.operandTwo.getName())==null)
							{
								if(curr.operandTwo.getName().startsWith("!"))
								{
									curr.operandTwo.setValue(getValI(curr.operandTwo.getName())); //Here is the error!
								}
								else
								{
									curr.operandTwo.setValue(getValF(curr.operandTwo.getName())); //Also error...
									
								}
							}
							else //Store the location
							{
								String loc = regLocations.get(curr.operandTwo.getName());
								if(loc.startsWith("RR") || loc.startsWith("RF")) //Renaming Register Get value from it
								{
									if(loc.charAt(1)=='R')
									{
										curr.operandTwo.setValue(getReValI(loc));
									}
									else
									{
										curr.operandTwo.setValue(getReValF(loc));
									}
								}
								else //TODO Hard part, value is in a reservation station
								{
								
								}
							}
						}
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
				System.out.println(fetch);
				System.out.println(decoder);
				for(int i = 0; i < 6; i++)
					System.out.println("Reservation Station " + i + " contains " + stations[i]);
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
		if(name.charAt(1)== 'F')
		{
			return(Frenaming[Integer.parseInt(num)]);
		}
	}
	public static int getReValI(String name)
	{
		String num = name.substring(2,name.length());
		if(name.charAt(1)== 'R')
		{
			return(Irenaming[Integer.parseInt(num)]);
		}
		
	}
}
