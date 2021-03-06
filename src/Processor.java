import java.util.ArrayList;
import java.util.List;

class Processor {
	
	private int count = 0; // For display purposes
	
	private int[] instructions;
	
	private ControlUnit control = new ControlUnit();
	private RegisterFile registerFile = new RegisterFile();
	private ArithmeticLogicUnit alu = new ArithmeticLogicUnit();
	
	Processor(int[] instructions) {
		this.instructions = instructions;
	}
	
	// Will run until it exhausts all instructions. Returns the result of all Get statements or the contents of all the registers if there weren't any
	int[] start() {
		List<Integer> outputs = new ArrayList<>();
		
		for (int i = 0; i < instructions.length; i++) {
			int instr = instructions[i];
			if (range(instr, 31, 27) == 1) { // If the command is a Get, then just grab the value from the register, log it, and skip the instruction
				outputs.add(registerFile.registers[range(instr, 26, 23)]);
				continue;
			}
			
			control.input(range(instr, 31, 27));
			
			// Set up register file inputs
			registerFile.inputRegWrite(control.regWrite());
			registerFile.inputRR1(range(instr, 22, 19));
			registerFile.inputRR2(range(instr, 18, 15));
			registerFile.inputWR(range(instr, 26, 23));
			
			// Set up ALU inputs
			alu.inputAluOp(control.aluOp());
			alu.inputA(registerFile.outputRD1());
			// Mux
			int b;
			if (control.aluSrc() == 0) {
				b = registerFile.outputRD2();
			} else {
				b = range(instr, 18, 0);
			}
			alu.inputB(b);
			
			// Mux
			int wd;
			if (control.regWriteSrc() == 0) {
				wd = alu.process();
			} else {
				if (Main.precisionError.get(i)) { // Set wd to a good value if it went out of bounds
					wd = Main.goodVal.get(i);
				} else {
					wd = Float.floatToIntBits(customToSinglePrecision(range(instr, 22, 0)));
				}
			}
			
			registerFile.inputWD(wd);
			System.out.println(Main.rightPad(Main.rightPad(++count + ":", 4) +Float.intBitsToFloat(wd) + "", 17) + "-> R" + range(instr, 26, 23));
			registerFile.process(); // Update the register file for the new value
		}
		
		if (outputs.size() == 0) {
			System.out.println("No Get instructions in program. Outputting all register values");
		}
		return outputs.size() == 0 ? registerFile.registers : outputs.stream().mapToInt(i -> i).toArray(); // Dealing with type safety gone wrong
	}
	
	// Mask a range of bits and return them shifted to the right
	// Range is inclusive on both ends
	// Left is more significant, right is less (31-0)
	private int range(int bits, int left, int right) {
		if (right > left) {
			throw new RuntimeException("Bad range parameters: " + left + ", " + right);
		}
		
		int mask = 0xFFFFFFFF;
		mask >>>= 31 - (left - right);
		mask <<= right;
		return (bits & mask) >>> right;
	}
	
	// Expects a 23 bit custom precision number as stated in the first milestone document
	private static float customToSinglePrecision(int bits) {
		int sign = (bits & 0x400000) << 9;
		int exp = ((((bits & 0x3E0000) >>> 17) + 112) << 23) & 0x7F800000;
		int manti = (bits & 0x1FFFF) << 7;
		
		return Float.intBitsToFloat(sign | exp | manti);
	}
	
	public static class ControlUnit {
		
		int opcode = 0;
		
		ControlUnit() {
			
		}
		
		void input(int opcode) {
			this.opcode = opcode;
		}
		
		int regWrite() {
			return 1;
		}
		
		int regWriteSrc() {
			return opcode == 0 ? 1 : 0;
		}
		
		int aluSrc() {
			if (opcode == 0) {
				return 1;
			}
			
			if (opcode == 15) {
				return 2;
			}
			
			return 0;
		}
		
		int aluOp() {
			return opcode == 1?0:opcode-2;
		}
	}
	
