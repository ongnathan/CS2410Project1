
import java.util.ArrayList;

public class ReorderBuffer {

	public static void main(String[] args, String Inst, Object SmaIns) {
	      
	    // create an empty array list with an initial capacity
	    ArrayList<String> arrlist = new ArrayList<String>();

		// use add() method to add values in the list
	    arrlist.add(Inst);
	   

	    System.out.println(" " + arrlist.size());

	    // let us print all the values available in list
	    for (String value : arrlist) {
	      System.out.println("" + value);
	    }  
		
	    // Removes the smallest value instruction 
	    arrlist.remove(SmaIns);

	    System.out.println(" " + arrlist.size());
		
	    // let us print all the values available in list
	    for (String value : arrlist) {
	      System.out.println(" " + value);
	    }  
	  }
	}   
	
