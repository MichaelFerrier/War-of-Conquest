using UnityEngine;
using UnityEngine.UI;
using System.Collections;
using I2.Loc;

namespace Woc
{
	public class ContextMenu : MonoBehaviour, RequestorListener
	{
		public static Woc.ContextMenu instance;

		public MapView mapView;
		public GameData gameData;
		public RectTransform arrow;

		public GameObject contextMenuBase, contextMenu;
		public RectTransform arrowUp, arrowDown, arrowLeft, arrowRight, arrowUpLeft, arrowUpRight, arrowDownLeft, arrowDownRight;
		public RectTransform contextMenuRectTransform;
		public Button salvageButton, evacuateButton, buildButton, buildPreviousButton, upgradeButton, chatListButton, nationInfoButton, objectInfoButton, evacuateAreaButton, occupyAreaButton, moveToButton, flagButton, splashAttackButton, completeButton, allianceButton;

		bool press_and_hold, press_released_after_activation, menu_is_active = false;
		int blockX = -1, blockZ = -1;
		int blockNationID = -1;
		float blockEndTime = 0;
		string blockNationName;
		bool blockNationIncognito, blockNationIsInChatList, blockNationWasSentAllianceInvitation, blockNationIsAlly;
		int availableUpgrade = -1;
		int completionCost = 0;
		int nationInfoID = -1;
		int objectInfoID = -1;
		GameData.LimitType limit_type = GameData.LimitType.Undef;
		float stateChangeTime = -1;

		enum Direction { Up, UpperRight, Right, LowerRight, Down, LowerLeft, Left, UpperLeft };

		private enum RequestorTask
		{
			SendAllianceInvitation,
			WithdrawAllianceInvitation,
			BreakAlliance
		};

		private const float SIDE_OFFSET_Y = 46.5f;
		private const float SIDE_OFFSET_X = 44f;
		private const float CORNER_OFFSET_Y = 28.83f;
		private const float CORNER_OFFSET_X = 24.68f;

		public ContextMenu()
		{
			// Set instance in constructor, as that is always called when the app starts.
			instance = this;
		}

		public bool Activate(int _blockX, int _blockY, bool _press_and_hold)
		{
			press_and_hold = _press_and_hold;
			menu_is_active = true;

			// Set up the menu for the given block.
			bool valid = Setup(_blockX, _blockY, press_and_hold);

			if (valid)
			{
				// Show the context menu.
				// Transition in the menu and the arrow separately, rather than the base, because the base canvas group blocks access to the buttons.
				contextMenuBase.GetComponent<GUITransition>().EndTransition();
				contextMenuBase.SetActive(true);
				contextMenu.GetComponent<GUITransition>().StartTransition(0, 1, 1, 1, false);
				arrow.gameObject.GetComponent<GUITransition>().StartTransition(0, 1, 1, 1, false);
				stateChangeTime = Time.unscaledTime;
			}

			// Keep track of whether the press that opened this menu has been released after activation.
			press_released_after_activation = false;

			return valid;
		}

		public void Deactivate()
		{
			contextMenuBase.GetComponent<GUITransition>().StartTransition(1, 0, 1, 1, true);
			contextMenu.GetComponent<GUITransition>().StartTransition(1, 0, 1, 1, false);
			if (arrow != null) arrow.gameObject.GetComponent<GUITransition>().StartTransition(1, 0, 1, 1, false);

			stateChangeTime = Time.unscaledTime;

			// Hide the map's selection squares.
			MapView.instance.HideSelectionSquares();

			menu_is_active = false;
		}

		void Update()
		{	
        if ((Input.touchCount > 0) || Input.GetMouseButton(0))
        {
            // Do not deactivate the context menu if the current press is within the menu itself.
            if (!((Input.GetMouseButton(0) && (RectTransformUtility.RectangleContainsScreenPoint(contextMenuRectTransform, new Vector2(Input.mousePosition.x,Input.mousePosition.y)))) ||
                 ((Input.touchCount == 1) && (RectTransformUtility.RectangleContainsScreenPoint(contextMenuRectTransform, Input.touches[0].position)))))
            {
                // The current press is not within the menu. If the initial press that activated the menu was released already, and it's been enough time since the menu was activated, deactivate it.
                if (press_released_after_activation && ((Time.unscaledTime - stateChangeTime) > 0.5f)) {
						Deactivate();
					}
            }
        }
        else
        {
            // Record that the initial press that opened this menu has been released since the menu was activated.
            press_released_after_activation = true;
        }

			// If the block has just become unlocked, update the menu.
			if ((blockEndTime != 0f) && (blockEndTime <=Time.unscaledTime))
			{
				blockEndTime = 0f;
				BlockModified(blockX, blockZ);
			}
		}

