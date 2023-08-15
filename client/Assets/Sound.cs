using UnityEngine;
using UnityEngine.Audio;
using System.Collections;
using System.Collections.Generic;

[System.Serializable]
public class Sound : MonoBehaviour
{
    public static Sound instance;

    public AudioMixerSnapshot playSnapshot, suspendSnapshot;
    public AudioMixerGroup audioGroupMaster, audioGroup3D, audioGroup2D, audioGroupMusic, audioGroupOcean;

    public AudioSource oceanWaves;
    public AudioSource ambientAudioSource;

    public GameObject objectAudioSourcePrefab;

    public AudioClip bats1;
    public AudioClip brown_thrush1;
    public AudioClip brown_thrush2;
    public AudioClip brown_thrush3;
    public AudioClip brown_thrush4;
    public AudioClip cardinal1;
    public AudioClip chimes;
    public AudioClip cold_wind1;
    public AudioClip cold_wind2;
    public AudioClip cold_wind3;
    public AudioClip cold_wind4;
    public AudioClip crow1;
    public AudioClip crow2;
    public AudioClip hawk1;
    public AudioClip hawk2;
    public AudioClip icecrack1;
    public AudioClip nice_bird1;
    public AudioClip nice_bird2;
    public AudioClip owl1;
    public AudioClip raven1;
    public AudioClip robin;
    public AudioClip rooster;
    public AudioClip seagulls1;
    public AudioClip seagulls2;
    public AudioClip seagulls3;
    public AudioClip sparrow1;
    public AudioClip vortex1;
    public AudioClip wild_turkey_gobbles;
    public AudioClip wood_thrush1;
    public AudioClip wood_thrush2;
    public AudioClip wood_thrush3;
    public AudioClip wood_thrush4;
    public AudioClip woodpecker_pecking;

    public AudioClip geothermal_vent;
    public AudioClip ley_lines;
    public AudioClip oil_deposit;
    public AudioClip orb_of_noontide;
    public AudioClip orb_of_shadow;
    public AudioClip orb_of_destiny;
    public AudioClip orb_of_the_void;
    public AudioClip orb_of_fire;

    public AudioClip brainsweeper;
    public AudioClip electricity;
    public AudioClip fireball;
    public AudioClip teleproj;
    public AudioClip gore;
    public AudioClip walkthroughleaves;
    public AudioClip counter_attack_tanks;
    public AudioClip counter_attack_planes;
    public AudioClip counter_attack_mechs;
    public AudioClip toxic_chemical_dump;
    public AudioClip supervirus_contagion;
    public AudioClip hypnotic_inducer;
    public AudioClip zoth;
    public AudioClip slime;
    public AudioClip djinni_appear;
    public AudioClip nuclear;
    public AudioClip geo_wipe;

    public AudioClip occupy_land;
    public AudioClip evac_land;
    public AudioClip construction;
    public AudioClip salvage;
    public AudioClip capture;
    public AudioClip build_complete;
    public AudioClip appear;
    public AudioClip trigger_inert;

    public AudioClip level_up;
    public AudioClip credits;
    public AudioClip manpower;
    public AudioClip energy;
    public AudioClip energy_defecit;
    public AudioClip capture_resource;
    public AudioClip advance_permanent;
    public AudioClip advance_temp;
    public AudioClip advance_temp_expire;
    public AudioClip chat_received;
    public AudioClip message_received;
    public AudioClip quest_completed;
    public AudioClip quest_collected;
    public AudioClip tutorial_appears;
    public AudioClip tutorial_changes;
    public AudioClip star_appear;
    public AudioClip ad_bonus;

    public AudioClip explosion4;

    public AudioClip musicTitle;
    public AudioClip musicMenu;
    public AudioClip musicEnterGame;
    public AudioClip musicMajorAchievement1;
    public AudioClip musicMajorAchievement2;
    public AudioClip musicMinorAchievement;
    public AudioClip musicBattle1;
    public AudioClip musicBattle2;
    public AudioClip musicBattle3;
    public AudioClip musicExplore;
    public AudioClip musicBuild;
    public AudioClip musicAdvance;
    public AudioClip musicCredits;

    public ArrayList ambientSoundArray = new ArrayList();
    public float[] ambientProbabilities = new float[5] {0,0,0,0,0};

    private Stack<GameObject> audioSourcePool = new Stack<GameObject>();

    float beach_volume = 0f;
    float ambient_volume = 0f;
    float zoom_volume_multiplier = 0f;

    public AudioClip prevMusic = null;
    MusicFader prevMusicFader = null;
    float prevMusicEndTime = 0f;

