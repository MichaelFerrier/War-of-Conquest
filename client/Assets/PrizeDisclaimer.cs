using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class PrizeDisclaimer : MonoBehaviour
{
    public void OnClick_ShowContestRules()
	{
		GameGUI.instance.OpenContestRulesPanel();
	}
}
