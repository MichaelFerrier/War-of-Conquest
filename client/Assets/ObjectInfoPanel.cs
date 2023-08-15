using UnityEngine;
using UnityEngine.UI;
using UnityEngine.EventSystems;
using System.Collections;
using I2.Loc;

public class ObjectInfoPanel : MonoBehaviour, IPointerDownHandler
{
    public static ObjectInfoPanel instance;

    public Camera info_camera;
    public RawImage camera_raw_image;
    public TMPro.TextMeshProUGUI title, stats_text, desc_text;
    public GameObject orbRanksButtonObject;

    private bool active = false;
    public Vector3 camera_target = new Vector3(0, 0, 0);
    private float camera_speed = 25f;
    private GameObject object_model = null;
    private string line_title_color = "yellow";
    private int objectID, blockX, blockZ;

    // Link information
    public LinkManager linkManager = new LinkManager();

    public ObjectInfoPanel()
    {
        instance = this;
    }

    public void InitForBuild(int _blockX, int _blockZ, int _buildID)
    {
        string fragment;

        if (active) {
            CleanUp();
        }

        active = true;

        // Reset the count of links to 0.
        linkManager.ResetLinks();

        // Get the build data
        BuildData build_data = BuildData.GetBuildData(_buildID);

        // Set the name
        title.text = build_data.name;

        // Hide the orb earning ranks button.
        orbRanksButtonObject.SetActive(false);

        // Set up the info camera display of the build object.
        SetUpBuildDisplay(build_data);

        // Set the info camera's aspect ratio to match that of the RawImage that will be displaying its output.
        info_camera.aspect = camera_raw_image.gameObject.GetComponent<RectTransform>().rect.width / camera_raw_image.gameObject.GetComponent<RectTransform>().rect.height;

        // Compose stats text ///////////////////

        string stats = "";

        // GB-Localization
        // Attack
        string _attack = LocalizationManager.GetTranslation("Generic Text/attack_word");
        //  hit" + ((build_data.num_attacks > 1) ? "s" : "")
        string _hit_or_hits = (build_data.num_attacks > 1) ? LocalizationManager.GetTranslation("hits_word") : LocalizationManager.GetTranslation("hit_word");
        string _nearby_enemy_square_or_squares = (build_data.num_attacks > 1) ? LocalizationManager.GetTranslation("squares_word") : LocalizationManager.GetTranslation("square_word");

        if ((build_data.type == BuildData.Type.DIRECTED_MULTIPLE) || (build_data.type == BuildData.Type.SPLASH) || (build_data.type == BuildData.Type.TOWER_BUSTER) || (build_data.type == BuildData.Type.COUNTER_ATTACK) || (build_data.type == BuildData.Type.RECAPTURE))
        {
            // "<color=" + line_title_color + ">Attack:</color> " + build_data.num_attacks + " hit" + ((build_data.num_attacks > 1) ? "s" : "") + " for " + build_data.attack_min_hp + "-" + build_data.attack_max_hp + " hp\n";
            stats += System.String.Format("<color={0}>{1}:</color> {2} {3}  for {4}-{5} hp\n",
                line_title_color, _attack, build_data.num_attacks, _hit_or_hits, build_data.attack_min_hp, build_data.attack_max_hp);
        }
        else if ((build_data.type == BuildData.Type.WIPE) || (build_data.type == BuildData.Type.GENERAL_LASTING_WIPE) || (build_data.type == BuildData.Type.SPECIFIC_LASTING_WIPE))
        {
            // "<color=" + line_title_color + ">Attack:</color> wipes out all enemies within radius of " + build_data.effect_radius + "\n";
            stats += System.String.Format("<color={0}>{1}:</color> {2} {3}\n",
                line_title_color, _attack, LocalizationManager.GetTranslation("wipes_out_all_enemies_radius"), build_data.effect_radius);
        }
        else if (build_data.type == BuildData.Type.AIR_DROP)
        {
            // "<color=" + line_title_color + ">Attack:</color> captures up to " + build_data.num_attacks + " nearby enemy square" + ((build_data.num_attacks > 1) ? "s" : "") + "\n";
            // source: "captures up to {[NUM_ATTACKS]} nearby enemy squares"
            // source: "captures up to {[NUM_ATTACKS]} nearby enemy square"

            fragment = ((build_data.num_attacks > 1) ? LocalizationManager.GetTranslation("captures_n_enemy_sqares_fragment") : LocalizationManager.GetTranslation("captures_1_enemy_square_fragment"))
                            .Replace("{[NUM_ATTACKS]}", build_data.num_attacks.ToString());

            stats += System.String.Format("<color={0}>{1}:</color> {2}\n", line_title_color, _attack, fragment);
        }
        else if (build_data.type == BuildData.Type.AREA_EFFECT)
        {
            // "<color=" + line_title_color + ">Attack:</color> hits all enemy squares within a radius of " + build_data.attack_radius + " for " + build_data.attack_min_hp + "-" + build_data.attack_max_hp + " hp\n";
            // source: "hits all enemy squares within a radius of {[ATTACK_RADIUS]} for {[ATTACK_MIN_HP]}-{[ATTACK_MAX_HP]} hp"
            fragment = LocalizationManager.GetTranslation("hit_all_enemy_radius_for_min_max_hp_fragment")
                            .Replace("{[ATTACK_RADIUS]}", build_data.attack_radius.ToString())
                            .Replace("{[ATTACK_MIN_HP]}", build_data.attack_min_hp.ToString())
                            .Replace("{[ATTACK_MAX_HP]}", build_data.attack_max_hp.ToString());

            stats += System.String.Format("<color={0}>{1}:</color> {2}\n", line_title_color, _attack, fragment);
        }
        else if (build_data.type == BuildData.Type.DUMMY)
        {
            // "<color=" + line_title_color + ">Attack:</color> none\n";
            stats += System.String.Format("<color={0}>{1}:</color> {2}\n",
                line_title_color, _attack, LocalizationManager.GetTranslation("none_word"));
        }

        // Splash Radius
        if (build_data.type == BuildData.Type.SPLASH)
        {
            // "<color=" + line_title_color + ">Splash Radius:</color> " + build_data.effect_radius + "\n";
            stats += System.String.Format("<color={0}>{1}:</color> {2}\n",
                line_title_color, LocalizationManager.GetTranslation("splash_radius"), build_data.effect_radius);
        }

        // GB-Localization
        // Trigger

        string _triggered = LocalizationManager.GetTranslation("triggered_word");

        if (build_data.trigger_on == BuildData.TriggerOn.DIRECT_ATTACK)
        {
            // "<color=" + line_title_color + ">Triggered:</color> when directly attacked\n";
            // "when directly attacked"
            stats += string.Format("<color={0}>{1}:</color> {2}\n",
                line_title_color, _triggered, LocalizationManager.GetTranslation("when_directly_attacked_fragment"));
        }
        else if (build_data.trigger_on == BuildData.TriggerOn.RADIUS_ATTACK)
        {
            // "<color=" + line_title_color + ">Triggered:</color> when the nation is attacked within radius of " + build_data.attack_radius + "\n";
            // when the nation is attacked within radius of {[ATTACK_RADIUS]}
            fragment = LocalizationManager.GetTranslation("when_nation_attacked_within_radius_fragment")
                            .Replace("{[ATTACK_RADIUS]}", build_data.attack_radius.ToString());

            stats += string.Format("<color={0}>{1}:</color> {2}\n",
                line_title_color, _triggered, fragment);
        }
        else if (build_data.trigger_on == BuildData.TriggerOn.RADIUS_TOWER)
        {
            // "<color=" + line_title_color + ">Triggered:</color> when an enemy tower activates within radius of " + build_data.attack_radius + "\n";
            // when an enemy tower activates within radius of {[ATTACK_RADIUS]}
            fragment = LocalizationManager.GetTranslation("when_enemy_tower_attacked_within_radius_fragment")
                            .Replace("{[ATTACK_RADIUS]}", build_data.attack_radius.ToString());

            stats += string.Format("<color={0}>{1}:</color> {2}\n",
                line_title_color, _triggered, fragment);
        }
        else if (build_data.trigger_on == BuildData.TriggerOn.RADIUS_ATTACK_EMPTY)
        {
            // "<color=" + line_title_color + ">Triggered:</color> when an empty square belonging to the nation is attacked within radius of " + build_data.attack_radius + "\n";
            // when the nation is attacked within radius of {[ATTACK_RADIUS]}
            fragment = LocalizationManager.GetTranslation("when_empty_attacked_within_radius_fragment")
                            .Replace("{[ATTACK_RADIUS]}", build_data.attack_radius.ToString());

            stats += string.Format("<color={0}>{1}:</color> {2}\n",
                line_title_color, _triggered, fragment);
        }

        // Flank nullifies
        string _flanking = LocalizationManager.GetTranslation("flanking_word");

        if (build_data.flank_nullifies)
        {
            // "<color=" + line_title_color + ">Flanking:</color> attack nullified if flanked on all sides\n"
            stats += string.Format("<color={0}>{1}:</color> {2}\n",
                line_title_color, _flanking, LocalizationManager.GetTranslation("attack_nullified_if_flanked_fragment"));
        }

        // Cooldown time
        string _cooldown = LocalizationManager.GetTranslation("cooldown_word");

        if (build_data.cooldown_time > 0)
        {
            // "<color=" + line_title_color + ">Cooldown:</color> " + GameData.instance.GetDurationText(build_data.cooldown_time) + "\n";
            stats += string.Format("<color={0}>{1}:</color> {2}\n",
                line_title_color, _cooldown, GameData.instance.GetDurationText(build_data.cooldown_time));
        }

        // Capacity
        string _capacity = LocalizationManager.GetTranslation("capacity_word");

        if (build_data.type == BuildData.Type.MANPOWER_STORAGE)
        {
            // "<color=" + line_title_color + ">Capacity:</color> " + build_data.capacity + " manpower\n";
            stats += string.Format("<color={0}>{1}:</color> {2} {3}\n",
                line_title_color, _capacity, build_data.capacity, LocalizationManager.GetTranslation("manpower_word"));
        }
        else if (build_data.type == BuildData.Type.ENERGY_STORAGE)
        {
            // "<color=" + line_title_color + ">Capacity:</color> " + build_data.capacity + " energy\n";
            stats += string.Format("<color={0}>{1}:</color> {2} {3}\n",
                line_title_color, _capacity, build_data.capacity, LocalizationManager.GetTranslation("energy_word"));
        }

        // XP per hour

        if (build_data.xp_per_hour > 0)
        {
            // "<color=" + line_title_color + ">XP generated (max):</color> " + build_data.xp_per_hour + " / hour\n";
            stats += string.Format("<color={0}>{1}:</color> {2} / {3}\n",
                line_title_color, LocalizationManager.GetTranslation("xp_generated_max"), build_data.xp_per_hour, LocalizationManager.GetTranslation("time_hour"));
        }

        // Wipe duration

        if ((build_data.type == BuildData.Type.GENERAL_LASTING_WIPE) || (build_data.type == BuildData.Type.SPECIFIC_LASTING_WIPE))
        {
            // "<color=" + line_title_color + ">Wipe Duration:</color> " + GameData.instance.GetDurationText(build_data.wipe_duration) + "\n";
            stats += string.Format("<color={0}>{1}:</color> " + GameData.instance.GetDurationText(build_data.wipe_duration) + "\n",
                line_title_color, LocalizationManager.GetTranslation("wipe_duration"));
        }

        // GB-Localization
        // Hit points

        if (build_data.hit_points > 0)
        {
            // "<color=" + line_title_color + ">Hit Points:</color> " + build_data.hit_points + "\n";
            stats += string.Format("<color={0}>{1}:</color> " + build_data.hit_points + "\n",
                line_title_color, LocalizationManager.GetTranslation("hit_points"));
        }

        // Cost

        // "<color=" + line_title_color + ">Cost:</color> " + build_data.manpower_cost + " <sprite=0>, " + build_data.energy_burn_rate + " <sprite=1> / hour\n";
        stats += string.Format("<color={0}>{1}:</color> {2} <sprite=0>, {3} <sprite=1> / {4}\n",
            line_title_color, LocalizationManager.GetTranslation("cost_word"), build_data.manpower_cost, build_data.energy_burn_rate, LocalizationManager.GetTranslation("time_hour"));

        // Build time

        // "<color=" + line_title_color + ">Build Time:</color> " + ((build_data.build_time == 0) ? "instant" :  GameData.instance.GetDurationText(build_data.build_time)) + "\n";
        stats += string.Format("<color={0}>{1}:</color> {2}\n",
            line_title_color, LocalizationManager.GetTranslation("build_time"),
            ((build_data.build_time == 0) ? LocalizationManager.GetTranslation("instant_word") :  GameData.instance.GetDurationText(build_data.build_time)));

        //stats += "\n"; // This extra space makes some descriptions too long.

        // GB-Localization
        // Upgrades

        if (build_data.upgrades != -1)
        {
            // "<color=" + line_title_color + ">Upgrades from:</color> <link=\"upgrades_from\"><u>" + BuildData.GetBuildData(build_data.upgrades).name + "</u></link>\n";
            stats += string.Format("<color={0}>{1}:</color> <link=\"upgrades_from\"><u>{2}</u></link>\n",
                line_title_color, LocalizationManager.GetTranslation("upgrades_from"), BuildData.GetBuildData(build_data.upgrades).name);
            linkManager.AddLink(LinkManager.LinkType.BUILD, build_data.upgrades);
        }

        if (build_data.upgrades_to != -1)
        {
            // "<color=" + line_title_color + ">Upgrades to:</color> <link=\"upgrades_to\"><u>" + BuildData.GetBuildData(build_data.upgrades_to).name + "</u></link>\n";
            stats += string.Format("<color={0}>{1}:</color> <link=\"upgrades_to\"><u>{2}</u></link>\n",
                line_title_color, LocalizationManager.GetTranslation("upgrades_to"), BuildData.GetBuildData(build_data.upgrades_to).name);
            linkManager.AddLink(LinkManager.LinkType.BUILD, build_data.upgrades_to);
        }

        if (build_data.required_advance != -1)
        {
            // "<color=" + line_title_color + ">Requires:</color> <link=\"required_advance\"><u>" + TechData.GetTechData(build_data.required_advance).name + "</u></link>\n";
            stats += string.Format("<color={0}>{1}:</color> <link=\"required_advance\"><u>{2}</u></link>\n",
                line_title_color, LocalizationManager.GetTranslation("requires_word"), TechData.GetTechData(build_data.required_advance).name);
            linkManager.AddLink(LinkManager.LinkType.TECH, build_data.required_advance);
        }

        // Display the stats text
        stats_text.text = stats;

        // Display the description
        BlockData block_data = MapView.instance.GetBlockData(_blockX, _blockZ);
        if ((block_data != null) && (block_data.crumble_time > Time.time))
        {
            // Display info about the build being captured, and that it will crumble.
            // This {build_name} has been captured from the nation that built it. Unless it is reclaimed by that nation within {time} it will crumble and be lost.
            desc_text.text = "<color=orange>" + 
                LocalizationManager.GetTranslation("object_info_crumble_message")
                .Replace("{build_name}", build_data.name)
                .Replace("{time}", GameData.instance.GetDurationText((int)(block_data.crumble_time - Time.time))) + 
                "</color>";
        }
        else if ((block_data != null) && (block_data.build_object != null) && (block_data.nationID == GameData.instance.nationID) && block_data.build_object.IsInert())
        {
            // Display info about the build being inert.
            if (GameData.instance.GetFinalGeoEfficiency() < 1f)
            {
                // The nation {nation_name}'s geographic efficiency isn't high enough to support all of its defenses. This {build_name} has become inert and useless until the geographic efficiency is increased.
                NationData nation_data = GameData.instance.nationTable[block_data.nationID];
                desc_text.text = "<color=orange>" + 
                    LocalizationManager.GetTranslation("object_info_inert_message_geo")
                    .Replace("{nation_name}", nation_data.GetName(block_data.nationID != GameData.instance.nationID))
                    .Replace("{build_name}", build_data.name) +
                    "</color>";
            }
            else
            {
                // The nation {nation_name} isn't generating energy fast enough to support all of its defenses. This {build_name} has become inert and useless until there is enough energy to support it.
                NationData nation_data = GameData.instance.nationTable[block_data.nationID];
                desc_text.text = "<color=orange>" + 
                    LocalizationManager.GetTranslation("object_info_inert_message")
                    .Replace("{nation_name}", nation_data.GetName(block_data.nationID != GameData.instance.nationID))
                    .Replace("{build_name}", build_data.name) +
                    "</color>";
            }
        }
        else
        {
            // Display the build's description.
            desc_text.text = build_data.description;
        }
    }

