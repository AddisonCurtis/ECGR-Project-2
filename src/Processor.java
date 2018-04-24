import java.util.ArrayList;
import java.util.List;

public class Processor {
    
    private int[] instructions;
    
    ControlUnit control;
    RegisterFile registerFile;
    ArithmeticLogicUnit alu;
    
    
    public Processor(int[] instructions) {
        this.instructions = instructions;
        this.control = new ControlUnit();
        this.registerFile = new RegisterFile();
        this.alu = new ArithmeticLogicUnit();
    }
    
    // Will run until it exhausts all instructions. Returns the result of all Get statements or the contents of all the registers if there weren't any
    public int[] start() {
        List<Integer> outputs = new ArrayList<>();
        
        for (int instr : instructions) {
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
            if (control.aluSrc()==0) {
                b = registerFile.outputRD2();
            }
            else {
                b = range(instr, 18, 0);
            }
            alu.inputB(b);
            
            int wd;
            if (control.regWriteSrc() == 0) {
                wd = alu.process();
            } else {
                wd = Float.floatToIntBits(customToSinglePrecision(range(instr, 22, 0)));
            }
            
            registerFile.process(); // Update the register file for the new value
        }
        
        return outputs.stream().mapToInt(i -> i).toArray(); // Dealing with type safety gone wrong
    }
    
    // Mask a range of bits and return them shifted to the right
    // Range is inclusive on both ends
    // Left is more significant, right is less (31-0)
    public int range(int bits, int left, int right) {
        if (right > left) {
            throw new RuntimeException("Bad range parameters: " + left + ", " + right);
        }
        
        int mask = 0xFFFFFFFF;
        mask >>= 31 - (left - right);
        return bits & mask;
    }
    
    // Quick function to make it a bit more clear whats being done
    static int mux(int control, int[] inputs) {
        return inputs[control];
    }
    
    // Expects a 23 bit custom precision number as stated in the first milestone document
    static float customToSinglePrecision(int bits) {
        int sign = (bits & 0x200000) << 9;
        int exp = (((bits >>> 17) + 112) << 23) & 0x7F800000;
        int manti = (bits & 0x1FFFF) << 7;
        
        return Float.intBitsToFloat(sign | exp | manti);
    }
    
    
    public static class ControlUnit {
        
        int opcode = 0;
        
        public ControlUnit() {
            
        }
        
        public void input(int opcode) {
            this.opcode = opcode;
        }
        
        public int regWrite() {
            return 1;
        }
        
        public int regWriteSrc() {
            return opcode == 0 ? 1 : 0;
        }
        
        public int aluSrc() {
            if (opcode == 0) {
                return 1;
            }
            
            if (opcode == 15) {
                return 2;
            }
            
            return 0;
        }
        
        public int aluOp() {
            return opcode == 1?0:opcode+2;
        }
    }
    
    public static class RegisterFile {
        int[] registers;
        
        int RR1, RR2;
        int WR, WD;
        int regWrite;
        
        public RegisterFile() {
            registers = new int[16];
        }
        
        public void inputRR1(int RR1) {
            this.RR1 = RR1;
        }
        
        public void inputRR2(int RR2) {
            this.RR2 = RR2;
        }
        
        public void inputWR(int WR) {
            this.WR = WR;
        }
        
        public void inputWD(int WD) {
            this.WD = WD;
        }
        
        public void inputRegWrite(int regWrite) {
            this.regWrite = regWrite;
        }
        
        public int outputRD1() {
            return registers[RR1];
        }
        
        public int outputRD2() {
            return registers[RR2];
        }
        
        public void process() {
            if (regWrite == 0) {
                return;
            }
            
            registers[WR] = WD;
        }
    }
    
    public static class ArithmeticLogicUnit {
        int aluOp;
        int a, b;
        
        public ArithmeticLogicUnit() {
            
        }
        
        public void inputAluOp(int aluOp) {
            this.aluOp = aluOp;
        }
        
        public void inputA(int a) {
            this.a = a;
        }
        
        public void inputB(int b) {
            this.b = b;
        }
        
