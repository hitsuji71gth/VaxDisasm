import java.io.IOException;
import java.util.Arrays;


public class VaxDisasm implements Disasm {

	private static final int MAXREGSIZE = 16;
	private static final int MAXMEMSIZE = 65536;
	private static final int PC = 15;
	
	private int[] reg = new int[MAXREGSIZE];
	private String[] regLabel = 
		{"r0", "r1", "r2", "r3", "r4", "r5", "r6", "r7", "r8", "r9", "r10", "r11", "ap", "fp", "sp", "pc"};
	private Memory mem = new Memory(MAXMEMSIZE);
	private int tsize;
	private int dsize;
	
	enum VAXOp {

	    HALT(0x00, ""), NOP(0x01, ""), REI(0x02, ""), BPT(0x03, ""),
	    RET(0x04, ""), RSB(0x05, ""), LDPCTX(0x06, ""), SVPCTX(0x07, ""),
	    CVTPS(0x08, "wbwb"), CVTSP(0x09, "wbwb"), INDEX(0x0a, "llllll"), CRC(0x0b, "blwb"),
	    PROBER(0x0c, "bwb"), PROBEW(0x0d, "bwb"), INSQUE(0x0e, "bb"), REMQUE(0x0f, "bl"),
	    BSBB(0x10, "1"), BRB(0x11, "1"), BNEQ(0x12, "1"), BEQL(0x13, "1"),
	    BGTR(0x14, "1"), BLEQ(0x15, "1"), JSB(0x16, "b"), JMP(0x17, "b"),
	    BGEQ(0x18, "1"), BLSS(0x19, "1"), BGTRU(0x1a, "1"), BLEQU(0x1b, "1"),
	    BVC(0x1c, "1"), BVS(0x1d, "1"), BCC(0x1e, "1"), BLSSU(0x1f, "1"),
	    ADDP4(0x20, "wbwb"), ADDP6(0x21, "wbwbwb"), SUBP4(0x22, "wbwb"), SUBP6(0x23, "wbwbwb"),
	    CVTPT(0x24, "wbbwb"), MULP(0x25, "wbwbwb"), CVTTP(0x26, "wbbwb"), DIVP(0x27, "wbwbwb"),
	    MOVC3(0x28, "wbb"), CMPC3(0x29, "wbb"), SCANC(0x2a, "wbbb"), SPANC(0x2b, "wbbb"),
	    MOVC5(0x2c, "wbbwb"), CMPC5(0x2d, "wbbwb"), MOVTC(0x2e, "wbbbwb"), MOVTUC(0x2f, "wbbbwb"),
	    BSBW(0x30, "2"), BRW(0x31, "2"), CVTWL(0x32, "wl"), CVTWB(0x33, "wb"),
	    MOVP(0x34, "wbb"), CMPP3(0x35, "wbb"), CVTPL(0x36, "wbl"), CMPP4(0x37, "wbwb"),
	    EDITPC(0x38, "wbbb"), MATCHC(0x39, "wbwb"), LOCC(0x3a, "bwb"), SKPC(0x3b, "bwb"),
	    MOVZWL(0x3c, "wl"), ACBW(0x3d, "www2"), MOVAW(0x3e, "wl"), PUSHAW(0x3f, "w"),
	    ADDF2(0x40, "ff"), ADDF3(0x41, "fff"), SUBF2(0x42, "ff"), SUBF3(0x43, "fff"),
	    MULF2(0x44, "ff"), MULF3(0x45, "fff"), DIVF2(0x46, "ff"), DIVF3(0x47, "fff"),
	    CVTFB(0x48, "fb"), CVTFW(0x49, "fw"), CVTFL(0x4a, "fl"), CVTRFL(0x4b, "fl"),
	    CVTBF(0x4c, "bf"), CVTWF(0x4d, "wf"), CVTLF(0x4e, "lf"), ACBF(0x4f, "fff2"),
	    MOVF(0x50, "ff"), CMPF(0x51, "ff"), MNEGF(0x52, "ff"), TSTF(0x53, "f"),
	    EMODF(0x54, "fbflf"), POLYF(0x55, "fwb"), CVTFD(0x56, "fd"),
	    ADAWI(0x58, "ww"),
	    INSQHI(0x5c, "bq"), INSQTI(0x5d, "bq"), REMQHI(0x5e, "ql"), REMQTI(0x5f, "ql"),
	    ADDD2(0x60, "dd"), ADDD3(0x61, "ddd"), SUBD2(0x62, "dd"), SUBD3(0x63, "ddd"),
	    MULD2(0x64, "dd"), MULD3(0x65, "ddd"), DIVD2(0x66, "dd"), DIVD3(0x67, "ddd"),
	    CVTDB(0x68, "db"), CVTDW(0x69, "dw"), CVTDL(0x6a, "dl"), CVTRDL(0x6b, "dl"),
	    CVTBD(0x6c, "bd"), CVTWD(0x6d, "wd"), CVTLD(0x6e, "ld"), ACBD(0x6f, "ddd2"),
	    MOVD(0x70, "dd"), CMPD(0x71, "dd"), MNEGD(0x72, "dd"), TSTD(0x73, "d"),
	    EMODD(0x74, "dbdld"), POLYD(0x75, "dwb"), CVTDF(0x76, "df"),
	    ASHL(0x78, "bll"), ASHQ(0x79, "bqq"), EMUL(0x7a, "lllq"), EDIV(0x7b, "lqll"),
	    CLRD(0x7c, "d"), MOVQ(0x7d, "qq"), MOVAQ(0x7e, "ql"), PUSHAQ(0x7f, "q"),
	    ADDB2(0x80, "bb"), ADDB3(0x81, "bbb"), SUBB2(0x82, "bb"), SUBB3(0x83, "bbb"),
	    MULB2(0x84, "bb"), MULB3(0x85, "bbb"), DIVB2(0x86, "bb"), DIVB3(0x87, "bbb"),
	    BISB2(0x88, "bb"), BISB3(0x89, "bbb"), BICB2(0x8a, "bb"), BICB3(0x8b, "bbb"),
	    XORB2(0x8c, "bb"), XORB3(0x8d, "bbb"), MNEGB(0x8e, "bb"), CASEB(0x8f, "bbb"),
	    MOVB(0x90, "bb"), CMPB(0x91, "bb"), MCOMB(0x92, "bb"), BITB(0x93, "bb"),
	    CLRB(0x94, "b"), TSTB(0x95, "b"), INCB(0x96, "b"), DECB(0x97, "b"),
	    CVTBL(0x98, "bl"), CVTBW(0x99, "bw"), MOVZBL(0x9a, "bl"), MOVZBW(0x9b, "bw"),
	    ROTL(0x9c, "bll"), ACBB(0x9d, "bbb2"), MOVAB(0x9e, "bl"), PUSHAB(0x9f, "b"),
	    ADDW2(0xa0, "ww"), ADDW3(0xa1, "www"), SUBW2(0xa2, "ww"), SUBW3(0xa3, "www"),
	    MULW2(0xa4, "ww"), MULW3(0xa5, "www"), DIVW2(0xa6, "ww"), DIVW3(0xa7, "www"),
	    BISW2(0xa8, "ww"), BISW3(0xa9, "www"), BICW2(0xaa, "ww"), BICW3(0xab, "www"),
	    XORW2(0xac, "ww"), XORW3(0xad, "www"), MNEGW(0xae, "ww"), CASEW(0xaf, "www"),
	    MOVW(0xb0, "ww"), CMPW(0xb1, "ww"), MCOMW(0xb2, "ww"), BITW(0xb3, "ww"),
	    CLRW(0xb4, "w"), TSTW(0xb5, "w"), INCW(0xb6, "w"), DECW(0xb7, "w"),
	    BISPSW(0xb8, "w"), BICPSW(0xb9, "w"), POPR(0xba, "w"), PUSHR(0xbb, "w"),
	    CHMK(0xbc, "w"), CHME(0xbd, "w"), CHMS(0xbe, "w"), CHMU(0xbf, "w"),
	    ADDL2(0xc0, "ll"), ADDL3(0xc1, "lll"), SUBL2(0xc2, "ll"), SUBL3(0xc3, "lll"),
	    MULL2(0xc4, "ll"), MULL3(0xc5, "lll"), DIVL2(0xc6, "ll"), DIVL3(0xc7, "lll"),
	    BISL2(0xc8, "ll"), BISL3(0xc9, "lll"), BICL2(0xca, "ll"), BICL3(0xcb, "lll"),
	    XORL2(0xcc, "ll"), XORL3(0xcd, "lll"), MNEGL(0xce, "ll"), CASEL(0xcf, "lll"),
	    MOVL(0xd0, "ll"), CMPL(0xd1, "ll"), MCOML(0xd2, "ll"), BITL(0xd3, "ll"),
	    CLRF(0xd4, "f"), TSTL(0xd5, "l"), INCL(0xd6, "l"), DECL(0xd7, "l"),
	    ADWC(0xd8, "ll"), SBWC(0xd9, "ll"), MTPR(0xda, "ll"), MFPR(0xdb, "ll"),
	    MOVPSL(0xdc, "l"), PUSHL(0xdd, "l"), MOVAL(0xde, "ll"), PUSHAL(0xdf, "l"),
	    BBS(0xe0, "lb1"), BBC(0xe1, "lb1"), BBSS(0xe2, "lb1"), BBCS(0xe3, "lb1"),
	    BBSC(0xe4, "lb1"), BBCC(0xe5, "lb1"), BBSSI(0xe6, "lb1"), BBCCI(0xe7, "lb1"),
	    BLBS(0xe8, "l1"), BLBC(0xe9, "l1"), FFS(0xea, "lbbl"), FFC(0xeb, "lbbl"),
	    CMPV(0xec, "lbbl"), CMPZV(0xed, "lbbl"), EXTV(0xee, "lbbl"), EXTZV(0xef, "lbbl"),
	    INSV(0xf0, "llbb"), ACBL(0xf1, "lll2"), AOBLSS(0xf2, "ll1"), AOBLEQ(0xf3, "ll1"),
	    SOBGEQ(0xf4, "l1"), SOBGTR(0xf5, "l1"), CVTLB(0xf6, "lb"), CVTLW(0xf7, "lw"),
	    ASHP(0xf8, "bwbbwb"), CVTLP(0xf9, "lwb"), CALLG(0xfa, "bb"), CALLS(0xfb, "lb"),
	    XFC(0xfc, ""),
	    CVTDH(0xfd32, "dh"), CVTGF(0xfd33, "gh"),
	    ADDG2(0xfd40, "gg"), ADDG3(0xfd41, "ggg"), SUBG2(0xfd42, "gg"), SUBG3(0xfd43, "ggg"),
	    MULG2(0xfd44, "gg"), MULG3(0xfd45, "ggg"), DIVG2(0xfd46, "gg"), DIVG3(0xfd47, "ggg"),
	    CVTGB(0xfd48, "gb"), CVTGW(0xfd49, "gw"), CVTGL(0xfd4a, "gl"), CVTRGL(0xfd4b, "gl"),
	    CVTBG(0xfd4c, "bg"), CVTWG(0xfd4d, "wg"), CVTLG(0xfd4e, "lg"), ACBG(0xfd4f, "ggg2"),
	    MOVG(0xfd50, "gg"), CMPG(0xfd51, "gg"), MNEGG(0xfd52, "gg"), TSTG(0xfd53, "g"),
	    EMODG(0xfd54, "gwglg"), POLYG(0xfd55, "gwb"), CVTGH(0xfd56, "gh"),
	    ADDH2(0xfd60, "hh"), ADDH3(0xfd61, "hhh"), SUBH2(0xfd62, "hh"), SUBH3(0xfd63, "hhh"),
	    MULH2(0xfd64, "hh"), MULH3(0xfd65, "hhh"), DIVH2(0xfd66, "hh"), DIVH3(0xfd67, "hhh"),
	    CVTHB(0xfd68, "hb"), CVTHW(0xfd69, "hw"), CVTHL(0xfd6a, "hl"), CVTRHL(0xfd6b, "hl"),
	    CVTBH(0xfd6c, "bh"), CVTWH(0xfd6d, "wh"), CVTLH(0xfd6e, "lh"), ACBH(0xfd6f, "hhh2"),
	    MOVH(0xfd70, "hh"), CMPH(0xfd71, "hh"), MNEGH(0xfd72, "hh"), TSTH(0xfd73, "h"),
	    EMODH(0xfd74, "hwhlh"), POLYH(0xfd75, "hwb"), CVTHG(0xfd76, "hg"),
	    CLRH(0xfd7c, "h"), MOVO(0xfd7d, "oo"), MOVAH(0xfd7e, "hl"), PUSHAH(0xfd7f, "h"),
	    CVTFH(0xfd98, "fh"), CVTFG(0xfd99, "fg"),
	    CVTHF(0xfdf6, "hf"), CVTHD(0xfdf7, "hd"),
	    BUGL(0xfffd, "l"), BUGW(0xfffe, "w");

