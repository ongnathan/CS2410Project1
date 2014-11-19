package basicUnits.register;

import basicUnits.ClockDependentUnit;

public class Register<T extends Number> extends ClockDependentUnit
{
	private static final String[] PREFIXES = {"R", "F"};
	private T value;
	private String name;
	
	//FIXME
	public Register(String name)
	{
		this((T)(Integer)Integer.MAX_VALUE, name);
	}
	
	public Register(T value, String name)
	{
		super(Integer.MAX_VALUE);
		this.name = name;
		if(this.name.equals("R0"))
		{
			this.value = (T)(Integer)0;
		}
		else
		{
			this.value = value;
		}
	}
	
	public T getValue()
	{
		return value;
	}
	
	public void setValue(T value)
	{
		if(!this.name.equals("R0"))
		{
			this.value = value;
		}
	}
	
	public void setValue(Integer i)
	{
		if(!this.name.equals("R0"))
		{
			this.value = (T)i;
		}
	}
	
	public void setValue(Double d)
	{
		this.value = (T)d;
	}
	
	public String getName()
	{
		return name;
	}
	
	@Override
	public String toString()
	{
		return this.name + " = " + this.value.toString();
	}
	
	/*public static void main(String[] args)
	{
		Register<Integer> r = new Register<Integer>(10, "R1");
		System.out.println(r.getValue());
		r.setValue(20);
		System.out.println(r.getValue());
		Register<Integer> r2 = new Register<Integer>(100, "R2");
		System.out.println(r2.getValue());
		r.setValue(r2.getValue());
		System.out.println(r.getValue());
	}*/
}