    public const float DEFAULT_AUDIO_VOLUME = 1f;
    public const float OBJECT_AUDIO_VOLUME = 0.3f;
    public const float MIN_SECONDS_BETWEEN_MUSIC = 240f;
    public const float MIN_SECONDS_BETWEEN_MUSIC_SHORT = 120f;
    public const float MUSIC_FULL_VOLUME = 0.5f;
    public const float MUSIC_FADE_DURATION = 5f;
    public const float MUSIC_INTERRUPT_FADE_DURATION = 2f;

	public Sound()
    {
        instance = this;
	}

    void Start()
    {
        // Begin with suspend mixer snapshot
        suspendSnapshot.TransitionTo(0.01f);

        ambientSoundArray.Add(new AmbientSound(bats1, 1, 0, 25, 0, 0, 0));
        ambientSoundArray.Add(new AmbientSound(brown_thrush1, 1, 2f, 0, 0, 0, 0));
        ambientSoundArray.Add(new AmbientSound(brown_thrush2, 1, 4, 0, 0, 0, 0));
        ambientSoundArray.Add(new AmbientSound(brown_thrush3, 1, 5, 0, 0, 0, 0));
        ambientSoundArray.Add(new AmbientSound(brown_thrush4, 1, 5, 0, 0, 0, 0));
        ambientSoundArray.Add(new AmbientSound(cardinal1, 1, 0, 0, 0, 6, 0));
        ambientSoundArray.Add(new AmbientSound(chimes, 1, 0, 0, 0, 4, 0));
        ambientSoundArray.Add(new AmbientSound(cold_wind1, 1, 0, 0, 4, 0, 0));
        ambientSoundArray.Add(new AmbientSound(cold_wind2, 1, 0, 0, 6, 0, 0));
        ambientSoundArray.Add(new AmbientSound(cold_wind3, 1, 0, 0, 8, 0, 0));
        ambientSoundArray.Add(new AmbientSound(cold_wind4, 1, 0, 0, 15, 0, 0));
        ambientSoundArray.Add(new AmbientSound(crow1, 1, 10, 8, 0, 0, 35));
        ambientSoundArray.Add(new AmbientSound(crow2, 1, 35, 15, 0, 0, 0));
        ambientSoundArray.Add(new AmbientSound(hawk1, 1, 10, 8, 0, 0, 10));
        ambientSoundArray.Add(new AmbientSound(hawk2, 1, 25, 15, 0, 0, 0));
        ambientSoundArray.Add(new AmbientSound(icecrack1, 1, 0, 0, 4, 0, 0));
        ambientSoundArray.Add(new AmbientSound(nice_bird1, 1, 0, 0, 0, 2, 0));
        ambientSoundArray.Add(new AmbientSound(nice_bird2, 1, 0, 0, 0, 4, 0));
        ambientSoundArray.Add(new AmbientSound(owl1, 1, 15, 30, 0, 0, 0));
        ambientSoundArray.Add(new AmbientSound(raven1, 1, 0, 8, 0, 0, 0));
        ambientSoundArray.Add(new AmbientSound(robin, 1, 6, 0, 0, 0, 0));
        ambientSoundArray.Add(new AmbientSound(rooster, 1, 50, 0, 0, 0, 0));
        ambientSoundArray.Add(new AmbientSound(seagulls1, 1, 0, 0, 0, 0, 12));
        ambientSoundArray.Add(new AmbientSound(seagulls2, 1, 0, 0, 0, 0, 12));
        ambientSoundArray.Add(new AmbientSound(seagulls3, 1, 0, 0, 0, 0, 12));
        ambientSoundArray.Add(new AmbientSound(sparrow1, 1, 0, 0, 0, 6, 0));
        ambientSoundArray.Add(new AmbientSound(vortex1, 1, 0, 6, 50, 0, 0));
        ambientSoundArray.Add(new AmbientSound(wild_turkey_gobbles, 1, 50, 0, 0, 0, 0));
        ambientSoundArray.Add(new AmbientSound(wood_thrush1, 1, 0, 0, 0, 8, 0));
        ambientSoundArray.Add(new AmbientSound(wood_thrush2, 1, 0, 0, 0, 8, 0));
        ambientSoundArray.Add(new AmbientSound(wood_thrush3, 1, 0, 0, 0, 16, 0));
        ambientSoundArray.Add(new AmbientSound(wood_thrush4, 1, 0, 0, 0, 20, 0));
        ambientSoundArray.Add(new AmbientSound(woodpecker_pecking, 1, 40, 0, 0, 0, 20));

        // Begin playing of ambient environmental sounds.
        StartCoroutine(PlayAmbientSounds());

        // Begin monitoring for conditions to play music.
        StartCoroutine(MonitorForMusicConditions());
    }

