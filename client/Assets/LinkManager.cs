using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class LinkManager
{
    public enum LinkType
    {
        UNDEF,
        TECH,
        BUILD,
        STAT,
        NATION,
        LOCATION
    };

    public List<int> link_ids = new List<int>();
    public List<LinkType> link_types = new List<LinkType>();
    public int num_links = 0;

    public void ResetLinks()
    {
        link_types.Clear();
        link_ids.Clear();
    }

    public int GetNumLinks()
    {
        return link_types.Count;
    }

    public void AddLink(LinkType _link_type, int _linkID)
    {
        link_types.Add(_link_type);
        link_ids.Add(_linkID);
    }
}