	public static class RegisterFile {
		int[] registers;
		
		int RR1, RR2;
		int WR, WD;
		int regWrite;
		
		RegisterFile() {
			registers = new int[16];
		}
		
		void inputRR1(int RR1) {
			this.RR1 = RR1;
		}
		
		void inputRR2(int RR2) {
			this.RR2 = RR2;
		}
		
		void inputWR(int WR) {
			this.WR = WR;
		}
		
		void inputWD(int WD) {
			this.WD = WD;
		}
		
		void inputRegWrite(int regWrite) {
			this.regWrite = regWrite;
		}
		
		int outputRD1() {
			return registers[RR1];
		}
		
		int outputRD2() {
			return registers[RR2];
		}
		
		void process() {
			if (regWrite == 0) {
				return;
			}
			
			registers[WR] = WD;
		}
	}
	
	public static class ArithmeticLogicUnit {
		int aluOp;
		int a, b;
		
		ArithmeticLogicUnit() {
			
		}
		
		void inputAluOp(int aluOp) {
			this.aluOp = aluOp;
		}
		
		void inputA(int a) {
			this.a = a;
		}
		
		void inputB(int b) {
			this.b = b;
		}
		
		int process() {
			switch (aluOp) {
				case 0:  return a;
				case 1:  return addition();
				case 2:  return subtraction();
				case 3:  return negate();
				case 4:  return multiplication();
				case 5:  return division();
				case 6:  return floor();
				case 7:  return ceiling();
				case 8:  return round();
				case 9:  return absolute();
				case 10: return inverse();
				case 11: return minimum();
				case 12: return maximum();
				case 13: return power();
				case 14: return sine();
				case 15: return cosine();
				case 16: return tangent();
				case 17: return exponent();
				case 18: return logarithm();
				case 19: return squareRoot();
			}
			throw new RuntimeException("Invalid aluOps value: " + aluOp); // Should never be reached
		}

		// Small function for checking what the sticky should be
		static int sticky(int val, int shift) {
			int sticky = 0;
			for (int i=0; i < shift; i++) {
				if (sticky != 1) {
					sticky = (val >>> i) & 1;
				}
			}
			return sticky;
		}
		