    public void EnterPlay()
    {
        playSnapshot.TransitionTo(1f);
    }

    public void ExitPlay()
    {
        suspendSnapshot.TransitionTo(1f);
    }

    public void DefenseTriggered(int _triggerNationID, int _defenseNationID)
    {
        // If we're in the game and it's been at least the min time since the prev piece of music played...
        if (GameGUI.instance.IsInGame() && ((prevMusicEndTime == 0f) || (prevMusicEndTime < (Time.unscaledTime - MIN_SECONDS_BETWEEN_MUSIC))))
        {
            // If the prev piece of music wasn't musicBattle2, no panel is open, and the player has been attacking frequently...
            if ((prevMusic != musicBattle2) && (GameGUI.instance.GetActiveGamePanel() == GameGUI.GamePanel.GAME_PANEL_NONE) && (GameData.instance.GetUserEventFrequency(GameData.UserEventType.ATTACK) >= 5))
            {
                // If the defense was triggered by the player's nation, and the nation that owns the defense has been attacked successfully several times by the player's nation, and the nation that owns the defense has not been fighting back... 
                if ((_triggerNationID == GameData.instance.nationID) && (GameData.instance.GetNumBlocksTakenByPlayerNation(_defenseNationID) >= 3) && (GameData.instance.GetNumBlocksTakenFromPlayerNation(_defenseNationID) == 0)) {
                    PlayMusic(musicBattle2, true, 4f, 60f, MUSIC_FADE_DURATION, MUSIC_FADE_DURATION);
                }
            }
        }

        // If we're in the game and it's been at least the min time since the prev piece of music played...
        if (GameGUI.instance.IsInGame() && ((prevMusicEndTime == 0f) || (prevMusicEndTime < (Time.unscaledTime - MIN_SECONDS_BETWEEN_MUSIC))))
        {
            // If the prev piece of music wasn't musicBattle3, no panel is open, and the player has been attacking frequently...
            if ((prevMusic != musicBattle3) && (GameGUI.instance.GetActiveGamePanel() == GameGUI.GamePanel.GAME_PANEL_NONE) && (GameData.instance.GetUserEventFrequency(GameData.UserEventType.ATTACK) >= 5))
            {
                // If the triggered defense belongs to the player's nation, and the nation that triggered it has been attacked successfully several times by the player's nation... 
                if ((_defenseNationID == GameData.instance.nationID) && (GameData.instance.GetNumBlocksTakenByPlayerNation(_triggerNationID) >= 3)) {
                    PlayMusic(musicBattle3, true, 4f, 60f, MUSIC_FADE_DURATION, MUSIC_FADE_DURATION);
                }
            }
        }
    }

    public void QuestRewardCollected()
    {
        // If we're in the game...
        if (GameGUI.instance.IsInGame())
        {
            if ((prevMusicEndTime == 0f) || (prevMusicEndTime < (Time.unscaledTime - MIN_SECONDS_BETWEEN_MUSIC_SHORT))) {
                PlayMusic(musicMinorAchievement, false, 0f, -1, 0f, 0f);
            } else {
                Sound.instance.Play2D(Sound.instance.quest_collected);
            }
        }
    }

    public void BlockOccupied(int _x, int _z, BlockData _block_data)
    {
        // If we're in the game...
        if (GameGUI.instance.IsInGame())
        {
            if (_block_data.nationID == -1) // The block is empty before being occupied...
            {
                if (_block_data.objectID >= ObjectData.ORB_BASE_ID) // The player's nation has captured an orb...
                {
                    if ((prevMusicEndTime == 0f) || (prevMusicEndTime < (Time.unscaledTime - MIN_SECONDS_BETWEEN_MUSIC_SHORT))) // Music hasn't been played in a while...
                    {
                        // The unoccupied orb was captured.
                        PlayMusic(musicMinorAchievement, false, 0f, -1, 0f, 0f);
                    }
                }
            }
        } 
    }