    public void InitForObject(int _blockX, int _blockZ, int _objectID)
    {
        if (active) {
            CleanUp();
        }

        active = true;
        objectID = _objectID;
        blockX = _blockX;
        blockZ = _blockZ;

        // Reset the count of links to 0.
        linkManager.ResetLinks();

        // Get the block's data
        BlockData block_data = MapView.instance.GetBlockData(blockX, blockZ);

        // Get the object data
        ObjectData object_data = ObjectData.GetObjectData(_objectID);

        // Set the name
        title.text = object_data.name;

        // Show the orb earning ranks button only if the object is an orb.
        orbRanksButtonObject.SetActive(_objectID >= ObjectData.ORB_BASE_ID);

        // Set the info camera's culling mask
        info_camera.cullingMask = (1 << 0) | (1 << 4) | (1 << 9); // Default, Water, Build Info
        
        // Position camera at appropriate height depending on which landscape object is being displayed.
        if ((objectID == 1001)) {
            info_camera.gameObject.transform.localPosition = new Vector3(0, 12.6f, -24.5f) + MapView.instance.GetBlockCenterWorldPos(_blockX, _blockZ);
            info_camera.fieldOfView = 36;
        } else if ((objectID == 1000) || (objectID == 1004) || (objectID == 1005) || (objectID == 1006) || (objectID == 1007) || (objectID == 1008) || (objectID == 1010) || (objectID == 1012) || (objectID == 1014) || (objectID == 1015) || (objectID == 1016)) {
            info_camera.gameObject.transform.localPosition = new Vector3(0, 16.6f, -24.5f) + MapView.instance.GetBlockCenterWorldPos(_blockX, _blockZ);
            info_camera.fieldOfView = 36;
        } else if ((objectID == 1002) || (objectID == 1003) || (objectID == 2000)) {
            info_camera.gameObject.transform.localPosition = new Vector3(0, 18.6f, -24.5f) + MapView.instance.GetBlockCenterWorldPos(_blockX, _blockZ);
            info_camera.fieldOfView = 36;
        } else if ((objectID == 2001) || (objectID == 2002) || (objectID == 2003) || (objectID == 2004)) {
            info_camera.gameObject.transform.localPosition = new Vector3(0, 23.6f, -24.5f) + MapView.instance.GetBlockCenterWorldPos(_blockX, _blockZ);
            info_camera.fieldOfView = 45;
        } else { // 1009, 1011, 1013
            info_camera.gameObject.transform.localPosition = new Vector3(0, 20.6f, -24.5f) + MapView.instance.GetBlockCenterWorldPos(_blockX, _blockZ);
            info_camera.fieldOfView = 36;
        }
        
        // Set the camera's rotation and target position.
        info_camera.gameObject.transform.eulerAngles = new Vector3(30, 0, 0);
        camera_target = MapView.instance.GetBlockCenterWorldPos(_blockX, _blockZ);

        // Orb of the Void or Orb of Fire
        if ((_objectID == 2002) || (_objectID == 2004)) 
        {
            if ((block_data != null) && (block_data.landscape_object != null))
            {
                // Have the glow face the info camera
                block_data.landscape_object.object0.transform.GetChild(0).GetChild(0).gameObject.GetComponent<LookAtScript>().camera_to_face = info_camera;

                // Change the glow's layer so that it doesn't show up (facing the info camera) in the game camera.
                block_data.landscape_object.object0.transform.GetChild(0).GetChild(0).gameObject.layer = 9;
            }
        }

        // Enable the info camera
        info_camera.enabled = true;
        
        // Set the info camera's aspect ratio to match that of the RawImage that will be displaying its output.
        info_camera.aspect = camera_raw_image.gameObject.GetComponent<RectTransform>().rect.width / camera_raw_image.gameObject.GetComponent<RectTransform>().rect.height;
        
        // Compose stats text ///////////////////

        string stats = "";

        if (object_data.type == ObjectData.Type.TECH)
        {
            // GB-Localization
            // Get the landscape object's technology's data.
            TechData tech_data = TechData.GetTechData(object_data.techID);  
     
            // Add prerequisites
            if ((tech_data.prerequisite_tech_1 != -1) || (tech_data.prerequisite_tech_2 != -1) || (tech_data.prerequisite_level > 0))
            {
                stats += string.Format("<color=yellow>{0}:</color> ", LocalizationManager.GetTranslation("requires_word"));

                if (tech_data.prerequisite_tech_1 != -1) 
                {
                    stats += "<link=\"" + linkManager.GetNumLinks() + "\"><u>" + TechData.GetTechData(tech_data.prerequisite_tech_1).name + "</u></link>";
                    linkManager.AddLink(LinkManager.LinkType.TECH, tech_data.prerequisite_tech_1);
                }

                if (tech_data.prerequisite_tech_2 != -1) 
                {
                    stats += ", <link=\"" + linkManager.GetNumLinks() + "\"><u>" + TechData.GetTechData(tech_data.prerequisite_tech_2).name + "</u></link>";
                    linkManager.AddLink(LinkManager.LinkType.TECH, tech_data.prerequisite_tech_2);
                }

                if (tech_data.prerequisite_level > 0) 
                {
                    if (tech_data.prerequisite_tech_1 != -1) {
                        stats += ", ";
                    }
                    stats += string.Format("{0} " + tech_data.prerequisite_level, LocalizationManager.GetTranslation("level_word"));
                }

                stats += "\n";
            }

            // Determine the object's position in the map.
            float position = object_data.GetPosition(blockX);

            // GB-Localization
            // Add bonuses
            if ((tech_data.bonus_type_1 != TechData.Bonus.UNDEF) || (tech_data.bonus_type_2 != TechData.Bonus.UNDEF) || (tech_data.bonus_type_3 != TechData.Bonus.UNDEF) || (tech_data.new_build_name != ""))
            {
                bool bonus_added = false;
                stats += string.Format("<color=yellow>{0}:</color> ", LocalizationManager.GetTranslation("bonus_word"));

                if (tech_data.bonus_type_1 != TechData.Bonus.UNDEF) {
                    stats += GameGUI.instance.GetBonusText(tech_data.bonus_type_1, tech_data.GetBonusVal(1), tech_data.GetBonusValMax(1), position, true, linkManager);
                    bonus_added = true;
                }

                if (tech_data.bonus_type_2 != TechData.Bonus.UNDEF) {
                    stats += (bonus_added ? ", " : "") + GameGUI.instance.GetBonusText(tech_data.bonus_type_2, tech_data.GetBonusVal(2), tech_data.GetBonusValMax(2), position, true, linkManager);
                    bonus_added = true;
                }

                if (tech_data.bonus_type_3 != TechData.Bonus.UNDEF) {
                    stats += (bonus_added ? ", " : "") + GameGUI.instance.GetBonusText(tech_data.bonus_type_3, tech_data.GetBonusVal(3), tech_data.GetBonusValMax(3), position, true, linkManager);
                    bonus_added = true;
                }
                
                stats += "\n";
            }
        }
        else if (object_data.type == ObjectData.Type.ORB)
        {
            // GB-Localization
            // "Generates {[credits]} credits and {[xp]} XP every hour."
            stats += LocalizationManager.GetTranslation((object_data.credits_per_hour == 1) ? "orb_info_singular" : "orb_info_plural")
                .Replace("{[credits]}", "<color=#00ff00ff>" + "$" + (GameData.instance.orbPayoutRates[_objectID] / 100.0).ToString("N2") + "</color>")
                .Replace("{[xp]}", "<color=#00ff00ff>" + (object_data.xp_per_hour * 24) + "</color>");
        }
        
        if ((block_data != null) && (block_data.nationID != -1))
        {
            NationData nation_data = GameData.instance.GetNationData(block_data.nationID);

            if (nation_data != null)
            {
                stats += " <color=yellow>" + LocalizationManager.GetTranslation("Generic Text/captured_by") + ":</color> ";

                if (nation_data.GetFlag(GameData.NationFlags.INCOGNITO)) {
                    stats += nation_data.GetName(block_data.nationID != GameData.instance.nationID);
                } else {
                    stats += "<link=\"" + linkManager.GetNumLinks() + "\"><u>" + nation_data.GetName(block_data.nationID != GameData.instance.nationID) + "</u></link>";
                    linkManager.AddLink(LinkManager.LinkType.NATION, block_data.nationID);
                }                
            }
        }      

        // Display the stats text
        stats_text.text = stats;

        // Display the description
        desc_text.text = object_data.description;
    }

