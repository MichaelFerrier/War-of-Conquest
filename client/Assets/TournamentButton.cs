using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;

public class TournamentButton : MonoBehaviour
{
    public TMPro.TextMeshProUGUI trophiesText, rankText;
    public GameObject joinTextObject;
    int numTrophies = 0, rank = 0;

    public static TournamentButton instance;

    void Awake() {
		instance = this;
	}

    public void InfoEventReceived()
    {
        numTrophies = -1;
        rank = -1;
    }

    public void UpdateForGlobalStatus()
    {
        // Show the button only if appropriate.
        UpdateObjectVisibility();
    }

    public void OnSwitchedMap()
    {
        // Show the button only if appropriate.
        UpdateObjectVisibility();
    }

    public void UpdateForNationStatus()
    {
        int newNumTrophies = (int)(GameData.instance.tournamentTrophiesAvailable + GameData.instance.tournamentTrophiesBanked + 0.5f);

        // Display number of trophies
        if (newNumTrophies != numTrophies)
        {
            // If this isn't the first update of the number of trophies, and if the number has increased, show anim text of increase amount.
            if ((numTrophies != -1) && (newNumTrophies > numTrophies)) 
            {
                GameObject anim_text_object= MemManager.instance.GetAnimTextObject();
                AnimText anim_text = anim_text_object.GetComponent<AnimText>();
                anim_text.Activate(gameObject.transform.parent, gameObject.GetComponent<RectTransform>().anchorMin, gameObject.GetComponent<RectTransform>().anchorMax, gameObject.GetComponent<RectTransform>().anchoredPosition, "+" + (newNumTrophies - numTrophies));
            }

            // Update the trophies text.
            trophiesText.text = string.Format("{0:n0}", newNumTrophies) + "<sprite=23>";

            // Record new number of trophies.
            numTrophies = newNumTrophies;
        }

        // Disply rank
        if (GameData.instance.tournamentRank != rank)
        {
            rank = GameData.instance.tournamentRank;
            rankText.text = "#" + string.Format("{0:n0}", rank);            
        }

        // Show the button only if appropriate.
        UpdateObjectVisibility();
    }

    public void UpdateObjectVisibility()
    {
        if ((GameData.instance.mapMode == GameData.MapMode.MAINLAND) && ((GameData.instance.tournamentEnrollmentClosesTime > Time.unscaledTime) || (GameData.instance.nationTournamentStartDay == GameData.instance.globalTournamentStartDay)))
        {
            this.gameObject.SetActive(true);

            if (GameData.instance.nationTournamentStartDay == GameData.instance.globalTournamentStartDay)
            {
                // The nation is a contender. Show rank and number of trophies.
                rankText.gameObject.SetActive(true);
                trophiesText.gameObject.SetActive(true);
                joinTextObject.SetActive(false);
            }
            else if (GameData.instance.tournamentEnrollmentClosesTime > Time.unscaledTime)
            {
                // The nation is not a contender, but enrollment is open. Show the join text.
                rankText.gameObject.SetActive(false);
                trophiesText.gameObject.SetActive(false);
                joinTextObject.SetActive(true);
            }
        }
        else
        {
            // Hide the button.
            this.gameObject.SetActive(false);
        }
    }
}
