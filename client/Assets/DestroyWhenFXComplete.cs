using UnityEngine;
using System.Collections;

public class DestroyWhenFXComplete : MonoBehaviour
{
    private ParticleSystem psys = null;
    private float end_time = 0f;

	// Use this for initialization
	void Start ()
    {
	    psys = this.GetComponent<ParticleSystem>();
        if (psys != null) end_time = Time.time + psys.duration;
	}

    void Update ()
    {
        if ((psys != null) && (Time.time > end_time) && (psys.particleCount == 0)) Destroy(this.gameObject, psys.duration);
    }
}
