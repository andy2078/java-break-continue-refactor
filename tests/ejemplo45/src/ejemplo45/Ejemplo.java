package ejemplo45;

public class Ejemplo {
	/*
	 * while loop with a break and a switch containing break statements
	 */
	public void m() {
		int x = 1, i = 1;
		while (x < 5) {
			switch(i) {
			case 1: System.out.println("uno"); break;
			case 2: System.out.println("dos"); break;
			case 3: System.out.println("tres"); break;
			}
			if (x == 3)
				break;
			System.out.println("Salgo del ciclo");
			x++;
		}
	}
}
