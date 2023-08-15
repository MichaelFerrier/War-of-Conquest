using UnityEngine;
using UnityEngine.EventSystems;
using UnityEngine.UI;
 
public class TabSelect : MonoBehaviour {
 
    private EventSystem eventSystem;
 
    // Use this for initialization
    void Start () {
        this.eventSystem = EventSystem.current;
        //Debug.Log("TabSelect starting, this.eventSystem: " + this.eventSystem);
    }
     
    // Update is called once per frame
    void Update () {
        // When TAB is pressed, we should select the next selectable UI element
        if (Input.GetKeyDown(KeyCode.Tab)) {
            Selectable next = null;
            Selectable current = null;

            //Debug.Log("Tab pressed");
 
            // Figure out if we have a valid current selected gameobject
            if (eventSystem.currentSelectedGameObject != null) {
                // Unity doesn't seem to "deselect" an object that is made inactive
                if (eventSystem.currentSelectedGameObject.activeInHierarchy) {
                    current = eventSystem.currentSelectedGameObject.GetComponent<Selectable>();
                }
            }
             
            if (current != null) {
                //Debug.Log("Currently sel: " + current);
                // When SHIFT is held along with tab, go backwards instead of forwards
                if (Input.GetKey(KeyCode.LeftShift) || Input.GetKey(KeyCode.RightShift)) {
                    next = current.FindSelectableOnLeft();
                    if (next == null) {
                        next = current.FindSelectableOnUp();
                    }
                } else {
                    next = current.FindSelectableOnRight();
                    if (next == null) {
                        next = current.FindSelectableOnDown();
                    }
                }
            } else {
                // If there is no current selected gameobject, select the first one
                if (Selectable.allSelectables.Count > 0) {
                    next = Selectable.allSelectables[0];
                }
            }

            //Debug.Log("Next to sel: " + next);
            if (next != null)  {
                next.Select();
            }
        }
    }
}