		public bool Setup(int _blockX, int _blockZ, bool _press_and_hold)
		{
			bool valid = true;

			blockX = _blockX;
			blockZ = _blockZ;

			// Get the data for the block.
			BlockData block_data = mapView.GetBlockData(blockX, blockZ);

			// Record the ID of the nation at the block.
			blockNationID = block_data.nationID;

			// Determine the block's nation's name.
			if (blockNationID != -1) 
			{
				NationData nationData = gameData.nationTable[blockNationID];
				blockNationName = nationData.GetName(true);
				blockNationIncognito = nationData.GetFlag(GameData.NationFlags.INCOGNITO);
			}

			// Record whether the block is locked.
			blockEndTime = (block_data.locked_until <= Time.unscaledTime) ? 0f : block_data.locked_until;

			Debug.Log("Context menu setup for block " + _blockX + "," + _blockZ + " blockNationID: " + blockNationID + ", objectID: " + block_data.objectID);

			// Determine whether this block displays a limit boundary.
			if (GameData.instance.mapMode == GameData.MapMode.MAINLAND)
			{
				if (_blockX == GameData.instance.map_position_limit) {
					limit_type = GameData.LimitType.LimitWestern;
				}
				else if (_blockX == GameData.instance.map_position_limit_next_level) {
					limit_type = GameData.LimitType.LimitWesternNextLevel;
				}
				else if (_blockX == GameData.instance.map_position_eastern_limit) {
					limit_type = GameData.LimitType.LimitEastern;
				}
				else if ((_blockZ == GameData.instance.newPlayerAreaBoundary) && (!GameData.instance.nationIsVeteran) && (!GameData.instance.userIsVeteran)) {
					limit_type = GameData.LimitType.LimitNewArea;
				}
				else if ((_blockZ == GameData.instance.newPlayerAreaBoundary) && ((GameData.instance.nationIsVeteran) || (GameData.instance.userIsVeteran))) {
					limit_type = GameData.LimitType.LimitVetArea;
				}
				else if (((_blockX == MapView.instance.mainlandMaxExtentX0) || (_blockX == MapView.instance.mainlandMaxExtentX1)) && (_blockZ >= MapView.instance.mainlandMaxExtentZ0) && (_blockZ <= MapView.instance.mainlandMaxExtentZ1)) {
					limit_type = GameData.LimitType.LimitExtent;
				}
				else if (((_blockZ == MapView.instance.mainlandMaxExtentZ0) || (_blockZ == MapView.instance.mainlandMaxExtentZ1)) && (_blockX >= MapView.instance.mainlandMaxExtentX0) && (_blockX <= MapView.instance.mainlandMaxExtentX1)) {
					limit_type = GameData.LimitType.LimitExtent;
				}
				else {
					limit_type = GameData.LimitType.Undef;
				}
			}
			else
			{
				limit_type = GameData.LimitType.Undef;
			}

			// Determine whether info is available for an object in this block.
			objectInfoID = -1;
			if (block_data.objectID != -1)
			{
				if (((block_data.objectID < ObjectData.RESOURCE_OBJECT_BASE_ID) && ((blockNationID == gameData.nationID) || (block_data.invisible_time == -1) || (block_data.invisible_time > Time.time))) ||
					(block_data.objectID >= ObjectData.RESOURCE_OBJECT_BASE_ID))
				{
					objectInfoID = block_data.objectID;
				}
			}

			if (_press_and_hold)
			{
				bool block_can_be_occupied = MapView.instance.BlockCanBeOccupied(_blockX, _blockZ, true);
				bool block_can_be_flagged = MapView.instance.BlockCanBeOccupied(_blockX, _blockZ, false);

				bool eligible_for_evac_area = MapView.instance.IsEligibleForAutoProcess(MapView.AutoProcessType.EVACUATE, _blockX, _blockZ);
				bool eligible_for_occupy_area = MapView.instance.IsEligibleForAutoProcess(MapView.AutoProcessType.OCCUPY, _blockX, _blockZ);
				bool eligible_for_move_to = MapView.instance.IsEligibleForAutoProcess(MapView.AutoProcessType.MOVE_TO, _blockX, _blockZ);
				bool eligible_for_flag = block_can_be_flagged;
				bool eligible_for_splash_attack = (GameData.instance.splashDamage > 0) && block_can_be_occupied && (blockNationID != GameData.instance.nationID) && (blockNationID != -1) && MapView.instance.IsBlockAdjacentToNation(_blockX, _blockZ);

				if ((!eligible_for_evac_area) && (!eligible_for_occupy_area) && (!eligible_for_move_to) && (!eligible_for_flag) && (!eligible_for_splash_attack)) 
				{
					if (isActiveAndEnabled) {
						Deactivate();
					}

					return false;
				}

				// Deactivate buttons used for non-press-and-hold version of menu.
				salvageButton.gameObject.SetActive(false);
				evacuateButton.gameObject.SetActive(false);
				allianceButton.gameObject.SetActive(false);
				buildButton.gameObject.SetActive(false);
				buildPreviousButton.gameObject.SetActive(false);
				upgradeButton.gameObject.SetActive(false);
				completeButton.gameObject.SetActive(false);
				chatListButton.gameObject.SetActive(false);

				if (eligible_for_evac_area)
				{
					evacuateAreaButton.gameObject.SetActive(true);
					((ContextMenuButton)(evacuateAreaButton.gameObject.GetComponent<ContextMenuButton>())).Init();
				}
				else 
				{
					evacuateAreaButton.gameObject.SetActive(false);
				}

				if (eligible_for_occupy_area)
				{
					occupyAreaButton.gameObject.SetActive(true);
					((ContextMenuButton)(occupyAreaButton.gameObject.GetComponent<ContextMenuButton>())).Init();
				}
				else 
				{
					occupyAreaButton.gameObject.SetActive(false);
				}

				if (eligible_for_move_to)
				{
					moveToButton.gameObject.SetActive(true);
					((ContextMenuButton)(moveToButton.gameObject.GetComponent<ContextMenuButton>())).Init();
				}
				else 
				{
					moveToButton.gameObject.SetActive(false);
				}

				if (eligible_for_flag)
				{
					// Set the flag button's text
					flagButton.gameObject.transform.GetChild(0).GetComponent<TMPro.TextMeshProUGUI>().text = 
								LocalizationManager.GetTranslation("map_context_flag") + " " + MapView.instance.GetMapLocationText(_blockX, _blockZ, false);

					flagButton.gameObject.SetActive(true);
					((ContextMenuButton)(flagButton.gameObject.GetComponent<ContextMenuButton>())).Init();
				}
				else 
				{
					flagButton.gameObject.SetActive(false);
				}

				if (eligible_for_splash_attack)
				{
					splashAttackButton.gameObject.SetActive(true);
					splashAttackButton.enabled = (blockEndTime <= Time.unscaledTime);
					((ContextMenuButton)(splashAttackButton.gameObject.GetComponent<ContextMenuButton>())).Init();
				}
				else 
				{
					splashAttackButton.gameObject.SetActive(false);
				}
			}
			else 
			{
				if ((blockNationID != gameData.nationID) && (mapView.IsBlockAdjacentToNation(_blockX, _blockZ) || (block_data.nationID == -1)) && (block_data.objectID < ObjectData.RESOURCE_OBJECT_BASE_ID) && (limit_type == GameData.LimitType.Undef))
				{
					if (isActiveAndEnabled) {
						Deactivate();
					}

					return false;
				}

				// Deactivate buttons used for press-and-hold version of menu.
				evacuateAreaButton.gameObject.SetActive(false);
				occupyAreaButton.gameObject.SetActive(false);
				moveToButton.gameObject.SetActive(false);
				flagButton.gameObject.SetActive(false);
				splashAttackButton.gameObject.SetActive(false);

				// Determine which buttons should be active and enabled.
				if (blockNationID != gameData.nationID)
				{
					evacuateButton.gameObject.SetActive(false);
					buildButton.gameObject.SetActive(false);
					buildPreviousButton.gameObject.SetActive(false);
					upgradeButton.gameObject.SetActive(false);
					completeButton.gameObject.SetActive(false);
					salvageButton.gameObject.SetActive(false);

					if ((blockNationID == -1) || blockNationIncognito)
					{
						chatListButton.gameObject.SetActive(false);
						allianceButton.gameObject.SetActive(false);
					}
					else
					{
						chatListButton.gameObject.SetActive(true);
						((ContextMenuButton)(chatListButton.gameObject.GetComponent<ContextMenuButton>())).Init();

						allianceButton.gameObject.SetActive(true);
						((ContextMenuButton)(allianceButton.gameObject.GetComponent<ContextMenuButton>())).Init();

						//Debug.Log("block " + _blockX + "," + _blockZ + " blockNationID: " + blockNationID + " blockNationName: " + blockNationName);

						// Determine whether the block nation is in the player's nation's chat list.
						blockNationIsInChatList = Chat.instance.IsNationInChatList(blockNationID);

						// GB-Localization
						// Set text of chat list button depending on whether the block nation is in the chat list.
						if (blockNationIsInChatList)
						{
							// "Remove " + blockNationName + " from Chat List"
							chatListButton.gameObject.transform.GetChild(0).GetComponent<TMPro.TextMeshProUGUI>().text = 
								LocalizationManager.GetTranslation("remove_name_from_chat_list").Replace("{[NATION_NAME]}", blockNationName);
						}
						else
						{
							// "Add " + blockNationName + " to Chat List"
							chatListButton.gameObject.transform.GetChild(0).GetComponent<TMPro.TextMeshProUGUI>().text =
								LocalizationManager.GetTranslation("add_name_to_chat_list").Replace("{[NATION_NAME]}", blockNationName); ;
						}

						// Determine whether an alliance invitation to the block nation is currently pending.
						blockNationWasSentAllianceInvitation = GameData.instance.NationIsInAllianceList(GameData.instance.outgoingAllyRequestsList, blockNationID);
						blockNationIsAlly = GameData.instance.NationIsInAllianceList(GameData.instance.alliesList, blockNationID);

						// GB-Localization
						// Set text of alliance button depending on whether the block nation has an alliance invitation pending.
						if (blockNationIsAlly)
						{
							// "Break Alliance"
							allianceButton.gameObject.transform.GetChild(0).GetComponent<TMPro.TextMeshProUGUI>().text = 
								LocalizationManager.GetTranslation("Chat Context/break_alliance");
						}
						else if (blockNationWasSentAllianceInvitation)
						{
							// "Withdraw Alliance Invitation"
							allianceButton.gameObject.transform.GetChild(0).GetComponent<TMPro.TextMeshProUGUI>().text =
								LocalizationManager.GetTranslation("Chat Context/withdraw_ally_invitation");
						}
						else
						{
							// "Send Alliance Invitation"
							allianceButton.gameObject.transform.GetChild(0).GetComponent<TMPro.TextMeshProUGUI>().text =
								LocalizationManager.GetTranslation("Chat Context/send_ally_invitation");
						}
					}
				}
				else
				{
					bool salvagable = (block_data.objectID != -1) && (block_data.owner_nationID == GameData.instance.nationID);

					if (salvagable)
					{
						salvageButton.gameObject.SetActive(true);
						salvageButton.enabled = (blockEndTime <= Time.unscaledTime);
						((ContextMenuButton)(salvageButton.gameObject.GetComponent<ContextMenuButton>())).Init();
					}
					else 
					{
						salvageButton.gameObject.SetActive(false);
					}

					// GB-Localization
					// "Salvage & Evacuate", "Evacuate"
					evacuateButton.gameObject.SetActive(true);
					evacuateButton.enabled = (blockEndTime <= Time.unscaledTime);
					evacuateButton.gameObject.transform.GetChild(0).GetComponent<TMPro.TextMeshProUGUI>().text = 
						(salvagable ? LocalizationManager.GetTranslation("command_salvage_evacuate") : LocalizationManager.GetTranslation("command_evacuate"));
					((ContextMenuButton)(evacuateButton.gameObject.GetComponent<ContextMenuButton>())).Init();

					bool buildable = (block_data.objectID == -1);
					bool prev_buildable = (BuildMenu.instance.prevBuildID == -1) ? false : (buildable && (GameData.instance.IsBuildAvailable(BuildMenu.instance.prevBuildID)));
					BuildData prev_build_data = (prev_buildable == false) ? null : BuildData.GetBuildData(BuildMenu.instance.prevBuildID);

					if (prev_buildable)
					{
						// If the nation already has the maximum number of the previous build, do not allow another to be built.
						if ((prev_build_data.max_count != -1) && GameData.instance.builds.ContainsKey(prev_build_data.ID) && (GameData.instance.builds[prev_build_data.ID] >= prev_build_data.max_count)) {
							prev_buildable = false;
						}

						// If the build object cannot be built on the map currently being viewed, do not allow another to be built.
						if (((GameData.instance.mapMode == GameData.MapMode.MAINLAND) && ((prev_build_data.land & BuildData.LAND_FLAG_MAINLAND) == 0)) ||
							((GameData.instance.mapMode == GameData.MapMode.HOMELAND) && ((prev_build_data.land & BuildData.LAND_FLAG_HOMELAND) == 0)) ||
							((GameData.instance.mapMode == GameData.MapMode.RAID) && ((prev_build_data.land & BuildData.LAND_FLAG_RAID) == 0)))
						{
							prev_buildable = false;
						}
					}

					// Determine if there is a build object in this block that is eligible for an upgrade.
					if ((block_data.objectID != -1) && (block_data.owner_nationID == GameData.instance.nationID) && ((block_data.completion_time == -1) || (block_data.completion_time <= Time.time))) {
						availableUpgrade = GameData.instance.GetAvailableUpgrade(block_data.objectID);
					} else {
						availableUpgrade = -1;
					}

					// Determine whether there is a build object in this block that can be completed immediately for a cost in credits.
					if ((block_data.objectID != -1) && (block_data.owner_nationID == GameData.instance.nationID) && (block_data.completion_time != -1) && (block_data.completion_time > Time.time)) {
						completionCost = ((int)(block_data.completion_time - Time.time) * GameData.instance.completionCostPerMinute / 60) + 1;
					} else {
						completionCost = 0;
					}

					if (buildable)
					{
						buildButton.gameObject.SetActive(true);
						((ContextMenuButton)(buildButton.gameObject.GetComponent<ContextMenuButton>())).Init();
					}
					else
					{
						buildButton.gameObject.SetActive(false);
					}

					// GB-Localization
					if (prev_buildable)
					{
						// "Build"
						buildPreviousButton.gameObject.SetActive(true);
						buildPreviousButton.gameObject.transform.GetChild(0).GetComponent<TMPro.TextMeshProUGUI>().text = 
							LocalizationManager.GetTranslation("Generic Text/build_word") + " " + prev_build_data.name;
						((ContextMenuButton)(buildPreviousButton.gameObject.GetComponent<ContextMenuButton>())).Init();
					}
					else 
					{
						buildPreviousButton.gameObject.SetActive(false);
					}

					if (availableUpgrade != -1)
					{
						// "Upgrade to "
						BuildData build_data = BuildData.GetBuildData(availableUpgrade);
						upgradeButton.gameObject.SetActive(true);
						upgradeButton.gameObject.transform.GetChild(0).GetComponent<TMPro.TextMeshProUGUI>().text = 
							LocalizationManager.GetTranslation("Generic Text/upgrade_to") + " " + build_data.name;
						((ContextMenuButton)(upgradeButton.gameObject.GetComponent<ContextMenuButton>())).Init();
					}
					else 
					{
						upgradeButton.gameObject.SetActive(false);
					}

					if (completionCost != 0)
					{
						// "Complete"
						completeButton.gameObject.SetActive(true);
						completeButton.gameObject.transform.GetChild(0).GetComponent<TMPro.TextMeshProUGUI>().text = 
							LocalizationManager.GetTranslation("Generic Text/complete_word") + " (" + completionCost + "<sprite=2>)";
						((ContextMenuButton)(completeButton.gameObject.GetComponent<ContextMenuButton>())).Init();
					}
					else 
					{
						completeButton.gameObject.SetActive(false);
					}

					nationInfoButton.gameObject.SetActive(false);
					chatListButton.gameObject.SetActive(false);
					allianceButton.gameObject.SetActive(false);
				}
			}

			// Nation info button may appear in either press or press-and-hold menu.
			if ((blockNationID != gameData.nationID) && (blockNationID != -1))
			{
				nationInfoID = blockNationID;
				nationInfoButton.gameObject.transform.GetChild(0).GetComponent<TMPro.TextMeshProUGUI>().text = blockNationName + " <sprite=3>";
				nationInfoButton.gameObject.SetActive(true);
				((ContextMenuButton)(nationInfoButton.gameObject.GetComponent<ContextMenuButton>())).Init();
			}
			else 
			{
				nationInfoButton.gameObject.SetActive(false);
			}

			// Object info button may appear in either press or press-and-hold menu.
			if (objectInfoID != -1)
			{
				objectInfoButton.gameObject.transform.GetChild(0).GetComponent<TMPro.TextMeshProUGUI>().text = ((objectInfoID < ObjectData.RESOURCE_OBJECT_BASE_ID) ? BuildData.GetBuildData(objectInfoID).name : ObjectData.GetObjectData(objectInfoID).name) + " <sprite=3>";
				objectInfoButton.gameObject.SetActive(true);
				((ContextMenuButton)(objectInfoButton.gameObject.GetComponent<ContextMenuButton>())).Init();
			}
			else if (limit_type != GameData.LimitType.Undef)
			{
				// GB-Localization
				// "Western Boundary", "Western Boundary Next Level", "Eastern Boundary"
				objectInfoButton.gameObject.transform.GetChild(0).GetComponent<TMPro.TextMeshProUGUI>().text =
					((limit_type == GameData.LimitType.LimitWestern) ? LocalizationManager.GetTranslation("western_boundary") :
					((limit_type == GameData.LimitType.LimitWesternNextLevel) ? LocalizationManager.GetTranslation("western_boundary_next_level") :
					((limit_type == GameData.LimitType.LimitEastern) ? LocalizationManager.GetTranslation("eastern_boundary") :
					((limit_type == GameData.LimitType.LimitExtent) ? LocalizationManager.GetTranslation("range_boundary") :
					LocalizationManager.GetTranslation("veteran_area_boundary"))))) + " <sprite=3>";
				objectInfoButton.gameObject.SetActive(true);
				((ContextMenuButton)(objectInfoButton.gameObject.GetComponent<ContextMenuButton>())).Init();
			}
			else
			{
				objectInfoButton.gameObject.SetActive(false);
			}

			// Determine where the context menu should appear on the screen.

			Direction placement_dir;

			int screen_width = Screen.width;
			int screen_height = Screen.height;

			Vector3 block_screen_pos = mapView.GetBlockCenterScreenPos(blockX, blockZ);

			int y_space_above = screen_height - (int)(block_screen_pos.y);
			int y_space_below = (int)(block_screen_pos.y);
			int y_space_beside = (int)(Mathf.Min(y_space_above, y_space_below) * 2);

			int x_space_right = screen_width - (int)(block_screen_pos.x);
			int x_space_left = (int)(block_screen_pos.x);
			int x_space_beside = (int)(Mathf.Min(x_space_left, x_space_right) * 2);

			if ((y_space_above >= y_space_beside) && (y_space_above >= y_space_below))
			{
				// Place the context menu above the block.

				if ((x_space_right >= x_space_beside) && (x_space_right >= x_space_left))
				{
					// Place the context menu to the upper right of the block.
					placement_dir = Direction.UpperRight;
				}
				else if ((x_space_left >= x_space_beside) && (x_space_left >= x_space_right))
				{
					// Place the context menu to the upper left of the block.
					placement_dir = Direction.UpperLeft;
				}
				else
				{
					// Place the context menu directly above the block.
					placement_dir = Direction.Up;
				}
			}
			else if ((y_space_below >= y_space_beside) && (y_space_below >= y_space_above))
			{
				// Place the context menu below the block.

				if ((x_space_right >= x_space_beside) && (x_space_right >= x_space_left))
				{
					// Place the context menu to the lower right of the block.
					placement_dir = Direction.LowerRight;
				}
				else if ((x_space_left >= x_space_beside) && (x_space_left >= x_space_right))
				{
					// Place the context menu to the lower left of the block.
					placement_dir = Direction.LowerLeft;
				}
				else
				{
					// Place the context menu directly below the block.
					placement_dir = Direction.Down;
				}
			}
			else
			{
				// Place the context menu beside the block.

				if (x_space_right >= x_space_left)
				{
					// Place the context menu to the right of the block.
					placement_dir = Direction.Right;
				}
				else
				{
					// Place the context menu to the left of the block.
					placement_dir = Direction.Left;
				}
			}

			switch (placement_dir)
			{
				case Direction.Up:
					contextMenuRectTransform.pivot = new Vector2(0.5f, 0.0f);
					contextMenuRectTransform.position = block_screen_pos + new Vector3(0, SIDE_OFFSET_Y * MapView.instance.canvas.scaleFactor, 0);
					arrow = arrowDown;
					arrowDown.position = block_screen_pos;
					break;
				case Direction.UpperRight:
					contextMenuRectTransform.pivot = new Vector2(0.0f, 0.0f);
					contextMenuRectTransform.position = block_screen_pos + new Vector3(CORNER_OFFSET_X * MapView.instance.canvas.scaleFactor, CORNER_OFFSET_Y * MapView.instance.canvas.scaleFactor, 0);
					arrow = arrowDownLeft;
					arrowDownLeft.position = block_screen_pos;
					break;
				case Direction.Right:
					contextMenuRectTransform.pivot = new Vector2(0.0f, 0.5f);
					contextMenuRectTransform.position = block_screen_pos + new Vector3(SIDE_OFFSET_X * MapView.instance.canvas.scaleFactor, 0, 0);
					arrow = arrowLeft;
					arrowLeft.position = block_screen_pos;
					break;
				case Direction.LowerRight:
					contextMenuRectTransform.pivot = new Vector2(0.0f, 1.0f);
					contextMenuRectTransform.position = block_screen_pos + new Vector3(CORNER_OFFSET_X * MapView.instance.canvas.scaleFactor, -CORNER_OFFSET_Y * MapView.instance.canvas.scaleFactor, 0);
					arrow = arrowUpLeft;
					arrowUpLeft.position = block_screen_pos;
					break;
				case Direction.Down:
					contextMenuRectTransform.pivot = new Vector2(0.5f, 1.0f);
					contextMenuRectTransform.position = block_screen_pos + new Vector3(0, -SIDE_OFFSET_Y * MapView.instance.canvas.scaleFactor, 0);
					arrow = arrowUp;
					arrowUp.position = block_screen_pos;
					break;
				case Direction.LowerLeft:
					contextMenuRectTransform.pivot = new Vector2(1.0f, 1.0f);
					contextMenuRectTransform.position = block_screen_pos + new Vector3(-CORNER_OFFSET_X * MapView.instance.canvas.scaleFactor, -CORNER_OFFSET_Y * MapView.instance.canvas.scaleFactor, 0);
					arrow = arrowUpRight;
					arrowUpRight.position = block_screen_pos;
					break;
				case Direction.Left:
					contextMenuRectTransform.pivot = new Vector2(1.0f, 0.5f);
					contextMenuRectTransform.position = block_screen_pos + new Vector3(-SIDE_OFFSET_X * MapView.instance.canvas.scaleFactor, 0, 0);
					arrow = arrowRight;
					arrowRight.position = block_screen_pos;
					break;
				case Direction.UpperLeft:
					contextMenuRectTransform.pivot = new Vector2(1.0f, 0.0f);
					contextMenuRectTransform.position = block_screen_pos + new Vector3(-CORNER_OFFSET_X * MapView.instance.canvas.scaleFactor, CORNER_OFFSET_Y * MapView.instance.canvas.scaleFactor, 0);
					arrow = arrowDownRight;
					arrowDownRight.position = block_screen_pos;
					break;
			}

			arrowUp.gameObject.SetActive(placement_dir == Direction.Down);
			arrowDown.gameObject.SetActive(placement_dir == Direction.Up);
			arrowLeft.gameObject.SetActive(placement_dir == Direction.Right);
			arrowRight.gameObject.SetActive(placement_dir == Direction.Left);
			arrowUpLeft.gameObject.SetActive(placement_dir == Direction.LowerRight);
			arrowDownLeft.gameObject.SetActive(placement_dir == Direction.UpperRight);
			arrowUpRight.gameObject.SetActive(placement_dir == Direction.LowerLeft);
			arrowDownRight.gameObject.SetActive(placement_dir == Direction.UpperLeft);

			//arrow.position = block_screen_pos;

			//contextMenuRectTransform.position = block_screen_pos;

			// Turn on the map's selection squares if appropriate.
			if (valid)
			{
				MapView.instance.ShowSmallSelectionSquare(_blockX, _blockZ, _blockX, _blockZ);

				if ((evacuateAreaButton.gameObject.activeSelf) || (occupyAreaButton.gameObject.activeSelf))  {
					MapView.instance.ShowLargeSelectionSquare(_blockX - 2, _blockZ- 2, _blockX + 2, _blockZ + 2);
				}
			}

			return valid;
		}

