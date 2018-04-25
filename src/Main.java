import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {

	static List<Boolean> precisionError = new ArrayList<>();
	static  List<Integer> goodVal = new ArrayList<>();

	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			System.out.println("The single argument should be the program to load.");
			return;
		}

		List<Integer> instrBins = new ArrayList<>();

		for (String instrStr : Files.readAllLines(new File("Program" + args[0] + ".txt").toPath())) {
			int instruction = translate(instrStr);
			if (instruction != -1) { // All ones would be an invalid instruction, so it's used to mean that the line was just a comment
				instrBins.add(instruction);
			}
		}

		Processor processor = new Processor(instrBins.stream().mapToInt(i -> i).toArray());
		int[] results = processor.start();
		System.out.println("----Results----");
		for (int result : results) {
			System.out.println(Float.intBitsToFloat(result));
		}

	}

	private static final String[] INSTRUCTION_NAMES = {"Set","Get","Move","Fadd","Fsub","Fneg","Fmul","Fdiv","Floor","Ceil","Round","Fabs",
		"Finv","Min","Max","Pow","Sin","Cos","Tan","Exp","Log","Sqrt"};

	private static int translate(String instrStr) {
		// Regex match comments and clear them out, then remove spaces so that the instruction can be split by commas
		String[] tokens = instrStr.replaceAll("\\s*(--.*)?$", "").replace(" ", ",").split(",+");
		if (tokens.length <= 1) {
			return -1; // Line was just a comment
		}

		int opcode = Arrays.asList(INSTRUCTION_NAMES).indexOf(tokens[0]) << 27; // Index in array is the same as the opcode, so just shift that into the right place
		if (opcode == -1) {
			throw new RuntimeException("Bad instruction keyword: " + tokens[0]);
		}

		int dest = Integer.parseInt(tokens[1].replace("R", "")) << 23;

		int flt = 0, src1 = 0, src2 = 0; // src2 has a max value of 524288 for integer immediates, but that shouldn't matter
		                        // because raising something to that power would go out of bounds for single precision anyway

		if (opcode >>> 27 == 0) { // F Type - Parse floating point stuff
			flt = src1 = Float.floatToIntBits(Float.parseFloat(tokens[2].replace("#", "")));

			int sign = (src1 >>> 9) & 0x400000;
			int exp = ((((src1 & 0x7f800000) >>> 23) - 112) << 17) & 0x3E0000;
			int manti = (src1 >>> 7) & 0x1FFFF;

			src1 = sign | exp | manti;
		} else if (opcode >>> 27 == 15) { // I type - Parse the first and second source, with the second being an unsigned int
			src1 = Integer.parseInt(tokens[2].replace("R", "")) << 19;
			src2 = Integer.parseInt(tokens[3].replace("#", ""));
		} else if (opcode >>> 27 != 1) { // R type - All sources are registers
			src1 = Integer.parseInt(tokens[2].replace("R", "")) << 19;
			if (tokens.length == 4) {
				src2 = Integer.parseInt(tokens[3].replace("R", "")) << 15;
			}
		}

		// Work around for out of bound immediates
		precisionError.add(((((flt & 0x7f800000) >>> 23) - 112) < 0 || (((flt & 0x7f800000) >>> 23) - 112) > 31));
		goodVal.add(flt);

		return opcode | dest | src1 | src2; // Assumes that all pieces have already been shifted into the right place
	}

	static String rightPad(String str, int length) {
		StringBuilder strBuilder = new StringBuilder(str);
		while (strBuilder.length() < length) {
			strBuilder.append(" ");
		}
		str = strBuilder.toString();
		return str;
	}

}
