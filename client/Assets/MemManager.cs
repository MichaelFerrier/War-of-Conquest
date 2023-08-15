using UnityEngine;
using UnityEngine.UI;
using System.Collections;
using System.Collections.Generic;

public class MemManager : MonoBehaviour
{
    public static MemManager instance;
    public Stack<BlockProcess> blockProcessObject_FreeList = new Stack<BlockProcess>();
    public Stack<GameObject> takeLandParticleObject_FreeList = new Stack<GameObject>();
    public Stack<GameObject> chatTabObject_FreeList = new Stack<GameObject>();
    public Stack<GameObject> chatMessageObject_FreeList = new Stack<GameObject>();
    public Stack<GameObject> chatNameObject_FreeList = new Stack<GameObject>();
    public Stack<GameObject> chatNameXObject_FreeList = new Stack<GameObject>();
    public Stack<GameObject> memberEntryObject_FreeList = new Stack<GameObject>();
    public Stack<GameObject> allyEntryObject_FreeList = new Stack<GameObject>();
    public Stack<GameObject> messageEntryObject_FreeList = new Stack<GameObject>();
    public Stack<GameObject> tierEntryObject_FreeList = new Stack<GameObject>();
    public Stack<GameObject> questEntryObject_FreeList = new Stack<GameObject>();
    public Stack<GameObject> raidEntryObject_FreeList = new Stack<GameObject>();
    public Stack<GameObject> buildIconObject_FreeList = new Stack<GameObject>();
    public Stack<GameObject> nationLabelObject_FreeList = new Stack<GameObject>();
    public Stack<GameObject> mapTextObject_FreeList = new Stack<GameObject>();
    public Stack<GameObject> surroundCountObject_FreeList = new Stack<GameObject>();
    public Stack<GameObject> displayAttackObject_FreeList = new Stack<GameObject>();
    public Stack<GameObject> displayProcessObject_FreeList = new Stack<GameObject>();
    public Stack<GameObject> displayHitPointsObject_FreeList = new Stack<GameObject>();
    public Stack<GameObject> displayTimerObject_FreeList = new Stack<GameObject>();
    public Stack<GameObject> advanceIconGlowObject_FreeList = new Stack<GameObject>();
    public Stack<GameObject> animTextObject_FreeList = new Stack<GameObject>();
    public Stack<GameObject> flyTextObject_FreeList = new Stack<GameObject>();
    public Stack<GameObject> mapMarkerObject_FreeList = new Stack<GameObject>();
    public Stack<GameObject> mapFlagLineObject_FreeList = new Stack<GameObject>();
    public Stack<GameObject> resourceLineObject_FreeList = new Stack<GameObject>();
    public Stack<GameObject> resourceLocLineObject_FreeList = new Stack<GameObject>();
    public Stack<GameObject> storageMeterObject_FreeList = new Stack<GameObject>();
    public Stack<GameObject> triggerInertParticleObject_FreeList = new Stack<GameObject>();

    public Stack<GameObject> westernLimitObject_FreeList = new Stack<GameObject>();
    public Stack<GameObject> westernLimitNextLevelObject_FreeList = new Stack<GameObject>();
    public Stack<GameObject> easternLimitObject_FreeList = new Stack<GameObject>();
    public Stack<GameObject> vetAreaLimitObject_FreeList = new Stack<GameObject>();
    public Stack<GameObject> newAreaLimitObject_FreeList = new Stack<GameObject>();
    public Stack<GameObject> extentLimitObject_FreeList = new Stack<GameObject>();

    public GameObject takeLandParticlePrefab, chatTabPrefab, chatMessagePrefab, chatNamePrefab, chatNameXPrefab, memberEntryPrefab, allyEntryPrefab, 
        messageEntryPrefab, tierEntryPrefab, questEntryPrefab, raidEntryPrefab, buildIconPrefab, nationLabelPrefab, mapTextPrefab, surroundCountPrefab, displayAttackPrefab, 
        displayProcessPrefab, displayHitPointsPrefab, displayTimerPrefab, advanceIconGlowPrefab, animTextPrefab, flyTextPrefab, mapMarkerPrefab, mapFlagLinePrefab,
        resourceLinePrefab, resourceLocLinePrefab, storageMeterPrefab, triggerInertParticlePrefab;
    public GameObject westernLimit, westernLimitNextLevel, easternLimit, vetAreaLimit, newAreaLimit, extentLimit;
    
    public MemManager()
    {
        instance = this;
    }

    public BlockProcess GetBlockProcessObject()
    {
        if (blockProcessObject_FreeList.Count == 0)
        {
            BlockProcess blockProcessObject = new BlockProcess();
            return blockProcessObject;
        }
        else
        {
            return blockProcessObject_FreeList.Pop();
        }
    }

