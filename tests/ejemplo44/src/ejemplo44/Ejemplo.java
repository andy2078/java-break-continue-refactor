package ejemplo44;

public class Ejemplo {
	/*
	 * while loop with exception handling: try-catch-finally
	 */
	public void m() {
		int x = 2, y = 3;
		int i = 0;
		while (i < 5) {
			try {
				i++; 
				int z = y/x;
				System.out.println("i: " + i + " - Divido " + y + " / " + x + " = " + z);
				if (x == 1) {
					System.out.println("Pego el salto");
					break;
				}
				x--;
			} 
			catch (Exception e) {
				System.out.println("hubo un error " + e);
			}
			finally {
				System.out.println("Entro al finally");
				if (i == 2)
					break;
				System.out.println("Salgo del finally");
			}
		}
	}
}
