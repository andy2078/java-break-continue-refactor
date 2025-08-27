package ejemplo39;

public class Ejemplo {
	/*
	 * break statements inside for and while loops
	 */
	public void m() {
		int x = 0;
		for (int i = 0; i < 10; i++) {
			System.out.println("hola");
			if (i == 5)
				if (x % 2 == 0)
					break;
			x++;
			while (x < 10) {
				if (x == 7)
					break;
				x *= 2;
			}
			System.out.println("fin for");
		}
		System.out.println("fin m");
	}
}
