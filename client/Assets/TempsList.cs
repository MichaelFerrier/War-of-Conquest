using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class TempsList : MonoBehaviour
{
    public GameObject tempIconPrefab;

    public void Clear()
    {
        // Remove all child TempIcons
        while (gameObject.transform.childCount > 0)
        {
            GameObject curChild = gameObject.transform.GetChild(0).gameObject;
            curChild.transform.SetParent(null);
            GameObject.Destroy(curChild);
        }
    }

    public void AddTechnology(int _techID, float _expireTime)
    {
        // Instantiate a TempIcon to represent the given tech
        GameObject tempIconObject = (GameObject) Instantiate(tempIconPrefab,  new Vector3(0f, 0f, 0f), Quaternion.identity);
        tempIconObject.transform.SetParent(gameObject.transform);
        tempIconObject.transform.localScale = new Vector3(1, 1, 1);
        tempIconObject.transform.SetAsLastSibling();

        // Position the new TempIcon in the list, ordered from soonest to latest to expire.
        foreach(Transform child in transform)
        {
            if (child.gameObject.GetComponent<TempIcon>().expireTime > _expireTime)
            {
                tempIconObject.transform.SetSiblingIndex(child.GetSiblingIndex());
                break;
            }
        }

        // Initialize the TempIcon
        tempIconObject.GetComponent<TempIcon>().Init(_techID, _expireTime);
    }

    public void RemoveTechnology(int _techID)
    {
        foreach(Transform child in transform)
        {
            if (child.gameObject.GetComponent<TempIcon>().techID == _techID)
            {
                child.gameObject.GetComponent<TempIcon>().Remove();
                break;
            }
        }
    }

    public void UpdateFillLevels()
    {
        foreach(Transform child in transform)
        {
            child.gameObject.GetComponent<TempIcon>().UpdateFillLevel();
        }
    }
}
