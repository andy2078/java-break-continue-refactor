package ejemplo9;

public class Ejemplo {
	/*
	 * Multiple break statements belonging to the same while loop
	 */
	public void m() {
		int z = 6;
		System.out.println(z);
		int x = 5;
		int cond = 3;
		
		while (x < 10) {
			
			int y = 10;
			double w;
			
			if (x == 5)
				break;
			
			if (z % 2 == 0)
				break;
			
			w = 6.2;
			x++;
		 }
	}
}
