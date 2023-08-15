using UnityEngine;
using System.Collections;

public class ParticleFXManager : MonoBehaviour
{
    public void SetEndTime(float _end_time)
    {
        StartCoroutine(HandleEndTime(_end_time));
    }

    IEnumerator HandleEndTime(float _end_time)
    {
        // Wait until this lasting wipe's end time.
        yield return new WaitForSeconds(_end_time - Time.time);
    
        // Turn off this lasting wipe's particle effect's emission.
        TurnOffEmission_Recursive(gameObject);

        // Wait a few seconds for the effect to disappear.
        yield return new WaitForSeconds(10f);

        // Destroy this lasting wipe effect.
        UnityEngine.Object.Destroy(gameObject);
    }

    public void TurnOffEmission_Recursive(GameObject _gameObject)
    {
        // Turn off the given GameObject's particle effect's emission.
        ParticleSystem ps = _gameObject.GetComponent<ParticleSystem>();
        if (ps != null)
        {
            ParticleSystem.EmissionModule em = ps.emission;
            em.enabled = false;
        }

        /*
        // Turn off legacy particle system emitter, if the object has it.
        ParticleEmitter pe = _gameObject.GetComponent<ParticleEmitter>();
        if (pe != null) {
            pe.emit = false;
        }
        */

        // Call this recursively for all child GameObjects.
        foreach(Transform child in _gameObject.transform) {
            TurnOffEmission_Recursive(child.gameObject);
        }
    }

    public void PreWarm()
    {
        PreWarm_Recursive(gameObject);
    }

    private void PreWarm_Recursive(GameObject _gameObject)
    {
        // Turn off the given GameObject's particle effect's emission.
        ParticleSystem ps = _gameObject.GetComponent<ParticleSystem>();
        if (ps != null)
        {
            ps.Simulate(10f, true);
            ps.Play(true);
        }

        // Call this recursively for all child GameObjects.
        foreach(Transform child in _gameObject.transform) {
            PreWarm_Recursive(child.gameObject);
        }
    }
}
