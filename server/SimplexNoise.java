package WOCServer;

// From: http://stackoverflow.com/questions/18279456/any-simplex-noise-tutorials-or-resources
// http://www.java-gaming.org/index.php?topic=31637.0

import java.util.Random;


public class SimplexNoise 
{
    SimplexNoise_octave[] octaves;
    double[] wavelengths;
    double[] amplitudes;

    public SimplexNoise(int largestFeature, double persistence, int seed)
		{
        //recieves a number (eg 128) and calculates what power of 2 it is (eg 2^7)
        int numberOfOctaves=(int)Math.ceil(Math.log10(largestFeature)/Math.log10(2));

        octaves=new SimplexNoise_octave[numberOfOctaves];
        wavelengths=new double[numberOfOctaves];
        amplitudes=new double[numberOfOctaves];

        Random rnd=new Random(seed);

        for(int i=0;i<numberOfOctaves;i++){
            octaves[i]=new SimplexNoise_octave(rnd.nextInt());

            wavelengths[i] = Math.pow(2,i);
            amplitudes[i] = Math.pow(persistence,octaves.length-i);
        }
    }

		public SimplexNoise(int smallestFeature, int largestFeature, double persistence, int seed)
		{
				smallestFeature = Math.max(smallestFeature, 2);
				largestFeature = Math.max(largestFeature, smallestFeature);

        //recieves a number (eg 128) and calculates what power of 2 it is (eg 2^7)
        int highestOctave =(int)Math.ceil(Math.log10(largestFeature)/Math.log10(2)) - 1;
				int lowestOctave =(int)Math.ceil(Math.log10(smallestFeature)/Math.log10(2)) - 1;
				int numberOfOctaves = highestOctave - lowestOctave + 1;

        octaves=new SimplexNoise_octave[numberOfOctaves];
        wavelengths=new double[numberOfOctaves];
        amplitudes=new double[numberOfOctaves];

        Random rnd=new Random(seed);

				// Use up seeds for unused lower octaves, so that changing the smallestFeature doesn't change the seeds for larger octaves.
				for(int i=0;i<lowestOctave;i++){
					rnd.nextInt();
				}

        for(int i=0;i<numberOfOctaves;i++){
            octaves[i]=new SimplexNoise_octave(rnd.nextInt());

            wavelengths[i] = Math.pow(2,lowestOctave + i);
            amplitudes[i] = Math.pow(persistence,octaves.length-i);
        }
    }

    public double getNoise(int x, int y)
		{
        double result=0;

        for(int i=0;i<octaves.length;i++){
					double wavelength = wavelengths[i];
					result=result+octaves[i].noise(x/wavelength, y/wavelength)* amplitudes[i];
        }

        return result;
    }

    public double getNoise(int x,int y, int z)
		{
        double result=0;

        for(int i=0;i<octaves.length;i++){
          double wavelength = wavelengths[i];
          result=result+octaves[i].noise(x/wavelength, y/wavelength,z/wavelength)* amplitudes[i];
        }

        return result;
    }
} 
