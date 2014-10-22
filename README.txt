To run the default values:
1.	On the command line, change directory to the folder that contains the jar.
2.	Type: “java –jar simulator.jar prog.dat” (without the quotation marks)

To Change Parameters from the Project Description:
On the command line write the name of the parameter followed by a space, followed by the value you want it to have
Ex. java –jar simulator.jar prog.dat nd 5 nr 3

If you write “branch” as an argument, then this will disable branch predicting
If you write “—dump_mem 300 400”, then this will print the contents of memory from address 300 to address 400
If you write “–dump_reg”, then this will print out the contents of all of the registers
If you write “–dump_branch”, then this will print out the contents of the branch predictor

To change the number of reservation stations: 
“INT1” or “MULT”, or “LOAD” or “FPU” or “FPDIV” or “BU” followed by an integer specifying the amount
Ex. java –jar simulator.jar INT1 5 

To change the number of integer and floating point renaming registers:
Type: “INTREG” or “FLOATREG” followed by a number. 

To change the number of busses from the execution stage to the renaming registers:
Type: “EXECBUS” followed by a number

To change the number of busses from the renaming registers to the Write Back:
Type: “RENAMEBUS” followed by a number
