import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {
	
	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			System.out.println("The single argument should be the program to load.");
			return;
		}

		List<Integer> instrBins = new ArrayList<>();

		for (String instrStr : Files.readAllLines(new File("Program" + args[0] + ".txt").toPath())) {
			int instruction = translate(instrStr);
			if (instruction != -1) { // All ones would be an invalid instruction, so I use it to mean "Line was just a comment"
				instrBins.add(instruction);
			}
		}

		Processor processor = new Processor(instrBins.stream().mapToInt(i -> i).toArray());
		int[] results = processor.start();
		for (int result : results) {
			printBits(result);
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
		
		int dest = Integer.parseInt(tokens[1].replace("R", "")) << 23;
		
		int src1 = 0, src2 = 0; // src2 has a max value of 524288 for integer immediates, but that shouldn't matter
							// because raising something to that power would go out of bounds for single precision anyway
		// Get, which is special in that it only has a "destination"
		if (opcode >>> 27 == 1) { } else if (opcode >>> 27 == 0) { // F Type - Parse floating point stuff
			src1 = Float.floatToIntBits(Float.parseFloat(tokens[2].replace("#", "")));
			int sign = (src1 >>> 9) & 0x20000;
			int exp = ((((src1 & 0x7f800000) >>> 23) - 112) << 17) & 0x3E0000;
			int manti = (src1 >>> 7) & 0x1FFFF;
			
			src1 = sign | exp | manti;
		} else if (opcode >> 27 == 15) { // I type - Parse the first and second source, with the second being an unsigned int
			src1 = Integer.parseInt(tokens[2].replace("R", "")) << 19;
			src2 = Integer.parseInt(tokens[3].replace("#", ""));
		} else { // R type - All sources are registers
			src1 = Integer.parseInt(tokens[2].replace("R", "")) << 19;
			if (tokens.length == 4) {
				src2 = Integer.parseInt(tokens[3].replace("R", "")) << 15;
			}
		}
		
		return opcode | dest | src1 | src2; // Assumes that all pieces have already been shifted into the right place
	}
	
	private static boolean[] toBooleanArray(int num) {
		boolean[] bits = new boolean[32];
		for (int i = 31; i >= 0; --i) {
			bits[31-i] = (num & (1 << i)) != 0;
		}
		return bits;
	}
	
	public static boolean[] toBooleanArray(float num) {
		return toBooleanArray(Float.floatToIntBits(num));
	}
	
	private static void printBits(int arrBits) {
		printBits(toBooleanArray(arrBits));
	}

	private static void printBits(float num) {
		printBits(Float.floatToIntBits(num));
	}
	
	private static void printBits(boolean[] arr) {
		System.out.print("[");
		for(boolean temp : arr) {
			System.out.print(temp?"1":"0");
		}
		System.out.println("]");
	}
	
}