    public void ReleaseBlockProcessObject(BlockProcess _blockProcessObject)
    {
        blockProcessObject_FreeList.Push(_blockProcessObject);
    }

    public GameObject GetTakeLandParticleObject()
    {
        if (takeLandParticleObject_FreeList.Count == 0)
        {
            GameObject takeLandParticleObject = Instantiate(takeLandParticlePrefab, new Vector3(0f, 0f, 0f), Quaternion.identity) as GameObject;
            return takeLandParticleObject;
        }
        else
        {
            return takeLandParticleObject_FreeList.Pop();
        }
    }

    public void ReleaseTakeLandParticleObject(GameObject _takeLandParticleObject)
    {
        takeLandParticleObject_FreeList.Push(_takeLandParticleObject);
    }

    public GameObject GetChatTabObject()
    {
        if (chatTabObject_FreeList.Count == 0)
        {
            GameObject chatTabObject = Instantiate(chatTabPrefab, new Vector3(0f, 0f, 0f), Quaternion.identity) as GameObject;
            return chatTabObject;
        }
        else
        {
            return chatTabObject_FreeList.Pop();
        }
    }

    public void ReleaseChatTabObject(GameObject _chatTabObject)
    {
        chatTabObject_FreeList.Push(_chatTabObject);
    }

    public GameObject GetChatMessageObject()
    {
        if (chatMessageObject_FreeList.Count == 0)
        {
            GameObject chatMessageObject = Instantiate(chatMessagePrefab, new Vector3(0f, 0f, 0f), Quaternion.identity) as GameObject;
            ChatMessage chatMessage = chatMessageObject.GetComponent<ChatMessage>();
            return chatMessageObject;
        }
        else
        {
            return chatMessageObject_FreeList.Pop();
        }
    }

    public void ReleaseChatMessageObject(GameObject _chatMessageObject)
    {
        chatMessageObject_FreeList.Push(_chatMessageObject);
    }

    public GameObject GetChatNameObject()
    {
        if (chatNameObject_FreeList.Count == 0)
        {
            GameObject chatNameObject = Instantiate(chatNamePrefab, new Vector3(0f, 0f, 0f), Quaternion.identity) as GameObject;
            return chatNameObject;
        }
        else
        {
            return chatNameObject_FreeList.Pop();
        }
    }

    public void ReleaseChatNameObject(GameObject _chatNameObject)
    {
        chatNameObject_FreeList.Push(_chatNameObject);
    }

    public GameObject GetChatNameXObject()
    {
        if (chatNameXObject_FreeList.Count == 0)
        {
            GameObject chatNameXObject = Instantiate(chatNameXPrefab, new Vector3(0f, 0f, 0f), Quaternion.identity) as GameObject;
            return chatNameXObject;
        }
        else
        {
            return chatNameXObject_FreeList.Pop();
        }
    }

    public void ReleaseChatNameXObject(GameObject _chatNameXObject)
    {
        chatNameXObject_FreeList.Push(_chatNameXObject);
    }

    public GameObject GetMemberEntryObject()
    {
        if (memberEntryObject_FreeList.Count == 0)
        {
            GameObject memberEntryObject = Instantiate(memberEntryPrefab, new Vector3(0f, 0f, 0f), Quaternion.identity) as GameObject;
            return memberEntryObject;
        }
        else
        {
            return memberEntryObject_FreeList.Pop();
        }
    }

    public void ReleaseMemberEntryObject(GameObject _memberEntryObject)
    {
        memberEntryObject_FreeList.Push(_memberEntryObject);
    }

    public GameObject GetAllyEntryObject()
    {
        if (allyEntryObject_FreeList.Count == 0)
        {
            GameObject allyEntryObject = Instantiate(allyEntryPrefab, new Vector3(0f, 0f, 0f), Quaternion.identity) as GameObject;
            return allyEntryObject;
        }
        else
        {
            return allyEntryObject_FreeList.Pop();
        }
    }

    public void ReleaseAllyEntryObject(GameObject _allyEntryObject)
    {
        allyEntryObject_FreeList.Push(_allyEntryObject);
    }

    public GameObject GetMessageEntryObject()
    {
        if (messageEntryObject_FreeList.Count == 0)
        {
            GameObject messageEntryObject = Instantiate(messageEntryPrefab, new Vector3(0f, 0f, 0f), Quaternion.identity) as GameObject;
            return messageEntryObject;
        }
        else
        {
            return messageEntryObject_FreeList.Pop();
        }
    }