	    public static final VAXOp[] table = new VAXOp[0x10000];
	    public final int op;
	    public final String mne;
	    public final char[] oprs;

	    static {
	        for (VAXOp op : VAXOp.values()) {
	            table[op.op] = op;
	        }
	    }

	    private VAXOp(int op, String oprs) {
	        this.op = op;
	        this.mne = toString().toLowerCase();
	        this.oprs = oprs.toCharArray();
	    }
	}

	@Override
	public void load(String path) throws IOException {
		byte[] raw = java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(path));
		tsize = raw[4] & 0xff | (raw[5] & 0xff) << 8 | (raw[6] & 0xff) << 16 | (raw[7] & 0xff) << 24;
		dsize = raw[8] & 0xff | (raw[9] & 0xff) << 8 | (raw[10] & 0xff) << 16 | (raw[11] & 0xff) << 24;
		mem.load(Arrays.copyOfRange(raw, 0x20, raw.length), 0);
	}
	
	@Override
	public void disasm() {
		while(reg[PC] < tsize) {
			System.out.println(generateOpStr());
		}
	}
	
	private String generateOpStr() {
		int pcOld = reg[PC];
		int opc = fetchMem(1);
		VAXOp op = VAXOp.table[opc];
		opc = op == null ? opc = opc << 8 | fetchMem(1) : opc;
		op = VAXOp.table[opc];
		String sOp = op == null ? String.format(".word %#x", opc) : op.mne + generateOprsStr(op.oprs);
		return formatOpStr(sOp, pcOld, reg[PC] - 1);
	}
	
	private String formatOpStr(String sOp, int pcFrom, int pcTo) {
		String sFormatOp = "";
		int i = 0;
		for (int pc = pcFrom; pc <= pcTo; pc++) {
			if (i % 4 == 0) {
				if (i != 0) sFormatOp += "\n";
				sFormatOp += String.format("%4x:\t", pc);
			}
			sFormatOp += String.format("%02x ", mem.fetch(pc, 1));
			if (i == 3) sFormatOp += String.format("\t%s", sOp);
			i++;
		}
		for (int j = i; j < 4; j++) {
			sFormatOp += "   ";
			if (j == 3) sFormatOp += String.format("\t%s", sOp);
		}
		return sFormatOp;
	}
	
	private String generateOprsStr(char[] oprs) {
		boolean isFirst = true;
		String sOprs = "";
		for (char opr: oprs) {
			sOprs = isFirst ? sOprs + " " : sOprs + ",";
			isFirst = false;
			switch (opr) {
			case 'b':
			case 'w':
			case 'l':
			case 'q':
			case 'o':
			case 'f':
			case 'd':
			case 'g':
			case 'h':
				sOprs += generateOprStr(opr);
				break;
			case '1':
			case '2':
				sOprs += generateOprImmStr(opr);
				break;
			default:
				throw new IllegalStateException("ILLEGAL OPERAND: " + opr);
			}
		}
		return sOprs;
	}
	
	private String generateOprStr(char opr) {
		int val = fetchMem(1);
		if        (0x00 <= val && val <= 0x3f) {
			return String.format("$%#x%s", val, getSuffix(opr));
		} else if (0x40 <= val && val <= 0x4f) {
			return String.format("%s[%s]", generateOprStr(opr), regLabel[val & 0x0f]);
		} else if (0x50 <= val && val <= 0x5f) {
			return regLabel[val & 0x0f];
		} else if (0x60 <= val && val <= 0x6f) {
			return String.format("(%s)", regLabel[val & 0x0f]);
		} else if (0x70 <= val && val <= 0x7f) {
			return String.format("-(%s)", regLabel[val & 0x0f]);
		} else if (0x80 <= val && val <= 0x8e) {
			return String.format("(%s)+", regLabel[val & 0x0f]);
		} else if (0x8f == val) {
			int nextVal = fetchMem(getSize(opr));
			return String.format("$%#0" + (getSize(opr) * 2 + 2) + "x%s", nextVal,getSuffix(opr));
		} else if (0x90 <= val && val <= 0x9e) {
			return String.format("@(%s)+", regLabel[val & 0x0f]);
		} else if (0x9f == val) {
			int nextVal = fetchMem(4);
			return String.format("*%#x", nextVal);
		} else if (0xa0 <= val && val <= 0xaf) {
			int nextVal = fetchMem(1);
			nextVal = nextVal < 0x80 ? nextVal : nextVal | 0xffffff00;
			if (val == 0xaf) {
				return String.format("%#x", (nextVal + reg[PC]) & 0xffffffff);
			} else {
				return String.format("%#x(%s)", nextVal, regLabel[val & 0x0f]);
			}
		} else if (0xb0 <= val && val <= 0xbf) {
			int nextVal = fetchMem(1);
			nextVal = nextVal < 0x80 ? nextVal : nextVal | 0xffffff00;
			if (val == 0xbf) {
				return String.format("*%#x", (nextVal + reg[PC]) & 0xffffffff);
			} else {
				return String.format("*%#x(%s)", nextVal, regLabel[val & 0x0f]);
			}
		} else if (0xc0 <= val && val <= 0xcf) {
			int nextVal = fetchMem(2);
			nextVal = nextVal < 0x8000 ? nextVal : nextVal | 0xffff0000;
			if (val == 0xcf) {
				return String.format("%#x", (nextVal + reg[PC]) & 0xffffffff);
			} else {
				return String.format("%#x(%s)", nextVal, regLabel[val & 0x0f]);
			}
		} else if (0xd0 <= val && val <= 0xdf) {
			int nextVal = fetchMem(2);
			nextVal = nextVal < 0x8000 ? nextVal : nextVal | 0xffff0000;
			if (val == 0xdf) {
				return String.format("*%#x", (nextVal + reg[PC]) & 0xffffffff);
			} else {
				return String.format("*%#x(%s)", nextVal, regLabel[val & 0x0f]);
			}
		} else if (0xe0 <= val && val <= 0xef) {
			int nextVal = fetchMem(4);
			if (val == 0xef) {
				return String.format("%#x", (nextVal + reg[PC]) & 0xffffffff);
			} else {
				return String.format("%#x(%s)", nextVal, regLabel[val & 0x0f]);
			}
		} else if (0xf0 <= val && val <= 0xff) {
			int nextVal = fetchMem(4);
			if (val == 0xff) {
				return String.format("*%#x", (nextVal + reg[PC]) & 0xffffffff);
			} else {
				return String.format("*%#x(%s)", nextVal, regLabel[val & 0x0f]);
			}
		} else {
			throw new IllegalStateException("ILLEGAL VALUE: " + val + "@" + (reg[PC] - 1));
		}
	}

	private String generateOprImmStr(char opr) {
		switch (opr) {
		case '1':
			int val = fetchMem(1);
			val = val < 0x80 ? val : val | 0xffffff00;
			return String.format("%#x", (val + reg[PC]) & 0xffffffff);
		case '2':
			val = fetchMem(2);
			val = val < 0x8000 ? val : val | 0xffff0000;
			return String.format("%#x", (val + reg[PC]) & 0xffffffff);
		default:
			throw new IllegalArgumentException("ILLEGAL OPERAND: " + opr);
		}
	}
	
	private int getSize(char opr) {
		switch (opr) {
		case 'b':
			return 1;
		case 'w':
			return 2;
		case 'l':
			return 4;
		case 'q':
			return 8;
		case 'o':
			return 16;
		case 'f':
			return 4;
		case 'd':
		case 'g':
			return 8;
		case 'h':
			return 16;
		default:
			throw new IllegalArgumentException("ILLEGAL OPERAND: " + opr);
		}
	}
	
	private String getSuffix(char opr) {
		switch (opr) {
		case 'b':
		case 'w':
		case 'l':
		case 'q':
		case 'o':
			return "";
		case 'f':
			return " [f-float]";
		case 'd':
			return " [d-float]";
		case 'g':
			return " [g-float]";
		case 'h':
			return " [h-float]";
		default:
			throw new IllegalArgumentException("ILLEGAL OPERAND: " + opr);
		}
	}
	
	private int fetchMem(int size) {
		int val = mem.fetch(reg[PC], size);
		reg[PC] += size;
		return val;
	}
}
