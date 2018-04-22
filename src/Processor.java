public class Processor {
	public Processor () {

	}

	public boolean[] halfToSinglePrecision(boolean[] halfBits) {
		return new boolean[2];
	}

	public boolean[] singleToHalfPrecision(boolean[] singleBits) {
		return new boolean[2];
	}
	// Expects a 23 bit custom precision number as stated in the first milestone document
	public static float customToSinglePrecision(int bits) {
		int sign = (bits & 0x200000) << 9;
		int exp = (((bits >>> 17) + 127) << 23) & 0x7F800000;
		int manti = (bits & 0x1FFFF) << 7;

		return Float.intBitsToFloat(sign | exp | manti);
	}

	public static class RegisterFile {
		

		public RegisterFile() {

		}
	}
}