    public void ReleaseMessageEntryObject(GameObject _messageEntryObject)
    {
        messageEntryObject_FreeList.Push(_messageEntryObject);
    }

    public GameObject GetTierEntryObject()
    {
        if (tierEntryObject_FreeList.Count == 0)
        {
            GameObject tierEntryObject = Instantiate(tierEntryPrefab, new Vector3(0f, 0f, 0f), Quaternion.identity) as GameObject;
            return tierEntryObject;
        }
        else
        {
            return tierEntryObject_FreeList.Pop();
        }
    }

    public void ReleaseTierEntryObject(GameObject _tierEntryObject)
    {
        tierEntryObject_FreeList.Push(_tierEntryObject);
    }

    public GameObject GetQuestEntryObject()
    {
        if (questEntryObject_FreeList.Count == 0)
        {
            GameObject questEntryObject = Instantiate(questEntryPrefab, new Vector3(0f, 0f, 0f), Quaternion.identity) as GameObject;
            return questEntryObject;
        }
        else
        {
            return questEntryObject_FreeList.Pop();
        }
    }

    public void ReleaseQuestEntryObject(GameObject _questEntryObject)
    {
        questEntryObject_FreeList.Push(_questEntryObject);
    }

    public GameObject GetRaidEntryObject()
    {
        if (raidEntryObject_FreeList.Count == 0)
        {
            GameObject raidEntryObject = Instantiate(raidEntryPrefab, new Vector3(0f, 0f, 0f), Quaternion.identity) as GameObject;
            return raidEntryObject;
        }
        else
        {
            return raidEntryObject_FreeList.Pop();
        }
    }

    public void ReleaseRaidEntryObject(GameObject _raidEntryObject)
    {
        raidEntryObject_FreeList.Push(_raidEntryObject);
    }

    public GameObject GetBuildIconObject()
    {
        if (buildIconObject_FreeList.Count == 0)
        {
            GameObject buildIconObject = Instantiate(buildIconPrefab, new Vector3(0f, 0f, 0f), Quaternion.identity) as GameObject;
            return buildIconObject;
        }
        else
        {
            return buildIconObject_FreeList.Pop();
        }
    }

    public void ReleaseBuildIconObject(GameObject _buildIconObject)
    {
        buildIconObject_FreeList.Push(_buildIconObject);
    }

    public GameObject GetNationLabelObject()
    {
        if (nationLabelObject_FreeList.Count == 0)
        {
            GameObject nationLabelObject = Instantiate(nationLabelPrefab, new Vector3(0f, 0f, 0f), Quaternion.identity) as GameObject;
            return nationLabelObject;
        }
        else
        {
            return nationLabelObject_FreeList.Pop();
        }
    }
        
    public void ReleaseNationLabelObject(GameObject _nationLabelObject)
    {
        nationLabelObject_FreeList.Push(_nationLabelObject);
    }

    public GameObject GetMapTextObject()
    {
        if (mapTextObject_FreeList.Count == 0)
        {
            GameObject mapTextObject = Instantiate(mapTextPrefab, new Vector3(0f, 0f, 0f), Quaternion.identity) as GameObject;
            return mapTextObject;
        }
        else
        {
            return mapTextObject_FreeList.Pop();
        }
    }

    public void ReleaseMapTextObject(GameObject _mapTextObject)
    {
        mapTextObject_FreeList.Push(_mapTextObject);
    }

    public GameObject GetSurroundCountObject()
    {
        if (surroundCountObject_FreeList.Count == 0)
        {
            GameObject surroundCountObject = Instantiate(surroundCountPrefab, new Vector3(0f, 0f, 0f), Quaternion.identity) as GameObject;
            return surroundCountObject;
        }
        else
        {
            return surroundCountObject_FreeList.Pop();
        }
    }

    public void ReleaseSurroundCountObject(GameObject _surroundCountObject)
    {
        if (surroundCountObject_FreeList.Contains(_surroundCountObject)) Debug.Log("ERROR: Freeing surround count that is already in free list!");
        surroundCountObject_FreeList.Push(_surroundCountObject);
    }

    public GameObject GetDisplayAttackObject()
    {
        if (displayAttackObject_FreeList.Count == 0)
        {
            GameObject displayAttackObject = Instantiate(displayAttackPrefab, new Vector3(0f, 0f, 0f), Quaternion.identity) as GameObject;
            return displayAttackObject;
        }
        else
        {
            return displayAttackObject_FreeList.Pop();
        }
    }

    public void ReleaseDisplayAttackObject(GameObject _displayAttackObject)
    {
        displayAttackObject_FreeList.Push(_displayAttackObject);
    }

