using UnityEngine;
using UnityEngine.UI;
using I2.Loc;
using System.Collections;

public class QuestEntry : MonoBehaviour
{
    public TMPro.TextMeshProUGUI nameText, descText, rewardsText, barText;
    public Image barFill;
    public Button collectButton;
    public GameObject barObject, rewardsObject, completeObject, collectObject, spacerObject;
    public Image medal1, medal2, medal3;
    public int completed, collected;

    private QuestData questData;
    private int ID, criteria_amount;

    public void Init(int _ID, QuestData _questData, int _cur_amount, int _completed, int _collected)
    {
        questData = _questData;
        ID = _ID;

        // Display name
        nameText.text = questData.name;

        // Display status
        SetStatus(_cur_amount, _completed, _collected);

        // Show the apropriate medal images
        medal1.gameObject.SetActive(questData.num_stages > 1);
        medal2.gameObject.SetActive(questData.num_stages >= 2);
        medal3.gameObject.SetActive(questData.num_stages >= 3);
        spacerObject.SetActive(questData.num_stages > 1);

		if (gameObject.activeInHierarchy)
		{
			// Have TextFitters update font sizes for new language.
			collectButton.gameObject.transform.GetChild(0).GetComponent<TextFitter>().Fit();
		}
    }

    public void SetStatus(int _cur_amount, int _completed, int _collected)
    {
        // Record status
        completed = _completed;
        collected = _collected;

        //Debug.Log("Quest " + questData.name + " _cur_amount: " + _cur_amount + ", _completed: " + _completed + ", _collected: " + _collected);

        // Determine current criteria amount
        criteria_amount = questData.criteria_amount[Mathf.Min(collected, questData.num_stages - 1)];

        // Display description for appropriate stage
        descText.text = questData.description.Replace("{[criteria_amount]}", string.Format("{0:n0}", criteria_amount));

        // Display reward text for the appropriate stage
        rewardsText.text = I2.Loc.LocalizationManager.GetTranslation("Generic Text/rewards_word") + ":   " + string.Format("{0:n0}", questData.reward_credits[Mathf.Min(collected, questData.num_stages - 1)]) + " <sprite=2>    " + string.Format("{0:n0}", questData.reward_xp[Mathf.Min(collected, questData.num_stages - 1)]) + " <color=#440044>" + LocalizationManager.GetTranslation("Generic Text/xp_word") + "</color>";

        rewardsObject.SetActive(collected < questData.num_stages);
        completeObject.SetActive(collected >= questData.num_stages);
        collectObject.SetActive(completed > collected);
        barObject.SetActive((criteria_amount > 1) && (completed == collected) && (collected < questData.num_stages));

        if (criteria_amount > 1)
        {
            int cur_amount = Mathf.Min(criteria_amount, _cur_amount);
            barText.text = string.Format("{0:n0}", cur_amount) + " / " + string.Format("{0:n0}", criteria_amount);
            barFill.fillAmount = (float)cur_amount / (float)criteria_amount;
        }

        // Display medals
        medal1.sprite = (collected < 1) ? QuestsPanel.instance.medalEmpty : QuestsPanel.instance.medalBronze;
        medal2.sprite = (collected < 2) ? QuestsPanel.instance.medalEmpty : QuestsPanel.instance.medalSilver;
        medal3.sprite = (collected < 3) ? QuestsPanel.instance.medalEmpty : QuestsPanel.instance.medalGold;
    }

	public int GetID()
    {
        return ID;
    }
}