		int addition() {
			// Quick exiting for any special cases
			// Adding with NaN
			if (a == 0xFFFFFFFF || b == 0xFFFFFFFF || a == 0x7FFFFFFF || b == 0x7FFFFFFF) {
				return 0xFFFFFFFF;
			}

			// Adding with (+/-)infinity
			if (a == 0x7f800000 || b == 0x7f800000 || a == 0xff800000 || b == 0xff800000) {
				return 0xFFFFFFFF;
			}
			
			// Adding with zero
			if (a == 0 || a == 0x80000000) {
				return b;
			}
			if (b == 0 || b == 0x80000000) {
				return a;
			}
			
			// Adding with the negation of the other value
			if (a == (b ^ 0x80000000)) {
				return 0;
			}
			
			int sumSign, sumExp, sumMant;
			int signA = a & 0x80000000;
			int signB = b & 0x80000000;
			int expA = a & 0x7F800000;
			int expB = b & 0x7F800000;
			int mantissaA = (a & 0x007FFFFF) | 0x00800000; // Add the implicit one to both mantissa
			int mantissaB = (b & 0x007FFFFF) | 0x00800000;
			int grBits = 0; // Guard and round bits
			int stickyBit = 0; // Sticky bit is separate for ease of use
			boolean agrsBits = false; // If the grs bits belong to the 'a' mantissa
			
			// Rewrite smaller number to have the same exponent as the larger one
			if (expA > expB) {
				int difference = (expA >>> 23) - (expB >>> 23);
				grBits = (mantissaB >>> difference-3);
				stickyBit = sticky(mantissaB, difference);
				mantissaB >>>= difference;
				sumExp = expA;
			} else { // Also catches the case of them being equal, but it doesn't matter because the difference would be zero
				int difference = (expB >>> 23) - (expA >>> 23);
				grBits = (mantissaA >>> difference-3);
				stickyBit = sticky(mantissaA, difference);
				mantissaA >>>= difference;
				sumExp = expB;
				agrsBits = true;
			}
			
			if (signA == signB) { // Both are either negative or positive
				sumMant = mantissaA + mantissaB;
				sumSign = signA;
				if ((grBits | stickyBit) > 4) { // Regardless of where they belong, the grs bits are added if they round
					sumMant += 1;
				}
			} else { // Both are different
				if (signA < signB) { // A is negative, B isn't
					sumMant = -mantissaA + mantissaB;
					if (agrsBits && (grBits | stickyBit) >= 4) { // If the grs bits belong to the negative mantissa and they round up, then we need to subtract one as well
						sumMant -= 1;
						grBits = 0;
					} else if ((grBits | stickyBit) > 4) { // If they belong to the positive mantissa we need to add
						sumMant += 1;
						grBits = 0;
					}
					
				} else {             // B is negative, A isn't
					sumMant = mantissaA - mantissaB;
					if (!agrsBits && (grBits | stickyBit) >= 4) { // Same as above, just reversed
						sumMant -= 1;
						grBits = 0;
					} else if ((grBits | stickyBit) > 4) {
						sumMant += 1;
						grBits = 0;
					}
				}
				
				if (sumMant < 0) {
					// If the result of adding the mantissas together is negative, then the
					// sign bit needs to be set
					sumSign = 0x80000000;
					sumMant =  Math.abs(sumMant);
				} else { // If it's still positive, then the sign bit is zero
					sumSign = 0x00000000;
				}
			}
			
			// Check if sum is normal, if not, then normalize it
			if (sumMant >>> 23 == 0) { // Leading zero, need to left shift
				for (int i=22; i>0; i--) { // Find how much the mantissa needs to be shifted to normalize it
					if (sumMant >>> i == 1) {
						sumMant = (sumMant << (23-i)) & 0x007FFFFF; // Normalize mantissa and remove implicit 1
						grBits = (grBits << (23-i)) & 6;
						sumExp -= (23-i) << 23; // Add the correct change to the exponent
						break;
					}
				}
			} else if (sumMant >>> 23 > 1) {
				for (int i=1; i<9; i++) { // Find how much the mantissa needs to be shifted to normalize it
					if (sumMant >>> (23+i) == 1) {
						stickyBit |= sticky(sumMant, (23-i)) | sticky(grBits, (23-i)); // Check if sticky bit ends up enabled from the shift
						sumMant = (sumMant >>> i) & 0x007FFFFF; // Normalize mantissa and remove implicit 1
						grBits = (grBits >>> (23-i - 3)); // Shift the rounding bits by the same amount
						sumExp += i << 23; // Add the correct change to the exponent
						break;
					}
				}
			}
			// Check for too large or too small numbers
			if ((sumExp >>> 23)-127 < -126 || (sumExp >>> 23)-127 > 127) {
				return 0xFFFFFFFF; // Return NaN
			}

			int sum = (sumExp & 0x7FE00000) | (sumMant & 0x007FFFFF); // Integer addition will propagate through this properly if needed
			
			// For the midpoint, round "down" if negative, truncate if positive
			if ((grBits | stickyBit) > 4 || ((grBits | stickyBit) == 4 && sumSign != 0)) {
				sum += 1;
			}
			// Nothing needs to be done for truncation

			if ((sum & 0x80000000) != 0) { // Value overflowed during rounding
				return 0xFFFFFFFF;
			}
			
			return (sumSign & 0x80000000) |  sum; // Add in the sign to the existing sum
		}
		
		int subtraction() {
			this.b = this.b ^ 0x80000000; // quick negation to reuse the addition function
			return addition();
		}
		
		int negate() {
			return this.a ^ 0x80000000;
		}
		
