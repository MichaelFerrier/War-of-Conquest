using UnityEngine;
using System.Collections;
using System;

// Based on:
// http://blogs.unity3d.com/2015/01/07/a-primer-on-repeatable-random-numbers/
// https://bitbucket.org/runevision/random-numbers-testing/src/113e3cdaf14ab86d3a03a5b2ed1d178549952bcd/Assets/Implementations/HashFunctions/XXHash.cs?at=default&fileviewer=file-view-default

public class XXHash
{
    private uint seed;
	
	const uint PRIME32_1 = 2654435761U;
	const uint PRIME32_2 = 2246822519U;
	const uint PRIME32_3 = 3266489917U;
	const uint PRIME32_4 = 668265263U;
	const uint PRIME32_5 = 374761393U;
	
	public XXHash (int seed) {
		this.seed = (uint)seed;
	}
	
	public uint GetHashUInt(int _val0, int _val1)
    {
		uint h32;
		int len = 2;
		
    	h32 = (uint)seed + PRIME32_5;
		h32 += (uint)len * 4;

        h32 += (uint)_val0 * PRIME32_3;
		h32 = RotateLeft (h32, 17) * PRIME32_4;

		
		h32 += (uint)_val1 * PRIME32_3;
		h32 = RotateLeft (h32, 17) * PRIME32_4;

		h32 ^= h32 >> 15;
		h32 *= PRIME32_2;
		h32 ^= h32 >> 13;
		h32 *= PRIME32_3;
		h32 ^= h32 >> 16;
		
		return h32;
	}

    public float GetHashFloat(int _val0, int _val1)
    {
		return (float)(GetHashUInt(_val0, _val1)) / (float)(UInt32.MaxValue);
	}
	
	private static uint RotateLeft (uint value, int count) {
		return (value << count) | (value >> (32 - count));
	}
}
