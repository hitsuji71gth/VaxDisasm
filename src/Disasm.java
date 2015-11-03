import java.io.IOException;


public interface Disasm {
	void load(String path) throws IOException;
	void disasm();
}