    public void SetUpBuildDisplay(BuildData _build_data)
    {
        if (_build_data.type == BuildData.Type.WALL)
        {
            object_model = new GameObject();

            GameObject post1 = Object.Instantiate(_build_data.GetPrefab()) as GameObject;
            post1.transform.position = new Vector3(-5,0,0);
            post1.transform.parent = object_model.transform;

            GameObject post2 = Object.Instantiate(_build_data.GetPrefab()) as GameObject;
            post2.transform.position = new Vector3(5,0,0);
            post2.transform.parent = object_model.transform;

            GameObject length = Object.Instantiate(_build_data.GetWallLengthPrefab()) as GameObject;
            length.transform.position = new Vector3(-5,0,0);
            length.transform.parent = object_model.transform;
        }
        else 
        {
            object_model = Object.Instantiate(_build_data.GetPrefab()) as GameObject;
            object_model.transform.position = new Vector3(0,0,0);
        }

        _build_data.PrepObject(object_model);
        
        // Move the object model to the Build Info layer, that the info_camera views.
        MoveToLayer(object_model.transform, LayerMask.NameToLayer("Build Info"));
        
        // Set camera appropriately based on height of the object.
        float object_height = DetermineObjectHeight(object_model);
        Debug.Log("Build object height: " + object_height);
        if (object_height > 10)
        {
            info_camera.gameObject.transform.localPosition = new Vector3(0, 20.6f, -24.5f);
	        info_camera.gameObject.transform.eulerAngles = new Vector3(30f, 0, 0);
	        info_camera.fieldOfView = 36f;
        }
        else if (object_height > 7.5)
        {
            info_camera.gameObject.transform.localPosition = new Vector3(0, 20.6f, -24.5f);
            info_camera.gameObject.transform.eulerAngles = new Vector3(33.4f, 0, 0);
	        info_camera.fieldOfView = 31.4f;
        }
        else if (object_height > 5)
        {
            info_camera.gameObject.transform.localPosition = new Vector3(0, 20.6f, -24.5f);
            info_camera.gameObject.transform.eulerAngles = new Vector3(36f, 0, 0);
	        info_camera.fieldOfView = 23f;
        }
        else
        {
            info_camera.gameObject.transform.localPosition = new Vector3(0, 20.6f, -24.5f);
	        info_camera.gameObject.transform.eulerAngles = new Vector3(37f, 0, 0);
	        info_camera.fieldOfView = 18f;
        }

        // Set the info camera's culling mask
        info_camera.cullingMask = /*(1 << 0) | */(1 << 9); // Only show Build Info layer, so underwater terrain is not visible as it was when Default was also shown.

        // Set the info camera's position, rotation and target
        //info_camera.gameObject.transform.localPosition = new Vector3(0, 20.6f, -24.5f);
        //info_camera.gameObject.transform.eulerAngles = new Vector3(30, 0, 0);
        camera_target = new Vector3(0, 0, 0);
        
        // Enable the info camera
        info_camera.enabled = true;
    }