		public void BlockModified(int _x, int _z)
		{
			// If the context menu is active for the given block, set up the menu from scratch again.
			if (isActiveAndEnabled && menu_is_active && (_x == blockX) && (_z == blockZ)) {
				Setup(blockX, blockZ, press_and_hold);
			}
		}

		public void OnClick_Salvage()
		{
			// Send salvage event to the server.
			Network.instance.SendCommand("action=salvage|x=" + blockX + "|y=" + blockZ);
		
			// Hide the context menu
			Deactivate();
		}

		public void OnClick_Evacuate()
		{
			Evacuate(blockX, blockZ);

			// Hide the context menu
			Deactivate();
		}

		public void Evacuate(int _blockX, int _blockZ)
		{
			// If the number of processes (including attacks) is less than the maximum allowed...
			if ((DisplayAttack.GetNumActive() + DisplayProcess.GetNumActive()) < GameData.instance.maxSimultaneousProcesses)
			{
				// Send evacuate event to the server.
				Network.instance.SendCommand("action=evacuate|x=" + _blockX + "|y=" + _blockZ);

				// Record that this user event has occurred
				GameData.instance.UserEventOccurred(GameData.UserEventType.EVACUATE);
			}
		}

		public void OnClick_Build()
		{
			// Hide the context menu
			Deactivate();

			// Show the build menu
			BuildMenu.instance.Activate(blockX, blockZ);
		}