    public GameObject GetDisplayProcessObject()
    {
        if (displayProcessObject_FreeList.Count == 0)
        {
            GameObject displayProcessObject = Instantiate(displayProcessPrefab, new Vector3(0f, 0f, 0f), Quaternion.identity) as GameObject;
            return displayProcessObject;
        }
        else
        {
            return displayProcessObject_FreeList.Pop();
        }
    }

    public void ReleaseDisplayProcessObject(GameObject _displayProcessObject)
    {
        displayProcessObject_FreeList.Push(_displayProcessObject);
    }

    public GameObject GetDisplayHitPointsObject()
    {
        if (displayHitPointsObject_FreeList.Count == 0)
        {
            GameObject displayHitPointsObject = Instantiate(displayHitPointsPrefab, new Vector3(0f, 0f, 0f), Quaternion.identity) as GameObject;
            return displayHitPointsObject;
        }
        else
        {
            return displayHitPointsObject_FreeList.Pop();
        }
    }

    public void ReleaseDisplayHitPointsObject(GameObject _displayHitPointsObject)
    {
        displayHitPointsObject_FreeList.Push(_displayHitPointsObject);
    }

    public GameObject GetDisplayTimerObject()
    {
        if (displayTimerObject_FreeList.Count == 0)
        {
            GameObject displayTimerObject = Instantiate(displayTimerPrefab, new Vector3(0f, 0f, 0f), Quaternion.identity) as GameObject;
            return displayTimerObject;
        }
        else
        {
            return displayTimerObject_FreeList.Pop();
        }
    }

    public void ReleaseDisplayTimerObject(GameObject _displayTimerObject)
    {
        displayTimerObject_FreeList.Push(_displayTimerObject);
    }

    public GameObject GetAdvanceIconGlowObject()
    {
        if (advanceIconGlowObject_FreeList.Count == 0)
        {
            GameObject advanceIconGlowObject = Instantiate(advanceIconGlowPrefab, new Vector3(0f, 0f, 0f), Quaternion.identity) as GameObject;
            return advanceIconGlowObject;
        }
        else
        {
            return advanceIconGlowObject_FreeList.Pop();
        }
    }

    public void ReleaseAdvanceIconGlowObject(GameObject _advanceIconGlowObject)
    {
        advanceIconGlowObject_FreeList.Push(_advanceIconGlowObject);
    }

    public GameObject GetAnimTextObject()
    {
        if (animTextObject_FreeList.Count == 0)
        {
            GameObject animTextObject = Instantiate(animTextPrefab, new Vector3(0f, 0f, 0f), Quaternion.identity) as GameObject;
            return animTextObject;
        }
        else
        {
            return animTextObject_FreeList.Pop();
        }
    }

    public void ReleaseAnimTextObject(GameObject _animTextObject)
    {
        animTextObject_FreeList.Push(_animTextObject);
    }

    public GameObject GetFlyTextObject()
    {
        if (flyTextObject_FreeList.Count == 0)
        {
            GameObject flyTextObject = Instantiate(flyTextPrefab, new Vector3(0f, 0f, 0f), Quaternion.identity) as GameObject;
            return flyTextObject;
        }
        else
        {
            return flyTextObject_FreeList.Pop();
        }
    }

    public void ReleaseFlyTextObject(GameObject _flyTextObject)
    {
        flyTextObject_FreeList.Push(_flyTextObject);
    }

    public GameObject GetMapMarkerObject()
    {
        if (mapMarkerObject_FreeList.Count == 0)
        {
            GameObject mapMarkerObject = Instantiate(mapMarkerPrefab, new Vector3(0f, 0f, 0f), Quaternion.identity) as GameObject;
            return mapMarkerObject;
        }
        else
        {
            return mapMarkerObject_FreeList.Pop();
        }
    }

    public void ReleaseMapMarkerObject(GameObject _mapMarkerObject)
    {
        mapMarkerObject_FreeList.Push(_mapMarkerObject);
    }

    public GameObject GetMapFlagLineObject()
    {
        if (mapFlagLineObject_FreeList.Count == 0)
        {
            GameObject mapFlagLineObject = Instantiate(mapFlagLinePrefab, new Vector3(0f, 0f, 0f), Quaternion.identity) as GameObject;
            return mapFlagLineObject;
        }
        else
        {
            return mapFlagLineObject_FreeList.Pop();
        }
    }

    public void ReleaseMapFlagLineObject(GameObject _mapFlagLineObject)
    {
        mapFlagLineObject_FreeList.Push(_mapFlagLineObject);
    }