		int multiplication() {
			// Multiplying with NaN
			if (a == 0xFFFFFFFF || b == 0xFFFFFFFF || a == 0x7FFFFFFF || b == 0x7FFFFFFF)
				return 0xFFFFFFFF;
			
			// Multiplying with (+/-)infinity
			if (a == 0x7F800000 || b == 0x7F800000)
				return 0x7F800000;
			else if (a == 0xFF800000 || b == 0xFF800000)
				return 0xFF800000;
			
			int signProd, expProd = 0;
			long mantissaProd = 0;
			int signA = a & 0x80000000;
			int signB = b & 0x80000000;
			int expA = a & 0x7F800000;
			int expB = b & 0x7F800000;
			long mantissaA = (a & 0x007FFFFF) | 0x00800000;
			long mantissaB = (b & 0x007FFFFF) | 0x00800000;
			float[] fractionsA = new float[23];
			float[] fractionsB = new float[23];
			
			signProd = signA ^ signB;
			
			expProd = expA + expB - 0x3F800000;
			
			for (int i = 0; i < (int) ((23 - Math.log(Float.intBitsToFloat(expA)) / Math.log(2))); i++){
				fractionsA[22] = fractionsA[21];
				fractionsA[21] = fractionsA[20];
				fractionsA[20] = fractionsA[19];
				fractionsA[19] = fractionsA[18];
				fractionsA[18] = fractionsA[17];
				fractionsA[17] = fractionsA[16];
				fractionsA[16] = fractionsA[15];
				fractionsA[15] = fractionsA[14];
				fractionsA[14] = fractionsA[13];
				fractionsA[13] = fractionsA[12];
				fractionsA[12] = fractionsA[11];
				fractionsA[11] = fractionsA[10];
				fractionsA[10] = fractionsA[9];
				fractionsA[9] = fractionsA[8];
				fractionsA[8] = fractionsA[7];
				fractionsA[7] = fractionsA[6];
				fractionsA[6] = fractionsA[5];
				fractionsA[5] = fractionsA[4];
				fractionsA[4] = fractionsA[3];
				fractionsA[3] = fractionsA[2];
				fractionsA[2] = fractionsA[1];
				fractionsA[1] = fractionsA[0];
				fractionsA[0] = mantissaA & 0x00000001;
				
				mantissaA >>= 1;
			}
			
			for (int i = 0; i < (int) ((23 - Math.log(Float.intBitsToFloat(expB)) / Math.log(2))); i++){
				fractionsB[22] = fractionsB[21];
				fractionsB[21] = fractionsB[20];
				fractionsB[20] = fractionsB[19];
				fractionsB[19] = fractionsB[18];
				fractionsB[18] = fractionsB[17];
				fractionsB[17] = fractionsB[16];
				fractionsB[16] = fractionsB[15];
				fractionsB[15] = fractionsB[14];
				fractionsB[14] = fractionsB[13];
				fractionsB[13] = fractionsB[12];
				fractionsB[12] = fractionsB[11];
				fractionsB[11] = fractionsB[10];
				fractionsB[10] = fractionsB[9];
				fractionsB[9] = fractionsB[8];
				fractionsB[8] = fractionsB[7];
				fractionsB[7] = fractionsB[6];
				fractionsB[6] = fractionsB[5];
				fractionsB[5] = fractionsB[4];
				fractionsB[4] = fractionsB[3];
				fractionsB[3] = fractionsB[2];
				fractionsB[2] = fractionsB[1];
				fractionsB[1] = fractionsB[0];
				fractionsB[0] = mantissaB & 0x00000001;
				
				mantissaB >>= 1;
			}
			
			float numA = mantissaA;
			float numB = mantissaB;
			
			for (int i = 0; i < 23; i++){
				fractionsA[i] *= Math.pow(2, -(i + 1));
				numA += fractionsA[i];
				fractionsB[i] *= Math.pow(2, -(i + 1));
				numB += fractionsB[i];
			}
			
			float integerProd = numA * numB;
			
			if (integerProd >= Float.intBitsToFloat(expProd) * 2){
				expProd >>= 23;
				expProd++;
				expProd <<= 23;
			}
			
			System.out.println();
			
			mantissaProd = Float.floatToIntBits(integerProd);
			
			mantissaProd &= 0x007FFFFF;
			
			return signProd | expProd | (int) mantissaProd;
		}
		
