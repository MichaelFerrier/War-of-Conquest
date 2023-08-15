using UnityEngine;
using System.Collections;

public class Sun_Scale : MonoBehaviour {
	Vector3 cur_scale ;
	Transform glow;
	Transform flares;
	Transform flares_big;
	private float saturation_old;
	private float contrast_old;
	private float brightness_old;
	private float edge_brightness_old;
	private float hue_old;
	private  float Epsilon = 1e-10f;
	// Use this for initialization
	void Start () {
		cur_scale = this.transform.localScale;
		glow = this.transform.Find("Glow");
		flares = this.transform.Find("Flares");
		flares_big = this.transform.Find("Flares_big");
		glow.GetComponent<ParticleSystem>().startSize = glow.GetComponent<ParticleSystem>().startSize*cur_scale.x;
		flares.GetComponent<ParticleSystem>().startSize = flares.GetComponent<ParticleSystem>().startSize*cur_scale.x;
		flares_big.GetComponent<ParticleSystem>().startSize = flares_big.GetComponent<ParticleSystem>().startSize*cur_scale.x;

	}
	





}
