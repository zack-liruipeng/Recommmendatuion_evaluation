package Evaluation.rival3_test;

public class timer {
	private long exe_time;
	public timer(){
		this.exe_time = 0;
		
	}
	
	public void start(){
		this.exe_time = System.nanoTime();
	}
	
	public void end_time(){
		this.exe_time = System.nanoTime() - exe_time;
	}
	
	public long get_exe_time(){
		return exe_time;
	}
}
