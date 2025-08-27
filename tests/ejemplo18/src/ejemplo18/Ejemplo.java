package ejemplo18;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Iterator;

public class Ejemplo {

	/*
	 * Refactoring of an enhanced for-each loop with lists and arrays
	 */
	public void m() {
		int x = 0;
		List<String> nombres1 = new ArrayList<>();
		String nombres2[] = {"Juan", "Silvia", "Ramon", "Laura"};
		
		nombres1.add("Juan");
		nombres1.add("Silvia");
		nombres1.add("Ramon");
		nombres1.add("Laura");
		
		for (String s: nombres1) {
			System.out.println(s);
			if (s.equalsIgnoreCase("Ramon"))
				break;
		}
		
		for (String s: nombres2) {
			System.out.println(s);
			if (s.equalsIgnoreCase("Ramon"))
				break;
		}
	}
}
 