		public void OnClick_BuildPrevious()
		{
			// Build the previously built structure.
			Build(BuildMenu.instance.prevBuildID, blockX, blockZ);

			// Hide the context menu
			Deactivate();
		}

		public void Build(int _buildID, int _blockX, int _blockZ)
		{
			BlockData block_data = MapView.instance.GetBlockData(_blockX, _blockZ);

			// This should be prevented by earlier checks, but make sure no command is sent to server to build where there is already an object.
			if ((block_data == null) || (block_data.objectID != -1)) {
				return;
			}

			if (!GameData.instance.FealtyPreventsAction())
			{
				// Send build event to the server.
				Network.instance.SendCommand("action=build|buildID=" + _buildID + "|x=" + _blockX + "|y=" + _blockZ);

				// Record that this user event has occurred
				GameData.instance.UserEventOccurred(GameData.UserEventType.BUILD);

				// Tell the tutorial system about the command to build
				Tutorial.instance.BuildCommandSent(_buildID);
			}
		}

		public void OnClick_Upgrade()
		{
			// Send upgrade event to the server.
			Upgrade(availableUpgrade, blockX, blockZ);

			// Hide the context menu
			Deactivate();
		}

		public void Upgrade(int _buildID, int _blockX, int _blockZ)
		{
			if (!GameData.instance.FealtyPreventsAction())
			{
				// Send upgrade event to the server.
				Network.instance.SendCommand("action=upgrade|buildID=" + _buildID + "|x=" + _blockX + "|y=" + _blockZ);

				// Record that this user event has occurred
				GameData.instance.UserEventOccurred(GameData.UserEventType.BUILD);
			}
		}

