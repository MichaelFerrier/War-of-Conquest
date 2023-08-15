using UnityEngine;
using System.Collections;

public class DjinnPortal : MonoBehaviour
{
    public GameObject portal = null;
    public GameObject genie = null;
    public RocketGun gun = null;

    public Vector3 genie_start_pos = new Vector3(0,12,0);
    public Vector3 genie_fly_pos = new Vector3(0,22,0);

	// Use this for initialization
	void Start ()
    {
        // Get a pointer to this object's genie and portal child objects.
        genie = gameObject.transform.GetChild(0).gameObject;
        portal = gameObject.transform.GetChild(1).gameObject;
        gun = genie.transform.GetChild(0).gameObject.GetComponent<RocketGun>();

        // Hide the genie
        genie.SetActive(false);

        // Turn off the portal's particle effect.
        SetShowPortal(false);
	}
	
    public void SetShowPortal(bool _show)
    {
        if (portal == null) 
        {
            Debug.Log("DjinnPortal SetShowPortal() called though portal is null.");
            return;
        }

        ParticleSystem.EmissionModule em = portal.GetComponent<ParticleSystem>().emission;
        em.enabled = _show;
    }

    public void SetShowGenie(bool _show)
    {
        StartCoroutine(SetShowGenie_Coroutine(_show));
    }

    public IEnumerator SetShowGenie_Coroutine(bool _show)
    {
        if (_show)
        {
            genie.SetActive(true);
            genie.transform.localPosition = genie_start_pos;
            genie.transform.localEulerAngles = new Vector3(0,180,0);
            genie.GetComponent<Animation>().Play("appear");
            StartCoroutine(MoveOverSeconds(genie, genie_fly_pos, 0.5f));

            yield return new WaitForSeconds(1f);

            genie.GetComponent<Animation>()["fly"].wrapMode = WrapMode.Loop;
            genie.GetComponent<Animation>().CrossFade("fly");
        }
        else
        {
            genie.GetComponent<Animation>().CrossFade("attack 3");
            StartCoroutine(MoveOverSeconds(genie, genie_start_pos, 0.5f));

            yield return new WaitForSeconds(0.5f);

            genie.SetActive(false);
        }
    }

    public void Aim(Vector3 _target, float _duration)
    {
        genie.GetComponent<Animation>().CrossFade("talk 2");
        StartCoroutine(RotateOverSeconds(genie, Quaternion.LookRotation(_target - gameObject.transform.position), _duration));
    }

    public void Fire(Vector3 _target)
    {
        StartCoroutine(Fire_Coroutine(_target));
    }

    public IEnumerator Fire_Coroutine(Vector3 _target)
    {
        genie.GetComponent<Animation>().CrossFade("attack 2");
        yield return new WaitForSeconds(0.8f);
        gun.Fire(_target);
    }

    public IEnumerator MoveOverSeconds(GameObject objectToMove, Vector3 end, float seconds)
     {
         float elapsedTime = 0;
         Vector3 startingPos = objectToMove.transform.localPosition;
         while (elapsedTime < seconds)
         {
             objectToMove.transform.localPosition = Vector3.Lerp(startingPos, end, (elapsedTime / seconds));
             elapsedTime += Time.deltaTime;
             yield return new WaitForEndOfFrame();
         }
         objectToMove.transform.localPosition = end;
     }

    public IEnumerator RotateOverSeconds(GameObject _objectToRotate, Quaternion _angle, float _duration)
    {
        float elapsedTime = 0;
        Quaternion startingRot = _objectToRotate.transform.rotation;
        while (elapsedTime < _duration)
        {
            _objectToRotate.transform.rotation = Quaternion.Lerp(startingRot, _angle, (elapsedTime / _duration));
            elapsedTime += Time.deltaTime;
            yield return new WaitForEndOfFrame();
        }
        _objectToRotate.transform.rotation = _angle;
    }
}
