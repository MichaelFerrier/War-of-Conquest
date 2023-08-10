package WOCServer;

import WOCServer.*;

// Based on:
// http://blogs.unity3d.com/2015/01/07/a-primer-on-repeatable-random-numbers/
// https://bitbucket.org/runevision/random-numbers-testing/src/113e3cdaf14ab86d3a03a5b2ed1d178549952bcd/Assets/Implementations/HashFunctions/XXHash.cs?at=default&fileviewer=file-view-default

public class XXHash
{
  private long seed;
	
	final long PRIME32_1 = 2654435761L;
	final long PRIME32_2 = 2246822519L;
	final long PRIME32_3 = 3266489917L;
	final long PRIME32_4 = 668265263L;
	final long PRIME32_5 = 374761393L;
	
	public XXHash (int seed) {
		this.seed = (long)seed;
	}
	
	public long GetHashUInt(int _val0, int _val1)
  {
		long h32;
		long len = 2;
		
    h32 = (seed + PRIME32_5) & 0xFFFFFFFFL;
		h32 += len * 4;

    h32 += (long)_val0 * PRIME32_3; h32 = h32 & 0xFFFFFFFFL;
		h32 = RotateLeft(h32, 17) * PRIME32_4; h32 = h32 & 0xFFFFFFFFL;

		
		h32 += (long)_val1 * PRIME32_3; h32 = h32 & 0xFFFFFFFFL;
		h32 = RotateLeft(h32, 17) * PRIME32_4; h32 = h32 & 0xFFFFFFFFL;

		h32 ^= h32 >> 15;
		h32 *= PRIME32_2; h32 = h32 & 0xFFFFFFFFL;
		h32 ^= h32 >> 13;
		h32 *= PRIME32_3; h32 = h32 & 0xFFFFFFFFL;
		h32 ^= h32 >> 16;
		
		return h32;
	}

  public float GetHashFloat(int _val0, int _val1)
  {
		return (float)GetHashUInt(_val0, _val1) / (float)0xFFFFFFFFL;
	}
	
	private static long RotateLeft (long value, int count) {
		return ((value << count) | (value >> (32 - count))) & 0xFFFFFFFFL;
	}
}