		int division() {
			return Float.floatToIntBits(Float.intBitsToFloat(a)/Float.intBitsToFloat(b));
		}
		
		int floor() {
			int exp = a & 0x7F800000;
			
			if (Float.intBitsToFloat(exp) < 1 && Float.intBitsToFloat(exp) > 0 && Float.intBitsToFloat(a) < 0)
				return 0xbF800000;
			else if (Float.intBitsToFloat(exp) < 1 && Float.intBitsToFloat(exp) > 0 && Float.intBitsToFloat(a) >= 0)
				return 0x00000000;
			
			int power = a & 0xFF800000;
			int mantissa = a & 0x007FFFFF;
			
			mantissa >>>= (int) (23 - Math.log(Float.intBitsToFloat(exp)) / Math.log(2));
			if (Float.intBitsToFloat(a) < 0){
				if (mantissa == 0x007FFFFF){
					power >>= 23;
					power++;
					power <<= 23;
					mantissa = 0;
				}
				else
					mantissa += 0x00000001;
			}
			mantissa <<= (int) (23 - Math.log(Float.intBitsToFloat(exp)) / Math.log(2));
			
			return power | mantissa;
		}
		
		int ceiling() {
			int exp = a & 0x7F800000;
			
			if (Float.intBitsToFloat(exp) < 1 && Float.intBitsToFloat(exp) > 0 && Float.intBitsToFloat(a) < 0)
				return 0x00000000;
			else if (Float.intBitsToFloat(exp) < 1 && Float.intBitsToFloat(exp) > 0 && Float.intBitsToFloat(a) >= 0)
				return 0x3F800000;
			
			int power = a & 0xFF800000;
			int mantissa = a & 0x007FFFFF;
			
			mantissa >>>= (int) (23 - Math.log(Float.intBitsToFloat(exp)) / Math.log(2));
			if (Float.intBitsToFloat(a) > 0){
				if (mantissa == 0x007FFFFF){
					power >>= 23;
					power++;
					power <<= 23;
					mantissa = 0;
				}
				else
					mantissa += 0x00000001;
			}
			mantissa <<= (int) (23 - Math.log(Float.intBitsToFloat(exp)) / Math.log(2));
			
			return power | mantissa;
		}
		
		int round() {
			int exp = a & 0x7F800000;
			int g = 0, r = 0, s = 0;
			
			if (Float.intBitsToFloat(exp) < 1 && Float.intBitsToFloat(exp) > 0 && Float.intBitsToFloat(a) < 0)
				return 0x00000000;
			else if (Float.intBitsToFloat(exp) < 1 && Float.intBitsToFloat(exp) > 0 && Float.intBitsToFloat(a) >= 0)
				return 0x3F800000;
			int mantissa = a & 0x007FFFFF;
			
			for (int i = 0; i < (int) ((23 - Math.log(Float.intBitsToFloat(exp)) / Math.log(2))); i++){
				if (s != 1)
					s = r;
				r = g;
				g = mantissa & 0x00000001;
				
				mantissa >>= 1;
			}
			
			int sumBits = g * 100 + s * 10 + r;
			
			if ((a & 0x80000000) == 0){
				if (sumBits > 100)
					return ceiling();
				else if (sumBits < 100)
					return floor();
				else{
					if ((mantissa & 0x00000001) == 1)
						return ceiling();
					else
						return floor();
				}
			}
			else{
				if (sumBits > 100)
					return floor();
				else if (sumBits < 100)
					return ceiling();
				else{
					if ((mantissa & 0x00000001) == 1)
						return floor();
					else
						return ceiling();
				}
			}
		}
		