    public void BlockCaptured(int _x, int _z, int _newNationID, int _oldNationID, BlockData _block_data)
    {
        // If we're in the game and it's been at least the min time since the prev piece of music played...
        if (GameGUI.instance.IsInGame() && ((prevMusicEndTime == 0f) || (prevMusicEndTime < (Time.unscaledTime - MIN_SECONDS_BETWEEN_MUSIC_SHORT))))
        {
            // If the block has been captured by the player's nation...
            if (_newNationID == GameData.instance.nationID)
            {
                if (_block_data.objectID >= ObjectData.ORB_BASE_ID) // The player's nation has captured an orb...
                {
                    if ((_oldNationID != -1) && (GameData.instance.GetNumBlocksTakenByPlayerNation(_oldNationID) >= 10))
                    {
                        // The orb was captured from a nation that the player's nation has previously takn many squares from.
                        PlayMusic(musicMajorAchievement1, false, 0f, -1, 0f, 0f);
                    }
                    else if ((_oldNationID != -1) && ((prevMusicEndTime == 0f) || (prevMusicEndTime < (Time.unscaledTime - MIN_SECONDS_BETWEEN_MUSIC))))
                    {
                        // The orb was captured from another nation that we haven't been taking many squares from.
                        PlayMusic(musicMajorAchievement2, false, 0f, -1, 0f, 0f);
                    }
                }
                else if (_block_data.objectID >= ObjectData.RESOURCE_OBJECT_BASE_ID) // The player's nation has captured a resource...
                {
                    if ((_oldNationID != -1) && ((prevMusicEndTime == 0f) || (prevMusicEndTime < (Time.unscaledTime - MIN_SECONDS_BETWEEN_MUSIC))))
                    {
                        // The resource was captured from another nation.
                        PlayMusic(musicMinorAchievement, false, 0f, -1, 0f, 0f);
                    }
                }
                else if ((_oldNationID != -1) && (GameData.instance.GetNumBlocksTakenByPlayerNation(_oldNationID) >= 10) && (GameData.instance.mapMode != GameData.MapMode.RAID))
                {
                    // We've captured a square from a nation that we've previously captured many squares from.
                    if (MapView.instance.NationIsInArea(_oldNationID, _x, _z, 20) == false)
                    {
                        // We've eliminated this nation from the local area.
                        PlayMusic(musicMajorAchievement2, false, 0f, -1, 0f, 0f);
                    }
                }
            }
        }

        // If we're in the game and it's been at least the min time since the prev piece of music played...
        if (GameGUI.instance.IsInGame() && ((prevMusicEndTime == 0f) || (prevMusicEndTime < (Time.unscaledTime - MIN_SECONDS_BETWEEN_MUSIC))))
        {
            //Debug.Log("BC2() attack frequency: " + GameData.instance.GetUserEventFrequency(GameData.UserEventType.ATTACK) + " prevMusic: " + prevMusic + ", musicBattle1: " + musicBattle1 + ", active game panel: " + GameGUI.instance.GetActiveGamePanel());
            // If the prev piece of music wasn't musicBattle1, no panel is open, and the player has been attacking frequently...
            if ((prevMusic != musicBattle1) && (GameGUI.instance.GetActiveGamePanel() == GameGUI.GamePanel.GAME_PANEL_NONE) && (GameData.instance.GetUserEventFrequency(GameData.UserEventType.ATTACK) >= 5))
            {
                //Debug.Log("BC3() taken by: " + GameData.instance.GetNumBlocksTakenByPlayerNation(_oldNationID) + ", taken from: " + GameData.instance.GetNumBlocksTakenFromPlayerNation(_oldNationID));
                // If the block has been captured by the player's nation, and the both the player's nation and the nation that the block was captured from have captured several of each others' blocks...
                if ((_newNationID == GameData.instance.nationID) && (GameData.instance.GetNumBlocksTakenByPlayerNation(_oldNationID) >= 8) && (GameData.instance.GetNumBlocksTakenFromPlayerNation(_oldNationID) >= 8)) {
                    PlayMusic(musicBattle1, true, 4f, 60f, MUSIC_FADE_DURATION, MUSIC_FADE_DURATION);
                }
            }
        }
    }

    public void SetZoomVolumeMultiplier(float _zoom_volume_multiplier)
    {
        zoom_volume_multiplier = _zoom_volume_multiplier;

        if (oceanWaves.isPlaying) {
            oceanWaves.volume = beach_volume * zoom_volume_multiplier;
        }

        if (ambientAudioSource.isPlaying) {
            ambientAudioSource.volume = ambient_volume * zoom_volume_multiplier;
        }
    }
	
	public void SetOceanVolume(float _beach_volume)
    {
        if (_beach_volume > 0)
        {
            // Set the volume of the AudioSource.
            oceanWaves.volume = _beach_volume * zoom_volume_multiplier;

            // Un-mute the AudioSource if it was muted.
            if (beach_volume == 0) {
                oceanWaves.mute = false;
            }
        }
        else 
        {
            // Mute the AudioSource if necessary.
            if (beach_volume > 0) {
                oceanWaves.mute = true;
            }
        }

        // Record the new beach volume.
        beach_volume = _beach_volume;

        // Record ambient probability for the beach sounds.
        ambientProbabilities[4] = _beach_volume;
    }