		public void OnClick_Complete()
		{
			if (completionCost > GameData.instance.credits)
			{
				// Open window asking if player wants to buy credits.
				GameGUI.instance.RequestBuyCredits();

				// Hide the context menu
				Deactivate();
			}
			else 
			{
				// Send complete event to the server.
				Network.instance.SendCommand("action=complete|x=" + blockX + "|y=" + blockZ);

				// Hide the context menu
				Deactivate();
			}
		}

		public void OnClick_ChatListButton()
		{
			if (blockNationIsInChatList == false)
			{
				// Send chat_list_add event to the server.
				Network.instance.SendCommand("action=chat_list_add|nationID=" + gameData.nationID + "|addedNationID=" + blockNationID);
			}
			else
			{
				// Send chat_list_remove event to the server.
				Network.instance.SendCommand("action=chat_list_remove|nationID=" + gameData.nationID + "|removedNationID=" + blockNationID);
			}

			// Hide the context menu
			Deactivate();
		}

		public void OnClick_AllianceInvitationButton()
		{
			string message = "";
			string yes = LocalizationManager.GetTranslation("Generic Text/yes_word");
			string no = LocalizationManager.GetTranslation("Generic Text/no_word");

			// GB-Localization
			if (blockNationIsAlly)
			{
				// "Break " + GameData.instance.nationName + "'s alliance with " + blockNationName + "?"
				// "Break {[NATION_NAME]}'s alliance with {[NATION_NAME2]}?"
				message = LocalizationManager.GetTranslation("Chat Context/confirm_break_alliance")
					.Replace("{[NATION_NAME]}", GameData.instance.nationName)
					.Replace("{[NATION_NAME2]}", blockNationName);

				Requestor.Activate((int)RequestorTask.BreakAlliance, blockNationID, this, message, yes, no);
			}
			else if (blockNationWasSentAllianceInvitation)
			{
				// "Withdraw the alliance invitation that was sent to " + blockNationName + "?"
				// "Withdraw the alliance invitation that was sent to {[NATION_NAME]}?"
				message = LocalizationManager.GetTranslation("Chat Context/confirm_withdraw_ally_invitation")
					.Replace("{[NATION_NAME]}", blockNationName);

				Requestor.Activate((int)RequestorTask.WithdrawAllianceInvitation, blockNationID, this, message, yes, no);
			}
			else
			{
				// "Send an invitation to " + blockNationName + " to form an alliance with " + GameData.instance.nationName + "?"
				// "Send an invitation to {[NATION_NAME]} to form an alliance with {[NATION_NAME2]}?"
				message = LocalizationManager.GetTranslation("Chat Context/confirm_alliance_suggestion_with_other_nation")
					.Replace("{[NATION_NAME]}", blockNationName)
					.Replace("{[NATION_NAME2]}", GameData.instance.nationName);

				Requestor.Activate((int)RequestorTask.SendAllianceInvitation, blockNationID, this, message, yes, no);
			}

			// Hide the context menu
			Deactivate();
		}

