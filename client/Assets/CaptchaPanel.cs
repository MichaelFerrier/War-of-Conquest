using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;

public class CaptchaPanel : MonoBehaviour
{
    public static CaptchaPanel instance;
    public Text captchaTextLeft, captchaTextRight;
    public InputField captchaEntry;
    private int correctVal;

    public CaptchaPanel()
    {
        instance = this;
    }

    public void Init()
    {
        int leftVal, rightVal;

        // Give panel random Y position, to make it more difficult to defeat with automation.
        gameObject.GetComponent<RectTransform>().anchoredPosition = new Vector3(0, UnityEngine.Random.Range(0, 160) - 80, 0f);
        gameObject.GetComponent<RectTransform>().localScale = new Vector3(1f, 1f, 1f);

        // Clear the entry text.
        captchaEntry.text = "";

        if (UnityEngine.Random.Range(0,2) == 0)
        {
            // Addition
            leftVal = UnityEngine.Random.Range(0, 12);
            correctVal = UnityEngine.Random.Range(0, 12);
            rightVal = leftVal + correctVal;

            captchaTextLeft.text = leftVal + " +";
            captchaTextRight.text = "= " + rightVal;
        }
        else
        {
            // Subtraction
            leftVal = UnityEngine.Random.Range(0, 25);
            correctVal = UnityEngine.Random.Range(0, leftVal);
            rightVal = leftVal - correctVal;

            captchaTextLeft.text = leftVal + " -";
            captchaTextRight.text = "= " + rightVal;
        }
    }

    public void OnClick_Done()
    {
        // Do nothing if no input is given.
        if (captchaEntry.text.Length == 0) {
            return;
        }

        int entryVal = -1;
        int.TryParse(captchaEntry.text, out entryVal);

        if (entryVal == correctVal) 
        {
            // Correct answer given. Close this panel.
            GameGUI.instance.DeactivateAllPanels();
        }
        else
        {
            // Incorrect answer given. Send message to server and close the game.
            Network.instance.SendCommand("action=captcha|event=failed");
            GameGUI.instance.ExitGame();
        }
    }
}