    public void SetAmbientAreaProbabilities(float _band0, float _band1, float _band2, float _band3)
    {
        // Record ambient probabilities for each of the area bands.
        ambientProbabilities[0] = _band0;
        ambientProbabilities[1] = _band1;
        ambientProbabilities[2] = _band2;
        ambientProbabilities[3] = _band3;
    }

    public IEnumerator MonitorForMusicConditions()
    {
        for (;;)
        {
            if (GameGUI.instance.IsInGame() && ((prevMusicEndTime == 0f) || (prevMusicEndTime < (Time.unscaledTime - MIN_SECONDS_BETWEEN_MUSIC))))
            {
                // Play musicExplore if appropriate
                if ((prevMusic != musicExplore) && (GameGUI.instance.GetActiveGamePanel() == GameGUI.GamePanel.GAME_PANEL_NONE) && (GameData.instance.GetUserEventFrequency(GameData.UserEventType.PAN_MAP) >= 20)) {
                    PlayMusic(musicExplore, false, 0f, -1f, 0f, 0f);
                }

                // Play musicBuild if appropriate
                if ((prevMusic != musicBuild) && (GameGUI.instance.GetActiveGamePanel() == GameGUI.GamePanel.GAME_PANEL_NONE) && (GameData.instance.GetUserEventFrequency(GameData.UserEventType.BUILD) >= 6) && ((GameData.instance.GetUserEventFrequency(GameData.UserEventType.OCCUPY) >= 8) || (GameData.instance.GetUserEventFrequency(GameData.UserEventType.EVACUATE) >= 8))) {
                    PlayMusic(musicBuild, false, 0f, -1f, 0f, 0f);
                }

                // Play musicAdvance if appropriate
                if ((prevMusic != musicAdvance) && (GameGUI.instance.GetActiveGamePanel() == GameGUI.GamePanel.GAME_PANEL_ADVANCES) && (GameData.instance.GetUserEventFrequency(GameData.UserEventType.SELECT_ADVANCE) >= 10)) {
                    PlayMusic(musicAdvance, false, 0f, -1f, 0f, 0f);
                }
            }

            // Wait five seconds before checking again.
            yield return new WaitForSeconds(5f);
        }
    }

    public bool IsMusicPlaying()
    {
        return ((prevMusic != null) && (prevMusicEndTime > Time.unscaledTime));
    }

    public void PlayMusic(AudioClip _music, bool _loop, float _delay, float _duration, float _fadeIn, float _fadeOut, float _startTime = 0f)
    {
        Debug.Log("PlayMusic() clip " + _music + ", _loop: " + _loop + ", _delay: " + _delay + ", duration: " + _duration + ", _fadeIn: " + _fadeIn + ", _fadeOut: " + _fadeOut);

        // If necessary, replace music that's currently playing by fading it out first.
        if (IsMusicPlaying()) 
        {
            _delay += (MUSIC_INTERRUPT_FADE_DURATION / 2f);
            prevMusicFader.FadeAndEnd(MUSIC_INTERRUPT_FADE_DURATION);
        }

        // If the music flag is turned off for the logged-in user, do not play the music.
        if (GameGUI.instance.IsInGame() && (GameData.instance.GetUserFlag(GameData.UserFlags.MUSIC) == false)) {
            return;
        }

        // Create an AudioSource to play the given clip through the music mixer group.
        GameObject audioSourceObject = GetAudioSource();
        AudioSource audioSource = audioSourceObject.GetComponent<AudioSource>();
        audioSource.clip = _music;
        audioSource.outputAudioMixerGroup = audioGroupMusic;
        audioSource.loop = _loop;
        audioSource.volume = 0f;
        audioSource.spatialBlend = 0;
        audioSource.pitch = 1;
        audioSource.time = _startTime;
        
        if (_duration == -1f) {
            _duration = _music.length;
        }

        prevMusic = _music;
        prevMusicFader = audioSource.GetComponent<MusicFader>();
        prevMusicEndTime = Time.unscaledTime + _delay + _duration;

        // Init playing the music
        prevMusicFader.Init(audioSource, _delay, _duration, MUSIC_FULL_VOLUME, _fadeIn, _fadeOut); 
    }

