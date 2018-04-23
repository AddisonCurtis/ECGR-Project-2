public class Processor {
    
    private int[] instructions;
    
    ControlUnit control;
    
    RegisterFile registerFile;
    
    
    public Processor(int[] instructions) {
        this.instructions = instructions;
        this.control = new ControlUnit();
        this.registerFile = new RegisterFile();
    }
    
    // Expects a 23 bit custom precision number as stated in the first milestone document
    static float customToSinglePrecision(int bits) {
        int sign = (bits & 0x200000) << 9;
        int exp = (((bits >>> 17) + 112) << 23) & 0x7F800000;
        int manti = (bits & 0x1FFFF) << 7;
        
        return Float.intBitsToFloat(sign | exp | manti);
    }
    
    // Quick function to make it a bit more clear whats being done
    static int mux(int control, int[] inputs) {
        return inputs[control];
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
            return new int[]{1, 0, 1, 1, 1, 0, 1, 1, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0}[opcode];
        }
        
        public int convType() {
            return opcode == 0x0F ? 1 : 0;
        }
        
        public int aluSrc() {
            return opcode == 0x0F || opcode == 0x00 ? 1 : 0;
        }
        
        // Don't cares default to 7 (Passthrough)
        public int aluOp() {
            return new int[]{7, 7, 7, 0, 1, 7, 2, 3, 7, 7, 7, 7, 7, 4, 5, 6, 7, 7, 7, 7, 7, 7}[opcode];
        }
        
        // Don't cares default to 0 (Addition)
        public int fnsOp() {
            return new int[]{0, 0, 0, 0, 0, 0, 0, 0, 1, 2, 3, 4, 5, 0, 0, 0, 6, 7, 8, 9, 0xA, 0xB}[opcode];
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
    
    public static class FunctionUnit {
        int fnsOp;
        int a;
        
        public FunctionUnit() {
            
        }
        
        public void inputFnsOp(int fnsOp) {
            this.fnsOp = fnsOp;
        }
        
        public void inputA(int a) {
            this.a = a;
        }
        
        public int process() {
            switch (fnsOp) {
                case 0: return negate();
                case 1: return floor();
                case 2: return ceiling();
                case 3: return round();
                case 4: return absolute();
                case 5: return inverse();
                case 6: return sine();
                case 7: return cosine();
                case 8: return tangent();
                case 9: return exponent();
                case 10: return logarithm();
                case 11: return squareRoot();
            }
            throw new RuntimeException("Invalid fnsOp value: " + fnsOp); // Should never be reached
        }
        
        private int negate() {
            return Float.floatToIntBits(-Float.intBitsToFloat(a));
        }
        
        private int floor() {
            return Float.floatToIntBits((float) Math.floor(Float.intBitsToFloat(a)));
        }
        
        private int ceiling() {
            return Float.floatToIntBits((float) Math.ceil(Float.intBitsToFloat(a)));
        }
        
        private int round() {
            return 0; // TODO
        }
        
        private int absolute() {
            return a & 0x7FFFFFFF; // Mask out the sign bit to make it positive
        }
        
        private int inverse() {
            return a ^ 0x80000000; // XOR the sign bit with one to negate
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
            return 0; // TODO
        }
        
        private int exponent() {
            return 0; // TODO
        }
        
        private int logarithm() {
            return 0; // TODO
        }
        
        private int squareRoot() {
            return 0; // TODO
        }
    }
    
    public static class ArithmeticLogicUnit {
        int aluOp;
        int a, b;
        
        public ArithmeticLogicUnit() {
            
        }
        
        public void inputSluOp(int aluOp) {
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
                case 0: return addition();
                case 1: return subtraction();
                case 2: return multiplication();
                case 3: return division();
                case 4: return minimum();
                case 5: return maximum();
                case 6: return power();
                case 7: return a;
            }
            throw new RuntimeException("Invalid aluOps value: " + aluOp); // Should never be reached
        }
        
        public int addition() {
            return 0; // TODO
        }
        
        public int subtraction() {
            this.b = this.b ^ 0x80000000; // quick negation to reuse the addition function
            return addition();
        }
        
        public int multiplication() {
            return 0; // TODO
        }
        
        public int division() {
            return Float.floatToIntBits(Float.intBitsToFloat(a)/Float.intBitsToFloat(b));
        }
        
        public int minimum() {
            return 0; // TODO
        }
        
        public int maximum() {
            return 0; // TODO
        }
        
        public int power() {
            float result = 0;
            for (int i =0; i < b; i++) {
                result += Float.intBitsToFloat(a);
            }
            return Float.floatToIntBits(result);
        }
        
        
    }
}