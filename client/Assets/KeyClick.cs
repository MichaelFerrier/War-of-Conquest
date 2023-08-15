using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;

public class KeyClick : MonoBehaviour
{
    public KeyCode keyCode;

	// Update is called once per frame
	void Update ()
    {
        if (Input.GetKeyDown(keyCode)) 
        {
            Button button = GetComponent<Button>();

            if (button != null) 
            {
                Debug.Log("Invoking button click");
                button.onClick.Invoke();
            }
        }	
	}
}
