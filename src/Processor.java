public class Processor {
	public Processor () {

	}

	public boolean[] halfToSinglePrecision(boolean[] halfBits) {
		return new boolean[2];
	}

	public boolean[] singleToHalfPrecision(boolean[] singleBits) {
		return new boolean[2];
	}

	public static int singleToHalfPrecision(int singleBits) {
		int halfBits = (singleBits >> 16) & 0x8000;	// Move and mask sign bit
		int mant = (singleBits >> 12) & 0x07FF;		// Mantissa + extra bit to round
		int exp = (singleBits >> 23) & 0xFF;		// Exponent
	
		if (exp < 103) {
			return halfBits;
		}

		if (exp > 142) {
			halfBits = halfBits | 0x7c00;
			//halfBits |= exp == 255 && (singleBits & 0x007FFFFF);
			return halfBits;
		}

		if (exp < 113) {
			mant = mant | 0x0800;
			halfBits = halfBits | (mant >> (114 - exp)) + ((mant >>(113 - exp)) & 1);
			return halfBits;
		}

		halfBits = halfBits | ((exp - 112) << 10) | (mant >> 1);
		halfBits += mant & 1;
		return halfBits;
	}	

	public static class RegisterFile {
		

		public RegisterFile() {

		}
	}
}