        public int process() {
            switch (aluOp) {
                case 0: return a;
                case 1: return addition();
                case 2: return subtraction();
                case 3: return negate();
                case 4: return multiplication();
                case 5: return division();
                case 6: return floor();
                case 7: return ceiling();
                case 8: return round();
                case 9: return absolute();
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
        
        public int addition() {
            int sum, signSum, expSum, mantissaSum;
            int signA = a & 0x80000000;
            int signB = b & 0x80000000;
            int expA = a & 0x7F800000;
            int expB = b & 0x7F800000;
            int mantissaA = a & 0x007FFFFF;
            int mantissaB = b & 0x007FFFFF;
            
            if (expA > expB){
                mantissaB >>= expA >> 23 - expB >> 23;
                expB = expA;
            }
            else{
                mantissaA >>= expB >> 23 - expA >> 23;
                expA = expB;
            }
            
            expSum = expA;
            
            if (signA != signB)
                mantissaSum = mantissaA - mantissaB;
            else
                mantissaSum = mantissaA + mantissaB;
            
            if ((mantissaSum & 0x80000000) == 0x80000000)
                signSum = 0x80000000;
            else
                signSum = 0x00000000;
            
            int shiftCheck = mantissaSum >> 23;
            int shift = ((int) Math.log(shiftCheck) / (int) Math.log(2));
            mantissaSum >>= shift;
            expSum += shift;
            
            sum = signSum | expSum | mantissaSum;
            
            return sum;
        }
        
        public int subtraction() {
            this.b = this.b ^ 0x80000000; // quick negation to reuse the addition function
            return addition();
        }
        
        private int negate() {
            return Float.floatToIntBits(-Float.intBitsToFloat(a));
        }
        
        public int multiplication() {
            int sum, signSum, expSum, mantissaSum;
            int signA = a & 0x80000000;
            int signB = b & 0x80000000;
            int expA = a & 0x7F800000;
            int expB = b & 0x7F800000;
            int mantissaA = a & 0x007FFFFF;
            int mantissaB = b & 0x007FFFFF;
            
            signSum = signA ^ signB;
            
            if (signA == signB)
                expSum = expA + expB - 0x3F800000;
            else
                expSum = expA - expB - 0x3F800000;
            
            mantissaSum = mantissaA * mantissaB;
            
            int shiftCheck = mantissaSum >> 23;
            int shift = ((int) Math.log(shiftCheck) / (int) Math.log(2));
            mantissaSum >>= shift;
            expSum += shift;
            
            return 0; // TODO
        }
        
        public int division() {
            return Float.floatToIntBits(Float.intBitsToFloat(a)/Float.intBitsToFloat(b));
        }
        
        private int floor() {
            int exp = a & 0x7F800000;
            
            if (Float.intBitsToFloat(exp) < 1 && Float.intBitsToFloat(exp) > 0 && Float.intBitsToFloat(a) < 0)
                return 0xbF800000;
            else if (Float.intBitsToFloat(exp) < 1 && Float.intBitsToFloat(exp) > 0 && Float.intBitsToFloat(a) >= 0)
                return 0x00000000;
            
            int power = a & 0xFF800000;
            int mantissa = a & 0x007FFFFF;
            
            mantissa >>= (int) (23 - Math.log(Float.intBitsToFloat(exp)) / Math.log(2));
            if (Float.intBitsToFloat(a) < 0){
                mantissa++;
            }
            mantissa <<= (int) (23 - Math.log(Float.intBitsToFloat(exp)) / Math.log(2));
            
            return power | mantissa;
        }
        
        private int ceiling() {
            int exp = a & 0x7F800000;
            
            if (Float.intBitsToFloat(exp) < 1 && Float.intBitsToFloat(exp) > 0 && Float.intBitsToFloat(a) < 0)
                return 0x00000000;
            else if (Float.intBitsToFloat(exp) < 1 && Float.intBitsToFloat(exp) > 0 && Float.intBitsToFloat(a) >= 0)
                return 0x3F800000;
            
            
            int power = a & 0xFF800000;
            int mantissa = a & 0x007FFFFF;
            
            mantissa >>= (int) (23 - Math.log(Float.intBitsToFloat(exp)) / Math.log(2));
            if (Float.intBitsToFloat(a) > 0){
                mantissa++;
            }
            mantissa <<= (int) (23 - Math.log(Float.intBitsToFloat(exp)) / Math.log(2));
            
            return power | mantissa;
        }
        
        private int round() {
            int exp = a & 0x7F800000;
            
            if (Float.intBitsToFloat(exp) < 1 && Float.intBitsToFloat(exp) > 0 && Float.intBitsToFloat(a) < 0)
                return 0x00000000;
            else if (Float.intBitsToFloat(exp) < 1 && Float.intBitsToFloat(exp) > 0 && Float.intBitsToFloat(a) >= 0)
                return 0x3F800000;
            int mantissa = a & 0x007FFFFF;
            
            mantissa >>= (int) ((23 - Math.log(Float.intBitsToFloat(exp)) / Math.log(2)) - 1);
            
            if (((mantissa & 0x00000001) == 1 && (a & 0x80000000) == 0) || ((mantissa & 0x00000001) == 0 && (a & 0x80000000) == 0x80000000))
                return ceiling();
            else
                return floor();
        }
        
        private int absolute() {
            return a & 0x7FFFFFFF; // Mask out the sign bit to make it positive
        }
        
        private int inverse() {
            return a ^ 0x80000000; // XOR the sign bit with one to negate
        }
        
        public int minimum() {
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
        
        public int maximum() {
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
        
        public int power() {
            float result = 0;
            for (int i =0; i < b; i++) {
                result += Float.intBitsToFloat(a);
            }
            return Float.floatToIntBits(result);
        }
        
        private int sine() {
            float sum, t, rad;
            rad = Float.intBitsToFloat(a) * (3.14159F/180.0F);
            sum = t = 1;
            
            for (int i = 0; i < 100; i += 2) {
                t = t * (-1) * rad * rad / (i * (i + 1));
                sum += t;
            }
            
            return Float.floatToIntBits(sum);
        }
        
        private int cosine() {
            float sum, t, rad;
            sum = t = rad = Float.intBitsToFloat(a) * (3.14159F/180.0F);
            
            for (int i = 0; i < 100; i++) {
                t = (t * (-1) * rad * rad) / (2 * i * (2 * i + 1));
                sum += t;
            }
            
            return Float.floatToIntBits(sum);
        }
        
        private int tangent() {
            return sine() / cosine();
        }
        
        private int exponent() {
            return Float.floatToIntBits((float) Math.pow(Math.E, Float.intBitsToFloat(a)));
        }
        
        private int logarithm() {
            return Float.floatToIntBits((float) Math.log(Float.intBitsToFloat(a))); // TODO
        }
        
        private int squareRoot() {
            return Float.floatToIntBits((float) Math.sqrt(Float.intBitsToFloat(a)));
        }
        
        
    }
}
