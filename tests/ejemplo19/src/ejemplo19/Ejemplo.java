package ejemplo19;

public class Ejemplo {
	/*
	 * Refactoring of a continue statement inside a while loop
	 */
	public void m() {
		int z = 6;
		System.out.println(z);
		int x = 5;
		int cond = 3;
		
		while (x < 10) {
			int y = 10;
			double w;
			
			if (x == 5) {
				if (z % 2 == 0) {
					if (cond == 3) {
						System.out.println(cond); 
						continue;
					}
					w = 3.14;
				}
				x++;
			}
			z++;
			y++;
		 }
	}
}
