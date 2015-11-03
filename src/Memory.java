
public class Memory {

	private final byte[] mem;
	
	public Memory(int size) {
		mem = new byte[size];
	}

	public void load(byte[] raw, int addr) {
		System.arraycopy(raw, 0, mem, addr, raw.length);
	}
	
	public int fetch(int addr, int size) {
		int val = 0;
		for (int i = size-1; i >= 0; i--) {
			val = (val << 8) + (mem[addr+i] & 0xff);
		}
		return val;
	}
	
	public void dump() {
		//TODO dump memory
	}

}
