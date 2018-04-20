import java.util.Arrays;
public class Main {

	public static void main(String[] args) {
		System.out.println(toFloat(toBooleanArray(1.25F)));
		printBoolArray(toBooleanArray(toFloat(toBooleanArray(1.25F))));
		Processor proc = new Processor();
		printBoolArray(toBooleanArray(proc.singleToHalfPrecision(toIntBits(toBooleanArray(1.25F)))));
	}

	public static boolean[] toBooleanArray(int num) {
		boolean[] bits = new boolean[32];
		for (int i = 31; i >= 0; --i) {
			bits[31-i] = (num & (1 << i)) != 0;
		}
		return bits;
	}

	public static boolean[] toBooleanArray(float num) {
		return toBooleanArray(Float.floatToIntBits(num));
	}

	public static int toIntBits(boolean[] bitArray) {
		int bits = 0;
		for (int i = 0; i < bitArray.length; ++i) {
			bits = (bits << 1) + (bitArray[i]?1:0);
		}
		return bits;
	}


	public static float toFloat(boolean[] bits) {
		return Float.intBitsToFloat(toIntBits(bits));
	}

	public static void printBoolArray(boolean[] arr) {
		System.out.print("[");
		for(boolean temp : arr) {
			System.out.print(temp?"1":"0");
		}
		System.out.println("]");

	}

}
