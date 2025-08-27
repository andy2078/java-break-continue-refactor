package ejemplo11;

public class Ejemplo {
	/*
	 * Nested while loops, each containing a break and two methods
	 */
	public void m() {
		int z = 6;
		System.out.println(z);
		int x = 5;
		int cond = 3;
		
		if (cond == 3)
			System.out.println(cond);
		
		while (x < 10) {
			int y = 10;
			double w;
			
			if (x == 5) {
				if (z % 2 == 0)
					break;
			} 
			
			while (z > 8) {
				if (y == 10) { 
					break;
				}
			}
		
			w = 6.2;
			x++;
		 }
	}
	
	public void m2(int j, float f) {
		System.out.println("Algo");
		while (j < 10) {
			if (j % 3 == 0)
				break;
			else j++;
		}
	}
}