		int absolute() {
			return a & 0x7FFFFFFF; // Mask out the sign bit to make it positive
		}
		
		int inverse() {
			return a ^ 0x80000000; // XOR the sign bit with one to negate
		}
		
		int minimum() {
			//Mask and isolate the sign bits of each number
			int signA = a & 0x80000000;
			int signB = b & 0x80000000;
			
			//If one of the numbers is positive and the other is negative, return the positive one; otherwise, move on
			if (signA < signB)
				return a;
			else if (signB < signA)
				return b;
			
			//Mask and isolate the exponent bits of each number
			int expA = a & 0x7F800000;
			int expB = b & 0x7F800000;
			
			//Return the number with the lower exponent; if they're the same, then move on
			if (expA < expB)
				return a;
			else if (expB < expA)
				return b;
			
			//Mask and isolate the mantissa bits of each number
			int mantissaA = a & 0x007FFFFF;
			int mantissaB = b & 0x007FFFFF;
			
			//Return the number with the lower mantissa
			if (mantissaA < mantissaB)
				return a;
			else
				return b;
		}
		
		int maximum() {
			//Mask and isolate the sign bits of each number
			int signA = a & 0x80000000;
			int signB = b & 0x80000000;
			
			//If one of the numbers is positive and the other is negative, return the positive one; otherwise, move on
			if (signA > signB)
				return a;
			else if (signB > signA)
				return b;
			
			//Mask and isolate the exponent bits of each number
			int expA = a & 0x7F800000;
			int expB = b & 0x7F800000;
			
			//Return the number with the higher exponent; if they're the same, then move on
			if (expA > expB)
				return a;
			else if (expB > expA)
				return b;
			
			//Mask and isolate the mantissa bits of each number
			int mantissaA = a & 0x007FFFFF;
			int mantissaB = b & 0x007FFFFF;
			
			//Return the number with the higher mantissa
			if (mantissaA > mantissaB)
				return a;
			else
				return b;
		}
		
		int power() {
			float result = Float.intBitsToFloat(a);
			for (int i = 1; i < b; i++) {
				result *= Float.intBitsToFloat(a);
			}
			return Float.floatToIntBits(result);
		}
		
		int sine() {
			float x = Float.intBitsToFloat(a);
			float result = 0;
			
			for (int i = 0; i < 12; i++) {
				result += pow(-1, i) * (pow(x, 2*i+1) / factorial(2*i + 1));
			}
			
			return Float.floatToIntBits(result);
		}
		
		int cosine() {
			float x = Float.intBitsToFloat(a);
			float result = 0;
			
			for (int i = 0; i < 12; i++) {
				result += pow(-1, i) * (pow(x, 2*i) / factorial(2*i));
			}
			
			return Float.floatToIntBits(result);
		}
		
		int tangent() {
			return Float.floatToIntBits(Float.intBitsToFloat(sine()) / Float.intBitsToFloat(cosine()));
		}
		
		int exponent() {
			float x = Float.intBitsToFloat(a);
			float result = 0;
			
			for (int i = 0; i < 12; i++) {
				result += pow(x, i) / factorial(i);
			}
			
			return Float.floatToIntBits(result);
		}
		
		int logarithm() {
			return Float.floatToIntBits((float) Math.log(Float.intBitsToFloat(a)));
		}
		
		int squareRoot() {
			return Float.floatToIntBits((float) Math.sqrt(Float.intBitsToFloat(a)));
		}
		
		// Various math functions
		int factorial(int num) {
			int result = 1;
			for(int i = 2; i <= num; i++) {
				result *= i;
			}
			return result;
		}
		
		float pow(float num, int power) {
			if (power < 0) {
				throw new RuntimeException("Custom power function does not support negative powers");
			}
			
			if (power == 0) {
				return 1;
			}
			
			float sum = num;
			for (int i = 1; i < power; i++) {
				sum *= num;
			}
			
			return sum;
		}
	}
}
