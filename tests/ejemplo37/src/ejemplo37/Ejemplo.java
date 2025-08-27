package ejemplo37;

public class Ejemplo {
	/*
	 * Refactoring of break and continue inside a while loop nested in a do-while loop
	 */
	public void m() {
		int z = 6;
		System.out.println(z);
		int x = 5;
		int cond = 3;
		
		do {
			int y = 10;
			double w;
			
			if (x == 5) {
				if (z % 2 == 0) {
					if (cond == 3) {
						System.out.println(cond); 
						break;
					}
					w = 3.14;
				}
				x++;
			}
			else
				continue;
			while (y < 10) {
				y++;
				if (y == 8)
					continue;
				System.out.println("valor de y: " + y);
			}
			z++;
			y++;
		 } while (x < 10);
	}
}
