package ejemplo16;

public class Ejemplo {
	/*
	 * Refactoring of a do-while loop
	 */
	public void m() {
		int z = 6;
		System.out.println(z);
		int x = 5;
		int cond = 3;
		
		int i = 0;
		do {
			int y = 10;
			double w;
			
			if (x == 5) {
				if (z % 2 == 0) {
					if (cond == 3) {
						System.out.println(cond); 
					} else
						break;
					w = 3.14;
				}
				x++;
			}
			z++;
			y++;
			i++;
		 } while (i < 10);
	}
}
