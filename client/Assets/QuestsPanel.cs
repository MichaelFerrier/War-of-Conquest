using UnityEngine;
using UnityEngine.UI;
using System.Collections;
using System.Collections.Generic;

public class QuestsPanel : MonoBehaviour
{
    public static QuestsPanel instance;
    public GameObject contentArea;
    public Sprite medalGold, medalSilver, medalBronze, medalEmpty;

    private Dictionary<int, QuestEntry> questEntries = new Dictionary<int, QuestEntry>();

    public QuestsPanel()
    {
        instance = this;
    }

    public void OnEnable()
    {
        // If there are any quests that have been completed but not yet collected, scroll to make the first such quest visible.
        QuestEntry cur_entry;
        int index = 0;
        foreach(Transform t in contentArea.transform)
        {
            cur_entry = t.gameObject.GetComponent<QuestEntry>();
            if (cur_entry.completed > cur_entry.collected)
            {
                StartCoroutine(SetScrollPosition(1f - ((float)index / (float)(contentArea.transform.childCount - 1))));
                break;
            }
            index++;
        }
    }

    public IEnumerator SetScrollPosition(float _vpos)
    {
        yield return new WaitForEndOfFrame(); 
        contentArea.transform.parent.parent.gameObject.GetComponent<ScrollRect>().verticalNormalizedPosition = _vpos; 
    }
    
    public void InfoEventReceived()
    {
        // Remove any quests from the quests list.
        GameObject cur_entry_object;
        while (contentArea.transform.childCount > 0)
        {
            cur_entry_object = contentArea.transform.GetChild(0).gameObject;
            cur_entry_object.transform.SetParent(null);
			cur_entry_object.SetActive(false);
            MemManager.instance.ReleaseQuestEntryObject(cur_entry_object);
        }

        // Clear the questEntries dictionary
        questEntries.Clear();

        // Add quest entries to list.
        for (int curID = 0; curID <= QuestData.quests.Count; curID++)
        {
            // Get the data for the quest with the current ID
            QuestData questData = QuestData.GetQuestData(curID);

            // If there is no data for the quest with the current ID, skip it.
            if (questData == null) {
                continue;
            }
             
            // Get a new quest entry
            GameObject entryObject = MemManager.instance.GetQuestEntryObject();

            // Add the new entry to the list.
            entryObject.transform.SetParent(contentArea.transform);
            entryObject.transform.SetAsLastSibling();
            entryObject.transform.localScale = new Vector3(1, 1, 1); // Needs to be done each time it's activated, in case it was changed last time used.
			entryObject.SetActive(false); // Make sure it is inactive before activting it, so that onEnable() will be called (for TextFitter).
			entryObject.SetActive(true);

            // Get pointer to QuestEntry component.
            QuestEntry curEntry = entryObject.GetComponent<QuestEntry>();

            // Set up the ChatMessageObject button's listener.
            Button messageButton = entryObject.GetComponent<Button>();
            curEntry.collectButton.onClick.RemoveAllListeners();
            curEntry.collectButton.onClick.AddListener(() => CollectButtonPressed(curEntry));

            // Get the QuestRecord for this quest, if it exists.
            QuestRecord quest_record = GameData.instance.GetQuestRecord(curID, false);

            // Initialize the new entry
            curEntry.Init(curID, questData, (quest_record == null) ? 0 : quest_record.cur_amount, (quest_record == null) ? 0 : quest_record.completed, (quest_record == null) ? 0 : quest_record.collected);

            // Add the new entry to the questEntries dictionary.
            questEntries.Add(curID, curEntry);
        }

        // Set the Quest Panel's alert state as appropriate.
        UpdateAlertState();
    }

    public void UpdateForLocalization()
    {
        InfoEventReceived();
    }

    public void UpdateQuestStatus(QuestRecord _questRecord)
    {
        if (questEntries.ContainsKey(_questRecord.ID) == false)
        {
            Debug.Log("UpdateQuestStatus(): No quest entry for quest ID " + _questRecord.ID);
            return;
        }

        // Get the quest entry corresponding to the given quest ID.
        QuestEntry curEntry = questEntries[_questRecord.ID];

        int prevCompleted = curEntry.completed;
        int prevCollected = curEntry.collected;

        // Update the status of the quest entry.
        curEntry.SetStatus(_questRecord.cur_amount, _questRecord.completed, _questRecord.collected);

        // If the quest's status has changed...
        if ((prevCompleted != curEntry.completed) || (prevCollected != curEntry.collected))
        {
            if (_questRecord.collected == _questRecord.completed) 
            {
                // Play music for quest that has been collected.
                Sound.instance.QuestRewardCollected();

                // Set the state of the Quests Panel alert according to whether there are any completed quests awaiting collection.
                UpdateAlertState();
            }
            else if (_questRecord.completed > _questRecord.collected) 
            {
                // Play sound for quest that hs been completed.
                Sound.instance.Play2D(Sound.instance.quest_completed);

                // Turn on Quests Panel alert, to tell player that  completed quest is waiting to be collected for.
                GameGUI.instance.SetPanelAlertState(GameGUI.GamePanel.GAME_PANEL_QUESTS, true);

                // Get the quest's data
                QuestData quest_data = QuestData.GetQuestData(_questRecord.ID);

                // Display message stating that the quest has been completed.
                GameGUI.instance.DisplayMessage(quest_data.name + " " + I2.Loc.LocalizationManager.GetTranslation("Quests Panel/quest_completed_lower") + "!");
            }
        }
    }

    // Set the state of the Quests Panel alert according to whether there are any completed quests awaiting collection.
    public void UpdateAlertState()
    {
        foreach(var questRecord in GameData.instance.questRecords.Values)
        {
          if (questRecord.completed > questRecord.collected)
          {
                // Turn on the panel alert, as there is at least one completed quests waiting to be collected.
                GameGUI.instance.SetPanelAlertState(GameGUI.GamePanel.GAME_PANEL_QUESTS, true);
                return;
          }
        }

        // turn off the panel alert, as there are no completed quests waiting to be collected.
        GameGUI.instance.SetPanelAlertState(GameGUI.GamePanel.GAME_PANEL_QUESTS, false);
    }

    public void CollectButtonPressed(QuestEntry _entry)
    {
        // Let the AdBonusButton know the latest QuestEntry that has been attempted to be collected.
        AdBonusButton.instance.SetQuestAnchorBonus(_entry.collectButton.gameObject.GetComponent<RectTransform>());

        Network.instance.SendCommand("action=quest_collect|questID=" + _entry.GetID());
    }
}