    public GameObject GetResourceLineObject()
    {
        if (resourceLineObject_FreeList.Count == 0)
        {
            GameObject resourceLineObject = Instantiate(resourceLinePrefab, new Vector3(0f, 0f, 0f), Quaternion.identity) as GameObject;
            return resourceLineObject;
        }
        else
        {
            return resourceLineObject_FreeList.Pop();
        }
    }

    public void ReleaseResourceLineObject(GameObject _resourceLineObject)
    {
        resourceLineObject_FreeList.Push(_resourceLineObject);
    }

    public GameObject GetResourceLocLineObject()
    {
        if (resourceLocLineObject_FreeList.Count == 0)
        {
            GameObject resourceLocLineObject = Instantiate(resourceLocLinePrefab, new Vector3(0f, 0f, 0f), Quaternion.identity) as GameObject;
            return resourceLocLineObject;
        }
        else
        {
            return resourceLocLineObject_FreeList.Pop();
        }
    }

    public void ReleaseResourceLocLineObject(GameObject _resourceLocLineObject)
    {
        resourceLocLineObject_FreeList.Push(_resourceLocLineObject);
    }

    public GameObject GetStorageMeterObject()
    {
        if (storageMeterObject_FreeList.Count == 0)
        {
            GameObject storageMeterObject = Instantiate(storageMeterPrefab, new Vector3(0f, 0f, 0f), Quaternion.identity) as GameObject;
            return storageMeterObject;
        }
        else
        {
            return storageMeterObject_FreeList.Pop();
        }
    }

    public void ReleaseStorageMeterObject(GameObject _storageMeterObject)
    {
        storageMeterObject_FreeList.Push(_storageMeterObject);
    }

    public GameObject GetTriggerInertParticleObject()
    {
        if (triggerInertParticleObject_FreeList.Count == 0)
        {
            GameObject triggerInertParticleObject = Instantiate(triggerInertParticlePrefab, new Vector3(0f, 0f, 0f), Quaternion.identity) as GameObject;
            return triggerInertParticleObject;
        }
        else
        {
            return triggerInertParticleObject_FreeList.Pop();
        }
    }

    public void ReleaseTriggerInertParticleObject(GameObject _triggerInertParticleObject)
    {
        triggerInertParticleObject_FreeList.Push(_triggerInertParticleObject);
    }

    public GameObject GetLimitObject(GameData.LimitType _limit_type)
    {
        Stack<GameObject> freeList = null;
        GameObject prefab = null;

        switch (_limit_type)
        {
            case GameData.LimitType.LimitWestern:
                freeList = westernLimitObject_FreeList;
                prefab = westernLimit;
                break;
            case GameData.LimitType.LimitWesternNextLevel:
                freeList = westernLimitNextLevelObject_FreeList;
                prefab = westernLimitNextLevel;
                break;
            case GameData.LimitType.LimitEastern:
                freeList = easternLimitObject_FreeList;
                prefab = easternLimit;
                break;
            case GameData.LimitType.LimitNewArea:
                freeList = newAreaLimitObject_FreeList;
                prefab = newAreaLimit;
                break;
            case GameData.LimitType.LimitVetArea:
                freeList = vetAreaLimitObject_FreeList;
                prefab = vetAreaLimit;
                break;
            case GameData.LimitType.LimitExtent:
                freeList = extentLimitObject_FreeList;
                prefab = extentLimit;
                break;
        }

        if (prefab == null)
        {
            Debug.Log("No prefab for limit object type " + _limit_type);
            return null;
        }

        if (freeList.Count == 0)
        {
            GameObject limitObject = Instantiate(prefab, new Vector3(0f, 0f, 0f), Quaternion.identity) as GameObject;
            return limitObject;
        }
        else
        {
            return freeList.Pop();
        }
    }

    public void ReleaseLimitObject(GameData.LimitType _limit_type, GameObject _limitObject)
    {
        Stack<GameObject> freeList = null;

        switch (_limit_type)
        {
            case GameData.LimitType.LimitWestern:
                freeList = westernLimitObject_FreeList;
                break;
            case GameData.LimitType.LimitWesternNextLevel:
                freeList = westernLimitNextLevelObject_FreeList;
                break;
            case GameData.LimitType.LimitEastern:
                freeList = easternLimitObject_FreeList;
                break;
            case GameData.LimitType.LimitNewArea:
                freeList = newAreaLimitObject_FreeList;
                break;
            case GameData.LimitType.LimitVetArea:
                freeList = vetAreaLimitObject_FreeList;
                break;
            case GameData.LimitType.LimitExtent:
                freeList = extentLimitObject_FreeList;
                break;
        }

        freeList.Push(_limitObject);
    }
}
