using UnityEngine;
using UnityEngine.UI;
using UnityEngine.EventSystems;
using System.Collections;

public class PanelButton : MonoBehaviour, IPointerDownHandler
{
    public GameGUI.GamePanel panelID;
    public TMPro.TMP_FontAsset activeFont, inactiveFont;
    public Sprite activeSprite, inactiveSprite;
    public Image image, alertImage;
    public TMPro.TextMeshProUGUI text;

    bool panelIsActive = false, alertState = false;

	// Use this for initialization
	void Start ()
    {
        SetAlertState(false);
    }

    public void OnEnable()
    {
        alertImage.gameObject.SetActive(alertState);
        if (alertState) {            
            StartCoroutine(Alert_Coroutine());
        }
    }
	
	public void SetPanelIsActive(bool _panelIsActive)
    {
        if (_panelIsActive != panelIsActive)
        {
            image.sprite = _panelIsActive ? activeSprite : inactiveSprite;
			text.font = _panelIsActive ? activeFont : inactiveFont;
            panelIsActive = _panelIsActive;
        }
    }

    public void SetAlertState(bool _alertState)
    {
        alertImage.gameObject.SetActive(_alertState);

        if (alertState != _alertState)
        {
            alertState = _alertState;
            if (alertState && gameObject.activeInHierarchy) {
                StartCoroutine(Alert_Coroutine());
            }
        }
    }

    public IEnumerator Alert_Coroutine()
    {
        float start_time = Time.unscaledTime;
        while (alertState && gameObject.activeInHierarchy)
        {
            alertImage.color = new Color(1f, 1f, 1f, Mathf.Abs(Mathf.Sin(Time.unscaledTime * 2)));
            yield return null;
        }
    }

    public void OnPointerDown(PointerEventData eventData)
    {
        switch (panelID)
        {
            case GameGUI.GamePanel.GAME_PANEL_NATION: GameGUI.instance.OnClick_Nation(); break;
            case GameGUI.GamePanel.GAME_PANEL_QUESTS: GameGUI.instance.OnClick_Quests(); break;
            case GameGUI.GamePanel.GAME_PANEL_RAID: GameGUI.instance.OnClick_RaidLog(); break;
            case GameGUI.GamePanel.GAME_PANEL_ADVANCES: GameGUI.instance.OnClick_Advances(); break;
            case GameGUI.GamePanel.GAME_PANEL_MESSAGES: GameGUI.instance.OnClick_Messages(); break;
            case GameGUI.GamePanel.GAME_PANEL_CONNECT: GameGUI.instance.OnClick_Info(); break;
            case GameGUI.GamePanel.GAME_PANEL_OPTIONS: GameGUI.instance.OnClick_Options(); break;
        }
    }
}
