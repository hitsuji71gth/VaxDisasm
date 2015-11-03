import java.io.IOException;


public class Main {

	public static void main(String[] args) {
		if (args.length < 1) {
			System.err.println("No argument");
			return;
		}
		
		VaxDisasm vax = new VaxDisasm();
		try {
			vax.load(args[0]);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		vax.disasm();
		
	}

}
