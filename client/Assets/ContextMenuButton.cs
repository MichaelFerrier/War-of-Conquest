using UnityEngine;
using UnityEngine.UI;
using System.Collections;

public class ContextMenuButton : MonoBehaviour
{
    private Color color_normal = new Color(1.0f, 1.0f, 1.0f);
    private Color color_pressed = new Color(1.0f, 1.0f, 0.5f);
    private Color color_disabled = new Color(0.6f, 0.6f, 0.6f);
    private bool pressed = false;
    private bool hover = false;
    Vector3 original_scale = new Vector3(-1,-1,-1);

    Button button;
    TMPro.TextMeshProUGUI text;
    GUITransition transition;

    const float PRESSED_SCALE = 1.1f;

    public void Init()
    {
        // First time it is initialized, record original scale.
        if (original_scale.x == -1) {
            original_scale = transform.localScale;
        }

        button = ((Button)(this.gameObject.GetComponent<Button>()));
        text = gameObject.transform.GetChild(0).gameObject.GetComponent<TMPro.TextMeshProUGUI>();
        transition = gameObject.transform.GetComponent<GUITransition>();

        text.color = button.enabled ? color_normal : color_disabled;

        pressed = false;
        transform.localScale = original_scale;
    }

    public void OnPointerDown()
    {
        if ((button != null) && button.enabled)
        {
            AppearPressed();
            pressed = true;
        }
    }

    public void OnPointerUp()
    {
        if (pressed)
        {
            if (hover) {
                AppearNormal();
            }

            pressed = false;
        }
    }

    public void OnPointerEnter()
    {
        hover = true;
        if (pressed) AppearPressed();
    }

    public void OnPointerExit()
    {
        hover = false;
        if (pressed) AppearNormal();
    }

    private void AppearNormal()
    {
        text.color = color_normal;
        if (transition != null) {
            transition.StartTransition(1f, 1f, PRESSED_SCALE, original_scale.x, false);
        } else {
            transform.localScale = original_scale;
        }
    }

    private void AppearPressed()
    {
        text.color = color_pressed;
        if (transition != null) {
            transition.StartTransition(1f, 1f, original_scale.x, PRESSED_SCALE, false);
        } else {
            transform.localScale = new Vector3(original_scale.x * PRESSED_SCALE, original_scale.y * PRESSED_SCALE, original_scale.z * PRESSED_SCALE);
        }
    }
}
