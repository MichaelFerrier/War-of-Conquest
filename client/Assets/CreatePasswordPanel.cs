using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using I2.Loc;

public class CreatePasswordPanel : MonoBehaviour, RequestorListener
{
    public enum Context
    {
        AccountCreation,
        OptionsPanel
    }

    public static CreatePasswordPanel instance;

    public InputField email_entry_inputfield, security_answer_entry_inputfield;
    public Text email_message_text, security_answer_message_text;
    public Text message_text;
    public Dropdown security_question_dropdown;

    Context cur_context;

    public CreatePasswordPanel()
    {
        instance = this;
    }

    public void Start()
    {
        // Initialize the security questions listbox
        security_question_dropdown.ClearOptions();
        security_question_dropdown.AddOptions(GameGUI.instance.securityQuestions);
    }


    public void Init(Context _context)
    {
        cur_context = _context;

        // Reset all text fields
        Reset();

        // Choose a random security question.
        security_question_dropdown.value = UnityEngine.Random.Range(0, GameGUI.instance.securityQuestions.Count);
    }

    public void Reset()
    {
        // Clear the text fields.
        message_text.text = LocalizationManager.GetTranslation("password_by_email");
        email_entry_inputfield.text = "";
        security_answer_entry_inputfield.text = "";
        email_message_text.text = "";
        security_answer_message_text.text = "";
    }

    public void OnClick_Done()
    {
        // Check all inputs

        // E-mail
        string email = email_entry_inputfield.text;

        // Remove any control characters from the string and display modified version.
        email = GameGUI.RemoveControlCharacters(email);
        email = email.Replace("|", "");
        email_entry_inputfield.text = email;

        if (email.Length == 0)
        {
            // "Please enter your e-mail address."
            email_message_text.text = LocalizationManager.GetTranslation("please_enter_email_address");
            return;
        }

        // Security answer
        string security_answer = security_answer_entry_inputfield.text;

        // Remove any control characters from the string and display modified version.
        security_answer = GameGUI.RemoveControlCharacters(security_answer);
        security_answer = security_answer.Replace("|", "");
        security_answer_entry_inputfield.text = security_answer;

        if (security_answer.Length == 0)
        {
            // "Please answer a security question."
            security_answer_message_text.text = LocalizationManager.GetTranslation("please_answer_security_question");
            return;
        }

        // Security question
        string securityQuestion = GameGUI.instance.securityQuestions[security_question_dropdown.value];
        Debug.Log("Security question index: " + security_question_dropdown.value + ", question: " + securityQuestion);

        // Send message to create player account
        Network.instance.SendCommand("action=create_password|email=" + email + "|question=" + securityQuestion + "|answer=" + security_answer);
    }

    public void OnClick_Close()
    {
        if (cur_context == Context.OptionsPanel)
        {
            GameGUI.instance.CloseAllPanels();
        }
        else
        {
            GameGUI.instance.OpenNewNationPanel();
        }
    }

    public void OnEdit_Email()
    {
        email_message_text.text = "";
    }

    public void OnEdit_SecurityAnswer()
    {
        security_answer_message_text.text = "";
    }

    public void CreatePasswordResult(bool _success, string _message)
    {
        if (_success)
        {
            // "Account created, you are now logged in as " + GameData.instance.username + "!"
            // "Account created, you are now logged in as {[_USER_NAME]}!"
            string msg = LocalizationManager.GetTranslation("password_emailed"); // "Success! Your account password has been e-mailed to " + email_entry_inputfield.text + "."
            msg = msg.Replace("{email}", email_entry_inputfield.text);

            // Display success message
            Requestor.Activate(0, 0, this, msg, LocalizationManager.GetTranslation("Generic Text/okay"), "");

            // Close the player account creation panel.
            GameGUI.instance.CloseAllPanels();
        }
        else
        {
            // Display the given message.
            message_text.text = _message;
        }
    }

    public void RequestorResponse(int _task, int _data, Requestor.RequestorButton _result)
    {
        if (cur_context == Context.AccountCreation)
        {
            // Continue the account creation process by opening the new nation panel.
            GameGUI.instance.OpenNewNationPanel();
        }
    }
}