		public void OnClick_NationInfoButton()
		{
			if (blockNationIncognito)
			{
				// "This nation is incognito. We can't gather any intelligence about it."
				string message = LocalizationManager.GetTranslation("nation_info_incognito");
				Requestor.Activate(0, 0, null, message, LocalizationManager.GetTranslation("Generic Text/okay"), "");
			}
			else
			{
				// Send request_nation_info event to the server.
				Network.instance.SendCommand("action=request_nation_info|targetNationID=" + nationInfoID);
			}

			// Hide the context menu
			Deactivate();
		}

		public void OnClick_ObjectInfoButton()
		{
			if (objectInfoID != -1)
			{
				if (objectInfoID < ObjectData.RESOURCE_OBJECT_BASE_ID)
				{
					// Display the build object info
					GameGUI.instance.OpenBuildInfoDialog(blockX, blockZ, objectInfoID);
				}
				else
				{
					// Display the object info
					GameGUI.instance.OpenObjectInfoDialog(blockX, blockZ, objectInfoID);
				}
			}
			else if (limit_type != GameData.LimitType.Undef)
			{
				// GB-Localization
				string message = "";
				string ok = LocalizationManager.GetTranslation("Generic Text/okay");

				if (limit_type == GameData.LimitType.LimitWestern) {
					// "Because our nation has reached level {[MY_NATION_LEVEL]}, we cannot hold territory in the primitive lands west of this line, and any territories that we did hold there have been evacuated. Greater riches can be found further east!"
					message = LocalizationManager.GetTranslation("kicking_nation_out_of_primitive_lands_notice")
								.Replace("{[MY_NATION_LEVEL]}", GameData.instance.level.ToString());
					Requestor.Activate(0, 0, null, message, ok, "");
				}
				else if (limit_type == GameData.LimitType.LimitWesternNextLevel) {
					// "When our nation advances to level {[MY_NATION_LEVEL]}, we will no longer be able to hold territory in the primitive lands west of this line, and any territory that we do hold there will be evacuated. Greater riches can be found further east!"
					message = LocalizationManager.GetTranslation("warning_nation_out_of_primitive_lands_notice")
								.Replace("{[MY_NATION_LEVEL]}", (GameData.instance.level + 1).ToString());
					Requestor.Activate(0, 0, null, message, ok, "");
				}
				else if (limit_type == GameData.LimitType.LimitEastern) {
					// "The lands east of this line are too inhospitable for our nation to inhabit with the technology we have now. Once we advance to level {[MY_NATION_LEVEL]}, we will be able to progress further east."
					message = LocalizationManager.GetTranslation("warning_nation_of_eastern_dangers")
								.Replace("{[MY_NATION_LEVEL]}", (GameData.instance.level + 1).ToString());
					Requestor.Activate(0, 0, null, message, ok, "");
				}
				else if (limit_type == GameData.LimitType.LimitNewArea) {
					// "The lands north of this line are inhabited by veteran players. Cross at your own risk."
					message = LocalizationManager.GetTranslation("new_player_area_notice");
					Requestor.Activate(0, 0, null, message, ok, "");
				}
				else if (limit_type == GameData.LimitType.LimitVetArea) {
					// "The lands south of this line are reserved for new players, and impassable to veterans."
					message = LocalizationManager.GetTranslation("veteran_area_notice");
					Requestor.Activate(0, 0, null, message, ok, "");
				}
				else if (limit_type == GameData.LimitType.LimitExtent) {
					// "A nation can be spread over a maximum range of {[MAX_EXTENT]}x{[MAX_EXTENT]} squares. To move further in this direction, you will first need to evacuate squares on the other side of your nation's range."
					message = LocalizationManager.GetTranslation("range_boundary_notice").Replace("{[MAX_EXTENT]}", string.Format("{0:n0}", MapView.instance.nationMaxExtent));
					Requestor.Activate(0, 0, null, message, ok, "");
				}
			}

			// Hide the context menu
			Deactivate();
		}