    public void FadeAndEndMusic()
    {
        if (IsMusicPlaying()) {
            prevMusicFader.FadeAndEnd(MUSIC_INTERRUPT_FADE_DURATION);
        }
    }

    public IEnumerator PlayAmbientSounds()
    {
        float probability = 0f, rand_num;

        // Don't play a sound in the first frame, before volumes are set.
        yield return null;

        for (;;)
        {
            if ((ambientAudioSource.isPlaying == false) && (IsMusicPlaying() == false)) // Don't play an ambient sound if one is already playing, or if music is playing.
            {
                //int elindex = 0;// TESTING
                foreach (AmbientSound element in ambientSoundArray)
                {
                    probability = 0f;

                    for (int i = 0; i < 5; i++) {
                        probability = Mathf.Max(probability, ambientProbabilities[i] * element.probabilityPerSecond[i]);
                        //Debug.Log("clip: " + element.audioClip + ": ambientProbabilities[" + i + "]: " + ambientProbabilities[i] + ", element.probabilityPerSecond[" + i + "]: " + element.probabilityPerSecond[i] + ", probability: " + probability);
                    }

                    rand_num = UnityEngine.Random.value;

                    // TESTING
                    //if (elindex == 1) Debug.Log("clip: " + element.audioClip + ", prob: " + probability + ", rand: " + rand_num);
                    //elindex++;                    

                    // Generate random number 0->1. If it falls under the determined probability, play the sound.
                    if (rand_num < probability)
                    {
                        // Place the sound randomly near the camera in 3D space.
                        Vector3 cameraPosition = MapView.instance.camera.transform.position;
                        ambientAudioSource.gameObject.transform.position = new Vector3(cameraPosition.x + UnityEngine.Random.Range(-400, 400), 200 + UnityEngine.Random.Range(-100, 100), cameraPosition.z + UnityEngine.Random.Range(-400, 400));
                        ambient_volume = element.volume;
                        ambientAudioSource.clip = element.audioClip;
                        ambientAudioSource.Play();
                        break;
                    }
                }
            }

            // Wait one second before checking again.
            yield return new WaitForSeconds(1f);
        }
    }

    public GameObject InstantiateObjectAudioSource(int _objectID)
    {
        GameObject audioSource = null;

        if (_objectID == 1006) // Oil deposit
        {
            audioSource = UnityEngine.Object.Instantiate(objectAudioSourcePrefab) as GameObject;
            audioSource.GetComponent<AudioSource>().clip = oil_deposit;
        }
        else if (_objectID == 1009) // Geothermal vent
        {
            audioSource = UnityEngine.Object.Instantiate(objectAudioSourcePrefab) as GameObject;
            audioSource.GetComponent<AudioSource>().clip = geothermal_vent;
        }
        else if (_objectID == 1010) // Ley lines
        {
            audioSource = UnityEngine.Object.Instantiate(objectAudioSourcePrefab) as GameObject;
            audioSource.GetComponent<AudioSource>().clip = ley_lines;
        }
        else if (_objectID == 2000) // Orb of Noontide
        {
            audioSource = UnityEngine.Object.Instantiate(objectAudioSourcePrefab) as GameObject;
            audioSource.GetComponent<AudioSource>().clip = orb_of_noontide;   
        }
        else if (_objectID == 2001) // Orb of Shadow
        {
            audioSource = UnityEngine.Object.Instantiate(objectAudioSourcePrefab) as GameObject;
            audioSource.GetComponent<AudioSource>().clip = orb_of_shadow;   
        }
        else if (_objectID == 2002) // Orb of Destiny
        {
            audioSource = UnityEngine.Object.Instantiate(objectAudioSourcePrefab) as GameObject;
            audioSource.GetComponent<AudioSource>().clip = orb_of_destiny;   
        }
        else if (_objectID == 2003) // Orb of the Void
        {
            audioSource = UnityEngine.Object.Instantiate(objectAudioSourcePrefab) as GameObject;
            audioSource.GetComponent<AudioSource>().clip = orb_of_the_void;   
        }
        else if (_objectID == 2004) // Orb of Fire
        {
            audioSource = UnityEngine.Object.Instantiate(objectAudioSourcePrefab) as GameObject;
            audioSource.GetComponent<AudioSource>().clip = orb_of_fire;   
        }

        if (audioSource != null) 
        {
            audioSource.GetComponent<AudioSource>().volume = OBJECT_AUDIO_VOLUME;
            audioSource.GetComponent<AudioSource>().Play();
        }

        return audioSource;
    }

