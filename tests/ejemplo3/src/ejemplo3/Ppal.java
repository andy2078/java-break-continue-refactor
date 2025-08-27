package ejemplo3;

public class Ppal {

	/*
	 * Simple break in a while loop
	 */
	public static void main(String[] args) {
		int i = 0;
		while (i < 10) {
			if (i == 3) {
				i += 2;
				System.out.println("hola");
				break;
			}
			i++;
		}
	}

}
