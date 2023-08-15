using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class HypnoSpin : MonoBehaviour
{
    public void Spin(float _duration)
    {
        StartCoroutine(Spin_Coroutine(_duration));
    }

    IEnumerator Spin_Coroutine(float _duration)
    {
        float start_time = Time.time;
        float half_duration = (_duration / 2f);
        float mid_time = Time.time + half_duration;
        float end_time = Time.time + _duration;
        float speed, max_speed = 800f;
        
        while (Time.time < end_time)
        {
            if (Time.time <= mid_time) {
                speed = (Time.time - start_time) / half_duration * max_speed;
            } else {
                speed = (end_time - Time.time) / half_duration * max_speed;
            }

            // Rotate this object's transform.
            transform.Rotate(Vector3.up, Time.deltaTime * speed);

            yield return 0;
        }
    }
}
