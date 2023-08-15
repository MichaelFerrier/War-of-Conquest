using UnityEngine;
using System.Collections;

public class LookAtScript : MonoBehaviour
{
    public Camera camera_to_face;

	// Use this for initialization
	void Start () {
        camera_to_face = Camera.main;
	}
	
	// Update is called once per frame
	void Update () {
		this.transform.LookAt(camera_to_face.transform.position);
	}
}