    void MoveToLayer(Transform root, int layer)
    {
        root.gameObject.layer = layer;
        foreach(Transform child in root)
            MoveToLayer(child, layer);
    }

    void OnDisable()
    {
        CleanUp();
    }

    public void OnPointerDown(PointerEventData _eventData)
    {
        // Determine whether link text has been clicked.
        int link_index = TMPro.TMP_TextUtilities.FindIntersectingLink(stats_text, Input.mousePosition, null);

        if (link_index != -1)
        {
            if (linkManager.link_types[link_index] == LinkManager.LinkType.BUILD)
            {
                // Display info about the clicked build in this panel.
                InitForBuild(-1, -1, linkManager.link_ids[link_index]);
            }
            else if (linkManager.link_types[link_index] == LinkManager.LinkType.TECH)
            {
                // Display the Advances panel with info about the clicked tech.
                GameGUI.instance.CloseAllPanels();
                int advanceID = linkManager.link_ids[link_index]; // Get ID from link before SetActiveGamePanel() replaces links.
                if (AdvancesPanel.instance.mode == AdvancesPanel.Mode.Tree) {
                    // Open the advances panel.
                    GameGUI.instance.SetActiveGamePanel(GameGUI.GamePanel.GAME_PANEL_ADVANCES);
                    AdvancesPanel.instance.SelectAdvance(advanceID, true);
                } else {
                    // Open the advance details panel to display info on this advance.
                    AdvanceDetailsPanel.instance.Activate(advanceID);
                    AdvancesPanel.instance.SelectAdvance(advanceID, true);
                }                
            }
            else if (linkManager.link_types[link_index] == LinkManager.LinkType.STAT)
            {
                // Display the stat details panel with info about the clicked bonus' stat.
                GameGUI.instance.CloseAllPanels();
                StatDetailsPanel.instance.ActivateForBonus((TechData.Bonus)(linkManager.link_ids[link_index]));
            }
            else if (linkManager.link_types[link_index] == LinkManager.LinkType.NATION)
            {
                // Close this panel
                GameGUI.instance.CloseAllPanels();

                // Send event_nation_info event to the server.
                Network.instance.SendCommand("action=request_nation_info|targetNationID=" + linkManager.link_ids[link_index]);
            }
        }
    }

