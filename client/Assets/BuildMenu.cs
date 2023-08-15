using UnityEngine;
using UnityEngine.UI;
using UnityEngine.EventSystems;
using System.Collections;

public class BuildMenu : MonoBehaviour
{
    public static BuildMenu instance;
    public GameObject buildMenuBase, buildMenuContentObject;
    public int blockX, blockZ;
    public int prevBuildID = -1;
    public bool requires_repopulate = true;
    public RenderTexture rt = null;

    public const int BUILD_ICON_WIDTH = 256;    
    public const int BUILD_ICON_HEIGHT = 256;

    public BuildMenu()
    {
        instance = this;
    }

    void Start ()
    {
    }

    public void Activate(int _blockX, int _blockZ)
    {
        // Record block coordinates
        blockX = _blockX;
        blockZ = _blockZ;

        // Show the build menu.
        buildMenuBase.SetActive(true);
        buildMenuBase.GetComponent<GUITransition>().StartTransition(0, 1, 1, 1, false);

        if (requires_repopulate) 
        {
            StartCoroutine(Populate());
            requires_repopulate = false;
        }

        // Tell the tutorial system that the build panel has opened.
        Tutorial.instance.BuildMenuOpened();
    }

    public void Deactivate()
    {
        buildMenuBase.GetComponent<GUITransition>().StartTransition(1, 0, 1, 1, true);
    }

    public void UpdateForTechnologies()
    {
        Refresh();
    }

    public void Refresh()
    {
        if (gameObject.activeSelf) {
            StartCoroutine(Populate());
        } else {
            requires_repopulate = true;
        }
    }

    public IEnumerator Populate()
    {
        // Remove any build icons from the build menu.
        GameObject cur_entry_object;
        while (buildMenuContentObject.transform.childCount > 0)
        {
            cur_entry_object = buildMenuContentObject.transform.GetChild(0).gameObject;
            cur_entry_object.transform.SetParent(null);
            MemManager.instance.ReleaseBuildIconObject(cur_entry_object);
        }

        foreach(var item in GameData.instance.availableBuilds)
        {
            if (item.Value == false) {
                continue;
            }

            BuildData build_data = BuildData.GetBuildData(item.Key);

            if ((GameData.instance.mapMode == GameData.MapMode.MAINLAND) && ((build_data.land & BuildData.LAND_FLAG_MAINLAND) == 0)) {
                continue;
            }

            if ((GameData.instance.mapMode == GameData.MapMode.HOMELAND) && ((build_data.land & BuildData.LAND_FLAG_HOMELAND) == 0)) {
                continue;
            }

            if ((GameData.instance.mapMode == GameData.MapMode.RAID) && ((build_data.land & BuildData.LAND_FLAG_RAID) == 0)) {
                continue;
            }

            // If the nation already has the maximum number of this build, do not add it to the build menu.
            if ((build_data.max_count != -1) && GameData.instance.builds.ContainsKey(build_data.ID) && (GameData.instance.builds[build_data.ID] >= build_data.max_count)) {
                continue;
            }

            // Get a new build icon
            GameObject iconObject = MemManager.instance.GetBuildIconObject();

            // Add the new icon to the list.
            iconObject.transform.SetParent(buildMenuContentObject.transform);
            iconObject.transform.SetAsLastSibling();
            iconObject.transform.localScale = new Vector3(1, 1, 1); // Needs to be done each time it's activated, in case it was changed last time used.

            // Get pointer to BuildIcon component.
            BuildIcon curIcon = iconObject.GetComponent<BuildIcon>();

            // Initialize the new icon
            curIcon.Init(build_data);

            // Set up the Build Icon's button's listener.
            Button iconButton = iconObject.GetComponent<Button>();
            iconButton.onClick.RemoveAllListeners();
            iconButton.onClick.AddListener(() => IconButtonPressed());

            // Set up the Build Icon's info button's listener.
            Button infoButton = iconObject.transform.GetChild(0).GetChild(0).gameObject.GetComponent<Button>();
            infoButton.onClick.RemoveAllListeners();
            infoButton.onClick.AddListener(() => InfoButtonPressed());

            // Generate the build object's icon image if necessary
            if (build_data.build_icon_sprite == null)
            {
                if (rt == null)
                {
                    // Create a RenderTexture for rendering build icon images.
                    rt = new RenderTexture(BUILD_ICON_WIDTH, BUILD_ICON_HEIGHT, 24, RenderTextureFormat.ARGB32);
                    rt.antiAliasing = 4;
                    rt.Create();
                }

                // Create a Sprite to display the build
                Texture2D newTexture = new Texture2D(BUILD_ICON_WIDTH, BUILD_ICON_HEIGHT);
                build_data.build_icon_sprite = Sprite.Create(newTexture, new Rect(0f,0f,BUILD_ICON_WIDTH,BUILD_ICON_HEIGHT), new Vector2(0f,0f));

                // Set up the object info panel's display of the build object.
                ObjectInfoPanel.instance.SetUpBuildDisplay(build_data);

                // Set angle of info camera
                MapView.instance.infoCamera.transform.RotateAround(ObjectInfoPanel.instance.camera_target, Vector3.up, 40f);

                // Assign the RenderTexture to be the info camera's render target.
                RenderTexture originalRenderTexture = MapView.instance.infoCamera.targetTexture;
                MapView.instance.infoCamera.targetTexture = rt;

                // Render to the RenderTexture, and wait unto the frame is complete.
                MapView.instance.infoCamera.Render();
                yield return new WaitForEndOfFrame();

                // Make the RenderTexture active, so that ReadPixels will read from it rather than from the screen buffer.
                RenderTexture.active = rt;

                // Copy the image from the RenderTexture into the build's sprite's texture.
                build_data.build_icon_sprite.texture.ReadPixels(new Rect(0,0,rt.width, rt.height), 0,0);
                build_data.build_icon_sprite.texture.Apply();

                // Clean up.
                RenderTexture.active = null;
                MapView.instance.infoCamera.targetTexture = originalRenderTexture;

                // Clean up the object info panel's display of the build object.
                ObjectInfoPanel.instance.CleanUp();
            }

            // Set up the Build Icon's image.
            curIcon.image.sprite = build_data.build_icon_sprite;
            
            //Debug.Log("Build icon added: " + build_data.name);
        }
    }

    public void IconButtonPressed()
    {
        // Make sure that a BuildIcon is the currently selected GameObject.
        if ((EventSystem.current == null) || (EventSystem.current.currentSelectedGameObject == null) || (EventSystem.current.currentSelectedGameObject.GetComponent<BuildIcon>() == null)) {
            return;
        }

        // Get the ID of the build icon's build object.
        // (Note that passing that info through the listener into this method wasn't working anymore once I made Populate() a coroutine.)
        int buildID = EventSystem.current.currentSelectedGameObject.GetComponent<BuildIcon>().ID;

        // Send build event to the server.
        Woc.ContextMenu.instance.Build(buildID, blockX, blockZ);

        // Record the ID of the previously built structure.
        prevBuildID = buildID;

        // Close the build menu.
        Deactivate();
    }

    public void InfoButtonPressed()
    {
        // Get the ID of the build icon's build object.
        // (Note that passing that info through the listener into this method wasn't working anymore once I made Populate() a coroutine.)
        int buildID = EventSystem.current.currentSelectedGameObject.transform.parent.parent.gameObject.GetComponent<BuildIcon>().ID;

        // Open the info dialog.
        GameGUI.instance.OpenBuildInfoDialog(-1, -1, buildID);
    }
}
