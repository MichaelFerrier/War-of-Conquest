using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class SelectionSquare : MonoBehaviour
{
    enum Mode
    {
        enter,
        loop,
        exit
    }

    const float ENTER_DURATION = 0.5f;
    const float EXIT_DURATION = 0.25f;
    const float LOOP_DURATION = 0.5f;
    const float ENTER_Y = 30f;
    const float TOP_Y = 40f;
    const float BOTTOM_Y = 36f;

    Mode mode;
    float modeStartTime;
    float modeStartY;

    public void Show()
    {
        mode = Mode.enter;
        modeStartTime = Time.unscaledTime;
        UpdatePosition();

        gameObject.SetActive(true);
    }

    public void Hide()
    {
        modeStartY = this.gameObject.transform.position.y;
        modeStartTime = Time.unscaledTime;
        mode = Mode.exit;
    }
	
	// Update is called once per frame
	void Update ()
    {
        UpdatePosition();
	}

    void UpdatePosition()
    {
        float pos;

        if (mode == Mode.enter)
        {
            if ((Time.unscaledTime - modeStartTime) >= ENTER_DURATION)
            {
                mode = Mode.loop;
                modeStartTime += ENTER_DURATION;
            }
            else
            {
                pos = (Time.unscaledTime - modeStartTime) / ENTER_DURATION;
                pos *= Mathf.Sin(pos * (Mathf.PI / 2));
                pos = ENTER_Y + (pos * (TOP_Y - ENTER_Y));
                this.gameObject.transform.position = new Vector3(this.gameObject.transform.position.x, pos, this.gameObject.transform.position.z);
            }
        }

        if (mode == Mode.loop)
        {
            pos = (Mathf.Sin(Time.unscaledTime - modeStartTime + (Mathf.PI / 2)) + 1f) / 2f;
            pos = BOTTOM_Y + (pos * (TOP_Y - BOTTOM_Y));
            this.gameObject.transform.position = new Vector3(this.gameObject.transform.position.x, pos, this.gameObject.transform.position.z);
        }

        if (mode == Mode.exit)
        {
            if ((Time.unscaledTime - modeStartTime) >= EXIT_DURATION)
            {
                // Deactivate
                gameObject.SetActive(false);
            }
            else
            {
                pos = (Time.unscaledTime - modeStartTime) / EXIT_DURATION;
                pos *= Mathf.Sin(pos * (Mathf.PI / 2));
                pos = modeStartY + (pos * (ENTER_Y - modeStartY));
                this.gameObject.transform.position = new Vector3(this.gameObject.transform.position.x, pos, this.gameObject.transform.position.z);
            }
        }
    }
}
