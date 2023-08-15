using UnityEngine;
using System.Collections;

public class RandomProgress
{
    private const int PROGRESS_ARRAY_NUM_ELEMENTS = 15;
    private const float PROGRESS_ACCELERATION_FACTOR = 5.0f;

    private float[] progress_array = new float[PROGRESS_ARRAY_NUM_ELEMENTS];

	public void Init()
    {
        int i;
        float progress_total = 0;
        float progress_sum = 0;

        for (i = 0; i < PROGRESS_ARRAY_NUM_ELEMENTS; i++)
        {
            // Determine weight value for current place in array
            progress_array[i] = Random.value * (1.0f + ((i / (PROGRESS_ARRAY_NUM_ELEMENTS - 1)) * PROGRESS_ACCELERATION_FACTOR));
            progress_total += progress_array[i];
        }

        for (i = 0; i < PROGRESS_ARRAY_NUM_ELEMENTS; i++)
        {
            // Determine attacker state values for each place in array
            progress_sum += progress_array[i];
            progress_array[i] = progress_sum / progress_total;
        }
    }

    public float GetRandomProgress(float _progress)
    {
        float place = _progress * (PROGRESS_ARRAY_NUM_ELEMENTS - 1);
        int low_index = Mathf.FloorToInt(place);
        int high_index = Mathf.CeilToInt(place);
        float mix = place - (float)low_index;

        float low_val = progress_array[low_index];
        float high_val = progress_array[high_index];

        return low_val + ((high_val - low_val) * mix);
    }
}