    public void PlaySoundForAttack(int _x, int _z)
    {
        foreach(KeyValuePair<int, float> entry in GameData.instance.tempTechExpireTime)
        {
            if (entry.Value < Time.unscaledTime) {
                continue;
            }

            if (entry.Key == 309) // Ace Pilot
            {
            }
        }
    }

    public void Play2D(AudioClip _clip, float _volume = 1f, bool _play_over_music = false)
    {
        // Do not play UI sound if music is playing.
        if (IsMusicPlaying() && !_play_over_music) {
            return;
        }

        // Create an AudioSource to play the given clip through the GUI mixer group.
        GameObject audioSourceObject = GetAudioSource();
        AudioSource audioSource = audioSourceObject.GetComponent<AudioSource>();
        audioSource.clip = _clip;
        audioSource.outputAudioMixerGroup = audioGroup2D;
        audioSource.loop = false;
        audioSource.volume = _volume;
        audioSource.spatialBlend = 0;
        audioSource.pitch = 1;
        audioSource.Play();

        // Release the AudioSource object once the clip has finished playing.
        StartCoroutine(ReleaseAfterDuration(audioSourceObject, _clip.length));
    }

    public void Play2DAfterDelay(float _delay, AudioClip _clip)
    {
        StartCoroutine(Play2DAfterDelay_Coroutine(_delay, _clip));
    }

    public IEnumerator Play2DAfterDelay_Coroutine(float _delay, AudioClip _clip)
    {
        yield return new WaitForSeconds(_delay);
        Play2D(_clip);
    }

    public void PlayInWorld(AudioClip _clip, Vector3 _world_pos, float _volume=DEFAULT_AUDIO_VOLUME, float _spatial_blend = 1, float _pitch = 1)
    {
        // Create an AudioSource to play the given clip at the specified world position.
        GameObject audioSourceObject = GetAudioSource();
        AudioSource audioSource = audioSourceObject.GetComponent<AudioSource>();
        audioSourceObject.transform.position = _world_pos;
        audioSource.clip = _clip;
        audioSource.outputAudioMixerGroup = audioGroup3D;
        audioSource.loop = false;
        audioSource.volume = _volume;
        audioSource.spatialBlend = _spatial_blend;
        audioSource.pitch = _pitch;
        audioSource.Play();

        // Release the AudioSource object once the clip has finished playing.
        StartCoroutine(ReleaseAfterDuration(audioSourceObject, _clip.length));
    }

    public void PlayInWorldForDuration(AudioClip _clip, Vector3 _world_pos, float _duration, float _volume=DEFAULT_AUDIO_VOLUME, float _spatial_blend = 1)
    {
        // Create an AudioSource to play the given clip at the specified world position.
        GameObject audioSourceObject = GetAudioSource();
        AudioSource audioSource = audioSourceObject.GetComponent<AudioSource>();
        audioSourceObject.transform.position = _world_pos;
        audioSource.clip = _clip;
        audioSource.outputAudioMixerGroup = audioGroup3D;
        audioSource.loop = true;
        audioSource.volume = _volume;
        audioSource.spatialBlend = _spatial_blend;
        audioSource.Play();

        // Release the AudioSource object once the given duration has ended.
        StartCoroutine(ReleaseAfterDuration(audioSourceObject, _duration));
    }

    public void PlayInWorldAfterDelay(float _delay, AudioClip _clip, Vector3 _world_pos, float _volume=DEFAULT_AUDIO_VOLUME, float _spatial_blend = 1, float _pitch = 1)
    {
        StartCoroutine(PlayInWorldAfterDelay_Coroutine(_delay, _clip, _world_pos, _volume, _spatial_blend, _pitch));
    }

    public IEnumerator PlayInWorldAfterDelay_Coroutine(float _delay, AudioClip _clip, Vector3 _world_pos, float _volume=DEFAULT_AUDIO_VOLUME, float _spatial_blend = 1, float _pitch = 1)
    {
        yield return new WaitForSeconds(_delay);
        PlayInWorld(_clip, _world_pos, _volume, _spatial_blend, _pitch);
    }

    public GameObject GetAudioSource()
    {
        GameObject audioSource;

        if (audioSourcePool.Count == 0) {
            audioSource = UnityEngine.Object.Instantiate(objectAudioSourcePrefab) as GameObject;
        } else {
            audioSource = audioSourcePool.Pop();
        }

        // Add the MusicFader component to the AudioSource.
        if (audioSource.GetComponent<MusicFader>() == null) {
            audioSource.AddComponent<MusicFader>();
        }

        return audioSource;
    }

    public void ReleaseAudioSource(GameObject _audioSource)
    {
        audioSourcePool.Push(_audioSource);
    }