		public void OnClick_EvacuateArea()
		{
			// Hide the context menu
			Deactivate();

			// Start the automatic process of evacuating an area.
			MapView.instance.StartAutoProcess(MapView.AutoProcessType.EVACUATE, blockX, blockZ);
		}

		public void OnClick_OccupyArea()
		{
			// Hide the context menu
			Deactivate();

			// Start the automatic process of occupying an area.
			MapView.instance.StartAutoProcess(MapView.AutoProcessType.OCCUPY, blockX, blockZ);
		}

		public void OnClick_MoveTo()
		{
			// Hide the context menu
			Deactivate();

			// Start the automatic process of moving to a square.
			MapView.instance.StartAutoProcess(MapView.AutoProcessType.MOVE_TO, blockX, blockZ);
		}

		public void OnClick_Flag()
		{
			// Hide the context menu
			Deactivate();

			// Open the map flag dialog, initializing it to the title of the existing flag at this location, if there is one.
			GameGUI.instance.OpenMapFlagDialog(blockX, blockZ, MapPanel.instance.GetMapFlagText(blockX, blockZ));
		}

		public void OnClick_SplashAttack()
		{
			// Hide the context menu
			Deactivate();

			// Get the data for the block.
			BlockData block_data = mapView.GetBlockData(blockX, blockZ);

			// If the block is not currently locked...
			if ((block_data != null) && (block_data.locked_until <= Time.unscaledTime))
			{
				// If the number of processes (including attacks) is less than the maximum allowed...
				if ((DisplayAttack.GetNumActive() + DisplayProcess.GetNumActive()) < GameData.instance.maxSimultaneousProcesses)
				{
					if (!GameData.instance.FealtyPreventsAction())
					{
						// Send a mapclick event to the server, with the splash flag on.
						Network.instance.SendCommand("action=mapclick|x=" + blockX + "|y=" + blockZ + "|splash=1");
					}
				}
			}
		}

		public void RequestorResponse(int _task, int _data, Requestor.RequestorButton _result)
		{
			if (_result == Requestor.RequestorButton.LeftButton)
			{
				if ((RequestorTask)_task == RequestorTask.BreakAlliance)
				{
					// Send event_break_alliance event to the server.
					Network.instance.SendCommand("action=event_break_alliance|targetNationID=" + _data);
				}
				else if ((RequestorTask)_task == RequestorTask.SendAllianceInvitation)
				{
					// Send event_request_alliance event to the server.
					Network.instance.SendCommand("action=event_request_alliance|targetNationID=" + _data);
				}
				else if ((RequestorTask)_task == RequestorTask.WithdrawAllianceInvitation)
				{
					// Send event_withdraw_alliance event to the server.
					Network.instance.SendCommand("action=event_withdraw_alliance|targetNationID=" + _data);
				}
			}
		}
	}
}