    public void CleanUp()
    {
        // Disable the info camera
        if (info_camera != null) {
            info_camera.enabled = false;
        }

        // Destroy the object model.
        if (object_model != null) 
        {
            ObjectData.DestroyAllMaterials(object_model);
            Object.Destroy(object_model);
        }

        // Orb of the Void or Orb of Fire
        if ((objectID == 2002) || (objectID == 2004)) 
        {
            BlockData block_data = MapView.instance.GetBlockData(blockX, blockZ);
            if ((block_data != null) && (block_data.landscape_object != null))
            {
                // Return the glow to facing the game camera
                block_data.landscape_object.object0.transform.GetChild(0).GetChild(0).gameObject.GetComponent<LookAtScript>().camera_to_face = Camera.main;

                // Change the glow's layer back to default so that it show up again in the game camera.
                block_data.landscape_object.object0.transform.GetChild(0).GetChild(0).gameObject.layer = 1;
            }
        }

        active = false;
    }

    void Update()
    {
        if (active)
        {
            info_camera.transform.RotateAround(camera_target, Vector3.up, camera_speed * Time.unscaledDeltaTime);
        }
    }

    public void OnClick_OrbRanksButton()
    {
        // Close the object info panel and open the Info/Ranks panel to display this orb's ranks.
        GameGUI.instance.CloseAllPanels();
        ConnectPanel.instance.SetPendingRanksList(GameData.RanksListType.ORB_WINNINGS, blockX, blockZ);
        GameGUI.instance.SetActiveGamePanel(GameGUI.GamePanel.GAME_PANEL_CONNECT);
    }

    public void OnClick_Close()
    {
        GameGUI.instance.CloseAllPanels();
    }

    // Figure out what to scale what by here:
    private float DetermineObjectHeight(GameObject _object)
    {
        float result = 0f;

        if (_object == null) {
            Debug.Log("DetermineObjectHeight() given null");
            return 0f;
        }

        //Debug.Log("DetermineObjectHeight() called for " + _object.name);

        Renderer renderer = _object.GetComponent<Renderer>();
        if (renderer != null) {
            result = renderer.bounds.max.y;
            //Debug.Log("   " + _object.name + "'s max y: " + renderer.bounds.max.y);
        }

        // Use maximum between this object's height and height of each of its children.
        foreach (Transform child in _object.transform) {
            result = Mathf.Max(result, DetermineObjectHeight(child.gameObject));
        }

        return result;
    }
}