    public IEnumerator ReleaseAfterDuration(GameObject _audioSource, float _duration)
    {
        yield return new WaitForSeconds(_duration);
        _audioSource.GetComponent<AudioSource>().Stop();
        ReleaseAudioSource(_audioSource);
    }

    public void SetSoundVolume(float _volume)
    {
        audioGroupMaster.audioMixer.SetFloat("master_volume", _volume);
    }

    public class MusicFader : MonoBehaviour
    {
        public AudioSource audioSource;
        float startTime, fadeInEndTime, fadeOutStartTime, endTime, fadeInDuration, fadeOutDuration, fullVolume;

        public void Init(AudioSource _audioSource, float _delay, float _duration, float _fullVolume, float _fadeIn, float _fadeOut)
        {
            audioSource = _audioSource;
            fullVolume = _fullVolume;
            startTime = Time.unscaledTime + _delay;
            fadeInEndTime = startTime + _fadeIn;
            endTime = startTime + _duration;
            fadeOutStartTime = endTime - _fadeOut;
            fadeInDuration = _fadeIn;
            fadeOutDuration = _fadeOut;

            StartCoroutine(Run());
        }

        public void FadeAndEnd(float _fadeOut)
        {
            fadeOutStartTime = Time.unscaledTime;
            endTime = Time.unscaledTime + _fadeOut;
            fadeOutDuration = _fadeOut;
        }

        public void End()
        {
            fadeOutStartTime = endTime = Time.unscaledTime;
        }

        public IEnumerator Run()
        {
            //Debug.Log("MusicFader starting at time " + Time.unscaledTime + ", startTime: " + startTime + ", fadeOutStartTime: " + fadeOutStartTime + ", endTime: " + endTime);

            // If there's a delay or fade in, start by setting the volume to 0.
            if ((startTime > Time.unscaledTime) || (fadeInEndTime > startTime)) {
                audioSource.volume = 0;
            }

            // Wait through the delay
            if (startTime > Time.unscaledTime) {
                yield return new WaitForSeconds(startTime - Time.unscaledTime);
            }

            // Start playing the AudioSource.
            audioSource.Play();

            // Fade in
            if (startTime != fadeInEndTime)
            {
                while (Time.unscaledTime < fadeInEndTime) 
                {
                    audioSource.volume = ((Time.unscaledTime - startTime) / fadeInDuration) * ((Time.unscaledTime - startTime) / fadeInDuration) * fullVolume;
                    yield return null; // Wait until next frame
                }
            }

            // Set the volume to full
            audioSource.volume = fullVolume;

            // Wait until fadeOutStartTime (in increments, in case the music is interrupted).
            while (Time.unscaledTime < fadeOutStartTime) {
                yield return new WaitForSeconds(0.2f);
            }

            // Fade out
            if (endTime != fadeOutStartTime)
            {
                while (Time.unscaledTime < endTime) 
                {
                    audioSource.volume = (1f - ((Time.unscaledTime - fadeOutStartTime) / fadeOutDuration)) * (1f - ((Time.unscaledTime - fadeOutStartTime) / fadeOutDuration)) * fullVolume;
                    yield return null; // Wait until next frame
                }
            }

            // Set the volume to 0
            audioSource.volume = 0f;

            // All done
            audioSource.GetComponent<AudioSource>().Stop();
            Sound.instance.ReleaseAudioSource(audioSource.gameObject);
            audioSource = null;
        }
    }

    [System.Serializable]
    public class AmbientSound
    {
        public AudioClip audioClip;
        public float volume;
        public float[] probabilityPerSecond = new float[5];

        public AmbientSound(AudioClip _audioClip, float _volume, float _period_band_0, float _period_band_1, float _period_band_2, float _period_band_3, float _period_beach)
        {
            audioClip = _audioClip;
            volume = _volume;

            // Convert from period band (specified in avg number of minutes between period when the sound is played) to probability per second.
            probabilityPerSecond[0] = (_period_band_0 == 0) ? 0 : (1f / (60f * _period_band_0));
            probabilityPerSecond[1] = (_period_band_1 == 0) ? 0 : (1f / (60f * _period_band_1));
            probabilityPerSecond[2] = (_period_band_2 == 0) ? 0 : (1f / (60f * _period_band_2));
            probabilityPerSecond[3] = (_period_band_3 == 0) ? 0 : (1f / (60f * _period_band_3));
            probabilityPerSecond[4] = (_period_beach == 0) ? 0 : (1f / (60f * _period_beach));
        }
    }
}
