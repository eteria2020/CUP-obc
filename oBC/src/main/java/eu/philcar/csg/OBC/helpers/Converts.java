package eu.philcar.csg.OBC.helpers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class Converts {
	public static final char[] BToA;
	private static final char[] hexDigit;

	static {
		BToA = "0123456789abcdef".toCharArray();
		hexDigit = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
	}

	public static byte[] intToByteArray(int i) {
		return new byte[]{(byte) ((i >> 24) & 255), (byte) ((i >> 16) & 255), (byte) ((i >> 8) & 255), (byte) (i & 255)};
	}

	public static byte[] hexStringToByte(String hex) {
		int len = hex.length() / 2;
		byte[] result = new byte[len];
		char[] achar = hex.toCharArray();
		for (int i = 0; i < len; i++) {
			int pos = i * 2;
			result[i] = (byte) ((toByte(achar[pos]) << 4) | toByte(achar[pos + 1]));
		}
		return result;
	}

	public static final String bytesToHexString(byte[] bArray) {
		StringBuffer sb = new StringBuffer(bArray.length);
		for (byte b : bArray) {
			String sTemp = Integer.toHexString(b & 255);
			if (sTemp.length() < 2) {
				sb.append(0);
			}
			sb.append(sTemp.toUpperCase());
		}
		return sb.toString();
	}

	public static final Object bytesToObject(byte[] bytes) throws IOException, ClassNotFoundException {
		ObjectInputStream oi = new ObjectInputStream(new ByteArrayInputStream(bytes));
		Object o = oi.readObject();
		oi.close();
		return o;
	}

	public static final byte[] objectToBytes(Serializable s) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ObjectOutputStream ot = new ObjectOutputStream(out);
		ot.writeObject(s);
		ot.flush();
		ot.close();
		return out.toByteArray();
	}

	public static final String objectToHexString(Serializable s) throws IOException {
		return bytesToHexString(objectToBytes(s));
	}

	public static final Object hexStringToObject(String hex) throws IOException, ClassNotFoundException {
		return bytesToObject(hexStringToByte(hex));
	}

	public static String bcd2Str(byte[] bytes) {
		StringBuffer temp = new StringBuffer(bytes.length * 2);
		for (int i = 0; i < bytes.length; i++) {
			temp.append((byte) ((bytes[i] & 240) >>> 4));
			temp.append((byte) (bytes[i] & 15));
		}
		return temp.toString().substring(0, 1).equalsIgnoreCase("0") ? temp.toString().substring(1) : temp.toString();
	}

	public static byte[] str2Bcd(String asc) {
		int len = asc.length();
		if (len % 2 != 0) {
			asc = "0" + asc;
			len = asc.length();
		}
		byte[] abt = new byte[len];
		if (len >= 2) {
			len /= 2;
		}
		byte[] bbt = new byte[len];
		abt = asc.getBytes();
		int p = 0;
		while (p < asc.length() / 2) {
			int j;
			int k;
			if (abt[p * 2] >= (byte) 48 && abt[p * 2] <= (byte) 57) {
				j = abt[p * 2] - 48;
			} else if (abt[p * 2] < (byte) 97 || abt[p * 2] > (byte) 122) {
				j = (abt[p * 2] - 65) + 10;
			} else {
				j = (abt[p * 2] - 97) + 10;
			}
			if (abt[(p * 2) + 1] >= (byte) 48 && abt[(p * 2) + 1] <= (byte) 57) {
				k = abt[(p * 2) + 1] - 48;
			} else if (abt[(p * 2) + 1] < (byte) 97 || abt[(p * 2) + 1] > (byte) 122) {
				k = (abt[(p * 2) + 1] - 65) + 10;
			} else {
				k = (abt[(p * 2) + 1] - 97) + 10;
			}
			bbt[p] = (byte) ((j << 4) + k);
			p++;
		}
		return bbt;
	}

	public static String BCD2ASC(byte[] bytes) {
		StringBuffer temp = new StringBuffer(bytes.length * 2);
		for (int i = 0; i < bytes.length; i++) {
			temp.append(BToA[(bytes[i] & 240) >>> 4]).append(BToA[bytes[i] & 15]);
		}
		return temp.toString();
	}

	public static String MD5EncodeToHex(String origin) {
		return bytesToHexString(MD5Encode(origin));
	}

	public static byte[] MD5Encode(String origin) {
		return MD5Encode(origin.getBytes());
	}

	public static byte[] MD5Encode(byte[] bytes) {
		try {
			return MessageDigest.getInstance("MD5").digest(bytes);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return new byte[0];
		}
	}

	public static int byteToInt2(byte[] b) {
		int n = 0;
		for (int i = 0; i < 4; i++) {
			n = (n << 8) | (b[i] & 255);
		}
		return n;
	}

	public static String convertToUtf8Hex(String inStr) {
		String str = "";
		try {
			str = bytesToHexString(inStr.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return str;
	}

	public static int hexStrToInt(String hexStr) {
		hexStr = hexStr.toLowerCase().replace("0x", "");
		int result = 0;
		int length = hexStr.length();
		if (length < 1) {
			return 0;
		}
		for (int i = 0; i < length; i++) {
			char c = hexStr.charAt(i);
			int is = c;
			if ('`' < c && c < 'g') {
				is -= 87;
			} else if ('/' < c && c < ':') {
				is -= 48;
			}
			result += power(16, (length - i) - 1) * is;
		}
		return result;
	}

	public static int power(int inputNum, int powerNum) {
		if (powerNum < 0) {
			return 0;
		}
		if (powerNum == 0) {
			return 1;
		}
		return powerNum != 1 ? inputNum * power(inputNum, powerNum - 1) : inputNum;
	}

	public static String toUnicode(String theString, boolean escapeSpace) {
		int len = theString.length();
		int bufLen = len * 2;
		if (bufLen < 0) {
			bufLen = Integer.MAX_VALUE;
		}
		StringBuffer outBuffer = new StringBuffer(bufLen);
		for (int x = 0; x < len; x++) {
			char aChar = theString.charAt(x);
			if (aChar <= '=' || aChar >= '\u007f') {
				switch (aChar) {
					case '\t':
						outBuffer.append('\\');
						outBuffer.append('t');
						break;
					case '\n':
						outBuffer.append('\\');
						outBuffer.append('n');
						break;
					case '\f':
						outBuffer.append('\\');
						outBuffer.append('f');
						break;
					case '\r':
						outBuffer.append('\\');
						outBuffer.append('r');
						break;
					case ' ':
						if (x == 0 || escapeSpace) {
							outBuffer.append('\\');
						}
						outBuffer.append(' ');
						break;
					case '!':
					case '#':
					case ':':
					case '=':
						outBuffer.append('\\');
						outBuffer.append(aChar);
						break;
					default:
						if (aChar >= ' ' && aChar <= '~') {
							outBuffer.append(aChar);
							break;
						}
						outBuffer.append('\\');
						outBuffer.append('u');
						outBuffer.append(toHex((aChar >> 12) & 15));
						outBuffer.append(toHex((aChar >> 8) & 15));
						outBuffer.append(toHex((aChar >> 4) & 15));
						outBuffer.append(toHex(aChar & 15));
						break;
				}
			} else if (aChar == '\\') {
				outBuffer.append('\\');
				outBuffer.append('\\');
			} else {
				outBuffer.append(aChar);
			}
		}
		return outBuffer.toString();
	}

	public static char toHex(int nibble) {
		return hexDigit[nibble & 15];
	}

	public static String fromUnicode(char[] in, int off, int len, char[] convtBuf) {
		if (convtBuf.length < len) {
			int newLen = len * 2;
			if (newLen < 0) {
				newLen = Integer.MAX_VALUE;
			}
			convtBuf = new char[newLen];
		}
		char[] out = convtBuf;
		int end = off + len;
		int outLen = 0;
		int off2 = off;
		while (off2 < end) {
			off = off2 + 1;
			char aChar = in[off2];
			int outLen2;
			if (aChar == '\\') {
				off2 = off + 1;
				aChar = in[off];
				if (aChar == 'u') {
					int value = 0;
					int i = 0;
					while (i < 4) {
						off = off2 + 1;
						aChar = in[off2];
						switch (aChar) {
							case '0':
							case '1':
							case '2':
							case '3':
							case '4':
							case '5':
							case '6':
							case '7':
							case '8':
							case '9':
								value = ((value << 4) + aChar) - 48;
								break;
							case 'A':
							case 'B':
							case 'C':
							case 'D':
							case 'E':
							case 'F':
								value = (((value << 4) + 10) + aChar) - 65;
								break;
							case 'a':
							case 'b':
							case 'c':
							case 'd':
							case 'e':
							case 'f':
								value = (((value << 4) + 10) + aChar) - 97;
								break;
							default:
								throw new IllegalArgumentException("Malformed \\uxxxx encoding.");
						}
						i++;
						off2 = off;
					}
					outLen2 = outLen + 1;
					out[outLen] = (char) value;
					outLen = outLen2;
				} else {
					if (aChar == 't') {
						aChar = '\t';
					} else if (aChar == 'r') {
						aChar = '\r';
					} else if (aChar == 'n') {
						aChar = '\n';
					} else if (aChar == 'f') {
						aChar = '\f';
					}
					outLen2 = outLen + 1;
					out[outLen] = aChar;
					outLen = outLen2;
				}
			} else {
				outLen2 = outLen + 1;
				out[outLen] = aChar;
				outLen = outLen2;
				off2 = off;
			}
		}
		return new String(out, 0, outLen);
	}

	private Converts() {
	}

	private static byte toByte(char c) {
		return (byte) "0123456789ABCDEF".indexOf(c);
	}
}